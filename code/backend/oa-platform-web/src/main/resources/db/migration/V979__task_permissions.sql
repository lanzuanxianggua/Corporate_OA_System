-- ============================================
-- V979__task_permissions.sql
-- 增量: 任务模块权限注册
-- 对应模块: oa-task (菜单 id=10 '任务协作')
-- ============================================

-- 1) 注册任务子菜单
-- parent_id=10 对应 V900 种子数据中 '任务协作' 菜单 (id=10)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (10, 'task:project', '项目管理', 'MENU', '/task/projects', NULL, 1, 'ACTIVE', 'system'),
  (10, 'task:item',    '任务管理', 'MENU', '/task/items',    NULL, 2, 'ACTIVE', 'system');

-- 项目按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'task:project:list',   '项目列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'task:project:view',   '项目详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'task:project:create', '新建项目', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'task:project:update', '修改项目', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'task:project:delete', '删除项目', 'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 任务按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'task:item:list',   '任务列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'task:item:view',   '任务详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'task:item:create', '新建任务', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'task:item:update', '修改任务', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'task:item:delete', '删除任务', 'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system'),
  (0, 'task:item:assign', '分配任务', 'BUTTON', NULL, NULL, 6, 'ACTIVE', 'system');

-- 工时按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'task:hour:list',   '工时列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'task:hour:create', '登记工时', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有任务权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'task:project', 'task:item',
    'task:project:list', 'task:project:view', 'task:project:create', 'task:project:update', 'task:project:delete',
    'task:item:list', 'task:item:view', 'task:item:create', 'task:item:update', 'task:item:delete', 'task:item:assign',
    'task:hour:list', 'task:hour:create'
  ) AND `del_flag` = '0';
