# 企业OA系统 - 第四阶段设计文档：高级特性与智能化模块

## 1. 阶段概述

第四阶段在前三个阶段的基础上，构建移动办公、智能决策分析、AI智能助手三大高级能力，实现OA系统从"工具平台"到"智能中枢"的升级。

### 1.1 依赖关系

```
第一~三阶段（已完成）        第四阶段
┌──────────────────┐      ┌──────────────────────┐
│ 所有业务数据       │◄─────│ 数据驾驶舱/智能分析    │
│ 流程审批引擎       │◄─────│ AI智能助手            │
│ 所有前端页面       │◄─────│ 移动办公(H5/小程序)    │
│ 组织架构/权限      │◄─────│ 系统集成中台          │
└──────────────────┘      └──────────────────────┘
```

### 1.2 新增服务

| 服务 | 职责 | 端口 |
|------|------|------|
| oa-report | 报表与数据分析服务 | 9208 |
| oa-mobile | 移动端BFF(Backend For Frontend) | 9209 |

AI智能助手通过集成外部AI服务实现，不独立部署服务，而是在各业务服务中嵌入AI能力。

---

## 2. 数据驾驶舱与智能分析

### 2.1 数据驾驶舱

管理层视角的核心经营数据实时展示页面，支持自定义仪表盘。

#### 2.1.1 预置仪表盘

**高管驾驶舱：**
| 指标 | 数据来源 | 展示方式 |
|------|---------|---------|
| 在职人数/本月新入职/离职率 | 人事模块 | 数字卡片 |
| 本月考勤异常率 | 考勤模块 | 数字卡片 |
| 待审批流程数量 | 流程审批 | 数字卡片+列表 |
| 本月费用支出/预算执行率 | 财务模块 | 进度条+柱状图 |
| 资产总额/闲置率 | 资产模块 | 数字卡片 |
| 合同到期预警 | 合同模块 | 列表 |
| 流程审批效率趋势 | 流程审批 | 折线图 |

**HR驾驶舱：**
| 指标 | 展示方式 |
|------|---------|
| 部门人数分布 | 饼图 |
| 入职离职趋势 | 折线图 |
| 年龄/学历/工龄分布 | 柱状图/饼图 |
| 考勤异常排名 | 横向柱状图 |
| 培训覆盖率 | 数字卡片 |
| 薪酬成本趋势 | 折线图 |

**财务驾驶舱：**
| 指标 | 展示方式 |
|------|---------|
| 收支总览 | 数字卡片 |
| 预算执行率 | 仪表盘 |
| 费用分类占比 | 饼图 |
| 月度费用趋势 | 折线图 |
| 部门费用排名 | 横向柱状图 |
| 应收应付 | 列表 |

#### 2.1.2 自定义仪表盘

- 用户可创建个人仪表盘
- 从组件库拖拽选择指标组件到画布
- 支持自由布局和栅格布局
- 支持设置刷新频率（实时/5分钟/手动）
- 支持全屏展示（适合大屏投屏）

**图表组件库：**
| 组件 | 说明 |
|------|------|
| 数字卡片 | 单个指标数值 + 同比/环比 |
| 折线图 | 趋势数据 |
| 柱状图 | 对比数据 |
| 饼图/环形图 | 占比数据 |
| 仪表盘 | 完成率/百分比 |
| 进度条 | 执行进度 |
| 数据表格 | 明细列表 |
| 排行榜 | 排名列表 |

#### 2.1.3 报表中心

**预置报表：**
| 报表 | 说明 | 频率 |
|------|------|------|
| 月度人事报表 | 入职/离职/转正/调动汇总 | 月 |
| 月度考勤报表 | 各部门出勤/异常/加班统计 | 月 |
| 月度费用报表 | 各部门费用支出明细 | 月 |
| 季度培训报表 | 培训场次/人次/覆盖率 | 季 |
| 年度经营报表 | 全年各维度数据汇总 | 年 |
| 资产盘点报告 | 盘点结果和差异分析 | 按需 |

**报表功能：**
- 定时自动生成（配置生成规则和推送对象）
- 在线查看（表格+图表）
- 导出（Excel/PDF）
- 报表订阅（生成后自动推送给订阅人）

### 2.2 智能分析

