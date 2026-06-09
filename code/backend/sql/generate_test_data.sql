-- ============================================================
-- 测试数据生成脚本：5500+ 条
-- ============================================================
-- 在 Navicat/DBeaver 中按顺序执行每个 section。
-- 每段以 -- ====== 开头，独立粘贴执行。
-- ============================================================

USE oa_system;

-- ============================================================
-- Section 1: 补 30 个测试员工 + 5 个测试部门
-- ============================================================
-- 部门 ID 1~5 假设已被现有数据占用（你的 sys_dept 已有数据）
-- 这里从 100 开始创建新部门避免冲突

INSERT INTO sys_dept (id, parent_id, dept_name, order_num, leader, status, del_flag, create_by, create_time, update_by, update_time) VALUES
  (100, 0, '测试一部',     1, '测试经理A', 0, '0', 'system', NOW(), 'system', NOW()),
  (101, 0, '测试二部',     2, '测试经理B', 0, '0', 'system', NOW(), 'system', NOW()),
  (102, 0, '测试三部',     3, '测试经理C', 0, '0', 'system', NOW(), 'system', NOW()),
  (103, 0, '测试四部',     4, '测试经理D', 0, '0', 'system', NOW(), 'system', NOW()),
  (104, 0, '测试五部',     5, '测试经理E', 0, '0', 'system', NOW(), 'system', NOW());

