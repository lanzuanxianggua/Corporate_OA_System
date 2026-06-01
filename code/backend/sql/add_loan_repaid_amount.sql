-- =============================================
-- 借支管理表新增 repaid_amount 字段
-- 用于记录已还款总额，支持原子更新和超还校验
-- 执行: ALTER TABLE oa_loan ADD COLUMN repaid_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已还金额';
-- =============================================

USE oa_system;

ALTER TABLE oa_loan
    ADD COLUMN repaid_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已还金额' AFTER loan_amount;

-- 初始化已有还款记录的 repaid_amount（数据一致性）
UPDATE oa_loan l
    INNER JOIN (
        SELECT loan_id, SUM(amount) AS total_repaid
        FROM oa_loan_repayment
        GROUP BY loan_id
    ) r ON l.id = r.loan_id
SET l.repaid_amount = r.total_repaid
WHERE l.del_flag = '0';

-- 根据 repaid_amount 同步 status 字段
UPDATE oa_loan
SET status = CASE
    WHEN status = '1' AND repaid_amount >= loan_amount AND loan_amount > 0 THEN '4'
    WHEN status = '1' AND repaid_amount > 0 AND repaid_amount < loan_amount THEN '3'
    ELSE status
END
WHERE del_flag = '0' AND loan_amount > 0;
