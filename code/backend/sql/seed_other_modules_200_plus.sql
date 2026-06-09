-- Insert 200+ mock rows into non-leave OA modules.
-- This script only inserts new rows. It does not delete or update old rows,
-- except linking the rows it just created to their generated workflow instances.

START TRANSACTION;

SET @seed_time := NOW();
SET @seed_user := CAST(CONCAT('mock_other_', DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s')) AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_general_ci;
SET @approval_rows := 30;

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

DROP TEMPORARY TABLE IF EXISTS tmp_active_emp_2;
CREATE TEMPORARY TABLE tmp_active_emp_2 AS
SELECT * FROM tmp_active_emp;

DROP TEMPORARY TABLE IF EXISTS tmp_active_emp_3;
CREATE TEMPORARY TABLE tmp_active_emp_3 AS
SELECT * FROM tmp_active_emp;

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
SET @admin_id := COALESCE(@admin_id, @fallback_emp_id);

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq AS
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 80
)
SELECT n FROM seq;

DROP TEMPORARY TABLE IF EXISTS tmp_room;
CREATE TEMPORARY TABLE tmp_room AS
SELECT ROW_NUMBER() OVER (ORDER BY id) AS rn, id AS room_id
FROM oa_meeting_room
WHERE status = '0';
SET @room_count := COALESCE((SELECT COUNT(*) FROM tmp_room), 0);

DROP TEMPORARY TABLE IF EXISTS tmp_asset;
CREATE TEMPORARY TABLE tmp_asset AS
SELECT ROW_NUMBER() OVER (ORDER BY id) AS rn, id AS asset_id
FROM oa_asset
WHERE del_flag = '0';
SET @asset_count := COALESCE((SELECT COUNT(*) FROM tmp_asset), 0);

DROP TEMPORARY TABLE IF EXISTS tmp_doc_category;
CREATE TEMPORARY TABLE tmp_doc_category AS
SELECT ROW_NUMBER() OVER (ORDER BY id) AS rn, id AS category_id
FROM oa_document_category;
SET @doc_category_count := COALESCE((SELECT COUNT(*) FROM tmp_doc_category), 0);

INSERT INTO oa_business_trip (
  emp_id, destination, purpose, start_time, end_time, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  ELT(1 + MOD(s.n - 1, 8), '北京', '上海', '深圳', '广州', '杭州', '成都', '武汉', '南京'),
  CONCAT('模拟出差 #', LPAD(s.n, 3, '0'), ' - ', ELT(1 + MOD(s.n - 1, 5), '客户拜访', '项目验收', '供应商沟通', '技术交流', '市场调研')),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 45) - 20) DAY), '09:00:00'),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 45) - 18 + MOD(s.n, 4)) DAY), '18:00:00'),
  CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END,
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (80 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (80 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 3 - 1, @emp_count)
WHERE s.n <= @approval_rows;

INSERT INTO oa_outing (
  emp_id, reason, destination, start_time, end_time, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  CONCAT('模拟外出 #', LPAD(s.n, 3, '0'), ' - ', ELT(1 + MOD(s.n - 1, 5), '客户现场沟通', '银行业务办理', '合同材料递送', '税务咨询', '设备维修')),
  ELT(1 + MOD(s.n - 1, 6), '客户A办公区', '政务服务中心', '银行营业部', '供应商园区', '会议中心', '税务大厅'),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 35) - 12) DAY), MAKETIME(8 + MOD(s.n, 4), 0, 0)),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 35) - 12) DAY), MAKETIME(13 + MOD(s.n, 5), 30, 0)),
  CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END,
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (70 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (70 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 5 - 1, @emp_count)
WHERE s.n <= @approval_rows;

INSERT INTO oa_overtime (
  emp_id, overtime_date, start_time, end_time, hours, reason, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 35) - 20) DAY),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 35) - 20) DAY), '18:30:00'),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 35) - 20) DAY), MAKETIME(20 + MOD(s.n, 3), 30, 0)),
  CAST(2 + MOD(s.n, 4) AS DECIMAL(4,1)),
  CONCAT('模拟加班 #', LPAD(s.n, 3, '0'), ' - ', ELT(1 + MOD(s.n - 1, 5), '版本上线', '故障修复', '投标支持', '月度结算', '培训准备')),
  CAST(CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END AS CHAR),
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (65 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (65 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 7 - 1, @emp_count)
WHERE s.n <= @approval_rows;

INSERT INTO oa_purchase (
  emp_id, item_name, quantity, amount, reason, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  ELT(1 + MOD(s.n - 1, 8), '笔记本电脑', '显示器', '办公椅', '打印耗材', '会议设备', '网络交换机', '移动硬盘', '办公文具'),
  1 + MOD(s.n, 12),
  ROUND(300 + MOD(s.n * 791, 35000), 2),
  CONCAT('模拟采购 #', LPAD(s.n, 3, '0'), ' - 部门办公与项目支持'),
  CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END,
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (60 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (60 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 11 - 1, @emp_count)
WHERE s.n <= @approval_rows;

INSERT INTO oa_expense (
  emp_id, title, amount, category, description, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  CONCAT('模拟报销 #', LPAD(s.n, 3, '0')),
  ROUND(80 + MOD(s.n * 257, 18000), 2),
  CAST(MOD(s.n - 1, 5) AS CHAR),
  CONCAT('模拟报销说明 - ', ELT(1 + MOD(s.n - 1, 5), '差旅交通', '办公用品', '客户接待', '培训资料', '其他费用')),
  CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END,
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (55 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (55 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 13 - 1, @emp_count)
WHERE s.n <= @approval_rows;

INSERT INTO oa_loan (
  emp_id, loan_amount, repaid_amount, loan_reason, repayment_plan, status, process_instance_id,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  ROUND(1000 + MOD(s.n * 997, 60000), 2),
  CASE WHEN MOD(s.n, 10) IN (2, 3, 4, 6) THEN ROUND(MOD(s.n * 113, 3000), 2) ELSE 0.00 END,
  CONCAT('模拟借款 #', LPAD(s.n, 3, '0'), ' - ', ELT(1 + MOD(s.n - 1, 5), '差旅备用金', '项目周转', '培训垫付', '客户接待', '临时资金需求')),
  ELT(1 + MOD(s.n - 1, 4), '一次性还款', '3期等额还款', '6期等额还款', '12期等额还款'),
  CAST(CASE WHEN MOD(s.n, 10) IN (0, 8, 9) THEN 0 WHEN MOD(s.n, 10) = 5 THEN 2 WHEN MOD(s.n, 10) = 7 THEN 3 ELSE 1 END AS CHAR),
  NULL, '0', @seed_user,
  @seed_time - INTERVAL (50 - s.n) HOUR,
  @seed_user, @seed_time - INTERVAL (50 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 17 - 1, @emp_count)
WHERE s.n <= 25;

DROP TEMPORARY TABLE IF EXISTS tmp_wf_business;
CREATE TEMPORARY TABLE tmp_wf_business AS
SELECT 'trip' COLLATE utf8mb4_general_ci AS business_type, id AS business_id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci AS business_status,
       create_time, JSON_OBJECT('destination', destination) AS condition_context
FROM oa_business_trip WHERE create_by = @seed_user
UNION ALL
SELECT 'outing' COLLATE utf8mb4_general_ci, id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci, create_time, JSON_OBJECT('destination', destination)
FROM oa_outing WHERE create_by = @seed_user
UNION ALL
SELECT 'overtime' COLLATE utf8mb4_general_ci, id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci, create_time, JSON_OBJECT('hours', hours)
FROM oa_overtime WHERE create_by = @seed_user
UNION ALL
SELECT 'purchase' COLLATE utf8mb4_general_ci, id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci, create_time, JSON_OBJECT('amount', amount)
FROM oa_purchase WHERE create_by = @seed_user
UNION ALL
SELECT 'expense' COLLATE utf8mb4_general_ci, id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci, create_time, JSON_OBJECT('amount', amount)
FROM oa_expense WHERE create_by = @seed_user
UNION ALL
SELECT 'loan' COLLATE utf8mb4_general_ci, id, emp_id, CAST(status AS CHAR) COLLATE utf8mb4_general_ci, create_time, JSON_OBJECT('amount', loan_amount)
FROM oa_loan WHERE create_by = @seed_user;

DROP TEMPORARY TABLE IF EXISTS tmp_wf_business_numbered;
CREATE TEMPORARY TABLE tmp_wf_business_numbered AS
SELECT
  ROW_NUMBER() OVER (ORDER BY business_type, business_id) AS rn,
  b.*,
  d.id AS process_id,
  d.version AS process_version,
  d.node_config
FROM tmp_wf_business b
JOIN wf_process_definition d
  ON d.process_type = b.business_type
 AND d.status = '0'
 AND d.del_flag = '0';

INSERT INTO wf_process_instance (
  process_id, business_type, business_id, initiator_id, current_node, condition_context,
  status, start_time, end_time, del_flag, create_by, create_time, snapshot_node_config, process_version
)
SELECT
  process_id,
  business_type,
  business_id,
  emp_id,
  CASE WHEN business_status = '0' THEN 0 ELSE 1 END,
  condition_context,
  business_status,
  create_time,
  CASE WHEN business_status = '0' THEN NULL ELSE create_time + INTERVAL 12 HOUR END,
  '0',
  @seed_user,
  create_time,
  node_config,
  process_version
FROM tmp_wf_business_numbered;

UPDATE oa_business_trip b
JOIN wf_process_instance i
  ON i.business_type = 'trip' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

UPDATE oa_outing b
JOIN wf_process_instance i
  ON i.business_type = 'outing' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

UPDATE oa_overtime b
JOIN wf_process_instance i
  ON i.business_type = 'overtime' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

UPDATE oa_purchase b
JOIN wf_process_instance i
  ON i.business_type = 'purchase' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

UPDATE oa_expense b
JOIN wf_process_instance i
  ON i.business_type = 'expense' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

UPDATE oa_loan b
JOIN wf_process_instance i
  ON i.business_type = 'loan' COLLATE utf8mb4_general_ci
 AND i.business_id = b.id
 AND i.create_by = @seed_user
SET b.process_instance_id = i.id, b.update_by = @seed_user, b.update_time = @seed_time
WHERE b.create_by = @seed_user;

DROP TEMPORARY TABLE IF EXISTS tmp_new_instance;
CREATE TEMPORARY TABLE tmp_new_instance AS
SELECT
  ROW_NUMBER() OVER (ORDER BY id) AS rn,
  id AS instance_id,
  business_type,
  business_id,
  initiator_id,
  status,
  create_time
FROM wf_process_instance
WHERE create_by = @seed_user;

INSERT INTO wf_task (
  instance_id, node_id, node_name, assignee_id, task_type, parent_task_id, status,
  opinion, signature, due_time, complete_time, remind_count, escalation_count, create_time
)
SELECT
  i.instance_id,
  NULL,
  CASE i.business_type
    WHEN 'expense' THEN '财务审批'
    WHEN 'loan' THEN '财务确认'
    WHEN 'purchase' THEN '采购审批'
    WHEN 'overtime' THEN '加班审批'
    WHEN 'outing' THEN '外出审批'
    ELSE '部门经理审批'
  END,
  COALESCE(a.emp_id, @admin_id),
  'TODO',
  NULL,
  CASE WHEN i.status = '0' THEN '0' WHEN i.status = '2' THEN '2' WHEN i.status = '3' THEN '4' ELSE '1' END,
  CASE WHEN i.status = '0' THEN NULL WHEN i.status = '2' THEN '资料不完整，请补充后重新提交' WHEN i.status = '3' THEN '申请人已撤回' ELSE '同意' END,
  NULL,
  i.create_time + INTERVAL 48 HOUR,
  CASE WHEN i.status = '0' THEN NULL ELSE i.create_time + INTERVAL 5 HOUR END,
  MOD(i.rn, 3),
  0,
  i.create_time + INTERVAL 10 MINUTE
FROM tmp_new_instance i
LEFT JOIN tmp_active_emp a ON a.rn = 1 + MOD(i.rn * 5 - 1, @emp_count);

INSERT INTO oa_approval_record (
  apply_id, business_type, approver_id, delegator_id, approve_status,
  remark, approve_time, task_id, node_name
)
SELECT
  i.business_id,
  i.business_type,
  t.assignee_id,
  NULL,
  CAST(t.status AS UNSIGNED),
  t.opinion,
  COALESCE(t.complete_time, @seed_time),
  t.id,
  t.node_name
FROM tmp_new_instance i
JOIN wf_task t ON t.instance_id = i.instance_id
WHERE t.status IN ('1', '2', '4');

INSERT INTO oa_schedule (
  emp_id, title, content, start_time, end_time, status,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  e.emp_id,
  CONCAT('模拟日程 #', LPAD(s.n, 3, '0')),
  CONCAT('模拟日程内容：', ELT(1 + MOD(s.n - 1, 5), '客户沟通', '需求评审', '项目复盘', '部门周会', '资料整理')),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 30) - 5) DAY), MAKETIME(9 + MOD(s.n, 5), 0, 0)),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 30) - 5) DAY), MAKETIME(10 + MOD(s.n, 5), 30, 0)),
  MOD(s.n, 4),
  '0', @seed_user, @seed_time - INTERVAL (45 - s.n) HOUR, @seed_user, @seed_time - INTERVAL (45 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 19 - 1, @emp_count)
