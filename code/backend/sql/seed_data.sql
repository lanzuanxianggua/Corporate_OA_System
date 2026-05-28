-- ============================================================================
-- 企业OA办公系统 — 种子数据
-- Database: oa_system | Charset: utf8mb4
-- ============================================================================
-- 密码说明:
--   admin123 -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
--   123456   -> $2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Kz2aGMhJvpMFST9MhTWxm
-- ============================================================================

USE `oa_system`;

-- ============================================================================
-- 一、系统基础数据
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 部门
-- ---------------------------------------------------------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `order_num`, `leader`, `status`, `create_by`) VALUES
(1, 0, '总经办',   1, '李四', '0', 1),
(2, 0, '技术部',   2, '李四', '0', 1),
(3, 0, '行政部',   3, '李四', '0', 1);

-- ---------------------------------------------------------------------------
-- 1.2 岗位
-- ---------------------------------------------------------------------------
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`) VALUES
(1, 'ceo',      '总经理',   1, '0', 1),
(2, 'dev_mgr',  '技术经理', 2, '0', 1),
(3, 'dev',      '开发工程师', 3, '0', 1),
(4, 'admin',    '行政专员', 4, '0', 1);

-- ---------------------------------------------------------------------------
-- 1.3 员工
-- ---------------------------------------------------------------------------
INSERT INTO `sys_employee` (`id`, `emp_no`, `name`, `password`, `phone`, `email`, `avatar`, `dept_id`, `post_id`, `status`, `hire_date`, `gender`, `create_by`) VALUES
(1, 'admin',    '管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000001', 'admin@oa.com',    NULL, 1, 1, '0', '2020-01-01', '0', 1),
(2, 'zhangsan', '张三',   '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Kz2aGMhJvpMFST9MhTWxm', '13800000002', 'zhangsan@oa.com', NULL, 2, 3, '0', '2023-03-15', '0', 1),
(3, 'lisi',     '李四',   '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Kz2aGMhJvpMFST9MhTWxm', '13800000003', 'lisi@oa.com',     NULL, 2, 2, '0', '2022-06-01', '0', 1);

-- ---------------------------------------------------------------------------
-- 1.4 角色
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `status`, `create_by`) VALUES
(1, '管理员', 'ADMIN', 1, '0', 1),
(2, '普通用户', 'USER', 2, '0', 1);

-- ---------------------------------------------------------------------------
-- 1.5 员工-角色
-- ---------------------------------------------------------------------------
INSERT INTO `sys_emp_role` (`emp_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 2);

-- ---------------------------------------------------------------------------
-- 1.6 菜单 (顶层目录 + 各业务模块)
-- ---------------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `path`, `component`, `perms`, `menu_type`, `icon`, `order_num`, `status`, `create_by`) VALUES
-- 一级目录
(1,   0,  '工作台',     '/dashboard',     'views/dashboard/index',           NULL,                 'C', 'home',      1,  '0', 1),
(10,  0,  'OA办公',     '/oa',            NULL,                               NULL,                 'M', 'clipboard', 2,  '0', 1),
(20,  0,  '考勤管理',   '/attendance',    NULL,                               NULL,                 'M', 'date',      3,  '0', 1),
(30,  0,  '流程中心',   '/workflow',      NULL,                               NULL,                 'M', 'guide',      4,  '0', 1),
(40,  0,  '系统管理',   '/system',        NULL,                               NULL,                 'M', 'setting',    5,  '0', 1),
(50,  0,  '监控日志',   '/monitor',       NULL,                               NULL,                 'M', 'monitor',    6,  '0', 1),

