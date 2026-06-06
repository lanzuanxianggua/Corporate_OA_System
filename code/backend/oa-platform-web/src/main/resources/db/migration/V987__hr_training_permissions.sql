-- V987__hr_training_permissions.sql
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by) VALUES
  ((SELECT id FROM sys_permission WHERE perm_code='hr-training' OR (path='/hr/training' AND perm_type='MENU') LIMIT 1),
   'hr-training:course:list', '课程管理', 'BUTTON', 1, 'ACTIVE', 'system'),
  ((SELECT id FROM sys_permission WHERE perm_code='hr-training' OR (path='/hr/training' AND perm_type='MENU') LIMIT 1),
   'hr-training:session:list', '班级管理', 'BUTTON', 2, 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by) SELECT 1, id, 'system'
FROM sys_permission WHERE perm_code LIKE 'hr-training:%' AND del_flag='0';
