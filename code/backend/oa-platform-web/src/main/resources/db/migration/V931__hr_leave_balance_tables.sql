-- ============================================
-- V931__hr_leave_balance_tables.sql
-- 增量: HR 假期余额表 (hr_leave_balance) + 业务权限
-- 对应 oa-hr-leave 模块余额管理
-- ============================================

-- 1) hr_leave_balance 表 (假期余额)
CREATE TABLE `hr_leave_balance` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emp_id`          BIGINT        NOT NULL                COMMENT '员工 emp_id',
  `leave_type`      VARCHAR(20)   NOT NULL                COMMENT '请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY',
  `year`            INT           NOT NULL                COMMENT '年度',
  `total_days`      DECIMAL(6,1)  NOT NULL DEFAULT 0      COMMENT '总额度(天)',
  `used_days`       DECIMAL(6,1)  NOT NULL DEFAULT 0      COMMENT '已用天数',
  `frozen_days`     DECIMAL(6,1)  NOT NULL DEFAULT 0      COMMENT '冻结天数(审批中)',
  `remaining_days`  DECIMAL(6,1)  NOT NULL DEFAULT 0      COMMENT '剩余天数',
  `status`          CHAR(10)      NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)            DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_year` (`emp_id`, `leave_type`, `year`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='假期余额';

-- 2) 请假余额相关权限 (挂载在已有 hr-leave 菜单下)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'hr-leave:balance', '假期余额', 'BUTTON', NULL, NULL, 200, 'ACTIVE', 'system'),
  (0, 'hr-leave:leave:revoke', '撤回请假', 'BUTTON', NULL, NULL, 201, 'ACTIVE', 'system');

-- 3) 给 SUPER_ADMIN (id=1) 分配新权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN ('hr-leave:balance', 'hr-leave:leave:revoke') AND `del_flag` = '0';
