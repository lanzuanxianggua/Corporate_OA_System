-- ============================================
-- V973__doc_msg_notification_types.sql
-- 增量: 文档模块通知类型 (DISPATCH/SIGN_REPORT) + 任务评论权限码
-- ============================================

-- 1) 注册文档模块通知类型
INSERT INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`) VALUES
  ('DISPATCH_APPROVE',     '发文审批通过', '发文流程审批通过时通知申请人', 1, 50, 'system'),
  ('DISPATCH_REJECT',      '发文审批拒绝', '发文流程被拒绝时通知申请人', 1, 51, 'system'),
  ('SIGN_REPORT_APPROVE',  '签报审批通过', '签报流程审批通过时通知申请人', 1, 60, 'system'),
  ('SIGN_REPORT_REJECT',   '签报审批拒绝', '签报流程被拒绝时通知申请人', 1, 61, 'system');

-- 2) 注册 task:comment:* 权限码 (V979 缺)
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (0, 'task:comment:create', '发表评论', 'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0, 'task:comment:list',   '评论列表', 'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system');

-- 3) 给 SUPER_ADMIN (role_id=1) 授权
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN ('task:comment:create', 'task:comment:list')
    AND `del_flag` = '0';
