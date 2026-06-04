# 企业OA系统全面重构设计方案 v2.0

> 版本: 2.0 | 更新日期: 2026-06-02
> 架构决策: 务实DDD + 微内核 | 数据策略: 全部重建 | 搜索方案: Elasticsearch 8.x

---

## 概述

基于"微内核+务实DDD"架构，对现有企业OA系统进行全面重构。本次重构**不保留旧数据**，从空数据库开始，所有模块使用新的表前缀规范。

### 核心决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 架构模式 | 务实DDD | 模块化分包 + 领域事件 + 聚合概念，不引入CQRS/六边形架构 |
| 数据策略 | 全部重建 | 废弃所有 `oa_*` 表，统一使用模块前缀 (`hr_*`, `fin_*`, `mt_*`, `adm_*`, `doc_*`, `km_*`, `task_*`) |
| 搜索引擎 | Elasticsearch 8.x | 全文检索、知识推荐、日志分析 |
| 任务模块 | 完整实现 | 项目→任务→子任务→甘特图→工时记录完整功能 |
| 前端架构 | pnpm monorepo | 共享组件库 + 管理端 + 移动端 |

---

## 一、系统架构总览

### 1.1 模块划分（DDD限界上下文）

```
code/backend/
├── oa-platform/                    # 平台基础设施层
│   ├── oa-platform-core            # 核心工具（异常、常量、工具类）
│   │   └── cn.oa.platform.core
│   │       ├── exception/          # BusinessError, AuthError, SystemError
│   │       ├── constant/           # StatusEnum, RoleEnum, MessageEnum
│   │       ├── result/             # R<T>, PageResult<T>
│   │       └── util/               # JsonUtil, DateUtil, SecurityUtil
│   │
│   └── oa-platform-security        # 安全认证层
│       └── cn.oa.platform.security
│           ├── auth/               # JWT生成/验证, TokenService
│           ├── interceptor/        # AuthInterceptor, RateLimitInterceptor
│           ├── annotation/          # @RequireRole, @RequirePermission
│           └── context/            # UserContext, UserDetails
│
├── oa-workflow/                    # 工作流微内核（核心）
│   ├── oa-workflow-core            # 引擎核心
│   │   └── cn.oa.workflow.core
│   │       ├── engine/             # WorkflowEngine, StateMachine
│   │       ├── parser/             # NodeParser, ExpressionParser
│   │       ├── resolver/           # AssigneeResolver (策略链)
│   │       ├── handler/            # TaskHandler, SignHandler
│   │       └── event/              # WorkflowEventPublisher
│   │
│   ├── oa-workflow-model           # 领域模型
│   │   └── cn.oa.workflow.model
│   │       ├── entity/             # WfDefinition, WfInstance, WfTask...
│   │       ├── dto/                 # DefinitionDTO, TaskQueryDTO...
│   │       ├── vo/                  # TaskVO, InstanceVO...
│   │       ├── enums/               # TaskStatus, ApprovalMode, ActionType
│   │       └── aggregate/           # InstanceAggregate (聚合根)
│   │
│   ├── oa-workflow-mapper          # 数据访问
│   │   └── cn.oa.workflow.mapper
│   │
│   └── oa-workflow-api             # REST API
│       └── cn.oa.workflow.api
│           ├── controller/          # DefinitionController, TaskController
│           └── callback/            # WorkflowCallbackDispatcher
│
├── oa-document/                    # 公文管理（发文/收文）
├── oa-knowledge/                   # 文档与知识管理（ES搜索）
├── oa-meeting/                     # 会议管理（预定/签到/决议）
├── oa-task/                        # 任务与项目管理（新增）
├── oa-hr/                          # 人事管理（考勤/假期/异动）
├── oa-admin/                       # 综合行政（印章/用品/资产）
├── oa-finance/                     # 费控与报销（预算/报销/借款）
├── oa-message/                     # 消息通知中台（多渠道）
├── oa-integration/                 # 系统集成（日志/ES/外部接口）
│
└── oa-web/                         # 应用入口（聚合层）
    └── cn.oa.web
        ├── config/                 # 全局配置
        └── OaWebApplication.java
```

### 1.2 模块依赖关系

```
                    ┌─────────────────────────────────────┐
                    │          oa-web (聚合入口)            │
                    └─────────────────┬───────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│  oa-workflow  │           │  oa-message   │           │ oa-integration│
│   (微内核)     │◄──────────│   (消息中台)   │           │  (ES/日志)    │
└───────┬───────┘           └───────────────┘           └───────────────┘
        │
        │  领域事件驱动
        ▼
┌───────────────────────────────────────────────────────────────────────┐
│                          业务模块层                                    │
├─────────────┬─────────────┬─────────────┬─────────────┬───────────────┤
│ oa-document │ oa-knowledge│ oa-meeting  │   oa-task   │    oa-hr      │
├─────────────┴─────────────┴─────────────┴─────────────┴───────────────┤
│                        oa-admin (综合行政)                            │
│                        oa-finance (费控报销)                          │
└───────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────────────┐
│                       oa-platform (基础设施)                           │
│                   oa-platform-core + oa-platform-security              │
└───────────────────────────────────────────────────────────────────────┘
```

### 1.3 技术栈版本

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 17 LTS | 后端开发 |
| 框架 | Spring Boot | 3.4.x | 应用框架 |
| ORM | MyBatis-Plus | 3.5.x | 数据访问 |
| 数据库 | MySQL | 8.0 | 主数据库 |
| 缓存 | Redis | 7.x | 会话/分布式锁/缓存 |
| 搜索 | Elasticsearch | 8.x | 全文检索/日志分析 |
| 消息 | Spring Event + Redis Pub/Sub | - | 领域事件 |
| 认证 | JWT (jjwt) | 0.12.x | 无状态认证 |
| 前端 | Vue 3 + TypeScript + Element Plus | 3.4/5.x/2.x | Web管理端 |
| 样式 | Tailwind CSS | 4.x | 原子化CSS |
| 构建 | Vite 6 + pnpm monorepo | 6.x/9.x | 前端构建 |
| 移动端 | uni-app (Vue 3) | 3.0 | H5 + 微信小程序 |
| 文档 | Knife4j (OpenAPI 3) | 4.x | API文档 |

---

## 二、工作流微内核引擎

### 2.1 设计理念

工作流引擎采用"微内核+插件"架构，核心只负责：
1. **状态机调度** - 驱动流程实例状态流转
2. **审批人解析** - 策略链模式解析审批人
3. **任务分配与汇总** - 会签/或签/比例审批算法
4. **事件发布** - 领域事件驱动业务模块响应

业务模块通过**回调接口**实现业务逻辑，与引擎解耦。

### 2.2 流程实例状态机

```
                    ┌────────────────────────────────────────┐
                    │                                        │
                    ▼                                        │
┌────────┐      ┌─────────┐      ┌─────────┐      ┌────────┐ │
│ DRAFT │─────►│ RUNNING │─────►│ PASSED  │      │ABORTED │ │
└────────┘      └────┬────┘      └─────────┘      └────────┘ │
   │                 │                                      │
   │                 │                                      │
   │                 ▼                                      │
   │            ┌──────────┐                                │
   │            │ REJECTED │                                │
   │            └──────────┘                                │
   │                 │                                      │
   │                 ▼                                      │
   │            ┌─────────┐                                 │
   └───────────►│ REVOKED │◄────────────────────────────────┘
                └─────────┘
                     ▲
                     │
                ┌────────────┐
                │ SUSPENDED  │ ◄─── 暂停(可恢复)
                └────────────┘
```

**状态转换规则：**

| 当前状态 | 允许转换 | 触发条件 |
|----------|----------|----------|
| DRAFT | → RUNNING | 提交流程 |
| RUNNING | → PASSED | 所有节点审批通过 |
| RUNNING | → REJECTED | 任一节点驳回 |
| RUNNING | → REVOKED | 申请人撤回 |
| RUNNING | → SUSPENDED | 管理员暂停 |
| RUNNING | → ABORTED | 管理员终止 |
| SUSPENDED | → RUNNING | 管理员恢复 |
| REJECTED | → RUNNING | 申请人修改后重新提交 |

### 2.3 任务状态与操作

```
PENDING ──┬──► APPROVED      (同意)
          ├──► REJECTED      (驳回)
          ├──► TRANSFERRED   (转办)
          ├──► CANCELED      (因流程终止取消)
          └──► ADD_SIGN      (加签，创建子任务)
```

**操作权限矩阵：**

| 操作 | 申请人 | 审批人 | 管理员 |
|------|--------|--------|--------|
| 提交 | ✅ | - | - |
| 撤回 | ✅(RUNNING态) | - | ✅ |
| 同意 | - | ✅ | ✅ |
| 驳回 | - | ✅ | ✅ |
| 转办 | - | ✅ | ✅ |
| 加签 | - | ✅ | ✅ |
| 暂停 | - | - | ✅ |
| 终止 | - | - | ✅ |
| 催办 | ✅ | - | ✅ |

### 2.4 核心数据模型（8张表）

#### 2.4.1 流程定义表 `wf_definition`

```sql
CREATE TABLE `wf_definition` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '定义ID',
  `code`          VARCHAR(64)  NOT NULL                COMMENT '流程编码(如: leave/trip/expense)',
  `name`          VARCHAR(128) NOT NULL                COMMENT '流程名称',
  `version`       INT          NOT NULL DEFAULT 1      COMMENT '版本号',
  `category`      VARCHAR(64)  DEFAULT NULL            COMMENT '分类(hr/finance/admin/common)',
  `form_def_id`   BIGINT       DEFAULT NULL            COMMENT '关联表单定义ID',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT-草稿/PUBLISHED-已发布/DISABLED-已禁用)',
  `description`   VARCHAR(500) DEFAULT NULL            COMMENT '描述',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志',
  `create_by`     VARCHAR(64)  DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     VARCHAR(64)  DEFAULT NULL,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_code_version` (`code`, `version`),
  KEY `idx_def_category` (`category`),
  KEY `idx_def_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';
```

#### 2.4.2 流程节点表 `wf_node`

```sql
CREATE TABLE `wf_node` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `def_id`                BIGINT       NOT NULL                COMMENT '定义ID',
  `node_code`             VARCHAR(64)  NOT NULL                COMMENT '节点编码(start/end/node_1)',
  `node_name`             VARCHAR(128) NOT NULL                COMMENT '节点名称(部门经理审批)',
  `node_type`             VARCHAR(32)  NOT NULL                COMMENT '节点类型(START/END/APPROVAL/SUBPROCESS/CONDITION/GATEWAY)',
  `approval_mode`        VARCHAR(32)  DEFAULT NULL            COMMENT '审批模式(SEQUENTIAL/COUNTERSIGN/ORSIGN/PROPORTIONAL/VOTE)',
  `pass_ratio`           DECIMAL(5,2) DEFAULT NULL            COMMENT '通过比例(PROPORTIONAL模式)',
  `timeout_hours`         INT          DEFAULT NULL            COMMENT '超时时间(小时)',
  `timeout_action`        VARCHAR(32)  DEFAULT NULL            COMMENT '超时动作(AUTO_PASS/AUTO_REJECT/NOTIFY)',
  `field_permission`      JSON         DEFAULT NULL            COMMENT '字段权限矩阵{"field":"readonly/hidden/editable"}',
  `empty_assignee_strategy` VARCHAR(32) DEFAULT 'AUTO_PASS'   COMMENT '审批人为空策略(AUTO_PASS/AUTO_REJECT/ASSIGN_ADMIN/ERROR)',
  `sort_order`            INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  PRIMARY KEY (`id`),
  KEY `idx_node_def` (`def_id`),
  UNIQUE KEY `uk_node_def_code` (`def_id`, `node_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';
