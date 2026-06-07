-- ============================================
-- V910__add_sys_employee_password.sql
-- 增量: sys_employee 加 password 列 + 设 admin/hr01/mgr01/emp01/fin01 初始密码 admin123
-- 密码存储策略: 写入时是明文 'admin123'; 首次登录由 AuthService.matchesPassword
--   走双轨制 (BCrypt 优先, 明文命中自动 rehash), 自动升级为 BCrypt 哈希.
-- 详见 cn.oa.platform.security.password.BCryptPasswordEncoder.
-- ============================================

ALTER TABLE `sys_employee` ADD COLUMN `password` VARCHAR(100) NOT NULL DEFAULT '' AFTER `email`;

-- 5 个 seed 用户的初始密码 (明文, 首次登录后由 AuthService Lazy Rehash 升级)
UPDATE `sys_employee` SET `password` = 'admin123' WHERE `username` IN ('admin', 'hr01', 'mgr01', 'emp01', 'fin01');
