# 消息通知中台 + 平台安全 重构实施与任务拆分

> 日期: 2026-06-03
> 实施范围: `oa-message` 消息通知中台 + `oa-platform` 平台安全与配置
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`
> 模板参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 目标

为 OA 系统建立统一的消息通知中台和平台安全基础设施,使所有业务模块(HR/Finance/Admin/Meeting/Workflow)可以:

1. 通过统一入口 `MessageNotificationService.notify()` 发送多渠道消息。
2. 基于用户偏好和模板变量渲染消息内容,实现渠道自适应。
3. 失败重试和死信队列保证送达率,SLA 可观测。
4. 单一 JWT 认证 + Redis Token Session 支撑单点登录与多端互踢。
5. 通过 SQL 拦截器对查询自动注入数据权限(行级隔离)。
6. 对身份证/银行卡/薪资等敏感字段 AES 加密落库,展示按权限脱敏。
7. 全量审计操作日志与登录日志,字典/配置集中管理。
8. 完成后,后端/前端/移动端构建通过,具备可演示的端到端链路。

---

## 2. 边界

### 2.1 本次重构包含

| 区域 | 内容 |
|------|------|
| 数据库 | `msg_template`、`msg_user_preference`、`msg_message`、`msg_channel_log`、`msg_dead_letter`、`sys_config`、`sys_dict_type`、`sys_dict_data`、`sys_operation_log`、`sys_login_log`、`sys_data_scope` 共 11 张新表 |
| 后端模块 | `oa-message`(消息)、`oa-platform`(安全/字典/配置/日志) |
| 渠道适配 | 站内信(WebSocket)、SMTP 邮件、阿里云短信、企业微信机器人 |
| 认证 | JWT 签发/解析、Redis Token Session、多端互踢、滑动过期 |
| 限流 | `RateLimitInterceptor`(登录 5 次/分钟/IP) |
| 加密 | AES-256-GCM 工具 + `DataPermissionInterceptor` 行级数据隔离 |
| 审计 | `OperationLog` AOP、`LoginLog` Service |
| 字典/配置 | `DictService`、`ConfigService`、Web 端管理 UI |
| Web 端 | 消息中心、消息偏好、字典管理、配置管理、操作日志、登录日志 |
| Mobile | 消息中心(已读/未读/未读数) |
| 测试 | 渠道分发、模板渲染、重试/死信、JWT/DataScope/Encrypt 单测,前端构建 |

### 2.2 本次重构不包含

| 不包含 | 原因 |
|--------|------|
| APP 原生 APNs/FCM 推送 | 移动端只做 uni-app H5 + 微信小程序,WebSocket 已覆盖 |
| 外呼电话/Twilio/Voice | 与海外电话合规与本轮范围无关 |
| 物理防泄漏/DLP | 属于企业安全部门独立建设 |
| VPN/零信任网络 | 属于基础设施层 |
| 全文检索 ES 集成 | 消息搜索可用 LIKE,ES 留给后续知识库 |
| 端到端加密(E2EE) | 当前仅落库加密+传输 HTTPS,足够本轮要求 |
| SSO/OAuth2 第三方登录 | 留作下轮,本期只做账号密码 + 图形验证码 |
| 前端 monorepo 改造 | 同步其他任务拆分策略,延后 |

---

## 3. 数据模型 DDL

统一说明:

- 数据库:`oa_system`,字符集 `utf8mb4`,排序 `utf8mb4_0900_ai_ci`。
- 逻辑删除字段:`del_flag` TINYINT NOT NULL DEFAULT 0,索引中按需冗余。
- 公共字段:`create_by`、`create_time`、`update_by`、`update_time`、`del_flag`。
- 金额统一 `DECIMAL(18,2)`,时间统一 `DATETIME`(默认 `CURRENT_TIMESTAMP`)。

### 3.1 消息中台 5 张表

```sql
-- ============================================================
-- 1. msg_template  消息模板
-- ============================================================
DROP TABLE IF EXISTS `msg_template`;
CREATE TABLE `msg_template` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `template_code`  VARCHAR(64)  NOT NULL                         COMMENT '模板编码(业务唯一,如 LEAVE_PASSED)',
  `template_name`  VARCHAR(128) NOT NULL                         COMMENT '模板名称',
  `msg_type`       VARCHAR(32)  NOT NULL                         COMMENT '消息类型 TODO_ASSIGN/TODO_URGE/APPROVAL_PASS/APPROVAL_REJECT/NOTICE_PUBLISH/MEETING_REMIND/SYSTEM_ALERT',
  `default_channels` VARCHAR(128) NOT NULL DEFAULT 'SITE'        COMMENT '默认渠道 JSON 数组',
  `title_template` VARCHAR(256) NOT NULL                         COMMENT '标题模板,支持 ${var} 变量',
  `content_template` MEDIUMTEXT  NOT NULL                        COMMENT '内容模板,支持 ${var} 与 #if 简单指令',
  `email_subject`  VARCHAR(256)          DEFAULT NULL            COMMENT '邮件主题模板(可空)',
  `sms_template_id` VARCHAR(64)          DEFAULT NULL            COMMENT '阿里云短信模板ID(可空)',
  `wechat_template` MEDIUMTEXT           DEFAULT NULL            COMMENT '企业微信消息模板 JSON(可空)',
  `variables_schema` JSON                DEFAULT NULL            COMMENT '变量定义 JSON Schema',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '启用标志 0-否 1-是',
  `version`        INT          NOT NULL DEFAULT 1                COMMENT '乐观锁版本号',
  `remark`         VARCHAR(500)          DEFAULT NULL            COMMENT '备注',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_template_code` (`template_code`, `del_flag`),
  KEY `idx_msg_template_type` (`msg_type`, `enabled`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

-- ============================================================
-- 2. msg_user_preference  用户消息偏好
-- ============================================================
DROP TABLE IF EXISTS `msg_user_preference`;
CREATE TABLE `msg_user_preference` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `emp_id`         BIGINT       NOT NULL                         COMMENT '员工ID',
  `msg_type`       VARCHAR(32)  NOT NULL                         COMMENT '消息类型,与模板表对应',
  `channels`       VARCHAR(128) NOT NULL                         COMMENT '启用渠道 JSON 数组 ["SITE","EMAIL","SMS","WECHAT"]',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '是否启用 0-否 1-是',
  `quiet_start`    TIME                  DEFAULT NULL            COMMENT '免打扰开始时间',
  `quiet_end`      TIME                  DEFAULT NULL            COMMENT '免打扰结束时间',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_pref_emp_type` (`emp_id`, `msg_type`, `del_flag`),
  KEY `idx_msg_pref_emp` (`emp_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息偏好配置';

-- ============================================================
-- 3. msg_message  消息主表
-- ============================================================
DROP TABLE IF EXISTS `msg_message`;
CREATE TABLE `msg_message` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `biz_id`         VARCHAR(64)           DEFAULT NULL            COMMENT '业务ID,如流程实例ID',
  `biz_type`       VARCHAR(32)           DEFAULT NULL            COMMENT '业务类型 LEAVE/EXPENSE/CONTRACT/...',
  `template_code`  VARCHAR(64)  NOT NULL                         COMMENT '使用的模板编码',
  `msg_type`       VARCHAR(32)  NOT NULL                         COMMENT '消息类型冗余,便于查询',
  `sender_id`      BIGINT                DEFAULT NULL            COMMENT '发送人(0=系统)',
  `receiver_id`    BIGINT       NOT NULL                         COMMENT '接收员工ID',
  `title`          VARCHAR(256) NOT NULL                         COMMENT '渲染后标题',
  `content`        MEDIUMTEXT   NOT NULL                        COMMENT '渲染后内容',
  `link_url`       VARCHAR(512)          DEFAULT NULL            COMMENT '站内信跳转链接',
  `priority`       TINYINT      NOT NULL DEFAULT 1                COMMENT '优先级 0-低 1-普通 2-高 3-紧急',
  `status`         TINYINT      NOT NULL DEFAULT 0                COMMENT '状态 0-未发送 1-发送中 2-已发送 3-部分失败 4-失败',
  `read_flag`      TINYINT      NOT NULL DEFAULT 0                COMMENT '已读 0-否 1-是',
  `read_time`      DATETIME              DEFAULT NULL            COMMENT '已读时间',
  `scheduled_time` DATETIME              DEFAULT NULL            COMMENT '定时发送时间',
  `sent_time`      DATETIME              DEFAULT NULL            COMMENT '实际发送时间',
  `ext_json`       JSON                 DEFAULT NULL            COMMENT '扩展字段,如卡片、按钮',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_msg_msg_receiver_status` (`receiver_id`, `status`, `create_time`, `del_flag`),
  KEY `idx_msg_msg_receiver_read` (`receiver_id`, `read_flag`, `create_time`, `del_flag`),
  KEY `idx_msg_msg_biz` (`biz_type`, `biz_id`),
  KEY `idx_msg_msg_template` (`template_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息主表';

-- ============================================================
-- 4. msg_channel_log  渠道分发日志
-- ============================================================
DROP TABLE IF EXISTS `msg_channel_log`;
CREATE TABLE `msg_channel_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `message_id`     BIGINT       NOT NULL                         COMMENT '消息主表ID',
  `channel`        VARCHAR(16)  NOT NULL                         COMMENT '渠道 SITE/EMAIL/SMS/WECHAT',
  `receiver`       VARCHAR(256) NOT NULL                         COMMENT '接收方 SITE=empId,EMAIL=email,SMS=mobile,WECHAT=userid',
  `status`         TINYINT      NOT NULL DEFAULT 0                COMMENT '0-待发送 1-成功 2-失败',
  `retry_count`    INT          NOT NULL DEFAULT 0                COMMENT '已重试次数',
  `max_retry`      INT          NOT NULL DEFAULT 3                COMMENT '最大重试次数',
  `next_retry_time` DATETIME             DEFAULT NULL            COMMENT '下一次重试时间',
  `response`       VARCHAR(1000)         DEFAULT NULL            COMMENT '渠道返回(截断)',
  `error_message`  VARCHAR(1000)         DEFAULT NULL            COMMENT '错误信息(截断)',
  `cost_ms`        INT                   DEFAULT NULL            COMMENT '耗时(毫秒)',
  `sent_time`      DATETIME              DEFAULT NULL            COMMENT '实际发送时间',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_msg_channel_msg` (`message_id`),
  KEY `idx_msg_channel_status_retry` (`status`, `next_retry_time`),
  KEY `idx_msg_channel_channel_status` (`channel`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息渠道分发日志';

-- ============================================================
-- 5. msg_dead_letter  死信队列
-- ============================================================
DROP TABLE IF EXISTS `msg_dead_letter`;
CREATE TABLE `msg_dead_letter` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `message_id`     BIGINT                DEFAULT NULL            COMMENT '原始消息ID(可空,手工投递)',
  `channel_log_id` BIGINT                DEFAULT NULL            COMMENT '原始渠道日志ID',
  `channel`        VARCHAR(16)  NOT NULL                         COMMENT '渠道',
  `receiver`       VARCHAR(256) NOT NULL                         COMMENT '接收方',
  `payload`        MEDIUMTEXT   NOT NULL                        COMMENT '原始 payload 快照',
  `error_message`  VARCHAR(2000)         DEFAULT NULL            COMMENT '最后错误',
  `retry_count`    INT          NOT NULL DEFAULT 0                COMMENT '已重试次数',
  `status`         TINYINT      NOT NULL DEFAULT 0                COMMENT '0-待处理 1-已补偿 2-已丢弃',
  `handled_by`     BIGINT                DEFAULT NULL            COMMENT '处理人',
  `handled_time`   DATETIME              DEFAULT NULL            COMMENT '处理时间',
  `handle_remark`  VARCHAR(500)          DEFAULT NULL            COMMENT '处理备注',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_msg_dl_status_time` (`status`, `create_time`),
  KEY `idx_msg_dl_channel_status` (`channel`, `status`, `create_time`),
  KEY `idx_msg_dl_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息死信队列';
```

### 3.2 平台安全 6 张表

```sql
-- ============================================================
-- 6. sys_config  系统配置
-- ============================================================
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `config_key`     VARCHAR(128) NOT NULL                         COMMENT '配置键(全局唯一)',
  `config_value`   MEDIUMTEXT   NOT NULL                        COMMENT '配置值',
  `config_type`    VARCHAR(16)  NOT NULL DEFAULT 'STRING'        COMMENT '类型 STRING/NUMBER/BOOLEAN/JSON',
  `config_group`   VARCHAR(64)  NOT NULL DEFAULT 'default'       COMMENT '配置分组 security/message/feature',
  `value_encrypted` TINYINT     NOT NULL DEFAULT 0                COMMENT '值是否 AES 加密 0-否 1-是',
  `description`    VARCHAR(500)          DEFAULT NULL            COMMENT '配置说明',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '启用标志 0-否 1-是',
  `version`        INT          NOT NULL DEFAULT 1                COMMENT '乐观锁',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_key` (`config_key`, `del_flag`),
  KEY `idx_sys_config_group` (`config_group`, `enabled`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================================
-- 7. sys_dict_type  字典类型
-- ============================================================
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `dict_type`      VARCHAR(64)  NOT NULL                         COMMENT '字典类型编码(全局唯一)',
  `dict_name`      VARCHAR(128) NOT NULL                         COMMENT '字典名称',
  `description`    VARCHAR(500)          DEFAULT NULL            COMMENT '字典说明',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '启用标志',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type_code` (`dict_type`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';

-- ============================================================
-- 8. sys_dict_data  字典数据
-- ============================================================
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `dict_type`      VARCHAR(64)  NOT NULL                         COMMENT '字典类型编码',
  `dict_label`     VARCHAR(128) NOT NULL                         COMMENT '字典标签',
  `dict_value`     VARCHAR(128) NOT NULL                         COMMENT '字典值',
  `css_class`      VARCHAR(64)           DEFAULT NULL            COMMENT '标签样式',
  `list_class`     VARCHAR(64)           DEFAULT NULL            COMMENT '列表样式 primary/success/warning/danger',
  `is_default`     TINYINT      NOT NULL DEFAULT 0                COMMENT '是否默认 0-否 1-是',
  `sort_order`     INT          NOT NULL DEFAULT 0                COMMENT '排序',
  `description`    VARCHAR(500)          DEFAULT NULL            COMMENT '说明',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '启用标志',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_data_type_value` (`dict_type`, `dict_value`, `del_flag`),
  KEY `idx_sys_dict_data_type_sort` (`dict_type`, `sort_order`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典数据表';

-- ============================================================
-- 9. sys_operation_log  操作日志
-- ============================================================
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `module`         VARCHAR(64)  NOT NULL                         COMMENT '业务模块 hr/finance/admin/...',
  `operation`      VARCHAR(64)  NOT NULL                         COMMENT '操作名 create/update/delete/approve/export/...',
  `business_type`  VARCHAR(32)           DEFAULT NULL            COMMENT '业务对象类型 leave/expense/contract',
  `business_id`    VARCHAR(64)           DEFAULT NULL            COMMENT '业务对象ID',
  `request_method` VARCHAR(8)            DEFAULT NULL            COMMENT 'HTTP 方法 GET/POST/PUT/DELETE',
  `request_url`    VARCHAR(512)          DEFAULT NULL            COMMENT '请求 URL(截断)',
  `request_params` MEDIUMTEXT            DEFAULT NULL            COMMENT '请求参数(脱敏后)',
  `response_result` MEDIUMTEXT           DEFAULT NULL            COMMENT '返回结果(截断)',
  `emp_id`         BIGINT       NOT NULL                         COMMENT '操作员工ID',
  `emp_name`       VARCHAR(64)           DEFAULT NULL            COMMENT '员工姓名冗余',
  `dept_id`        BIGINT                DEFAULT NULL            COMMENT '部门ID',
  `ip`             VARCHAR(64)           DEFAULT NULL            COMMENT '客户端 IP',
  `user_agent`     VARCHAR(512)          DEFAULT NULL            COMMENT 'UA',
  `status`         TINYINT      NOT NULL DEFAULT 1                COMMENT '1-成功 0-失败',
  `error_message`  VARCHAR(2000)         DEFAULT NULL            COMMENT '错误信息',
  `cost_ms`        BIGINT       NOT NULL DEFAULT 0                COMMENT '耗时(毫秒)',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_oplog_emp_time` (`emp_id`, `create_time`),
  KEY `idx_sys_oplog_module_time` (`module`, `create_time`),
  KEY `idx_sys_oplog_biz` (`business_type`, `business_id`),
  KEY `idx_sys_oplog_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ============================================================
-- 10. sys_login_log  登录日志
-- ============================================================
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `emp_id`         BIGINT                DEFAULT NULL            COMMENT '员工ID(失败时为 0)',
  `username`       VARCHAR(64)  NOT NULL                         COMMENT '登录账号',
  `login_type`     VARCHAR(16)  NOT NULL DEFAULT 'PASSWORD'      COMMENT '登录类型 PASSWORD/CAPTCHA/SSO/REFRESH',
  `status`         TINYINT      NOT NULL                         COMMENT '1-成功 0-失败',
  `failure_reason` VARCHAR(128)          DEFAULT NULL            COMMENT '失败原因',
  `ip`             VARCHAR(64)           DEFAULT NULL            COMMENT '客户端 IP',
  `location`       VARCHAR(128)          DEFAULT NULL            COMMENT '登录地点',
  `browser`        VARCHAR(64)           DEFAULT NULL            COMMENT '浏览器',
  `os`             VARCHAR(64)           DEFAULT NULL            COMMENT '操作系统',
  `device`         VARCHAR(64)           DEFAULT NULL            COMMENT '设备类型 WEB/MOBILE/...',
  `user_agent`     VARCHAR(512)          DEFAULT NULL            COMMENT 'UA',
  `session_id`     VARCHAR(64)           DEFAULT NULL            COMMENT '会话ID(Redis token key)',
  `kick_prev`      TINYINT      NOT NULL DEFAULT 0                COMMENT '是否踢出上一次登录 0-否 1-是',
  `login_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_loginlog_emp_time` (`emp_id`, `login_time`),
  KEY `idx_sys_loginlog_status_time` (`status`, `login_time`),
  KEY `idx_sys_loginlog_username_time` (`username`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统登录日志表';

-- ============================================================
-- 11. sys_data_scope  数据权限规则
-- ============================================================
DROP TABLE IF EXISTS `sys_data_scope`;
CREATE TABLE `sys_data_scope` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
  `scope_name`     VARCHAR(64)  NOT NULL                         COMMENT '规则名称',
  `scope_code`     VARCHAR(64)  NOT NULL                         COMMENT '规则编码(唯一)',
  `role_id`        BIGINT                DEFAULT NULL            COMMENT '绑定角色ID(可空,表示按人)',
  `emp_id`         BIGINT                DEFAULT NULL            COMMENT '绑定员工ID(可空,表示按角色)',
  `scope_type`     VARCHAR(16)  NOT NULL                         COMMENT 'ALL/DEPT/DEPT_AND_SUB/PERSONAL/CUSTOM',
  `dept_ids`       VARCHAR(2000)         DEFAULT NULL            COMMENT '自定义部门ID列表,逗号分隔',
  `include_self`   TINYINT      NOT NULL DEFAULT 1                COMMENT '是否包含本人创建 0-否 1-是',
  `description`    VARCHAR(500)          DEFAULT NULL            COMMENT '说明',
  `enabled`        TINYINT      NOT NULL DEFAULT 1                COMMENT '启用标志',
  `priority`       INT          NOT NULL DEFAULT 0                COMMENT '优先级(数字越大越优先)',
  `create_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT       NOT NULL DEFAULT 0                COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0                COMMENT '逻辑删除 0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_ds_code` (`scope_code`, `del_flag`),
  KEY `idx_sys_ds_role` (`role_id`, `enabled`, `del_flag`),
  KEY `idx_sys_ds_emp` (`emp_id`, `enabled`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限规则表';
```

### 3.3 初始化种子数据

```sql
-- 消息模板种子
INSERT INTO `msg_template` (`template_code`, `template_name`, `msg_type`, `default_channels`, `title_template`, `content_template`, `enabled`, `create_by`, `update_by`)
VALUES
('TODO_ASSIGN', '待办分配', 'TODO_ASSIGN', '["SITE","WECHAT"]', '您有一条新的待办', '${empName}，您有新的待办【${title}】，请及时处理。', 1, 0, 0),
('TODO_URGE', '催办提醒', 'TODO_URGE', '["SITE","SMS"]', '【催办】${title}', '${empName}，您的待办【${title}】已被催办,请尽快处理。', 1, 0, 0),
('APPROVAL_PASS', '审批通过', 'APPROVAL_PASS', '["SITE"]', '您的申请【${title}】已通过', '${empName}，您的【${bizType}】申请已审批通过。', 1, 0, 0),
('APPROVAL_REJECT', '审批驳回', 'APPROVAL_REJECT', '["SITE","SMS"]', '您的申请【${title}】被驳回', '${empName}，您的【${bizType}】申请被驳回,原因:${reason}。', 1, 0, 0),
('NOTICE_PUBLISH', '公告发布', 'NOTICE_PUBLISH', '["SITE","EMAIL"]', '公告:${title}', '请查看最新公告《${title}》。', 1, 0, 0),
('MEETING_REMIND', '会议提醒', 'MEETING_REMIND', '["SITE","SMS","WECHAT"]', '会议提醒:${title}', '您预约的会议【${title}】将于 ${startTime} 开始,请准时参加。', 1, 0, 0),
('SYSTEM_ALERT', '系统告警', 'SYSTEM_ALERT', '["SITE","EMAIL","SMS"]', '【系统告警】${title}', '系统告警:${content}', 1, 0, 0);

-- 字典类型种子
INSERT INTO `sys_dict_type` (`dict_type`, `dict_name`, `description`, `create_by`, `update_by`) VALUES
('MSG_CHANNEL', '消息渠道', 'SITE/EMAIL/SMS/WECHAT', 0, 0),
('MSG_TYPE', '消息类型', '业务消息分类', 0, 0),
('DATA_SCOPE', '数据权限范围', 'ALL/DEPT/DEPT_AND_SUB/PERSONAL/CUSTOM', 0, 0),
('SENSITIVE_FIELD', '敏感字段', 'ID_CARD/BANK_CARD/SALARY/PHONE', 0, 0),
('LOGIN_STATUS', '登录结果', 'SUCCESS/FAILURE', 0, 0);

-- 字典数据种子(部分)
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`, `is_default`, `create_by`, `update_by`) VALUES
('MSG_CHANNEL', '站内信', 'SITE', 1, 1, 0, 0),
('MSG_CHANNEL', '邮件',   'EMAIL', 2, 0, 0, 0),
('MSG_CHANNEL', '短信',   'SMS', 3, 0, 0, 0),
('MSG_CHANNEL', '企业微信', 'WECHAT', 4, 0, 0, 0),
('DATA_SCOPE', '全部', 'ALL', 1, 0, 0, 0),
('DATA_SCOPE', '本部门', 'DEPT', 2, 0, 0, 0),
('DATA_SCOPE', '本部门及下级', 'DEPT_AND_SUB', 3, 0, 0, 0),
('DATA_SCOPE', '仅本人', 'PERSONAL', 4, 0, 0, 0),
('DATA_SCOPE', '自定义', 'CUSTOM', 5, 0, 0, 0),
('LOGIN_STATUS', '成功', '1', 1, 0, 0, 0),
('LOGIN_STATUS', '失败', '0', 2, 0, 0, 0);

-- 系统配置种子
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`, `create_by`, `update_by`) VALUES
('security.jwt.secret',           'PLACEHOLDER_CHANGE_ME', 'STRING', 'security', 'JWT 签名密钥(>=32字节,首次部署必须替换)', 0, 0),
('security.jwt.expire-seconds',   '7200',                   'NUMBER', 'security', 'Token 过期秒数(默认2小时)', 0, 0),
('security.rate-limit.login',     '5',                      'NUMBER', 'security', '每分钟每IP登录尝试次数', 0, 0),
('security.aes.master-key',       'PLACEHOLDER_CHANGE_ME', 'STRING', 'security', 'AES 主密钥(Base64,>=32字节)', 0, 0),
('security.sso.kick-prev',        'true',                   'BOOLEAN', 'security', '新登录是否踢出上一次会话', 0, 0),
('message.retry.max-attempts',    '3',                      'NUMBER', 'message', '渠道失败最大重试次数', 0, 0),
('message.retry.backoff-seconds', '60',                     'NUMBER', 'message', '重试退避基数(秒)', 0, 0),
('message.dead-letter.retention-days', '30',                'NUMBER', 'message', '死信保留天数', 0, 0),
('message.email.smtp-host',       'smtp.example.com',       'STRING', 'message', 'SMTP 主机', 0, 0),
('message.email.smtp-port',       '465',                    'NUMBER', 'message', 'SMTP 端口', 0, 0),
('message.sms.access-key-id',     'PLACEHOLDER',            'STRING', 'message', '阿里云短信 AccessKeyId', 0, 0),
('message.wechat.corp-id',        'PLACEHOLDER',            'STRING', 'message', '企业微信 CorpID', 0, 0);
```

### 3.4 索引与 EXPLAIN 验收

| 查询场景 | 索引 | 验收 |
|----------|------|------|
| 我的消息列表 | `idx_msg_msg_receiver_status` | `EXPLAIN` 命中,不出现全表扫描 |
| 我的未读消息 | `idx_msg_msg_receiver_read` | 按 receiver + read_flag 命中 |
| 重试扫描 | `idx_msg_channel_status_retry` | `status=0 AND next_retry_time<=NOW()` 命中 |
| 死信治理 | `idx_msg_dl_status_time` | `status=0 ORDER BY create_time` 命中 |
| 操作日志按人查 | `idx_sys_oplog_emp_time` | `WHERE emp_id=? AND create_time BETWEEN ?` 命中 |
| 字典数据按类型 | `uk_sys_dict_data_type_value` / `idx_sys_dict_data_type_sort` | `dict_type=?` 命中 |
| 数据权限按角色 | `idx_sys_ds_role` | `role_id=?` 命中 |
| 登录日志按账号 | `idx_sys_loginlog_username_time` | 命中 |

---

## 4. API 契约

### 4.1 消息中台 `/api/message`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/message/page` | `msg:message:list` | 分页查询我的消息 |
| `GET` | `/api/message/unread-count` | `msg:message:view` | 当前用户未读数 |
| `GET` | `/api/message/{id}` | `msg:message:view` | 消息详情 |
| `POST` | `/api/message/{id}/read` | `msg:message:read` | 标记已读 |
| `POST` | `/api/message/read-batch` | `msg:message:read` | 批量已读 |
| `POST` | `/api/message/read-all` | `msg:message:read` | 全部已读 |
| `DELETE` | `/api/message/{id}` | `msg:message:delete` | 删除(逻辑) |
| `GET` | `/api/message/preferences` | `msg:preference:view` | 查看我的偏好 |
| `PUT` | `/api/message/preferences` | `msg:preference:update` | 更新我的偏好 |
| `GET` | `/api/message/templates` | `msg:template:list` | 模板列表(管理) |
| `POST` | `/api/message/templates` | `msg:template:create` | 新增模板 |
| `PUT` | `/api/message/templates/{id}` | `msg:template:update` | 更新模板 |
| `DELETE` | `/api/message/templates/{id}` | `msg:template:delete` | 删除模板 |
| `GET` | `/api/message/dead-letters` | `msg:dead-letter:list` | 死信列表(管理) |
| `POST` | `/api/message/dead-letters/{id}/replay` | `msg:dead-letter:replay` | 死信重投 |
| `POST` | `/api/message/dead-letters/{id}/discard` | `msg:dead-letter:discard` | 死信丢弃 |
| `POST` | `/api/message/test-send` | `msg:message:send` | 测试发送(管理员) |

#### `MessageVO` 主要字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 消息ID |
| `templateCode` | String | 模板编码 |
| `msgType` / `msgTypeName` | String | 消息类型 + 名称 |
| `title` | String | 渲染后标题 |
| `content` | String | 渲染后内容 |
| `linkUrl` | String | 站内跳转 |
| `priority` | Integer | 0~3 |
| `status` | Integer | 0/1/2/3/4 |
| `readFlag` | Boolean | 已读 |
| `readTime` | LocalDateTime | 已读时间 |
| `sentTime` | LocalDateTime | 发送时间 |
| `bizId` | String | 业务ID |
| `bizType` | String | 业务类型 |
| `channels` | List\<String\> | 实际发送渠道列表 |
| `createTime` | LocalDateTime | 创建时间 |

#### `MessagePreferenceDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `msgType` | String | 必填 |
| `channels` | List\<String\> | 必填,非空 |
| `enabled` | Boolean | 必填 |
| `quietStart` / `quietEnd` | String(HH:mm) | 可选 |

#### `MessageTemplateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `templateCode` | String | 必填,^[A-Z0-9_]{4,64}$ |
| `templateName` | String | 必填 |
| `msgType` | String | 必填,枚举 |
| `defaultChannels` | List\<String\> | 必填 |
| `titleTemplate` | String | 必填,最大256 |
| `contentTemplate` | String | 必填,最大 64KB |
| `emailSubject` | String | 可选 |
| `smsTemplateId` | String | 可选 |
| `wechatTemplate` | String(JSON) | 可选 |
| `variablesSchema` | String(JSON) | 可选 |
| `enabled` | Boolean | 默认 true |
| `remark` | String | 可选 |

### 4.2 平台安全 `/api/auth` + `/api/system`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/auth/captcha` | 公开 | 获取图形验证码 |
| `POST` | `/api/auth/login` | 公开 | 账号密码登录 |
| `POST` | `/api/auth/refresh` | RefreshToken | 刷新访问令牌 |
| `POST` | `/api/auth/logout` | 登录用户 | 登出(失效 Redis Session) |
| `GET` | `/api/auth/online-devices` | `auth:device:list` | 当前账号在线设备 |
| `POST` | `/api/auth/kick-out/{sessionId}` | `auth:device:kick` | 踢出指定会话 |
| `GET` | `/api/system/dict-types` | `system:dict:list` | 字典类型分页 |
| `POST` | `/api/system/dict-types` | `system:dict:create` | 新增字典类型 |
| `PUT` | `/api/system/dict-types/{id}` | `system:dict:update` | 修改字典类型 |
| `DELETE` | `/api/system/dict-types/{id}` | `system:dict:delete` | 删除字典类型 |
| `GET` | `/api/system/dict-data` | `system:dict:list` | 按 type 查字典数据 |
| `POST` | `/api/system/dict-data` | `system:dict:create` | 新增字典数据 |
| `PUT` | `/api/system/dict-data/{id}` | `system:dict:update` | 修改字典数据 |
| `DELETE` | `/api/system/dict-data/{id}` | `system:dict:delete` | 删除字典数据 |
| `GET` | `/api/system/configs` | `system:config:list` | 配置分页 |
| `GET` | `/api/system/configs/{key}` | `system:config:view` | 按 key 查 |
| `PUT` | `/api/system/configs/{key}` | `system:config:update` | 更新配置 |
| `POST` | `/api/system/configs/refresh-cache` | `system:config:update` | 刷新 Redis 缓存 |
| `GET` | `/api/system/operation-logs` | `system:oplog:list` | 操作日志分页 |
| `GET` | `/api/system/login-logs` | `system:loginlog:list` | 登录日志分页 |
| `GET` | `/api/system/data-scopes` | `system:datascope:list` | 数据权限规则 |
| `POST` | `/api/system/data-scopes` | `system:datascope:create` | 新增规则 |
| `PUT` | `/api/system/data-scopes/{id}` | `system:datascope:update` | 修改规则 |
| `DELETE` | `/api/system/data-scopes/{id}` | `system:datascope:delete` | 删除规则 |

#### `LoginRequest`

| 字段 | 类型 | 校验 |
|------|------|------|
| `username` | String | 必填,4-64 |
| `password` | String | 必填,8-64 |
| `captchaKey` | String | 必填 |
| `captchaCode` | String | 必填 |
| `deviceId` | String | 可选,用于多端互踢 |

#### `LoginResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | String | 短期访问令牌 |
| `refreshToken` | String | 长期刷新令牌(可存 Redis) |
| `expiresIn` | Integer | 过期秒数 |
| `tokenType` | String | 固定 Bearer |
| `empId` | Long | 当前用户 |
| `empName` | String | 姓名 |
| `roles` | List\<String\> | 角色编码 |
| `permissions` | List\<String\> | 权限码 |
| `deptId` | Long | 主部门 |
| `dataScope` | String | 命中数据权限范围 |

#### `CaptchaResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| `captchaKey` | String | Redis 中的 key,5 分钟过期 |
| `captchaImage` | String | base64 PNG |

#### `OperationLogQueryDTO` / `LoginLogQueryDTO`

通用分页 + 关键字 + 时间区间;按 `empId`、`status`、`module`、`username`、`ip` 过滤。

#### `DataScopeDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `scopeName` | String | 必填 |
| `scopeCode` | String | 必填,^[a-z][a-z0-9_:]{2,63}$ |
| `roleId` | Long | 二选一 |
| `empId` | Long | 二选一 |
| `scopeType` | String | 必填,枚举 |
| `deptIds` | List\<Long\> | scopeType=CUSTOM 时必填 |
| `includeSelf` | Boolean | 默认 true |
| `enabled` | Boolean | 默认 true |
| `priority` | Integer | 默认 0 |

### 4.3 错误码

| 错误码 | 含义 |
|--------|------|
| `0` | 成功 |
| `-1` | 业务异常(配合 message) |
| `401` | 未登录 / Token 失效 |
| `403` | 无权限 |
| `429` | 限流命中 |
| `1001` | 验证码错误或已过期 |
| `1002` | 账号或密码错误 |
| `1003` | 账号被锁定 |
| `1004` | 账号被禁用 |
| `2001` | 模板不存在 |
| `2002` | 模板渲染失败(变量缺失) |
| `2003` | 渠道全部失败,已转死信 |
| `2004` | 偏好设置非法 |
| `3001` | AES 加密失败 |
| `3002` | AES 解密失败(密钥不匹配) |
| `3003` | 数据权限规则冲突 |
| `3010` | JWT 解析失败 |
| `3011` | Refresh Token 无效 |

---

## 5. 任务波次

### Wave 1: 消息基础表与种子

#### T1 消息库表 DDL 与种子

| 字段 | 内容 |
|------|------|
| 目标 | 落地消息 5 张表 DDL、索引、种子模板/字典/配置 |
| 路径 | `code/backend/sql/message_contract.sql`、`code/backend/sql/message_seed.sql` |
| 输入 | 重构文档第四章(消息通知中台)、`oa_message`、`oa_todo` 旧实现 |
| 输出 | 可执行 DDL、种子 SQL、索引说明、EXPLAIN 验收脚本 |
| 禁止修改 | 不实现 Service/Controller/业务 |
| 验收 | MySQL 8.0 顺序执行无报错;`EXPLAIN` 命中设计索引 |

#### T2 旧消息实现影响分析

| 字段 | 内容 |
|------|------|
| 目标 | 盘点旧 `oa_message`、`oa_todo`、`NotificationService`、`NotificationEndpoint` 的接口、字段、依赖 |
| 路径 | `code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、前端 `src/api/message.ts`、`src/api/todo.ts`、Mobile `src/api/message.ts` |
| 输出 | 旧入口清单、新旧映射、保留/替换/下线建议 |
| 禁止修改 | 不删除旧代码 |
| 验收 | 文档记录旧文件、新接口、切换方式、回滚方式 |

### Wave 2: 渠道适配器与模板渲染

#### T3 消息 Entity / Mapper / Enum

| 字段 | 内容 |
|------|------|
| 目标 | 在 `oa-message` 中建立 `MsgTemplate`、`MsgUserPreference`、`MsgMessage`、`MsgChannelLog`、`MsgDeadLetter` Entity/Mapper |
| 路径 | `code/backend/oa-message/src/main/java/cn/oa/message/` |
| 输出 | Entity、Mapper、Enum(`MsgType`、`MsgChannel`、`MsgStatus`、`MessagePriority`)、DTO/VO |
| 禁止修改 | 不实现业务 Service、不动旧 oa-message |
| 验收 | `cd code/backend && mvn -pl oa-message -am test` |

#### T4 消息 Service + 渠道分发 + 模板渲染

| 字段 | 内容 |
|------|------|
| 目标 | `MessageNotificationService.notify()`、模板变量渲染、`ChannelRouter` 按用户偏好派发到 4 个适配器(SITE/EMAIL/SMS/WECHAT) |
| 路径 | `code/backend/oa-message` |
| 输出 | `IMessageNotificationService`、`TemplateEngine`(占位符替换)、`ChannelRouter`、4 个 `MessageChannel` 实现 |
| 禁止修改 | 不动旧 `oa-message` Service、不实现重试调度器(Wave 5) |
| 验收 | 单元测试覆盖渲染失败、偏好禁用、多渠道并发;`mvn -pl oa-message -am test` |

### Wave 3: 偏好与模板管理

#### T5 偏好与模板管理 Service

| 字段 | 内容 |
|------|------|
| 目标 | `MsgPreferenceService`(CRUD、批量更新、免打扰判断)、`MsgTemplateService`(CRUD、版本号) |
| 路径 | `code/backend/oa-message` |
| 输出 | Service 接口/实现、DTO/VO、Redis 缓存(`msg:pref:{empId}` TTL 30min) |
| 禁止修改 | 不动 Controller |
| 验收 | Service 单测覆盖免打扰区间、偏好不存在时回退默认渠道 |

### Wave 4: 事件订阅统一入口

#### T6 领域事件订阅器

| 字段 | 内容 |
|------|------|
| 目标 | 业务模块发布 `DomainEvent`,消息中台订阅并自动派发 |
| 路径 | `code/backend/oa-message`、`code/backend/oa-common`(事件总线抽象) |
| 输出 | `DomainEventPublisher`、`MessageEventSubscriber`、示例事件(请假通过、待办分配、合同到期) |
| 禁止修改 | 不动 HR/Finance/Admin 业务模块,只定义事件契约 |
| 验收 | 单元测试模拟业务模块发布事件,验证消息生成和渠道分发 |

### Wave 5: 重试与死信

#### T7 重试调度器 + 死信治理

| 字段 | 内容 |
|------|------|
| 目标 | 失败渠道按指数退避重试,达到上限入死信;死信支持列表/重投/丢弃 |
| 路径 | `code/backend/oa-message` |
| 输出 | `RetryScheduler`(@Scheduled)、`DeadLetterService`、`MessageChannelLogService` |
| 禁止修改 | 不修改 Wave 4 已完成的事件订阅 |
| 验收 | 单测模拟连续失败入死信、重投成功后状态翻转、丢弃软删 |

#### T8 消息 REST API

| 字段 | 内容 |
|------|------|
| 目标 | 暴露消息/偏好/模板/死信 REST API,对齐 `/api/message/*` 契约 |
| 路径 | `code/backend/oa-message` |
| 输出 | Controller、Knife4j 注解、权限注解、Controller 测试 |
| 禁止修改 | 旧 `oa_message` Controller 兼容期保留 |
| 验收 | `mvn -pl oa-message,oa-web -am test` |

### Wave 6: 平台安全 — JWT + 限流

#### T9 平台库表 DDL + 种子 + Entity/Mapper

| 字段 | 内容 |
|------|------|
| 目标 | 落地平台 6 张表 DDL 与种子;Entity/Mapper 基础结构 |
| 路径 | `code/backend/sql/platform_contract.sql`、`code/backend/oa-platform` |
| 输出 | 6 张表 SQL、Entity、Mapper、Enum(`DataScopeType`、`LoginType`、`OperationResult`、`SensitiveField`) |
| 禁止修改 | 不实现 Service/Interceptor |
| 验收 | `mvn -pl oa-platform -am test` |

#### T10 JWT 认证 + Redis Session + 多端互踢 + 限流

| 字段 | 内容 |
|------|------|
| 目标 | 改造 `AuthInterceptor`,接入 Redis Session(`token:{empId}`),支持 Refresh Token、滑动过期、SSO 互踢;新增 `RateLimitInterceptor`(登录 5 次/分钟/IP) |
| 路径 | `code/backend/oa-common`、`code/backend/oa-platform`、`code/backend/oa-web` |
| 输出 | `JwtTokenProvider`、`TokenSessionService`、`AuthInterceptor` 升级、`RateLimitInterceptor` |
| 禁止修改 | 不动业务 Controller |
| 验收 | 单测覆盖签发/解析/过期/刷新/互踢/限流;集成测试验证登录 → 刷新 → 踢出 |

#### T11 Auth REST API + 登录日志

| 字段 | 内容 |
|------|------|
| 目标 | `/api/auth/captcha`、`/login`、`/refresh`、`/logout`、`/online-devices`、`/kick-out/{sessionId}`;登录成功/失败写 `sys_login_log` |
| 路径 | `code/backend/oa-platform`、`code/backend/oa-web` |
| 输出 | Controller、Service、DTO/VO、Knife4j 注解、Controller 测试 |
| 禁止修改 | 旧 `/api/auth/login` 兼容期保留 |
| 验收 | Controller 测试 + 集成测试 |

### Wave 7: 数据权限 + AES 加密

#### T12 DataPermissionInterceptor + AES 工具

| 字段 | 内容 |
|------|------|
| 目标 | MyBatis `Interceptor` 拦截查询,根据 `UserContext` 自动追加 `dept_id IN (...)` 条件;`AesGcmCrypto` 工具,支持 `@EncryptedField` 注解在 MyBatis-Plus `TypeHandler` 加密/解密敏感字段 |
| 路径 | `code/backend/oa-platform`、`code/backend/oa-common` |
| 输出 | `DataPermissionInterceptor`、`DataScopeResolver`、`AesGcmCrypto`、`EncryptedFieldTypeHandler`、`@EncryptedField` 注解 |
| 禁止修改 | 不重写已有 Mapper SQL,只新增拦截与 TypeHandler |
| 验收 | 单测:不同 scopeType 下生成的 SQL 片段;加密 roundtrip;密钥轮转;展示脱敏 |

### Wave 8: 字典/配置中心

#### T13 DictService + ConfigService

| 字段 | 内容 |
|------|------|
| 目标 | 字典类型/数据 CRUD + Redis 缓存(`sys:dict:{type}`);配置项 CRUD + 加密字段落库 + 缓存(`sys:cfg:{key}`);刷新缓存接口 |
| 路径 | `code/backend/oa-platform` |
| 输出 | `DictTypeService`、`DictDataService`、`ConfigService`、`DictCache`、`ConfigCache` |
| 禁止修改 | 旧字典/配置实现不删除 |
| 验收 | 单测覆盖缓存命中/失效、加密配置回显、批量查询 |

#### T14 Dict/Config REST API

| 字段 | 内容 |
|------|------|
| 目标 | `/api/system/dict-types`、`/api/system/dict-data`、`/api/system/configs` 全套接口 |
| 路径 | `code/backend/oa-platform` |
| 输出 | Controller、Knife4j 注解、Controller 测试 |
| 验收 | `mvn -pl oa-platform,oa-web -am test` |

### Wave 9: 审计日志 + 数据权限规则 API

#### T15 OperationLog AOP + LoginLog Service + 数据权限规则 API

| 字段 | 内容 |
|------|------|
| 目标 | `@OperationLog` 注解 + AOP 自动写 `sys_operation_log`;`LoginLogService` 写登录日志;`/api/system/operation-logs`、`/api/system/login-logs`、`/api/system/data-scopes` 接口 |
| 路径 | `code/backend/oa-platform`、`code/backend/oa-common` |
| 输出 | `OperationLogAspect`、`OperationLogService`、`LoginLogService`、`DataScopeService` + 三个 Controller |
| 禁止修改 | 旧审计实现不删除 |
| 验收 | 单测覆盖敏感参数脱敏、异常路径记录、IP 解析;Controller 测试 |

### Wave 10: Web + Mobile 演示

#### T16 Web 端消息中心 + 偏好 + 系统管理

| 字段 | 内容 |
|------|------|
| 目标 | 消息中心(收件箱/已读/未读/未读数/跳转)、偏好设置页、字典管理、配置管理、操作日志、登录日志、数据权限规则 |
| 路径 | `code/frontend/src/api/message*.ts`、`src/api/system*.ts`、`src/api/auth.ts`、`src/views/oa/message`、`src/views/system/*` |
| 输出 | typed API、消息铃铛未读数 Store、字典 Composable、系统管理页 |
| 禁止修改 | 不动其他业务模块 |
| 验收 | `pnpm typecheck && pnpm build` |

#### T17 Mobile 端消息中心

| 字段 | 内容 |
|------|------|
| 目标 | 移动端消息中心、已读/未读、未读数 badge |
| 路径 | `code/mobile/src/api/message.ts`、`code/mobile/src/pages/message/*` |
| 输出 | typed API、消息列表页、消息详情页、tabBar badge |
| 禁止修改 | 不动其他业务模块 |
| 验收 | `pnpm build:h5` |

#### T18 端到端演示与文档

| 字段 | 内容 |
|------|------|
| 目标 | 演示脚本:登录 → 触发业务事件 → 收到通知 → 偏好生效 → 重试入死信 → 管理后台处理 |
| 路径 | `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` |
| 输出 | 演示手册(步骤、截图位、关键日志) |
| 禁止修改 | 演示不动正式 SQL baseline |
| 验收 | 一遍跑通 |

### Wave 11: 验收与下线准备

#### T19 端到端回归

| 字段 | 内容 |
|------|------|
| 目标 | 验证登录/权限/数据隔离/加密/字典/消息/重试 完整链路 |
| 路径 | `tests/integration`、`code/backend/src/test` |
| 输出 | API 集成测试 + E2E 测试 |
| 禁止修改 | 不扩大其他业务模块 |
| 验收 | 全部通过;`mvn -pl oa-message,oa-platform,oa-web -am test`、`pnpm typecheck && pnpm build`、`pnpm build:h5` |

#### T20 旧入口下线清单

| 字段 | 内容 |
|------|------|
| 目标 | 标记旧 `/api/message/*`、`/api/auth/login`、`/oa_message` 表的替换关系和下线时机 |
| 路径 | `docs/superpowers/specs/` |
| 输出 | 下线清单、兼容策略、风险说明 |
| 禁止修改 | 未通过 E2E 前不删除旧代码 |
| 验收 | 清单覆盖旧路径、新路径、切换条件、回滚方式 |

---

## 6. 推荐执行顺序

```
Wave 1:  T1 + T2
Wave 2:  T3 -> T4
Wave 3:  T5
Wave 4:  T6
Wave 5:  T7 -> T8
Wave 6:  T9 -> T10 -> T11
Wave 7:  T12
Wave 8:  T13 -> T14
Wave 9:  T15
Wave 10: T16 与 T17 并行 -> T18
Wave 11: T19 -> T20
```

T1/T2/T9 完成前不得开始业务实现。T8/T11 完成后,Web/Mobile 可以接入;但 T15 之前不要宣称审计闭环完成。

---

## 7. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| 消息后端 | `cd code/backend && mvn -pl oa-message -am test` |
| 平台后端 | `cd code/backend && mvn -pl oa-platform -am test` |
| 集成 | `cd code/backend && mvn -pl oa-message,oa-platform,oa-web -am test` |
| Web | `cd code/frontend && pnpm typecheck && pnpm build` |
| Mobile | `cd code/mobile && pnpm build:h5` |
| 文档 DDL | `mysql -uroot -p oa_system < code/backend/sql/message_contract.sql` |
| 文档 DDL | `mysql -uroot -p oa_system < code/backend/sql/platform_contract.sql` |

---

## 8. 第一个可执行任务提示词

```text
请执行 消息通知中台 + 平台安全 重构 T1:消息库表 DDL 与种子。

必须先阅读:
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第四章
- docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md
- docs/superpowers/workflows/claude-code-oa-redesign-workflow.md

范围:
- 只允许新建/修改 SQL 草案文件。
- 不实现 Java/Vue/uni-app 业务代码。
- 不直接替换 code/backend/sql/oa_system_full.sql 或 oa_system_extensions.sql。

输出:
- code/backend/sql/message_contract.sql (5 张消息表 + 索引)
- code/backend/sql/message_seed.sql (种子模板/字典/配置)
- 索引和 EXPLAIN 验收说明
- 后续 T3 需要的 DTO/VO/Enum 字段清单

完成后汇报改动文件、T3 注意事项。
```

---

## 9. T1 数据库与 API 契约结果

### 9.1 旧实现摘要

| 项目 | 旧实现 |
|------|--------|
| 旧消息主表 | `oa_message` |
| 旧站内信/待办 | `oa_todo` |
| 旧后端入口 | `code/backend/oa-web/src/main/java/cn/oa/controller/MessageController.java` |
| 旧服务 | `MessageServiceImpl`、`TodoServiceImpl`、`NotificationServiceImpl` |
| 旧 WebSocket | `code/backend/oa-web/src/main/java/cn/oa/websocket/NotificationEndpoint.java` |
| 旧 Web API | `code/frontend/src/api/message.ts`、`src/api/todo.ts` |
| 旧 Mobile API | `code/mobile/src/api/message.ts`、`src/api/todo.ts` |

旧逻辑需要保留的能力:

1. 站内信推送(WebSocket `/ws/notification?empId=...`)。
2. 公告/审批/待办三类消息的写入与未读数累加。
3. 多端订阅:同一 `empId` 在 PC、Web、移动端都收到推送。
4. 待办与消息的合并查询入口。
5. 简单的失败重发(基于轮询)。

### 9.2 SQL 草案

新增 SQL 草案文件:

- `code/backend/sql/message_contract.sql`
- `code/backend/sql/platform_contract.sql`
- `code/backend/sql/message_seed.sql`

`message_contract.sql` 包含:

| 表 | 说明 |
|----|------|
| `msg_template` | 消息模板,支持变量替换 |
| `msg_user_preference` | 用户偏好与免打扰 |
| `msg_message` | 消息主表(接收人维度的实际消息) |
| `msg_channel_log` | 渠道分发日志(一次消息多条) |
| `msg_dead_letter` | 死信队列(超过重试上限) |

### 9.3 消息主表核心字段

#### `msg_message`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `biz_id` | VARCHAR(64) | 业务ID(如流程实例) |
| `biz_type` | VARCHAR(32) | 业务类型 |
| `template_code` | VARCHAR(64) | 模板编码 |
| `msg_type` | VARCHAR(32) | 消息类型 |
| `receiver_id` | BIGINT | 接收员工 |
| `title` | VARCHAR(256) | 渲染后标题 |
| `content` | MEDIUMTEXT | 渲染后内容 |
| `link_url` | VARCHAR(512) | 站内跳转 |
| `priority` | TINYINT | 0/1/2/3 |
| `status` | TINYINT | 0/1/2/3/4 |
| `read_flag` | TINYINT | 0/1 |
| `read_time` | DATETIME | 已读时间 |
| `scheduled_time` | DATETIME | 定时发送 |
| `sent_time` | DATETIME | 实际发送 |

#### 消息状态枚举

| code | 名称 | 说明 |
|------|------|------|
| `0` | UNSENT | 未发送 |
| `1` | SENDING | 发送中 |
| `2` | SENT | 已发送(全部成功) |
| `3` | PARTIAL | 部分成功 |
| `4` | FAILED | 失败(全部失败) |

#### 消息渠道枚举

| code | 名称 | 说明 |
|------|------|------|
| `SITE` | 站内信 | WebSocket |
| `EMAIL` | 邮件 | SMTP |
| `SMS` | 短信 | 阿里云 |
| `WECHAT` | 企业微信 | 机器人/应用消息 |

#### 渠道日志状态

| code | 名称 |
|------|------|
| `0` | PENDING |
| `1` | SUCCESS |
| `2` | FAILED |

#### 死信状态

| code | 名称 |
|------|------|
| `0` | PENDING |
| `1` | REPLAYED |
| `2` | DISCARDED |

### 9.4 消息类型与默认渠道

| 消息类型 | 默认渠道 | 失败后回退 | 备注 |
|----------|----------|------------|------|
| `TODO_ASSIGN` | SITE,WECHAT | 邮件 | 待办分配 |
| `TODO_URGE` | SITE,SMS | 邮件 | 催办 |
| `APPROVAL_PASS` | SITE | 邮件 | 审批通过 |
| `APPROVAL_REJECT` | SITE,SMS | 邮件 | 审批驳回 |
| `NOTICE_PUBLISH` | SITE,EMAIL | - | 公告 |
| `MEETING_REMIND` | SITE,SMS,WECHAT | 邮件 | 会议提醒 |
| `SYSTEM_ALERT` | SITE,EMAIL,SMS | - | 系统告警 |

### 9.5 后续任务输入

T3/T4 实现时必须使用以上契约,不再沿用旧 `oa_message` 字段:

| 旧项 | 新项 |
|------|------|
| `oa_message` | `msg_message` |
| `oa_message.title` VARCHAR(128) | `msg_message.title` VARCHAR(256) |
| 数字渠道(1/2/3) | 字符串渠道(SITE/EMAIL/SMS/WECHAT) |
| `oa_message.status` (0/1) | `msg_message.status` (0~4) |
| 无 `biz_id` / `biz_type` | 新增业务关联 |
| 无模板 | `msg_template` + `template_code` |
| 无偏好 | `msg_user_preference` |
| 无渠道日志 | `msg_channel_log` |
| 无死信 | `msg_dead_letter` |
| 无重试 | 渠道级 retry_count + next_retry_time |

兼容期前端可保留旧 API,但新页面必须优先使用 `/api/message/*`。

---

## 10. T2 旧实现影响分析

### 10.1 旧后端文件清单

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Entity | `code/backend/oa-model/src/main/java/cn/oa/entity/OaMessage.java` | 站内信主表 | 迁移字段语义到 `MsgMessage` |
| Entity | `code/backend/oa-model/src/main/java/cn/oa/entity/OaTodo.java` | 待办主表 | 保留为待办独立能力,本期不动 |
| Mapper | `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaMessageMapper.java` | 站内信 CRUD | 迁移为 `MsgMessageMapper` |
| Service | `code/backend/oa-service/src/main/java/cn/oa/service/MessageService.java` | 发送/查询/已读 | 迁移为 `IMessageNotificationService` |
| Service | `code/backend/oa-service/src/main/java/cn/oa/service/NotificationService.java` | WebSocket 推送 | 保留为 `SiteMessageChannel` 实现 |
| Controller | `code/backend/oa-web/src/main/java/cn/oa/controller/MessageController.java` | `/api/message/*` | 旧版保留;新增 `/api/message/*` 新版 |
| WebSocket | `code/backend/oa-web/src/main/java/cn/oa/websocket/NotificationEndpoint.java` | `/ws/notification` | 保留,新 SITE 渠道复用 |

### 10.2 旧前端与移动端文件清单

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Web API | `code/frontend/src/api/message.ts` | 旧站内信 API | 后续 T16 改为新路径 |
| Web API | `code/frontend/src/api/todo.ts` | 待办 API | 保留,与消息分离 |
| Web 页面 | `code/frontend/src/views/oa/message/index.vue` | 收件箱 | T16 重做 |
| Web 组件 | `code/frontend/src/layout/components/notify/index.vue` | 铃铛未读 | T16 接入新未读数 |
| Mobile API | `code/mobile/src/api/message.ts` | 旧站内信 API | T17 改新路径 |
| Mobile 页面 | `code/mobile/src/pages/message/*` | 消息中心 | T17 改模板 |

### 10.3 新旧接口映射

| 旧接口 | 新接口 | 说明 |
|--------|--------|------|
| `GET /api/message/page` | `GET /api/message/page` | 兼容路径;新实现返回 VO |
| `GET /api/message/unread` | `GET /api/message/unread-count` | 字段更名 |
| `POST /api/message/{id}/read` | `POST /api/message/{id}/read` | 兼容 |
| 无 | `POST /api/message/read-batch` | 新增 |
| 无 | `POST /api/message/read-all` | 新增 |
| 无 | `GET /api/message/preferences` | 新增 |
| 无 | `PUT /api/message/preferences` | 新增 |
| 无 | `GET /api/message/templates` | 新增(管理) |
| 无 | `GET /api/message/dead-letters` | 新增(管理) |

### 10.4 风险点

| 风险 | 影响 | 缓解 |
|------|------|------|
| 旧 `oa_message` 与新 `msg_message` 字段不同 | 兼容期数据未迁移 | 新表从空开始,只读旧数据用于回查 |
| 渠道全部失败 | 用户无感知 | 退避重试 + 死信,管理后台补发 |
| 模板渲染失败 | 消息不发送 | 渲染异常落入 `msg_message.status=FAILED` + 死信 |
| WebSocket 断线 | 漏推 | 客户端重连后调 `/api/message/unread-count` 拉增量 |
| 旧接口调用方 | 切换窗口期共存 | URL 路径相同、字段兼容 |

### 10.5 回滚方式

| 回滚场景 | 操作 |
|----------|------|
| T3/T4 消息基础失败 | 停止引用 `oa-message` 新接口,旧 `MessageController` 不受影响 |
| T7 重试调度异常 | 关闭 `@Scheduled` 任务,渠道失败直接走死信 |
| T10 JWT 升级失败 | `AuthInterceptor` 保留旧路径兼容,新接口降级为旧解析 |
| T12 DataScope 拦截过严 | 临时关闭拦截器 Bean,保留能力代码 |
| 模板渲染异常 | 模板引擎 fallback 到原始 `title/content` 字段 |
| DDL 草案不通过 | 不合并到 baseline,继续使用旧表 |

---

## 11. T3 Claude Code 任务单:消息 Entity + Mapper

### 11.1 任务目标

在 `oa-message` 模块内建立消息通知中台所需的 Entity、Enum、Mapper 基础结构,对齐 `message_contract.sql`,但不实现业务 Service 和 Controller。

### 11.2 必须先阅读

```text
CLAUDE.md
docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第四章
docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md
code/backend/sql/message_contract.sql
code/backend/sql/message_seed.sql
code/backend/oa-message/pom.xml
code/backend/oa-model/src/main/java/cn/oa/entity/OaMessage.java
code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaMessageMapper.java
```

### 11.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-message/src/main/java/**` | 新增 Entity、Enum、Mapper、DTO、VO |
| `code/backend/oa-message/src/test/java/**` | 新增 Mapper/枚举测试 |
| `code/backend/oa-message/pom.xml` | 仅在缺少必要依赖时修改 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 11.4 禁止修改

```text
code/backend/oa-service/**
code/backend/oa-web/src/main/java/cn/oa/controller/MessageController.java
code/backend/oa-web/src/main/java/cn/oa/websocket/NotificationEndpoint.java
code/frontend/**
code/mobile/**
code/backend/sql/oa_system_full.sql
code/backend/sql/oa_system_extensions.sql
```

### 11.5 产出物

建议类名:

| 类型 | 建议名称 |
|------|----------|
| Entity | `MsgTemplate`、`MsgUserPreference`、`MsgMessage`、`MsgChannelLog`、`MsgDeadLetter` |
| Enum | `MsgType`、`MsgChannel`、`MsgStatus`、`MessagePriority`、`DeadLetterStatus`、`ChannelLogStatus` |
| DTO | `MessageNotifyDTO`、`MessagePreferenceDTO`、`MessageTemplateDTO`、`MessageQueryDTO`、`DeadLetterHandleDTO` |
| VO | `MessageVO`、`MessagePreferenceVO`、`MessageTemplateVO`、`DeadLetterVO`、`ChannelLogVO` |
| Mapper | `MsgTemplateMapper`、`MsgUserPreferenceMapper`、`MsgMessageMapper`、`MsgChannelLogMapper`、`MsgDeadLetterMapper` |

### 11.6 完成标准

1. Entity 字段和表字段完整对应,含审计字段。
2. 枚举覆盖 7 种 MsgType、4 种 Channel、5 种 Status、4 档 Priority。
3. DTO 包含基本 Jakarta Validation 注解。
4. Mapper 使用 MyBatis-Plus `BaseMapper`。
5. 不引入业务逻辑。
6. 不删除旧 `oa_message` 实现。

### 11.7 验收命令

```bash
cd code/backend
mvn -pl oa-message -am test
```

### 11.8 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T3:消息 Entity + Mapper。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 11 章。

只允许新增/修改 oa-message 模块内的 Entity、DTO、VO、Enum、Mapper 和必要测试。
禁止修改旧 oa-service、旧 oa-web Controller、WebSocket Endpoint、frontend、mobile、正式 SQL baseline。

完成后运行:
cd code/backend
mvn -pl oa-message -am test

最终汇报:
- 新增/修改文件清单
- 是否发现已有重复消息模型
- 验收命令结果
- T4 需要注意的问题
```

---

## 12. T4 Claude Code 任务单:消息 Service + 渠道分发

### 12.1 任务目标

在 `oa-message` 模块内实现 `IMessageNotificationService`、模板变量渲染、4 个 `MessageChannel` 适配器(SITE/EMAIL/SMS/WECHAT)、`ChannelRouter`,但不实现重试调度器、不实现 REST Controller。

### 12.2 必须先阅读

```text
T1/T2/T3 结果
code/backend/sql/message_contract.sql
code/backend/sql/message_seed.sql
旧 NotificationServiceImpl
旧 NotificationEndpoint
重构文档第四章 4.1-4.3
```

### 12.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-message/src/main/java/**` | 新增 Service/ServiceImpl、模板引擎、渠道适配器、事件订阅 |
| `code/backend/oa-message/src/test/java/**` | 新增 Service/渠道单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 12.4 禁止修改

```text
code/backend/oa-web/**
code/backend/oa-service/**
code/frontend/**
code/mobile/**
旧 NotificationEndpoint.java(只读,作为 SITE 实现参考)
正式 SQL baseline
```

### 12.5 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `notify(NotifyRequest)` | 统一入口,负责模板查询、变量渲染、生成 `msg_message` 记录、派发到渠道 |
| `renderTemplate` | 支持 `${var}` 替换、缺失变量抛出 `2002` 错误码、落 `msg_message` 的 `title`/`content` |
| `ChannelRouter` | 根据 `msg_user_preference` + 默认渠道 + 免打扰时间,计算实际发送渠道列表 |
| `SiteMessageChannel` | 复用旧 `NotificationEndpoint` 推 WebSocket;离线时落站内信表待查 |
| `EmailMessageChannel` | Hutool `Mail` + SMTP,从 `sys_config` 读主机/账号/密码 |
| `SmsMessageChannel` | 阿里云 SDK 封装,`sms_template_id` + 手机号 + 变量 |
| `WechatMessageChannel` | 企业微信机器人/应用消息,CorpID + AgentID + Secret |
| `markAsRead` / `markBatchRead` / `markAllRead` | 维护 `read_flag`/`read_time` |
| `unreadCount` | 缓存 5s,防刷 |
| `pageQuery` | 按 `receiver_id` + `status` + `msg_type` 过滤,索引命中 |
| `getDetail` | 校验当前用户为 receiver |

### 12.6 并发与幂等要求

1. `notify()` 必须先写 `msg_message` 拿到 `id`,再分发;分发成功后才更新 `status=2`,否则保持 `0/1`。
2. 同一 `message_id` 同一渠道不重复发送(幂等键 `message_id+channel`)。
3. 渠道适配器抛出后必须被 `ChannelRouter` 捕获,记录 `msg_channel_log` 失败状态,不中断其他渠道。
4. `markAsRead` 检查当前用户是 `receiver_id` 才能标记。

### 12.7 测试要求

至少覆盖:

| 测试 | 场景 |
|------|------|
| 模板渲染 | 正常替换、变量缺失抛错 |
| 渠道派发 | 默认渠道生效、用户偏好覆盖、免打扰区间过滤 |
| 多渠道并发 | SITE+EMAIL 并发,失败不影响其他 |
| 已读 | 标记已读、未读数下降、重复幂等 |
| 详情权限 | 非接收人不可查 |
| 渠道失败 | SITE 失败时 EMAIL 继续,落日志 |

### 12.8 验收命令

```bash
cd code/backend
mvn -pl oa-message -am test
```

### 12.9 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T4:消息 Service + 渠道分发。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 12 章。

前置条件:
- T3 Entity/Mapper 已完成。
- 模板与字典/配置种子已就绪。

实现重点:
- 统一 notify() 入口
- 模板变量渲染
- ChannelRouter 派发
- 4 个渠道适配器(SITE/EMAIL/SMS/WECHAT)
- Service 单元测试

禁止:
- 不实现 @Scheduled 重试(Wave 5)
- 不实现 REST Controller
- 不修改 frontend/mobile/旧 oa-service

完成后运行:
cd code/backend
mvn -pl oa-message -am test

最终汇报:
- 新增/修改文件
- 4 个渠道实现方式(关键类名)
- 模板渲染失败的处理路径
- 测试覆盖场景
- T5/T6/T7 注意事项
```

---

## 13. T5 Claude Code 任务单:偏好与模板管理 Service

### 13.1 任务目标

在 `oa-message` 模块内实现 `MsgPreferenceService` 和 `MsgTemplateService` 的 CRUD、缓存与免打扰判断。

### 13.2 必须先阅读

```text
T1-T4 结果
code/backend/sql/message_contract.sql
重构文档第四章 4.2
```

### 13.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-message/src/main/java/**` | 新增 Service/ServiceImpl、Redis 缓存组件 |
| `code/backend/oa-message/src/test/java/**` | 新增 Service 单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 13.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
旧 MessageController
正式 SQL baseline
```

### 13.5 必须实现的服务能力

| Service | 方法 |
|---------|------|
| `MsgPreferenceService` | `getByEmpAndType`、`getAllByEmp`、`upsert`、`batchUpdate`、`isInQuietPeriod`、`evictCache` |
| `MsgTemplateService` | `getByCode`、`pageQuery`、`create`、`update`、`delete`、`evictCache` |

### 13.6 缓存设计

| Key | Value | TTL |
|-----|-------|-----|
| `msg:pref:{empId}` | Map<msgType, Preference> | 30min |
| `msg:tpl:{code}` | Template | 30min |
| `msg:unread:{empId}` | count | 5s(防刷) |

### 13.7 验收命令

```bash
cd code/backend
mvn -pl oa-message -am test
```

### 13.8 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T5:偏好与模板管理 Service。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 13 章。

前置:
- T3/T4 已完成
- DDL 与种子已就绪

实现重点:
- 偏好 upsert + 免打扰判断
- 模板 CRUD + 版本号
- Redis 缓存与失效

完成后运行:
cd code/backend
mvn -pl oa-message -am test

最终汇报:
- 新增/修改文件
- 缓存 Key 清单
- 测试覆盖
- T6 事件订阅前置条件
```

---

## 14. T6 Claude Code 任务单:领域事件订阅统一入口

### 14.1 任务目标

在 `oa-common` 定义轻量事件总线,在 `oa-message` 订阅 `DomainEvent` 并自动派发到 `IMessageNotificationService`,为 HR/Finance/Admin 业务模块提供统一通知入口。

### 14.2 必须先阅读

```text
T1-T5 结果
重构文档第四章
工作流设计章节(已有事件概念)
```

### 14.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-common/src/main/java/cn/oa/common/event/**` | 新增 `DomainEvent`、`DomainEventPublisher`、`DomainEventSubscriber` 接口 |
| `code/backend/oa-message/src/main/java/cn/oa/message/event/**` | 新增 `MessageEventSubscriber` 抽象 + 若干示例实现 |
| `code/backend/oa-message/src/test/java/**` | 事件订阅单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 14.4 禁止修改

```text
code/backend/oa-hr/**
code/backend/oa-finance/**
code/backend/oa-admin/**
code/frontend/**
code/mobile/**
旧 oa_message
```

### 14.5 必须实现

| 组件 | 要求 |
|------|------|
| `DomainEvent` | 抽象类,含 `eventId`、`eventType`、`source`、`payload`、时间戳 |
| `DomainEventPublisher` | `publish(event)` 同步发布 + Spring `ApplicationEventPublisher` 包装 |
| `DomainEventSubscriber` | 接口 `onEvent(event)` + `supportedType()` |
| `MessageEventSubscriber<T extends DomainEvent>` | 抽象基类,负责 `event -> NotifyRequest` 转换 |
| 示例订阅器 | `LeaveApprovedSubscriber`、`TodoAssignedSubscriber`、`ContractExpiringSubscriber` |
| 注册机制 | `MessageEventSubscriberRegistry` 自动收集 `MessageEventSubscriber` Bean |

### 14.6 验收命令

```bash
cd code/backend
mvn -pl oa-common,oa-message -am test
```

### 14.7 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T6:领域事件订阅统一入口。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 14 章。

实现重点:
- DomainEvent / DomainEventPublisher 抽象
- MessageEventSubscriber 抽象基类
- 3 个示例订阅器(请假/待办/合同)
- 注册中心
- 单测覆盖事件 → 通知的转换

禁止:
- 不动 HR/Finance/Admin 业务模块
- 不实现 Controller

完成后运行:
cd code/backend
mvn -pl oa-common,oa-message -am test

最终汇报:
- 事件契约
- 订阅器清单
- 测试覆盖
- T7 重试与死信前置条件
```

---

## 15. T7 Claude Code 任务单:重试与死信

### 15.1 任务目标

实现失败渠道的指数退避重试,达到上限后入死信;死信支持列表查询、重投、丢弃三种治理动作。

### 15.2 必须先阅读

```text
T1-T6 结果
重构文档第四章 4.1
```

### 15.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-message/src/main/java/cn/oa/message/retry/**` | 新增 `RetryScheduler`、`RetryPolicy` |
| `code/backend/oa-message/src/main/java/cn/oa/message/deadletter/**` | 新增 `DeadLetterService` |
| `code/backend/oa-message/src/test/java/**` | 重试与死信单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 15.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
旧 oa_message
正式 SQL baseline
```

### 15.5 必须实现

| 能力 | 要求 |
|------|------|
| `RetryScheduler` | `@Scheduled` 30s 扫一次 `msg_channel_log`,`status=2 AND next_retry_time<=NOW()` |
| `RetryPolicy` | 指数退避(60s × 2^n),最大重试 `message.retry.max-attempts`(默认3) |
| 重投成功 | 更新 `msg_channel_log.status=1`,`msg_message.status=2`(全部成功) |
| 全部渠道失败 | `msg_message.status=4` |
| 达到上限 | 写入 `msg_dead_letter` |
| `DeadLetterService.replay` | 重新调用渠道,落新 `msg_channel_log`,原死信 `status=1` |
| `DeadLetterService.discard` | 死信 `status=2`,备注 |

### 15.6 验收命令

```bash
cd code/backend
mvn -pl oa-message -am test
```

### 15.7 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T7:重试与死信。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 15 章。

实现重点:
- RetryScheduler @Scheduled
- 指数退避(60s × 2^n)
- 死信写入与治理(replay/discard)
- 单测覆盖连续失败 → 死信 → 重投成功

禁止:
- 不实现 Controller(Wave 5 T8)
- 不动 frontend/mobile

完成后运行:
cd code/backend
mvn -pl oa-message -am test

最终汇报:
- 重试策略
- 死信治理流程
- 测试覆盖
- T8 消息 REST API 注意事项
```

---

## 16. T8 Claude Code 任务单:消息 REST API

### 16.1 任务目标

为 T3-T7 的消息服务暴露 REST API,对齐 `/api/message/*` 契约,并补 Controller 测试。

### 16.2 必须先阅读

```text
T1-T7 结果
code/backend/oa-web/src/main/java/cn/oa/controller/MessageController.java
重构文档第四章 API 契约
```

### 16.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-message/src/main/java/cn/oa/message/controller/**` | 新增 4 个 Controller |
| `code/backend/oa-message/src/test/java/**` | 新增 Controller 测试 |
| `code/backend/oa-web/pom.xml` | 仅当依赖缺失时修改 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 16.4 禁止修改

```text
旧 MessageController.java
code/frontend/**
code/mobile/**
正式 SQL baseline
```

### 16.5 API 必须实现

| 方法 | 路径 | 调用 Service | 权限码 |
|------|------|--------------|--------|
| `GET` | `/api/message/page` | `MessageQuery` | `msg:message:list` |
| `GET` | `/api/message/unread-count` | `unreadCount` | `msg:message:view` |
| `GET` | `/api/message/{id}` | `getDetail` | `msg:message:view` |
| `POST` | `/api/message/{id}/read` | `markAsRead` | `msg:message:read` |
| `POST` | `/api/message/read-batch` | `markBatchRead` | `msg:message:read` |
| `POST` | `/api/message/read-all` | `markAllRead` | `msg:message:read` |
| `DELETE` | `/api/message/{id}` | `delete` | `msg:message:delete` |
| `GET` | `/api/message/preferences` | `MsgPreferenceService.getAll` | `msg:preference:view` |
| `PUT` | `/api/message/preferences` | `MsgPreferenceService.upsert` | `msg:preference:update` |
| `GET` | `/api/message/templates` | `MsgTemplateService.pageQuery` | `msg:template:list` |
| `POST` | `/api/message/templates` | `MsgTemplateService.create` | `msg:template:create` |
| `PUT` | `/api/message/templates/{id}` | `MsgTemplateService.update` | `msg:template:update` |
| `DELETE` | `/api/message/templates/{id}` | `MsgTemplateService.delete` | `msg:template:delete` |
| `GET` | `/api/message/dead-letters` | `DeadLetterService.pageQuery` | `msg:dead-letter:list` |
| `POST` | `/api/message/dead-letters/{id}/replay` | `DeadLetterService.replay` | `msg:dead-letter:replay` |
| `POST` | `/api/message/dead-letters/{id}/discard` | `DeadLetterService.discard` | `msg:dead-letter:discard` |
| `POST` | `/api/message/test-send` | `notify` | `msg:message:send` |

### 16.6 Controller 要求

1. 统一返回 `R<T>`。
2. DTO 参数必须 `@Valid`。
3. 从 `WebUtil.getEmpId` 获取当前用户。
4. 管理接口必须有权限注解。
5. Knife4j 注解完整。
6. 死信 replay 写操作日志。
7. 模板渲染失败的入参校验由 Service 完成。

### 16.7 验收命令

```bash
cd code/backend
mvn -pl oa-message,oa-web -am test
```

### 16.8 可直接交给 Claude Code 的提示词

```text
请执行 消息通知中台 T8:消息 REST API。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 16 章。

前置:
- T3-T7 已完成

允许修改:
- oa-message 内的 Controller 与测试
- 必要时 oa-web/pom.xml
- 本文档执行结果记录

禁止:
- 旧 MessageController
- frontend/mobile
- 正式 SQL baseline

完成后运行:
cd code/backend
mvn -pl oa-message,oa-web -am test

最终汇报:
- 新增文件
- 17 个 API 路径与权限码
- Controller 测试覆盖
- 验收命令结果
- T16 Web 端消息中心前置条件
```

---

## 17. T9 Claude Code 任务单:平台库表 + Entity/Mapper

### 17.1 任务目标

落地平台 6 张表 DDL 与种子,在 `oa-platform` 模块内建立 Entity/Mapper 基础结构,不对应业务 Service 和 Interceptor。

### 17.2 必须先阅读

```text
CLAUDE.md
重构文档第七章(安全设计)
docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md
code/backend/sql/platform_contract.sql
旧 oa_operation_log、oa_login_log、sys_config、sys_dict_type、sys_dict_data
```

### 17.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/sql/platform_contract.sql` | 新建 6 张表 DDL |
| `code/backend/sql/platform_seed.sql` | 字典/配置种子 |
| `code/backend/oa-platform/src/main/java/**` | 新增 Entity、Enum、Mapper、DTO、VO |
| `code/backend/oa-platform/src/test/java/**` | 基础测试 |
| `code/backend/oa-platform/pom.xml` | 必要依赖 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 17.4 禁止修改

```text
code/backend/oa-service/**
code/backend/oa-web/src/main/java/cn/oa/controller/AuthController.java(如果存在)
code/frontend/**
code/mobile/**
旧 oa_operation_log / oa_login_log / sys_config / sys_dict_*
正式 SQL baseline
```

### 17.5 产出物

| 类型 | 建议名称 |
|------|----------|
| Entity | `SysConfig`、`SysDictType`、`SysDictData`、`SysOperationLog`、`SysLoginLog`、`SysDataScope` |
| Enum | `DataScopeType`、`LoginType`、`OperationResult`、`SensitiveField`、`OperationStatus` |
| DTO | `DictTypeDTO`、`DictDataDTO`、`ConfigDTO`、`OperationLogQueryDTO`、`LoginLogQueryDTO`、`DataScopeDTO`、`LoginRequest`、`CaptchaRequest` |
| VO | `DictTypeVO`、`DictDataVO`、`ConfigVO`、`OperationLogVO`、`LoginLogVO`、`DataScopeVO`、`LoginResponse`、`CaptchaResponse` |
| Mapper | `SysConfigMapper`、`SysDictTypeMapper`、`SysDictDataMapper`、`SysOperationLogMapper`、`SysLoginLogMapper`、`SysDataScopeMapper` |

### 17.6 验收命令

```bash
cd code/backend
mvn -pl oa-platform -am test
```

### 17.7 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T9:平台库表 + Entity/Mapper。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 17 章。

只允许:
- 新建 platform_contract.sql / platform_seed.sql
- 在 oa-platform 中新增 Entity/DTO/VO/Enum/Mapper
- 记录执行结果

禁止:
- 不实现 Service/Interceptor/Controller
- 不动旧表/旧 Entity/旧 Controller
- 不动 frontend/mobile

完成后运行:
cd code/backend
mvn -pl oa-platform -am test

最终汇报:
- 6 张表 DDL
- 新增文件
- 验收命令结果
- T10 注意事项
```

---

## 18. T10 Claude Code 任务单:JWT + Redis Session + 多端互踢 + 限流

### 18.1 任务目标

升级认证基础设施:JWT 签发/解析、Redis Token Session、Refresh Token、滑动过期、多端互踢、登录限流。

### 18.2 必须先阅读

```text
T1-T9 结果
重构文档第七章 7.1
旧 AuthInterceptor、RateLimitInterceptor
code/backend/oa-common 中已有 JWT、Redis 工具
```

### 18.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/main/java/cn/oa/platform/security/**` | 新增 `JwtTokenProvider`、`TokenSessionService`、`AuthInterceptor` 升级版、`RateLimitInterceptor` |
| `code/backend/oa-common/**` | 仅在缺少必要抽象时新增 |
| `code/backend/oa-web/src/main/java/cn/oa/config/SecurityConfig.java` | 注册新拦截器、配置白名单 |
| `code/backend/oa-platform/src/test/java/**` | 单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 18.4 禁止修改

```text
code/backend/oa-hr/**
code/backend/oa-finance/**
code/backend/oa-admin/**
code/backend/oa-message/**
code/backend/oa-workflow/**
code/frontend/**
code/mobile/**
旧 AuthInterceptor.java 兼容期保留(改名/新类)
```

### 18.5 必须实现

| 组件 | 关键能力 |
|------|----------|
| `JwtTokenProvider` | `issueAccessToken`、`issueRefreshToken`、`parseClaims`、`validate` |
| `TokenSessionService` | `createSession(empId, deviceId)` 写 `token:{empId}`、可选 `token:{empId}:{deviceId}` |
| `TokenSessionService.refresh` | 滑动过期,延长 TTL 至 `security.jwt.expire-seconds` |
| `TokenSessionService.kickOut(empId, sessionId)` | 删除指定 session,可选踢出所有 |
| `TokenSessionService.kickPrevious(empId, deviceId)` | 同设备再次登录时强制踢出 |
| `AuthInterceptor` | 解析 Bearer Token → 查 Redis Session → 校验签名/有效期 → 写 UserContext → 续期 |
| `RateLimitInterceptor` | 基于 `rate:login:{ip}`,5 次/分钟,超限 429 |

### 18.6 Redis Key 设计

| Key | 用途 | TTL |
|-----|------|-----|
| `token:{empId}` | 当前主会话 Hash(`{deviceId:token,loginTime,kickPrev}`) | 7200s(滑动) |
| `token:refresh:{empId}` | RefreshToken | 7d |
| `rate:login:{ip}` | 登录计数 | 60s |
| `captcha:{key}` | 图形验证码 | 300s |

### 18.7 测试要求

| 测试 | 场景 |
|------|------|
| JWT 签发解析 | 正常/过期/篡改/不同算法 |
| Session 创建 | 首次/同设备/不同设备 |
| 滑动过期 | 临近过期被读取后延长 |
| 多端互踢 | A 登录 → B 登录 → A 请求 401 |
| 限流 | 连续 5 次成功,第 6 次 429 |

### 18.8 验收命令

```bash
cd code/backend
mvn -pl oa-platform,oa-web -am test
```

### 18.9 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T10:JWT + Redis Session + 多端互踢 + 限流。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 18 章。

实现重点:
- JwtTokenProvider
- TokenSessionService(滑动过期/互踢/Refresh)
- AuthInterceptor 升级
- RateLimitInterceptor 5次/分钟/IP

禁止:
- 不动 HR/Finance/Admin/Message/Workflow 业务模块
- 不实现 Controller(T11)
- 不删除旧 AuthInterceptor

完成后运行:
cd code/backend
mvn -pl oa-platform,oa-web -am test

最终汇报:
- 关键类与方法
- Redis Key 清单
- 测试覆盖
- T11 Auth REST API 注意事项
```

---

## 19. T11 Claude Code 任务单:Auth REST API + 登录日志

### 19.1 任务目标

实现 `/api/auth/captcha`、`/login`、`/refresh`、`/logout`、`/online-devices`、`/kick-out/{sessionId}`,登录成功/失败写 `sys_login_log`。

### 19.2 必须先阅读

```text
T1-T10 结果
重构文档第七章 7.1
code/backend/oa-web/src/main/java/cn/oa/controller/AuthController.java(如果存在)
```

### 19.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/main/java/cn/oa/platform/auth/**` | 新增 Controller、Service |
| `code/backend/oa-web/pom.xml` | 必要时 |
| `code/backend/oa-platform/src/test/java/**` | Controller 测试 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 19.4 禁止修改

```text
code/backend/oa-hr/**
code/backend/oa-finance/**
code/backend/oa-admin/**
code/backend/oa-message/**
code/frontend/**
code/mobile/**
旧 AuthController
```

### 19.5 必须实现

| API | 关键点 |
|-----|--------|
| `/api/auth/captcha` | 生成 4 位字母数字,写 `captcha:{key}` |
| `/api/auth/login` | 校验验证码 → 查账号 → BCrypt 校验 → 触发 T10 互踢 → 写登录日志(成功) → 返回 Token |
| `/api/auth/refresh` | 校验 RefreshToken,签发新 AccessToken |
| `/api/auth/logout` | 删除 `token:{empId}` 对应 session,登出日志 |
| `/api/auth/online-devices` | 列 Hash 内 device 列表 |
| `/api/auth/kick-out/{sessionId}` | 管理员/本人踢出指定设备 |

### 19.6 验收命令

```bash
cd code/backend
mvn -pl oa-platform,oa-web -am test
```

### 19.7 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T11:Auth REST API + 登录日志。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 19 章。

前置:
- T10 JWT/Session/限流已完成

实现重点:
- 6 个 Auth API
- 登录成功/失败写 sys_login_log
- 多端会话列表/踢出

禁止:
- 不动业务模块
- 不实现 sysop log API(T15)

完成后运行:
cd code/backend
mvn -pl oa-platform,oa-web -am test

最终汇报:
- 新增文件
- API 路径与权限码
- 登录日志写入路径
- 测试覆盖
- T12 注意事项
```

---

## 20. T12 Claude Code 任务单:DataPermissionInterceptor + AES 工具

### 20.1 任务目标

实现 MyBatis `Interceptor` 自动追加数据权限 WHERE 条件;实现 `AesGcmCrypto` 工具与 `EncryptedFieldTypeHandler`,支持 `@EncryptedField` 注解的字段在落库/读取时自动加解密。

### 20.2 必须先阅读

```text
T1-T11 结果
重构文档第七章 7.2-7.3
旧 AOP 切面、TypeHandler
code/backend/oa-platform/src/main/java/cn/oa/platform/security/DataScope*.java(若已有部分实现)
```

### 20.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/main/java/cn/oa/platform/security/datascope/**` | 新增 |
| `code/backend/oa-platform/src/main/java/cn/oa/platform/security/crypto/**` | 新增 |
| `code/backend/oa-common/src/main/java/cn/oa/common/annotation/EncryptedField.java` | 新增注解 |
| `code/backend/oa-platform/src/test/java/**` | 单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 20.4 禁止修改

```text
code/backend/oa-hr/**
code/backend/oa-finance/**
code/backend/oa-admin/**
code/backend/oa-message/**
code/backend/oa-workflow/**
code/frontend/**
code/mobile/**
旧 Entity 的字段加 @EncryptedField
```

### 20.5 必须实现

| 组件 | 关键能力 |
|------|----------|
| `DataPermissionAnnotation` | 标注在 Mapper 方法或 Entity 上,声明所需 scope |
| `DataScopeResolver` | 根据 `UserContext` + `sys_data_scope` 计算部门ID列表 |
| `DataPermissionInterceptor` | 拦截 `Executor.query/update`,重写 SQL 追加 `dept_id IN (...)` 或 `creator_id = ?` |
| `AesGcmCrypto` | AES-256-GCM 加密/解密,密钥从 `sys_config` 读 Base64 |
| `EncryptedFieldTypeHandler` | MyBatis-Plus `BaseTypeHandler`,调用 `AesGcmCrypto` |
| `@EncryptedField` | 标注敏感字段 |
| 脱敏工具 | `DesensitizationUtils.maskPhone/maskIdCard/maskBankCard` |

### 20.6 数据权限 SQL 注入规则

| scopeType | 注入片段 |
|-----------|----------|
| `ALL` | (无) |
| `DEPT` | `dept_id = #{currentDeptId}` |
| `DEPT_AND_SUB` | `dept_id IN (#{deptAndSubIds})` |
| `PERSONAL` | `creator_id = #{currentEmpId}` |
| `CUSTOM` | `dept_id IN (#{customDeptIds})` |

若 `includeSelf=1` 且 scopeType 非 PERSONAL,追加 OR `creator_id = #{currentEmpId}`。

### 20.7 测试要求

| 测试 | 场景 |
|------|------|
| SQL 注入 | 5 种 scopeType 各 1 例 |
| 加密 roundtrip | 加密 → 解密 一致 |
| 密钥轮转 | 旧字段可被新密钥解密或迁移接口 |
| TypeHandler | 入参出参自动加解密 |
| 脱敏 | phone/idCard/bankCard 展示 |
| 性能 | 单条加密 < 1ms |

### 20.8 验收命令

```bash
cd code/backend
mvn -pl oa-platform,oa-common -am test
```

### 20.9 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T12:DataPermissionInterceptor + AES 工具。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 20 章。

实现重点:
- DataPermissionInterceptor(MyBatis 拦截,SQL 重写)
- AesGcmCrypto + EncryptedFieldTypeHandler
- @EncryptedField 注解
- 脱敏工具

禁止:
- 不动业务模块的 Entity 字段加注解(留待下轮迁移)
- 不动 frontend/mobile

完成后运行:
cd code/backend
mvn -pl oa-platform,oa-common -am test

最终汇报:
- SQL 注入规则
- 加密算法与 Key 来源
- TypeHandler 装配方式
- 测试覆盖
- 业务模块集成注意事项
```

---

## 21. T13 Claude Code 任务单:Dict/Config Service

### 21.1 任务目标

实现 `DictTypeService`、`DictDataService`、`ConfigService` 的 CRUD、缓存、加密配置回显。

### 21.2 必须先阅读

```text
T1-T12 结果
重构文档第七章
code/backend/sql/platform_contract.sql
code/backend/sql/platform_seed.sql
```

### 21.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/main/java/cn/oa/platform/dict/**` | 新增 |
| `code/backend/oa-platform/src/main/java/cn/oa/platform/config/**` | 新增 |
| `code/backend/oa-platform/src/test/java/**` | 单测 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 21.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
旧 DictService/ConfigService
```

### 21.5 必须实现

| Service | 方法 |
|---------|------|
| `DictTypeService` | `page`、`create`、`update`、`delete`、`getByCode` |
| `DictDataService` | `listByType`、`page`、`create`、`update`、`delete`、按 type 缓存 |
| `ConfigService` | `page`、`getByKey`(含解密)、`updateByKey`、`refreshCache`、按 key 缓存 |

### 21.6 缓存设计

| Key | Value | TTL |
|-----|-------|-----|
| `sys:dict:type:{code}` | DictType | 30min |
| `sys:dict:data:{code}` | List<DictData> | 30min |
| `sys:cfg:{key}` | Config(decrypt 后) | 30min |

### 21.7 验收命令

```bash
cd code/backend
mvn -pl oa-platform -am test
```

### 21.8 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T13:Dict/Config Service。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 21 章。

实现重点:
- DictType/DictData/Config CRUD
- Redis 缓存
- 加密配置回显
- 刷新缓存接口

完成后运行:
cd code/backend
mvn -pl oa-platform -am test

最终汇报:
- 缓存 Key 清单
- 加密配置写入/读取
- 测试覆盖
- T14 注意事项
```

---

## 22. T14 Claude Code 任务单:Dict/Config REST API

### 22.1 任务目标

实现 `/api/system/dict-types`、`/api/system/dict-data`、`/api/system/configs` 全部 REST 接口。

### 22.2 必须先阅读

```text
T1-T13 结果
重构文档第七章
```

### 22.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/main/java/cn/oa/platform/controller/**` | 新增 |
| `code/backend/oa-platform/src/test/java/**` | Controller 测试 |
| `code/backend/oa-web/pom.xml` | 必要时 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 22.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
旧 DictController/ConfigController
```

### 22.5 验收命令

```bash
cd code/backend
mvn -pl oa-platform,oa-web -am test
```

### 22.6 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T14:Dict/Config REST API。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 22 章。

前置:
- T13 Service 已完成

实现:
- 字典类型/数据/配置 全部 REST 接口
- Knife4j 注解
- Controller 测试

禁止:
- 不动 frontend/mobile

完成后运行:
cd code/backend
mvn -pl oa-platform,oa-web -am test

最终汇报:
- 16 个 API 路径
- 权限码清单
- 测试覆盖
```

---

## 23. T15 Claude Code 任务单:审计日志 + 数据权限规则 API

### 23.1 任务目标

实现 `@OperationLog` 注解 + AOP 自动写 `sys_operation_log`,`LoginLogService` 写登录日志;`/api/system/operation-logs`、`/api/system/login-logs`、`/api/system/data-scopes` 三个 Controller。

### 23.2 必须先阅读

```text
T1-T14 结果
重构文档第七章
```

### 23.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-common/src/main/java/cn/oa/common/annotation/OperationLog.java` | 注解 |
| `code/backend/oa-common/src/main/java/cn/oa/common/aop/OperationLogAspect.java` | AOP |
| `code/backend/oa-platform/src/main/java/cn/oa/platform/audit/**` | Service |
| `code/backend/oa-platform/src/main/java/cn/oa/platform/controller/**` | 新增 3 个 Controller |
| `code/backend/oa-platform/src/test/java/**` | 测试 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 23.4 禁止修改

```text
code/backend/oa-hr/**
code/backend/oa-finance/**
code/backend/oa-admin/**
code/backend/oa-message/**
code/backend/oa-workflow/**
code/frontend/**
code/mobile/**
旧 OperationLog/OperationLogAspect
```

### 23.5 必须实现

| 组件 | 关键能力 |
|------|----------|
| `@OperationLog(module, operation, businessType)` | 标注在 Controller 方法上 |
| `OperationLogAspect` | `@Around` 拦截,记录请求/响应/异常/耗时/IP,敏感参数脱敏 |
| `OperationLogService` | `pageQuery` |
| `LoginLogService` | `recordSuccess`、`recordFailure`、`pageQuery` |
| `DataScopeService` | `page`、`create`、`update`、`delete`、`resolveByUser(empId, roleId)` |

### 23.6 脱敏规则

| 字段 | 处理 |
|------|------|
| `password` | 全替换 `******` |
| `idCard` | 保留首末 4 位 |
| `bankCard` | 保留末 4 位 |
| `phone` | 中间 4 位 `*` |
| `salary` | 全替换 |

### 23.7 验收命令

```bash
cd code/backend
mvn -pl oa-common,oa-platform,oa-web -am test
```

### 23.8 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T15:审计日志 + 数据权限规则 API。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 23 章。

实现重点:
- @OperationLog 注解 + Aspect
- LoginLog/OperationLog/DataScope 三个 Service
- 3 个 Controller
- 敏感参数脱敏

禁止:
- 不动业务模块 Controller 上的注解(留待后续任务)
- 不实现 @OperationLog 的具体业务埋点(本任务只提供 AOP)

完成后运行:
cd code/backend
mvn -pl oa-common,oa-platform,oa-web -am test

最终汇报:
- 新增文件
- AOP 拦截路径
- 脱敏规则覆盖字段
- 测试覆盖
- 业务模块接入步骤
```

---

## 24. T16 Claude Code 任务单:Web 端消息中心 + 偏好 + 系统管理

### 24.1 任务目标

Web 端实现消息中心(收件箱/已读/未读/未读数/跳转)、偏好设置页、系统管理(字典/配置/操作日志/登录日志/数据权限规则)。

### 24.2 必须先阅读

```text
T1-T15 结果
code/frontend/src/api/message.ts
code/frontend/src/api/system.ts
重构文档第五章
```

### 24.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/frontend/src/api/message*.ts` | 新增/改造 typed API |
| `code/frontend/src/api/system*.ts` | 新增 typed API |
| `code/frontend/src/api/auth.ts` | 改造登录 API |
| `code/frontend/src/views/oa/message/**` | 消息中心 |
| `code/frontend/src/views/system/**` | 字典/配置/日志/数据权限 |
| `code/frontend/src/stores/modules/notification.ts` | 通知 Store(铃铛未读数) |
| `code/frontend/src/composables/useDict.ts` | 字典 Composable |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 24.4 禁止修改

```text
code/mobile/**
其他业务模块的 views
全局 layout 改造
```

### 24.5 必须实现页面

| 页面 | 关键能力 |
|------|----------|
| `views/oa/message/inbox.vue` | 收件箱、分页、批量已读、跳详情 |
| `views/oa/message/detail.vue` | 详情 + 跳转业务链接 |
| `views/oa/message/preferences.vue` | 按 msgType 切换渠道 + 免打扰 |
| `views/system/dict/type.vue` | 字典类型管理 |
| `views/system/dict/data.vue` | 字典数据管理 |
| `views/system/config/index.vue` | 配置管理(加密项隐藏) |
| `views/system/log/operation.vue` | 操作日志查询 |
| `views/system/log/login.vue` | 登录日志查询 |
| `views/system/security/data-scope.vue` | 数据权限规则 |
| `views/system/security/online-devices.vue` | 在线设备 + 踢出 |

### 24.6 验收命令

```bash
cd code/frontend
pnpm typecheck
pnpm build
```

### 24.7 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T16:Web 端消息中心 + 偏好 + 系统管理。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 24 章。

实现重点:
- 消息中心(收件箱/已读/未读/未读数/跳转)
- 偏好设置
- 字典/配置/操作日志/登录日志/数据权限 管理页
- 通知 Store(铃铛未读数)
- 字典 Composable

禁止:
- 不动 mobile
- 不改全局 layout
- 不做 monorepo 改造

完成后运行:
cd code/frontend
pnpm typecheck && pnpm build

最终汇报:
- 新增文件
- 路由清单
- 字典 Composable 使用方式
- 验收命令结果
- T17 移动端前置条件
```

---

## 25. T17 Claude Code 任务单:Mobile 端消息中心

### 25.1 任务目标

实现移动端消息中心(收件箱、详情、未读数 badge)、偏好设置。

### 25.2 必须先阅读

```text
T1-T16 结果
code/mobile/src/api/message.ts
code/mobile/src/pages/message/*
```

### 25.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/mobile/src/api/message.ts` | 改造 typed API |
| `code/mobile/src/api/auth.ts` | 登录 API |
| `code/mobile/src/pages/message/*` | 消息中心 |
| `code/mobile/src/pages/mine/preferences.vue` | 偏好 |
| `code/mobile/src/store/notification.ts` | 通知 Store(可选) |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录执行结果 |

### 25.4 禁止修改

```text
code/frontend/**
其他业务模块的 pages
tabbar 结构变更
```

### 25.5 必须实现页面

| 页面 | 关键能力 |
|------|----------|
| `pages/message/inbox.vue` | 列表、分页、已读 |
| `pages/message/detail.vue` | 详情 + 跳转 |
| `pages/message/preferences.vue` | 渠道切换 + 免打扰 |
| `components/MessageBadge.vue` | 角标组件,首页/工作台显示未读 |

### 25.6 验收命令

```bash
cd code/mobile
pnpm build:h5
```

### 25.7 可直接交给 Claude Code 的提示词

```text
请执行 平台安全 T17:Mobile 端消息中心。

严格遵循 docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md 第 25 章。

实现重点:
- 消息收件箱 + 详情
- 偏好设置
- 未读数 badge

禁止:
- 不动 frontend
- 不改 tabbar
- 不动其他业务模块

完成后运行:
cd code/mobile
pnpm build:h5

最终汇报:
- 新增文件
- 路由清单
- badge 接入方式
- 验收命令结果
- T18 演示前置条件
```

---

## 26. T18 Claude Code 任务单:端到端演示与文档

### 26.1 任务目标

编写端到端演示手册,覆盖登录、触发业务事件、收通知、偏好生效、重试/死信、管理后台处理。

### 26.2 必须先阅读

```text
T1-T17 结果
重构文档第四章 + 第七章
```

### 26.3 允许修改

| 路径 | 说明 |
|------|------|
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 追加演示章节 |
| `tests/demo/**` | 演示脚本(可选) |

### 26.4 演示步骤

1. 登录 admin → 创建测试用户 + 测试模板 + 测试偏好。
2. 业务模块触发事件(可手动调 Service 或 `DomainEventPublisher.publish`)。
3. WebSocket 收到推送 + 消息列表显示。
4. 修改偏好(关闭 SMS)→ 再次触发 → 只收到 SITE/EMAIL。
5. 模拟 SMTP 失败(改坏密码)→ 重试 → 入死信。
6. 管理员在死信列表点"重投"→ 修复 SMTP → 死信 replay → 状态翻转。
7. 移动端登录 → 收到同一条消息 → 铃铛未读数变化。
8. 切到第二个设备 → 第一个设备请求 401。
9. 操作日志中可查看到上述所有写操作。

---

## 27. T19 Claude Code 任务单:端到端回归

### 27.1 任务目标

集成测试/E2E 验证登录/权限/数据隔离/加密/字典/消息/重试 完整链路。

### 27.2 必须先阅读

```text
T1-T18 结果
```

### 27.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-platform/src/test/java/**` | 集成测试 |
| `code/backend/oa-message/src/test/java/**` | 集成测试 |
| `tests/integration/**` | 跨模块测试 |
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 记录结果 |

### 27.4 测试场景

| 场景 | 断言 |
|------|------|
| 登录 → 业务事件 → 消息 | 收到 1 条 SITE + 1 条 EMAIL |
| 偏好修改 | 后续消息渠道数符合 |
| 死信 | 失败 3 次后入死信,replay 成功 |
| 数据权限 | 普通员工只能查本人请假 |
| AES | 落库为密文,读取明文 |
| 操作日志 | 写/改/删 都有记录,IP 正确 |
| 多端互踢 | 设备 A 401 |

### 27.5 验收命令

```bash
cd code/backend
mvn -pl oa-common,oa-message,oa-platform,oa-web -am test
cd code/frontend && pnpm typecheck && pnpm build
cd code/mobile && pnpm build:h5
```

---

## 28. T20 Claude Code 任务单:旧入口下线清单

### 28.1 任务目标

标记旧 `/api/message/*`、`/api/auth/login`、`oa_message` 等表/接口的替换关系和下线时机。

### 28.2 必须先阅读

```text
T1-T19 结果
T2 旧实现影响分析
```

### 28.3 允许修改

| 路径 | 说明 |
|------|------|
| `docs/superpowers/specs/2026-06-02-msg-notification-platform-task-split.md` | 追加下线清单章节 |

### 28.4 输出

| 旧路径 | 新路径 | 切换条件 | 回滚方式 |
|--------|--------|----------|----------|
| `GET /api/message/page`(旧) | `GET /api/message/page`(新) | T19 集成通过 | 旧 Controller 保留 |
| `GET /api/message/unread` | `GET /api/message/unread-count` | 字段名变更 | 前端 fallback |
| 旧 `oa_message` 表 | `msg_message` 表 | 数据迁移脚本确认 | 双写期 |
| 旧 `sys_dict_type` | `sys_dict_type`(新) | 新表数据导入完成 | 双写期 |
| 旧 `sys_config` | `sys_config`(新) | 旧配置迁移完成 | 双写期 |
| 旧 `oa_operation_log` | `sys_operation_log` | AOP 接入完成 | 双写期 |
| 旧 `oa_login_log` | `sys_login_log` | T11 接入完成 | 双写期 |

---

## 29. 与其他任务拆分文档的协作

| 协作项 | 关系 |
|--------|------|
| `2026-06-02-hr-leave-pilot-task-split.md` | HR 业务回调通过 `DomainEventPublisher` 通知 T6 订阅器 |
| `2026-06-02-wf-engine-kernel-task-split.md` | 工作流回调同样通过 `DomainEventPublisher` 发消息 |
| `2026-06-02-hr-attendance-task-split.md` | 考勤异常通过事件触发 SMS 通知 |
| `2026-06-02-fin-budget-expense-task-split.md` | 报销通过/驳回通过事件触发邮件/短信 |
| `2026-06-02-doc-dispatch-receive-task-split.md` | 公文分发通过事件触发通知 |
| `2026-06-02-hr-employee-archive-task-split.md` | 档案敏感字段通过 T12 加密落库 |
| `2026-06-02-hr-performance-task-split.md` | 绩效通过事件触发通知 |
| `2026-06-02-hr-recruitment-task-split.md` | 招聘通过事件触发通知 |
| `2026-06-02-hr-training-task-split.md` | 培训通过事件触发通知 |

后续所有业务模块只需要:

1. 在 `oa-common` 发布 `DomainEvent`(不直接调 `MessageNotificationService`)。
2. 在 `oa-message` 增加一个 `MessageEventSubscriber` 把事件转为 `NotifyRequest`。
3. 在模板表里维护 `template_code` + 变量 schema。

---

## 30. 后续迭代建议

1. 引入 Prometheus + Grafana 监控 `msg_channel_log` 成功率、平均耗时、渠道分布。
2. 死信告警:管理员未处理的死信超过 N 条时通过 SYSTEM_ALERT 通知。
3. 模板可视化编辑器(Web),低代码维护。
4. 接入企业微信/钉钉/飞书多机器人适配器(目前只 WECHAT)。
5. 全文检索(ES)接入消息内容,提升搜索能力。
6. SSOS 接入(钉钉扫码、企微扫码)。
7. 密钥管理(KMS)替代 `sys_config` 存主密钥。
8. 国际化(i18n)消息模板,多语言渲染。

---

## 31. 文档结束

本任务拆分文档与 `2026-06-02-hr-leave-pilot-task-split.md` 风格保持一致,包含 11 个波次、20 个 T 任务(消息中台 T1-T8, 平台安全 T9-T15, 前端 T16-T18, 验收 T19-T20),每个任务给出目标、范围、产出、验收命令和 Claude Code 提示词。后续业务模块的拆分文档可通过 `DomainEventPublisher` 直接对接本中台,无需重复实现通知链路。