WHERE s.n <= 40;

INSERT INTO oa_message (
  sender_id, receiver_id, title, content, is_read,
  del_flag, create_by, create_time, update_by
)
SELECT
  sender.emp_id,
  receiver.emp_id,
  CONCAT('模拟消息 #', LPAD(s.n, 3, '0')),
  CONCAT('这是一条模拟站内消息，用于列表、未读、详情测试。批次：', @seed_user),
  MOD(s.n, 2),
  '0', @seed_user, @seed_time - INTERVAL (55 - s.n) HOUR, @seed_user
FROM tmp_seq s
JOIN tmp_active_emp sender ON sender.rn = 1 + MOD(s.n * 7 - 1, @emp_count)
JOIN tmp_active_emp_2 receiver ON receiver.rn = 1 + MOD(s.n * 11 - 1, @emp_count)
WHERE s.n <= 50
  AND sender.emp_id <> receiver.emp_id;

INSERT INTO oa_notice (
  title, content, type, emp_id, status, is_top,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  CONCAT('模拟公告 #', LPAD(s.n, 3, '0')),
  CONCAT('模拟公告内容，覆盖通知、公告、制度三类数据。批次：', @seed_user),
  MOD(s.n, 3),
  e.emp_id,
  CASE WHEN MOD(s.n, 5) = 0 THEN 0 WHEN MOD(s.n, 7) = 0 THEN 2 ELSE 1 END,
  IF(MOD(s.n, 9) = 0, '1', '0'),
  '0', @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR, @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 23 - 1, @emp_count)
