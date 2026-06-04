# 会议管理 oa-meeting 重构实施与任务拆分

> 日期: 2026-06-03  
> 范围: oa-meeting 模块（会议室 / 预定 / 签到 / 纪要决议 / 周期性会议 / 提醒）  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 第三章 3.4.1 节  
> 参考模板: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 模块目标

完成企业 OA 会议管理（`oa-meeting`）从旧实现到 DDD + 微内核架构的全面重构，聚焦会议全生命周期（会议室 → 预定 → 签到 → 纪要决议 → 跟进任务 → 提醒），不引入外部视频/直播 SDK。

完成后应具备：

1. 6 张核心表（`mt_room` / `mt_booking` / `mt_participant` / `mt_signin` / `mt_resolution` / `mt_recurring`）的标准化 DDL 与初始数据。
2. `oa-meeting` 模块内会议室 CRUD、预定 + 冲突检测、签到（含扫码/定位占位）、纪要/决议、转任务、提醒、周期性会议、报表等能力。
3. 与 `oa-message` / `oa-workflow` 联动：会议提醒推送、审批流程接入、决议派发为待办任务。
4. Web 管理端与移动端均能完成会议室预定、签到、查看纪要、跟进决议。
5. 后端 mvn 测试、前端 typecheck/build、移动端 H5 build 通过。

### 1.1 重点能力清单

| 能力 | 简述 |
|------|------|
| 会议室预定 | 选会议室 + 时间段 + 参与人 + 会议主题 |
| 冲突检测 | 同房间时间段重叠检测，预订/改期自动拒绝冲突 |
| 签到 | 二维码签到 + GPS 定位签到，支持补签 |
| 会议纪要 | 会后录入/上传会议纪要，可关联附件 |
| 决议派发 | 决议可指派负责人、截止日期，并一键转 `task_item` |
| 自动跟进任务 | 决议转任务后由 `oa-task` 创建条目并提醒 |
| 周期性会议 | 周/双周/月循环模板，自动批量生成预订 |
| 提前提醒 | 提前 X 分钟推送（默认 15 分钟）到站内 + 企业微信 |

### 1.2 不在本期范围

| 不包含 | 原因 |
|--------|------|
| 视频会议 SDK 接入（腾讯会议/Zoom） | 仅留 `mt_meeting_link` 字段占位，外部接入独立任务 |
| 会议直播 | 与传统 OA 场景弱相关，避免范围蔓延 |
| OCR 纪要、智能总结 | 留给 AI 增强阶段（后续 `oa-knowledge` 或独立模块） |
| 会议室硬件控制（门禁、灯光、投屏） | 需 IoT 集成，本期不实现 |

---

## 2. 边界

### 2.1 本期包含

| 区域 | 内容 |
|------|------|
| 数据库 | 6 张核心表（`mt_room` / `mt_booking` / `mt_participant` / `mt_signin` / `mt_resolution` / `mt_recurring`） |
| 后端 | `oa-meeting` 模块（entity/mapper/service/controller/dto/vo/event） |
| 联动 | `oa-message` 站内推送 + `oa-workflow` 审批回调 + `oa-task` 决议转任务 |
| Web | 会议室管理、预定、签到、纪要、决议、报表 |
| 移动端 | 会议预定、签到、查看纪要/决议 |
| 测试 | Service 单元测试、Controller 测试、关键前端构建 |

### 2.2 本期不包含

| 不包含 | 原因 |
|--------|------|
| 视频会议 SDK 集成 | 仅占位接口（`meetingLink`） |
| 会议直播 | 范围过大 |
| 会议室 IoT 硬件联动 | 需独立硬件投入 |
| 复杂权限矩阵（按部门/职级预定） | 简化为「部门可见 + 管理员全控」 |
| 与 HR 考勤深度联动 | 仅在会议时段内自动标记「会议」状态，复杂规则留二期 |
| ES 全文检索会议纪要 | 留待 `oa-knowledge` 二期整合 |

---

## 3. 数据模型 DDL

DDL 草案存放路径：`code/backend/sql/mt_meeting_contract.sql`（契约文件，正式合并到 `baseline/001_schema.sql` 前不在生产执行）。

### 3.1 会议室表 `mt_room`

```sql
CREATE TABLE `mt_room` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT          COMMENT '会议室ID',
  `room_code`         VARCHAR(64)   NOT NULL                         COMMENT '会议室编码(如: MT-3F-01)',
  `name`              VARCHAR(128)  NOT NULL                         COMMENT '会议室名称',
  `location`          VARCHAR(200)  DEFAULT NULL                     COMMENT '位置描述',
  `floor`             VARCHAR(32)   DEFAULT NULL                     COMMENT '楼层',
  `capacity`          INT           NOT NULL DEFAULT 0               COMMENT '容纳人数',
  `devices`           JSON          DEFAULT NULL                     COMMENT '设备列表[{"type":"PROJECTOR","qty":1},...]',
  `gps`               VARCHAR(64)   DEFAULT NULL                     COMMENT 'GPS坐标(经度,纬度)',
  `signin_radius_m`   INT           DEFAULT 200                      COMMENT '签到有效半径(米)',
  `need_approval`     TINYINT       NOT NULL DEFAULT 0               COMMENT '是否需要审批(0否 1是)',
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'ENABLED'       COMMENT '状态(ENABLED/MAINTENANCE/DISABLED)',
  `description`       VARCHAR(500)  DEFAULT NULL                     COMMENT '描述',
  `sort_order`        INT           NOT NULL DEFAULT 0               COMMENT '排序号',
  `del_flag`          CHAR(1)       NOT NULL DEFAULT '0'             COMMENT '删除标志(0否 1是)',
  `create_by`         VARCHAR(64)   DEFAULT NULL,
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`         VARCHAR(64)   DEFAULT NULL,
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_room_code` (`room_code`),
  KEY `idx_mt_room_status` (`status`),
  KEY `idx_mt_room_floor` (`floor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';
```

### 3.2 会议预定表 `mt_booking`

```sql
CREATE TABLE `mt_booking` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT              COMMENT '预定ID',
  `booking_no`            VARCHAR(64)   NOT NULL                             COMMENT '会议编号(MTyyyyMMddHHmmssXXXX)',
  `room_id`               BIGINT        NOT NULL                             COMMENT '会议室ID',
  `title`                 VARCHAR(200)  NOT NULL                             COMMENT '会议主题',
  `agenda`                TEXT          DEFAULT NULL                         COMMENT '会议议程',
  `book_emp_id`           BIGINT        NOT NULL                             COMMENT '预订人ID',
  `book_dept_id`          BIGINT        DEFAULT NULL                         COMMENT '预订人部门ID',
  `start_time`            DATETIME      NOT NULL                             COMMENT '开始时间',
  `end_time`              DATETIME      NOT NULL                             COMMENT '结束时间',
  `participant_ids`       JSON          DEFAULT NULL                         COMMENT '参与人ID列表([1001,1002,...])',
  `external_participants` VARCHAR(2000) DEFAULT NULL                         COMMENT '外部参与人(姓名/单位/联系方式)',
  `meeting_link`          VARCHAR(512)  DEFAULT NULL                         COMMENT '视频会议链接(占位)',
  `remind_minutes`        INT           NOT NULL DEFAULT 15                  COMMENT '提前提醒分钟数',
  `remind_sent`           TINYINT       NOT NULL DEFAULT 0                   COMMENT '是否已发送提醒(0否 1是)',
  `status`                VARCHAR(16)   NOT NULL DEFAULT 'DRAFT'             COMMENT '状态(DRAFT/PENDING/RUNNING/FINISHED/CANCELED/REJECTED)',
  `signin_open`           TINYINT       NOT NULL DEFAULT 0                   COMMENT '是否开启签到(0否 1是)',
  `process_instance_id`   BIGINT        DEFAULT NULL                         COMMENT '工作流实例ID',
  `recurring_id`          BIGINT        DEFAULT NULL                         COMMENT '周期性会议模板ID',
  `parent_booking_id`     BIGINT        DEFAULT NULL                         COMMENT '父预定ID(由模板生成)',
  `cancel_reason`         VARCHAR(500)  DEFAULT NULL                         COMMENT '取消原因',
  `del_flag`              CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`             VARCHAR(64)   DEFAULT NULL,
  `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`             VARCHAR(64)   DEFAULT NULL,
  `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_booking_no` (`booking_no`),
  KEY `idx_mt_booking_room_time` (`room_id`, `start_time`, `end_time`),
  KEY `idx_mt_booking_book_emp` (`book_emp_id`, `status`),
  KEY `idx_mt_booking_dept` (`book_dept_id`, `start_time`),
  KEY `idx_mt_booking_status_time` (`status`, `start_time`),
  KEY `idx_mt_booking_process` (`process_instance_id`),
  KEY `idx_mt_booking_recurring` (`recurring_id`),
  KEY `idx_mt_booking_parent` (`parent_booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议预定表';
```

### 3.3 会议参与人表 `mt_participant`

```sql
CREATE TABLE `mt_participant` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT            COMMENT 'ID',
  `booking_id`      BIGINT       NOT NULL                           COMMENT '预定ID',
  `emp_id`          BIGINT       NOT NULL                           COMMENT '员工ID',
  `role`            VARCHAR(16)  NOT NULL DEFAULT 'ATTENDEE'        COMMENT '角色(ORGANIZER/PRESIDER/ATTENDEE/NOTE_TAKER)',
  `required`        TINYINT      NOT NULL DEFAULT 1                 COMMENT '是否必须参加(0否 1是)',
  `signin_status`   VARCHAR(16)  NOT NULL DEFAULT 'PENDING'         COMMENT '签到状态(PENDING/SIGNED/LATE/ABSENT/EXCUSED)',
  `signin_time`     DATETIME     DEFAULT NULL                       COMMENT '签到时间',
  `response_status` VARCHAR(16)  NOT NULL DEFAULT 'PENDING'         COMMENT '回复状态(PENDING/ACCEPTED/DECLINED/TENTATIVE)',
  `response_time`   DATETIME     DEFAULT NULL                       COMMENT '回复时间',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0',
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_participant_booking_emp` (`booking_id`, `emp_id`),
  KEY `idx_mt_participant_emp` (`emp_id`),
  KEY `idx_mt_participant_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参与人表';
```

### 3.4 会议签到表 `mt_signin`

```sql
CREATE TABLE `mt_signin` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '签到ID',
  `booking_id`      BIGINT       NOT NULL                             COMMENT '预定ID',
  `emp_id`          BIGINT       NOT NULL                             COMMENT '签到员工ID',
  `signin_type`     VARCHAR(16)  NOT NULL DEFAULT 'QR'                COMMENT '签到类型(QR/GPS/MANUAL/IMPORT)',
  `signin_time`     DATETIME     NOT NULL                             COMMENT '签到时间',
  `signin_lat`      DECIMAL(10,6) DEFAULT NULL                        COMMENT '签到纬度',
  `signin_lng`      DECIMAL(10,6) DEFAULT NULL                        COMMENT '签到经度',
  `distance_m`      INT          DEFAULT NULL                         COMMENT '距会议室距离(米)',
  `device`          VARCHAR(64)  DEFAULT NULL                         COMMENT '签到设备/UA',
  `qr_token`        VARCHAR(64)  DEFAULT NULL                         COMMENT '二维码token',
  `late_flag`       TINYINT      NOT NULL DEFAULT 0                   COMMENT '是否迟到(0否 1是)',
  `supplement_flag` TINYINT      NOT NULL DEFAULT 0                   COMMENT '是否补签(0否 1是)',
  `supplement_by`   BIGINT       DEFAULT NULL                         COMMENT '补签操作人ID',
  `supplement_reason` VARCHAR(255) DEFAULT NULL                       COMMENT '补签原因',
  `remark`          VARCHAR(255) DEFAULT NULL,
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mt_signin_booking_emp` (`booking_id`, `emp_id`),
  KEY `idx_mt_signin_emp_time` (`emp_id`, `signin_time`),
  KEY `idx_mt_signin_booking_time` (`booking_id`, `signin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议签到表';
