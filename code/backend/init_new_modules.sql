-- ============================================
-- OA System - New Modules Migration Script
-- Generated for 10 new HR/business modules
-- ============================================

-- Module 1: Attendance Group (考勤组)
CREATE TABLE IF NOT EXISTS `oa_attendance_group` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `group_name` VARCHAR(50) NOT NULL COMMENT '考勤组名称',
    `work_start` TIME NOT NULL COMMENT '上班时间',
    `work_end` TIME NOT NULL COMMENT '下班时间',
    `late_threshold` INT DEFAULT 15 COMMENT '迟到阈值(分钟)',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-正常 1-停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组';

CREATE TABLE IF NOT EXISTS `oa_attendance_group_emp` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `group_id` BIGINT NOT NULL COMMENT '考勤组ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组员工关联';

-- Module 2: Leave Balance (假期余额)
CREATE TABLE IF NOT EXISTS `oa_leave_balance` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `leave_type` INT NOT NULL COMMENT '假期类型 0-年假 1-病假 2-事假 3-婚假 4-产假',
    `year` INT NOT NULL COMMENT '年份',
    `total_days` DECIMAL(6,1) NOT NULL COMMENT '总天数',
    `used_days` DECIMAL(6,1) DEFAULT 0 COMMENT '已用天数',
    `remaining_days` DECIMAL(6,1) NOT NULL COMMENT '剩余天数',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_type_year` (`emp_id`, `leave_type`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额';

-- Module 3: Overtime (加班管理)
CREATE TABLE IF NOT EXISTS `oa_overtime` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `overtime_date` DATE NOT NULL COMMENT '加班日期',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `hours` DECIMAL(5,1) NOT NULL COMMENT '加班时长(小时)',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '加班原因',
    `status` INT DEFAULT 0 COMMENT '状态 0-待审批 1-已通过 2-已拒绝',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班申请';

-- Module 4: Salary (薪资管理)
CREATE TABLE IF NOT EXISTS `oa_salary_structure` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `base_salary` DECIMAL(12,2) NOT NULL COMMENT '基本工资',
    `post_salary` DECIMAL(12,2) DEFAULT 0 COMMENT '岗位工资',
    `merit_salary` DECIMAL(12,2) DEFAULT 0 COMMENT '绩效工资',
    `allowance` DECIMAL(12,2) DEFAULT 0 COMMENT '补贴',
    `effective_date` DATE NOT NULL COMMENT '生效日期',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-有效 1-失效',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资结构';

CREATE TABLE IF NOT EXISTS `oa_salary_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `salary_month` VARCHAR(7) NOT NULL COMMENT '薪资月份(yyyy-MM)',
    `base_salary` DECIMAL(12,2) NOT NULL COMMENT '基本工资',
    `post_salary` DECIMAL(12,2) DEFAULT 0 COMMENT '岗位工资',
    `merit_salary` DECIMAL(12,2) DEFAULT 0 COMMENT '绩效工资',
    `allowance` DECIMAL(12,2) DEFAULT 0 COMMENT '补贴',
    `deduction` DECIMAL(12,2) DEFAULT 0 COMMENT '扣款',
    `actual_amount` DECIMAL(12,2) NOT NULL COMMENT '实发金额',
    `pay_time` DATETIME DEFAULT NULL COMMENT '发放时间',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-未发放 1-已发放',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_emp_month` (`emp_id`, `salary_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资记录';

-- Module 5: Employee Archive (员工档案)
CREATE TABLE IF NOT EXISTS `oa_emp_archive` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `education` VARCHAR(20) DEFAULT NULL COMMENT '学历',
    `major` VARCHAR(50) DEFAULT NULL COMMENT '专业',
    `graduate_school` VARCHAR(100) DEFAULT NULL COMMENT '毕业院校',
    `entry_date` DATE DEFAULT NULL COMMENT '入职日期',
    `probation_end_date` DATE DEFAULT NULL COMMENT '试用期结束日期',
    `contract_start` DATE DEFAULT NULL COMMENT '合同开始日期',
    `contract_end` DATE DEFAULT NULL COMMENT '合同结束日期',
    `emergency_contact` VARCHAR(30) DEFAULT NULL COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系电话',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '住址',
    `remark` TEXT DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案';

-- Module 6: Asset Management (资产管理)
CREATE TABLE IF NOT EXISTS `oa_asset` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编号',
    `asset_name` VARCHAR(100) NOT NULL COMMENT '资产名称',
    `category` VARCHAR(30) DEFAULT NULL COMMENT '资产分类',
    `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    `purchase_date` DATE DEFAULT NULL COMMENT '采购日期',
    `purchase_price` DECIMAL(12,2) DEFAULT NULL COMMENT '采购价格',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-闲置 1-在用 2-维修 3-报废',
    `current_user_id` BIGINT DEFAULT NULL COMMENT '当前使用人',
    `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_code` (`asset_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产信息';