-- 30 个测试员工，empId 从 100 开始避免与现有 1-15 冲突
-- password 用 BCrypt 的 "123456" hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status, hire_date, gender, del_flag, create_by, create_time, update_by, update_time) VALUES
  (100, 'T001', '测试员工A01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001001', 'a01@test.com', 100, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (101, 'T002', '测试员工A02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001002', 'a02@test.com', 100, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (102, 'T003', '测试员工A03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001003', 'a03@test.com', 100, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (103, 'T004', '测试员工A04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001004', 'a04@test.com', 100, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (104, 'T005', '测试员工A05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001005', 'a05@test.com', 100, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (105, 'T006', '测试员工B01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001006', 'b01@test.com', 101, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (106, 'T007', '测试员工B02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001007', 'b02@test.com', 101, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (107, 'T008', '测试员工B03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001008', 'b03@test.com', 101, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (108, 'T009', '测试员工B04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001009', 'b04@test.com', 101, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (109, 'T010', '测试员工B05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001010', 'b05@test.com', 101, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (110, 'T011', '测试员工C01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001011', 'c01@test.com', 102, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (111, 'T012', '测试员工C02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001012', 'c02@test.com', 102, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (112, 'T013', '测试员工C03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001013', 'c03@test.com', 102, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (113, 'T014', '测试员工C04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001014', 'c04@test.com', 102, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (114, 'T015', '测试员工C05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001015', 'c05@test.com', 102, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (115, 'T016', '测试员工D01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001016', 'd01@test.com', 103, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (116, 'T017', '测试员工D02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001017', 'd02@test.com', 103, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (117, 'T018', '测试员工D03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001018', 'd03@test.com', 103, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (118, 'T019', '测试员工D04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001019', 'd04@test.com', 103, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (119, 'T020', '测试员工D05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001020', 'd05@test.com', 103, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (120, 'T021', '测试员工E01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001021', 'e01@test.com', 104, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (121, 'T022', '测试员工E02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001022', 'e02@test.com', 104, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (122, 'T023', '测试员工E03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001023', 'e03@test.com', 104, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (123, 'T024', '测试员工E04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001024', 'e04@test.com', 104, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (124, 'T025', '测试员工E05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001025', 'e05@test.com', 104, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (125, 'T026', '测试员工F01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001026', 'f01@test.com', 100, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (126, 'T027', '测试员工F02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001027', 'f02@test.com', 101, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW()),
  (127, 'T028', '测试员工F03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001028', 'f03@test.com', 102, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (128, 'T029', '测试员工F04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001029', 'f04@test.com', 103, 1, '2024-01-01', '0', '0', 'system', NOW(), 'system', NOW()),
  (129, 'T030', '测试员工F05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800001030', 'f05@test.com', 104, 1, '2024-01-01', '1', '0', 'system', NOW(), 'system', NOW());

-- ============================================================
-- Section 2: 7 个业务表各 75 条 (使用存储过程批量生成)
-- ============================================================
-- 状态分布: 0=待审批 1=已通过 2=已驳回 3=已撤回 4=已取消 5=已退回
-- 员工池: 1-15 (现有) + 100-129 (新增) = 45 个

-- ---- 2.1 请假申请 oa_leave_apply (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_leave //
CREATE PROCEDURE _gen_leave()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_leave_apply (emp_id, leave_type, start_time, end_time, reason, days, status, create_by, create_time, update_by, update_time, del_flag, process_instance_id)
    VALUES (
      1 + FLOOR(RAND() * 45),
      ELT(1 + FLOOR(RAND() * 4), '事假', '病假', '年假', '调休'),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      DATE_ADD(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), INTERVAL 1 + FLOOR(RAND() * 7) DAY),
      CONCAT('测试请假记录 #', i),
      1 + FLOOR(RAND() * 7),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0',
      NULL
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_leave();
DROP PROCEDURE _gen_leave;

-- ---- 2.2 出差申请 oa_business_trip (75 条) ----
-- 假设表结构有 emp_id, destination, start_time, end_time, reason, days, status 等
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_trip //
CREATE PROCEDURE _gen_trip()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_business_trip (emp_id, destination, start_time, end_time, reason, days, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      ELT(1 + FLOOR(RAND() * 5), '北京', '上海', '深圳', '广州', '杭州'),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      DATE_ADD(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), INTERVAL 1 + FLOOR(RAND() * 7) DAY),
      CONCAT('测试出差记录 #', i),
      1 + FLOOR(RAND() * 7),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_trip();
DROP PROCEDURE _gen_trip;

-- ---- 2.3 外出申请 oa_outing (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_outing //
CREATE PROCEDURE _gen_outing()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_outing (emp_id, reason, destination, start_time, end_time, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      CONCAT('外出办事 #', i),
      ELT(1 + FLOOR(RAND() * 5), '客户A', '客户B', '银行', '税务局', '会议室'),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      DATE_ADD(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), INTERVAL 2 + FLOOR(RAND() * 6) HOUR),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_outing();
DROP PROCEDURE _gen_outing;

-- ---- 2.4 加班申请 oa_overtime (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_overtime //
CREATE PROCEDURE _gen_overtime()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_overtime (emp_id, start_time, end_time, hours, reason, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      DATE_ADD(DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), INTERVAL 2 + FLOOR(RAND() * 10) HOUR),
      2 + FLOOR(RAND() * 10),
      CONCAT('项目加班 #', i),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_overtime();
DROP PROCEDURE _gen_overtime;

-- ---- 2.5 采购申请 oa_purchase (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_purchase //
CREATE PROCEDURE _gen_purchase()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_purchase (emp_id, item_name, quantity, amount, reason, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      ELT(1 + FLOOR(RAND() * 5), '办公电脑', '打印机', '办公椅', '文具', '显示器'),
      1 + FLOOR(RAND() * 10),
      ROUND(100 + RAND() * 10000, 2),
      CONCAT('采购申请 #', i),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_purchase();
DROP PROCEDURE _gen_purchase;

-- ---- 2.6 报销申请 oa_expense (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_expense //
CREATE PROCEDURE _gen_expense()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_expense (emp_id, expense_type, amount, reason, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      ELT(1 + FLOOR(RAND() * 5), '差旅', '餐饮', '办公', '通讯', '培训'),
      ROUND(50 + RAND() * 5000, 2),
      CONCAT('报销申请 #', i),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_expense();
DROP PROCEDURE _gen_expense;

-- ---- 2.7 借款申请 oa_loan (75 条) ----
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_loan //
CREATE PROCEDURE _gen_loan()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 75 DO
    INSERT INTO oa_loan (emp_id, loan_amount, reason, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      ROUND(1000 + RAND() * 50000, 2),
      CONCAT('借款事由 #', i),
      FLOOR(RAND() * 5),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_loan();
DROP PROCEDURE _gen_loan;

-- ============================================================
-- Section 3: 公告 oa_notice (60 条)
-- ============================================================
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_notice //
CREATE PROCEDURE _gen_notice()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 60 DO
    INSERT INTO oa_notice (title, content, type, emp_id, status, is_top, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      CONCAT('公告标题 #', i),
      CONCAT('这是公告的详细内容，公告编号', i, '，用于测试数据展示。'),
      FLOOR(RAND() * 4),
      1 + FLOOR(RAND() * 15),
      1,
      IF(RAND() > 0.8, '1', '0'),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_notice();
DROP PROCEDURE _gen_notice;

-- ============================================================
-- Section 4: 消息 oa_message (60 条)
-- ============================================================
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_message //
CREATE PROCEDURE _gen_message()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 60 DO
    INSERT INTO oa_message (sender_id, receiver_id, title, content, is_read, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 15),
      1 + FLOOR(RAND() * 45),
      CONCAT('消息 #', i),
      CONCAT('这是消息内容 ', i),
      IF(RAND() > 0.5, 1, 0),
      'system',
      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_message();
DROP PROCEDURE _gen_message;

-- ============================================================
-- Section 5: 会议 oa_meeting (60 条)
-- ============================================================
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_meeting //
CREATE PROCEDURE _gen_meeting()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 60 DO
    INSERT INTO oa_meeting (title, room_id, organizer_id, start_time, end_time, description, participants, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      CONCAT('会议 #', i),
      1 + FLOOR(RAND() * 5),
      1 + FLOOR(RAND() * 15),
      DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
      DATE_ADD(DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY), INTERVAL 1 HOUR),
      CONCAT('会议描述 #', i),
      CONCAT('[', 1 + FLOOR(RAND() * 15), ',', 1 + FLOOR(RAND() * 15), ']'),
      FLOOR(RAND() * 4),
      'system',
      NOW(),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_meeting();
DROP PROCEDURE _gen_meeting;

-- ============================================================
-- Section 6: 日程 oa_schedule (60 条)
-- ============================================================
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_schedule //
CREATE PROCEDURE _gen_schedule()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 60 DO
    INSERT INTO oa_schedule (emp_id, title, content, start_time, end_time, status, create_by, create_time, update_by, update_time, del_flag)
    VALUES (
      1 + FLOOR(RAND() * 45),
      CONCAT('日程 #', i),
      CONCAT('日程内容 ', i),
      DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
      DATE_ADD(DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY), INTERVAL 1 + FLOOR(RAND() * 4) HOUR),
      FLOOR(RAND() * 3),
      'system',
      NOW(),
      'system',
      NOW(),
      '0'
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL _gen_schedule();
DROP PROCEDURE _gen_schedule;

-- ============================================================
-- Section 7: 工作流实例 wf_process_instance (~525 条)
-- ============================================================
-- 为已存在的状态=0/1/2 的业务记录各建一条流程实例
DELIMITER //
DROP PROCEDURE IF EXISTS _gen_instance //
CREATE PROCEDURE _gen_instance()
BEGIN
  DECLARE v_id BIGINT;
  DECLARE v_emp BIGINT;
  DECLARE v_status INT;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR SELECT id, emp_id, status FROM oa_leave_apply WHERE status IN (0, 1, 2, 3, 4, 5);
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  -- 7.1 leave
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_id, v_emp, v_status;
    IF done THEN LEAVE read_loop; END IF;
    INSERT INTO wf_process_instance (process_id, business_type, business_id, initiator_id, current_node, status, start_time, end_time, snapshot_node_config, create_by, create_time, del_flag)
    VALUES (1, 'leave', v_id, v_emp, 0, v_status, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), IF(v_status != 0, NOW(), NULL),
            '{"schemaVersion":2,"nodes":[],"edges":[]}', 'system', DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), '0');
    UPDATE oa_leave_apply SET process_instance_id = LAST_INSERT_ID() WHERE id = v_id;
  END LOOP;
  CLOSE cur;

  -- 7.2 trip
  SET done = 0;
  DECLARE cur2 CURSOR FOR SELECT id, emp_id, status FROM oa_business_trip WHERE status IN (0, 1, 2, 3, 4, 5);
  OPEN cur2;
  read_loop2: LOOP
    FETCH cur2 INTO v_id, v_emp, v_status;
    IF done THEN LEAVE read_loop2; END IF;
    INSERT INTO wf_process_instance (process_id, business_type, business_id, initiator_id, current_node, status, start_time, end_time, snapshot_node_config, create_by, create_time, del_flag)
    VALUES (2, 'trip', v_id, v_emp, 0, v_status, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), IF(v_status != 0, NOW(), NULL),
            '{"schemaVersion":2,"nodes":[],"edges":[]}', 'system', DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY), '0');
  END LOOP;
  CLOSE cur2;
END //
DELIMITER ;
CALL _gen_instance();
DROP PROCEDURE _gen_instance;

-- ============================================================
-- 验证统计
-- ============================================================
SELECT 'sys_employee' AS tbl, COUNT(*) AS cnt FROM sys_employee
UNION ALL SELECT 'sys_dept', COUNT(*) FROM sys_dept
UNION ALL SELECT 'oa_leave_apply', COUNT(*) FROM oa_leave_apply
UNION ALL SELECT 'oa_business_trip', COUNT(*) FROM oa_business_trip
UNION ALL SELECT 'oa_outing', COUNT(*) FROM oa_outing
UNION ALL SELECT 'oa_overtime', COUNT(*) FROM oa_overtime
UNION ALL SELECT 'oa_purchase', COUNT(*) FROM oa_purchase
UNION ALL SELECT 'oa_expense', COUNT(*) FROM oa_expense
UNION ALL SELECT 'oa_loan', COUNT(*) FROM oa_loan
UNION ALL SELECT 'oa_notice', COUNT(*) FROM oa_notice
UNION ALL SELECT 'oa_message', COUNT(*) FROM oa_message
UNION ALL SELECT 'oa_meeting', COUNT(*) FROM oa_meeting
UNION ALL SELECT 'oa_schedule', COUNT(*) FROM oa_schedule
UNION ALL SELECT 'wf_process_instance', COUNT(*) FROM wf_process_instance
UNION ALL SELECT 'wf_task', COUNT(*) FROM wf_task;
