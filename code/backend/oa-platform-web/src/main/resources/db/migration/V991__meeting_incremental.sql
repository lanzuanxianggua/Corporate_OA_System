-- ============================================================
-- V991: oa-meeting 增量
--   1) mt_resolutions 加 complete_time 列
--   2) 通知类型 2 条 (BOOKING_APPROVE/BOOKING_REJECT)
--   3) 补挂会议/决议按钮权限到正确子菜单
-- ============================================================

-- 1) 决议加完成时间
ALTER TABLE mt_resolutions
  ADD COLUMN complete_time DATETIME DEFAULT NULL COMMENT '完成时间' AFTER status;

-- 2) 通知类型
INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'BOOKING_APPROVE', '会议室预约通过', '会议室预约审批通过', 1, 61, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'BOOKING_APPROVE' AND `del_flag` = '0');

INSERT IGNORE INTO `msg_notification_types` (`code`, `name`, `description`, `enabled`, `sort_order`, `create_by`)
  SELECT 'BOOKING_REJECT', '会议室预约拒绝', '会议室预约被驳回', 1, 62, 'system'
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM `msg_notification_types` WHERE `code` = 'BOOKING_REJECT' AND `del_flag` = '0');

-- 3) 把 V973 挂在 parent_id=0 的会议/决议按钮权限重新挂到对应子菜单下
-- 找到子菜单 id
UPDATE sys_permission sp
INNER JOIN sys_permission sub ON sub.perm_code = 'meeting:meeting' AND sub.del_flag = '0'
SET sp.parent_id = sub.id
WHERE sp.perm_code IN ('meeting:meeting:list', 'meeting:meeting:view', 'meeting:meeting:create')
  AND sp.del_flag = '0'
  AND sp.parent_id = 0;

UPDATE sys_permission sp
INNER JOIN sys_permission sub ON sub.perm_code = 'meeting:resolution' AND sub.del_flag = '0'
SET sp.parent_id = sub.id
WHERE sp.perm_code IN ('meeting:resolution:list', 'meeting:resolution:view', 'meeting:resolution:create', 'meeting:resolution:update')
  AND sp.del_flag = '0'
  AND sp.parent_id = 0;

UPDATE sys_permission sp
INNER JOIN sys_permission sub ON sub.perm_code = 'meeting:room' AND sub.del_flag = '0'
SET sp.parent_id = sub.id
WHERE sp.perm_code IN ('meeting:room:list', 'meeting:room:view', 'meeting:room:create', 'meeting:room:update', 'meeting:room:delete')
  AND sp.del_flag = '0'
  AND sp.parent_id = 0;

UPDATE sys_permission sp
INNER JOIN sys_permission sub ON sub.perm_code = 'meeting:booking' AND sub.del_flag = '0'
SET sp.parent_id = sub.id
WHERE sp.perm_code IN ('meeting:booking:list', 'meeting:booking:view', 'meeting:booking:create', 'meeting:booking:update', 'meeting:booking:cancel')
  AND sp.del_flag = '0'
  AND sp.parent_id = 0;