#### 2.2.1 自然语言问数

用户通过自然语言提问，系统自动查询数据并返回图表或文字回答。

**示例对话：**
```
用户：上个月技术部请了多少天假？
系统：技术部2026年4月共请假 23.5 天，其中事假 8 天，年假 10.5 天，病假 5 天。
     [附饼图：假期类型分布]

用户：和去年同期比呢？
系统：2025年4月技术部请假 18 天，今年同比增长 30.6%。
     [附柱状图：同比对比]

用户：哪个部门请假最多？
系统：2026年4月各部门请假天数排名：
     1. 市场部 45天
     2. 技术部 23.5天
     3. 行政部 15天
     [附横向柱状图]
```

**技术方案：**
- 前端：对话框 + 图表渲染区
- 后端：自然语言 → SQL/查询接口（可选用以下方案）：
  - 方案A：预定义查询模板 + 关键词匹配（简单可控）
  - 方案B：对接大语言模型（需部署LLM，成本高）
- 第一版先用方案A（模板匹配），后续升级方案B

#### 2.2.2 智能预警

系统自动监控关键指标，触发预警：

| 预警项 | 规则 | 通知对象 |
|--------|------|---------|
| 离职率异常 | 月离职率超过阈值(如5%) | HR负责人 |
| 考勤异常率 | 部门月异常率超过阈值 | 部门主管+HR |
| 预算超支 | 部门预算使用超80% | 部门负责人+财务 |
| 合同到期 | 到期前30/15/7天 | 合同负责人+法务 |
| 审批超时 | 审批节点超过N小时未处理 | 审批人+发起人 |
| 资产闲置 | 闲置超过90天 | 资产管理员 |
| 薪酬异常 | 薪酬波动超过20% | HR+财务 |

预警管理：规则配置、通知方式（站内信/邮件/短信）、预警记录、确认/忽略。

### 2.3 数据库设计（oa_report库）

