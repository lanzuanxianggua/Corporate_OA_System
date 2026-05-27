-- Test data for OA system new modules
USE oa_system;

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status) VALUES
(1, 'leave_type', 'leave_type', '0'),
(2, 'approval_status', 'approval_status', '0'),
(3, 'asset_category', 'asset_category', '0'),
(4, 'contract_type', 'contract_type', '0'),
(5, 'education', 'education', '0');

INSERT INTO sys_dict_data (data_id, dict_type, dict_label, dict_value, dict_sort, status) VALUES
(1, 'leave_type', '事假', '1', 1, '0'),
(2, 'leave_type', '病假', '2', 2, '0'),
(3, 'leave_type', '年假', '3', 3, '0'),
(4, 'leave_type', '婚假', '4', 4, '0'),
(5, 'leave_type', '产假', '5', 5, '0'),
(6, 'approval_status', '待审批', '0', 1, '0'),
(7, 'approval_status', '已通过', '1', 2, '0'),
(8, 'approval_status', '已驳回', '2', 3, '0'),
(9, 'asset_category', '电子设备', 'electronics', 1, '0'),
(10, 'asset_category', '办公家具', 'furniture', 2, '0'),
(11, 'asset_category', '交通工具', 'vehicle', 3, '0'),
(12, 'contract_type', '采购合同', 'purchase', 1, '0'),
(13, 'contract_type', '销售合同', 'sales', 2, '0'),
(14, 'contract_type', '劳动合同', 'labor', 3, '0'),
(15, 'education', '高中', 'high_school', 1, '0'),
(16, 'education', '大专', 'college', 2, '0'),
(17, 'education', '本科', 'bachelor', 3, '0'),
(18, 'education', '硕士', 'master', 4, '0'),
(19, 'education', '博士', 'doctor', 5, '0');

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, remark) VALUES
(1, '考勤上班时间', 'attendance.work.start', '09:00', '0', '默认上班打卡时间'),
(2, '考勤下班时间', 'attendance.work.end', '18:00', '0', '默认下班打卡时间'),
(3, '迟到阈值(分钟)', 'attendance.late.threshold', '15', '0', '超过上班时间多少分钟算迟到'),
(4, '文件上传大小限制(MB)', 'upload.max.size', '50', '0', '文件上传最大MB'),
(5, '密码最小长度', 'password.min.length', '8', '0', '密码最小长度');

INSERT INTO sys_post (post_id, post_code, post_name, post_sort, status) VALUES
(1, 'CEO', '首席执行官', 1, '0'),
(2, 'CTO', '首席技术官', 2, '0'),
(3, 'CFO', '首席财务官', 3, '0'),
(4, 'HR', '人力资源经理', 4, '0'),
(5, 'DEV', '开发工程师', 5, '0'),
(6, 'QA', '测试工程师', 6, '0'),
(7, 'PM', '项目经理', 7, '0');

UPDATE sys_employee SET post_id = 1 WHERE id = 1;
UPDATE sys_employee SET post_id = 4 WHERE id = 2;
UPDATE sys_employee SET post_id = 5 WHERE id IN (3, 4, 5, 6, 7);

INSERT INTO oa_attendance_group (id, group_name, work_start, work_end, late_threshold, status) VALUES
(1, '默认考勤组', '09:00:00', '18:00:00', 15, '0'),
(2, '弹性考勤组', '10:00:00', '19:00:00', 30, '0');

INSERT INTO oa_attendance_group_emp (id, group_id, emp_id) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5),
(6, 1, 6), (7, 1, 7), (8, 2, 8);

INSERT INTO oa_leave_balance (id, emp_id, leave_type, year, total_days, used_days, remaining_days) VALUES
(1, 1, 1, 2026, 10.0, 0.0, 10.0),
(2, 1, 2, 2026, 15.0, 0.0, 15.0),
(3, 1, 3, 2026, 5.0, 0.0, 5.0),
(4, 3, 1, 2026, 10.0, 2.0, 8.0),
(5, 3, 2, 2026, 15.0, 1.0, 14.0),
(6, 3, 3, 2026, 5.0, 1.0, 4.0),
(7, 4, 1, 2026, 10.0, 3.0, 7.0),
(8, 4, 3, 2026, 5.0, 0.0, 5.0);