-- OA办公子菜单
(1001, 10, '请假申请', '/oa/leave',       'views/oa/leave/index',             'oa:leave:list',      'C', NULL, 1, '0', 1),
(1002, 10, '出差申请', '/oa/trip',        'views/oa/trip/index',              'oa:trip:list',       'C', NULL, 2, '0', 1),
(1003, 10, '外出申请', '/oa/outing',      'views/oa/outing/index',            'oa:outing:list',     'C', NULL, 3, '0', 1),
(1004, 10, '加班申请', '/oa/overtime',    'views/oa/overtime/index',          'oa:overtime:list',   'C', NULL, 4, '0', 1),
(1005, 10, '采购申请', '/oa/purchase',    'views/oa/purchase/index',          'oa:purchase:list',   'C', NULL, 5, '0', 1),
(1006, 10, '报销申请', '/oa/expense',     'views/oa/expense/index',           'oa:expense:list',    'C', NULL, 6, '0', 1),
(1007, 10, '借款申请', '/oa/loan',        'views/oa/loan/index',              'oa:loan:list',       'C', NULL, 7, '0', 1),
(1008, 10, '公告管理', '/oa/notice',      'views/oa/notice/index',            'oa:notice:list',     'C', NULL, 8, '0', 1),
(1009, 10, '公文管理', '/oa/document',    'views/oa/document/index',          'oa:document:list',   'C', NULL, 9, '0', 1),
(1010, 10, '日程管理', '/oa/schedule',    'views/oa/schedule/index',          'oa:schedule:list',   'C', NULL, 10, '0', 1),
(1011, 10, '合同管理', '/oa/contract',    'views/oa/contract/index',          'oa:contract:list',   'C', NULL, 11, '0', 1),
(1012, 10, '会议管理', '/oa/meeting',     'views/oa/meeting/index',           'oa:meeting:list',    'C', NULL, 12, '0', 1),

-- 考勤管理子菜单
(2001, 20, '考勤记录', '/attendance/record',  'views/attendance/record/index',  'attendance:record:list',  'C', NULL, 1, '0', 1),
(2002, 20, '考勤组',   '/attendance/group',   'views/attendance/group/index',   'attendance:group:list',   'C', NULL, 2, '0', 1),

-- 流程中心子菜单
(3001, 30, '流程定义', '/workflow/definition', 'views/workflow/definition/index', 'workflow:def:list',   'C', NULL, 1, '0', 1),
(3002, 30, '待办任务', '/workflow/todo',       'views/workflow/todo/index',       'workflow:todo:list',  'C', NULL, 2, '0', 1),
(3003, 30, '已办任务', '/workflow/done',       'views/workflow/done/index',       'workflow:done:list',  'C', NULL, 3, '0', 1),
(3004, 30, '我的申请', '/workflow/mine',       'views/workflow/mine/index',       'workflow:mine:list',  'C', NULL, 4, '0', 1),

-- 系统管理子菜单
(4001, 40, '员工管理', '/system/employee', 'views/system/employee/index',  'system:emp:list',   'C', NULL, 1, '0', 1),
(4002, 40, '部门管理', '/system/dept',     'views/system/dept/index',      'system:dept:list',  'C', NULL, 2, '0', 1),
(4003, 40, '角色管理', '/system/role',     'views/system/role/index',      'system:role:list',  'C', NULL, 3, '0', 1),
(4004, 40, '菜单管理', '/system/menu',     'views/system/menu/index',      'system:menu:list',  'C', NULL, 4, '0', 1),
(4005, 40, '岗位管理', '/system/post',     'views/system/post/index',      'system:post:list',  'C', NULL, 5, '0', 1),
(4006, 40, '字典管理', '/system/dict',     'views/system/dict/index',      'system:dict:list',  'C', NULL, 6, '0', 1),
(4007, 40, '系统配置', '/system/config',   'views/system/config/index',    'system:config:list','C', NULL, 7, '0', 1),

-- 监控日志子菜单
(5001, 50, '操作日志', '/monitor/operation', 'views/monitor/operation/index', 'monitor:oplog:list',  'C', NULL, 1, '0', 1),
(5002, 50, '登录日志', '/monitor/login',     'views/monitor/login/index',     'monitor:loginlog:list','C', NULL, 2, '0', 1);

-- ---------------------------------------------------------------------------
-- 1.7 角色-菜单 (ADMIN拥有全部菜单, USER拥有OA+考勤+流程子集)
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `status` = '0';

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1),
(2, 10), (2, 1001), (2, 1002), (2, 1003), (2, 1004), (2, 1005), (2, 1006), (2, 1007),
(2, 1008), (2, 1009), (2, 1010), (2, 1011), (2, 1012),
(2, 20), (2, 2001), (2, 2002),
(2, 30), (2, 3002), (2, 3003), (2, 3004);

