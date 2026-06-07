-- ============================================================
-- V990: oa-admin 印章申请/资产领用 权限码 (挂行政事务 parent_id=4)
--      + 通知类型 4 条 (SEAL_APPROVE/SEAL_REJECT/ASSET_APPROVE/ASSET_REJECT)
-- ============================================================

-- 1) 注册权限码
INSERT IGNORE INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, sort_order, status, create_by) VALUES
  -- 印章申请
  (4, 'admin:seal-apply:create',  '印章申请-新增',  'BUTTON', NULL, 11, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:submit',  '印章申请-提交',  'BUTTON', NULL, 12, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:use',     '印章申请-用印',  'BUTTON', NULL, 13, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:archive', '印章申请-归档',  'BUTTON', NULL, 14, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:delete',  '印章申请-删除',  'BUTTON', NULL, 15, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:view',    '印章申请-查看',  'BUTTON', NULL, 16, 'ACTIVE', 'system'),
  (4, 'admin:seal-apply:list',    '印章申请-列表',  'BUTTON', NULL, 17, 'ACTIVE', 'system'),
  -- 资产领用
  (4, 'admin:asset-loan:create',  '资产领用-新增',  'BUTTON', NULL, 21, 'ACTIVE', 'system'),
  (4, 'admin:asset-loan:submit',  '资产领用-提交',  'BUTTON', NULL, 22, 'ACTIVE', 'system'),
  (4, 'admin:asset-loan:return',  '资产领用-归还',  'BUTTON', NULL, 23, 'ACTIVE', 'system'),
  (4, 'admin:asset-loan:delete',  '资产领用-删除',  'BUTTON', NULL, 24, 'ACTIVE', 'system'),
  (4, 'admin:asset-loan:view',    '资产领用-查看',  'BUTTON', NULL, 25, 'ACTIVE', 'system'),
  (4, 'admin:asset-loan:list',    '资产领用-列表',  'BUTTON', NULL, 26, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN 分配
INSERT IGNORE INTO sys_role_permission (role_id, perm_id, create_by)
  SELECT 1, id, 'system'
  FROM sys_permission
  WHERE perm_code LIKE 'admin:seal-apply:%' OR perm_code LIKE 'admin:asset-loan:%';

-- 3) 通知类型 (oa-message)
INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'SEAL_APPROVE', '印章申请通过', '印章使用申请审批通过', 1, 51, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'SEAL_APPROVE' AND `del_flag` = '0');

INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'SEAL_REJECT', '印章申请拒绝', '印章使用申请被驳回', 1, 52, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'SEAL_REJECT' AND `del_flag` = '0');

INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'ASSET_APPROVE', '资产领用通过', '资产领用/报废审批通过', 1, 53, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'ASSET_APPROVE' AND `del_flag` = '0');

INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'ASSET_REJECT', '资产领用拒绝', '资产领用/报废被驳回', 1, 54, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'ASSET_REJECT' AND `del_flag` = '0');
