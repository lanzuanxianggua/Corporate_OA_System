-- ============================================
-- V932__hr_leave_balance_seed.sql
-- 增量: 为 V900 种子员工初始化默认假期余额
-- 幂等: WHERE NOT EXISTS 防止重复插入
-- ============================================

INSERT INTO `hr_leave_balance` (`emp_id`, `leave_type`, `year`, `total_days`, `used_days`, `frozen_days`, `remaining_days`, `status`, `create_by`)
SELECT t.emp_id, t.leave_type, t.year, t.total_days, 0, 0, t.total_days, 'ACTIVE', 'system'
FROM (
  -- admin (emp_id=1): 年假 15 天 + 病假 30 天 + 事假 10 天
  SELECT 1 AS emp_id, 'ANNUAL' AS leave_type, 2026 AS year, 15.0 AS total_days UNION ALL
  SELECT 1, 'SICK', 2026, 30.0 UNION ALL
  SELECT 1, 'PERSONAL', 2026, 10.0 UNION ALL
  -- hr01 (emp_id=2)
  SELECT 2, 'ANNUAL', 2026, 15.0 UNION ALL
  SELECT 2, 'SICK', 2026, 30.0 UNION ALL
  SELECT 2, 'PERSONAL', 2026, 10.0 UNION ALL
  -- mgr01 (emp_id=3)
  SELECT 3, 'ANNUAL', 2026, 15.0 UNION ALL
  SELECT 3, 'SICK', 2026, 30.0 UNION ALL
  SELECT 3, 'PERSONAL', 2026, 10.0 UNION ALL
  -- emp01 (emp_id=4)
  SELECT 4, 'ANNUAL', 2026, 15.0 UNION ALL
  SELECT 4, 'SICK', 2026, 30.0 UNION ALL
  SELECT 4, 'PERSONAL', 2026, 10.0 UNION ALL
  -- fin01 (emp_id=5)
  SELECT 5, 'ANNUAL', 2026, 15.0 UNION ALL
  SELECT 5, 'SICK', 2026, 30.0 UNION ALL
  SELECT 5, 'PERSONAL', 2026, 10.0
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_leave_balance` b
  WHERE b.emp_id = t.emp_id AND b.leave_type = t.leave_type AND b.year = t.year
);