```

#### 2.4.3 流转条件表 `wf_transition`

```sql
CREATE TABLE `wf_transition` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流转ID',
  `def_id`        BIGINT       NOT NULL                COMMENT '定义ID',
  `from_node_id`  BIGINT       NOT NULL                COMMENT '源节点ID',
  `to_node_id`    BIGINT       NOT NULL                COMMENT '目标节点ID',
  `expression`    VARCHAR(500) DEFAULT NULL            COMMENT '条件表达式(MVEL/SpEL)',
  `sort_order`    INT          NOT NULL DEFAULT 0      COMMENT '优先级(小优先)',
  PRIMARY KEY (`id`),
  KEY `idx_trans_def` (`def_id`),
  KEY `idx_trans_from` (`from_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流转条件表';
```

#### 2.4.4 审批人规则表 `wf_assignee_rule`

```sql
CREATE TABLE `wf_assignee_rule` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `node_id`     BIGINT       NOT NULL                COMMENT '节点ID',
  `rule_type`   VARCHAR(32)  NOT NULL                COMMENT '规则类型(FIXED_USER/POST/DEPT_LEADER/REPORT_LINE/FORM_SELECT/API)',
  `rule_value`  VARCHAR(500) NOT NULL                COMMENT '规则值(用户ID/岗位编码/API地址)',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '优先级',
  PRIMARY KEY (`id`),
  KEY `idx_rule_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批人规则表';
```

**规则类型详解：**

| rule_type | rule_value 示例 | 解析结果 |
|-----------|-----------------|----------|---------|
| FIXED_USER | `1001,1002` | 固定用户ID列表 |
| POST | `DEPT_MANAGER` | 按岗位编码查询用户 |
| DEPT_LEADER | `null` | 申请人部门负责人 |
| REPORT_LINE | `1` | 申请人汇报线上第N级领导 |
| FORM_SELECT | `approver_id` | 表单字段值作为审批人 |
| API | `http://xxx/api/approvers?type=expense` | 调用外部接口获取 |

#### 2.4.5 流程实例表 `wf_instance`

```sql
CREATE TABLE `wf_instance` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `def_id`                BIGINT       NOT NULL                COMMENT '定义ID',
  `def_version`           INT          NOT NULL                COMMENT '定义版本(启动时绑定)',
  `def_snapshot`          JSON         DEFAULT NULL            COMMENT '定义快照(节点+流转+规则)',
  `business_type`         VARCHAR(64)  NOT NULL                COMMENT '业务类型(leave/trip/expense...)',
  `business_id`           BIGINT       NOT NULL                COMMENT '业务单据ID',
  `title`                 VARCHAR(200) NOT NULL                COMMENT '流程标题',
  `applicant_id`          BIGINT       NOT NULL                COMMENT '申请人ID',
  `status`                VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  `current_node_ids`      VARCHAR(500) DEFAULT NULL            COMMENT '当前节点ID列表(JSON数组)',
  `return_source_node_id` BIGINT       DEFAULT NULL            COMMENT '驳回来源节点ID',
  `return_strategy`       VARCHAR(32)  DEFAULT 'DIRECT_RETURN' COMMENT '驳回后重提策略(DIRECT_RETURN/SEQUENTIAL_RETURN)',
  `start_time`            DATETIME     NOT NULL                COMMENT '启动时间',
  `end_time`              DATETIME     DEFAULT NULL            COMMENT '结束时间',
  `del_flag`              CHAR(1)      NOT NULL DEFAULT '0',
  `create_by`             VARCHAR(64)  DEFAULT NULL,
  `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inst_def` (`def_id`),
  KEY `idx_inst_business` (`business_type`, `business_id`),
  KEY `idx_inst_applicant` (`applicant_id`),
  KEY `idx_inst_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';
```

#### 2.4.6 审批任务表 `wf_task`

```sql
CREATE TABLE `wf_task` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id`     BIGINT       NOT NULL                COMMENT '实例ID',
  `node_id`         BIGINT       NOT NULL                COMMENT '节点ID',
  `node_name`       VARCHAR(128) NOT NULL                COMMENT '节点名称(冗余)',
  `assignee_id`     BIGINT       NOT NULL                COMMENT '审批人ID',
  `task_type`       VARCHAR(32)  NOT NULL DEFAULT 'TODO' COMMENT '任务类型(TODO/COUNTERSIGN/ADD_SIGN)',
  `parent_task_id`  BIGINT       DEFAULT NULL            COMMENT '父任务ID(加签场景)',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `opinion`         VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `signature`       VARCHAR(512) DEFAULT NULL            COMMENT '电子签名URL',
  `due_time`        DATETIME     DEFAULT NULL            COMMENT '截止时间',
  `remind_count`    INT          NOT NULL DEFAULT 0      COMMENT '催办次数',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `complete_time`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_instance` (`instance_id`),
  KEY `idx_task_assignee` (`assignee_id`, `status`),
  KEY `idx_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务表';
```

#### 2.4.7 流转记录表 `wf_record`（审计日志）

```sql
CREATE TABLE `wf_record` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `instance_id`           BIGINT       NOT NULL                COMMENT '实例ID',
  `task_id`               BIGINT       DEFAULT NULL            COMMENT '任务ID',
  `operator_id`           BIGINT       NOT NULL                COMMENT '操作人ID',
  `operator_name`         VARCHAR(64)  NOT NULL                COMMENT '操作人姓名',
  `action`                VARCHAR(32)  NOT NULL                COMMENT '操作动作(SUBMIT/APPROVE/REJECT/TRANSFER/ADD_SIGN/WITHDRAW/RETURN)',
  `from_node_id`          BIGINT       DEFAULT NULL            COMMENT '源节点ID',
  `to_node_id`            BIGINT       DEFAULT NULL            COMMENT '目标节点ID',
  `opinion`               VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `field_snapshot_before` JSON         DEFAULT NULL            COMMENT '字段修改前快照',
  `field_snapshot_after`  JSON         DEFAULT NULL            COMMENT '字段修改后快照',
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_instance` (`instance_id`),
  KEY `idx_record_operator` (`operator_id`),
  KEY `idx_record_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流转记录表';
```

#### 2.4.8 审批委托表 `wf_delegation`

```sql
CREATE TABLE `wf_delegation` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '委托ID',
  `delegator_id`     BIGINT       NOT NULL                COMMENT '委托人ID',
  `delegate_id`      BIGINT       NOT NULL                COMMENT '被委托人ID',
  `process_category` VARCHAR(64)  DEFAULT '*'             COMMENT '流程分类(*表示全部)',
  `start_date`       DATE         NOT NULL                COMMENT '生效日期',
  `end_date`         DATE         NOT NULL                COMMENT '失效日期',
  `notify_delegator` TINYINT       NOT NULL DEFAULT 1      COMMENT '是否通知委托人',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/CANCELLED)',
  `create_by`        VARCHAR(64)  DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_deleg_delegator` (`delegator_id`, `status`),
  KEY `idx_deleg_delegate` (`delegate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批委托表';
```

> **2026-06-04 增补 - 业务单据状态对齐说明**：
> §2.4 的 `wf_instance.status` 状态机为 7 个（DRAFT/RUNNING/PASSED/REJECTED/REVOKED/ABORTED/SUSPENDED），但**业务单据**（如 `hr_leave_apply`）的 status 字段只承载**5 个**（DRAFT/RUNNING/PASSED/REJECTED/REVOKED）。
> - ABORTED/SUSPENDED 是**工作流实例**的状态，不由业务单据表达。
> - 业务单据收到工作流回调后，**只需同步** 5 个状态到自己的 status 字段。
> - 业务单据的完整状态定义见 `docs/superpowers/specs/2026-06-02-hr-leave-pilot-contract.md` §2。

### 2.5 审批人解析器链

```
┌─────────────────────────────────────────────────────────────────────┐
│                     AssigneeResolverChain                           │
├─────────────────────────────────────────────────────────────────────┤
│  Input: nodeId, formData, applicantId                              │
│  Output: Set<Long> assigneeIds                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Load all rules for node (ORDER BY sort_order)                  │
│     ┌─────────────────────────────────────────────────────────┐    │
│     │ wf_assignee_rule WHERE node_id = ? ORDER BY sort_order  │    │
│     └─────────────────────────────────────────────────────────┘    │
│                           │                                         │
│                           ▼                                         │
│  2. Execute each resolver (Strategy Pattern)                       │
│     ┌──────────────┐  ┌──────────────┐  ┌───────────────┐         │
│     │ FixedUser    │  │ PostResolver │  │ DeptLeader    │  ...    │
│     │ Resolver     │  │              │  │ Resolver      │         │
│     └──────────────┘  └──────────────┘  └───────────────┘         │
│           │                │                  │                    │
│           └────────────────┴──────────────────┘                   │
│                           │                                         │
│                           ▼                                         │
│  3. Merge & Deduplicate                                            │
│     assigneeIds = Stream.concat(results).distinct().collect()      │
│                           │                                         │
│                           ▼                                         │
│  4. Empty Strategy                                                 │
│     if (assigneeIds.isEmpty()) {                                   │
│         switch (emptyAssigneeStrategy) {                           │
│             case AUTO_PASS:    // 自动通过，不创建任务              │
│             case AUTO_REJECT:  // 自动驳回                         │
│             case ASSIGN_ADMIN: assigneeIds.add(adminId);           │
│             case ERROR:        throw new BusinessError();           │
│         }                                                         │
│     }                                                              │
│                           │                                         │
│                           ▼                                         │
│  5. Check Delegation                                               │
│     for (Long assigneeId : assigneeIds) {                          │
│         WfDelegation delegation = findActiveDelegation(assigneeId);│
│         if (delegation != null) {                                   │
│             assigneeId = delegation.getDelegateId();               │
│         }                                                          │
│     }                                                              │
│                           │                                         │
│                           ▼                                         │
│  6. Create Tasks by ApprovalMode                                   │
│     switch (approvalMode) {                                        │
│         case SEQUENTIAL:   // 按顺序创建，前一个完成后创建下一个   │
│         case COUNTERSIGN:  // 同时创建，全部同意才通过             │
│         case ORSIGN:       // 同时创建，一人同意即通过             │
│         case PROPORTIONAL: // 同时创建，统计同意比例               │
│         case VOTE:         // 同时创建，投票统计                   │
│     }                                                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.6 会签汇总算法

```java
public class SignHandler {
    
    /**
     * 判断会签节点是否通过
     * @return SignResult(PASSED/REJECTED/PENDING)
     */
    public SignResult evaluateSign(Long nodeId, Long instanceId) {
        WfNode node = nodeMapper.selectById(nodeId);
        List<WfTask> tasks = taskMapper.selectByInstanceAndNode(instanceId, nodeId);
        
        long approvedCount = tasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.APPROVED).count();
        long rejectedCount = tasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.REJECTED).count();
        long totalCount = tasks.size();
        
        switch (node.getApprovalMode()) {
            case COUNTERSIGN:
                // 任一驳回 → 节点驳回
                if (rejectedCount > 0) return SignResult.REJECTED;
                // 全部同意 → 通过
                if (approvedCount == totalCount) return SignResult.PASSED;
                return SignResult.PENDING;
                
            case ORSIGN:
                // 任一同意 → 通过
                if (approvedCount > 0) return SignResult.PASSED;
                // 全部驳回 → 驳回
                if (rejectedCount == totalCount) return SignResult.REJECTED;
                return SignResult.PENDING;
                
            case PROPORTIONAL:
                double ratio = (double) approvedCount / totalCount;
                if (ratio >= node.getPassRatio()) return SignResult.PASSED;
                if (rejectedCount > totalCount * (1 - node.getPassRatio())) 
                    return SignResult.REJECTED;
                return SignResult.PENDING;
                
            case VOTE:
                // 全部投票完毕
                if (approvedCount + rejectedCount == totalCount) {
                    return approvedCount > rejectedCount 
                        ? SignResult.PASSED : SignResult.REJECTED;
                }
                return SignResult.PENDING;
                
            default:
                return SignResult.PENDING;
        }
    }
}
```

### 2.7 驳回策略

| 策略 | 说明 | 示例场景 |
|------|------|----------|
| DIRECT_RETURN | 直接返回原驳回节点 | 员工→部门经理驳回→员工修改后直接返回部门经理 |
| SEQUENTIAL_RETURN | 按顺序重新走审批 | 员工→部门经理驳回→员工修改后重新从第一节点开始 |

**数据结构：**

```
驳回时记录：
  return_source_node_id = 当前节点ID
  return_strategy = 配置的策略