```sql
-- 仪表盘
CREATE TABLE rpt_dashboard (
    dashboard_id   BIGINT       NOT NULL AUTO_INCREMENT,
    dashboard_name VARCHAR(100) NOT NULL,
    dashboard_type CHAR(1)      DEFAULT '0' COMMENT '类型(0系统预置 1自定义)',
    layout_config  TEXT         NOT NULL COMMENT '布局配置JSON',
    owner_id       BIGINT       DEFAULT NULL COMMENT '所有者(自定义时)',
    is_default     CHAR(1)      DEFAULT 'N',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (dashboard_id)
) ENGINE=InnoDB COMMENT='仪表盘表';

-- 仪表盘组件
CREATE TABLE rpt_dashboard_widget (
    widget_id      BIGINT       NOT NULL AUTO_INCREMENT,
    dashboard_id   BIGINT       NOT NULL,
    widget_type    VARCHAR(30)  NOT NULL COMMENT '组件类型(number_card/line_chart/bar_chart/pie_chart/gauge/table/ranking)',
    title          VARCHAR(100) NOT NULL COMMENT '标题',
    data_source    VARCHAR(50)  NOT NULL COMMENT '数据源标识',
    data_config    TEXT         NOT NULL COMMENT '数据查询配置JSON',
    position_config TEXT        NOT NULL COMMENT '位置/大小配置JSON',
    refresh_interval INT        DEFAULT 0 COMMENT '刷新间隔(秒, 0不自动刷新)',
    PRIMARY KEY (widget_id),
    KEY idx_dashboard (dashboard_id)
) ENGINE=InnoDB COMMENT='仪表盘组件表';

-- 报表定义
CREATE TABLE rpt_report (
    report_id      BIGINT       NOT NULL AUTO_INCREMENT,
    report_name    VARCHAR(100) NOT NULL,
    report_type    VARCHAR(30)  NOT NULL COMMENT '报表类型',
    report_config  TEXT         NOT NULL COMMENT '报表配置JSON(数据源/列/筛选条件/图表)',
    cron_expression VARCHAR(50) DEFAULT '' COMMENT '定时生成Cron表达式',
    subscribers    VARCHAR(500) DEFAULT '' COMMENT '订阅人IDs',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (report_id)
) ENGINE=InnoDB COMMENT='报表定义表';

-- 生成的报表实例
CREATE TABLE rpt_report_instance (
    instance_id    BIGINT       NOT NULL AUTO_INCREMENT,
    report_id      BIGINT       NOT NULL,
    report_period  VARCHAR(20)  NOT NULL COMMENT '报表周期(2026-05)',
    file_url       VARCHAR(500) DEFAULT '' COMMENT '导出文件URL',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0生成中 1已完成 2失败)',
    generate_time  DATETIME     DEFAULT NULL,
    PRIMARY KEY (instance_id),
    KEY idx_report (report_id)
) ENGINE=InnoDB COMMENT='报表实例表';

-- 智能预警规则
CREATE TABLE rpt_alert_rule (
    rule_id        BIGINT       NOT NULL AUTO_INCREMENT,
    rule_name      VARCHAR(100) NOT NULL,
    rule_type      VARCHAR(30)  NOT NULL COMMENT '预警类型',
    metric         VARCHAR(50)  NOT NULL COMMENT '监控指标',
    condition_type VARCHAR(10)  NOT NULL COMMENT '条件(gt/lt/eq/between)',
    threshold      DECIMAL(12,2) DEFAULT 0 COMMENT '阈值',
    threshold_max  DECIMAL(12,2) DEFAULT NULL COMMENT '上限(between时)',
    check_cron     VARCHAR(50)  DEFAULT '' COMMENT '检查频率',
    notify_type    VARCHAR(30)  DEFAULT 'inner' COMMENT '通知方式(inner/email/sms)',
    notify_targets VARCHAR(500) DEFAULT '' COMMENT '通知对象(角色ID/用户ID)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (rule_id)
) ENGINE=InnoDB COMMENT='预警规则表';

-- 预警记录
CREATE TABLE rpt_alert_log (
    alert_id       BIGINT       NOT NULL AUTO_INCREMENT,
    rule_id        BIGINT       NOT NULL,
    rule_name      VARCHAR(100) DEFAULT '',
    alert_level    CHAR(1)      DEFAULT '0' COMMENT '级别(0提示 1警告 2严重)',
    metric_value   DECIMAL(12,2) DEFAULT 0 COMMENT '实际值',
    threshold      DECIMAL(12,2) DEFAULT 0 COMMENT '阈值',
    alert_content  VARCHAR(500) DEFAULT '' COMMENT '预警内容',
    notify_status  CHAR(1)      DEFAULT '0' COMMENT '通知状态(0未发送 1已发送)',
    handle_status  CHAR(1)      DEFAULT '0' COMMENT '处理状态(0未处理 1已确认 2已忽略)',
    handler        VARCHAR(30)  DEFAULT '' COMMENT '处理人',
    handle_remark  VARCHAR(200) DEFAULT '' COMMENT '处理备注',
    alert_time     DATETIME     DEFAULT NULL,
    handle_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (alert_id),
    KEY idx_rule_time (rule_id, alert_time)
) ENGINE=InnoDB COMMENT='预警记录表';
```

---

## 3. AI智能助手

将AI能力深度融入办公场景，实现从"被动工具"到"主动助手"的升级。

### 3.1 智能审批辅助

**功能：**
- 审批摘要：AI自动总结申请内容要点（如"张三申请3天年假，余额剩余5天"）
- 风险提示：自动识别异常（如"金额超出标准200%"、"该员工本月已请假3次"）
- 相似案例：推荐历史同类审批的处理结果供参考
- 审批建议：基于历史审批数据，给出"建议同意/建议拒绝"的参考意见

**技术方案：**
- 规则引擎 + 历史数据分析（第一版）
- 对接LLM做语义理解（第二版）

### 3.2 智能问答

**功能：**
- 企业制度问答："年假怎么算？"、"出差标准是多少？"
- 流程查询："我的请假审批到哪了？"、"上个月报销了多少？"
- 数据查询："技术部有多少人？"、"本月预算执行率？"
- 操作引导："怎么申请加班？"、"怎么修改密码？"

**知识库构建：**
- 导入企业制度文档，自动切片和向量化
- 审批流程说明、FAQ、操作手册作为知识源
- 系统数据通过API实时查询

