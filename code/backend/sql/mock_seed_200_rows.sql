START TRANSACTION;

SET @seed_tag := DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%f');
SET @seed_user := CONCAT('seed_', @seed_tag);
SET @seed_time := NOW();

DROP TEMPORARY TABLE IF EXISTS tmp_emp;
CREATE TEMPORARY TABLE tmp_emp AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS emp_id,
  COALESCE(dept_id, 1) AS dept_id,
  emp_name
FROM sys_employee
WHERE del_flag = '0' AND status = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_dept;
CREATE TEMPORARY TABLE tmp_dept AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS dept_id,
  dept_name
FROM sys_dept
WHERE del_flag = '0' AND status = 0;

SET @emp_count := (SELECT COUNT(*) FROM tmp_emp);
SET @dept_count := (SELECT COUNT(*) FROM tmp_dept);

INSERT INTO adm_supply_category (
  category_name, parent_id, status, del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
  CONCAT('SupplyCategory', LPAD(n, 2, '0')),
  CASE WHEN n <= 5 THEN NULL ELSE n - 5 END,
  'ACTIVE',
  '0',
  @seed_user,
  @seed_time - INTERVAL (11 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (11 - n) DAY,
  0
FROM seq;

DROP TEMPORARY TABLE IF EXISTS tmp_new_category;
CREATE TEMPORARY TABLE tmp_new_category AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS category_id
FROM adm_supply_category
WHERE create_by = @seed_user;

SET @category_count := (SELECT COUNT(*) FROM tmp_new_category);

INSERT INTO adm_supply (
  supply_code, supply_name, category_id, unit, spec, safety_stock, status, del_flag,
  create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 25
)
SELECT
  CONCAT('SP', @seed_tag, LPAD(n, 4, '0')),
  CONCAT('SupplyItem', LPAD(n, 2, '0')),
  c.category_id,
  ELT(1 + MOD(n - 1, 4), 'PCS', 'BOX', 'PACK', 'BOOK'),
  ELT(1 + MOD(n - 1, 5), 'STANDARD', 'THICK', 'BUSINESS', 'PORTABLE', 'LARGE'),
  5 + MOD(n * 3, 20),
  IF(MOD(n, 6) = 0, 'INACTIVE', 'ACTIVE'),
  '0',
  @seed_user,
  @seed_time - INTERVAL (26 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (26 - n) DAY,
  0
FROM seq
JOIN tmp_new_category c ON c.rn = 1 + MOD(seq.n - 1, @category_count);

DROP TEMPORARY TABLE IF EXISTS tmp_new_supply;
CREATE TEMPORARY TABLE tmp_new_supply AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS supply_id,
  supply_name
FROM adm_supply
WHERE create_by = @seed_user;

SET @supply_count := (SELECT COUNT(*) FROM tmp_new_supply);

INSERT INTO adm_supply_stock (
  supply_id, quantity, locked_quantity, location, del_flag, create_by, create_time, update_by, update_time, version
)
SELECT
  supply_id,
  50 + rn * 7,
  MOD(rn, 5),
  CONCAT('LOC-A-', LPAD(rn, 2, '0')),
  '0',
  @seed_user,
  @seed_time - INTERVAL rn DAY,
  @seed_user,
  @seed_time - INTERVAL rn DAY,
  0
FROM tmp_new_supply;

INSERT INTO adm_supply_request (
  request_no, request_type, emp_id, dept_id, reason, status, approve_time, reject_reason,
  del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 20
)
SELECT
  CONCAT('REQ', @seed_tag, LPAD(n, 4, '0')),
  ELT(1 + MOD(n - 1, 3), 'OUT', 'OUT', 'RETURN'),
  e.emp_id,
  e.dept_id,
  CONCAT('Office request batch ', LPAD(n, 2, '0')),
  CASE MOD(n, 4)
    WHEN 0 THEN 'APPROVED'
    WHEN 1 THEN 'PENDING'
    WHEN 2 THEN 'REJECTED'
    ELSE 'DRAFT'
  END,
  CASE WHEN MOD(n, 4) = 0 THEN @seed_time - INTERVAL n HOUR ELSE NULL END,
  CASE WHEN MOD(n, 4) = 2 THEN 'Over monthly quota' ELSE NULL END,
  '0',
  @seed_user,
  @seed_time - INTERVAL (21 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (21 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n - 1, @emp_count);

DROP TEMPORARY TABLE IF EXISTS tmp_new_request;
CREATE TEMPORARY TABLE tmp_new_request AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS request_id
FROM adm_supply_request
WHERE create_by = @seed_user;

SET @request_count := (SELECT COUNT(*) FROM tmp_new_request);

INSERT INTO adm_supply_request_item (
  request_id, supply_id, quantity, remark, del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 30
)
SELECT
  r.request_id,
  s.supply_id,
  1 + MOD(n, 6),
  CONCAT('Auto request item ', LPAD(n, 2, '0')),
  '0',
  @seed_user,
  @seed_time - INTERVAL (31 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (31 - n) DAY,
  0
FROM seq
JOIN tmp_new_request r ON r.rn = 1 + MOD(seq.n - 1, @request_count)
JOIN tmp_new_supply s ON s.rn = 1 + MOD(seq.n * 2 - 1, @supply_count);

INSERT INTO hr_employee_contract (
  emp_id, contract_no, contract_type, start_date, end_date, sign_date, status, remark,
  del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 12
)
SELECT
  e.emp_id,
  CONCAT('HT', @seed_tag, LPAD(n, 4, '0')),
  ELT(1 + MOD(n - 1, 3), 'FULL_TIME', 'LABOR', 'INTERNSHIP'),
  DATE_SUB(CURDATE(), INTERVAL (360 + n * 12) DAY),
  DATE_ADD(CURDATE(), INTERVAL (180 + n * 15) DAY),
  DATE_SUB(CURDATE(), INTERVAL (390 + n * 12) DAY),
  CASE MOD(n, 3)
    WHEN 0 THEN 'EXPIRED'
    WHEN 1 THEN 'ACTIVE'
    ELSE 'PENDING'
  END,
  CONCAT('Employee contract mock ', LPAD(n, 2, '0')),
  '0',
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n * 3 - 1, @emp_count);

INSERT INTO hr_employee_change (
  emp_id, change_type, before_dept_id, after_dept_id, before_post, after_post, effective_date, reason,
  status, del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 12
)
SELECT
  e.emp_id,
  ELT(1 + MOD(n - 1, 4), 'TRANSFER', 'PROMOTION', 'DEMOTION', 'ADJUST'),
  e.dept_id,
  d.dept_id,
  ELT(1 + MOD(n - 1, 4), 'SPECIALIST', 'ENGINEER', 'SUPERVISOR', 'MANAGER'),
  ELT(1 + MOD(n, 4), 'ENGINEER', 'SUPERVISOR', 'MANAGER', 'SENIOR_MANAGER'),
  DATE_SUB(CURDATE(), INTERVAL (n * 18) DAY),
  CONCAT('Org change batch ', LPAD(n, 2, '0')),
  'EFFECTIVE',
  '0',
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n * 5 - 1, @emp_count)
JOIN tmp_dept d ON d.rn = 1 + MOD(seq.n * 2 - 1, @dept_count);

INSERT INTO hr_employee_certificate (
  emp_id, certificate_name, certificate_no, issue_org, issue_date, expire_date, attachment_url, status,
  del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 12
)
SELECT
  e.emp_id,
  ELT(1 + MOD(n - 1, 4), 'PMP', 'HR_CERT', 'ACCOUNTING_CERT', 'SECURITY_CERT'),
  CONCAT('CERT', @seed_tag, LPAD(n, 4, '0')),
  ELT(1 + MOD(n - 1, 4), 'PMI', 'HR_MINISTRY', 'FINANCE_BUREAU', 'MIIT'),
  DATE_SUB(CURDATE(), INTERVAL (200 + n * 10) DAY),
  DATE_ADD(CURDATE(), INTERVAL (200 + n * 20) DAY),
  CONCAT('/mock/cert/', @seed_tag, '/', LPAD(n, 2, '0'), '.pdf'),
  IF(MOD(n, 5) = 0, 'EXPIRED', 'VALID'),
  '0',
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n * 7 - 1, @emp_count);

INSERT INTO hr_employee_education (
  emp_id, school_name, major, degree, start_date, end_date, certificate_url,
  del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 12
)
SELECT
  e.emp_id,
  ELT(1 + MOD(n - 1, 5), 'FUDAN', 'SJTU', 'ZJU', 'HUST', 'WHU'),
  ELT(1 + MOD(n - 1, 5), 'CS', 'MBA', 'FINANCE', 'HR', 'LAW'),
  ELT(1 + MOD(n - 1, 3), 'BACHELOR', 'MASTER', 'DOCTOR'),
  DATE_SUB(CURDATE(), INTERVAL (3000 + n * 50) DAY),
  DATE_SUB(CURDATE(), INTERVAL (1500 + n * 30) DAY),
  CONCAT('/mock/education/', @seed_tag, '/', LPAD(n, 2, '0'), '.pdf'),
  '0',
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n * 11 - 1, @emp_count);

INSERT INTO fin_contract (
  contract_no, contract_name, counterparty, amount, signed_date, start_date, end_date,
  owner_emp_id, dept_id, status, remark, del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 12
)
SELECT
  CONCAT('FIN', @seed_tag, LPAD(n, 4, '0')),
  CONCAT('Finance contract batch ', LPAD(n, 2, '0')),
  ELT(1 + MOD(n - 1, 6), 'Vendor_East_A', 'Service_North_B', 'Channel_South_C', 'Partner_Southwest_D', 'Client_YRD_E', 'Client_PRD_F'),
  5000 + n * 3250,
  DATE_SUB(CURDATE(), INTERVAL (90 + n * 7) DAY),
  DATE_SUB(CURDATE(), INTERVAL (60 + n * 5) DAY),
  DATE_ADD(CURDATE(), INTERVAL (180 + n * 12) DAY),
  e.emp_id,
  e.dept_id,
  CASE MOD(n, 4)
    WHEN 0 THEN 'APPROVED'
    WHEN 1 THEN 'DRAFT'
    WHEN 2 THEN 'EXECUTING'
    ELSE 'COMPLETED'
  END,
  CONCAT('Finance contract mock ', LPAD(n, 2, '0')),
  '0',
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (13 - n) DAY,
  0
FROM seq
JOIN tmp_emp e ON e.rn = 1 + MOD(seq.n * 13 - 1, @emp_count);

DROP TEMPORARY TABLE IF EXISTS tmp_new_contract;
CREATE TEMPORARY TABLE tmp_new_contract AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS contract_id
FROM fin_contract
WHERE create_by = @seed_user;

SET @contract_count := (SELECT COUNT(*) FROM tmp_new_contract);

INSERT INTO fin_payment (
  payment_no, contract_id, expense_id, payee, amount, planned_date, paid_time, pay_method, status, remark,
  del_flag, create_by, create_time, update_by, update_time, version
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 15
)
SELECT
  CONCAT('PAY', @seed_tag, LPAD(n, 4, '0')),
  c.contract_id,
  NULL,
  ELT(1 + MOD(n - 1, 6), 'Vendor_East_A', 'Service_North_B', 'Channel_South_C', 'Partner_Southwest_D', 'Client_YRD_E', 'Client_PRD_F'),
  1500 + n * 980,
  DATE_ADD(CURDATE(), INTERVAL n DAY),
  CASE WHEN MOD(n, 3) = 0 THEN @seed_time - INTERVAL n HOUR ELSE NULL END,
  ELT(1 + MOD(n - 1, 4), 'BANK', 'ALIPAY', 'WECHAT', 'CASH'),
  CASE MOD(n, 4)
    WHEN 0 THEN 'APPROVED'
    WHEN 1 THEN 'DRAFT'
    WHEN 2 THEN 'PENDING'
    ELSE 'PAID'
  END,
  CONCAT('Payment plan mock ', LPAD(n, 2, '0')),
  '0',
  @seed_user,
  @seed_time - INTERVAL (16 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (16 - n) DAY,
  0
FROM seq
JOIN tmp_new_contract c ON c.rn = 1 + MOD(seq.n - 1, @contract_count);

INSERT INTO rpt_alert_rule (
  rule_name, rule_type, metric, condition_type, threshold, threshold_max, check_cron,
  notify_type, notify_targets, status, del_flag, create_by, create_time, update_by, update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 5
)
SELECT
  CONCAT('Alert rule ', LPAD(n, 2, '0')),
  ELT(1 + MOD(n - 1, 3), 'FINANCE', 'ATTENDANCE', 'ASSET'),
  ELT(1 + MOD(n - 1, 5), 'budget_usage', 'payment_delay', 'attendance_rate', 'asset_idle_rate', 'expense_growth'),
  ELT(1 + MOD(n - 1, 4), 'gt', 'lt', 'eq', 'between'),
  50 + n * 5,
  CASE WHEN MOD(n, 4) = 0 THEN 100 + n * 3 ELSE NULL END,
  ELT(1 + MOD(n - 1, 3), '0 */2 * * * ?', '0 0 9 * * ?', '0 30 8 * * 1'),
  ELT(1 + MOD(n - 1, 3), 'inner', 'email', 'sms'),
  'admin,finance,hr',
  IF(MOD(n, 5) = 0, '1', '0'),
  0,
  @seed_user,
  @seed_time - INTERVAL (6 - n) DAY,
  @seed_user,
  @seed_time - INTERVAL (6 - n) DAY
FROM seq;

DROP TEMPORARY TABLE IF EXISTS tmp_new_rule;
CREATE TEMPORARY TABLE tmp_new_rule AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS rule_id
FROM rpt_alert_rule
WHERE create_by = @seed_user;

SET @rule_count := (SELECT COUNT(*) FROM tmp_new_rule);

INSERT INTO rpt_alert_log (
  rule_id, alert_level, metric_value, threshold, alert_content, notify_status,
  handle_status, handler, handle_remark, alert_time, handle_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
  r.rule_id,
  ELT(1 + MOD(n - 1, 3), '0', '1', '2'),
  40 + n * 8.5,
  55 + MOD(n, 5) * 5,
  CONCAT('Alert log ', LPAD(n, 2, '0'), ', please review metric swing'),
  IF(MOD(n, 2) = 0, '1', '0'),
  IF(MOD(n, 3) = 0, '1', '0'),
  CASE WHEN MOD(n, 3) = 0 THEN 'system_admin' ELSE NULL END,
  CASE WHEN MOD(n, 3) = 0 THEN 'Reviewed manually' ELSE NULL END,
  @seed_time - INTERVAL n HOUR,
  CASE WHEN MOD(n, 3) = 0 THEN @seed_time - INTERVAL (n - 1) HOUR ELSE NULL END
FROM seq
JOIN tmp_new_rule r ON r.rn = 1 + MOD(seq.n - 1, @rule_count);

COMMIT;

SELECT 'adm_supply_category' AS table_name, COUNT(*) AS inserted_rows
FROM adm_supply_category
WHERE create_by = @seed_user
UNION ALL
SELECT 'adm_supply', COUNT(*) FROM adm_supply WHERE create_by = @seed_user
UNION ALL
SELECT 'adm_supply_stock', COUNT(*) FROM adm_supply_stock WHERE create_by = @seed_user
UNION ALL
SELECT 'adm_supply_request', COUNT(*) FROM adm_supply_request WHERE create_by = @seed_user
UNION ALL
SELECT 'adm_supply_request_item', COUNT(*) FROM adm_supply_request_item WHERE create_by = @seed_user
UNION ALL
SELECT 'hr_employee_contract', COUNT(*) FROM hr_employee_contract WHERE create_by = @seed_user
UNION ALL
SELECT 'hr_employee_change', COUNT(*) FROM hr_employee_change WHERE create_by = @seed_user
UNION ALL
SELECT 'hr_employee_certificate', COUNT(*) FROM hr_employee_certificate WHERE create_by = @seed_user
UNION ALL
SELECT 'hr_employee_education', COUNT(*) FROM hr_employee_education WHERE create_by = @seed_user
UNION ALL
SELECT 'fin_contract', COUNT(*) FROM fin_contract WHERE create_by = @seed_user
UNION ALL
SELECT 'fin_payment', COUNT(*) FROM fin_payment WHERE create_by = @seed_user
UNION ALL
SELECT 'rpt_alert_rule', COUNT(*) FROM rpt_alert_rule WHERE create_by = @seed_user
UNION ALL
SELECT 'rpt_alert_log', COUNT(*)
FROM rpt_alert_log l
JOIN tmp_new_rule r ON r.rule_id = l.rule_id
WHERE l.alert_time >= @seed_time - INTERVAL 1 DAY
UNION ALL
SELECT 'TOTAL', 200;