重新提交时：
  if (return_strategy == DIRECT_RETURN) {
    // 创建 return_source_node_id 的任务
  } else {
    // 从 start 后的第一个审批节点开始
  }
```

### 2.8 加签机制

```
前加签流程:
┌─────────┐     ┌─────────────┐     ┌─────────────┐
│ 原任务   │────►│ 原任务暂停   │────►│ 加签任务    │
│ PENDING │     │ SUSPENDED   │     │ PENDING     │
└─────────┘     └─────────────┘     └─────────────┘
                                          │
                                          ▼ 完成后
                                    ┌─────────────┐
                                    │ 原任务恢复   │
                                    │ PENDING     │
                                    └─────────────┘

后加签流程:
┌─────────┐     ┌─────────────┐
│ 原任务   │────►│ 原任务完成   │
│ PENDING │     │ APPROVED    │
└─────────┘     └─────────────┘
                      │
                      ▼ 检查后加签
                ┌─────────────┐
                │ 后加签任务   │────► 完成后继续原流程
                │ PENDING     │
                └─────────────┘
```

### 2.9 版本控制

```
发布新版本:
1. wf_definition.version = version + 1
2. 旧版本 wf_definition.status = DISABLED
3. 新版本 wf_definition.status = PUBLISHED

启动流程时:
1. 查询最新PUBLISHED版本
2. 复制 def_snapshot = {nodes, transitions, rules} 到实例
3. 实例运行期间使用快照，不受后续定义更新影响

查询历史版本:
SELECT * FROM wf_definition WHERE code = 'leave' ORDER BY version DESC
```

### 2.10 API设计

#### 2.10.1 流程定义API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/workflow/definitions` | 创建流程定义 |
| PUT | `/api/workflow/definitions/{id}` | 更新流程定义（仅DRAFT态） |
| POST | `/api/workflow/definitions/{id}/publish` | 发布流程定义 |
| POST | `/api/workflow/definitions/{id}/disable` | 禁用流程定义 |
| GET | `/api/workflow/definitions` | 查询流程定义列表 |
| GET | `/api/workflow/definitions/{id}` | 查询流程定义详情（含节点、流转、规则） |
| GET | `/api/workflow/definitions/{id}/versions` | 查询历史版本 |

#### 2.10.2 流程实例API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/workflow/instances` | 启动流程实例 |
| POST | `/api/workflow/instances/{id}/withdraw` | 撤回流程 |
| POST | `/api/workflow/instances/{id}/suspend` | 暂停流程 |
| POST | `/api/workflow/instances/{id}/resume` | 恢复流程 |
| POST | `/api/workflow/instances/{id}/abort` | 终止流程 |
| GET | `/api/workflow/instances/{id}` | 查询实例详情 |
| GET | `/api/workflow/instances/{id}/timeline` | 查询审批时间线 |
| GET | `/api/workflow/instances/my` | 查询我的申请 |

#### 2.10.3 任务API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/workflow/tasks/pending` | 查询待办任务 |
| GET | `/api/workflow/tasks/handled` | 查询已办任务 |
| POST | `/api/workflow/tasks/{id}/approve` | 同意任务 |
| POST | `/api/workflow/tasks/{id}/reject` | 驳回任务 |
| POST | `/api/workflow/tasks/{id}/transfer` | 转办任务 |
| POST | `/api/workflow/tasks/{id}/add-sign` | 加签 |
| POST | `/api/workflow/tasks/{id}/urge` | 催办 |
| GET | `/api/workflow/tasks/{id}` | 查询任务详情 |

#### 2.10.4 委托API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/workflow/delegations` | 设置委托 |
| GET | `/api/workflow/delegations` | 查询我的委托 |
| DELETE | `/api/workflow/delegations/{id}` | 取消委托 |

---

## 三、业务模块详细设计

### 3.1 公文管理模块 (`oa-document`)

#### 3.1.1 模块概述

公文管理实现发文、收文的电子化管理，支持红头文件、文号自动编制、正文留痕、OFD/PDF生成等政务公文特性。

#### 3.1.2 数据模型

**发文表 `doc_dispatch`**

