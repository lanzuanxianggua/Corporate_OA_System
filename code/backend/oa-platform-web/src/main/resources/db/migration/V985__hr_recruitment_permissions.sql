-- V985__hr_recruitment_permissions.sql
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by) VALUES
  ((SELECT id FROM sys_permission WHERE perm_code='hr-recruitment' OR (path='/hr/recruitment' AND perm_type='MENU') LIMIT 1),
   'hr-recruitment:job:list', '岗位管理', 'BUTTON', 1, 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by) SELECT 1, id, 'system'
FROM sys_permission WHERE perm_code LIKE 'hr-recruitment:%' AND del_flag='0';
