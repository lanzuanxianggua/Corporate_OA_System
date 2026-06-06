-- V983__hr_performance_permissions.sql
-- 1) 父菜单 (parent_id=2 => HR 管理)
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, sort_order, status, create_by) VALUES
  (2, 'hr-performance', '绩效管理', 'MENU', '/hr/performance', 3, 'ACTIVE', 'system');

-- 2) 业务权限
SET @parent = (SELECT id FROM sys_permission WHERE perm_code='hr-performance' OR (path='/hr/performance' AND perm_type='MENU') LIMIT 1);
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by) VALUES
  (@parent, 'hr-performance:template:list', '绩效模板', 'BUTTON', 1, 'ACTIVE', 'system'),
  (@parent, 'hr-performance:cycle:list', '绩效周期', 'BUTTON', 2, 'ACTIVE', 'system'),
  (@parent, 'hr-performance:result:list', '绩效结果', 'BUTTON', 3, 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by) SELECT 1, id, 'system'
FROM sys_permission WHERE perm_code LIKE 'hr-performance:%' AND del_flag='0';
