# 02 - Corporate OA System v2 数据库设计

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`

---

## 1. 设计原则

### 1.1 命名
- **数据库**: `oa_system_v2`（v1 改 `oa_system`）
- **表名**: `{module}_{entity_plural}` 蛇形（`hr_leaves`, `wf_tasks`）
- **主键**: `id BIGINT NOT NULL AUTO_INCREMENT`
- **外键**: 不物理外键（仅逻辑外键，命名 `{referenced_entity}_id`）
- **审计字段**: `create_by` `create_time` `update_by` `update_time` `del_flag`（5 个固定字段）
- **状态**: `status VARCHAR(16)`（字符串枚举）
- **软删除**: `del_flag CHAR(1) DEFAULT '0'`

### 1.2 字符集
- 数据库: `utf8mb4` / `utf8mb4_unicode_ci`
- 表: `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`

### 1.3 数值
- **金额**: `DECIMAL(18,4)`（4 位小数，避免金融舍入误差）
- **比率/系数**: `DECIMAL(6,2)`
- **天数**: `DECIMAL(6,1)`
- **计数**: `INT UNSIGNED` 或 `BIGINT UNSIGNED`
- **主键**: `BIGINT NOT NULL AUTO_INCREMENT`

### 1.4 时间
- `DATETIME`（不用 TIMESTAMP，TIMESTAMP 2038 限制）
- 统一 `DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE CURRENT_TIMESTAMP`
- 时区：MySQL 服务器 UTC，Java 应用层按用户时区展示

### 1.5 JSON
- MySQL 8 `JSON` 类型
- Java 层用 Jackson 序列化为 String
- 注释必标结构示例和兼容策略

### 1.6 索引
- **每表必建**：`PRIMARY KEY (id)` + `KEY idx_create_time (create_time)` + `KEY idx_update_time (update_time)`
- **业务索引**：单列不超过 5 个，复合索引前缀必须能用
- **唯一索引**：业务唯一约束（如 `uk_emp_type_year(emp_id, leave_type, year)`）
- **外键索引**：高频关联字段建复合索引
- **禁用**：`SELECT *`、全表扫描

### 1.7 软删除
- 所有业务表 `del_flag CHAR(1) DEFAULT '0'`
- MyBatis-Plus `@TableLogic` 注解，**自动过滤**已删除数据
- 真删除通过管理端审核流程

---

## 2. 通用字段（所有业务表必含）

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `id` | BIGINT | AUTO_INCREMENT | 主键 |
| `create_by` | VARCHAR(64) | NULL | 创建人 empId |
| `create_time` | DATETIME | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) | NULL | 更新人 empId |
| `update_time` | DATETIME | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| `del_flag` | CHAR(1) | '0' | 0=正常, 1=删除 |
| `version` | INT | 0 | 乐观锁版本号（@Version） |
| `tenant_id` | BIGINT | NULL | 租户 ID（v2 留位，多租户用） |

**MyBatis-Plus 实体**：
```java
@Data
public abstract class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    @TableField(select = false)
    private String delFlag;
    @Version
    private Integer version;
}
```

---

## 3. 数据库清单（v2 共 80+ 张表）

> 完整 DDL 见 `code/backend/sql/v2/` 目录（Phase 2 创建）
> 本节列关键表，详细字段见各模块详细设计 `05-modules/*`

### 3.1 平台表（5 张）

| 表 | 用途 |
|----|------|
| `sys_employees` | 员工主数据 |
| `sys_departments` | 部门（含层级） |
| `sys_roles` | 角色 |
| `sys_employee_roles` | 员工-角色 |
| `sys_permissions` | 权限码（菜单/按钮） |

### 3.2 工作流表（8 张）

| 表 | 用途 |
|----|------|
| `wf_definitions` | 流程定义 |
| `wf_nodes` | 流程节点 |
| `wf_transitions` | 流转条件 |
| `wf_assignee_rules` | 审批人规则 |
| `wf_instances` | 流程实例 |
| `wf_tasks` | 审批任务 |
| `wf_records` | 审批历史 |
| `wf_delegations` | 审批委托 |

### 3.3 HR 请假（5 张）

| 表 | 用途 |
|----|------|
| `hr_leaves` | 请假申请 |
| `hr_leave_balances` | 假期余额 |
| `hr_leave_rules` | 假期规则 |
| `hr_leave_adjustments` | 余额调整记录 |
| `hr_leave_holidays` | 节假日/工作日配置 |

### 3.4 财务（8 张）

| 表 | 用途 |
|----|------|
| `fin_budgets` | 预算 |
| `fin_budget_items` | 预算明细 |
| `fin_expenses` | 报销 |
| `fin_expense_items` | 报销明细 |
| `fin_loans` | 借款 |
| `fin_loan_repayments` | 还款 |
| `fin_payments` | 付款 |
| `fin_receipts` | 收款 |

### 3.5 文档（6 张）

| 表 | 用途 |
|----|------|
| `doc_dispatches` | 发文 |
| `doc_receives` | 收文 |
| `doc_sign_reports` | 签报 |
| `doc_sign_report_items` | 签报明细 |
| `doc_archives` | 档案 |
| `doc_archive_files` | 档案附件 |

### 3.6 行政（5 张）

| 表 | 用途 |
|----|------|
| `adm_seals` | 印章 |
| `adm_seal_usages` | 用印记录 |
| `adm_assets` | 资产 |
| `adm_asset_borrows` | 资产借用 |
| `adm_supplies` | 办公用品 |

### 3.7 知识库（3 张）

| 表 | 用途 |
|----|------|
| `km_entries` | 知识条目 |
| `km_versions` | 版本历史 |
| `km_categories` | 分类 |

### 3.8 消息（4 张）

| 表 | 用途 |
|----|------|
| `msg_notifications` | 站内消息 |
| `msg_notification_recipients` | 接收人 |
| `msg_email_logs` | 邮件日志 |
| `msg_sms_logs` | 短信日志 |

### 3.9 会议（4 张）

| 表 | 用途 |
|----|------|
| `mt_rooms` | 会议室 |
| `mt_bookings` | 预约 |
| `mt_meetings` | 会议 |
| `mt_resolutions` | 会议决议 |

### 3.10 任务（4 张）

| 表 | 用途 |
|----|------|
| `task_projects` | 项目 |
| `task_items` | 任务 |
| `task_hours` | 工时 |
| `task_comments` | 评论 |

### 3.11 HR 其他（15+ 张）

考勤 4 张、员工档案 3 张、绩效 3 张、招聘 3 张、培训 2+ 张

### 3.12 系统（10+ 张）

字典 2 张、配置 2 张、审计 2 张、附件 1 张、导入导出 1 张、通知 1 张

**总计**：约 80+ 张表（v1 是 30+ 张）

---

## 4. 关键表设计（详细字段）

### 4.1 `hr_leaves` 请假申请表

```sql
CREATE TABLE `hr_leaves` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '请假ID',
  `apply_no`          VARCHAR(32)  DEFAULT NULL                 COMMENT '申请单号(LVyyyyMMddHHmmssXXXX)',
  `emp_id`            BIGINT       NOT NULL                     COMMENT '请假人ID',
  `dept_id`           BIGINT       NOT NULL                     COMMENT '所属部门ID(冗余)',
  `leave_type`        VARCHAR(16)  NOT NULL                     COMMENT '假期类型(ENUM)',
  `start_time`        DATETIME     NOT NULL                     COMMENT '开始时间',
  `end_time`          DATETIME     NOT NULL                     COMMENT '结束时间',
  `leave_period`      VARCHAR(16)  NOT NULL DEFAULT 'FULL'      COMMENT '时段(FULL/AM/PM)',
  `days`              DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '请假天数(自动计算)',
  `reason`            VARCHAR(500) DEFAULT NULL                 COMMENT '请假原因',
  `attachments`       JSON         DEFAULT NULL                 COMMENT '附件',
  `status`            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT'     COMMENT '业务单据状态',
  `wf_instance_id`    BIGINT       DEFAULT NULL                 COMMENT '工作流实例ID',
  `wf_current_task_id` BIGINT      DEFAULT NULL                 COMMENT '当前任务ID',
  `approved_time`     DATETIME     DEFAULT NULL                 COMMENT '审批通过时间',
  `rejected_time`     DATETIME     DEFAULT NULL                 COMMENT '驳回时间',
  `reject_reason`     VARCHAR(500) DEFAULT NULL                 COMMENT '驳回原因',
  `revoked_time`      DATETIME     DEFAULT NULL                 COMMENT '撤回时间',
  `tenant_id`         BIGINT       DEFAULT NULL                 COMMENT '租户ID(留位)',
  `create_by`         VARCHAR(64)  DEFAULT NULL                 COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)  DEFAULT NULL                 COMMENT '更新人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`          CHAR(1)      NOT NULL DEFAULT '0'         COMMENT '软删除标记',
  `version`           INT          NOT NULL DEFAULT 0           COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_emp_status_time` (`emp_id`, `status`, `create_time`),
  KEY `idx_dept_status_time` (`dept_id`, `status`, `create_time`),
  KEY `idx_wf_instance` (`wf_instance_id`),
  KEY `idx_time_range` (`start_time`, `end_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';
```

**业务规则**：
- `apply_no` 由 `IdGen` 雪花算法生成（不是 `LVyyyyMMddHHmmssXXXX`）
- `days` 由 Service 计算（工作日 × 时段系数）
- `status` 5 个：`DRAFT/RUNNING/PASSED/REJECTED/REVOKED`
- `wf_instance_id`/`wf_current_task_id` 由工作流回调维护

### 4.2 `hr_leave_balances` 假期余额表

```sql
CREATE TABLE `hr_leave_balances` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT      COMMENT 'ID',
  `emp_id`          BIGINT       NOT NULL                     COMMENT '员工ID',
  `leave_type`      VARCHAR(16)  NOT NULL                     COMMENT '假期类型',
  `year`            INT          NOT NULL                     COMMENT '年度',
  `total_days`      DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '总额度(天)',
  `used_days`       DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '已用天数',
  `frozen_days`     DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '审批中冻结',
  `remaining_days`  DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '账面剩余(=total-used-frozen, 业务层维护)',
  `available_days`  DECIMAL(6,1) NOT NULL DEFAULT 0           COMMENT '可申请(=remaining, 同上)',
  `expire_date`     DATE         DEFAULT NULL                 COMMENT '过期日期',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'    COMMENT 'ACTIVE/INACTIVE',
  -- 标准字段省略
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_year` (`emp_id`, `leave_type`, `year`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='假期余额表';
```

**业务规则**：
- `total_days`/`used_days`/`frozen_days` 由 `adjustBalance` Service 维护
- `remaining_days` 业务层 SET（**不用 MySQL 计算列**）
- 并发控制：所有更新走 `WHERE used_days + frozen_days + adjust <= total_days` 条件

### 4.3 `wf_tasks` 审批任务表

```sql
CREATE TABLE `wf_tasks` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '任务ID',
  `instance_id`      BIGINT       NOT NULL                     COMMENT '流程实例ID',
  `node_id`          BIGINT       NOT NULL                     COMMENT '节点ID',
  `assignee_id`      BIGINT       NOT NULL                     COMMENT '当前审批人',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'PENDING'   COMMENT 'PENDING/APPROVED/REJECTED/TRANSFERRED/SKIPPED',
  `action`           VARCHAR(16)  DEFAULT NULL                 COMMENT '动作(APPROVE/REJECT/TRANSFER)',
  `action_time`      DATETIME     DEFAULT NULL                 COMMENT '动作时间',
  `action_emp_id`    BIGINT       DEFAULT NULL                 COMMENT '实际操作人(可能=assignee, 也可能=delegate)',
  `comment`          VARCHAR(1000) DEFAULT NULL                COMMENT '审批意见',
  `attachments`      JSON         DEFAULT NULL                 COMMENT '附件',
  `delegated_from`   BIGINT       DEFAULT NULL                 COMMENT '委托来源',
  `due_time`         DATETIME     DEFAULT NULL                 COMMENT '超时时间',
  -- 标准字段省略
  PRIMARY KEY (`id`),
  KEY `idx_instance` (`instance_id`),
  KEY `idx_assignee_status` (`assignee_id`, `status`),
  KEY `idx_due_time` (`due_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批任务表';
```

**索引**：
- `idx_assignee_status` 是核心：我的待办列表 `WHERE assignee_id=? AND status='PENDING' ORDER BY create_time`
- `idx_due_time` 用于超时扫描

---

## 5. 数据库迁移

### 5.1 迁移工具
- **Flyway**（`flyway-mysql` 10.x）
- 迁移文件：`code/backend/sql/v2/migration/`
- 命名：`V{version}__{description}.sql`（如 `V1__init_schema.sql`）

### 5.2 迁移顺序
```
V1__init_platform.sql          # 平台表
V2__init_workflow.sql          # 工作流表
V3__init_hr_leave.sql          # HR 请假
V4__init_finance.sql           # 财务
V5__init_admin.sql             # 行政
V6__init_document.sql          # 文档
V7__init_knowledge.sql         # 知识库
V8__init_message.sql           # 消息
V9__init_meeting.sql           # 会议
V10__init_task.sql             # 任务
V11__init_other_hr.sql         # HR 其他
V12__init_system.sql           # 系统
V20__seed_platform.sql         # 平台 seed
V21__seed_workflow.sql         # 工作流 seed
V22__seed_hr_leave.sql         # HR 请假 seed
V30__indexes.sql               # 索引（性能调优阶段）
```

### 5.3 v1 → v2 数据迁移

**v2 不做 v1 数据迁移**（v2 是重写，不是升级）。如需保留数据：
- 导出 v1 数据为 JSON
- 编写 `V100__import_v1_data.sql`（手工 ETL 脚本）
- v2 启动后用新模块重做

---

## 6. 索引设计

### 6.1 索引原则
- **每个查询**（Service 中所有 SQL）必须能命中索引
- **复合索引**：列顺序按 `= > IN > 范围 > 排序` 排列
- **单列索引**：仅在纯查询 / 唯一约束时使用
- **前缀索引**：仅当列很长且前 N 字符区分度高时使用

### 6.2 索引清单（v2 各表必建索引）

| 表 | 必建索引 | 业务场景 |
|----|----------|----------|
| `hr_leaves` | `idx_emp_status_time`, `idx_dept_status_time`, `idx_wf_instance`, `idx_time_range` | 我的请假、部门管理、工作流回查、日期范围 |
| `hr_leave_balances` | `uk_emp_type_year`, `idx_status` | 唯一性、管理员查询 |
| `wf_tasks` | `idx_assignee_status`, `idx_instance`, `idx_due_time` | 我的待办、流程历史、超时扫描 |
| `wf_instances` | `idx_business_key`, `idx_status`, `idx_initiator` | 业务单据关联、流程管理 |
| `sys_employees` | `uk_emp_code`, `uk_email`, `uk_mobile`, `idx_dept_id`, `idx_status` | 登录、部门员工、状态 |
| `sys_departments` | `uk_dept_code`, `idx_parent_id`, `idx_path` | 部门树 |

---

## 7. SQL 性能规范

### 7.1 查询性能
- **慢查询**: > 500ms 进 `slow_query_log`
- **EXPLAIN 验证**: 每个新查询必须 EXPLAIN，确认 `type` 不为 `ALL`（全表扫描）
- **结果集大小**: `LIMIT 100` 强制，列表接口 `LIMIT 100` + 分页
- **禁止**: `SELECT *`、`UNION`（用 `UNION ALL` + 业务去重）、子查询（用 JOIN 替代）

### 7.2 写入性能
- **批量插入**: `INSERT INTO ... VALUES (...), (...), (...)` 单次最多 1000 行
- **批量更新**: 用 `CASE WHEN` 或 `UPDATE ... JOIN`
- **禁用**: `REPLACE INTO`（先删后插，丢失自增 ID）、`INSERT ... ON DUPLICATE KEY UPDATE`（除非有明确业务需求）

### 7.3 事务
- **隔离级别**: `READ_COMMITTED`（v1 是 `REPEATABLE_READ`，v2 改 `READ_COMMITTED` 减少间隙锁）
- **事务大小**: 单事务最多 5 张表
- **事务时间**: < 5 秒
- **禁用**: 在事务中调用外部 HTTP/Redis/消息（用最终一致性）

---

## 8. 数据库连接池

- **HikariCP**（Spring Boot 默认）
- **配置**:
  - `maximum-pool-size`: 20
  - `minimum-idle`: 5
  - `connection-timeout`: 30000
  - `max-lifetime`: 1800000
  - `idle-timeout`: 600000
  - `leak-detection-threshold`: 60000

---

## 9. 备份与恢复

- **每日全量备份**（凌晨 3:00）
- **binlog 实时同步**（用于 point-in-time recovery）
- **保留期**: 30 天
- **异地备份**: 对象存储 OSS（与生产环境不同地域）
- **恢复演练**: 季度一次

---

## 10. M0 准备清单（迁移到 v2 前）

- [ ] v1 数据导出（按模块）
- [ ] Flyway 迁移脚本编写
- [ ] 测试库准备（MySQL 8.0 / Redis 7）
- [ ] CI 数据库实例
- [ ] 开发数据库实例（每开发者一份）
- [ ] 数据库账号矩阵（读写/只读/备份）
- [ ] 慢查询监控配置
- [ ] binlog 配置
- [ ] 备份策略部署