**技术方案：**
```
用户提问 → 意图识别(制度查询/数据查询/操作引导/闲聊)
  → 制度查询: RAG检索知识库 → LLM生成回答
  → 数据查询: 自然语言→SQL/API查询 → 格式化展示
  → 操作引导: 匹配操作手册 → 返回步骤
  → 闲聊: LLM直接回答
```

### 3.3 智能会议纪要

**功能：**
- 会议录音转文字（对接语音识别服务）
- 自动提取关键信息和决议
- 自动拆解待办任务，关联到待办中心
- 会议纪要自动生成，支持人工修正

### 3.4 RPA流程自动化

**场景：**
| 场景 | 自动化内容 |
|------|-----------|
| 日报/周报汇总 | 定时收集各人日报，汇总成部门周报 |
| 报销初审 | 自动校验金额、发票、费用标准，标记异常项 |
| 考勤月报 | 每月自动生成考勤汇总，异常项标记 |
| 合同到期提醒 | 定期扫描到期合同，自动发通知 |
| 入职自动化 | 入职审批通过后自动创建账号、分配权限、发送通知 |

**技术方案：**
- 基于定时任务 + 规则引擎实现
- 每个自动化场景对应一个Job
- Job配置化：触发条件、执行步骤、异常处理、通知方式

### 3.5 数据库设计（存oa_system库扩展）

```sql
-- AI知识库
CREATE TABLE ai_knowledge (
    knowledge_id   BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    content        LONGTEXT     NOT NULL,
    source_type    VARCHAR(30)  DEFAULT 'manual' COMMENT '来源(manual/document/policy)',
    source_id      BIGINT       DEFAULT NULL,
    category       VARCHAR(30)  DEFAULT '' COMMENT '分类(policy/process/faq/guide)',
    tags           VARCHAR(500) DEFAULT '',
    vector_id      VARCHAR(100) DEFAULT '' COMMENT '向量存储ID',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (knowledge_id)
) ENGINE=InnoDB COMMENT='AI知识库表';

-- AI对话记录
CREATE TABLE ai_chat_log (
    chat_id        BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    session_id     VARCHAR(64)  NOT NULL COMMENT '会话ID',
    question       TEXT         NOT NULL,
    answer         TEXT         NOT NULL,
    intent_type    VARCHAR(30)  DEFAULT '' COMMENT '意图类型',
    sources        TEXT         DEFAULT NULL COMMENT '引用来源JSON',
    feedback       CHAR(1)      DEFAULT '' COMMENT '用户反馈(1有用 2无用)',
    create_time    DATETIME     DEFAULT NULL,
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (chat_id),
    KEY idx_user_session (user_id, session_id)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- RPA自动化任务
CREATE TABLE rpa_auto_task (
    task_id        BIGINT       NOT NULL AUTO_INCREMENT,
    task_name      VARCHAR(100) NOT NULL,
    task_type      VARCHAR(30)  NOT NULL COMMENT '任务类型',
    cron_expression VARCHAR(50) DEFAULT '' COMMENT '定时表达式',
    trigger_config TEXT         DEFAULT NULL COMMENT '触发条件JSON',
    action_config  TEXT         NOT NULL COMMENT '执行步骤JSON',
    notify_config  TEXT         DEFAULT NULL COMMENT '通知配置JSON',
    last_execute_time DATETIME  DEFAULT NULL,
    last_execute_status CHAR(1) DEFAULT '' COMMENT '上次执行状态(0成功 1失败)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (task_id)
) ENGINE=InnoDB COMMENT='RPA自动化任务表';
```

---

## 4. 移动办公

### 4.1 技术方案

采用 **UniApp** 框架，一套代码适配 H5、微信小程序、Android、iOS、鸿蒙。

### 4.2 移动端核心功能

**优先级P0（第一版必须）：**
| 功能 | 说明 |
|------|------|
| 登录认证 | 账号密码 + 手机验证码登录 |
| 待办审批 | 查看/同意/拒绝审批 |
| 消息通知 | 站内信推送和查看 |
| 公告查看 | 通知公告列表和详情 |
| 请假申请 | 发起/查看请假 |
| 考勤打卡 | GPS定位打卡 |
| 通讯录 | 企业通讯录 |
| 个人中心 | 个人信息、修改密码 |

