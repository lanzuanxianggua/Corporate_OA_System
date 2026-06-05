-- ============================================
-- V933__hr_leave_balance_permissions.sql
-- 增量: HR 假期余额业务权限
-- ============================================

INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'hr-leave:leave-balance:view', '查看余额', 'BUTTON', NULL, NULL, 210, 'ACTIVE', 'system'),
  (0, 'hr-leave:leave-balance:list', '余额列表', 'BUTTON', NULL, NULL, 211, 'ACTIVE', 'system'),
  (0, 'hr-leave:leave-balance:init', '初始化余额', 'BUTTON', NULL, NULL, 212, 'ACTIVE', 'system'),
  (0, 'hr-leave:leave-balance:adjust', '调整余额', 'BUTTON', NULL, NULL, 213, 'ACTIVE', 'system');

INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN ('hr-leave:leave-balance:view', 'hr-leave:leave-balance:list',
    'hr-leave:leave-balance:init', 'hr-leave:leave-balance:adjust') AND `del_flag` = '0';