-- ---------------------------------------------------------------------------
-- 1.8 字典类型
-- ---------------------------------------------------------------------------
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`) VALUES
(1,  '请假类型',   'leave_type',         '0', 1),
(2,  '报销类别',   'expense_category',   '0', 1),
(3,  '合同类型',   'contract_type',      '0', 1),
(4,  '审批状态',   'approval_status',    '0', 1),
(5,  '性别',       'gender',             '0', 1),
(6,  '员工状态',   'emp_status',         '0', 1),
(7,  '考勤状态',   'attendance_status',  '0', 1),
(8,  '资产状态',   'asset_status',       '0', 1),
(9,  '公告类型',   'notice_type',        '0', 1),
(10, '会议状态',   'meeting_status',     '0', 1),
(11, '流程状态',   'process_status',     '0', 1),
(12, '是否',       'yes_no',             '0', 1),
(13, '学历',       'education',          '0', 1),
(14, '资产分类',   'asset_category',     '0', 1),
(15, '流程业务类型', 'business_type',    '0', 1);

-- ---------------------------------------------------------------------------
-- 1.9 字典数据
-- ---------------------------------------------------------------------------
INSERT INTO `sys_dict_data` (`data_id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
-- 请假类型
(1,   'leave_type', '事假',   '0', 1, '0', 1),
(2,   'leave_type', '年假',   '1', 2, '0', 1),
(3,   'leave_type', '病假',   '2', 3, '0', 1),
(4,   'leave_type', '婚假',   '3', 4, '0', 1),
(5,   'leave_type', '丧假',   '4', 5, '0', 1),
(6,   'leave_type', '产假',   '5', 6, '0', 1),
(7,   'leave_type', '陪产假', '6', 7, '0', 1),
-- 报销类别
(10,  'expense_category', '差旅', '0', 1, '0', 1),
(11,  'expense_category', '办公', '1', 2, '0', 1),
(12,  'expense_category', '招待', '2', 3, '0', 1),
(13,  'expense_category', '交通', '3', 4, '0', 1),
(14,  'expense_category', '其他', '4', 5, '0', 1),
-- 合同类型
(20,  'contract_type', '采购合同', '0', 1, '0', 1),
(21,  'contract_type', '销售合同', '1', 2, '0', 1),
(22,  'contract_type', '服务合同', '2', 3, '0', 1),
(23,  'contract_type', '劳务合同', '3', 4, '0', 1),
(24,  'contract_type', '其他合同', '4', 5, '0', 1),
-- 审批状态
(30,  'approval_status', '待审批', '0', 1, '0', 1),
(31,  'approval_status', '已通过', '1', 2, '0', 1),
(32,  'approval_status', '已驳回', '2', 3, '0', 1),
(33,  'approval_status', '已撤回', '3', 4, '0', 1),
-- 性别
(40,  'gender', '男',   '0', 1, '0', 1),
(41,  'gender', '女',   '1', 2, '0', 1),
(42,  'gender', '未知', '2', 3, '0', 1),
-- 员工状态
(50,  'emp_status', '正常', '0', 1, '0', 1),
(51,  'emp_status', '停用', '1', 2, '0', 1),
-- 考勤状态
(60,  'attendance_status', '正常',       '0', 1, '0', 1),
(61,  'attendance_status', '迟到',       '1', 2, '0', 1),
(62,  'attendance_status', '早退',       '2', 3, '0', 1),
(63,  'attendance_status', '缺勤',       '3', 4, '0', 1),
(64,  'attendance_status', '迟到且早退', '4', 5, '0', 1),
-- 资产状态
(70,  'asset_status', '闲置', '0', 1, '0', 1),
(71,  'asset_status', '在用', '1', 2, '0', 1),
(72,  'asset_status', '维修', '2', 3, '0', 1),
(73,  'asset_status', '报废', '3', 4, '0', 1),
-- 公告类型
(80,  'notice_type', '通知', '0', 1, '0', 1),
(81,  'notice_type', '公告', '1', 2, '0', 1),
(82,  'notice_type', '制度', '2', 3, '0', 1),
-- 会议状态
(90,  'meeting_status', '待开始', '0', 1, '0', 1),
(91,  'meeting_status', '进行中', '1', 2, '0', 1),
(92,  'meeting_status', '已结束', '2', 3, '0', 1),
(93,  'meeting_status', '已取消', '3', 4, '0', 1),
-- 流程状态
(100, 'process_status', '运行中', '0', 1, '0', 1),
(101, 'process_status', '已通过', '1', 2, '0', 1),
(102, 'process_status', '已驳回', '2', 3, '0', 1),
(103, 'process_status', '已撤回', '3', 4, '0', 1),
-- 是否
(110, 'yes_no', '是', '1', 1, '0', 1),
(111, 'yes_no', '否', '0', 2, '0', 1),
-- 学历
(120, 'education', '高中', '0', 1, '0', 1),
(121, 'education', '大专', '1', 2, '0', 1),
(122, 'education', '本科', '2', 3, '0', 1),
(123, 'education', '硕士', '3', 4, '0', 1),
(124, 'education', '博士', '4', 5, '0', 1),
-- 资产分类
(130, 'asset_category', '电子设备', 'electronics',    1, '0', 1),
(131, 'asset_category', '办公家具', 'furniture',      2, '0', 1),
(132, 'asset_category', '交通工具', 'vehicle',        3, '0', 1),
(133, 'asset_category', '其他设备', 'other',          4, '0', 1),
-- 流程业务类型
(140, 'business_type', '请假', 'leave',    1, '0', 1),
(141, 'business_type', '出差', 'trip',     2, '0', 1),
(142, 'business_type', '外出', 'outing',   3, '0', 1),
(143, 'business_type', '采购', 'purchase', 4, '0', 1),
(144, 'business_type', '报销', 'expense',  5, '0', 1),
(145, 'business_type', '加班', 'overtime', 6, '0', 1),
(146, 'business_type', '借款', 'loan',     7, '0', 1),
(147, 'business_type', '合同', 'contract', 8, '0', 1);