**优先级P1（第二版）：**
| 功能 | 说明 |
|------|------|
| 文档查看 | 文档库浏览和在线预览 |
| 报销申请 | 发起/查看报销 |
| 日程查看 | 日程列表和提醒 |
| IM消息 | 即时通讯 |
| 出差申请 | 发起/查看出差 |

**优先级P2（第三版）：**
| 功能 | 说明 |
|------|------|
| 数据驾驶舱 | 关键指标卡片展示 |
| AI智能助手 | 语音/文字交互 |
| 移动审批 | 审批流程管理 |
| 离线缓存 | 断网时可查看已缓存数据 |

### 4.3 移动端BFF层

移动端接口与PC端不同，需要专门的BFF（Backend For Frontend）层适配：

**差异点：**
| 维度 | PC端 | 移动端 |
|------|------|--------|
| 数据量 | 完整列表 | 分页更小，摘要优先 |
| 接口粒度 | 单一职责 | 聚合接口（减少请求次数） |
| 推送方式 | 轮询/WS | 厂商推送（华为/小米/APNs） |
| 认证方式 | JWT Token | Token + 设备绑定 |
| 图片 | 原图 | 压缩缩略图 |

oa-mobile服务提供聚合接口，内部调用其他微服务获取数据并整合返回。

### 4.4 消息推送方案

```
业务服务产生通知 → 消息队列 → 推送服务
                                 ├→ 站内信（写入待办表）
                                 ├→ 邮件（SMTP）
                                 ├→ 短信（阿里云/腾讯云短信API）
                                 ├→ 企微/钉钉（Webhook）
                                 └→ App推送（华为/小米/FCM/APNs）
```

---

## 5. 系统集成中台

实现OA系统与外部系统的数据互通。

### 5.1 集成能力

| 集成类型 | 说明 | 方式 |
|---------|------|------|
| ERP对接 | 同步采购订单、供应商数据 | REST API双向同步 |
| CRM对接 | 同步客户信息、合同数据 | REST API双向同步 |
| 财务系统 | 报销数据自动同步至财务系统 | 消息队列 + API |
| 邮件系统 | 统一通讯录、邮件通知 | SMTP/IMAP + LDAP |
| 企业微信/钉钉 | 组织架构同步、消息推送 | 官方SDK + Webhook |
| 身份源 | LDAP/AD统一认证 | LDAP协议 |
| 短信/邮件服务 | 验证码、通知推送 | 阿里云/腾讯云API |
| 电子签章 | 合同在线签署 | 第三方签章平台API |
| 文件存储 | 非结构化文件存储 | OSS/MinIO S3协议 |
| AI服务 | NLP、语音识别、OCR | 各AI平台API |

### 5.2 OpenAPI

提供标准RESTful API供第三方系统调用：

**API管理：**
- API密钥管理（AppKey + AppSecret）
- 接口权限控制（按应用授权）
- 调用频率限制
- 调用日志和统计
- API版本管理

**开放接口范围：**
- 组织架构：部门/用户/岗位查询
- 流程审批：发起/查询/审批
- 文档管理：上传/下载/查询
- 通知推送：发送站内信
- 数据查询：考勤/请假/报表数据

### 5.3 数据库设计

