-- Insert 200+ mock leave applications with matching workflow data.
-- Safe to run repeatedly: every run uses a unique create_by seed tag.

START TRANSACTION;

SET @seed_time := NOW();
SET @seed_user := CAST(CONCAT('mock_lwf_', DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s')) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci;
SET @row_count := 220;

SELECT
  id, version, node_config
INTO
  @leave_process_id, @leave_process_version, @leave_node_config
FROM wf_process_definition
WHERE process_type = 'leave'
  AND status = '0'
  AND del_flag = '0'
ORDER BY version DESC, id DESC
LIMIT 1;

SET @admin_id := (
  SELECT e.id
  FROM sys_employee e
  JOIN sys_emp_role er ON er.emp_id = e.id
  JOIN sys_role r ON r.id = er.role_id
  WHERE e.del_flag = '0'
    AND e.status = 1
    AND r.role_key = 'ADMIN'
  ORDER BY e.id
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_active_emp;
CREATE TEMPORARY TABLE tmp_active_emp AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS emp_id,
  emp_name
FROM sys_employee
WHERE del_flag = '0'
  AND status = 1
ORDER BY id;

SET @emp_count := (SELECT COUNT(*) FROM tmp_active_emp);
SET @fallback_emp_id := (SELECT emp_id FROM tmp_active_emp ORDER BY rn LIMIT 1);
SET @admin_id := COALESCE(@admin_id, @fallback_emp_id);

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq AS
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < @row_count
)
SELECT n FROM seq;

INSERT INTO oa_leave_apply (
  emp_id,
  leave_type,
  start_time,
  end_time,
  days,
  reason,
  status,
  process_instance_id,
  del_flag,
  create_by,
  create_time,
  update_by,
  update_time
)
SELECT
  e.emp_id,
  CAST(MOD(s.n - 1, 7) AS CHAR),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n - 1, 90) - 45) DAY), '09:00:00'),
  CASE
    WHEN MOD(s.n, 12) = 0 THEN TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n - 1, 90) - 45) DAY), '13:00:00')
    ELSE TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n - 1, 90) - 45 + MOD(s.n, 5)) DAY), '18:00:00')
  END,
  CASE
    WHEN MOD(s.n, 12) = 0 THEN 0.5
    ELSE CAST(1 + MOD(s.n, 5) AS DECIMAL(4,1))
  END,
  CONCAT(
    '模拟请假流程数据 #', LPAD(s.n, 3, '0'), ' - ',
    ELT(1 + MOD(s.n - 1, 6), '家庭事务', '身体不适', '年度休假', '证件办理', '子女事务', '个人安排')
  ),
  CASE
    WHEN MOD(s.n, 10) IN (2, 3, 4, 9) THEN 1
    WHEN MOD(s.n, 10) IN (5, 6) THEN 2
    WHEN MOD(s.n, 10) = 7 THEN 3
    ELSE 0
  END,
  NULL,
  '0',
  @seed_user,
  @seed_time - INTERVAL (220 - s.n) HOUR,
  @seed_user,
  @seed_time - INTERVAL (220 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 7 - 1, @emp_count);

DROP TEMPORARY TABLE IF EXISTS tmp_new_leave;
CREATE TEMPORARY TABLE tmp_new_leave AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS n,
  id AS leave_id,
  emp_id,
  status AS leave_status,
  days,
  create_time,
  start_time,
  end_time
FROM oa_leave_apply
WHERE create_by = @seed_user
ORDER BY id;

INSERT INTO wf_process_instance (
  process_id,
  business_type,
  business_id,
  initiator_id,
  current_node,
  condition_context,
  status,
  start_time,
  end_time,
  del_flag,
  create_by,
  create_time,
  snapshot_node_config,
  process_version
)
SELECT
  @leave_process_id,
  'leave',
  l.leave_id,
  l.emp_id,
  CASE WHEN MOD(l.n, 10) IN (1, 2, 3, 4, 6, 7, 9) THEN 1 ELSE 0 END,
  JSON_OBJECT('days', l.days),
  CAST(l.leave_status AS CHAR),
  l.create_time,
  CASE WHEN l.leave_status IN (1, 2, 3) THEN l.create_time + INTERVAL 18 HOUR ELSE NULL END,
  '0',
  @seed_user,
  l.create_time,
  @leave_node_config,
  @leave_process_version
FROM tmp_new_leave l;

DROP TEMPORARY TABLE IF EXISTS tmp_new_instance;
CREATE TEMPORARY TABLE tmp_new_instance AS
SELECT
  l.n,
  l.leave_id,
  l.emp_id,
  l.leave_status,
  l.days,
  l.create_time,
  i.id AS instance_id
FROM tmp_new_leave l
JOIN wf_process_instance i
  ON i.business_type = 'leave'
 AND i.business_id = l.leave_id
 AND i.create_by = @seed_user;

UPDATE oa_leave_apply l
JOIN tmp_new_instance i ON i.leave_id = l.id
SET l.process_instance_id = i.instance_id,
    l.update_by = @seed_user,
    l.update_time = @seed_time
WHERE l.create_by = @seed_user;

