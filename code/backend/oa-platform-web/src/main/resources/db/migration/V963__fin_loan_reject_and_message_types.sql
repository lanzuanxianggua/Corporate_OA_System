-- ============================================
-- V963__fin_loan_reject_and_message_types.sql
-- 增量: 1) 注册 finance:loan:reject 权限 (resubmit 复用 finance:expense:create)
--       2) 借款通知 type 种子 (LOAN_APPROVE / LOAN_REJECT)
-- ============================================

-- 1) 注册 finance:loan:reject 权限码
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`)
  SELECT 0, 'finance:loan:reject', '借款驳回', 'BUTTON', NULL, NULL, 6, 'ACTIVE', 'system'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1 FROM `sys_permission` WHERE `perm_code` = 'finance:loan:reject' AND `del_flag` = '0'
  );

-- 2) 给 SUPER_ADMIN (role_id=1) 授权
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` = 'finance:loan:reject' AND `del_flag` = '0'
    AND NOT EXISTS (
      SELECT 1 FROM `sys_role_permission` rp
      WHERE rp.role_id = 1 AND rp.perm_id = `sys_permission`.id
    );

-- 3) 借款通知 type 种子 (msg_notification_types 表)
INSERT INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'LOAN_APPROVE', '借款审批通过', '借款流程审批通过时通知申请人', 1, 50, 'system'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1 FROM `msg_notification_types` WHERE `code` = 'LOAN_APPROVE' AND `del_flag` = '0'
  );

INSERT INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'LOAN_REJECT', '借款审批拒绝', '借款流程被拒绝时通知申请人', 1, 60, 'system'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1 FROM `msg_notification_types` WHERE `code` = 'LOAN_REJECT' AND `del_flag` = '0'
  );
