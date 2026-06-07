-- ============================================
-- V988__msg_notification_types.sql
-- 增量: 消息通知类型字典 + 新增 msg:* 权限码
-- 现有: V974 已建 msg_email_logs / msg_sms_logs
--       V975 已建 message:* 权限 (message:notification/email-log/sms-log)
-- 本迁移: 1) 建 msg_notification_types 类型字典
--         2) 注册 msg:* 权限码并授权 SUPER_ADMIN
-- ============================================

-- 1) 通知类型字典表
CREATE TABLE `msg_notification_types` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`         VARCHAR(32)  NOT NULL                COMMENT '类型编码, 唯一',
  `name`         VARCHAR(64)  NOT NULL                COMMENT '类型名称',
  `description`  VARCHAR(256)          DEFAULT NULL   COMMENT '描述',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '0=禁用 1=启用',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '0=正常 1=删除',
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)           DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_type_code` (`code`),
  KEY `idx_msg_type_enabled` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知类型字典';

-- 2) 预置 5 条种子类型 (匹配 oa-workflow 业务场景)
INSERT INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`) VALUES
  ('LEAVE_APPROVE',  '请假审批通过', '请假流程审批通过时通知申请人', 1, 10, 'system'),
  ('LEAVE_REJECT',   '请假审批拒绝', '请假流程被拒绝时通知申请人', 1, 20, 'system'),
  ('EXPENSE_APPROVE','报销审批通过', '报销流程审批通过时通知申请人', 1, 30, 'system'),
  ('EXPENSE_REJECT', '报销审批拒绝', '报销流程被拒绝时通知申请人', 1, 40, 'system'),
  ('GENERAL',        '通用通知',     '通用业务通知', 1, 99, 'system');

-- 3) 注册 msg:* 权限码
--    parent_id=8 对应 V900 种子 '消息中心' 菜单
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (8, 'msg:notify:list',  '通知列表', 'BUTTON', NULL, NULL, 1,  'ACTIVE', 'system'),
  (8, 'msg:notify:view',  '通知详情', 'BUTTON', NULL, NULL, 2,  'ACTIVE', 'system'),
  (8, 'msg:notify:create','发送通知', 'BUTTON', NULL, NULL, 3,  'ACTIVE', 'system'),
  (8, 'msg:notify:read',  '标记已读', 'BUTTON', NULL, NULL, 4,  'ACTIVE', 'system'),
  (8, 'msg:type:list',    '类型列表', 'BUTTON', NULL, NULL, 5,  'ACTIVE', 'system'),
  (8, 'msg:type:create',  '新增类型', 'BUTTON', NULL, NULL, 6,  'ACTIVE', 'system'),
  (8, 'msg:type:update',  '更新类型', 'BUTTON', NULL, NULL, 7,  'ACTIVE', 'system'),
  (8, 'msg:type:delete',  '删除类型', 'BUTTON', NULL, NULL, 8,  'ACTIVE', 'system');

-- 4) 授权 SUPER_ADMIN (role_id=1)
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'msg:notify:list', 'msg:notify:view', 'msg:notify:create', 'msg:notify:read',
    'msg:type:list', 'msg:type:create', 'msg:type:update', 'msg:type:delete'
  ) AND `del_flag` = '0';
