-- ============================================
-- V940__hr_employee_profile.sql
-- 增量: HR 员工档案扩展表 (1 对 1 关联 sys_employee)
-- 对应 D-3 阶段: oa-hr-employee 模块
-- ============================================

CREATE TABLE `hr_employee_profile` (
  `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emp_id`              BIGINT      NOT NULL                COMMENT '关联 sys_employee.id',
  `work_no`             VARCHAR(50)          DEFAULT NULL   COMMENT '工号 (HR 业务编号)',
  `hire_date`           DATE                 DEFAULT NULL   COMMENT '入职日期',
  `contract_type`       VARCHAR(20)          DEFAULT NULL   COMMENT '合同类型: PROBATION/REGULAR/CONTRACT/INTERN',
  `contract_end_date`   DATE                 DEFAULT NULL   COMMENT '合同到期日',
  `emergency_contact`   VARCHAR(50)          DEFAULT NULL   COMMENT '紧急联系人',
  `emergency_phone`     VARCHAR(20)          DEFAULT NULL   COMMENT '紧急联系电话',
  `bank_name`           VARCHAR(100)         DEFAULT NULL   COMMENT '开户行',
  `bank_account`        VARCHAR(50)          DEFAULT NULL   COMMENT '银行卡号',
  `status`              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/LEAVE/RESIGN',
  `del_flag`            CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`           VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`             INT         NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_id` (`emp_id`),
  UNIQUE KEY `uk_work_no` (`work_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工档案 (HR 扩展)';

-- ============================================
-- V941__hr_employee_permissions.sql
-- 增量: 员工档案业务权限 + 给 SUPER_ADMIN 角色分配
-- 对应 HrEmployeeProfileController 5 个端点
-- ============================================

-- 1) hr-employee 业务权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'hr-employee', '员工档案', 'MENU', '/hr/employees', NULL, 800, 'ACTIVE', 'system'),
  (40, 'hr-employee:profile', '档案管理', 'MENU', '/hr/employees/profile', NULL, 801, 'ACTIVE', 'system'),
  (41, 'hr-employee:profile:create', '新建档案', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (41, 'hr-employee:profile:update', '修改档案', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (41, 'hr-employee:profile:delete', '删除档案', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (41, 'hr-employee:profile:list', '查看档案', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (id=1) 分配
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` LIKE 'hr-employee%' AND `del_flag` = '0';