-- ---------------------------------------------------------------------------
-- 1.10 系统配置
-- ---------------------------------------------------------------------------
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`) VALUES
(1, '系统名称',   'sys.name',          '企业OA办公系统',  '0', '全局系统名称',       1),
(2, '默认密码',   'sys.default.password', '123456',       '0', '新员工默认密码',     1),
(3, '上传大小限制', 'sys.upload.maxSize', '50',           '0', '文件上传大小限制(MB)', 1),
(4, '会话超时',   'sys.session.timeout', '120',           '0', '会话超时时间(分钟)',   1);


-- ============================================================================
-- 二、考勤配置
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 考勤组 + 关联员工
-- ---------------------------------------------------------------------------
INSERT INTO `oa_attendance_group` (`id`, `group_name`, `work_start`, `work_end`, `late_threshold`, `status`, `create_by`) VALUES
(1, '标准考勤组', '09:00:00', '18:00:00', 15, '0', 1);

INSERT INTO `oa_attendance_group_emp` (`group_id`, `emp_id`) VALUES
(1, 1),
(1, 2),
(1, 3);

-- ---------------------------------------------------------------------------
-- 2.2 假期余额 (2026年度)
-- ---------------------------------------------------------------------------
INSERT INTO `oa_leave_balance` (`emp_id`, `leave_type`, `year`, `total_days`, `used_days`, `remaining_days`) VALUES
(1, '1', 2026, 15.0, 0.0, 15.0),
(2, '1', 2026, 10.0, 2.0,  8.0),
(3, '1', 2026, 12.0, 1.0, 11.0);


-- ============================================================================
-- 三、流程定义 (7种业务类型)
-- ============================================================================
-- 每种流程定义包含节点配置JSON，定义审批链路
-- nodeConfig格式: [{"nodeIndex":N,"nodeName":"...","nodeType":"approval","assigneeType":"...","multiType":"...","conditions":[...],"ccList":[],"timeoutHours":N}]
-- assigneeType: specific=指定人, role=角色, role_global=全局角色, dept_manager=部门经理
-- multiType: countersign=会签, orsign=或签
-- ============================================================================

INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`, `create_by`) VALUES
-- 请假流程: 部门经理审批 -> 人事备案
(1, '请假审批流程', 'leave_process', 'leave',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48},{"nodeIndex":2,"nodeName":"人事备案","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":24}]',
'0', 1, 1),

