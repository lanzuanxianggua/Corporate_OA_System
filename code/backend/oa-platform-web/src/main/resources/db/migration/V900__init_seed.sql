-- =============================================================================
-- Corporate OA System v2 — 初始数据
-- File: V900__init_seed.sql
-- Database: oa_system_v2
-- v2 设计: docs/v2/02-database.md §5
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 默认部门 (顶级 + 3 个子部门)
-- -----------------------------------------------------------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`, `ancestors`, `status`, `create_by`) VALUES
  (1, 0, '总公司', 'ROOT', 1, '0', 'ACTIVE', 'system'),
  (2, 1, '行政部', 'ADMIN', 1, '0,1', 'ACTIVE', 'system'),
  (3, 1, '技术部', 'TECH', 2, '0,1', 'ACTIVE', 'system'),
  (4, 1, '人力资源部', 'HR', 3, '0,1', 'ACTIVE', 'system'),
  (5, 3, '后端组', 'TECH-BE', 1, '0,1,3', 'ACTIVE', 'system'),
  (6, 3, '前端组', 'TECH-FE', 2, '0,1,3', 'ACTIVE', 'system');

-- -----------------------------------------------------------------------------
-- 默认角色
-- -----------------------------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `sort_order`, `status`, `create_by`) VALUES
  (1, 'SUPER_ADMIN', '超级管理员', 'ALL', 1, 'ACTIVE', 'system'),
  (2, 'ADMIN', '系统管理员', 'COMPANY', 10, 'ACTIVE', 'system'),
  (3, 'HR', '人事', 'COMPANY', 20, 'ACTIVE', 'system'),
  (4, 'FINANCE', '财务', 'COMPANY', 30, 'ACTIVE', 'system'),
  (5, 'DEPT_MANAGER', '部门经理', 'DEPT_DOWN', 40, 'ACTIVE', 'system'),
  (6, 'EMPLOYEE', '普通员工', 'SELF', 100, 'ACTIVE', 'system');

-- -----------------------------------------------------------------------------
-- 默认管理员账号 (密码: admin123, bcrypt 加密)
-- -----------------------------------------------------------------------------
INSERT INTO `sys_employee` (`id`, `username`, `real_name`, `emp_no`, `email`, `dept_id`, `position`, `job_level`, `hire_date`, `status`, `data_scope`, `create_by`) VALUES
  (1, 'admin', '系统管理员', 'EMP00001', 'admin@oa.local', 1, '系统管理员', 'P8', '2020-01-01', 'ACTIVE', 'ALL', 'system'),
  (2, 'hr01', '张人事', 'EMP00002', 'hr01@oa.local', 4, 'HR 经理', 'P6', '2021-03-15', 'ACTIVE', 'COMPANY', 'system'),
  (3, 'mgr01', '李经理', 'EMP00003', 'mgr01@oa.local', 3, '技术经理', 'P7', '2021-06-01', 'ACTIVE', 'DEPT_DOWN', 'system'),
  (4, 'emp01', '王员工', 'EMP00004', 'emp01@oa.local', 5, '后端工程师', 'P5', '2023-07-01', 'ACTIVE', 'SELF', 'system'),
  (5, 'fin01', '赵财务', 'EMP00005', 'fin01@oa.local', 1, '财务经理', 'P7', '2020-09-15', 'ACTIVE', 'COMPANY', 'system');

-- -----------------------------------------------------------------------------
-- 员工-角色
-- -----------------------------------------------------------------------------
INSERT INTO `sys_employee_role` (`id`, `emp_id`, `role_id`, `create_by`) VALUES
  (1, 1, 1, 'system'),
  (2, 2, 3, 'system'),
  (3, 3, 5, 'system'),
  (4, 4, 6, 'system'),
  (5, 5, 4, 'system');

-- -----------------------------------------------------------------------------
-- 基础权限 (顶级菜单)
-- -----------------------------------------------------------------------------
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (1, 0, NULL, '工作台', 'MENU', '/dashboard', 'House', 1, 'ACTIVE', 'system'),
  (2, 0, NULL, 'HR 管理', 'MENU', '/hr', 'User', 10, 'ACTIVE', 'system'),
  (3, 0, NULL, '行政审批', 'MENU', '/workflow', 'Document', 20, 'ACTIVE', 'system'),
  (4, 0, NULL, '行政事务', 'MENU', '/admin', 'OfficeBuilding', 30, 'ACTIVE', 'system'),
  (5, 0, NULL, '文档中心', 'MENU', '/document', 'Folder', 40, 'ACTIVE', 'system'),
  (6, 0, NULL, '财务管理', 'MENU', '/finance', 'Money', 50, 'ACTIVE', 'system'),
  (7, 0, NULL, '知识库', 'MENU', '/knowledge', 'Reading', 60, 'ACTIVE', 'system'),
  (8, 0, NULL, '消息中心', 'MENU', '/message', 'Message', 70, 'ACTIVE', 'system'),
  (9, 0, NULL, '会议管理', 'MENU', '/meeting', 'Calendar', 80, 'ACTIVE', 'system'),
  (10, 0, NULL, '任务协作', 'MENU', '/task', 'List', 90, 'ACTIVE', 'system'),
  (11, 0, NULL, '系统管理', 'MENU', '/system', 'Setting', 100, 'ACTIVE', 'system');

-- -----------------------------------------------------------------------------
-- HR 请假子菜单
-- -----------------------------------------------------------------------------
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `component`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  (100, 2, 'hr-leave:menu', '请假管理', 'MENU', '/hr/leave', 'hr-leave/Index', 'Calendar', 1, 'ACTIVE', 'system'),
  (101, 100, 'hr-leave:leave:create', '新建请假', 'BUTTON', NULL, NULL, NULL, 1, 'ACTIVE', 'system'),
  (102, 100, 'hr-leave:leave:list', '我的请假', 'BUTTON', NULL, NULL, NULL, 2, 'ACTIVE', 'system'),
  (103, 100, 'hr-leave:leave:revoke', '撤回请假', 'BUTTON', NULL, NULL, NULL, 3, 'ACTIVE', 'system'),
  (104, 100, 'hr-leave:leave-balance:view', '查看余额', 'BUTTON', NULL, NULL, NULL, 4, 'ACTIVE', 'system'),
  (105, 100, 'hr-leave:leave-balance:list', '余额管理', 'BUTTON', NULL, NULL, NULL, 5, 'ACTIVE', 'system'),
  (106, 100, 'hr-leave:leave-balance:init', '初始化余额', 'BUTTON', NULL, NULL, NULL, 6, 'ACTIVE', 'system'),
  (107, 100, 'hr-leave:leave-balance:adjust', '调整余额', 'BUTTON', NULL, NULL, NULL, 7, 'ACTIVE', 'system'),
  (108, 100, 'hr-leave:leave-rule:list', '规则查看', 'BUTTON', NULL, NULL, NULL, 8, 'ACTIVE', 'system'),
  (109, 100, 'hr-leave:leave-rule:update', '规则编辑', 'BUTTON', NULL, NULL, NULL, 9, 'ACTIVE', 'system');

-- -----------------------------------------------------------------------------
-- 字典 - 假期类型
-- -----------------------------------------------------------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `create_by`) VALUES
  (1, 'hr_leave_type', '假期类型', 'system'),
  (2, 'hr_leave_period', '假期时段', 'system'),
  (3, 'hr_leave_status', '假期状态', 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `is_default`, `sort_order`, `create_by`) VALUES
  (1, 'hr_leave_type', '事假', 'PERSONAL', 'N', 1, 'system'),
  (2, 'hr_leave_type', '病假', 'SICK', 'N', 2, 'system'),
  (3, 'hr_leave_type', '年假', 'ANNUAL', 'N', 3, 'system'),
  (4, 'hr_leave_type', '调休', 'COMPENSATORY', 'N', 4, 'system'),
  (5, 'hr_leave_type', '婚假', 'MARRIAGE', 'N', 5, 'system'),
  (6, 'hr_leave_type', '产假', 'MATERNITY', 'N', 6, 'system'),
  (7, 'hr_leave_type', '陪产假', 'PATERNITY', 'N', 7, 'system'),
  (8, 'hr_leave_type', '丧假', 'BEREAVEMENT', 'N', 8, 'system'),
  (9, 'hr_leave_type', '其他', 'OTHER', 'N', 99, 'system'),
  (10, 'hr_leave_period', '全天', 'FULL', 'N', 1, 'system'),
  (11, 'hr_leave_period', '上午', 'AM', 'N', 2, 'system'),
  (12, 'hr_leave_period', '下午', 'PM', 'N', 3, 'system'),
  (13, 'hr_leave_status', '草稿', 'DRAFT', 'N', 1, 'system'),
  (14, 'hr_leave_status', '审批中', 'RUNNING', 'N', 2, 'system'),
  (15, 'hr_leave_status', '已通过', 'PASSED', 'N', 3, 'system'),
  (16, 'hr_leave_status', '已驳回', 'REJECTED', 'N', 4, 'system'),
  (17, 'hr_leave_status', '已撤回', 'REVOKED', 'N', 5, 'system');

-- -----------------------------------------------------------------------------
-- 超级管理员拥有所有权限
-- -----------------------------------------------------------------------------
INSERT INTO `sys_role_permission` (`id`, `role_id`, `perm_id`, `create_by`)
SELECT id, 1, perm_id, 'system' FROM (
  SELECT id AS perm_id FROM sys_permission WHERE del_flag = 0
) t;

-- =============================================================================
-- End V900__init_seed.sql
-- =============================================================================