CREATE TABLE IF NOT EXISTS `oa_asset_borrow` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `asset_id` BIGINT NOT NULL COMMENT '资产ID',
    `borrower_id` BIGINT NOT NULL COMMENT '借用人ID',
    `borrow_time` DATETIME NOT NULL COMMENT '借用时间',
    `expected_return` DATETIME DEFAULT NULL COMMENT '预计归还时间',
    `actual_return` DATETIME DEFAULT NULL COMMENT '实际归还时间',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-借出 1-已还',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_asset_id` (`asset_id`),
    KEY `idx_borrower_id` (`borrower_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产借用记录';

-- Module 7: Contract Management (合同管理)
CREATE TABLE IF NOT EXISTS `oa_contract` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `contract_no` VARCHAR(50) NOT NULL COMMENT '合同编号',
    `contract_name` VARCHAR(100) NOT NULL COMMENT '合同名称',
    `contract_type` VARCHAR(30) DEFAULT NULL COMMENT '合同类型',
    `party_a` VARCHAR(100) DEFAULT NULL COMMENT '甲方',
    `party_b` VARCHAR(100) DEFAULT NULL COMMENT '乙方',
    `amount` DECIMAL(14,2) DEFAULT NULL COMMENT '合同金额',
    `sign_date` DATE DEFAULT NULL COMMENT '签订日期',
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-执行中 1-已完成 2-已终止',
    `manager_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '合同文件URL',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_no` (`contract_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同管理';

-- Module 8: Budget Management (预算管理)
CREATE TABLE IF NOT EXISTS `oa_budget` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `budget_year` INT NOT NULL COMMENT '预算年度',
    `budget_month` INT NOT NULL COMMENT '预算月份',
    `amount` DECIMAL(14,2) NOT NULL COMMENT '预算金额',
    `used_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '已用金额',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-正常 1-冻结',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_year_month` (`dept_id`, `budget_year`, `budget_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理';

-- Module 9: Loan Management (借支管理)
CREATE TABLE IF NOT EXISTS `oa_loan` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `loan_amount` DECIMAL(12,2) NOT NULL COMMENT '借支金额',
    `loan_reason` VARCHAR(500) DEFAULT NULL COMMENT '借支原因',
    `repayment_plan` TEXT DEFAULT NULL COMMENT '还款计划',
    `status` INT DEFAULT 0 COMMENT '状态 0-待审批 1-已通过 2-已拒绝 3-已还清',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借支申请';

CREATE TABLE IF NOT EXISTS `oa_loan_repayment` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `loan_id` BIGINT NOT NULL COMMENT '借支ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '还款金额',
    `repay_time` DATETIME NOT NULL COMMENT '还款时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_loan_id` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借支还款记录';

-- Module 10: Alert Rules (预警规则)
CREATE TABLE IF NOT EXISTS `rpt_alert_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(30) DEFAULT NULL COMMENT '规则类型',
    `metric` VARCHAR(50) DEFAULT NULL COMMENT '监控指标',
    `condition_type` VARCHAR(10) DEFAULT NULL COMMENT '条件类型 gt/lt/eq/between',
    `threshold` DECIMAL(14,2) DEFAULT NULL COMMENT '阈值',
    `threshold_max` DECIMAL(14,2) DEFAULT NULL COMMENT '阈值上限(between时使用)',
    `check_cron` VARCHAR(50) DEFAULT NULL COMMENT '检查周期(cron表达式)',
    `notify_type` VARCHAR(30) DEFAULT 'inner' COMMENT '通知方式 inner/email/sms',
    `notify_targets` VARCHAR(500) DEFAULT NULL COMMENT '通知目标(逗号分隔empId)',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态 0-启用 1-停用',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则';

CREATE TABLE IF NOT EXISTS `rpt_alert_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `alert_level` CHAR(1) DEFAULT '0' COMMENT '预警级别 0-低 1-中 2-高',
    `metric_value` DECIMAL(14,2) DEFAULT NULL COMMENT '指标值',
    `threshold` DECIMAL(14,2) DEFAULT NULL COMMENT '触发阈值',
    `alert_content` VARCHAR(500) DEFAULT NULL COMMENT '预警内容',
    `notify_status` CHAR(1) DEFAULT '0' COMMENT '通知状态 0-未通知 1-已通知',
    `handle_status` CHAR(1) DEFAULT '0' COMMENT '处理状态 0-未处理 1-已处理 2-已忽略',
    `handler` VARCHAR(50) DEFAULT NULL COMMENT '处理人',
    `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
    `alert_time` DATETIME NOT NULL COMMENT '预警时间',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_alert_time` (`alert_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警日志';