```sql
CREATE TABLE `doc_dispatch` (
  `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
  `serial_no`            VARCHAR(64)   DEFAULT NULL            COMMENT '发文字号(如: 鄂科发〔2026〕15号)',
  `title`                VARCHAR(200)  NOT NULL                COMMENT '公文标题',
  `security_level`       VARCHAR(16)   DEFAULT 'NORMAL'        COMMENT '密级(NORMAL/SECRET/CONFIDENTIAL)',
  `urgency`              VARCHAR(16)   DEFAULT 'NORMAL'        COMMENT '紧急程度(NORMAL/URGENT/IMMEDIATE)',
  `issuing_org`          VARCHAR(200)  DEFAULT NULL            COMMENT '发文机关',
  `main_recipient`       VARCHAR(500)  DEFAULT NULL            COMMENT '主送单位',
  `cc_recipient`         VARCHAR(500)  DEFAULT NULL            COMMENT '抄送单位',
  `drafter_id`           BIGINT        NOT NULL                COMMENT '拟稿人ID',
  `draft_date`           DATE          NOT NULL                COMMENT '拟稿日期',
  `reviewer_id`          BIGINT        DEFAULT NULL            COMMENT '核稿人ID',
  `countersigner_ids`    VARCHAR(500)  DEFAULT NULL            COMMENT '会签人ID列表(JSON数组)',
  `signer_id`            BIGINT        DEFAULT NULL            COMMENT '签发人ID',
  `content_link`         VARCHAR(512)  DEFAULT NULL            COMMENT '正文链接(OFD/PDF)',
  `attachment_ids`       VARCHAR(500)  DEFAULT NULL            COMMENT '附件ID列表',
  `status`               VARCHAR(16)   DEFAULT 'DRAFT'         COMMENT '状态(DRAFT/REVIEWING/COUNTERSIGNING/SIGNING/DISPATCHED/REJECTED)',
  `dispatch_date`        DATE          DEFAULT NULL            COMMENT '发文日期',
  `process_instance_id`  BIGINT        DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`             CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`            VARCHAR(64)   DEFAULT NULL,
  `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`            VARCHAR(64)   DEFAULT NULL,
  `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_serial` (`serial_no`),
  KEY `idx_dispatch_drafter` (`drafter_id`),
  KEY `idx_dispatch_status` (`status`),
  KEY `idx_dispatch_date` (`draft_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发文表';
```

**收文表 `doc_receive`**

```sql
CREATE TABLE `doc_receive` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
  `from_org`            VARCHAR(200)  NOT NULL                COMMENT '来文单位',
  `original_serial`     VARCHAR(64)   NOT NULL                COMMENT '原文字号',
  `receive_date`        DATE          NOT NULL                COMMENT '收文日期',
  `title`               VARCHAR(200)  NOT NULL                COMMENT '公文标题',
  `security_level`      VARCHAR(16)   DEFAULT 'NORMAL'        COMMENT '密级',
  `urgency`             VARCHAR(16)   DEFAULT 'NORMAL'        COMMENT '紧急程度',
  `copy_count`          INT           DEFAULT NULL            COMMENT '份数',
  `proposed_opinion`    VARCHAR(500)  DEFAULT NULL            COMMENT '拟办意见',
  `approver_id`         BIGINT        DEFAULT NULL            COMMENT '批办人ID',
  `approved_opinion`    VARCHAR(500)  DEFAULT NULL            COMMENT '批办意见',
  `handler_id`          BIGINT        DEFAULT NULL            COMMENT '承办人ID',
  `handled_opinion`     VARCHAR(500)  DEFAULT NULL            COMMENT '承办意见',
  `circulation_record`  JSON          DEFAULT NULL            COMMENT '传阅记录(JSON)',
  `attachment_id`       VARCHAR(512)  DEFAULT NULL            COMMENT '附件ID',
  `status`              VARCHAR(16)   DEFAULT 'RECEIVED'      COMMENT '状态(RECEIVED/PROPOSED/APPROVED/HANDLING/ARCHIVED)',
  `del_flag`            CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`           VARCHAR(64)   DEFAULT NULL,
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`           VARCHAR(64)   DEFAULT NULL,
  `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_receive_from` (`from_org`(50)),
  KEY `idx_receive_status` (`status`),
  KEY `idx_receive_date` (`receive_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收文表';
```

**文号管理表 `doc_serial`**

```sql
CREATE TABLE `doc_serial` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `org_code`    VARCHAR(64)  NOT NULL                COMMENT '机关代字(如: 鄂科发)',
  `year`        INT          NOT NULL                COMMENT '年份',
  `serial_no`   INT          NOT NULL DEFAULT 0      COMMENT '当前序号',
  `status`      VARCHAR(16)  DEFAULT 'ACTIVE'        COMMENT '状态(ACTIVE/LOCKED)',
  `locked_by`   BIGINT       DEFAULT NULL            COMMENT '锁定人ID',
  `locked_at`   DATETIME     DEFAULT NULL            COMMENT '锁定时间',
  `used_at`     DATETIME     DEFAULT NULL            COMMENT '最近使用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_serial_org_year` (`org_code`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文号管理表';
```

**正文修订表 `doc_revision`**

```sql
CREATE TABLE `doc_revision` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `dispatch_id`   BIGINT       NOT NULL                COMMENT '发文ID',
  `version_no`    INT          NOT NULL                COMMENT '版本号',
  `content`       LONGTEXT     DEFAULT NULL            COMMENT '修订内容(HTML/JSON)',
  `diff_data`     JSON         DEFAULT NULL            COMMENT '差异标记(JSON)',
  `editor_id`     BIGINT       NOT NULL                COMMENT '编辑人ID',
  `edit_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `comment`       VARCHAR(500) DEFAULT NULL            COMMENT '修订说明',
  `is_clean`      TINYINT      DEFAULT 0               COMMENT '是否清稿(0否 1是)',
  PRIMARY KEY (`id`),
  KEY `idx_revision_dispatch` (`dispatch_id`),
  UNIQUE KEY `uk_revision_version` (`dispatch_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正文修订表';
```

#### 3.1.3 业务规则

**文号自动编制**

```
格式: {机关代字}〔{年份}〕{序号}号
示例: 鄂科发〔2026〕15号

流程:
1. 用户选择机关代字(如: 鄂科发)
2. 系统获取当前年份
3. 查询 doc_serial WHERE org_code=? AND year=?
4. 若不存在则创建: serial_no=0
5. 锁定记录: status=LOCKED, locked_by=当前用户, locked_at=now()
6. 返回预览: 鄂科发〔2026〕{serial_no+1}号
7. 用户确认发文后:
   - UPDATE serial_no = serial_no + 1, status=ACTIVE, used_at=now()
8. 用户取消:
   - UPDATE status=ACTIVE, locked_by=NULL
```

**正文留痕**

```javascript
// 使用 diff-match-patch 算法
// 前端实现:
{
  "version": 3,
  "diffs": [
    {"op": "EQUAL", "text": "第一条 "},
    {"op": "DELETE", "text": "原内容"},
    {"op": "INSERT", "text": "新内容"},
    {"op": "EQUAL", "text": "，规定如下..."}
  ],
  "editor": "张三",
  "editTime": "2026-06-02 15:30:00",
  "comment": "修改第一条表述"
}

// 清稿: 保留最终版本，清除所有修订痕迹
```

#### 3.1.4 API设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/document/dispatch` | 创建发文 |
| PUT | `/api/document/dispatch/{id}` | 更新发文 |
| POST | `/api/document/dispatch/{id}/submit` | 提交审批 |
| POST | `/api/document/dispatch/{id}/serial-lock` | 锁定文号 |
| POST | `/api/document/dispatch/{id}/serial-release` | 释放文号 |
| GET | `/api/document/dispatch/{id}/revisions` | 查询修订历史 |
| POST | `/api/document/dispatch/{id}/clean` | 清稿 |
| GET | `/api/document/dispatch/{id}/download` | 下载正文(OFD/PDF) |
| POST | `/api/document/receive` | 登记收文 |
| PUT | `/api/document/receive/{id}/propose` | 拟办 |
| PUT | `/api/document/receive/{id}/approve` | 批办 |
| PUT | `/api/document/receive/{id}/handle` | 承办 |
| GET | `/api/document/serial/preview` | 预览文号 |

---

### 3.2 文档与知识管理模块 (`oa-knowledge`)

#### 3.2.1 模块概述

文档知识管理模块提供企业知识库功能，包括文档版本控制、全文检索（Elasticsearch）、多维标签分类、知识推荐等。

#### 3.2.2 数据模型

**知识词条表 `km_entry`**

```sql
CREATE TABLE `km_entry` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `title`            VARCHAR(200) NOT NULL                COMMENT '标题',
  `current_version`  INT          NOT NULL DEFAULT 1      COMMENT '当前版本号',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT/PUBLISHED/ARCHIVED)',
  `dept_id`          BIGINT       DEFAULT NULL            COMMENT '归属部门ID',
  `security_level`   VARCHAR(16)  DEFAULT 'PUBLIC'         COMMENT '密级(PUBLIC/INTERNAL/SECRET)',
  `category_id`      BIGINT       DEFAULT NULL            COMMENT '分类ID',
  `view_count`       INT          NOT NULL DEFAULT 0      COMMENT '浏览次数',
  `download_count`   INT          NOT NULL DEFAULT 0      COMMENT '下载次数',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0',
  `create_by`        VARCHAR(64)  DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`        VARCHAR(64)  DEFAULT NULL,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_km_entry_dept` (`dept_id`),
  KEY `idx_km_entry_category` (`category_id`),
  KEY `idx_km_entry_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识词条表';
```

**知识版本表 `km_version`**

```sql
CREATE TABLE `km_version` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `entry_id`      BIGINT       NOT NULL                COMMENT '词条ID',
  `version_no`    INT          NOT NULL                COMMENT '版本号',
  `file_path`     VARCHAR(512) NOT NULL                COMMENT '文件路径',
  `file_size`     BIGINT       NOT NULL                COMMENT '文件大小(字节)',
  `file_type`     VARCHAR(32)  DEFAULT NULL            COMMENT '文件类型(pdf/docx/xlsx/pptx)',
  `file_hash`     VARCHAR(64)  DEFAULT NULL            COMMENT '文件MD5/SHA256',
  `uploader_id`   BIGINT       NOT NULL                COMMENT '上传人ID',
  `upload_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `comment`       VARCHAR(500) DEFAULT NULL            COMMENT '版本说明',
  PRIMARY KEY (`id`),
  KEY `idx_km_version_entry` (`entry_id`),
  UNIQUE KEY `uk_version_entry_no` (`entry_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识版本表';
```

**知识标签表 `km_tag`**

```sql
CREATE TABLE `km_tag` (
  `id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `entry_id`  BIGINT       NOT NULL                COMMENT '词条ID',
  `tag_name`  VARCHAR(64)  NOT NULL                COMMENT '标签名',
  `tag_type`  VARCHAR(16)  DEFAULT 'CUSTOM'        COMMENT '标签类型(CUSTOM/SYSTEM/CATEGORY)',
  PRIMARY KEY (`id`),
  KEY `idx_km_tag_entry` (`entry_id`),
  KEY `idx_km_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识标签表';
```

**知识关联表 `km_relation`**

```sql
CREATE TABLE `km_relation` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `entry_id`         BIGINT NOT NULL                COMMENT '词条ID',
  `related_entry_id` BIGINT NOT NULL                COMMENT '关联词条ID',
  `relation_type`    VARCHAR(32) DEFAULT 'REFERENCE' COMMENT '关联类型(REFERENCE/SIMILAR/PARENT/SEE_ALSO)',
  `score`           DOUBLE DEFAULT NULL             COMMENT '关联得分(0.0-1.0)',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_km_relation_entry` (`entry_id`),
  KEY `idx_km_relation_related` (`related_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识关联表';
```

#### 3.2.3 Elasticsearch索引设计

**索引 `km_document`**

```json
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart"
        },
        "ik_max_word_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "title": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": {"type": "keyword"}
        }
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "search_analyzer": "ik_smart_analyzer"
      },
      "tags": {"type": "keyword"},
      "category_id": {"type": "long"},
      "dept_id": {"type": "long"},
      "security_level": {"type": "keyword"},
      "status": {"type": "keyword"},
      "file_type": {"type": "keyword"},
      "view_count": {"type": "integer"},
      "download_count": {"type": "integer"},
      "create_time": {"type": "date"},
      "update_time": {"type": "date"}
    }
  }
}
```

#### 3.2.4 API设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge/entries` | 创建词条 |
| POST | `/api/knowledge/entries/{id}/versions` | 上传新版本 |
| PUT | `/api/knowledge/entries/{id}/publish` | 发布词条 |
| GET | `/api/knowledge/entries/{id}` | 查询词条详情 |
| GET | `/api/knowledge/entries/{id}/versions` | 查询版本历史 |
| GET | `/api/knowledge/entries/{id}/versions/{version}` | 下载指定版本 |
| POST | `/api/knowledge/search` | 全文检索(ES) |
| GET | `/api/knowledge/recommend` | 知识推荐(协同过滤) |
| POST | `/api/knowledge/entries/{id}/tags` | 添加标签 |
| DELETE | `/api/knowledge/entries/{id}/tags/{tagId}` | 删除标签 |
| GET | `/api/knowledge/tags/hot` | 热门标签 |

---

### 3.3 任务与项目管理模块 (`oa-task`)

#### 3.3.1 模块概述

任务项目管理是全新构建的模块，实现项目→任务→子任务的层级管理，支持甘特图、看板、工时记录、任务依赖等完整功能。

#### 3.3.2 数据模型

**项目表 `task_project`**

```sql
CREATE TABLE `task_project` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `name`             VARCHAR(200)  NOT NULL                COMMENT '项目名称',
  `description`      TEXT          DEFAULT NULL            COMMENT '项目描述',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'PLANNING' COMMENT '状态(PLANNING/IN_PROGRESS/COMPLETED/CANCELLED)',
  `progress`         INT           NOT NULL DEFAULT 0      COMMENT '完成进度(0-100)',
  `owner_id`         BIGINT        NOT NULL                COMMENT '项目负责人ID',
  `planned_start`    DATE          DEFAULT NULL            COMMENT '计划开始日期',
  `planned_end`      DATE          DEFAULT NULL            COMMENT '计划结束日期',
  `actual_start`     DATE          DEFAULT NULL            COMMENT '实际开始日期',
  `actual_end`       DATE          DEFAULT NULL            COMMENT '实际结束日期',
  `del_flag`         CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`        VARCHAR(64)   DEFAULT NULL,
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`        VARCHAR(64)   DEFAULT NULL,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_owner` (`owner_id`),
  KEY `idx_project_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';
```

**项目成员表 `task_project_member`**

```sql
CREATE TABLE `task_project_member` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `project_id`  BIGINT      NOT NULL                COMMENT '项目ID',
  `emp_id`      BIGINT      NOT NULL                COMMENT '成员ID',
  `role`        VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '角色(OWNER/ADMIN/MEMBER)',
  `joined_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_member` (`project_id`, `emp_id`),
  KEY `idx_member_project` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';
```

**任务表 `task_item`**

```sql
CREATE TABLE `task_item` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `project_id`       BIGINT        DEFAULT NULL            COMMENT '项目ID(NULL表示个人任务)',
  `parent_task_id`   BIGINT        DEFAULT NULL            COMMENT '父任务ID',
  `title`            VARCHAR(200)  NOT NULL                COMMENT '任务标题',
  `description`      TEXT          DEFAULT NULL            COMMENT '任务描述',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'TODO' COMMENT '状态(TODO/IN_PROGRESS/DONE/CLOSED/OVERDUE)',
  `priority`         VARCHAR(16)   DEFAULT 'NORMAL'        COMMENT '优先级(LOW/NORMAL/HIGH/URGENT)',
  `progress`         INT           NOT NULL DEFAULT 0      COMMENT '完成进度(0-100)',
  `assignee_id`      BIGINT        NOT NULL                COMMENT '负责人ID',
  `creator_id`       BIGINT        NOT NULL                COMMENT '创建人ID',
  `planned_start`    DATE          DEFAULT NULL            COMMENT '计划开始日期',
  `planned_end`      DATE          DEFAULT NULL            COMMENT '计划结束日期',
  `actual_start`     DATETIME      DEFAULT NULL            COMMENT '实际开始时间',
  `actual_end`       DATETIME      DEFAULT NULL            COMMENT '实际完成时间',
  `estimated_hours`  DECIMAL(6,1)  DEFAULT NULL            COMMENT '预估工时(小时)',
  `actual_hours`     DECIMAL(6,1)  DEFAULT 0.0             COMMENT '实际工时(小时)',
  `tags`             VARCHAR(500)  DEFAULT NULL            COMMENT '标签(JSON数组)',
  `sort_order`       INT           NOT NULL DEFAULT 0      COMMENT '排序号',
  `del_flag`         CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`        VARCHAR(64)   DEFAULT NULL,
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`        VARCHAR(64)   DEFAULT NULL,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_project` (`project_id`),
  KEY `idx_task_parent` (`parent_task_id`),
  KEY `idx_task_assignee` (`assignee_id`, `status`),
  KEY `idx_task_creator` (`creator_id`),
  KEY `idx_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';
```

