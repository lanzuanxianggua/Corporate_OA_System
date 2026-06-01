-- =============================================
-- 审批记录表新增 delegator_id 字段
-- 用于在委托审批场景下记录委托人（原审批人）ID
-- 非委托审批时该字段为 NULL
-- 执行: ALTER TABLE oa_approval_record ADD COLUMN delegator_id BIGINT DEFAULT NULL COMMENT '委托人ID' AFTER approver_id;
-- =============================================

USE oa_system;

ALTER TABLE oa_approval_record
    ADD COLUMN delegator_id BIGINT DEFAULT NULL COMMENT '委托人ID（委托审批时记录原审批人）' AFTER approver_id;