WHERE s.n <= 25;

INSERT INTO oa_meeting (
  title, room_id, organizer_id, start_time, end_time, description, participants, status,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  CONCAT('模拟会议 #', LPAD(s.n, 3, '0')),
  r.room_id,
  e.emp_id,
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 20) - 3) DAY), MAKETIME(9 + MOD(s.n, 6), 0, 0)),
  TIMESTAMP(DATE_ADD(DATE(@seed_time), INTERVAL (MOD(s.n, 20) - 3) DAY), MAKETIME(10 + MOD(s.n, 6), 30, 0)),
  CONCAT('模拟会议说明：', ELT(1 + MOD(s.n - 1, 5), '项目例会', '需求评审', '经营分析', '培训分享', '复盘会议')),
  CONCAT('[', e.emp_id, ',', p1.emp_id, ',', p2.emp_id, ']'),
  CAST(MOD(s.n, 4) AS CHAR),
  '0', @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR, @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_room r ON r.rn = 1 + MOD(s.n - 1, @room_count)
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 3 - 1, @emp_count)
JOIN tmp_active_emp_2 p1 ON p1.rn = 1 + MOD(s.n * 5 - 1, @emp_count)
JOIN tmp_active_emp_3 p2 ON p2.rn = 1 + MOD(s.n * 7 - 1, @emp_count)
WHERE s.n <= 25
  AND @room_count > 0;