-- 出差流程: 部门经理审批 -> 总经理审批
(2, '出差审批流程', 'trip_process', 'trip',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48},{"nodeIndex":2,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":72}]',
'0', 1, 1),

-- 外出流程: 部门经理审批
(3, '外出审批流程', 'outing_process', 'outing',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":24}]',
'0', 1, 1),

-- 采购流程: 部门经理审批 -> 总经理审批 (金额>5000时需总经理)
(4, '采购审批流程', 'purchase_process', 'purchase',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48},{"nodeIndex":2,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[{"field":"amount","operator":">","value":5000}],"ccList":[],"timeoutHours":72}]',
'0', 1, 1),

-- 报销流程: 部门经理审批 -> 财务审批
(5, '报销审批流程', 'expense_process', 'expense',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48},{"nodeIndex":2,"nodeName":"财务审批","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48}]',
'0', 1, 1),

-- 加班流程: 部门经理审批
(6, '加班审批流程', 'overtime_process', 'overtime',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":24}]',
'0', 1, 1),

-- 借款流程: 部门经理审批 -> 总经理审批 -> 财务确认
(7, '借款审批流程', 'loan_process', 'loan',
'[{"nodeIndex":1,"nodeName":"部门经理审批","nodeType":"approval","assigneeType":"dept_manager","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48},{"nodeIndex":2,"nodeName":"总经理审批","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":72},{"nodeIndex":3,"nodeName":"财务确认","nodeType":"approval","assigneeType":"role","roleKey":"ADMIN","multiType":"orsign","conditions":[],"ccList":[],"timeoutHours":48}]',
'0', 1, 1);


-- ============================================================================
-- 四、会议室
-- ============================================================================

INSERT INTO `oa_meeting_room` (`id`, `room_name`, `location`, `capacity`, `equipment`, `status`, `create_by`) VALUES
(1, '第一会议室', 'A栋3楼301', 10, '投影仪,白板,视频会议系统', '0', 1),
(2, '第二会议室', 'A栋3楼302', 20, '投影仪,白板,音响系统',     '0', 1),
(3, '多功能厅',   'B栋1楼',    50, '投影仪,音响,直播设备,白板', '0', 1);


-- ============================================================================
-- 五、资产
-- ============================================================================

INSERT INTO `oa_asset` (`id`, `asset_code`, `asset_name`, `category`, `specification`, `purchase_date`, `purchase_price`, `status`, `current_user_id`, `dept_id`, `create_by`) VALUES
(1, 'IT-2024-001', 'MacBook Pro 14英寸',  'electronics', 'M3 Pro/18GB/512GB',  '2024-03-10', 14999.00, '1', 2, 2, 1),
(2, 'IT-2024-002', 'Dell U2723QE 显示器', 'electronics', '27英寸 4K IPS',      '2024-03-10', 3299.00,  '1', 2, 2, 1),
(3, 'FN-2023-001', '办公桌(大)',          'furniture',   '1.4m*0.7m 带侧柜',   '2023-06-15', 1200.00,  '1', 3, 2, 1),
(4, 'IT-2024-003', 'ThinkPad X1 Carbon',  'electronics', 'i7-1365U/16GB/512GB', '2024-05-20', 9999.00,  '0', NULL, 2, 1),
(5, 'FN-2023-002', '文件柜',              'furniture',   '五层带锁',           '2023-08-01', 800.00,   '1', NULL, 3, 1);


-- ============================================================================
-- 六、合同
-- ============================================================================

INSERT INTO `oa_contract` (`id`, `contract_no`, `contract_name`, `contract_type`, `party_a`, `party_b`, `amount`, `sign_date`, `start_date`, `end_date`, `status`, `manager_id`, `create_by`) VALUES
(1, 'HT-2026-001', '2026年度办公用品采购合同', '0', '本企业', '晨光文具有限公司', 50000.00, '2026-01-10', '2026-01-15', '2026-12-31', '2', 3, 1);


-- ============================================================================
-- 七、预算
-- ============================================================================

INSERT INTO `oa_budget` (`id`, `dept_id`, `budget_year`, `budget_month`, `amount`, `used_amount`, `status`, `create_by`) VALUES
(1, 2, 2026, NULL, 500000.00, 0.00, '2', 1);
