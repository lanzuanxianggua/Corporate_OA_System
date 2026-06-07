-- ============================================
-- V964__fin_budget_unique_index.sql
-- 增量: fin_budgets 唯一索引 (dept_id + budget_year) 业务唯一性兜底
-- 说明: Service 层用 LIMIT 1 取一条, 这里在 DB 层加最后一道防线.
--       若有存量重复数据, 此迁移会失败 — 需先清理. 当前 V960 上线后尚无
--       生产数据, 直接添加.
-- ============================================

ALTER TABLE `fin_budgets`
  ADD UNIQUE INDEX `uk_fin_budget_dept_year` (`dept_id`, `budget_year`);