```

### 3.5 会议决议表 `mt_resolution`

```sql
CREATE TABLE `mt_resolution` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '决议ID',
  `booking_id`      BIGINT       NOT NULL                             COMMENT '预定ID',
  `content`         TEXT         NOT NULL                             COMMENT '决议内容',
  `assignee_id`     BIGINT       NOT NULL                             COMMENT '负责人ID',
  `assignee_name`   VARCHAR(64)  DEFAULT NULL                         COMMENT '负责人姓名(冗余)',
  `due_date`        DATE         NOT NULL                             COMMENT '截止日期',
  `priority`        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL'            COMMENT '优先级(LOW/NORMAL/HIGH/URGENT)',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'TODO'              COMMENT '状态(TODO/IN_PROGRESS/DONE/OVERDUE/CANCELED)',
  `progress`        INT          NOT NULL DEFAULT 0                   COMMENT '完成进度(0-100)',
  `completed_time`  DATETIME     DEFAULT NULL                         COMMENT '完成时间',
  `related_task_id` BIGINT       DEFAULT NULL                         COMMENT '关联任务ID(由oa-task创建)',
  `relate_resolution_id` BIGINT   DEFAULT NULL                         COMMENT '原始决议ID(转任务后的追踪)',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0',
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mt_resolution_booking` (`booking_id`),
  KEY `idx_mt_resolution_assignee_status` (`assignee_id`, `status`),
  KEY `idx_mt_resolution_due` (`status`, `due_date`),
  KEY `idx_mt_resolution_task` (`related_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议决议表';
```

### 3.6 周期性会议模板表 `mt_recurring`

```sql
CREATE TABLE `mt_recurring` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '模板ID',
  `template_name`   VARCHAR(128) NOT NULL                             COMMENT '模板名称',
  `room_id`         BIGINT       NOT NULL                             COMMENT '会议室ID',
  `title`           VARCHAR(200) NOT NULL                             COMMENT '会议主题',
  `agenda`          TEXT         DEFAULT NULL                         COMMENT '议程',
  `frequency`       VARCHAR(16)  NOT NULL                             COMMENT '周期(DAILY/WEEKLY/BIWEEKLY/MONTHLY)',
  `weekday_mask`    VARCHAR(32)  DEFAULT NULL                         COMMENT '周掩码(1-7拼接,如"1,3,5")',
  `day_of_month`    INT          DEFAULT NULL                         COMMENT '每月几号(MONTHLY有效)',
  `start_time_of_day` TIME        NOT NULL                             COMMENT '每日开始时间',
  `end_time_of_day` TIME          NOT NULL                             COMMENT '每日结束时间',
  `effective_from`  DATE         NOT NULL                             COMMENT '生效开始日期',
  `effective_to`    DATE         DEFAULT NULL                         COMMENT '生效结束日期',
  `participant_ids` JSON         DEFAULT NULL                         COMMENT '参与人列表',
  `remind_minutes`  INT          NOT NULL DEFAULT 15                  COMMENT '提前提醒分钟',
  `book_emp_id`     BIGINT       NOT NULL                             COMMENT '创建人',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'            COMMENT '状态(ACTIVE/PAUSED/ENDED)',
  `next_gen_date`   DATE         DEFAULT NULL                         COMMENT '下次生成预订日期',
  `last_gen_date`   DATE         DEFAULT NULL                         COMMENT '上次生成日期',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0',
  `create_by`       VARCHAR(64)  DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       VARCHAR(64)  DEFAULT NULL,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_mt_recurring_room` (`room_id`),
  KEY `idx_mt_recurring_status_next` (`status`, `next_gen_date`),
  KEY `idx_mt_recurring_book_emp` (`book_emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期性会议模板表';
```

### 3.7 字段补充说明

- `mt_booking.status` 状态机：`DRAFT → PENDING → RUNNING → FINISHED`，并允许 `CANCELED`（任何阶段可取消）和 `REJECTED`（审批驳回）。
- `mt_participant.signin_status` 与 `mt_signin` 关系：`mt_participant` 记录预期参与人与最终签到状态；`mt_signin` 记录每一次签到流水（仅 `QR/GPS/MANUAL/IMPORT` 入口）。
- `mt_resolution.related_task_id` 关联 `task_item.id`，通过 `oa-task` API 创建。
- `mt_recurring.next_gen_date` 由定时任务每日扫描，生成未来 30 天内的预订。

---

## 4. 任务波次

### Wave 1: 房间与基础

#### T1 数据库与 API 契约

| 字段 | 内容 |
|------|------|
| 目标 | 定义会议管理 6 张核心表、API 契约、权限码、DTO/VO |
| 路径 | `code/backend/sql/mt_meeting_contract.sql`、`docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 3.4.1 |
| 输入 | 重构文档 1.x/2.x/3.4.1 节；旧 `mt_room`/`mt_booking`/`mt_signin`/`mt_resolution` |
| 输出 | DDL/seed 草案、API 契约表、权限码清单 |
| 禁止修改 | 不实现 Service/Controller 业务逻辑 |
| 验收 | 文档列出接口、字段、索引、权限码、冲突检测算法、验收命令 |

#### T2 旧实现影响分析

| 字段 | 内容 |
|------|------|
| 目标 | 查清旧会议实体的字段、Mapper、Service、Controller、前端/移动端依赖 |
| 路径 | `code/backend/oa-meeting/**`、`code/frontend/src/api/meeting*.ts`、`code/mobile/src/api/meeting*.ts` |
| 输出 | 旧入口清单、迁移保留/替换/下线建议 |
| 禁止修改 | 不删除旧代码 |
| 验收 | 影响分析文档或本文件追加清单 |

### Wave 2: 后端核心（房间 + 预定 + 冲突检测）

#### T3 Entity 与 Mapper

| 字段 | 内容 |
|------|------|
| 目标 | 在 `oa-meeting` 中建立 6 张表对应的 Entity、DTO/VO、Enum、Mapper |
| 路径 | `code/backend/oa-meeting`、`code/backend/sql` |
| 输出 | Entity、Mapper、基础查询方法、Mapper 测试或集成测试 |
| 禁止修改 | 不修改前端、移动端；不重写 `oa-task`、`oa-message`、`oa-workflow` 核心 |
| 验收 | `cd code/backend && mvn -pl oa-meeting -am test` |

#### T4 Service（含冲突检测算法）

| 字段 | 内容 |
|------|------|
| 目标 | 实现房间 CRUD、预定/取消/改期、冲突检测、签到、纪要、决议、提醒、周期性 |
| 路径 | `code/backend/oa-meeting` |
| 输出 | Service 接口/实现、DTO/VO、单元测试 |
| 禁止修改 | 不直接操作 `task_*` / `msg_*` 内部表；通过 API/事件接口 |
| 验收 | `cd code/backend && mvn -pl oa-meeting -am test` |

#### T5 REST API

| 字段 | 内容 |
|------|------|
| 目标 | 暴露房间、预定、签到、纪要、决议、提醒、周期性 REST API |
| 路径 | `code/backend/oa-meeting` 或按当前模块设计的 API 包 |
| 输出 | Controller、OpenAPI 注解、权限注解、Controller 测试 |
| 禁止修改 | 不复制旧 Controller 大段逻辑 |
| 验收 | `cd code/backend && mvn -pl oa-meeting,oa-web -am test` |

### Wave 3: 联动与扩展

#### T6 工作流回调接入

| 字段 | 内容 |
|------|------|
| 目标 | 会议预定可走审批流程，审批通过/驳回/撤回回调会议业务状态 |
| 路径 | `code/backend/oa-workflow`、`code/backend/oa-meeting` |
| 输出 | 回调 Handler、事件、集成测试 |
| 禁止修改 | 不让 workflow core 依赖 oa-meeting 实现类 |
| 验收 | `cd code/backend && mvn -pl oa-workflow/oa-workflow-core,oa-meeting,oa-web -am test` |

#### T7 消息中心联动

| 字段 | 内容 |
|------|------|
| 目标 | 会议提醒、参与人邀请、签到结果、决议派发均通过 oa-message 推送 |
| 路径 | `code/backend/oa-message`、`code/backend/oa-meeting` |
| 输出 | 事件发布、消息模板、WebSocket 推送、集成测试 |
| 禁止修改 | 不实现具体短信/邮件外部渠道，先保证站内消息/WebSocket |
| 验收 | `cd code/backend && mvn -pl oa-message,oa-meeting,oa-web -am test` |

#### T8 决议转任务联动

| 字段 | 内容 |
|------|------|
| 目标 | 会议决议一键转为 oa-task 任务，并回写 `related_task_id` |
| 路径 | `code/backend/oa-task`、`code/backend/oa-meeting` |
| 输出 | 任务创建调用、状态同步回调 |
| 禁止修改 | 不修改 oa-task 内部算法；走 oa-task 对外 API |
| 验收 | `cd code/backend && mvn -pl oa-task,oa-meeting,oa-web -am test` |

### Wave 4: Web 与移动端

#### T9 Web API 与页面迁移

| 字段 | 内容 |
|------|------|
| 目标 | Web 管理端接入新会议接口（房间管理、预定、签到、纪要、决议、报表） |
| 路径 | `code/frontend/src/api/meeting.ts`、`code/frontend/src/views/oa/meeting/**` |
| 输出 | typed API、申请页、审批页、列表页、签到页、纪要页、报表 |
| 禁止修改 | 不做 monorepo 改造，不重构全局布局 |
| 验收 | `cd code/frontend && pnpm typecheck && pnpm build` |

#### T10 Mobile API 与页面迁移

| 字段 | 内容 |
|------|------|
| 目标 | 移动端接入预定、签到、纪要/决议查看 |
| 路径 | `code/mobile/src/api/meeting.ts`、`code/mobile/src/pages/oa/meeting/**` |
| 输出 | typed API、移动端表单、列表、签到、纪要/决议 |
| 禁止修改 | 不实现复杂管理配置页面 |
| 验收 | `cd code/mobile && pnpm build:h5` |

### Wave 5: 验证与下线

#### T11 端到端回归与下线准备

| 字段 | 内容 |
|------|------|
| 目标 | 端到端验证会议全链路；标记旧入口下线清单 |
| 路径 | `tests/`、`code/backend/src/test` 或现有测试目录 |
| 输出 | API/E2E 测试或手工验证脚本；下线清单、兼容策略、风险说明 |
| 禁止修改 | 未通过 E2E 前不删除旧代码 |
| 验收 | 登录 → 预订会议室 → 冲突检测 → 审批 → 提醒 → 签到 → 纪要 → 决议转任务 全部通过 |

---

## 5. 推荐执行顺序

```
Wave 1: T1 + T2
Wave 2: T3 -> T4 -> T5
Wave 3: T6 -> T7 -> T8
Wave 4: T9 与 T10 可并行
Wave 5: T11
```

T1/T2 完成前不得开始代码实现。T6/T7/T8 完成前，Web/Mobile 可以先做 API 类型和页面静态结构，但不能宣称闭环完成。

---