INSERT INTO oa_document (
  title, content, category_id, emp_id, file_url, status,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT
  CONCAT('模拟公文 #', LPAD(s.n, 3, '0')),
  CONCAT('模拟公文正文，用于公文列表、分类、归档状态测试。批次：', @seed_user),
  c.category_id,
  e.emp_id,
  CONCAT('/mock/document/', @seed_user, '/', LPAD(s.n, 3, '0'), '.pdf'),
  MOD(s.n, 3),
  '0', @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR, @seed_user, @seed_time - INTERVAL (30 - s.n) HOUR
FROM tmp_seq s
JOIN tmp_doc_category c ON c.rn = 1 + MOD(s.n - 1, @doc_category_count)
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 29 - 1, @emp_count)
WHERE s.n <= 25
  AND @doc_category_count > 0;

INSERT INTO oa_asset_borrow (
  asset_id, borrower_id, borrow_time, expected_return, actual_return, status, remark, create_time
)
SELECT
  a.asset_id,
  e.emp_id,
  @seed_time - INTERVAL (25 - s.n) DAY,
  @seed_time + INTERVAL (5 + MOD(s.n, 20)) DAY,
  CASE WHEN MOD(s.n, 3) = 0 THEN @seed_time - INTERVAL MOD(s.n, 8) DAY ELSE NULL END,
  CAST(CASE WHEN MOD(s.n, 3) = 0 THEN 1 WHEN MOD(s.n, 5) = 0 THEN 2 ELSE 0 END AS CHAR),
  CONCAT(@seed_user, ' 模拟资产借用 #', LPAD(s.n, 3, '0')),
  @seed_time - INTERVAL (25 - s.n) DAY
