-- Seed workflow roles
-- New approval roles
INSERT IGNORE INTO sys_role (id, role_name, role_key, sort, status) VALUES
(3, '部门主管', 'DEPT_MANAGER', 2, 1),
(4, '组长', 'TEAM_LEAD', 3, 1),
(5, '总监', 'DIRECTOR', 4, 1),
(6, '总经理', 'GM', 5, 1);

-- Assign roles to employees
-- Role assignment strategy:
--   ADMIN(id=1) = ADMIN + GM (总经理)
--   zhangsan(id=2, 技术部-后端组) = TEAM_LEAD
--   lisi(id=3, 市场部) = DEPT_MANAGER
--   wangwu(id=4, 财务部) = DIRECTOR
--   zhaoliu(id=5, 人事部) = DEPT_MANAGER
--   sunqi(id=6, 技术部) = TEAM_LEAD
--   chensy(id=11, 前端组) = TEAM_LEAD
--   yangsl(id=15, 销售组) = TEAM_LEAD
-- First clear existing extra roles for these employees (keep base USER role)
DELETE FROM sys_emp_role WHERE role_id > 2;

INSERT IGNORE INTO sys_emp_role (emp_id, role_id) VALUES
-- Approval roles
(1, 6),   -- admin = GM
(2, 4),   -- zhangsan = TEAM_LEAD
(3, 3),   -- lisi = DEPT_MANAGER
(4, 5),   -- wangwu = DIRECTOR
(5, 3),   -- zhaoliu = DEPT_MANAGER
(6, 4),   -- sunqi = TEAM_LEAD
(11, 4),  -- chensy = TEAM_LEAD (前端组长)
(15, 4);  -- yangsl = TEAM_LEAD (销售组长)
