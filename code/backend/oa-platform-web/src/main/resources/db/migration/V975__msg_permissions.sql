-- ============================================
-- V975__msg_permissions.sql
-- 增量: 消息模块权限注册
-- 对应模块: oa-message (菜单 id=8 '消息中心')
-- ============================================

-- 1) 注册消息子菜单
-- parent_id=8 对应 V900 种子数据中 '消息中心' 菜单 (id=8)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (8, 'message:notification', '消息通知', 'MENU', '/message/notifications', NULL, 1, 'ACTIVE', 'system'),
  (8, 'message:email-log',    '邮件日志', 'MENU', '/message/email-logs',    NULL, 2, 'ACTIVE', 'system'),
  (8, 'message:sms-log',      '短信日志', 'MENU', '/message/sms-logs',      NULL, 3, 'ACTIVE', 'system');

-- 消息通知按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'message:notification:list',   '通知列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'message:notification:view',   '通知详情',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0, 'message:notification:create', '发送通知',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system');

-- 邮件日志按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'message:email-log:list', '邮件日志列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'message:email-log:view', '邮件日志详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 短信日志按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'message:sms-log:list', '短信日志列表', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'message:sms-log:view', '短信日志详情', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有消息权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'message:notification', 'message:email-log', 'message:sms-log',
    'message:notification:list', 'message:notification:view', 'message:notification:create',
    'message:email-log:list', 'message:email-log:view',
    'message:sms-log:list', 'message:sms-log:view'
  ) AND `del_flag` = '0';