FROM tmp_seq s
JOIN tmp_asset a ON a.rn = 1 + MOD(s.n - 1, @asset_count)
JOIN tmp_active_emp e ON e.rn = 1 + MOD(s.n * 31 - 1, @emp_count)
WHERE s.n <= 20
  AND @asset_count > 0;

COMMIT;

SELECT @seed_user AS seed_user;

SET @business_trip_count := (SELECT COUNT(*) FROM oa_business_trip WHERE create_by = @seed_user);
SET @outing_count := (SELECT COUNT(*) FROM oa_outing WHERE create_by = @seed_user);
SET @overtime_count := (SELECT COUNT(*) FROM oa_overtime WHERE create_by = @seed_user);
SET @purchase_count := (SELECT COUNT(*) FROM oa_purchase WHERE create_by = @seed_user);
SET @expense_count := (SELECT COUNT(*) FROM oa_expense WHERE create_by = @seed_user);
SET @loan_count := (SELECT COUNT(*) FROM oa_loan WHERE create_by = @seed_user);
SET @schedule_count := (SELECT COUNT(*) FROM oa_schedule WHERE create_by = @seed_user);
SET @message_count := (SELECT COUNT(*) FROM oa_message WHERE create_by = @seed_user);
SET @notice_count := (SELECT COUNT(*) FROM oa_notice WHERE create_by = @seed_user);
SET @meeting_count := (SELECT COUNT(*) FROM oa_meeting WHERE create_by = @seed_user);
SET @document_count := (SELECT COUNT(*) FROM oa_document WHERE create_by = @seed_user);
SET @asset_borrow_count := (SELECT COUNT(*) FROM oa_asset_borrow WHERE remark LIKE CONCAT(@seed_user, '%'));
SET @workflow_instance_count := (SELECT COUNT(*) FROM wf_process_instance WHERE create_by = @seed_user);
SET @workflow_task_count := (
  SELECT COUNT(*)
  FROM wf_task t
  JOIN wf_process_instance i ON i.id = t.instance_id
  WHERE i.create_by = @seed_user
);
SET @approval_record_count := (
  SELECT COUNT(*)
  FROM oa_approval_record r
  JOIN wf_process_instance i
    ON i.business_type = r.business_type
   AND i.business_id = r.apply_id
  WHERE i.create_by = @seed_user
);

SELECT 'oa_business_trip' AS table_name, @business_trip_count AS inserted_rows
UNION ALL SELECT 'oa_outing', @outing_count
UNION ALL SELECT 'oa_overtime', @overtime_count
UNION ALL SELECT 'oa_purchase', @purchase_count
UNION ALL SELECT 'oa_expense', @expense_count
UNION ALL SELECT 'oa_loan', @loan_count
UNION ALL SELECT 'oa_schedule', @schedule_count
UNION ALL SELECT 'oa_message', @message_count
UNION ALL SELECT 'oa_notice', @notice_count
UNION ALL SELECT 'oa_meeting', @meeting_count
UNION ALL SELECT 'oa_document', @document_count
UNION ALL SELECT 'oa_asset_borrow', @asset_borrow_count
UNION ALL SELECT 'wf_process_instance', @workflow_instance_count
UNION ALL SELECT 'wf_task', @workflow_task_count
UNION ALL SELECT 'oa_approval_record', @approval_record_count
UNION ALL SELECT 'TOTAL',
  @business_trip_count + @outing_count + @overtime_count + @purchase_count + @expense_count + @loan_count
  + @schedule_count + @message_count + @notice_count + @meeting_count + @document_count + @asset_borrow_count
  + @workflow_instance_count + @workflow_task_count + @approval_record_count;