INSERT INTO wf_task (
  instance_id,
  node_id,
  node_name,
  assignee_id,
  task_type,
  parent_task_id,
  status,
  opinion,
  signature,
  due_time,
  complete_time,
  remind_count,
  escalation_count,
  create_time
)
SELECT
  i.instance_id,
  NULL,
  '部门经理审批',
  COALESCE(m.emp_id, @admin_id),
  'TODO',
  NULL,
  CASE
    WHEN MOD(i.n, 10) IN (0, 8) THEN '0'
    WHEN MOD(i.n, 10) = 5 THEN '2'
    ELSE '1'
  END,
  CASE
    WHEN MOD(i.n, 10) IN (0, 8) THEN NULL
    WHEN MOD(i.n, 10) = 5 THEN '请调整请假时间后重新提交'
    ELSE '同意，安排好工作交接'
  END,
  NULL,
  i.create_time + INTERVAL 48 HOUR,
  CASE WHEN MOD(i.n, 10) IN (0, 8) THEN NULL ELSE i.create_time + INTERVAL 6 HOUR END,
  MOD(i.n, 3),
  0,
  i.create_time + INTERVAL 10 MINUTE
FROM tmp_new_instance i
LEFT JOIN tmp_active_emp m ON m.rn = 1 + MOD(i.n * 3 - 1, @emp_count);

INSERT INTO wf_task (
  instance_id,
  node_id,
  node_name,
  assignee_id,
  task_type,
  parent_task_id,
  status,
  opinion,
  signature,
  due_time,
  complete_time,
  remind_count,
  escalation_count,
  create_time
)
SELECT
  i.instance_id,
  NULL,
  '人事备案',
  @admin_id,
  'TODO',
  NULL,
  CASE
    WHEN MOD(i.n, 10) = 1 THEN '0'
    WHEN MOD(i.n, 10) = 6 THEN '2'
    WHEN MOD(i.n, 10) = 7 THEN '4'
    ELSE '1'
  END,
  CASE
    WHEN MOD(i.n, 10) = 1 THEN NULL
    WHEN MOD(i.n, 10) = 6 THEN '请假天数与假期余额不匹配'
    WHEN MOD(i.n, 10) = 7 THEN '申请人已撤回'
    ELSE '备案完成'
  END,
  NULL,
  i.create_time + INTERVAL 72 HOUR,
  CASE WHEN MOD(i.n, 10) = 1 THEN NULL ELSE i.create_time + INTERVAL 16 HOUR END,
  MOD(i.n + 1, 3),
  0,
  i.create_time + INTERVAL 8 HOUR
FROM tmp_new_instance i
WHERE MOD(i.n, 10) IN (1, 2, 3, 4, 6, 7, 9);

DROP TEMPORARY TABLE IF EXISTS tmp_task1;
CREATE TEMPORARY TABLE tmp_task1 AS
SELECT
  i.n,
  i.leave_id,
  i.instance_id,
  t.id AS task_id,
  t.assignee_id,
  t.status,
  t.complete_time,
  t.node_name
FROM tmp_new_instance i
JOIN wf_task t
  ON t.instance_id = i.instance_id
 AND t.node_name = '部门经理审批';

DROP TEMPORARY TABLE IF EXISTS tmp_task2;
CREATE TEMPORARY TABLE tmp_task2 AS
SELECT
  i.n,
  i.leave_id,
  i.instance_id,
  t.id AS task_id,
  t.assignee_id,
  t.status,
  t.complete_time,
  t.node_name
FROM tmp_new_instance i
JOIN wf_task t
  ON t.instance_id = i.instance_id
 AND t.node_name = '人事备案';

INSERT INTO oa_approval_record (
  apply_id,
  business_type,
  approver_id,
  delegator_id,
  approve_status,
  remark,
  approve_time,
  task_id,
  node_name
)
SELECT
  t.leave_id,
  'leave',
  t.assignee_id,
  NULL,
  CAST(t.status AS UNSIGNED),
  CASE
    WHEN t.status = '2' THEN '请调整请假时间后重新提交'
    ELSE '同意，安排好工作交接'
  END,
  COALESCE(t.complete_time, @seed_time),
  t.task_id,
  t.node_name
FROM tmp_task1 t
WHERE t.status IN ('1', '2');

INSERT INTO oa_approval_record (
  apply_id,
  business_type,
  approver_id,
  delegator_id,
  approve_status,
  remark,
  approve_time,
  task_id,
  node_name
)
SELECT
  t.leave_id,
  'leave',
  t.assignee_id,
  NULL,
  CAST(t.status AS UNSIGNED),
  CASE
    WHEN t.status = '2' THEN '请假天数与假期余额不匹配'
    ELSE '备案完成'
  END,
  COALESCE(t.complete_time, @seed_time),
  t.task_id,
  t.node_name
FROM tmp_task2 t
WHERE t.status IN ('1', '2');

COMMIT;

SELECT @seed_user AS seed_user;

SET @inserted_leave_count := (
  SELECT COUNT(*)
  FROM oa_leave_apply
  WHERE create_by = @seed_user
);
SET @inserted_instance_count := (
  SELECT COUNT(*)
  FROM wf_process_instance
  WHERE create_by = @seed_user
);
SET @inserted_task_count := (
  SELECT COUNT(*)
  FROM wf_task t
  JOIN tmp_new_instance i ON i.instance_id = t.instance_id
);
SET @inserted_record_count := (
  SELECT COUNT(*)
  FROM oa_approval_record r
  JOIN tmp_new_leave l ON l.leave_id = r.apply_id
  WHERE r.business_type = CAST('leave' AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci
);

SELECT 'oa_leave_apply' AS table_name, @inserted_leave_count AS inserted_rows
UNION ALL
SELECT 'wf_process_instance', @inserted_instance_count
UNION ALL
SELECT 'wf_task', @inserted_task_count
UNION ALL
SELECT 'oa_approval_record', @inserted_record_count
UNION ALL
SELECT 'TOTAL', @inserted_leave_count + @inserted_instance_count + @inserted_task_count + @inserted_record_count;

SELECT status, COUNT(*) AS leave_rows
FROM oa_leave_apply
WHERE create_by = @seed_user
GROUP BY status
ORDER BY status;