## 6. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| Meeting 后端 | `cd code/backend && mvn -pl oa-meeting -am test` |
| Meeting + Web 入口 | `cd code/backend && mvn -pl oa-meeting,oa-web -am test` |
| 工作流联动 | `cd code/backend && mvn -pl oa-workflow/oa-workflow-core,oa-meeting,oa-web -am test` |
| 消息联动 | `cd code/backend && mvn -pl oa-message,oa-meeting,oa-web -am test` |
| 任务联动 | `cd code/backend && mvn -pl oa-task,oa-meeting,oa-web -am test` |
| Web | `cd code/frontend && pnpm typecheck && pnpm build` |
| Mobile | `cd code/mobile && pnpm build:h5` |

---

## 7. T1 数据库与 API 契约结果

### 7.1 旧实现摘要

| 项目 | 旧实现 |
|------|--------|
| 旧会议室表 | `mt_room`（name, capacity, devices, location, gps, status 数字枚举） |
| 旧预定表 | `mt_booking`（room_id, title, start_time, end_time, participants JSON, status 数字枚举） |
| 旧签到表 | `mt_signin`（booking_id, emp_id, signin_time, signin_type 数字） |
| 旧决议表 | `mt_resolution`（booking_id, content, assignee_id, due_date, status 数字） |
| 旧后端入口 | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtBookingController.java` 等 |
| 旧服务 | `MtMeetingService`（含 book/cancel/pageQuery/getResolutions） |
| 旧 Web API | `code/frontend/src/api/meeting*.ts`（`/api/meeting/bookings`） |
| 旧 Mobile API | `code/mobile/src/api/meeting*.ts` |

旧逻辑需要保留的能力：

1. 会议室基础信息维护（增删改查、状态）。
2. 会议预定、取消、改期。
3. 签到记录（含补签）。
4. 会议纪要/决议派发。
5. 旧实现状态全部使用数字枚举（0/1/2/3），新实现需改为字符串枚举（`DRAFT/PENDING/RUNNING/FINISHED/CANCELED/REJECTED`）。

### 7.2 SQL 草案

T1 新增 SQL 草案文件：

`code/backend/sql/mt_meeting_contract.sql`

该文件当前只作为契约草案，不直接替换 `oa_system_full.sql` 或 `oa_system_extensions.sql`。确认后在后续 T3/T4 合并进正式 baseline。

包含：

| 表 | 说明 |
|----|------|
| `mt_room` | 会议室（新增 `room_code`、`floor`、`signin_radius_m`、`need_approval`、`sort_order`） |
| `mt_booking` | 预定（新增 `booking_no`、`agenda`、`external_participants`、`remind_minutes/remind_sent`、`recurring_id/parent_booking_id`、`signin_open`） |
| `mt_participant` | 参与人（新表，独立记录预期参与人和签到状态） |
| `mt_signin` | 签到（新增 `signin_type` 字符串枚举、`distance_m`、`qr_token`、`late_flag`、`supplement_*`） |
| `mt_resolution` | 决议（新增字符串枚举状态、`priority`、`progress`、`related_task_id`） |
| `mt_recurring` | 周期性会议模板（新表） |

### 7.3 表结构要点

#### `mt_room` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `room_code` | VARCHAR(64) | 会议室编码，全局唯一 |
| `name` | VARCHAR(128) | 名称 |
| `location` | VARCHAR(200) | 位置描述 |
| `floor` | VARCHAR(32) | 楼层 |
| `capacity` | INT | 容纳人数 |
| `devices` | JSON | 设备列表 |
| `gps` | VARCHAR(64) | GPS 坐标 |
| `signin_radius_m` | INT | 签到有效半径（米） |
| `need_approval` | TINYINT | 是否需要审批 |
| `status` | VARCHAR(16) | `ENABLED/MAINTENANCE/DISABLED` |

#### `mt_booking` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `booking_no` | VARCHAR(64) | 会议编号，唯一 |
| `room_id` | BIGINT | 会议室 ID |
| `title` | VARCHAR(200) | 会议主题 |
| `start_time` / `end_time` | DATETIME | 起止时间 |
| `participant_ids` | JSON | 参与人 ID 列表 |
| `external_participants` | VARCHAR(2000) | 外部参与人 |
| `meeting_link` | VARCHAR(512) | 视频会议链接（占位） |
| `remind_minutes` | INT | 提前提醒分钟数 |
| `status` | VARCHAR(16) | `DRAFT/PENDING/RUNNING/FINISHED/CANCELED/REJECTED` |
| `process_instance_id` | BIGINT | 工作流实例 ID |
| `recurring_id` | BIGINT | 周期性模板 ID |
| `parent_booking_id` | BIGINT | 父预定 ID |

#### `mt_participant` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `booking_id` | BIGINT | 预定 ID |
| `emp_id` | BIGINT | 员工 ID |
| `role` | VARCHAR(16) | `ORGANIZER/PRESIDER/ATTENDEE/NOTE_TAKER` |
| `required` | TINYINT | 必须参加 |
| `signin_status` | VARCHAR(16) | `PENDING/SIGNED/LATE/ABSENT/EXCUSED` |
| `response_status` | VARCHAR(16) | `PENDING/ACCEPTED/DECLINED/TENTATIVE` |

#### `mt_signin` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `booking_id` | BIGINT | 预定 ID |
| `emp_id` | BIGINT | 员工 ID |
| `signin_type` | VARCHAR(16) | `QR/GPS/MANUAL/IMPORT` |
| `signin_time` | DATETIME | 签到时间 |
| `signin_lat` / `signin_lng` | DECIMAL(10,6) | 经纬度 |
| `distance_m` | INT | 距会议室距离 |
| `qr_token` | VARCHAR(64) | 二维码 token |
| `late_flag` | TINYINT | 是否迟到 |
| `supplement_flag` | TINYINT | 是否补签 |
| `supplement_by` / `supplement_reason` | - | 补签信息 |

#### `mt_resolution` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `booking_id` | BIGINT | 预定 ID |
| `content` | TEXT | 决议内容 |
| `assignee_id` | BIGINT | 负责人 ID |
| `due_date` | DATE | 截止日期 |
| `priority` | VARCHAR(16) | `LOW/NORMAL/HIGH/URGENT` |
| `status` | VARCHAR(16) | `TODO/IN_PROGRESS/DONE/OVERDUE/CANCELED` |
| `progress` | INT | 0-100 |
| `related_task_id` | BIGINT | 关联任务 ID |

#### `mt_recurring` 要点

| 字段 | 类型 | 说明 |
|------|------|------|
| `frequency` | VARCHAR(16) | `DAILY/WEEKLY/BIWEEKLY/MONTHLY` |
| `weekday_mask` | VARCHAR(32) | 周掩码如 `1,3,5` |
| `day_of_month` | INT | MONTHLY 模式有效 |
| `start_time_of_day` / `end_time_of_day` | TIME | 每日开始/结束时间 |
| `effective_from` / `effective_to` | DATE | 生效区间 |
| `status` | VARCHAR(16) | `ACTIVE/PAUSED/ENDED` |
| `next_gen_date` / `last_gen_date` | DATE | 定时任务生成游标 |

### 7.4 冲突检测算法

会议冲突定义：同一 `room_id` 下，存在任意一笔 **非 CANCELED/REJECTED** 的预定，与新预定的 `[start_time, end_time)` 区间重叠。

区间重叠的判断：

```
给定 [s1, e1) 与 [s2, e2)：
  重叠 iff s1 < e2 AND s2 < e1
```

冲突检测 SQL（伪代码）：

```sql
SELECT id, booking_no, title, start_time, end_time
FROM mt_booking
WHERE room_id = #{roomId}
  AND del_flag = '0'
  AND status NOT IN ('CANCELED', 'REJECTED')
  AND start_time < #{endTime}
  AND end_time   > #{startTime}
LIMIT 50;
```

并发安全建议：

| 方案 | 说明 |
|------|------|
| 唯一索引 + 重试 | 无法直接加唯一索引（区间列） |
| 悲观锁 | `SELECT ... FOR UPDATE` 在事务内串行化（推荐） |
| 乐观锁 + 重试 | 插入后查询是否冲突，冲突回滚重试 |
| Redis 分布式锁 | 按 `roomId` 加锁，TTL 短（不推荐作为唯一手段） |

推荐实现：

1. 进入预定事务。
2. 对 `mt_booking WHERE room_id = ? AND status IN (...) AND start_time < ? AND end_time > ?` 加 `FOR UPDATE`。
3. 若存在记录 → 抛 `BusinessError("会议室在该时段已被占用")`。
4. 若不存在 → 插入新预定并提交。

### 7.5 API 契约

统一前缀：`/api/meeting`

#### 房间管理

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/rooms` | `meeting:room:create` | 创建会议室 |
| `PUT` | `/api/meeting/rooms/{id}` | `meeting:room:update` | 更新会议室 |
| `DELETE` | `/api/meeting/rooms/{id}` | `meeting:room:delete` | 删除会议室（软删） |
| `GET` | `/api/meeting/rooms` | `meeting:room:list` | 分页查询会议室 |
| `GET` | `/api/meeting/rooms/{id}` | `meeting:room:detail` | 查询会议室详情 |
| `PUT` | `/api/meeting/rooms/{id}/status` | `meeting:room:status` | 修改会议室状态 |
| `GET` | `/api/meeting/rooms/availability` | `meeting:room:list` | 查询某时段可用会议室 |

#### 预定

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/bookings` | `meeting:booking:create` | 创建会议预定（含冲突检测） |
| `PUT` | `/api/meeting/bookings/{id}` | `meeting:booking:update` | 更新预定（标题/议程/参与人） |
| `POST` | `/api/meeting/bookings/{id}/submit` | `meeting:booking:submit` | 提交审批（如 `need_approval`） |
| `POST` | `/api/meeting/bookings/{id}/reschedule` | `meeting:booking:update` | 改期（时间变更） |
| `POST` | `/api/meeting/bookings/{id}/cancel` | `meeting:booking:cancel` | 取消预定 |
| `POST` | `/api/meeting/bookings/{id}/start` | `meeting:booking:update` | 标记会议开始 |
| `POST` | `/api/meeting/bookings/{id}/finish` | `meeting:booking:update` | 标记会议结束 |
| `GET` | `/api/meeting/bookings` | `meeting:booking:list` | 分页查询预定 |
| `GET` | `/api/meeting/bookings/my` | `meeting:booking:list` | 我的预定 |
| `GET` | `/api/meeting/bookings/calendar` | `meeting:booking:list` | 日历视图（按天/周） |
| `GET` | `/api/meeting/bookings/{id}` | `meeting:booking:detail` | 预定详情 |

#### 参与人

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/bookings/{id}/participants` | `meeting:booking:update` | 添加参与人 |
| `DELETE` | `/api/meeting/bookings/{id}/participants/{empId}` | `meeting:booking:update` | 移除参与人 |
| `PUT` | `/api/meeting/bookings/{id}/participants/{empId}/role` | `meeting:booking:update` | 修改参与人角色 |
| `POST` | `/api/meeting/bookings/{id}/respond` | `meeting:booking:list` | 参与人回复（接受/拒绝/待定） |

