-- =============================================
-- OA系统 数据一致性修复脚本
-- 修复部门负责人、角色分配、流程定义、请假余额等数据不一致问题
-- 执行顺序: 在 init.sql 和 init_dashboard_data.sql 之后执行
-- =============================================

USE oa_system;

-- =============================================
-- 1. 修复部门负责人 (sys_dept.leader)
-- resolveDeptManager() 先查 dept.leader 字段匹配 DEPT_MANAGER 角色员工
-- =============================================

-- 总公司 (id=1): 管理员(管理员) 担任 GM 角色
UPDATE sys_dept SET leader = '1' WHERE id = 1;

-- 技术部 (id=2): 张三(id=2) 后端组组长 -> 部门负责人应由 dept_manager 角色的人担任
-- 张三是 TEAM_LEAD(role_id=4), 不是 DEPT_MANAGER。技术部没有 DEPT_MANAGER。
-- 设定管理员(id=1, GM) 兼管技术部
UPDATE sys_dept SET leader = '1', phone = '13800000000' WHERE id = 2;

-- 市场部 (id=3): 李四(id=3) 是 DEPT_MANAGER
UPDATE sys_dept SET leader = '3', phone = '13800000002' WHERE id = 3;

-- 财务部 (id=4): 王五(id=4) 是 DIRECTOR -> 管理财务部
UPDATE sys_dept SET leader = '4', phone = '13800000003' WHERE id = 4;

-- 人事部 (id=5): 赵六(id=5) 是 DEPT_MANAGER
UPDATE sys_dept SET leader = '5', phone = '13800000004' WHERE id = 5;

-- 前端组 (id=6): 陈十一(id=11)
UPDATE sys_dept SET leader = '11', phone = '13800000010' WHERE id = 6;

-- 后端组 (id=7): 张三(id=2) 后端组长
UPDATE sys_dept SET leader = '2', phone = '13800000001' WHERE id = 7;

-- 销售组 (id=8): 杨十六(id=15)
UPDATE sys_dept SET leader = '15', phone = '13800000015' WHERE id = 8;

-- 运营组 (id=9): 何十七(id=16)
UPDATE sys_dept SET leader = '16', phone = '13800000016' WHERE id = 9;

-- 审计组 (id=10): 高二十(id=19)
UPDATE sys_dept SET leader = '19', phone = '13800000019' WHERE id = 10;

-- =============================================
-- 2. 确保工作流审批角色存在
-- =============================================

INSERT IGNORE INTO sys_role (id, role_name, role_key, sort, status) VALUES
(3, '部门主管', 'DEPT_MANAGER', 2, 1),
(4, '组长', 'TEAM_LEAD', 3, 1),
(5, '总监', 'DIRECTOR', 4, 1),
(6, '总经理', 'GM', 5, 1);

-- =============================================
-- 3. 修复角色分配（在 init_dashboard_data 之后重新设置审批角色）
-- 保持 emp 1-6 已有的基础 USER 角色不变，追加审批角色
-- =============================================

-- 清除旧的审批角色 (role_id > 2)
DELETE FROM sys_emp_role WHERE role_id > 2;

-- 基础用户角色 (所有员工都有 USER 角色)
INSERT IGNORE INTO sys_emp_role (emp_id, role_id) VALUES
(1, 1),   -- admin = ADMIN
(1, 2),   -- admin also USER
(2, 2), (3, 2), (4, 2), (5, 2), (6, 2),
(7, 2), (8, 2), (9, 2), (10, 2),
(11, 2), (12, 2), (13, 2), (14, 2),
(15, 2), (16, 2), (17, 2), (18, 2),
(19, 2), (20, 2);

-- 审批角色分配
-- emp1=admin -> GM (总经理)
-- emp2=zhangsan(技术部-后端组) -> TEAM_LEAD
-- emp3=lisi(市场部) -> DEPT_MANAGER
-- emp4=wangwu(财务部) -> DIRECTOR
-- emp5=zhaoliu(人事部) -> DEPT_MANAGER
-- emp6=sunqi(技术部) -> TEAM_LEAD
-- emp11=chensy(前端组) -> TEAM_LEAD
-- emp15=yangsl(销售组) -> TEAM_LEAD
INSERT IGNORE INTO sys_emp_role (emp_id, role_id) VALUES
(1, 6),   -- admin = GM
(2, 4),   -- zhangsan = TEAM_LEAD
(3, 3),   -- lisi = DEPT_MANAGER
(4, 5),   -- wangwu = DIRECTOR
(5, 3),   -- zhaoliu = DEPT_MANAGER
(6, 4),   -- sunqi = TEAM_LEAD
(11, 4),  -- chensy = TEAM_LEAD (前端组长)
(15, 4);  -- yangsl = TEAM_LEAD (销售组长)

-- =============================================
-- 4. 修复工作流流程定义 - 全部7种业务类型
-- nodeIndex 从 1 开始, 使用正确的 assigneeType/assigneeValue
-- =============================================