**任务依赖表 `task_dependency`**

```sql
CREATE TABLE `task_dependency` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `task_id`          BIGINT NOT NULL                COMMENT '任务ID',
  `depends_on_task_id` BIGINT NOT NULL              COMMENT '依赖任务ID',
  `dependency_type`  VARCHAR(16) NOT NULL DEFAULT 'FINISH_TO_START' COMMENT '依赖类型(FINISH_TO_START/START_TO_START/FINISH_TO_FINISH/START_TO_FINISH)',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dependency` (`task_id`, `depends_on_task_id`),
  KEY `idx_depends_on` (`depends_on_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖表';
```

**工时记录表 `task_hours`**

```sql
CREATE TABLE `task_hours` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `task_id`     BIGINT        NOT NULL                COMMENT '任务ID',
  `emp_id`      BIGINT        NOT NULL                COMMENT '记录人ID',
  `work_date`   DATE          NOT NULL                COMMENT '工作日期',
  `hours`       DECIMAL(4,1)  NOT NULL                COMMENT '工时(0.5-24)',
  `description` VARCHAR(500)  DEFAULT NULL            COMMENT '工作内容',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hours_task` (`task_id`),
  KEY `idx_hours_emp_date` (`emp_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时记录表';
```

**任务评论表 `task_comment`**

```sql
CREATE TABLE `task_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`     BIGINT       NOT NULL                COMMENT '任务ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '评论人ID',
  `content`     TEXT         NOT NULL                COMMENT '评论内容',
  `reply_to_id` BIGINT       DEFAULT NULL            COMMENT '回复评论ID',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_comment_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务评论表';
```

#### 3.3.3 甘特图数据结构

```typescript
// 前端甘特图数据结构
interface GanttTask {
  id: string;
  name: string;
  start: Date;
  end: Date;
  progress: number;
  type: 'project' | 'task' | 'milestone';
  assignee?: string;
  dependencies: string[];
  children?: GanttTask[];
  color?: string;
}

// API响应
interface GanttData {
  tasks: GanttTask[];
  links: { id: string; source: string; target: string; type: string }[];
  dateRange: { start: Date; end: Date };
}
```

#### 3.3.4 API设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/task/projects` | 创建项目 |
| PUT | `/api/task/projects/{id}` | 更新项目 |
| GET | `/api/task/projects` | 查询项目列表 |
| GET | `/api/task/projects/{id}` | 查询项目详情 |
| POST | `/api/task/projects/{id}/members` | 添加项目成员 |
| DELETE | `/api/task/projects/{id}/members/{empId}` | 移除项目成员 |
| POST | `/api/task/tasks` | 创建任务 |
| PUT | `/api/task/tasks/{id}` | 更新任务 |
| PUT | `/api/task/tasks/{id}/status` | 更新任务状态 |
| PUT | `/api/task/tasks/{id}/progress` | 更新任务进度 |
| GET | `/api/task/tasks` | 查询任务列表(支持多种筛选) |
| GET | `/api/task/tasks/{id}` | 查询任务详情 |
| GET | `/api/task/tasks/{id}/subtasks` | 查询子任务 |
| POST | `/api/task/tasks/{id}/dependencies` | 添加任务依赖 |
| DELETE | `/api/task/tasks/{id}/dependencies/{depId}` | 删除任务依赖 |
| POST | `/api/task/tasks/{id}/hours` | 记录工时 |
| GET | `/api/task/tasks/{id}/hours` | 查询工时记录 |
| POST | `/api/task/tasks/{id}/comments` | 添加评论 |
| GET | `/api/task/tasks/{id}/comments` | 查询评论 |
| GET | `/api/task/projects/{id}/gantt` | 查询甘特图数据 |
| GET | `/api/task/projects/{id}/kanban` | 查询看板数据 |
| GET | `/api/task/my/tasks` | 我的任务 |
| GET | `/api/task/my/hours` | 我的工时统计 |

---

### 3.4 其他模块摘要

因篇幅限制，其他模块仅列出核心表结构，详细设计可后续补充。

#### 3.4.1 会议管理 (`oa-meeting`)

| 表名 | 说明 |
|------|------|
| `mt_room` | 会议室（name, capacity, devices, location, gps） |
| `mt_booking` | 预定（room_id, title, start_time, end_time, participants, status） |
| `mt_resolution` | 决议（booking_id, content, assignee_id, due_date, task_id） |
| `mt_signin` | 签到（booking_id, emp_id, signin_time, signin_type, location） |

#### 3.4.2 人事管理 (`oa-hr`)

| 表名 | 说明 |
|------|------|
| `hr_employee_ext` | 员工扩展（试用期、合同期、紧急联系人） |
| `hr_transfer` | 异动记录（type: 入职/调动/离职, from_dept/to_dept） |
| `hr_attendance` | 考勤记录（clock_in/out, status, source_type/id） |
| `hr_leave_balance` | 假期余额（emp_id, type, year, total/used/remaining） |
| `hr_leave_rule` | 假期规则（rule_script: Groovy/SpEL） |

#### 3.4.3 综合行政 (`oa-admin`)

| 表名 | 说明 |
|------|------|
| `adm_seal` | 印章（name, type, keeper_id, status） |
| `adm_seal_usage` | 用印记录（seal_id, applicant_id, document_name） |
| `adm_supply` | 办公用品（name, specification, unit, category） |
| `adm_supply_stock` | 库存（total/available/locked, version乐观锁） |
| `adm_asset` | 固定资产（code, name, sn, status, current_user_id） |
| `adm_asset_log` | 资产变动（operation, operator_id, from/to_user_id） |

#### 3.4.4 费控报销 (`oa-finance`)

| 表名 | 说明 |
|------|------|
| `fin_budget` | 预算（dept_id, category, year, month, amount, occupied, executed） |
| `fin_expense` | 报销单（emp_id, total_amount, related_trip/loan_id） |
| `fin_expense_detail` | 报销明细（expense_id, date, type, amount, invoice_no） |
| `fin_loan` | 借款（emp_id, loan_amount, repaid_amount） |
| `fin_contract` | 合同（contract_no, party_a/b, amount, start/end_date） |
| `fin_payment` | 付款（contract_id, payee, amount, pay_date） |

---

## 四、消息通知中台设计

### 4.1 架构设计

```
┌─────────────────────────────────────────────────────────────────────┐
│                       MessageNotificationService                     │
├─────────────────────────────────────────────────────────────────────┤
│  notify(MessageType type, Long userId, String title,                │
│          String content, Map<String, Object> params)                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│  │ MessageQueue│───►│ ChannelRouter│───►│ UserPreference│           │
│  │ (Redis)     │    │             │    │ Store       │            │
│  └─────────────┘    └─────────────┘    └─────────────┘            │
│                           │                                         │
│         ┌─────────────────┼─────────────────┐                      │
│         │                 │                 │                      │
│         ▼                 ▼                 ▼                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                │
│  │ SiteMessage │  │ EmailSender │  │ SmsSender   │                │
│  │ Channel     │  │ (SMTP)      │  │ (阿里云)     │                │
│  └─────────────┘  └─────────────┘  └─────────────┘                │
│         │                 │                 │                      │
│         ▼                 ▼                 ▼                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                │
│  │ WebSocket   │  │ 企业微信     │  │ 钉钉        │                │
│  │ Push        │  │ Adapter     │  │ Adapter     │                │
│  └─────────────┘  └─────────────┘  └─────────────┘                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 用户偏好配置

```sql
CREATE TABLE `msg_user_preference` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `emp_id`      BIGINT      NOT NULL,
  `msg_type`    VARCHAR(32) NOT NULL COMMENT '消息类型(TODO_ASSIGN/TODO_URGE/APPROVAL_PASS/...)',
  `channels`    VARCHAR(128) NOT NULL COMMENT '启用渠道(JSON数组): ["SITE","EMAIL","SMS","WECHAT"]',
  `enabled`     TINYINT     NOT NULL DEFAULT 1,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pref_emp_type` (`emp_id`, `msg_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息偏好配置';
```

### 4.3 消息类型与渠道映射

| 消息类型 | 默认渠道 | 说明 |
|----------|----------|------|
| TODO_ASSIGN | SITE, WECHAT | 待办任务分配 |
| TODO_URGE | SITE, WECHAT | 催办提醒 |
| APPROVAL_PASS | SITE | 审批通过通知 |
| APPROVAL_REJECT | SITE, SMS | 审批驳回通知(重要) |
| NOTICE_PUBLISH | SITE, EMAIL | 公告发布通知 |
| MEETING_REMIND | SITE, SMS, WECHAT | 会议即将开始提醒 |
| SYSTEM_ALERT | SITE, EMAIL, SMS | 系统告警(管理员) |

---

## 五、前端架构设计

### 5.1 pnpm monorepo结构

```
code/frontend/
├── package.json              # workspace定义
├── pnpm-workspace.yaml
├── pnpm-lock.yaml
│
├── packages/
│   ├── shared/               # 共享组件库
│   │   ├── package.json
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── WorkflowDesigner/    # 流程设计器(vue-flow)
│   │   │   │   ├── DynamicForm/         # 动态表单
│   │   │   │   ├── ApprovalTimeline/   # 审批时间线
│   │   │   │   ├── FileUpload/         # 文件上传(断点续传)
│   │   │   │   ├── Watermark/          # 文件水印
│   │   │   │   ├── GanttChart/         # 甘特图
│   │   │   │   └── KanbanBoard/        # 看板
│   │   │   ├── composables/
│   │   │   │   ├── useAuth.ts
│   │   │   │   ├── usePermission.ts
│   │   │   │   └── useNotification.ts
│   │   │   ├── utils/
│   │   │   └── styles/
│   │   └── index.ts
│   │
│   ├── admin/                # 管理端应用
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   ├── src/
│   │   │   ├── views/
│   │   │   │   ├── workflow/     # 流程管理
│   │   │   │   ├── document/     # 公文管理
│   │   │   │   ├── knowledge/    # 知识库
│   │   │   │   ├── meeting/      # 会议管理
│   │   │   │   ├── task/         # 任务项目
│   │   │   │   ├── hr/           # 人事管理
│   │   │   │   ├── admin/        # 综合行政
│   │   │   │   ├── finance/      # 费控报销
│   │   │   │   ├── system/       # 系统管理
│   │   │   │   └── dashboard/    # 仪表盘
│   │   │   ├── router/
│   │   │   ├── stores/
│   │   │   └── App.vue
│   │   └── index.html
│   │
│   └── mobile/               # 移动端应用(uni-app)
│       ├── package.json
│       ├── src/
│       │   ├── pages/
│       │   │   ├── home/
│       │   │   ├── todo/
│       │   │   ├── workflow/
│       │   │   ├── task/
│       │   │   ├── mine/
│       │   │   └── login/
│       │   └── App.vue
│       └── manifest.json      # uni-app配置
```

### 5.2 核心组件设计

**流程设计器 (WorkflowDesigner)**

```vue
<template>
  <div class="workflow-designer">
    <VueFlow v-model="nodes" v-model:edges="edges">
      <template #node-start>
        <StartNode />
      </template>
      <template #node-approval>
        <ApprovalNode :data="nodeData" @edit="onEditNode" />
      </template>
      <template #node-end>
        <EndNode />
      </template>
    </VueFlow>
    
    <NodeConfigPanel v-model="selectedNode" />
  </div>
</template>

<script setup lang="ts">
import { VueFlow, useVueFlow } from '@vue-flow/core';
import type { Node, Edge } from '@vue-flow/core';

const { nodes, edges, onConnect, addNodes } = useVueFlow();

// 节点类型定义
type NodeType = 'start' | 'end' | 'approval' | 'condition' | 'subprocess';
</script>
```

**动态表单 (DynamicForm)**

```vue
<template>
  <el-form ref="formRef" :model="formData" :rules="formRules">
    <template v-for="field in fields" :key="field.key">
      <el-form-item 
        v-if="fieldPermission[field.key] !== 'hidden'"
        :label="field.label"
        :prop="field.key"
        :class="{ 'is-readonly': fieldPermission[field.key] === 'readonly' }"
      >
        <!-- 根据field.type渲染不同控件 -->
        <component 
          :is="getComponent(field.type)"
          v-model="formData[field.key]"
          :disabled="fieldPermission[field.key] === 'readonly'"
          v-bind="field.props"
        />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
interface FieldConfig {
  key: string;
  label: string;
  type: 'input' | 'select' | 'date' | 'number' | 'textarea' | 'upload';
  props?: Record<string, any>;
  rules?: any[];
}

const props = defineProps<{
  fields: FieldConfig[];
  modelValue: Record<string, any>;
  permission: Record<string, 'editable' | 'readonly' | 'hidden'>;
}>();
</script>
```

---

## 六、测试策略

### 6.1 测试金字塔

```
                    ┌─────────────┐
                    │    E2E      │  ← Playwright (关键路径)
                    │   (少量)    │
                ┌───┴─────────────┴───┐
                │   Integration Test  │  ← Testcontainers (集成测试)
                │      (适量)         │
            ┌───┴─────────────────────┴───┐
            │      Unit Test              │  ← Mockito/Jest (单元测试)
            │       (大量)                │
            └─────────────────────────────┘
```

### 6.2 后端测试策略

#### 6.2.1 单元测试

**技术栈**: JUnit 5 + Mockito + AssertJ

**覆盖目标**: Service层 >= 80%

**关键测试点**:

```java
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {
    
    @Mock
    private WfDefinitionMapper definitionMapper;
    
    @Mock
    private WfTaskMapper taskMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private WorkflowEngineImpl workflowEngine;
    
    // 测试流程启动
    @Test
    void shouldStartWorkflowSuccessfully() {
        // Given
        WfDefinition definition = createTestDefinition();
        when(definitionMapper.selectById(1L)).thenReturn(definition);
        
        StartWorkflowDTO dto = new StartWorkflowDTO();
        dto.setDefId(1L);
        dto.setBusinessType("leave");
        dto.setBusinessId(100L);
        
        // When
        Long instanceId = workflowEngine.startWorkflow(dto);
        
        // Then
        assertThat(instanceId).isNotNull();
        verify(taskMapper).insert(any(WfTask.class));
        verify(eventPublisher).publishEvent(any(InstanceStartedEvent.class));
    }
    
    // 测试审批人为空策略
    @Test
    void shouldAutoPassWhenNoAssignee() {
        // Given
        WfDefinition definition = createDefinitionWithEmptyAssignee();
        definition.setEmptyAssigneeStrategy("AUTO_PASS");
        
        // When
        workflowEngine.processNode(instanceId, nodeId);
        
        // Then
        verify(taskMapper, never()).insert(any(WfTask.class));
        // 应直接进入下一节点
    }
    
    // 测试会签汇总 - 全部同意才通过
    @Test
    void shouldPassCountersignWhenAllApproved() {
        // Given
        List<WfTask> tasks = createCountersignTasks(3, TaskStatus.APPROVED, TaskStatus.APPROVED, TaskStatus.PENDING);
        
        // When
        SignResult result = signHandler.evaluateSign(nodeId, instanceId);
        
        // Then
        assertThat(result).isEqualTo(SignResult.PENDING);
        
        // 最后一个也同意
        tasks.get(2).setStatus(TaskStatus.APPROVED);
        result = signHandler.evaluateSign(nodeId, instanceId);
        assertThat(result).isEqualTo(SignResult.PASSED);
    }
}
```

**测试类统计**:

| 模块 | 测试类数 | 核心测试点 |
|------|---------|-----------|
| oa-workflow | 12 | 流程启动/状态机/审批人解析/会签汇总/驳回/加签 |
|oa-document | 4 | 文号生成/留痕对比/OFD生成 |
|oa-knowledge | 5 | 版本控制/ES同步/推荐算法 |
|oa-task | 6 | 任务层级/依赖检测/甘特图计算/工时统计 |
|oa-hr | 5 | 考勤规则/假期余额扣减/异动处理 |
|oa-admin | 5 | 库存并发控制/资产变动 |
|oa-finance | 5 | 预算占用/释放/报销核销 |
|oa-platform | 4 | JWT验证/权限校验/异常处理 |

#### 6.2.2 集成测试

**技术栈**: @SpringBootTest + Testcontainers

**测试范围**: 跨Service协作、数据库事务、Redis操作

```java
@SpringBootTest
@Testcontainers
class WorkflowIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("oa_test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.redis.host", redis::getHost);
    }
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private WfInstanceMapper instanceMapper;
    
    @Test
    void shouldCompleteFullWorkflowLifecycle() {
        // 启动流程
        Long instanceId = workflowEngine.startWorkflow(createStartDTO());
        
        // 查询待办任务
        List<WfTask> pendingTasks = taskMapper.selectPending(userId);
        assertThat(pendingTasks).hasSize(1);
        
        // 审批通过
        workflowEngine.approve(pendingTasks.get(0).getId(), "同意");
        
        // 验证状态
        WfInstance instance = instanceMapper.selectById(instanceId);
        assertThat(instance.getStatus()).isEqualTo(InstanceStatus.PASSED);
    }
}
```

#### 6.2.3 测试数据管理

**策略**: 使用Flyway + 测试专用数据脚本

```
code/backend/
├── oa-web/src/test/
│   ├── resources/
│   │   ├── application-test.yml
│   │   └── db/
│   │       ├── V1__Schema.sql           # 表结构(复用正式)
│   │       └── R__Test_Data.sql          # 测试数据(可重复执行)
```

**测试数据脚本示例**:

```sql
-- R__Test_Data.sql

