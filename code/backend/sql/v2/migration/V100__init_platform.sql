-- =============================================================================
-- Corporate OA System v2 — 初始数据库 Schema
-- File: V100__init_platform.sql
-- Database: oa_system_v2 | Charset: utf8mb4_unicode_ci
-- v2 设计: docs/v2/02-database.md
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 部门表 (sys_dept) — 树形组织架构
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`    BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID, 0=根',
  `dept_name`    VARCHAR(64)  NOT NULL COMMENT '部门名称',
  `dept_code`    VARCHAR(64)  NOT NULL COMMENT '部门编码(全局唯一)',
  `sort_order`   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `leader_emp_id` BIGINT      DEFAULT NULL COMMENT '部门负责人',
  `ancestors`    VARCHAR(512) NOT NULL DEFAULT '' COMMENT '祖级路径(逗号分隔)',
  `phone`        VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
  `email`        VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0' COMMENT '0=正常 1=删除',
  `version`      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- -----------------------------------------------------------------------------
-- 2. 员工表 (sys_employee) — v2 唯一员工档案表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_employee`;
CREATE TABLE `sys_employee` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`     VARCHAR(64)  NOT NULL COMMENT '登录名',
  `real_name`    VARCHAR(64)  NOT NULL COMMENT '真实姓名',
  `emp_no`       VARCHAR(64)  NOT NULL COMMENT '工号',
  `email`        VARCHAR(128) DEFAULT NULL,
  `phone`        VARCHAR(32)  DEFAULT NULL,
  `avatar`       VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
  `gender`       VARCHAR(16)  DEFAULT NULL COMMENT 'MALE/FEMALE/UNKNOWN',
  `birthday`     DATE         DEFAULT NULL,
  `id_card`      VARCHAR(32)  DEFAULT NULL,
  `dept_id`      BIGINT       NOT NULL COMMENT '所属部门',
  `position`     VARCHAR(64)  DEFAULT NULL COMMENT '岗位',
  `job_level`    VARCHAR(32)  DEFAULT NULL COMMENT '职级',
  `hire_date`    DATE         DEFAULT NULL,
  `leave_date`   DATE         DEFAULT NULL,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LEAVE/SUSPENDED',
  `data_scope`   VARCHAR(16)  NOT NULL DEFAULT 'SELF' COMMENT '默认数据范围',
  `last_login_time` DATETIME   DEFAULT NULL,
  `last_login_ip`   VARCHAR(64) DEFAULT NULL,
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_emp_no` (`emp_no`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- -----------------------------------------------------------------------------
-- 3. 角色表 (sys_role)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `role_code`    VARCHAR(64)  NOT NULL COMMENT '角色编码',
  `role_name`    VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `data_scope`   VARCHAR(16)  NOT NULL DEFAULT 'SELF' COMMENT 'SELF/DEPT/DEPT_DOWN/COMPANY/ALL',
  `sort_order`   INT          NOT NULL DEFAULT 0,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- -----------------------------------------------------------------------------
-- 4. 权限表 (sys_permission) — 包含菜单/按钮/接口
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`    BIGINT       NOT NULL DEFAULT 0,
  `perm_code`    VARCHAR(128) DEFAULT NULL COMMENT '权限码, 如 hr-leave:leave:create',
  `perm_name`    VARCHAR(64)  NOT NULL COMMENT '权限名称',
  `perm_type`    VARCHAR(16)  NOT NULL COMMENT 'MENU/BUTTON/API',
  `path`         VARCHAR(256) DEFAULT NULL COMMENT '前端路径(菜单用)',
  `component`    VARCHAR(256) DEFAULT NULL,
  `icon`         VARCHAR(64)  DEFAULT NULL,
  `sort_order`   INT          NOT NULL DEFAULT 0,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_perm_type` (`perm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- -----------------------------------------------------------------------------
-- 5. 员工-角色关联 (sys_employee_role)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_employee_role`;
CREATE TABLE `sys_employee_role` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `emp_id`       BIGINT       NOT NULL,
  `role_id`      BIGINT       NOT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_role` (`emp_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工-角色关联';

-- -----------------------------------------------------------------------------
-- 6. 角色-权限关联 (sys_role_permission)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `role_id`      BIGINT       NOT NULL,
  `perm_id`      BIGINT       NOT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
  KEY `idx_perm_id` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联';

-- -----------------------------------------------------------------------------
-- 7. 字典类型表 (sys_dict_type)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_type`    VARCHAR(64)  NOT NULL COMMENT '字典类型编码',
  `dict_name`    VARCHAR(64)  NOT NULL,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- -----------------------------------------------------------------------------
-- 8. 字典数据表 (sys_dict_data)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_type`    VARCHAR(64)  NOT NULL,
  `dict_label`   VARCHAR(64)  NOT NULL,
  `dict_value`   VARCHAR(128) NOT NULL,
  `css_class`    VARCHAR(64)  DEFAULT NULL,
  `list_class`   VARCHAR(64)  DEFAULT NULL,
  `is_default`   CHAR(1)      NOT NULL DEFAULT 'N' COMMENT 'Y/N',
  `sort_order`   INT          NOT NULL DEFAULT 0,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `remark`       VARCHAR(500) DEFAULT NULL,
  `create_by`    VARCHAR(64)  NOT NULL DEFAULT 'system',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`    VARCHAR(64)  DEFAULT NULL,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  `version`      INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- -----------------------------------------------------------------------------
-- 9. 操作日志表 (oa_operation_log)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_operation_log`;
CREATE TABLE `oa_operation_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `emp_id`       BIGINT       DEFAULT NULL,
  `emp_name`     VARCHAR(64)  DEFAULT NULL,
  `module`       VARCHAR(32)  NOT NULL,
  `action`       VARCHAR(64)  NOT NULL,
  `biz_type`     VARCHAR(64)  DEFAULT NULL,
  `biz_id`       VARCHAR(64)  DEFAULT NULL,
  `request_uri`  VARCHAR(256) NOT NULL,
  `request_method` VARCHAR(8) NOT NULL,
  `request_params` TEXT      DEFAULT NULL,
  `response_result` TEXT     DEFAULT NULL,
  `ip`           VARCHAR(64)  DEFAULT NULL,
  `user_agent`   VARCHAR(512) DEFAULT NULL,
  `trace_id`     VARCHAR(64)  DEFAULT NULL,
  `cost_ms`      BIGINT       DEFAULT NULL,
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILURE',
  `error_msg`    TEXT         DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- -----------------------------------------------------------------------------
-- 10. 附件表 (sys_attachment)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_attachment`;
CREATE TABLE `sys_attachment` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `biz_type`     VARCHAR(64)  DEFAULT NULL COMMENT '业务类型',
  `biz_id`       VARCHAR(64)  DEFAULT NULL COMMENT '业务ID',
  `file_name`    VARCHAR(256) NOT NULL,
  `original_name` VARCHAR(256) NOT NULL,
  `file_ext`     VARCHAR(16)  NOT NULL,
  `file_size`    BIGINT       NOT NULL,
  `content_type` VARCHAR(128) DEFAULT NULL,
  `storage_type` VARCHAR(16)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/OSS/S3',
  `storage_path` VARCHAR(512) NOT NULL,
  `url`          VARCHAR(512) DEFAULT NULL,
  `md5`          VARCHAR(64)  DEFAULT NULL,
  `emp_id`       BIGINT       DEFAULT NULL COMMENT '上传人',
  `upload_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

-- -----------------------------------------------------------------------------
-- 11. 通知表 (msg_notification)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `msg_notification`;
CREATE TABLE `msg_notification` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(256) NOT NULL,
  `content`      TEXT         NOT NULL,
  `category`     VARCHAR(32)  NOT NULL COMMENT 'SYSTEM/WORKFLOW/ANNOUNCE/TODO',
  `biz_type`     VARCHAR(64)  DEFAULT NULL,
  `biz_id`       VARCHAR(64)  DEFAULT NULL,
  `sender_id`    BIGINT       DEFAULT NULL COMMENT '0=系统',
  `priority`     VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH/URGENT',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'UNREAD' COMMENT 'UNREAD/READ/ARCHIVED',
  `read_time`    DATETIME     DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_category` (`category`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- -----------------------------------------------------------------------------
-- 12. 通知接收人表 (msg_notification_recipient)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `msg_notification_recipient`;
CREATE TABLE `msg_notification_recipient` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `notification_id` BIGINT    NOT NULL,
  `recipient_id` BIGINT       NOT NULL,
  `is_read`      CHAR(1)      NOT NULL DEFAULT 'N',
  `read_time`    DATETIME     DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notif_recipient` (`notification_id`, `recipient_id`),
  KEY `idx_recipient` (`recipient_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知接收人表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- End V100__init_platform.sql
-- =============================================================================