-- 先清除旧定义
DELETE FROM wf_process_definition WHERE id <= 7;

-- 4.1 请假审批流程 (leave)
-- 节点1: 部门主管审批 -> 节点2: 如果请假>3天则需要总监审批
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(1, '请假审批流程', 'leave_approval', 'leave',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"总监审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"DIRECTOR","conditions":[{"field":"days","operator":">","value":3}]}]',
 '0', 1);

-- 4.2 出差审批流程 (trip)
-- 节点1: 部门主管审批 -> 节点2: 总经理审批(金额>5000)
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(2, '出差审批流程', 'trip_approval', 'trip',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"GM","conditions":[{"field":"amount","operator":">","value":5000}]}]',
 '0', 1);

-- 4.3 外出审批流程 (outing)
-- 节点1: 部门主管审批
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(3, '外出审批流程', 'outing_approval', 'outing',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"}]',
 '0', 1);

-- 4.4 采购审批流程 (purchase)
-- 节点1: 部门主管审批 -> 节点2: 总监审批(金额>10000) -> 节点3: 总经理审批(金额>50000)
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(4, '采购审批流程', 'purchase_approval', 'purchase',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"总监审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"DIRECTOR","conditions":[{"field":"amount","operator":">","value":10000}]},{"nodeIndex":3,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"GM","conditions":[{"field":"amount","operator":">","value":50000}]}]',
 '0', 1);

-- 4.5 报销审批流程 (expense)
-- 节点1: 部门主管审批 -> 节点2: 财务总监审批(金额>5000)
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(5, '报销审批流程', 'expense_approval', 'expense',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"财务总监审批","nodeType":"approval","assigneeType":"specific","assigneeValue":"4","conditions":[{"field":"amount","operator":">","value":5000}]}]',
 '0', 1);

-- 4.6 加班审批流程 (overtime) -- 之前缺失
-- 节点1: 组长审批 -> 节点2: 部门主管审批(加班>4小时)
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(6, '加班审批流程', 'overtime_approval', 'overtime',
 '[{"nodeIndex":1,"nodeName":"组长审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"总监审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"DIRECTOR","conditions":[{"field":"hours","operator":">","value":4}]}]',
 '0', 1);

-- 4.7 借款审批流程 (loan) -- 之前缺失
-- 节点1: 部门主管审批 -> 节点2: 财务总监审批 -> 节点3: 总经理审批(金额>100000)
INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(7, '借款审批流程', 'loan_approval', 'loan',
 '[{"nodeIndex":1,"nodeName":"部门主管审批","nodeType":"approval","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":2,"nodeName":"财务总监审批","nodeType":"approval","assigneeType":"specific","assigneeValue":"4"},{"nodeIndex":3,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role_global","assigneeValue":"GM","conditions":[{"field":"amount","operator":">","value":100000}]}]',
 '0', 1);

-- =============================================
-- 5. 补全请假余额 (oa_leave_balance)
-- 所有员工都应有事假(1)、病假(2)、年假(3) 的年度余额
-- =============================================

-- 清除旧余额重新初始化
DELETE FROM oa_leave_balance WHERE year = 2026;