#### 签到

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/bookings/{id}/signin/qr-token` | `meeting:signin:create` | 生成签到二维码 token |
| `POST` | `/api/meeting/bookings/{id}/signin/qr` | `meeting:signin:create` | 扫码签到 |
| `POST` | `/api/meeting/bookings/{id}/signin/gps` | `meeting:signin:create` | GPS 签到 |
| `POST` | `/api/meeting/signins/{id}/supplement` | `meeting:signin:update` | 补签（管理员/会议组织者） |
| `GET` | `/api/meeting/bookings/{id}/signins` | `meeting:signin:list` | 查询会议签到列表 |
| `GET` | `/api/meeting/signins/my` | `meeting:signin:list` | 我的签到记录 |

#### 纪要/决议

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/bookings/{id}/minutes` | `meeting:minutes:create` | 创建/上传会议纪要 |
| `PUT` | `/api/meeting/minutes/{id}` | `meeting:minutes:update` | 更新纪要 |
| `GET` | `/api/meeting/bookings/{id}/minutes` | `meeting:minutes:list` | 查询会议纪要 |
| `POST` | `/api/meeting/bookings/{id}/resolutions` | `meeting:resolution:create` | 新增决议 |
| `PUT` | `/api/meeting/resolutions/{id}` | `meeting:resolution:update` | 更新决议 |
| `DELETE` | `/api/meeting/resolutions/{id}` | `meeting:resolution:delete` | 删除决议 |
| `GET` | `/api/meeting/bookings/{id}/resolutions` | `meeting:resolution:list` | 查询会议决议 |
| `POST` | `/api/meeting/resolutions/{id}/to-task` | `meeting:resolution:create` | 决议转任务 |
| `PUT` | `/api/meeting/resolutions/{id}/progress` | `meeting:resolution:update` | 更新决议进度 |

#### 周期性会议

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/recurring` | `meeting:recurring:create` | 创建周期性会议模板 |
| `PUT` | `/api/meeting/recurring/{id}` | `meeting:recurring:update` | 更新模板 |
| `PUT` | `/api/meeting/recurring/{id}/pause` | `meeting:recurring:update` | 暂停模板 |
| `PUT` | `/api/meeting/recurring/{id}/resume` | `meeting:recurring:update` | 恢复模板 |
| `DELETE` | `/api/meeting/recurring/{id}` | `meeting:recurring:delete` | 结束模板 |
| `GET` | `/api/meeting/recurring` | `meeting:recurring:list` | 模板列表 |
| `GET` | `/api/meeting/recurring/{id}` | `meeting:recurring:detail` | 模板详情 |

#### 提醒与报表

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/meeting/bookings/{id}/remind` | `meeting:booking:update` | 手动触发提醒 |
| `GET` | `/api/meeting/reports/usage` | `meeting:report:view` | 会议室使用率报表 |
| `GET` | `/api/meeting/reports/signin` | `meeting:report:view` | 签到率报表 |
| `GET` | `/api/meeting/reports/resolution` | `meeting:report:view` | 决议完成率报表 |

### 7.6 DTO/VO 字段

#### `MtBookingCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `roomId` | Long | 必填 |
| `title` | String | 必填，最长 200 |
| `agenda` | String | 可选，最长 4000 |
| `startTime` | LocalDateTime | 必填，`yyyy-MM-dd HH:mm:ss` |
| `endTime` | LocalDateTime | 必填，必须晚于 `startTime` |
| `participantIds` | List\<Long\> | 可选；不能超过会议室容量 |
| `externalParticipants` | String | 可选，最长 2000 |
| `meetingLink` | String | 可选，最长 512 |
| `remindMinutes` | Integer | 可选，默认 15，0 表示不提醒 |
| `signinOpen` | Boolean | 可选，默认 false |
| `recurringId` | Long | 可选；非空表示由模板生成 |

#### `MtBookingRescheduleDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `newStartTime` | LocalDateTime | 必填 |
| `newEndTime` | LocalDateTime | 必填，必须晚于 `newStartTime` |

#### `MtSigninQrDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `qrToken` | String | 必填，扫码得到 |
| `signinTime` | LocalDateTime | 可选，默认服务器时间 |

#### `MtSigninGpsDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `latitude` | BigDecimal | 必填，范围 [-90, 90] |
| `longitude` | BigDecimal | 必填，范围 [-180, 180] |
| `accuracy` | BigDecimal | 可选，米 |

#### `MtSigninSupplementDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `signinTime` | LocalDateTime | 必填 |
| `reason` | String | 必填，最长 255 |

#### `MtResolutionCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `bookingId` | Long | 必填 |
| `content` | String | 必填，最长 4000 |
| `assigneeId` | Long | 必填 |
| `dueDate` | LocalDate | 必填 |
| `priority` | String | 必填，枚举值 |
| `toTask` | Boolean | 可选，是否同步转任务 |

#### `MtRecurringSaveDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `templateName` | String | 必填 |
| `roomId` | Long | 必填 |
| `title` | String | 必填 |
| `agenda` | String | 可选 |
| `frequency` | String | 必填，枚举值 |
| `weekdayMask` | String | WEEKLY/BIWEEKLY 必填，如 `1,3,5` |
| `dayOfMonth` | Integer | MONTHLY 必填，1-31 |
| `startTimeOfDay` | LocalTime | 必填 |
| `endTimeOfDay` | LocalTime | 必填，必须晚于 `startTimeOfDay` |
| `effectiveFrom` | LocalDate | 必填 |
| `effectiveTo` | LocalDate | 可选 |
| `participantIds` | List\<Long\> | 可选 |
| `remindMinutes` | Integer | 可选，默认 15 |

#### `MtBookingVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 预定 ID |
| `bookingNo` | String | 会议编号 |
| `roomId` / `roomName` / `roomLocation` | - | 会议室 |
| `title` | String | 主题 |
| `startTime` / `endTime` | LocalDateTime | 时间 |
| `durationMinutes` | Long | 时长（分钟） |
| `participantCount` | Integer | 参与人数 |
| `signinCount` / `lateCount` / `absentCount` | Integer | 签到统计 |
| `resolutionCount` / `completedResolutionCount` | Integer | 决议统计 |
| `status` / `statusName` | String | 状态 |
| `processInstanceId` | Long | 流程实例 |
| `canCancel` / `canEdit` / `canStart` / `canFinish` | Boolean | 当前用户可用动作 |

#### `MtRoomVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 房间 ID |
| `roomCode` / `name` | String | 编码 / 名称 |
| `location` / `floor` | String | 位置 |
| `capacity` | Integer | 容纳人数 |
| `devices` | List\<MtDevice\> | 设备列表（反序列化） |
| `signinRadiusM` | Integer | 签到半径 |
| `needApproval` | Boolean | 是否需要审批 |
| `status` / `statusName` | String | 状态 |
| `availableNow` | Boolean | 当前是否可用（实时计算） |

#### `MtResolutionVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 决议 ID |
| `bookingId` / `bookingTitle` | - | 关联预定 |
| `content` | String | 内容 |
| `assigneeId` / `assigneeName` | - | 负责人 |
| `dueDate` | LocalDate | 截止日期 |
| `priority` | String | 优先级 |
| `status` / `statusName` | String | 状态 |
| `progress` | Integer | 进度 |
| `relatedTaskId` | Long | 关联任务 |
| `overdue` | Boolean | 是否逾期（计算） |

### 7.7 索引与 EXPLAIN 验收

| 查询场景 | 索引 | 验收 |
|----------|------|------|
| 会议室可用性查询 | `idx_mt_booking_room_time` | `EXPLAIN` 命中该索引，不出现全表扫描 |
| 我的预定列表 | `idx_mt_booking_book_emp` | 命中 |
| 部门预定 | `idx_mt_booking_dept` | 命中 |
| 周期性生成 | `idx_mt_recurring_status_next` | 命中 |
| 签到流水 | `idx_mt_signin_booking_time` | 命中 |
| 我的决议 | `idx_mt_resolution_assignee_status` | 命中 |
| 决议逾期扫描 | `idx_mt_resolution_due` | 命中 |

### 7.8 后续任务输入

T3/T4 实现时必须使用以上契约，不再沿用旧的：

| 旧项 | 新项 |
|------|------|
| `mt_room` | `mt_room`（字段扩展） |
| `mt_booking` | `mt_booking`（状态改为字符串，字段扩展） |
| 无 | `mt_participant`（新表） |
| `mt_signin` | `mt_signin`（类型改为字符串，补签字段） |
| `mt_resolution` | `mt_resolution`（状态/优先级改为字符串） |
| 无 | `mt_recurring`（新表） |
| 数字状态 `0/1/2/3` | 字符串状态 `DRAFT/PENDING/RUNNING/FINISHED/CANCELED/REJECTED` |
| 数字 `signinType` | 字符串 `QR/GPS/MANUAL/IMPORT` |
| 数字 `priority` | 字符串 `LOW/NORMAL/HIGH/URGENT` |

兼容期前端可以保留旧 API 文件，但新页面和新接口必须优先使用 `/api/meeting`。

---

## 8. T2 旧实现影响分析