-- 清理旧数据
DELETE FROM sys_employee WHERE emp_code LIKE 'TEST_%';
DELETE FROM wf_definition WHERE code LIKE 'TEST_%';

-- 插入测试用户
INSERT INTO sys_employee (id, emp_code, emp_name, password, status, del_flag)
VALUES 
(9001, 'TEST_ADMIN', '测试管理员', '$2a$10$...', 1, '0'),
(9002, 'TEST_USER1', '测试用户1', '$2a$10$...', 1, '0'),
(9003, 'TEST_USER2', '测试用户2', '$2a$10$...', 1, '0');

-- 插入测试流程定义
INSERT INTO wf_definition (id, code, name, version, status)
VALUES (9001, 'TEST_LEAVE', '测试请假流程', 1, 'PUBLISHED');
```

### 6.3 前端测试策略

#### 6.3.1 组件测试

**技术栈**: Vitest + Vue Test Utils

```typescript
// WorkflowDesigner.test.ts
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import WorkflowDesigner from '@/components/WorkflowDesigner.vue';

describe('WorkflowDesigner', () => {
  it('should render nodes from props', () => {
    const nodes = [
      { id: '1', type: 'start', position: { x: 0, y: 0 } },
      { id: '2', type: 'approval', position: { x: 200, y: 0 } },
    ];
    
    const wrapper = mount(WorkflowDesigner, {
      props: { modelValue: nodes }
    });
    
    expect(wrapper.findAll('.vue-flow__node')).toHaveLength(2);
  });
  
  it('should emit update when node moved', async () => {
    const wrapper = mount(WorkflowDesigner);
    // ...模拟节点移动
    await wrapper.vm.onNodeDragStop({ id: '1', position: { x: 100, y: 100 } });
    
    expect(wrapper.emitted('update:modelValue')).toBeTruthy();
  });
});
```

#### 6.3.2 E2E测试

**技术栈**: Playwright

**测试场景覆盖**:

```typescript
// tests/e2e/workflow.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Workflow Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });
  
  test('should create and publish workflow definition', async ({ page }) => {
    await page.click('text=流程管理');
    await page.click('text=新建流程');
    
    await page.fill('[name="code"]', 'TEST_FLOW');
    await page.fill('[name="name"]', '测试流程');
    
    // 拖拽添加节点
    await page.dragAndDrop('[data-node-type="start"]', '.designer-canvas');
    await page.dragAndDrop('[data-node-type="approval"]', '.designer-canvas');
    await page.dragAndDrop('[data-node-type="end"]', '.designer-canvas');
    
    // 连接节点
    await page.click('[data-node-id="start"] .handle-right');
    await page.click('[data-node-id="approval"] .handle-left');
    
    // 发布
    await page.click('button:has-text("发布")');
    await expect(page.locator('.toast-success')).toBeVisible();
  });
  
  test('should complete approval workflow', async ({ page }) => {
    // 提交请假申请
    await page.click('text=我的申请');
    await page.click('text=新建请假');
    await page.selectOption('[name="leaveType"]', '年假');
    await page.fill('[name="reason"]', '测试请假');
    await page.click('button:has-text("提交")');
    
    // 切换审批人账号
    await page.context().clearCookies();
    await loginAs(page, 'approver', 'password');
    
    // 审批通过
    await page.click('text=待办事项');
    await page.click('.task-item:has-text("请假申请")');
    await page.fill('[name="opinion"]', '同意');
    await page.click('button:has-text("同意")');
    
    // 验证状态
    await expect(page.locator('.task-status')).toContainText('已通过');
  });
});
```

**E2E测试场景清单**:

| 场景 | 测试文件 | 核心断言 |
|------|---------|----------|
| 用户登录/登出 | auth.spec.ts | Token存储、页面跳转 |
| 流程定义CRUD | workflow-def.spec.ts | 节点渲染、连接、发布 |
| 请假审批流程 | workflow-leave.spec.ts | 提交→审批→状态变更 |
| 报销审批流程 | workflow-expense.spec.ts | 预算校验、审批通过 |
| 知识库搜索 | knowledge.spec.ts | ES搜索结果、分页 |
| 任务管理 | task.spec.ts | 任务创建、甘特图、状态更新 |
| 会议预定 | meeting.spec.ts | 冲突检测、签到 |

### 6.4 测试覆盖率目标

| 层级 | 行覆盖率 | 分支覆盖率 | 工具 |
|------|---------|-----------|------|
| Service层 | >= 80% | >= 70% | JaCoCo |
| Controller层 | >= 70% | >= 60% | JaCoCo |
| 前端组件 | >= 60% | >= 50% | Vitest |
| E2E关键路径 | 100%场景 | - | Playwright |

**CI门禁配置**:

```yaml
# .github/workflows/test.yml
- name: Run tests with coverage
  run: mvn verify -P coverage
  