INSERT INTO oa_meeting_room (id, room_name, location, capacity, equipment, status) VALUES
(1, 'Meeting Room A', '3F-301', 10, 'projector,whiteboard', '0'),
(2, 'Meeting Room B', '3F-302', 20, 'projector,video', '0'),
(3, 'Meeting Room C', '5F-501', 6, 'whiteboard,tv', '0'),
(4, 'Grand Meeting', '1F', 50, 'projector,audio,video', '0');

INSERT INTO wf_process_definition (id, process_name, process_key, process_type, node_config, status, version) VALUES
(1, 'Leave Process', 'leave_process', 'leave', '[{"nodeIndex":0,"nodeName":"Manager","assigneeType":"admin"}]', '0', 1),
(2, 'Trip Process', 'trip_process', 'trip', '[{"nodeIndex":0,"nodeName":"Manager","assigneeType":"admin"}]', '0', 1),
(3, 'Outing Process', 'outing_process', 'outing', '[{"nodeIndex":0,"nodeName":"Manager","assigneeType":"admin"}]', '0', 1),
(4, 'Purchase Process', 'purchase_process', 'purchase', '[{"nodeIndex":0,"nodeName":"Manager","assigneeType":"admin"}]', '0', 1),
(5, 'Expense Process', 'expense_process', 'expense', '[{"nodeIndex":0,"nodeName":"Manager","assigneeType":"admin"}]', '0', 1);

INSERT INTO sys_menu (menu_id, parent_id, menu_name, path, component, perms, menu_type, icon, order_num, status) VALUES
(1, 0, 'System', '/system', NULL, NULL, 'M', 'setting', 1, '0'),
(2, 0, 'OA Office', '/oa', NULL, NULL, 'M', 'office-building', 2, '0'),
(3, 0, 'OA Admin', '/oa-manage', NULL, NULL, 'M', 'management', 3, '0');

INSERT INTO oa_asset (id, asset_code, asset_name, category, specification, purchase_date, purchase_price, status, current_user_id, dept_id) VALUES
(1, 'IT-2024-001', 'MacBook Pro 16', 'electronics', 'M3 Max 36GB 1TB', '2024-01-15', 24999.00, '1', 3, 1),
(2, 'IT-2024-002', 'Dell Monitor', 'electronics', '27inch 4K', '2024-02-01', 3599.00, '1', 4, 1),
(3, 'IT-2024-003', 'ThinkPad X1', 'electronics', 'i7 16GB 512GB', '2024-03-10', 12999.00, '0', NULL, 2),
(4, 'OF-2024-001', 'Ergonomic Chair', 'furniture', 'Herman Miller', '2024-01-20', 12999.00, '1', 5, 1),
(5, 'OF-2024-002', 'Standing Desk', 'furniture', '1.4m white', '2024-02-15', 2999.00, '0', NULL, 2);

INSERT INTO oa_contract (id, contract_no, contract_name, contract_type, party_a, party_b, amount, sign_date, start_date, end_date, status, manager_id) VALUES
(1, 'HT-2026-001', 'Equipment Purchase', 'purchase', 'Company', 'Lenovo', 150000.00, '2026-01-10', '2026-01-15', '2026-12-31', '1', 1),
(2, 'HT-2026-002', 'Cloud Service', 'purchase', 'Company', 'Alibaba Cloud', 80000.00, '2026-02-01', '2026-02-01', '2027-01-31', '1', 1),
(3, 'HT-2026-003', 'Labor-zhangsan', 'labor', 'Company', 'Zhang San', NULL, '2025-06-01', '2025-06-01', '2026-05-31', '1', 2);

INSERT INTO oa_budget (id, dept_id, budget_year, budget_month, amount, used_amount, status) VALUES
(1, 1, 2026, 5, 50000.00, 12000.00, '0'),
(2, 2, 2026, 5, 30000.00, 8000.00, '0'),
(3, 1, 2026, 6, 50000.00, 0.00, '0');

INSERT INTO rpt_alert_rule (id, rule_name, rule_type, metric, condition_type, threshold, check_cron, notify_type, notify_targets, status) VALUES
(1, 'Resign Rate Alert', 'hr', 'resign_rate', 'gt', 5.00, '0 0 1 1 *', 'inner', '1', '0'),
(2, 'Attendance Alert', 'attendance', 'abnormal_rate', 'gt', 10.00, '0 0 2 * *', 'inner', '1,2', '0'),
(3, 'Budget Alert', 'finance', 'budget_usage', 'gt', 80.00, '0 0 3 * *', 'inner', '1', '0');
