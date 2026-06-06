-- ============================================
-- V973__mt_permissions.sql
-- 增量: 会议管理权限注册
-- 对应模块: oa-meeting (菜单 id=9 '会议管理')
-- ============================================

-- 1) 注册会议子菜单 + 按钮权限
-- parent_id=9 对应 V900 种子数据中 '会议管理' 菜单
INSERT IGNORE INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  -- 子菜单
  (9, 'meeting:room',     '会议室管理', 'MENU', '/meeting/rooms',     NULL, 1, 'ACTIVE', 'system'),
  (9, 'meeting:booking',  '预约管理',   'MENU', '/meeting/bookings',  NULL, 2, 'ACTIVE', 'system'),
  (9, 'meeting:meeting',  '会议记录',   'MENU', '/meeting/meetings',  NULL, 3, 'ACTIVE', 'system'),
  (9, 'meeting:resolution','决议管理',  'MENU', '/meeting/resolutions',NULL, 4, 'ACTIVE', 'system');

-- 会议室按钮权限
INSERT IGNORE INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'meeting:room:list',   '会议室列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'meeting:room:view',   '会议室详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'meeting:room:create', '会议室新增', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'meeting:room:update', '会议室修改', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'meeting:room:delete', '会议室删除', 'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 预约按钮权限
INSERT IGNORE INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'meeting:booking:list',   '预约列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'meeting:booking:view',   '预约详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'meeting:booking:create', '预约新建', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'meeting:booking:update', '预约修改', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0, 'meeting:booking:cancel', '预约取消', 'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 会议记录按钮权限
INSERT IGNORE INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'meeting:meeting:list',   '会议记录列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'meeting:meeting:view',   '会议记录详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'meeting:meeting:create', '会议记录新建', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system');

-- 决议按钮权限
INSERT IGNORE INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'meeting:resolution:list',   '决议列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'meeting:resolution:view',   '决议详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'meeting:resolution:create', '决议新建', 'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0, 'meeting:resolution:update', '决议修改', 'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有会议权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'meeting:room', 'meeting:booking', 'meeting:meeting', 'meeting:resolution',
    'meeting:room:list', 'meeting:room:view', 'meeting:room:create', 'meeting:room:update', 'meeting:room:delete',
    'meeting:booking:list', 'meeting:booking:view', 'meeting:booking:create', 'meeting:booking:update', 'meeting:booking:cancel',
    'meeting:meeting:list', 'meeting:meeting:view', 'meeting:meeting:create',
    'meeting:resolution:list', 'meeting:resolution:view', 'meeting:resolution:create', 'meeting:resolution:update'
  ) AND `del_flag` = '0';
