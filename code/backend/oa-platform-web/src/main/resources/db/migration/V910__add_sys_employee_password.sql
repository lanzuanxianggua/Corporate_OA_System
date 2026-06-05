-- ============================================
-- V910__add_sys_employee_password.sql
-- 增量: sys_employee 加 password 列 + 设 admin/hr01/mgr01/emp01/fin01 初始密码 admin123
-- 对应 AuthController.matchesPassword() 明文比较
-- ============================================

ALTER TABLE `sys_employee` ADD COLUMN `password` VARCHAR(100) NOT NULL DEFAULT '' AFTER `email`;

-- 5 个 seed 用户的初始密码
UPDATE `sys_employee` SET `password` = 'admin123' WHERE `username` IN ('admin', 'hr01', 'mgr01', 'emp01', 'fin01');
