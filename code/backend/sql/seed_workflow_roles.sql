-- Seed workflow roles
-- New approval roles
INSERT IGNORE INTO sys_role (id, role_name, role_key, sort, status) VALUES
(3, '部门主管', 'DEPT_MANAGER', 2, 1),
(4, '组长', 'TEAM_LEAD', 3, 1),
(5, '总监', 'DIRECTOR', 4, 1),
(6, '总经理', 'GM', 5, 1);

-- Assign roles to test employees
-- emp1=admin(GM), emp2=zhangsan(TEAM_LEAD), emp3=lisi(DEPT_MANAGER), emp4=wangwu(DIRECTOR)
-- emp5=zhaoliu(DEPT_MANAGER), emp6=sunqi(TEAM_LEAD)
-- First clear existing extra roles for these employees (keep emp1's ADMIN role)
DELETE FROM sys_emp_role WHERE emp_id IN (1,2,3,4,5,6) AND role_id > 2;

INSERT IGNORE INTO sys_emp_role (emp_id, role_id) VALUES
(1, 6),  -- admin also gets GM role
(2, 4),  -- zhangsan = TEAM_LEAD
(3, 3),  -- lisi = DEPT_MANAGER
(4, 5),  -- wangwu = DIRECTOR
(5, 3),  -- zhaoliu = DEPT_MANAGER
(6, 4);  -- sunqi = TEAM_LEAD
