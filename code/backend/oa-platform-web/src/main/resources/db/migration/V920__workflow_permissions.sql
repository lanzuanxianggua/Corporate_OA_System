-- ============================================
-- V920__workflow_permissions.sql
-- 增量: 给工作流引擎加权限定义 + 给 SUPER_ADMIN 角色分配 workflow 权限
-- 对应 WfInstanceController.start / getById / getTasks 端点的 @RequirePermission
-- ============================================

-- 1) 加 workflow 权限 (id 自动从 30 起)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'workflow', '工作流引擎', 'MENU', '/workflow', NULL, 900, 'ACTIVE', 'system'),
  (30, 'workflow:instance', '流程实例', 'MENU', '/workflow/instances', NULL, 901, 'ACTIVE', 'system'),
  (31, 'workflow:instance:start', '启动流程', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (31, 'workflow:instance:read', '查看流程', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (31, 'workflow:task', '流程任务', 'MENU', '/workflow/tasks', NULL, 902, 'ACTIVE', 'system'),
  (32, 'workflow:task:approve', '审批任务', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (32, 'workflow:task:read', '查看任务', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (id=1) 分配 workflow 权限
-- 拿到刚插入的 id (parent_id=30 时新 id 从 30 起, 但 AUTO_INCREMENT 可能不同)
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` LIKE 'workflow%' AND `del_flag` = '0';
