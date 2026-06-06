-- ============================================
-- V977__km_permissions.sql
-- 增量: 知识库权限注册
-- 对应模块: oa-knowledge (菜单 id=7 '知识库')
-- ============================================

-- 1) 注册知识库子菜单 + 按钮权限
-- parent_id=7 对应 V900 种子数据中 '知识库' 菜单 (id=7)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  -- 子菜单
  (7, 'knowledge:entry',    '知识条目',  'MENU',   '/knowledge/entries',    NULL, 1, 'ACTIVE', 'system'),
  (7, 'knowledge:category', '分类管理',  'MENU',   '/knowledge/categories', NULL, 2, 'ACTIVE', 'system');

-- 知识条目按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'knowledge:entry:list',   '条目列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'knowledge:entry:view',   '条目详情',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'knowledge:entry:create', '新增条目',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'knowledge:entry:update', '修改条目',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'knowledge:entry:delete', '删除条目',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 分类管理按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'knowledge:category:list',   '分类列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'knowledge:category:create', '新增分类',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'knowledge:category:update', '修改分类',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'knowledge:category:delete', '删除分类',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有知识库权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'knowledge:entry', 'knowledge:category',
    'knowledge:entry:list', 'knowledge:entry:view', 'knowledge:entry:create', 'knowledge:entry:update', 'knowledge:entry:delete',
    'knowledge:category:list', 'knowledge:category:create', 'knowledge:category:update', 'knowledge:category:delete'
  ) AND `del_flag` = '0';

-- ============================================
-- End V977__km_permissions.sql
-- ============================================
