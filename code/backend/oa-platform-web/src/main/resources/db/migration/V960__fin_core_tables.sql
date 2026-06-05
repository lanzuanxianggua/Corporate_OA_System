-- ============================================
-- V960__fin_core_tables.sql
-- 增量: 财务核心业务表 (预算/报销/借款)
-- 对应 oa-finance 模块
-- ============================================

-- 1) fin_budgets 预算表
CREATE TABLE `fin_budgets` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emp_id`          BIGINT        NOT NULL                COMMENT '预算责任人 emp_id',
  `dept_id`         BIGINT        NOT NULL                COMMENT '所属部门',
  `budget_year`     INT           NOT NULL                COMMENT '预算年度',
  `budget_name`     VARCHAR(100)  NOT NULL                COMMENT '预算名称',
  `total_amount`    DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '总预算金额',
  `used_amount`     DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '已使用金额',
  `frozen_amount`   DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '审批中冻结金额',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/FROZEN/CLOSED',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_dept_year` (`dept_id`, `budget_year`),
  KEY `idx_emp` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';

-- 2) fin_expenses 报销表
CREATE TABLE `fin_expenses` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`          VARCHAR(32)   NOT NULL                COMMENT '报销单号',
  `emp_id`            BIGINT        NOT NULL                COMMENT '报销人 emp_id',
  `dept_id`           BIGINT        NOT NULL                COMMENT '所属部门',
  `expense_type`      VARCHAR(20)   NOT NULL                COMMENT '报销类型: TRAVEL/MEAL/OFFICE/OTHER',
  `total_amount`      DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '报销总金额',
  `reason`            VARCHAR(500)           DEFAULT NULL   COMMENT '报销事由',
  `status`            VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED/PAID',
  `wf_instance_id`    BIGINT                 DEFAULT NULL   COMMENT '流程实例 ID',
  `loan_offset_amount` DECIMAL(18,2)         DEFAULT 0.00   COMMENT '冲抵借款金额',
  `paid_time`         DATETIME               DEFAULT NULL   COMMENT '支付时间',
  `del_flag`          CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`         VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_emp_status` (`emp_id`, `status`),
  KEY `idx_wf_instance` (`wf_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销表';

-- 3) fin_expense_details 报销明细表
CREATE TABLE `fin_expense_details` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `expense_id`      BIGINT        NOT NULL                COMMENT '关联报销单 ID',
  `fee_date`        DATE          NOT NULL                COMMENT '费用日期',
  `fee_type`        VARCHAR(20)   NOT NULL                COMMENT '费用类型: TRANSPORT/ACCOMMODATION/MEAL/OTHER',
  `amount`          DECIMAL(18,2) NOT NULL                COMMENT '金额',
  `invoice_no`      VARCHAR(50)            DEFAULT NULL   COMMENT '发票号',
  `invoice_amount`  DECIMAL(18,2)          DEFAULT NULL   COMMENT '发票金额',
  `remark`          VARCHAR(200)           DEFAULT NULL   COMMENT '备注',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_expense` (`expense_id`),
  KEY `idx_invoice` (`invoice_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销明细表';

-- 4) fin_loans 借款表
CREATE TABLE `fin_loans` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no`        VARCHAR(32)   NOT NULL                COMMENT '借款单号',
  `emp_id`          BIGINT        NOT NULL                COMMENT '借款人 emp_id',
  `dept_id`         BIGINT        NOT NULL                COMMENT '所属部门',
  `loan_type`       VARCHAR(20)   NOT NULL                COMMENT '借款类型: TRAVEL/BUSINESS/OTHER',
  `amount`          DECIMAL(18,2) NOT NULL                COMMENT '借款金额',
  `purpose`         VARCHAR(500)           DEFAULT NULL   COMMENT '借款用途',
  `status`          VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED/SETTLED',
  `wf_instance_id`  BIGINT                 DEFAULT NULL   COMMENT '流程实例 ID',
  `repaid_amount`   DECIMAL(18,2)          DEFAULT 0.00   COMMENT '已还款金额',
  `deadline_date`   DATE                   DEFAULT NULL   COMMENT '还款期限',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_emp_status` (`emp_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借款表';

-- 5) fin_loan_repayments 还款记录表
CREATE TABLE `fin_loan_repayments` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `loan_id`         BIGINT        NOT NULL                COMMENT '关联借款 ID',
  `repay_amount`    DECIMAL(18,2) NOT NULL                COMMENT '还款金额',
  `repay_type`      VARCHAR(16)   NOT NULL                COMMENT '还款类型: CASH/DEDUCT/EXPENSE_OFFSET',
  `expense_id`      BIGINT                 DEFAULT NULL   COMMENT '关联报销单 ID（冲抵时）',
  `repay_date`      DATE          NOT NULL                COMMENT '还款日期',
  `remark`          VARCHAR(200)           DEFAULT NULL   COMMENT '备注',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_loan` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='还款记录表';
