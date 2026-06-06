-- V981__hr_attendance_permissions.sql
SET @parent = (SELECT id FROM sys_permission WHERE perm_code='hr-attendance' OR (path='/hr/attendance' AND perm_type='MENU') LIMIT 1);
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by) VALUES
  (@parent, 'hr-attendance:record:list', '打卡记录', 'BUTTON', 1, 'ACTIVE', 'system'),
  (@parent, 'hr-attendance:exception:list', '异常列表', 'BUTTON', 2, 'ACTIVE', 'system'),
  (@parent, 'hr-attendance:exception:handle', '处理异常', 'BUTTON', 3, 'ACTIVE', 'system'),
  (@parent, 'hr-attendance:stat:list', '统计查看', 'BUTTON', 4, 'ACTIVE', 'system'),
  (@parent, 'hr-attendance:stat:recompute', '重算统计', 'BUTTON', 5, 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by) SELECT 1, id, 'system' FROM sys_permission WHERE perm_code LIKE 'hr-attendance:%' AND del_flag='0';
