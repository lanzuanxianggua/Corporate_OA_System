-- ============================================
-- V971__doc_permissions.sql
-- 增量: 文档模块权限注册
-- 对应模块: oa-document (菜单 id=5 '文档中心')
-- ============================================

-- 1) 注册文档子菜单 + 按钮权限
-- parent_id=5 对应 V900 种子数据中 '文档中心' 菜单
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  -- 子菜单
  (5, 'document:dispatch',    '发文管理',  'MENU',   '/document/dispatches',   NULL, 1, 'ACTIVE', 'system'),
  (5, 'document:receive',     '收文管理',  'MENU',   '/document/receives',     NULL, 2, 'ACTIVE', 'system'),
  (5, 'document:sign-report', '签报管理',  'MENU',   '/document/sign-reports', NULL, 3, 'ACTIVE', 'system'),
  (5, 'document:archive',     '档案管理',  'MENU',   '/document/archives',     NULL, 4, 'ACTIVE', 'system');

-- 发文按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'document:dispatch:create', '发文新建',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'document:dispatch:list',   '发文列表',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'document:dispatch:view',   '发文详情',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'document:dispatch:update', '发文修改',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'document:dispatch:delete', '发文删除',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system'),
  (0, 'document:dispatch:submit', '发文提交审批','BUTTON', NULL, NULL, 6, 'ACTIVE', 'system'),
  (0, 'document:dispatch:archive','发文归档',   'BUTTON', NULL, NULL, 7, 'ACTIVE', 'system');

-- 收文按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'document:receive:create', '收文登记',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'document:receive:list',   '收文列表',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'document:receive:view',   '收文详情',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'document:receive:update', '收文修改',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'document:receive:archive','收文归档',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 签报按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'document:sign-report:create', '签报新建',     'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'document:sign-report:list',   '签报列表',     'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'document:sign-report:view',   '签报详情',     'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'document:sign-report:update', '签报修改',     'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'document:sign-report:submit', '签报提交审批',  'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system'),
  (0, 'document:sign-report:approve','签报审批',     'BUTTON', NULL, NULL, 6, 'ACTIVE', 'system');

-- 档案按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'document:archive:list', '档案列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'document:archive:view', '档案详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有文档权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'document:dispatch', 'document:receive', 'document:sign-report', 'document:archive',
    'document:dispatch:create', 'document:dispatch:list', 'document:dispatch:view', 'document:dispatch:update', 'document:dispatch:delete', 'document:dispatch:submit', 'document:dispatch:archive',
    'document:receive:create', 'document:receive:list', 'document:receive:view', 'document:receive:update', 'document:receive:archive',
    'document:sign-report:create', 'document:sign-report:list', 'document:sign-report:view', 'document:sign-report:update', 'document:sign-report:submit', 'document:sign-report:approve',
    'document:archive:list', 'document:archive:view'
  ) AND `del_flag` = '0';