-- 所有20名员工的事假(1)、病假(2)、年假(3)余额
INSERT INTO oa_leave_balance (id, emp_id, leave_type, year, total_days, used_days, remaining_days) VALUES
-- emp1 管理员
(1,  1, 1, 2026, 10.0, 0.0, 10.0),
(2,  1, 2, 2026, 15.0, 0.0, 15.0),
(3,  1, 3, 2026, 10.0, 0.0, 10.0),
-- emp2 张三 (used 1天事假)
(4,  2, 1, 2026, 10.0, 1.0, 9.0),
(5,  2, 2, 2026, 15.0, 0.0, 15.0),
(6,  2, 3, 2026, 5.0, 0.0, 5.0),
-- emp3 李四 (used 1天事假)
(7,  3, 1, 2026, 10.0, 1.0, 9.0),
(8,  3, 2, 2026, 15.0, 0.0, 15.0),
(9,  3, 3, 2026, 5.0, 1.0, 4.0),
-- emp4 王五 (used 3天事假)
(10, 4, 1, 2026, 10.0, 3.0, 7.0),
(11, 4, 2, 2026, 15.0, 0.0, 15.0),
(12, 4, 3, 2026, 5.0, 0.0, 5.0),
-- emp5 赵六 (used 2天年假)
(13, 5, 1, 2026, 10.0, 0.0, 10.0),
(14, 5, 2, 2026, 15.0, 0.0, 15.0),
(15, 5, 3, 2026, 5.0, 2.0, 3.0),
-- emp6 孙七
(16, 6, 1, 2026, 10.0, 0.0, 10.0),
(17, 6, 2, 2026, 15.0, 0.0, 15.0),
(18, 6, 3, 2026, 5.0, 0.0, 5.0),
-- emp7 吴九
(19, 7, 1, 2026, 10.0, 0.0, 10.0),
(20, 7, 2, 2026, 15.0, 0.0, 15.0),
(21, 7, 3, 2026, 5.0, 0.0, 5.0),
-- emp8 周八
(22, 8, 1, 2026, 10.0, 0.0, 10.0),
(23, 8, 2, 2026, 15.0, 0.0, 15.0),
(24, 8, 3, 2026, 5.0, 0.0, 5.0),
-- emp9 郑十
(25, 9, 1, 2026, 10.0, 0.0, 10.0),
(26, 9, 2, 2026, 15.0, 0.0, 15.0),
(27, 9, 3, 2026, 5.0, 0.0, 5.0),
-- emp10 王十
(28, 10, 1, 2026, 10.0, 1.0, 9.0),
(29, 10, 2, 2026, 15.0, 0.0, 15.0),
(30, 10, 3, 2026, 5.0, 0.0, 5.0),
-- emp11 陈十一
(31, 11, 1, 2026, 10.0, 0.0, 10.0),
(32, 11, 2, 2026, 15.0, 0.0, 15.0),
(33, 11, 3, 2026, 5.0, 0.0, 5.0),
-- emp12 刘十二
(34, 12, 1, 2026, 10.0, 1.0, 9.0),
(35, 12, 2, 2026, 15.0, 0.0, 15.0),
(36, 12, 3, 2026, 5.0, 0.0, 5.0),
-- emp13 黄十三
(37, 13, 1, 2026, 10.0, 0.0, 10.0),
(38, 13, 2, 2026, 15.0, 0.0, 15.0),
(39, 13, 3, 2026, 5.0, 0.0, 5.0),
-- emp14 林十四
(40, 14, 1, 2026, 10.0, 0.0, 10.0),
(41, 14, 2, 2026, 15.0, 0.0, 15.0),
(42, 14, 3, 2026, 5.0, 0.0, 5.0),
-- emp15 杨十六
(43, 15, 1, 2026, 10.0, 0.0, 10.0),
(44, 15, 2, 2026, 15.0, 0.0, 15.0),
(45, 15, 3, 2026, 5.0, 0.0, 5.0),
-- emp16 何十七
(46, 16, 1, 2026, 10.0, 0.0, 10.0),
(47, 16, 2, 2026, 15.0, 1.0, 14.0),
(48, 16, 3, 2026, 5.0, 0.0, 5.0),
-- emp17 徐十八
(49, 17, 1, 2026, 10.0, 0.0, 10.0),
(50, 17, 2, 2026, 15.0, 0.0, 15.0),
(51, 17, 3, 2026, 5.0, 0.0, 5.0),
-- emp18 马十九
(52, 18, 1, 2026, 10.0, 0.0, 10.0),
(53, 18, 2, 2026, 15.0, 0.0, 15.0),
(54, 18, 3, 2026, 5.0, 1.0, 4.0),
-- emp19 高二十
(55, 19, 1, 2026, 10.0, 0.0, 10.0),
(56, 19, 2, 2026, 15.0, 1.0, 14.0),
(57, 19, 3, 2026, 5.0, 0.0, 5.0),
-- emp20 宋二一
(58, 20, 1, 2026, 10.0, 0.0, 10.0),
(59, 20, 2, 2026, 15.0, 0.0, 15.0),
(60, 20, 3, 2026, 5.0, 2.0, 3.0);

-- =============================================
-- 6. 补全考勤组员工关联
-- init_dashboard_data 新增的员工 (id 7-20) 没有分配考勤组
-- =============================================

DELETE FROM oa_attendance_group_emp;
INSERT INTO oa_attendance_group_emp (id, group_id, emp_id) VALUES
(1,  1, 1), (2,  1, 2), (3,  1, 3), (4,  1, 4), (5,  1, 5),
(6,  1, 6), (7,  1, 7), (8,  1, 8), (9,  1, 9), (10, 1, 10),
(11, 1, 11), (12, 1, 12), (13, 1, 13), (14, 1, 14), (15, 1, 15),
(16, 1, 16), (17, 1, 17), (18, 1, 18), (19, 1, 19), (20, 1, 20);

-- =============================================
-- 7. 确保所有员工都有部门 (dept_id 不为 NULL)
-- =============================================

-- 检查并修复: emp7-10 在 init_dashboard_data 中已设置 dept_id, 无需修复
-- emp11 前端组(dept=6), emp12 前端组(dept=6), emp13 后端组(dept=7), emp14 后端组(dept=7)
-- emp15 销售组(dept=8), emp16 运营组(dept=9), emp17 人事部(dept=5), emp18 人事部(dept=5)
-- emp19 审计组(dept=10), emp20 审计组(dept=10)
-- 全部已在 init_dashboard_data.sql 中正确设置, 无需修复

-- =============================================
-- 完成
-- =============================================