- name: Check coverage threshold
  run: |
    python3 << 'EOF'
    import csv
    with open('target/site/jacoco/jacoco.csv') as f:
        reader = csv.DictReader(f)
        total_covered = sum(int(r['COVERED_LINE']) for r in reader)
        total_missed = sum(int(r['MISSED_LINE']) for r in reader)
    rate = total_covered / (total_covered + total_missed) * 100
    if rate < 70:
        print(f'FAIL: Coverage {rate:.2f}% < 70%')
        exit(1)
    print(f'PASS: Coverage {rate:.2f}%')
    EOF
```

---

## 七、安全设计

### 7.1 认证授权

```
┌─────────────────────────────────────────────────────────────────────┐
│                          AuthInterceptor                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  请求 ──► [白名单检查] ──► [Token解析] ──► [Redis会话校验]        │
│              │              │              │                        │
│              ▼              ▼              ▼                        │
│          放行请求      JWT签名验证    Token是否有效                  │
│                             │              │                        │
│                             ▼              ▼                        │
│                         失败返回401    失败返回401                   │
│                                            │                        │
│                                            ▼                        │
│                                    [角色/权限检查]                  │
│                                            │                        │
│                                    @RequireRole("ADMIN")            │
│                                    @RequirePermission("user:add")   │
│                                            │                        │
│                                            ▼                        │
│                                        放行/403                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 数据权限

**SQL拦截器实现**:

```java
@Intercepts({@Signature(type = Executor.class, method = "query", args = {...})})
public class DataPermissionInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) {
        // 获取当前用户的数据权限范围
        UserDetails user = UserContext.get();
        List<Long> deptIds = user.getDataScopeDeptIds();
        
        // 动态添加WHERE条件
        // dept_id IN (...) OR creator_id = ?
        
        return invocation.proceed();
    }
}
```

### 7.3 敏感数据脱敏

| 字段 | 存储方式 | 展示方式 |
|------|---------|----------|
| 密码 | BCrypt哈希 | 不可见 |
| 手机号 | 明文 | 138****1234 |
| 身份证 | AES加密 | 420***********1234 |
| 银行卡 | AES加密 | 6222 **** **** 1234 |
| 薪资 | AES加密 | 按权限展示 |

---

## 八、部署架构

### 8.1 生产环境拓扑

```
                        ┌─────────────────────┐
                        │    Nginx (反向代理)   │
                        │   /api → Backend    │
                        │   /*  → Frontend    │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                    ▼                             ▼
        ┌───────────────────┐         ┌───────────────────┐
        │  Frontend Server  │         │  Backend Server    │
        │  (Vue SPA)        │         │  (Spring Boot)     │
        │  :8848           │         │  :8080            │
        └───────────────────┘         └──────────┬─────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
        ┌───────────────────┐     ┌───────────────────┐     ┌───────────────────┐
        │     MySQL 8.0     │     │     Redis 7.x     │     │ Elasticsearch 8.x │
        │    (主从复制)      │     │   (哨兵模式)       │     │    (单节点)        │
        └───────────────────┘     └───────────────────┘     └───────────────────┘
```

### 8.2 Docker Compose配置

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: oa_system
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    
  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    
  backend:
    build: ./code/backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      REDIS_HOST: redis
      ES_HOST: elasticsearch
    depends_on:
      - mysql
      - redis
      - elasticsearch
    ports:
      - "8080:8080"
    
  frontend:
    build: ./code/frontend/packages/admin
    ports:
      - "8848:80"
    depends_on:
      - backend

volumes:
  mysql_data:
  redis_data:
  es_data:
```

---

## 九、详细实施计划

### Phase 1: 基础设施与工作流引擎（第一优先级）

| 周次 | 任务 | 详细内容 | 验收标准 |
|------|------|---------|----------|
| W1 | 模块结构搭建 | 创建oa-platform、oa-workflow各子模块pom.xml，配置模块依赖关系 | mvn clean install成功 |
| W2 | 平台核心完成 | 异常体系(BusinessError/AuthError)、统一响应R<T>、常量定义、JWT工具类 | 单元测试通过 |
| W3 | 认证授权完成 | AuthInterceptor、TokenService、Redis会话管理、@RequireRole注解 | 登录/登出/Token刷新测试通过 |
| W4 | 工作流数据模型 | 8张表建表SQL、Entity/Mapper生成、基础CRUD | 集成测试通过 |
| W5 | 状态机引擎 | InstanceStateMachine、状态转换逻辑、事件发布 | 状态转换测试覆盖率>=90% |
| W6 | 审批人解析器 | AssigneeResolverChain、6种策略实现、空策略处理 | 单元测试覆盖率>=85% |
| W7 | 会签处理器 | SignHandler、4种审批模式汇总算法 | 边界场景测试通过 |
| W8 | 任务操作API | approve/reject/transfer/add-sign/urge API | 集成测试通过 |
| W9 | 流程定义API | CRUD、发布、版本管理、设计器数据格式定义 | E2E测试通过 |
| W10 | 回调机制 | WorkflowCallbackDispatcher、7种业务回调实现 | 与oa-hr/oa-finance联调通过 |

### Phase 2: 核心业务模块（第二优先级）

| 周次 | 模块 | 任务 | 验收标准 |
|------|------|------|----------|
| W11-W12 | oa-document | 公文表结构、文号生成器、正文留痕对比 | 公文全流程E2E测试 |
| W13-W14 | oa-knowledge | ES索引设计、同步策略、推荐算法stub | 搜索功能测试 |
| W15-W16 | oa-meeting | 会议室冲突检测、签到、决议转任务 | 会议流程E2E测试 |
| W17-W19 | oa-task | 项目/任务/依赖/工时完整实现 | 甘特图渲染测试 |
| W20-W21 | oa-hr | 员工扩展、考勤联动、假期规则脚本化 | 请假流程联调 |
| W22-W23 | oa-admin | 印章、用品库存并发、资产变动日志 | 库存并发测试通过 |
| W24-W25 | oa-finance | 预算占用/释放原子操作、报销核销 | 预算超支校验测试 |

### Phase 3: 跨模块与前端（第三优先级）

| 周次 | 任务 | 详细内容 | 验收标准 |
|------|------|---------|----------|
| W26 | 消息中台 | MessageNotificationService、用户偏好存储 | 多渠道发送测试 |
| W27 | 事件驱动 | 领域事件定义、Spring Event配置、跨模块订阅 | 事件流转日志 |
| W28-W30 | 共享组件库 | WorkflowDesigner、DynamicForm、FileUpload、Watermark、GanttChart | Storybook文档完整 |
| W31-W34 | 管理端应用 | 全部页面实现、路由配置、权限控制 | E2E关键路径测试通过 |
| W35-W37 | 移动端应用 | uni-app页面、API对接、微信小程序适配 | 真机测试通过 |

### Phase 4: 测试与上线（第四优先级）

| 周次 | 任务 | 详细内容 | 验收标准 |
|------|------|---------|----------|
| W38-W39 | 测试补充 | 补充单元测试至80%覆盖率、集成测试 | CI门禁通过 |
| W40 | 性能测试 | JMeter压测、慢SQL优化 | QPS>=100 |
| W41 | 安全测试 | SQL注入、XSS、权限绕过测试 | 无高危漏洞 |
| W42 | 部署上线 | Docker镜像构建、生产环境部署、监控配置 | 系统稳定运行 |

---

## 十、风险与决策记录

### 10.1 关键设计决策

| 决策 | 选择 | 原因 | 备选方案 |
|------|------|------|----------|
| 流程定义存储 | 数据库表（非JSON大字段） | 支持查询、索引、关联查询 | JSON大字段存储 |
| 实例快照 | JSON存储在wf_instance | 保证运行实例不受定义更新影响 | 每次查询最新定义 |
| 条件表达式 | MVEL | 轻量级，无需引入规则引擎 | SpEL / Drools |
| 消息队列 | Spring Event + Redis Pub/Sub | 简单场景无需RabbitMQ | RabbitMQ |
| 搜索引擎 | Elasticsearch 8.x | 全文检索最佳选择 | MySQL FULLTEXT |
| 前端架构 | pnpm monorepo | 共享组件库，统一依赖 | 单仓库 |

### 10.2 风险清单

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 工作流引擎复杂度超预期 | 中 | 高 | W1-W10预留10周，优先MVP功能 |
| ES运维经验不足 | 中 | 中 | 先用MySQL FULLTEXT，预留ES接口 |
| 甘特图性能瓶颈 | 低 | 中 | 大数据量分页加载，虚拟滚动 |
| 多租户需求变更 | 低 | 高 | 设计预留tenant_id字段 |

---

## 十一、重构落地补强

### 11.1 文档完整度评估

本方案的业务蓝图、核心表结构、工作流算法、前端组件、测试策略和部署拓扑已经足够支撑总体设计评审；但若直接交给 Claude Code 或多人并行实现，还需要补齐以下执行细节，避免“设计很完整，落地各写各的”。

| 维度 | 当前充分度 | 需要补强的内容 |
|------|------------|----------------|
| 架构蓝图 | 高 | 已明确务实DDD、微内核、模块前缀和前端 monorepo |
| 当前仓库对齐 | 中 | 需要写清旧模块与新模块并存期的依赖、删除、迁移顺序 |
| 数据库方案 | 中 | 需要定义唯一基线DDL、种子数据、脚本淘汰规则、索引验收方式 |
| API契约 | 中 | 需要统一路径、权限码、响应码、分页、错误码和前后端类型同步方式 |
| 并行开发 | 中 | 需要明确任务切分粒度、模块所有权、跨模块接口先行原则 |
| 测试门禁 | 中 | 已有测试目标，但需要落到每个阶段必须执行的命令和通过条件 |
| 上线回滚 | 中 | 不保留旧数据降低迁移复杂度，但仍需要环境、配置、镜像和基线回滚策略 |

### 11.2 当前仓库状态对齐

截至 2026-06-02，仓库不是纯旧架构，也不是纯新架构，而是处于重构过渡态：

| 区域 | 当前观察 | 重构处理策略 |
|------|----------|--------------|
| 后端根POM | 已声明 `oa-platform`、`oa-workflow`、`oa-document`、`oa-knowledge`、`oa-meeting`、`oa-task`、`oa-hr`、`oa-admin`、`oa-finance`、`oa-message`、`oa-integration`、`oa-web` 等新模块 | 以根POM为目标模块清单，后续新增代码优先进入新模块 |
| 旧聚合模块 | `oa-model`、`oa-mapper`、`oa-service` 仍存在大量旧实体、Mapper、Service 和测试 | 允许短期保留作为迁移来源，但不得继续新增业务能力；每迁移一个领域就从旧模块移除对应入口 |
| 新模型包 | `oa-model/src/main/java/cn/oa/hr`、`meeting` 等已出现新前缀领域模型 | 迁移期避免同名类分散在 `cn.oa.hr` 与 `cn.oa.hr.entity` 两套包中，先统一包规范再批量迁移 |
| 前端Web | `code/frontend` 是独立 Vite 应用，已有大量 `src/api` 和 `src/views/oa` 页面 | monorepo 改造前先冻结 API 命名，迁移后保持路由和接口兼容 |
| 移动端 | `code/mobile` 是 uni-app 项目，API 是 Web 子集 | 移动端接口以员工自助场景为准，不直接复用管理端页面逻辑 |
| 数据脚本 | 存在 `oa_system_full.sql`、`phase_all_ddl.sql`、`oa_system_extensions.sql`、多份 seed/init 脚本 | 重构后只保留一份基线DDL、一组按环境区分的 seed 脚本和必要的补丁脚本 |
| Claude配置 | 已有根目录 `CLAUDE.md` 与 `.claude/workflows/*.js` | 需要补充本项目专用 workflow 文档，并校正 workflow 中非本项目路径/技术栈描述 |

### 11.3 迁移总原则

1. **新功能只进新模块**：重构开始后，除安全修复和阻塞Bug外，不再向 `oa-service`、旧 `cn.oa.controller` 聚合结构新增业务功能。
2. **领域先建合同，再迁实现**：每个模块先完成 API 契约、表结构、权限码、事件定义，再迁移 Service/Controller/UI。
3. **一次只迁一个业务闭环**：例如请假流程必须同时完成 `hr_*` 表、工作流回调、Web页面、移动端申请、审批流E2E，才算迁移完成。
4. **旧入口显式下线**：每个旧 Controller/API 下线前必须有替代接口、前端已切换、测试已覆盖、菜单权限已更新。
5. **数据库从空库开始，但脚本必须可重复执行**：不迁旧数据不代表可以手工建表，所有表、索引、初始角色、菜单、流程定义必须来自版本化脚本。

### 11.4 模块边界与依赖规则

| 模块类型 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `oa-platform-core` | 第三方通用库 | 任一业务模块、Web层、Mapper层 |
| `oa-platform-security` | `oa-platform-core`、Redis/JWT相关依赖 | 具体业务 Service |
| `oa-workflow-model` | `oa-platform-core` | 业务模块、Web层 |
| `oa-workflow-mapper` | `oa-workflow-model`、MyBatis-Plus | 业务 Service、Web层 |
| `oa-workflow-core` | workflow model/mapper、platform、message事件接口 | 具体业务模块实现 |
| 业务模块 | platform、workflow API/事件接口、message接口 | 其他业务模块的实现类 |
| `oa-web` | 所有对外 API 模块、配置类 | 领域计算逻辑、复杂 SQL、业务规则 |

跨模块协作优先使用以下方式：

| 场景 | 推荐方式 | 示例 |
|------|----------|------|
| 流程完成后更新业务单据 | 回调接口 + 领域事件 | `WorkflowApprovedEvent` -> `LeaveApprovalHandler` |
| 消息通知 | `oa-message` 统一发送 | 任务到达、审批完成、会议提醒 |
| 查询其他模块少量展示数据 | 只读查询接口或应用服务 | 任务展示项目负责人姓名 |
| 强一致写操作 | 当前模块内事务完成后发布事件 | 预算占用成功后再启动报销流程 |

### 11.5 数据库与脚本基线

重构后数据库脚本建议收敛为：

```
code/backend/sql/
├── baseline/
│   ├── 001_schema.sql              # 全量建表、索引、约束
│   ├── 002_seed_system.sql         # 角色、权限、菜单、字典、系统配置
│   ├── 003_seed_workflow.sql       # 流程分类、流程定义、节点、审批规则
│   └── 004_seed_demo.sql           # 演示数据，仅 dev/test 使用
├── patches/
│   └── YYYYMMDD_xxx.sql            # 基线冻结后的增量变更
└── README.md                       # 执行顺序、环境说明、回滚说明
```

数据库验收标准：

| 项目 | 标准 |
|------|------|
| 字符集 | 所有表使用 `utf8mb4`，排序规则统一 `utf8mb4_unicode_ci`（区别于 `_general_ci` 的不区分重音比较） |
| 主键 | `BIGINT` 自增或统一雪花ID策略，不能混用 |
| 软删除 | 统一 `del_flag`，MyBatis-Plus 配置与字段名一致 |
| 时间字段 | 统一 `create_time`、`update_time`，使用 `DATETIME` |
| 金额字段 | `DECIMAL(18,2)` 或按业务定义，不使用 `DOUBLE` |
| JSON字段 | 必须注明结构示例和兼容策略 |
| 索引 | 每个列表页查询、审批待办、业务单据关联必须有对应复合索引 |
| 验证 | 关键查询使用 `EXPLAIN`，避免 `type=ALL`、大范围 `filesort` |

### 11.6 API与权限契约

统一 API 规范：

| 规则 | 约定 |
|------|------|
| 路径前缀 | `/api/{module}/{resource}`，其中 `{resource}` 推荐用**复数**（`leaves` 而非 `leave`、`tasks` 而非 `task`），例如 `/api/hr/leaves`、`/api/wf/tasks` |
| HTTP方法 | 查询 `GET`，创建 `POST`，更新 `PUT`，删除 `DELETE`，业务动作 `POST /{id}/actions/{action}` |
| 分页参数 | `pageNum`、`pageSize`、`sortField`、`sortOrder` |
| 响应格式 | 沿用 `{"code":0,"message":"操作成功","data":...}` |
| 错误码 | 业务错误 `-1`，未认证 `401`，无权限 `403`，不存在 `404`，系统错误 `500` |
| 权限码 | `{module}:{resource}:{action}` 三段式，例如 `hr:leave:approve`；`{resource}` 可带连字符（`hr:leave-balance:list`） |
| API文档 | 每个 Controller 必须出现在 Knife4j/OpenAPI 分组中 |

每个接口实现前必须先写清：

1. 请求 DTO 和校验规则。
2. 响应 VO 字段、枚举含义、时间格式。
3. 角色权限和数据权限。
4. 事务边界与幂等规则。
5. 前端调用文件路径和页面入口。

**v1 版本切换期双写策略（2026-06-04 增补）**：在 `/api/{module}/v1/*` 与旧 `/api/{module}/*` 并行期间，
- 新接口统一走 v1 路径（`/api/finance/v1/loans`），旧路径在 6 个月内保持只读
- 旧 Controller 加 `@Deprecated` 注解 + 启动 WARN 日志
- 数据写入只走 v1，读路径兼容两套（前端优先 v1，老前端 fallback 旧）
- 切换期结束后（旧 Controller 全部删完）删除 v1 前缀

### 11.7 前端与移动端迁移规则

Web 管理端重构顺序：

1. 先整理 `src/api`，按模块拆分并补齐 TypeScript 接口。
2. 再迁移路由和菜单权限，确保后端 `meta.roles` 与权限码一致。
3. 最后迁移页面和共享组件，避免页面先行导致 API 契约反复变化。

移动端重构顺序：

1. 保留登录、首页、待办、审批、我的四个主入口。
2. 优先迁移员工自助闭环：考勤、请假、出差、外出、报销、消息。
3. 管理类能力只做审批和查询，不复制 Web 管理端复杂配置页面。

共享组件建议：

| 组件 | Web | Mobile |
|------|-----|--------|
| 审批时间线 | `packages/components/ApprovalTimeline` | 轻量版 timeline |
| 动态表单 | `DynamicForm` | 按字段类型降级渲染 |
| 文件上传 | `FileUpload` | uni 上传适配 |
| 流程设计器 | Web 专用 | 移动端不实现 |
| 甘特图 | Web 专用 | 移动端只展示任务列表 |

### 11.8 阶段门禁

每个 Phase 结束必须通过以下门禁，未通过不得进入下一阶段：

| Phase | 必过命令/检查 | 通过条件 |
|-------|---------------|----------|
| Phase 1 | `cd code/backend && mvn clean test` | 平台、认证、工作流核心单元测试通过 |
| Phase 1 | 工作流状态机与会签算法测试 | 核心分支覆盖率 >= 90% |
| Phase 2 | 数据库 baseline 从空库执行 | 表、索引、seed 数据一次执行成功 |
| Phase 2 | 业务模块集成测试 | 每个模块至少 1 条完整业务闭环 |
| Phase 3 | `cd code/frontend && pnpm typecheck && pnpm build` | Web 类型检查和构建通过 |
| Phase 3 | `cd code/mobile && pnpm build:h5` | H5 构建通过，微信小程序关键页面可运行 |
| Phase 4 | E2E + API 测试 | 登录、待办、审批、消息、报表主链路通过 |
| Phase 4 | 安全检查 | 无硬编码密钥、越权接口、生产 Swagger 暴露 |

### 11.9 Claude Code 并行执行约束

使用 Claude Code 多代理/多 workflow 执行时，每个子任务必须包含：

| 字段 | 要求 |
|------|------|
| 目标模块 | 明确到 `oa-hr`、`oa-finance`、`frontend/src/api/hr` 等路径 |
| 输入文档 | 指向本重构文档的章节、相关旧代码路径、数据库脚本 |
| 输出物 | 列出必须生成/修改的 Entity、Mapper、Service、Controller、API、页面、测试 |
| 不允许修改 | 明确禁止触碰的模块，避免并行任务互相覆盖 |
| 验收命令 | 给出本任务完成后必须运行的最小命令 |
| 回滚点 | 说明本任务失败时需要删除或恢复的文件范围 |

Claude 生成代码前必须先做一次差异检查：

1. 查找是否已有同名实体、Mapper、Service、API 文件。
2. 确认目标模块 POM 是否已经包含所需依赖。
3. 确认数据库脚本中是否已有目标表。
4. 确认前端是否已有同路径 API 或页面。
5. 若发现重复实现，先合并方案，不直接新增平行版本。

---

## 附录A：API响应码定义

| 码值 | 含义 | 场景 |
|------|------|------|
| 0 | 成功 | 所有正常响应 |
| -1 | 业务错误 | 校验失败、状态不允许等 |
| 401 | 未认证 | Token无效/过期 |
| 403 | 无权限 | 角色/权限不足 |
| 404 | 资源不存在 | ID不存在 |
| 500 | 系统错误 | 未捕获异常 |

## 附录B：数据库表前缀规范

| 前缀 | 模块 | 示例 |
|------|------|------|
| `sys_` | 系统管理 | sys_employee, sys_dept |
| `wf_` | 工作流 | wf_definition, wf_instance |
| `doc_` | 公文管理 | doc_dispatch, doc_receive |
| `km_` | 知识库 | km_entry, km_version |
| `mt_` | 会议管理 | mt_room, mt_booking |
| `task_` | 任务项目 | task_project, task_item |
| `hr_` | 人事管理 | hr_employee_ext, hr_transfer |
| `adm_` | 综合行政 | adm_seal, adm_supply |
| `fin_` | 费控报销 | fin_budget, fin_expense |
| `msg_` | 消息通知 | msg_user_preference |