```sql
-- 集成配置
CREATE TABLE integration_config (
    config_id      BIGINT       NOT NULL AUTO_INCREMENT,
    integration_type VARCHAR(30) NOT NULL COMMENT '类型(erp/crm/email/wechat/dingtalk/sms/esign/ldap)',
    config_name    VARCHAR(100) NOT NULL,
    config_json    TEXT         NOT NULL COMMENT '连接配置JSON(URL/密钥/参数)',
    sync_config    TEXT         DEFAULT NULL COMMENT '同步规则JSON',
    sync_status    CHAR(1)      DEFAULT '0' COMMENT '同步状态',
    last_sync_time DATETIME     DEFAULT NULL,
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (config_id)
) ENGINE=InnoDB COMMENT='集成配置表';

-- OpenAPI应用
CREATE TABLE openapi_app (
    app_id         BIGINT       NOT NULL AUTO_INCREMENT,
    app_name       VARCHAR(100) NOT NULL,
    app_key        VARCHAR(64)  NOT NULL,
    app_secret     VARCHAR(128) NOT NULL,
    allowed_apis   TEXT         DEFAULT NULL COMMENT '授权接口列表JSON',
    rate_limit     INT          DEFAULT 100 COMMENT '频率限制(次/分钟)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (app_id),
    UNIQUE KEY uk_app_key (app_key)
) ENGINE=InnoDB COMMENT='OpenAPI应用表';

-- API调用日志
CREATE TABLE openapi_log (
    log_id         BIGINT       NOT NULL AUTO_INCREMENT,
    app_id         BIGINT       DEFAULT NULL,
    app_key        VARCHAR(64)  DEFAULT '',
    api_path       VARCHAR(255) NOT NULL,
    request_method VARCHAR(10)  NOT NULL,
    request_params TEXT         DEFAULT NULL,
    response_code  INT          DEFAULT 0,
    response_time  INT          DEFAULT 0 COMMENT '响应时间(ms)',
    caller_ip      VARCHAR(128) DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (log_id),
    KEY idx_app_time (app_id, create_time)
) ENGINE=InnoDB COMMENT='API调用日志表';

-- 数据同步记录
CREATE TABLE integration_sync_log (
    sync_id        BIGINT       NOT NULL AUTO_INCREMENT,
    config_id      BIGINT       NOT NULL,
    sync_type      VARCHAR(30)  NOT NULL COMMENT '同步类型',
    direction      CHAR(1)      NOT NULL COMMENT '方向(0入站 1出站)',
    record_count   INT          DEFAULT 0 COMMENT '同步记录数',
    success_count  INT          DEFAULT 0,
    fail_count     INT          DEFAULT 0,
    error_detail   TEXT         DEFAULT NULL,
    start_time     DATETIME     NOT NULL,
    end_time       DATETIME     DEFAULT NULL,
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0进行中 1成功 2部分成功 3失败)',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (sync_id)
) ENGINE=InnoDB COMMENT='数据同步日志表';
```

---

## 6. 前端新增页面

```
views/report/
├── dashboard/
│   ├── executive.vue           # 高管驾驶舱
│   ├── hr.vue                  # HR驾驶舱
│   ├── finance.vue             # 财务驾驶舱
│   └── custom/
│       ├── index.vue           # 自定义仪表盘列表
│       └── editor.vue          # 仪表盘编辑器（拖拽布局）
├── report/
│   ├── index.vue               # 报表中心
│   └── viewer.vue              # 报表查看
├── alert/
│   ├── rules.vue               # 预警规则配置
│   └── logs.vue                # 预警记录
└── analytics/
    └── index.vue               # 智能分析（对话式）

views/ai/
├── assistant.vue               # AI智能助手（全局悬浮入口 + 对话面板）
├── knowledge.vue               # 知识库管理

views/admin/
├── integration/
│   ├── index.vue               # 集成管理
│   └── sync-log.vue            # 同步日志
├── openapi/
│   ├── apps.vue                # API应用管理
│   └── logs.vue                # API调用日志
└── monitor/
    └── index.vue               # 系统监控

# 移动端项目（独立）
code/mobile/                    # UniApp项目
├── pages/
│   ├── login/login.vue
│   ├── home/index.vue          # 工作台
│   ├── todo/index.vue          # 待办
│   ├── approval/
│   │   ├── list.vue
│   │   └── detail.vue
│   ├── attendance/
│   │   └── clock.vue           # 打卡
│   ├── leave/
│   │   ├── apply.vue
│   │   └── list.vue
│   ├── notice/list.vue
│   ├── contacts/index.vue
│   ├── im/
│   │   ├── list.vue
│   │   └── chat.vue
│   ├── document/index.vue
│   ├── schedule/index.vue
│   └── mine/index.vue          # 个人中心
├── components/
├── store/
├── api/
└── utils/
```

---

## 7. 成功标准

- 高管驾驶舱能实时展示核心经营指标，数据准确
- 自定义仪表盘支持拖拽布局，至少8种图表组件
- 智能问答能回答企业制度、流程和数据查询三类问题
- 智能预警能自动检测异常并通知相关人员
- 移动端能完成核心办公操作（审批、打卡、请假、通讯录）
- OpenAPI能安全地对外提供数据接口，有调用统计和限流
- 系统能与至少一种外部系统（企微/钉钉/ERP）实现数据互通