### 8.1 旧后端文件清单

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Entity | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtRoom.java` | 映射 `mt_room`，含 name/capacity/devices/location/gps/status 数字 | 迁移到新 `MtRoom`；新增 `roomCode/floor/signinRadiusM/needApproval` |
| Entity | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtBooking.java` | 映射 `mt_booking`，数字 status 枚举，participants JSON 字段 | 迁移字段语义，状态改字符串，扩展 `agenda/meetingLink/remindMinutes/recurringId` |
| Entity | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtSignin.java` | 映射 `mt_signin`，数字 signinType | 迁移，类型改字符串，新增 `signinLat/Lng/distanceM/qrToken/lateFlag/supplement*` |
| Entity | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtResolution.java` | 映射 `mt_resolution`，数字 status | 迁移，状态改字符串，新增 `priority/progress/relatedTaskId` |
| DTO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/dto/MtBookingCreateDTO.java` | 旧预定 DTO | 不直接复用；新建 `MtBookingCreateDTO` |
| DTO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/dto/MtBookingQueryDTO.java` | 旧查询 DTO | 迁移语义，不复用 |
| DTO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/dto/MtResolutionDTO.java` | 旧决议 DTO | 新建 `MtResolutionCreateDTO` |
| DTO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/dto/MtRoomSaveDTO.java` | 旧房间 DTO | 新建 `MtRoomSaveDTO` |
| DTO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/dto/MtSigninDTO.java` | 旧签到 DTO | 新建 `MtSigninQrDTO/MtSigninGpsDTO/MtSigninSupplementDTO` |
| Mapper | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/mapper/MtRoomMapper.java` | `BaseMapper<MtRoom>` | 迁移为新 `MtRoomMapper` |
| Mapper | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/mapper/MtBookingMapper.java` | `BaseMapper<MtBooking>` | 迁移，新增冲突检测自定义 SQL |
| Mapper | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/mapper/MtSigninMapper.java` | `BaseMapper<MtSignin>` | 迁移 |
| Mapper | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/mapper/MtResolutionMapper.java` | `BaseMapper<MtResolution>` | 迁移 |
| Service | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/service/MtMeetingService.java` | 旧服务接口（book/cancel/pageQuery/getResolutions） | 迁移为 `MtBookingService` / `MtRoomService` / `MtSigninService` / `MtResolutionService` 等 |
| Service | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/service/MtRoomService.java` | 旧房间服务 | 迁移 |
| Controller | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtBookingController.java` | `/api/meeting/bookings` 等旧接口 | 新增 `/api/meeting/...`；旧 Controller 兼容期保留 |
| Controller | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtRoomController.java` | 旧房间接口 | 同上 |
| Controller | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtSigninController.java` | 旧签到接口 | 同上 |
| VO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/vo/MtBookingVO.java` | 旧 VO | 新建新 VO 字段更丰富版本 |
| VO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/vo/MtRoomVO.java` | 旧房间 VO | 新建 |
| VO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/vo/MtSigninVO.java` | 旧签到 VO | 新建 |
| VO | `code/backend/oa-meeting/src/main/java/cn/oa/meeting/vo/MtResolutionVO.java` | 旧决议 VO | 新建 |

### 8.2 旧前端与移动端文件清单

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Web API | `code/frontend/src/api/meeting*.ts` | 调用 `/api/meeting/*` | 后续 T9 改造为 typed API；按新枚举更新 |
| Web 页面 | `code/frontend/src/views/oa/meeting/**` | 旧会议页面 | 后续 T9 接入新 API |
| Mobile API | `code/mobile/src/api/meeting*.ts` | 调用 `/api/meeting/*` | 后续 T10 改造 |
| Mobile 页面 | `code/mobile/src/pages/oa/meeting/**` | 旧会议页面 | 后续 T10 改造 |

### 8.3 新旧接口映射

| 旧接口 | 新接口 | 迁移说明 |
|--------|--------|----------|
| `POST /api/meeting/bookings` | `POST /api/meeting/bookings` | 保持路径，DTO/枚举更新 |
| `POST /api/meeting/bookings/{id}/cancel` | `POST /api/meeting/bookings/{id}/cancel` | 保持路径 |
| `GET /api/meeting/bookings/page` | `GET /api/meeting/bookings` | 路径变更为不带 `/page` |
| `GET /api/meeting/bookings/{id}` | `GET /api/meeting/bookings/{id}` | 保持 |
| `GET /api/meeting/bookings/{id}/resolutions` | `GET /api/meeting/bookings/{id}/resolutions` | 保持 |
| 旧房间 CRUD | `/api/meeting/rooms` 系列 | 路径不变，按新契约扩字段 |
| 旧签到接口 | `/api/meeting/bookings/{id}/signin/*` | 拆分为 qr/gps/supplement 子资源 |
| 旧纪要接口 | `/api/meeting/bookings/{id}/minutes` | 新增 |
| 旧周期性接口 | `/api/meeting/recurring` | 全新 |
| 旧提醒接口 | `/api/meeting/bookings/{id}/remind` | 全新 |

### 8.4 数据字段映射

| 旧字段 | 新字段 | 迁移说明 |
|--------|--------|----------|
| `mt_room.id` | `mt_room.id` | 主键保留 |
| 无 | `room_code` | 新增，编码唯一 |
| `name` | `name` | 保留 |
| `location` | `location` | 保留 |
| 无 | `floor` | 新增 |
| `capacity` | `capacity` | 保留 |
| `devices` | `devices` | 保留 |
| `gps` | `gps` | 保留 |
| 无 | `signin_radius_m` | 新增，默认 200 |
| 无 | `need_approval` | 新增 |
| `status=0/1/2` | `status=ENABLED/MAINTENANCE/DISABLED` | 字符串枚举 |
| `mt_booking.id` | `mt_booking.id` | 保留 |
| 无 | `booking_no` | 新增，格式 `MTyyyyMMddHHmmssXXXX` |
| `room_id` | `room_id` | 保留 |
| `title` | `title` | 保留 |
| 无 | `agenda` | 新增 |
| `start_time` / `end_time` | 同名 | 保留 |
| `participants` (JSON 字符串) | `participant_ids` (JSON) + `mt_participant` 表 | 拆分为独立表 |
| 无 | `external_participants` | 新增 |
| 无 | `meeting_link` | 新增（占位） |
| 无 | `remind_minutes` / `remind_sent` | 新增 |
| `status=0/1/2/3` | `status=DRAFT/PENDING/RUNNING/FINISHED/CANCELED/REJECTED` | 字符串枚举 |
| `process_instance_id` | `process_instance_id` | 保留 |
| 无 | `recurring_id` / `parent_booking_id` | 新增 |
| `mt_signin.signin_type=0/1` | `signin_type=QR/GPS/MANUAL/IMPORT` | 字符串枚举 |
| `signin_time` | `signin_time` | 保留 |
| `location` | 改由 `signin_lat/signin_lng` + `mt_room.gps` 计算 | 改为结构化字段 |
| 无 | `distance_m` | 新增 |
| 无 | `qr_token` / `late_flag` / `supplement_*` | 新增 |
| `mt_resolution.status=0/1/2/3` | `status=TODO/IN_PROGRESS/DONE/OVERDUE/CANCELED` | 字符串枚举 |
| 无 | `priority` | 新增 |
| 无 | `progress` | 新增 |
| `task_id` | `related_task_id` | 保留语义，字段重命名 |
| 无 | `relate_resolution_id` | 新增（用于追踪转任务后的对应关系） |

### 8.5 保留、迁移、下线策略

| 阶段 | 策略 |
|------|------|
| T3-T7 | 旧 `mt_*` 实现保留，新 `mt_*` 实现并行开发 |
| T8-T9 | 新页面优先调用 `/api/meeting`，旧页面/API 可作为回滚入口 |
| T10 | E2E 通过后，标记旧 `/api/meeting/bookings/page` 等为 deprecated |
| T11 | 输出旧入口下线清单，确认无菜单/页面/移动端依赖后再删除旧代码 |

不得在 T3/T4/T5 阶段删除：

1. 旧 `MtRoom`、`MtBooking`、`MtSignin`、`MtResolution` Entity
2. 旧 `MtRoomMapper`、`MtBookingMapper`、`MtSigninMapper`、`MtResolutionMapper`
3. 旧 `MtMeetingService`、`MtRoomService`
4. 旧 `MtBookingController`、`MtRoomController`、`MtSigninController`
5. 旧 Web/Mobile API 文件

### 8.6 风险点

| 风险 | 影响 | 缓解 |
|------|------|------|
| 状态从数字改字符串 | 前后端枚举不一致 | T9/T10 新增统一枚举映射 |
| 冲突检测并发 | 并发预定同一房间同时段 | T4 使用 `FOR UPDATE` 行锁 + 重试 |
| 周期性会议定时任务失效 | 不会自动生成预订 | T4 增加任务幂等补偿 + 监控 |
| 签到 GPS 偏移 | 用户在边缘区域签到失败 | 半径可配置 + 超时补签 |
| 提醒时机错误 | 提前 X 分钟提醒误发 | T4 用调度任务按 `start_time - remind_minutes` 精确触发 |
| 决议转任务失败 | 任务未创建但状态错误 | 事务内同步调用 + 失败重试 |
| 旧接口仍可访问 | 用户进入旧页面 | 切换前保留，切换后旧入口 deprecated |
| 视频会议占位 | 字段为 null 时需隐藏 | 前端条件渲染 |

### 8.7 回滚方式

| 回滚场景 | 操作 |
|----------|------|
| T3/T4 后端新模块失败 | 停止引用 `oa-meeting` 新接口，旧 Controller 不受影响 |
| T6 工作流回调失败 | 会议室预定可临时跳过审批 |
| T7 消息推送失败 | 回退到 oa-message 旧版 |
| T8 决议转任务失败 | 关闭 `toTask` 入口 |
| T9 Web 切换失败 | 前端 API 切回 `src/api/meeting*.ts` 旧路径 |
| T10 Mobile 切换失败 | 移动端 API 切回旧路径 |
| 数据库草案不通过 | 不合并 `mt_meeting_contract.sql` 到 baseline，继续使用旧表 |

---

## 9. T3 Claude Code 任务单：Meeting Entity + Mapper

### 9.1 任务目标

在 `oa-meeting` 模块内建立会议管理 6 张表对应的 Entity、DTO/VO、Enum、Mapper 基础结构，对齐 `mt_meeting_contract.sql`，但不实现业务 Service 和 Controller。

### 9.2 必须先阅读

```text
CLAUDE.md
docs/superpowers/specs/2026-06-02-oa-system-redesign.md
docs/superpowers/workflows/claude-code-oa-redesign-workflow.md
docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md
code/backend/sql/mt_meeting_contract.sql
code/backend/oa-meeting/pom.xml
code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtRoom.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtBooking.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtSignin.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/entity/MtResolution.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/mapper/MtBookingMapper.java
```

### 9.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 Meeting Entity、DTO、VO、Enum、Mapper |
| `code/backend/oa-meeting/src/test/java/**` | 新增 Mapper/模型相关测试 |
| `code/backend/oa-meeting/pom.xml` | 仅在缺少必要依赖时修改 |
| `docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md` | 记录执行结果 |

### 9.4 禁止修改

```text
code/backend/oa-task/**
code/backend/oa-message/**
code/backend/oa-workflow/**
code/backend/oa-web/src/main/java/cn/oa/meeting/**
code/frontend/**
code/mobile/**
code/backend/sql/oa_system_full.sql
code/backend/sql/oa_system_extensions.sql
```

### 9.5 产出物

建议类名：

| 类型 | 建议名称 |
|------|----------|
| Entity | `MtRoom`、`MtBooking`、`MtParticipant`、`MtSignin`、`MtResolution`、`MtRecurring` |
| Enum | `MtBookingStatus`、`MtParticipantRole`、`MtParticipantSigninStatus`、`MtParticipantResponseStatus`、`MtSigninType`、`MtResolutionStatus`、`MtResolutionPriority`、`MtRecurringFrequency`、`MtRecurringStatus`、`MtRoomStatus` |
| DTO | `MtRoomSaveDTO`、`MtBookingCreateDTO`、`MtBookingRescheduleDTO`、`MtBookingQueryDTO`、`MtParticipantAddDTO`、`MtParticipantRespondDTO`、`MtSigninQrDTO`、`MtSigninGpsDTO`、`MtSigninSupplementDTO`、`MtMinutesSaveDTO`、`MtResolutionCreateDTO`、`MtResolutionProgressDTO`、`MtRecurringSaveDTO` |
| VO | `MtRoomVO`、`MtBookingVO`、`MtParticipantVO`、`MtSigninVO`、`MtMinutesVO`、`MtResolutionVO`、`MtRecurringVO` |
| Mapper | `MtRoomMapper`、`MtBookingMapper`、`MtParticipantMapper`、`MtSigninMapper`、`MtResolutionMapper`、`MtRecurringMapper` |

字段必须对齐 `mt_meeting_contract.sql`。如果现有 `oa-meeting` 中已有 `MtRoom` 等同名类（已存在），必须先说明冲突并选择合并路径，不得新增第三套重复模型。

### 9.6 完成标准

1. Entity 字段和表字段完整对应。
2. 枚举覆盖 T1 中所有状态、类型。
3. DTO 包含基本 Jakarta Validation 注解。
4. Mapper 使用 MyBatis-Plus `BaseMapper`。
5. 不引入业务逻辑。
6. 不删除旧 `mt_*` 实现。

### 9.7 验收命令

```bash
cd code/backend
mvn -pl oa-meeting -am test
```

如果 `oa-meeting` 当前还没有测试框架或依赖导致命令失败，Claude Code 必须说明失败原因，并给出最小修复建议，不得跳过不报。

### 9.8 可直接交给 Claude Code 的提示词

```text
请执行会议管理重构 T3：Meeting Entity + Mapper。

严格遵循 docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md 第 9 章。

只允许新增/修改 oa-meeting 模块内的 Entity、DTO、VO、Enum、Mapper 和必要测试。
禁止修改旧 oa-task、oa-message、oa-workflow、oa-web Controller、frontend、mobile、正式 SQL baseline。

完成后运行：
cd code/backend
mvn -pl oa-meeting -am test

最终汇报：
- 新增/修改文件
- 是否发现已有重复 Meeting 模型
- 验收命令结果
- T4 需要注意的问题
```

---

## 10. T4 Claude Code 任务单：Meeting Service（含冲突检测算法）

### 10.1 任务目标

在 `oa-meeting` 模块内实现会议室 CRUD、预定/取消/改期、冲突检测、签到、纪要、决议、提醒、周期性会议等业务 Service，包括但不限于：

| Service | 核心方法 |
|---------|----------|
| `MtRoomService` | `createRoom` / `updateRoom` / `deleteRoom` / `changeStatus` / `pageQuery` / `getDetail` / `listAvailable` |
| `MtBookingService` | `book` / `reschedule` / `cancel` / `submit` / `start` / `finish` / `pageQuery` / `getDetail` / `calendarView` / `onWorkflowApproved` / `onWorkflowRejected` |
| `MtParticipantService` | `addParticipants` / `removeParticipant` / `respond` / `listByBooking` / `syncToMtParticipant` |
| `MtSigninService` | `generateQrToken` / `signinByQr` / `signinByGps` / `manualSupplement` / `listByBooking` / `mySignins` / `markLate` |
| `MtMinutesService` | `saveMinutes` / `updateMinutes` / `getByBooking` |
| `MtResolutionService` | `createResolution` / `updateResolution` / `updateProgress` / `deleteResolution` / `toTask` / `listByBooking` / `listByAssignee` / `scanOverdue` |
| `MtRecurringService` | `createTemplate` / `updateTemplate` / `pause` / `resume` / `end` / `generateUpcoming` / `pageQuery` |
| `MtReminderService` | `scheduleReminders` / `sendNow` / `cancelReminders` |

### 10.2 必须先阅读

```text
T1/T2/T3 结果
code/backend/sql/mt_meeting_contract.sql
旧 MtMeetingService.java
旧 MtRoomService.java
文档: docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第三章 3.4.1
```

### 10.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 Service、ServiceImpl、领域方法、必要事件/接口 |
| `code/backend/oa-meeting/src/test/java/**` | 新增 Service 单元测试 |
| `docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md` | 记录执行结果 |

### 10.4 禁止修改

```text
code/backend/oa-web/**
code/backend/oa-task/**
code/backend/oa-message/**
code/backend/oa-workflow/**
code/frontend/**
code/mobile/**
旧 MtMeetingService.java
旧 MtRoomService.java
工作流核心算法文件，除非只是调用已有接口
```

### 10.5 必须实现的服务能力

#### 10.5.1 房间服务 `MtRoomService`

| 能力 | 要求 |
|------|------|
| `createRoom` | 校验 `roomCode` 唯一性 |
| `updateRoom` | 校验 `status` 不影响进行中的预定 |
| `deleteRoom` | 软删，校验无未来预定 |
| `changeStatus` | `ENABLED ↔ MAINTENANCE ↔ DISABLED` |
| `pageQuery` | 支持按状态、楼层筛选 |
| `listAvailable` | 给定 `[startTime, endTime)` 列出可用房间 |

#### 10.5.2 预定服务 `MtBookingService`

| 能力 | 要求 |
|------|------|
| `book` | 冲突检测、`bookingNo` 生成、参与人同步、提醒调度 |
| `reschedule` | 重新冲突检测、提醒重新调度 |
| `cancel` | 校验当前用户或管理员、释放提醒任务 |
| `submit` | `need_approval=1` 时启动工作流 |
| `start` | 时间到 / 手动开始，状态 `RUNNING` |
| `finish` | 状态 `FINISHED`，触发签到统计、决议逾期扫描 |
| `onWorkflowApproved` | 状态 `RUNNING`，生成提醒 |
| `onWorkflowRejected` | 状态 `REJECTED` |
| `pageQuery` | 按部门、状态、日期范围筛选 |
| `calendarView` | 日历视图，返回 `[{date, bookings: [...]}]` |

#### 10.5.3 冲突检测算法（关键实现）

```java
public List<MtBooking> findConflicts(Long roomId, LocalDateTime start, LocalDateTime end) {
    return mtBookingMapper.selectList(new LambdaQueryWrapper<MtBooking>()
        .eq(MtBooking::getRoomId, roomId)
        .eq(MtBooking::getDelFlag, "0")
        .notIn(MtBooking::getStatus, MtBookingStatus.CANCELED, MtBookingStatus.REJECTED)
        .lt(MtBooking::getStartTime, end)
        .gt(MtBooking::getEndTime, start));
}

@Transactional(isolation = Isolation.READ_COMMITTED)
public Long book(MtBookingCreateDTO dto, Long empId) {
    // 1. 校验会议室
    MtRoom room = mtRoomMapper.selectById(dto.getRoomId());
    if (room == null) throw new BusinessError("会议室不存在");
    if (room.getStatus() != MtRoomStatus.ENABLED) throw new BusinessError("会议室不可用");
    
    // 2. 加锁 + 冲突检测
    List<MtBooking> conflicts = mtBookingMapper.selectForUpdate(room.getId(), dto.getStartTime(), dto.getEndTime());
    if (!conflicts.isEmpty()) throw new BusinessError("会议室在该时段已被占用");
    
    // 3. 创建预定
    MtBooking booking = new MtBooking();
    booking.setBookingNo(generateBookingNo());
    booking.setRoomId(dto.getRoomId());
    booking.setStartTime(dto.getStartTime());
    booking.setEndTime(dto.getEndTime());
    booking.setBookEmpId(empId);
    booking.setStatus(MtBookingStatus.DRAFT);
    mtBookingMapper.insert(booking);
    
    // 4. 同步参与人
    mtParticipantService.syncParticipants(booking.getId(), dto.getParticipantIds());
    
    // 5. 调度提醒
    mtReminderService.scheduleReminders(booking.getId());
    
    // 6. 如需审批，提交工作流
    if (room.getNeedApproval()) {
        workflowEngine.startWorkflow("meeting_booking", booking.getId(), "会议预定审批", empId);
    } else {
        booking.setStatus(MtBookingStatus.PENDING);
        mtBookingMapper.updateById(booking);
    }
    
    return booking.getId();
}
```

#### 10.5.4 签到服务 `MtSigninService`

| 能力 | 要求 |
|------|------|
| `generateQrToken` | 30 秒过期，仅参与人可签 |
| `signinByQr` | 校验 `qrToken` 有效、参会人员身份 |
| `signinByGps` | 计算 `distanceM`，超过 `signin_radius_m` 拒绝 |
| `manualSupplement` | 仅会议组织者/管理员可补签 |
| `markLate` | `signin_time > start_time` 标记迟到 |
| `scanAndUpdateStatus` | 会议结束后批量更新 `mt_participant.signin_status` |

#### 10.5.5 纪要服务 `MtMinutesService`

| 能力 | 要求 |
|------|------|
| `saveMinutes` | 每个会议一份纪要，支持富文本/HTML/附件 |
| `getByBooking` | 查询会议纪要 |

#### 10.5.6 决议服务 `MtResolutionService`

| 能力 | 要求 |
|------|------|
| `createResolution` | 创建单条决议，可选立即转任务 |
| `updateProgress` | 同步更新 `mt_resolution.progress` 与 `task_item.progress` |
| `toTask` | 调用 `oa-task` API 创建 `task_item`，回写 `related_task_id` |
| `scanOverdue` | 定时扫描 `due_date < today AND status IN (TODO, IN_PROGRESS)`，标记 `OVERDUE` |

#### 10.5.7 周期性会议服务 `MtRecurringService`

| 能力 | 要求 |
|------|------|
| `createTemplate` | 校验 `frequency` 与 `weekday_mask/day_of_month` 匹配 |
| `generateUpcoming` | 由定时任务每日调用，生成未来 30 天预订，更新 `next_gen_date/last_gen_date` |
| `pause` / `resume` / `end` | 状态管理 |

`generateUpcoming` 算法：

```java
public int generateUpcoming(Long recurringId) {
    MtRecurring rec = mtRecurringMapper.selectById(recurringId);
    if (rec.getStatus() != ACTIVE) return 0;
    
    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(30);
    int generated = 0;
    
    for (LocalDate d = today; !d.isAfter(endDate); d = d.plusDays(1)) {
        if (!matches(d, rec)) continue;
        if (mtBookingMapper.existsByRecurringAndDate(recurringId, d)) continue;
        
        MtBooking b = new MtBooking();
        b.setRoomId(rec.getRoomId());
        b.setTitle(rec.getTitle());
        b.setStartTime(LocalDateTime.of(d, rec.getStartTimeOfDay()));
        b.setEndTime(LocalDateTime.of(d, rec.getEndTimeOfDay()));
        b.setRecurringId(recurringId);
        b.setBookEmpId(rec.getBookEmpId());
        b.setStatus(MtBookingStatus.PENDING);
        mtBookingMapper.insert(b);
        generated++;
    }
    
    rec.setLastGenDate(today);
    rec.setNextGenDate(today.plusDays(1));
    mtRecurringMapper.updateById(rec);
    return generated;
}
```

#### 10.5.8 提醒服务 `MtReminderService`

| 能力 | 要求 |
|------|------|
| `scheduleReminders` | 预订创建/改期时，根据 `remind_minutes` 计算 `triggerTime = start_time - remind_minutes` |
| `sendNow` | 定时任务扫描 `triggerTime <= now AND remind_sent = 0`，发送提醒并标记已发送 |
| `cancelReminders` | 取消或改期时清除未触发的提醒 |

### 10.6 幂等和并发要求

1. 冲突检测必须在事务内使用 `SELECT ... FOR UPDATE` 行锁。
2. 签到必须使用 `(booking_id, emp_id)` 唯一约束防重复。
3. 状态转换必须检查当前状态，重复回调不得重复发送提醒。
4. 周期性会议生成必须幂等：相同 `(recurring_id, date)` 不得生成两条。
5. 决议转任务必须使用 `oa-task` 提供的幂等 API，失败回滚决议状态。

### 10.7 测试要求

至少覆盖：

| 测试 | 场景 |
|------|------|
| 房间 CRUD | 创建/更新/删除/状态变更/唯一性校验 |
| 冲突检测 | 正常、空闲、边界（首尾相接）、跨日、改期 |
| 预定 | 正常、冲突、需审批、不需审批、参与人超容量 |
| 改期 | 正常、改到冲突时段 |
| 取消 | 申请人、管理员、已取消状态 |
| 审批回调 | 通过/驳回/幂等 |
| 签到 | 扫码、GPS 范围内、GPS 范围外、补签、迟到、重复签到 |
| 纪要 | 创建、更新 |
| 决议 | 创建、进度更新、转任务、逾期扫描 |
| 周期性 | 创建模板、生成预订、暂停/恢复、结束 |
| 提醒 | 调度、发送、取消、改期重新调度 |

### 10.8 验收命令

```bash
cd code/backend
mvn -pl oa-meeting -am test
```

### 10.9 可直接交给 Claude Code 的提示词

```text
请执行会议管理重构 T4：Meeting Service（含冲突检测算法）。

严格遵循 docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md 第 10 章。

前置条件：
- T3 已完成 Entity/DTO/VO/Enum/Mapper。
- 不修改 REST Controller、frontend、mobile、oa-task/msg/wf 内部实现。

实现重点：
- 冲突检测算法（FOR UPDATE 行锁）
- 预定/改期/取消
- 签到（扫码/GPS/补签）
- 决议转任务（通过 oa-task API）
- 周期性会议生成
- 提醒调度
- workflow 回调幂等状态转换
- Service 单元测试

完成后运行：
cd code/backend
mvn -pl oa-meeting -am test

最终汇报：
- 新增/修改文件
- 核心业务方法
- 测试覆盖场景
- 验收命令结果
- T5 Controller 需要注意的问题
```

---

## 11. T5 Claude Code 任务单：Meeting REST API

### 11.1 任务目标

为 T3/T4 的会议管理服务暴露 REST API，对齐 `/api/meeting/...` 契约，并补 Controller 测试。

### 11.2 必须先阅读

```text
T1/T2/T3/T4 结果
code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtBookingController.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtRoomController.java
code/backend/oa-meeting/src/main/java/cn/oa/meeting/controller/MtSigninController.java
```

### 11.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 Meeting Controller/API 类，或按模块既有结构放置 |
| `code/backend/oa-meeting/src/test/java/**` | 新增 Controller 测试 |
| `code/backend/oa-web/pom.xml` | 仅当 `oa-web` 尚未依赖 `oa-meeting` 或测试无法装配时修改 |
| `docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md` | 记录执行结果 |

### 11.4 禁止修改

```text
旧 MtBookingController.java
旧 MtRoomController.java
旧 MtSigninController.java
code/frontend/**
code/mobile/**
oa-task 内部 Controller
oa-message 内部 Controller
oa-workflow 内部 Controller
正式 SQL baseline
```

### 11.5 API 必须实现

完整 API 清单见本文档 7.5 节「API 契约」。T5 至少实现以下关键端点：

| 方法 | 路径 | 调用 Service | 权限码 |
|------|------|--------------|--------|
| `POST` | `/api/meeting/rooms` | `MtRoomService.createRoom` | `meeting:room:create` |
| `PUT` | `/api/meeting/rooms/{id}` | `MtRoomService.updateRoom` | `meeting:room:update` |
| `DELETE` | `/api/meeting/rooms/{id}` | `MtRoomService.deleteRoom` | `meeting:room:delete` |
| `GET` | `/api/meeting/rooms` | `MtRoomService.pageQuery` | `meeting:room:list` |
| `GET` | `/api/meeting/rooms/{id}` | `MtRoomService.getDetail` | `meeting:room:detail` |
| `PUT` | `/api/meeting/rooms/{id}/status` | `MtRoomService.changeStatus` | `meeting:room:status` |
| `GET` | `/api/meeting/rooms/availability` | `MtRoomService.listAvailable` | `meeting:room:list` |
| `POST` | `/api/meeting/bookings` | `MtBookingService.book` | `meeting:booking:create` |
| `PUT` | `/api/meeting/bookings/{id}` | `MtBookingService.update` | `meeting:booking:update` |
| `POST` | `/api/meeting/bookings/{id}/submit` | `MtBookingService.submit` | `meeting:booking:submit` |
| `POST` | `/api/meeting/bookings/{id}/reschedule` | `MtBookingService.reschedule` | `meeting:booking:update` |
| `POST` | `/api/meeting/bookings/{id}/cancel` | `MtBookingService.cancel` | `meeting:booking:cancel` |
| `POST` | `/api/meeting/bookings/{id}/start` | `MtBookingService.start` | `meeting:booking:update` |
| `POST` | `/api/meeting/bookings/{id}/finish` | `MtBookingService.finish` | `meeting:booking:update` |
| `GET` | `/api/meeting/bookings` | `MtBookingService.pageQuery` | `meeting:booking:list` |
| `GET` | `/api/meeting/bookings/my` | `MtBookingService.pageQuery(my=true)` | `meeting:booking:list` |
| `GET` | `/api/meeting/bookings/calendar` | `MtBookingService.calendarView` | `meeting:booking:list` |
| `GET` | `/api/meeting/bookings/{id}` | `MtBookingService.getDetail` | `meeting:booking:detail` |
| `POST` | `/api/meeting/bookings/{id}/participants` | `MtParticipantService.addParticipants` | `meeting:booking:update` |
| `DELETE` | `/api/meeting/bookings/{id}/participants/{empId}` | `MtParticipantService.removeParticipant` | `meeting:booking:update` |
| `PUT` | `/api/meeting/bookings/{id}/participants/{empId}/role` | `MtParticipantService.updateRole` | `meeting:booking:update` |
| `POST` | `/api/meeting/bookings/{id}/respond` | `MtParticipantService.respond` | `meeting:booking:list` |
| `POST` | `/api/meeting/bookings/{id}/signin/qr-token` | `MtSigninService.generateQrToken` | `meeting:signin:create` |
| `POST` | `/api/meeting/bookings/{id}/signin/qr` | `MtSigninService.signinByQr` | `meeting:signin:create` |
| `POST` | `/api/meeting/bookings/{id}/signin/gps` | `MtSigninService.signinByGps` | `meeting:signin:create` |
| `POST` | `/api/meeting/signins/{id}/supplement` | `MtSigninService.manualSupplement` | `meeting:signin:update` |
| `GET` | `/api/meeting/bookings/{id}/signins` | `MtSigninService.listByBooking` | `meeting:signin:list` |
| `GET` | `/api/meeting/signins/my` | `MtSigninService.mySignins` | `meeting:signin:list` |
| `POST` | `/api/meeting/bookings/{id}/minutes` | `MtMinutesService.saveMinutes` | `meeting:minutes:create` |
| `PUT` | `/api/meeting/minutes/{id}` | `MtMinutesService.updateMinutes` | `meeting:minutes:update` |
| `GET` | `/api/meeting/bookings/{id}/minutes` | `MtMinutesService.getByBooking` | `meeting:minutes:list` |
| `POST` | `/api/meeting/bookings/{id}/resolutions` | `MtResolutionService.createResolution` | `meeting:resolution:create` |
| `PUT` | `/api/meeting/resolutions/{id}` | `MtResolutionService.updateResolution` | `meeting:resolution:update` |
| `DELETE` | `/api/meeting/resolutions/{id}` | `MtResolutionService.deleteResolution` | `meeting:resolution:delete` |
| `GET` | `/api/meeting/bookings/{id}/resolutions` | `MtResolutionService.listByBooking` | `meeting:resolution:list` |
| `POST` | `/api/meeting/resolutions/{id}/to-task` | `MtResolutionService.toTask` | `meeting:resolution:create` |
| `PUT` | `/api/meeting/resolutions/{id}/progress` | `MtResolutionService.updateProgress` | `meeting:resolution:update` |
| `POST` | `/api/meeting/recurring` | `MtRecurringService.createTemplate` | `meeting:recurring:create` |
| `PUT` | `/api/meeting/recurring/{id}` | `MtRecurringService.updateTemplate` | `meeting:recurring:update` |
| `PUT` | `/api/meeting/recurring/{id}/pause` | `MtRecurringService.pause` | `meeting:recurring:update` |
| `PUT` | `/api/meeting/recurring/{id}/resume` | `MtRecurringService.resume` | `meeting:recurring:update` |
| `DELETE` | `/api/meeting/recurring/{id}` | `MtRecurringService.end` | `meeting:recurring:delete` |
| `GET` | `/api/meeting/recurring` | `MtRecurringService.pageQuery` | `meeting:recurring:list` |
| `GET` | `/api/meeting/recurring/{id}` | `MtRecurringService.getDetail` | `meeting:recurring:detail` |
| `POST` | `/api/meeting/bookings/{id}/remind` | `MtReminderService.sendNow` | `meeting:booking:update` |
| `GET` | `/api/meeting/reports/usage` | `MtReportService.usage` | `meeting:report:view` |
| `GET` | `/api/meeting/reports/signin` | `MtReportService.signin` | `meeting:report:view` |
| `GET` | `/api/meeting/reports/resolution` | `MtReportService.resolution` | `meeting:report:view` |

### 11.6 Controller 要求

1. 统一返回 `R<T>` 或项目当前统一响应类型。
2. DTO 参数必须 `@Valid`。
3. 从当前认证上下文获取 `empId` 与 `deptId`，普通用户不能伪造申请人。
4. 管理接口必须有管理员或权限注解。
5. OpenAPI/Knife4j 注解完整。
6. 审批动作不在 Meeting Controller 实现，继续使用 workflow task API。
7. Controller 不写冲突检测、状态流转等业务逻辑。

### 11.7 测试要求

至少覆盖：

| 测试 | 断言 |
|------|------|
| 创建会议室 | 返回 `code=0`，调用 Service |
| 冲突预定 | 期望抛 `BusinessError("会议室在该时段已被占用")` |
| 预定 | 正常返回 bookingId |
| 取消 | 当前用户 ID 传入 Service |
| 签到-扫码 | 校验 qrToken |
| 签到-GPS 范围外 | 期望抛业务异常 |
| 创建决议-转任务 | 期望调用 oa-task API |
| 创建周期性模板 | 校验频率与 weekday_mask |
| 参数校验 | 缺必填字段返回错误 |

### 11.8 验收命令

```bash
cd code/backend
mvn -pl oa-meeting,oa-web -am test
```

### 11.9 可直接交给 Claude Code 的提示词

```text
请执行会议管理重构 T5：Meeting REST API。

严格遵循 docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md 第 11 章。

前置条件：
- T3 Entity/Mapper 已完成。
- T4 Service 已完成。

允许修改：
- oa-meeting 中的 Controller/API 与测试
- 必要时 oa-web/pom.xml 依赖
- 本试点文档的执行结果记录

禁止修改：
- 旧 MtBookingController / MtRoomController / MtSigninController
- frontend
- mobile
- oa-task/msg/wf 内部 Controller
- 正式 SQL baseline

完成后运行：
cd code/backend
mvn -pl oa-meeting,oa-web -am test

最终汇报：
- 新增/修改文件
- API 路径清单
- 权限注解清单
- Controller 测试覆盖
- 验收命令结果
- T6 工作流回调接入前置条件
```

---

## 12. T6 Claude Code 任务单：工作流回调接入

### 12.1 任务目标

实现会议预定的可选审批流程。当 `mt_room.need_approval = 1` 时，预定提交后启动工作流；审批通过/驳回/撤回回调会议业务状态（`PENDING/RUNNING/REJECTED`）。

### 12.2 必须先阅读

```text
T1-T5 结果
docs/superpowers/specs/2026-06-02-wf-engine-kernel-task-split.md
code/backend/oa-workflow/oa-workflow-api/src/main/java/cn/oa/workflow/api/callback/WorkflowCallbackDispatcher.java
```

### 12.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 `MeetingWorkflowCallback` Handler |
| `code/backend/oa-workflow/**` | 仅在 `WorkflowCallbackDispatcher` 中注册新 Handler，不修改算法核心 |
| `code/backend/oa-meeting/src/test/java/**` | 新增集成测试 |
| `code/backend/sql/baseline/003_seed_workflow.sql` | 新增 `meeting_booking` 流程定义 seed |

### 12.4 禁止修改

```text
工作流引擎算法文件
code/frontend/**
code/mobile/**
正式 SQL baseline（除 seed_workflow.sql）
```

### 12.5 必须实现

1. `MeetingWorkflowCallback implements WorkflowCallback`：
   - `onApproved(instanceId)` → 查找 `mt_booking WHERE process_instance_id = ?`，状态置 `PENDING` 或 `RUNNING`（按 `need_approval` 决定），发送站内通知。
   - `onRejected(instanceId, reason)` → 状态置 `REJECTED`，发送通知。
   - `onWithdrawn(instanceId)` → 状态置 `CANCELED`，释放提醒任务。
2. 幂等：每次状态变更前检查当前状态。
3. 在 `WorkflowCallbackDispatcher` 中按 `businessType = "meeting_booking"` 注册 Handler。
4. 工作流定义 seed：`meeting_booking` 单节点审批（直属上级）。

### 12.6 验收命令

```bash
cd code/backend
mvn -pl oa-workflow/oa-workflow-core,oa-meeting,oa-web -am test
```

---

## 13. T7 Claude Code 任务单：消息中心联动

### 13.1 任务目标

通过 `oa-message` 实现会议相关的消息推送：

| 消息类型 | 触发时机 | 渠道（默认） |
|----------|----------|--------------|
| `MEETING_REMIND` | 会议开始前 X 分钟 | SITE、WECHAT |
| `MEETING_INVITE` | 创建预定/添加参与人 | SITE |
| `MEETING_CANCELED` | 取消会议 | SITE、WECHAT |
| `MEETING_RESCHEDULED` | 改期 | SITE |
| `MEETING_SIGNIN` | 签到成功 | SITE |
| `RESOLUTION_ASSIGNED` | 决议派发 | SITE、WECHAT |
| `RESOLUTION_OVERDUE` | 决议逾期 | SITE、SMS |

### 13.2 必须先阅读

```text
T1-T6 结果
docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第四章
code/backend/oa-message/src/main/java/cn/oa/message/service/MessageNotificationService.java
```

### 13.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 `MeetingMessagePublisher` |
| `code/backend/oa-message/src/main/java/**` | 注册新消息类型枚举与模板 |
| `code/backend/oa-meeting/src/test/java/**` | 集成测试 |

### 13.4 禁止修改

```text
oa-message 渠道实现
oa-task/oa-workflow 内部实现
code/frontend/**
code/mobile/**
```

### 13.5 必须实现

1. `MeetingMessagePublisher.publish(type, bookingId, params)` → 调用 `MessageNotificationService`。
2. 提醒消息使用 `mt_booking.remind_minutes` 与 `start_time` 计算触发时间，由 `MtReminderService` 通过调度任务驱动。
3. 消息模板支持 `bookingTitle/startTime/roomName/participants/...` 变量。
4. 集成测试覆盖：预定创建→邀请消息、提醒触发、取消通知、签到通知、决议派发通知。

### 13.6 验收命令

```bash
cd code/backend
mvn -pl oa-message,oa-meeting,oa-web -am test
```

---

## 14. T8 Claude Code 任务单：决议转任务联动

### 14.1 任务目标

会议决议一键转 `oa-task` 的 `task_item`，并维护 `mt_resolution.related_task_id` 关联。

### 14.2 必须先阅读

```text
T1-T7 结果
docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第三章 3.3
code/backend/oa-task 公开 API
```

### 14.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-meeting/src/main/java/**` | 新增 `ResolutionTaskBridge` |
| `code/backend/oa-meeting/src/test/java/**` | 集成测试 |

### 14.4 禁止修改

```text
oa-task 内部实现
code/frontend/**
code/mobile/**
```

### 14.5 必须实现

1. `ResolutionTaskBridge.toTask(resolutionId)` → 调用 `oa-task` API 创建 `task_item`：
   - `title` = `[会议决议] {content 前 50 字}`
   - `projectId` = `null`（个人任务）
   - `assignee_id` = `mt_resolution.assignee_id`
   - `planned_end` = `mt_resolution.due_date`
   - `priority` = 映射 `LOW/NORMAL/HIGH/URGENT`
   - `description` = `mt_resolution.content` + 会议链接
2. 事务性：先调用 oa-task，成功后回写 `related_task_id`；失败回滚决议状态。
3. `mt_resolution.status` 变 `IN_PROGRESS` 时同步 `task_item.status`；反之亦然。
4. 任务完成时同步决议 `progress = 100` 与 `status = DONE`。
5. 任务删除/取消时决议 `status = CANCELED` 并保留 `related_task_id` 备查。

### 14.6 验收命令

```bash
cd code/backend
mvn -pl oa-task,oa-meeting,oa-web -am test
```

---

## 15. T9 Claude Code 任务单：Web API 与页面迁移

### 15.1 任务目标

Web 管理端接入新会议接口。

### 15.2 必须先阅读

```text
T1-T8 结果
code/frontend/src/api/meeting*.ts
code/frontend/src/views/oa/meeting/**
```

### 15.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/frontend/src/api/meeting.ts` | 新 typed API |
| `code/frontend/src/views/oa/meeting/rooms/**` | 会议室管理 |
| `code/frontend/src/views/oa/meeting/bookings/**` | 预定管理（列表/日历/详情） |
| `code/frontend/src/views/oa/meeting/signin/**` | 签到管理 |
| `code/frontend/src/views/oa/meeting/minutes/**` | 纪要管理 |
| `code/frontend/src/views/oa/meeting/resolutions/**` | 决议管理 |
| `code/frontend/src/views/oa/meeting/recurring/**` | 周期性会议管理 |
| `code/frontend/src/views/oa/meeting/reports/**` | 报表 |

### 15.4 禁止修改

```text
不做 monorepo 改造
不重构全局布局
旧会议页面可保留作为回滚入口
```

### 15.5 必须实现

| 页面 | 关键能力 |
|------|----------|
| 会议室管理 | CRUD、状态变更、可用性查询 |
| 预定列表 | 筛选（部门/状态/日期）、分页 |
| 预定日历 | 周/月视图、点击预定查看详情 |
| 预定详情 | 标题、议程、参与人、签到状态、纪要、决议、提醒 |
| 创建/编辑预定 | 选会议室 + 时间 + 参与人、冲突检测提示 |
| 签到管理 | 二维码生成、签到列表、补签 |
| 纪要 | 富文本编辑、附件上传 |
| 决议 | 列表、新增、进度更新、转任务按钮 |
| 周期性会议 | 模板列表、生成历史预览、暂停/恢复 |
| 报表 | 会议室使用率、签到率、决议完成率 |

### 15.6 验收命令

```bash
cd code/frontend
pnpm typecheck && pnpm build
```

---

## 16. T10 Claude Code 任务单：Mobile API 与页面迁移

### 16.1 任务目标

移动端接入预定、签到、纪要/决议查看。

### 16.2 必须先阅读

```text
T1-T9 结果
code/mobile/src/api/meeting*.ts
code/mobile/src/pages/oa/meeting/**
```

### 16.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/mobile/src/api/meeting.ts` | 新 typed API |
| `code/mobile/src/pages/oa/meeting/booking-create.vue` | 创建预定 |
| `code/mobile/src/pages/oa/meeting/booking-list.vue` | 我的预定 |
| `code/mobile/src/pages/oa/meeting/booking-detail.vue` | 预定详情 |
| `code/mobile/src/pages/oa/meeting/signin.vue` | 签到（扫码 + GPS） |
| `code/mobile/src/pages/oa/meeting/minutes.vue` | 纪要查看 |
| `code/mobile/src/pages/oa/meeting/resolutions.vue` | 决议查看 + 进度更新 |

### 16.4 禁止修改

```text
不实现复杂管理配置页面
旧移动端页面可保留
```

### 16.5 必须实现

| 页面 | 关键能力 |
|------|----------|
| 我的预定 | 列表，按时间分组，状态显示 |
| 创建预定 | 简洁表单：会议室 + 时间 + 参与人 |
| 预定详情 | 主题、参与人、签到状态、决议 |
| 签到 | 调起扫码 / GPS 定位 |
| 纪要 | 富文本只读展示 |
| 决议 | 列表 + 进度更新（指派给自己的） |

### 16.6 验收命令

```bash
cd code/mobile
pnpm build:h5
```

---

## 17. T11 Claude Code 任务单：端到端回归与下线准备

### 17.1 任务目标

端到端验证会议全链路；标记旧入口下线清单。

### 17.2 端到端验证清单

| 场景 | 步骤 | 预期 |
|------|------|------|
| 创建会议 | 登录 → 会议室管理 → 创建 → 启用 | mt_room 出现新记录 |
| 预定 | 创建预定 → 选冲突时段 | 提示冲突 |
| 预定成功 | 创建预定 → 选空闲时段 | 状态 `DRAFT/PENDING` |
| 审批 | 需审批房间 → 提交 → 切换审批人 → 通过 | 状态 `PENDING/RUNNING` |
| 提醒 | 提前 X 分钟 | 收到消息 |
| 签到 | 会议开始 → 参与人扫码 | 签到成功 |
| 签到 GPS | 移动端 GPS 签到 | 范围内成功 |
| 补签 | 管理员补签 → 输入原因 | mt_signin supplement 字段写入 |
| 纪要 | 会议结束 → 上传纪要 | mt_minutes 记录 |
| 决议派发 | 创建决议 → 转任务 | oa-task 任务生成 |
| 任务同步 | 更新任务进度 | mt_resolution.progress 同步 |
| 周期性 | 创建周会模板 → 定时任务 | 30 天内预订自动生成 |
| 报表 | 查询使用率 | 数据正确 |

### 17.3 下线清单

| 旧项 | 新项 | 切换条件 | 回滚方式 |
|------|------|----------|----------|
| `MtRoom`/`MtBooking`/`MtSignin`/`MtResolution` 旧 Entity | 新 Entity | E2E 通过 | 保留旧实体 |
| `MtMeetingService` | 新 Service | E2E 通过 | 保留旧服务 |
| `MtBookingController` | 新 Controller | Web 切换完成 | 旧接口 deprecated |
| `code/frontend/src/api/meeting*.ts` 旧 API | 新 API | Web 切换完成 | 旧文件 deprecated |
| `code/mobile/src/api/meeting*.ts` 旧 API | 新 API | Mobile 切换完成 | 旧文件 deprecated |

### 17.4 验收命令

```bash
# 后端
cd code/backend
mvn -pl oa-meeting,oa-web,oa-task,oa-message,oa-workflow/oa-workflow-core -am test

# Web
cd code/frontend
pnpm typecheck && pnpm build

# Mobile
cd code/mobile
pnpm build:h5
```

### 17.5 可直接交给 Claude Code 的提示词

```text
请执行会议管理重构 T11：端到端回归与下线准备。

严格遵循 docs/superpowers/specs/2026-06-02-mt-meeting-task-split.md 第 17 章。

允许修改：
- 文档（追加下线清单、回归报告）
- 必要的 bug 修复

禁止：
- 未通过 E2E 前删除旧代码
- 大范围重构

最终汇报：
- 端到端测试结果
- 旧入口下线清单
- 风险与回滚方式
```

---

## 18. 附录 A：定时任务清单

| 任务 | cron | 说明 |
|------|------|------|
| 提醒发送 | 每分钟 | 扫描 `mt_booking WHERE status='PENDING'/'RUNNING' AND remind_sent=0 AND start_time - INTERVAL remind_minutes MINUTE <= now()` |
| 周期性会议生成 | 每天 02:00 | 扫描 `mt_recurring WHERE status='ACTIVE' AND last_gen_date < today` |
| 决议逾期扫描 | 每天 09:00 | 扫描 `mt_resolution WHERE status IN ('TODO','IN_PROGRESS') AND due_date < today` |
| 会议室状态校准 | 每小时 | 校验 `MAINTENANCE` 房间是否被预定，如有则警告 |
| 会议结束扫描 | 每 5 分钟 | 扫描 `mt_booking WHERE status='RUNNING' AND end_time <= now()` 标记 `FINISHED` |

---

## 19. 附录 B：状态机一览

### 19.1 `mt_booking.status`

```
DRAFT ─submit─► PENDING ─start─► RUNNING ─finish─► FINISHED
   │                │                                   
   └─── cancel ─────┴─────────────── CANCELED            
                                                   
              reject ──► REJECTED
```

### 19.2 `mt_resolution.status`

```
TODO ─progress update─► IN_PROGRESS ─complete─► DONE
   │                         │
   │                         └──► OVERDUE (扫描触发)
   └─── cancel ──────────────► CANCELED
```

### 19.3 `mt_recurring.status`

```
ACTIVE ─pause─► PAUSED ─resume─► ACTIVE
   │
   └─── end ──► ENDED (终态)
```

---

## 20. 附录 C：与第三方对接点（占位）

| 对接点 | 字段 | 实现方式 |
|--------|------|----------|
| 腾讯会议 | `mt_booking.meeting_link` | 留字段，外部任务接入 |
| Zoom | 同上 | 同上 |
| 钉钉会议 | 同上 | 同上 |
| 企业微信机器人 | `oa-message` 渠道 | T7 联动 |
| SMS | `oa-message` 渠道 | T7 联动 |
| 邮件 | `oa-message` 渠道 | T7 联动 |
