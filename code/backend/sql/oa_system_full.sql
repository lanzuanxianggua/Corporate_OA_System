-- ============================================================================
-- 企业OA办公系统 — 完整DDL
-- Database: oa_system | Charset: utf8mb4 | Collation: utf8mb4_general_ci
-- MySQL 8.0+
-- ============================================================================

DROP DATABASE IF EXISTS `oa_system`;
CREATE DATABASE `oa_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `oa_system`;

-- ============================================================================
-- 一、系统管理表 (System Tables)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 员工表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_employee`;
CREATE TABLE `sys_employee` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `emp_code`    VARCHAR(32)  NOT NULL                COMMENT '工号',
  `emp_name`    VARCHAR(64)  NOT NULL                COMMENT '姓名',
  `password`    VARCHAR(128) NOT NULL                COMMENT '密码(BCrypt)',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(128) DEFAULT NULL            COMMENT '邮箱',
  `avatar`      VARCHAR(512) DEFAULT NULL            COMMENT '头像URL',
  `dept_id`     BIGINT       DEFAULT NULL            COMMENT '部门ID',
  `post_id`     BIGINT       DEFAULT NULL            COMMENT '岗位ID',
  `status`      INT          NOT NULL DEFAULT 1      COMMENT '状态(1正常 0停用)',
  `hire_date`   DATE         DEFAULT NULL            COMMENT '入职日期',
  `gender`      CHAR(1)      DEFAULT '0'             COMMENT '性别(0男 1女 2未知)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_code` (`emp_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工表';

-- ---------------------------------------------------------------------------
-- 1.2 部门表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id`   BIGINT       DEFAULT 0               COMMENT '父部门ID(0为顶级)',
  `dept_name`   VARCHAR(100) NOT NULL                COMMENT '部门名称',
  `order_num`   INT          DEFAULT 0               COMMENT '显示顺序',
  `leader`      VARCHAR(64)  DEFAULT NULL            COMMENT '负责人',
  `status`      INT          NOT NULL DEFAULT 0    COMMENT '状态(0正常 1停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

-- ---------------------------------------------------------------------------
-- 1.3 角色表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   VARCHAR(64)  NOT NULL                COMMENT '角色名称',
  `role_key`    VARCHAR(64)  NOT NULL                COMMENT '角色权限字符',
  `role_sort`   INT          DEFAULT 0               COMMENT '显示顺序',
  `status`      INT          NOT NULL DEFAULT 0    COMMENT '状态(0正常 1停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ---------------------------------------------------------------------------
-- 1.4 员工-角色关联表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_emp_role`;
CREATE TABLE `sys_emp_role` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`  BIGINT NOT NULL                COMMENT '员工ID',
  `role_id` BIGINT NOT NULL                COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_role` (`emp_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工-角色关联表';

-- ---------------------------------------------------------------------------
-- 1.5 菜单表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id`   BIGINT       DEFAULT 0               COMMENT '父菜单ID(0为顶级)',
  `menu_name`   VARCHAR(100) NOT NULL                COMMENT '菜单名称',
  `path`        VARCHAR(200) DEFAULT NULL            COMMENT '路由地址',
  `component`   VARCHAR(200) DEFAULT NULL            COMMENT '组件路径',
  `perms`       VARCHAR(100) DEFAULT NULL            COMMENT '权限标识',
  `menu_type`   CHAR(1)      NOT NULL                COMMENT '菜单类型(M目录 C菜单 F按钮)',
  `icon`        VARCHAR(100) DEFAULT NULL            COMMENT '菜单图标',
  `order_num`   INT          DEFAULT 0               COMMENT '显示顺序',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- ---------------------------------------------------------------------------
-- 1.6 角色-菜单关联表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` BIGINT NOT NULL                COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL                COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-菜单关联表';

-- ---------------------------------------------------------------------------
-- 1.7 字典类型表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_name`   VARCHAR(100) NOT NULL                COMMENT '字典名称',
  `dict_type`   VARCHAR(100) NOT NULL                COMMENT '字典类型',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- ---------------------------------------------------------------------------
-- 1.8 字典数据表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `data_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_type`   VARCHAR(100) NOT NULL                COMMENT '字典类型',
  `dict_label`  VARCHAR(200) NOT NULL                COMMENT '字典标签',
  `dict_value`  VARCHAR(200) NOT NULL                COMMENT '字典值',
  `dict_sort`   INT          DEFAULT 0               COMMENT '排序',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  PRIMARY KEY (`data_id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- ---------------------------------------------------------------------------
-- 1.9 系统配置表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name`  VARCHAR(100) NOT NULL                COMMENT '配置名称',
  `config_key`   VARCHAR(100) NOT NULL                COMMENT '配置键',
  `config_value` TEXT         DEFAULT NULL            COMMENT '配置值',
  `config_type`  CHAR(1)      DEFAULT '0'             COMMENT '系统内置(0是 1否)',
  `remark`       VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `create_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- ---------------------------------------------------------------------------
-- 1.10 岗位表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `post_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code`   VARCHAR(64)  NOT NULL                COMMENT '岗位编码',
  `post_name`   VARCHAR(100) NOT NULL                COMMENT '岗位名称',
  `post_sort`   INT          DEFAULT 0               COMMENT '显示顺序',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';


-- ============================================================================
-- 二、OA核心业务表 (OA Core Tables)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 考勤组表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_attendance_group`;
CREATE TABLE `oa_attendance_group` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '考勤组ID',
  `group_name`      VARCHAR(100) NOT NULL               COMMENT '考勤组名称',
  `work_start`      TIME        NOT NULL                COMMENT '上班时间',
  `work_end`        TIME        NOT NULL                COMMENT '下班时间',
  `late_threshold`  INT         DEFAULT 0               COMMENT '迟到阈值(分钟)',
  `status`          CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `del_flag`        INT          NOT NULL DEFAULT 0       COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤组表';

-- ---------------------------------------------------------------------------
-- 2.2 考勤组-员工关联表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_attendance_group_emp`;
CREATE TABLE `oa_attendance_group_emp` (
  `id`       BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` BIGINT NOT NULL                COMMENT '考勤组ID',
  `emp_id`   BIGINT NOT NULL                COMMENT '员工ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_emp` (`group_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤组-员工关联表';

-- ---------------------------------------------------------------------------
-- 2.3 考勤记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_attendance`;
CREATE TABLE `oa_attendance` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `work_date`       DATE         NOT NULL                COMMENT '考勤日期',
  `clock_in`        DATETIME     DEFAULT NULL            COMMENT '签到时间',
  `clock_out`       DATETIME     DEFAULT NULL            COMMENT '签退时间',
  `status`          INT          NOT NULL DEFAULT 0      COMMENT '状态(0正常 1迟到 2早退 3缺勤 4迟到且早退 5请假 6出差)',
  `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `ip`              VARCHAR(50)  DEFAULT NULL            COMMENT '打卡IP',
  `address`         VARCHAR(200) DEFAULT NULL            COMMENT '打卡地址',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date` (`emp_id`, `work_date`),
  KEY `idx_work_date` (`work_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤记录表';

-- ---------------------------------------------------------------------------
-- 2.4 请假申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_leave_apply`;
CREATE TABLE `oa_leave_apply` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '请假ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `leave_type`          CHAR(1)      NOT NULL                COMMENT '请假类型(0事假 1年假 2病假 3婚假 4丧假 5产假 6陪产假)',
  `start_time`          DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`            DATETIME     NOT NULL                COMMENT '结束时间',
  `days`                DECIMAL(4,1) NOT NULL DEFAULT 0             COMMENT '请假天数',
  `reason`              VARCHAR(500) DEFAULT NULL            COMMENT '请假原因',
  `status`              INT          NOT NULL DEFAULT 0      COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='请假申请表';

-- ---------------------------------------------------------------------------
-- 2.5 假期余额表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_leave_balance`;
CREATE TABLE `oa_leave_balance` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `leave_type`      INT          NOT NULL                COMMENT '请假类型(0事假 1年假 2病假 3婚假 4丧假 5产假 6陪产假)',
  `year`            INT          NOT NULL                COMMENT '年度',
  `total_days`      DECIMAL(4,1) NOT NULL DEFAULT 0      COMMENT '总天数',
  `used_days`       DECIMAL(4,1) NOT NULL DEFAULT 0      COMMENT '已用天数',
  `remaining_days`  DECIMAL(4,1) NOT NULL DEFAULT 0      COMMENT '剩余天数',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_year` (`emp_id`, `leave_type`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='假期余额表';

-- ---------------------------------------------------------------------------
-- 2.6 公告表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_notice`;
CREATE TABLE `oa_notice` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title`       VARCHAR(200)  NOT NULL                COMMENT '标题',
  `content`     LONGTEXT      DEFAULT NULL            COMMENT '内容',
  `type`        INT          NOT NULL DEFAULT 0    COMMENT '类型(0通知 1公告 2制度)',
  `emp_id`      BIGINT        DEFAULT NULL            COMMENT '发布人ID',
  `status`      INT          NOT NULL DEFAULT 0    COMMENT '状态(0草稿 1已发布 2已撤回)',
  `is_top`      CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '是否置顶(0否 1是)',
  `del_flag`    CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告表';

-- ---------------------------------------------------------------------------
-- 2.7 公告阅读记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_notice_read`;
CREATE TABLE `oa_notice_read` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `notice_id`  BIGINT   NOT NULL                COMMENT '公告ID',
  `emp_id`     BIGINT   NOT NULL                COMMENT '员工ID',
  `read_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_emp` (`notice_id`, `emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告阅读记录表';

-- ---------------------------------------------------------------------------
-- 2.8 公文表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_document`;
CREATE TABLE `oa_document` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '公文ID',
  `title`       VARCHAR(200)  NOT NULL                COMMENT '标题',
  `content`     LONGTEXT      DEFAULT NULL            COMMENT '内容',
  `category_id` BIGINT        DEFAULT NULL            COMMENT '分类ID',
  `emp_id`      BIGINT        DEFAULT NULL            COMMENT '创建人ID',
  `file_url`    VARCHAR(512)  DEFAULT NULL            COMMENT '附件URL',
  `status`      INT          NOT NULL DEFAULT 0    COMMENT '状态(0草稿 1已发布 2已归档)',
  `del_flag`    CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文表';

-- ---------------------------------------------------------------------------
-- 2.9 公文分类表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_document_category`;
CREATE TABLE `oa_document_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`        VARCHAR(100) NOT NULL                COMMENT '分类名称',
  `parent_id`   BIGINT       DEFAULT 0               COMMENT '父分类ID(0为顶级)',
  `sort`        INT          DEFAULT 0               COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文分类表';

-- ---------------------------------------------------------------------------
-- 2.10 日程表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_schedule`;
CREATE TABLE `oa_schedule` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '员工ID',
  `title`       VARCHAR(200) NOT NULL                COMMENT '标题',
  `content`     TEXT         DEFAULT NULL            COMMENT '内容',
  `start_time`  DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`    DATETIME     NOT NULL                COMMENT '结束时间',
  `status`      INT          NOT NULL DEFAULT 0      COMMENT '状态(0待办 1进行中 2已完成 3已取消)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='日程表';

-- ---------------------------------------------------------------------------
-- 2.11 消息表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_message`;
CREATE TABLE `oa_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id`   BIGINT       NOT NULL                COMMENT '发送人ID',
  `receiver_id` BIGINT       NOT NULL                COMMENT '接收人ID',
  `title`       VARCHAR(200) DEFAULT NULL            COMMENT '标题',
  `content`     TEXT         DEFAULT NULL            COMMENT '内容',
  `is_read`     INT          NOT NULL DEFAULT 0    COMMENT '是否已读(0未读 1已读)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息表';

-- ---------------------------------------------------------------------------
-- 2.12 待办事项表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_todo`;
CREATE TABLE `oa_todo` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `emp_id`        BIGINT       NOT NULL                COMMENT '员工ID',
  `title`         VARCHAR(200) NOT NULL                COMMENT '标题',
  `todo_type`     VARCHAR(32)  NOT NULL                COMMENT '类型(0审批 1催办 2提醒)',
  `business_id`   BIGINT       DEFAULT NULL            COMMENT '业务ID',
  `business_type` VARCHAR(32)  DEFAULT NULL            COMMENT '业务类型(leave/trip/outing/purchase/expense/overtime/loan)',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待处理 1已处理 2已取消)',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `done_time`     DATETIME     DEFAULT NULL            COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='待办事项表';


-- ============================================================================
-- 三、工作流表 (Workflow Tables)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 流程定义表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_process_definition`;
CREATE TABLE `wf_process_definition` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `process_name` VARCHAR(200) NOT NULL                COMMENT '流程名称',
  `process_key`  VARCHAR(64)  NOT NULL                COMMENT '流程唯一标识',
  `process_type` VARCHAR(32)  NOT NULL                COMMENT '流程类型(leave/trip/outing/purchase/expense/overtime/loan/contract)',
  `node_config`  TEXT         DEFAULT NULL            COMMENT '节点配置JSON',
  `status`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `version`      INT          NOT NULL DEFAULT 1      COMMENT '版本号',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_key` (`process_key`),
  KEY `idx_process_type` (`process_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程定义表';

-- ---------------------------------------------------------------------------
-- 3.2 流程实例表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_process_instance`;
CREATE TABLE `wf_process_instance` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `process_id`         BIGINT       NOT NULL                COMMENT '流程定义ID',
  `business_type`      VARCHAR(32)  NOT NULL                COMMENT '业务类型',
  `business_id`        BIGINT       NOT NULL                COMMENT '业务ID',
  `initiator_id`       BIGINT       NOT NULL                COMMENT '发起人ID',
  `current_node`       INT          DEFAULT NULL            COMMENT '当前节点索引',
  `condition_context`  TEXT         DEFAULT NULL            COMMENT '条件上下文JSON',
  `status`             CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0运行中 1已通过 2已驳回 3已撤回)',
  `start_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `end_time`           DATETIME     DEFAULT NULL            COMMENT '结束时间',
  `del_flag`           CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `active_nodes`       VARCHAR(255) DEFAULT NULL            COMMENT '逗号分隔的活动节点ID(并行网关)',
  `snapshot_node_config` TEXT        DEFAULT NULL            COMMENT '流程启动时的节点配置快照',
  `parent_instance_id` BIGINT       DEFAULT NULL            COMMENT '父流程实例ID(子流程嵌套)',
  `process_version`    INT          DEFAULT NULL            COMMENT '流程定义版本号',
  PRIMARY KEY (`id`),
  KEY `idx_process_id` (`process_id`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程实例表';

-- ---------------------------------------------------------------------------
-- 3.3 任务表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_task`;
CREATE TABLE `wf_task` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id`  BIGINT       NOT NULL                COMMENT '流程实例ID',
  `process_id`   BIGINT       NOT NULL                COMMENT '流程定义ID',
  `node_index`   INT          NOT NULL                COMMENT '节点索引',
  `node_name`    VARCHAR(100) DEFAULT NULL            COMMENT '节点名称',
  `assignee_id`  BIGINT       NOT NULL                COMMENT '审批人ID',
  `status`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已通过 2已驳回 3已转办 4已退回 5已催办)',
  `action_time`  DATETIME     DEFAULT NULL            COMMENT '操作时间',
  `remark`       VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `remind_count` INT          DEFAULT 0               COMMENT '催办次数',
  `last_remind_time` DATETIME  DEFAULT NULL           COMMENT '最后催办时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务表';

-- ---------------------------------------------------------------------------
-- 3.4 抄送记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_cc_record`;
CREATE TABLE `wf_cc_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `instance_id` BIGINT   NOT NULL                COMMENT '流程实例ID',
  `task_id`     BIGINT   DEFAULT NULL            COMMENT '任务ID',
  `cc_user_id`  BIGINT   NOT NULL                COMMENT '抄送人ID',
  `business_type` VARCHAR(32) DEFAULT NULL       COMMENT '业务类型',
  `status`      CHAR(1)  DEFAULT '0'             COMMENT '状态(0未读 1已读)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_cc_user_id` (`cc_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='抄送记录表';

-- ---------------------------------------------------------------------------
-- 3.5 审批委托表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_delegation`;
CREATE TABLE `wf_delegation` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `delegator_id` BIGINT      NOT NULL                COMMENT '委托人ID',
  `delegate_id`  BIGINT      NOT NULL                COMMENT '被委托人ID',
  `business_type` VARCHAR(32) DEFAULT NULL            COMMENT '业务类型(NULL表示全部)',
  `start_time`   DATETIME    NOT NULL                COMMENT '生效时间',
  `end_time`     DATETIME    NOT NULL                COMMENT '失效时间',
  `status`       CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '状态(0生效中 1已失效 2已取消)',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_delegator_id` (`delegator_id`),
  KEY `idx_delegate_id` (`delegate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批委托表';


-- ============================================================================
-- 四、业务扩展表 (Business Extension Tables)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 4.1 出差申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_business_trip`;
CREATE TABLE `oa_business_trip` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出差ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `destination`         VARCHAR(200) NOT NULL                COMMENT '目的地',
  `purpose`             VARCHAR(500) DEFAULT NULL            COMMENT '出差目的',
  `start_time`          DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`            DATETIME     NOT NULL                COMMENT '结束时间',
  `status`              INT          NOT NULL DEFAULT 0      COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出差申请表';

-- ---------------------------------------------------------------------------
-- 4.2 外出申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_outing`;
CREATE TABLE `oa_outing` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '外出ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `reason`              VARCHAR(500) DEFAULT NULL            COMMENT '外出事由',
  `destination`         VARCHAR(200) DEFAULT NULL            COMMENT '目的地',
  `start_time`          DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`            DATETIME     NOT NULL                COMMENT '结束时间',
  `status`              INT          NOT NULL DEFAULT 0      COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外出申请表';

-- ---------------------------------------------------------------------------
-- 4.3 采购申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_purchase`;
CREATE TABLE `oa_purchase` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '采购ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `item_name`           VARCHAR(200)    NOT NULL                COMMENT '物品名称',
  `quantity`            INT             NOT NULL DEFAULT 1      COMMENT '数量',
  `amount`              DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '金额',
  `reason`              VARCHAR(500)    DEFAULT NULL            COMMENT '采购原因',
  `status`              INT          NOT NULL DEFAULT 0      COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购申请表';

-- ---------------------------------------------------------------------------
-- 4.4 报销申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_expense`;
CREATE TABLE `oa_expense` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '报销ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `title`               VARCHAR(200)    NOT NULL                COMMENT '报销标题',
  `amount`              DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '报销金额',
  `category`            CHAR(1)         NOT NULL                COMMENT '报销类别(0差旅 1办公 2招待 3交通 4其他)',
  `description`         VARCHAR(500)    DEFAULT NULL            COMMENT '说明',
  `status`              INT          NOT NULL DEFAULT 0      COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报销申请表';

-- ---------------------------------------------------------------------------
-- 4.5 加班申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_overtime`;
CREATE TABLE `oa_overtime` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '加班ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `overtime_date`       DATE            NOT NULL                COMMENT '加班日期',
  `start_time`          DATETIME        NOT NULL                COMMENT '开始时间',
  `end_time`            DATETIME        NOT NULL                COMMENT '结束时间',
  `hours`               DECIMAL(4,1)    NOT NULL DEFAULT 0      COMMENT '加班时长(小时)',
  `reason`              VARCHAR(500)    DEFAULT NULL            COMMENT '加班原因',
  `status`              CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_overtime_date` (`overtime_date`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='加班申请表';

-- ---------------------------------------------------------------------------
-- 4.6 借款申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_loan`;
CREATE TABLE `oa_loan` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '借款ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `loan_amount`         DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '借款金额',
  `repaid_amount`       DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '已还金额',
  `loan_reason`         VARCHAR(500)    DEFAULT NULL            COMMENT '借款原因',
  `repayment_plan`      VARCHAR(500)    DEFAULT NULL            COMMENT '还款计划',
  `status`              CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已通过 2已驳回 3已撤回 4已还清)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借款申请表';

-- ---------------------------------------------------------------------------
-- 4.7 还款记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_loan_repayment`;
CREATE TABLE `oa_loan_repayment` (
  `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '还款ID',
  `loan_id`     BIGINT          NOT NULL                COMMENT '借款ID',
  `amount`      DECIMAL(12,2)   NOT NULL DEFAULT 0      COMMENT '还款金额',
  `repay_time`  DATETIME        NOT NULL                COMMENT '还款时间',
  `remark`      VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_loan_id` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='还款记录表';

-- ---------------------------------------------------------------------------
-- 4.8 资产表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_asset`;
CREATE TABLE `oa_asset` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '资产ID',
  `asset_code`      VARCHAR(64)     NOT NULL                COMMENT '资产编号',
  `asset_name`      VARCHAR(200)    NOT NULL                COMMENT '资产名称',
  `category`        VARCHAR(64)     DEFAULT NULL            COMMENT '资产分类',
  `specification`   VARCHAR(200)    DEFAULT NULL            COMMENT '规格型号',
  `purchase_date`   DATE            DEFAULT NULL            COMMENT '购买日期',
  `purchase_price`  DECIMAL(12,2)   DEFAULT 0               COMMENT '购买价格',
  `status`          CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0闲置 1在用 2维修 3报废)',
  `current_user_id` BIGINT          DEFAULT NULL            COMMENT '使用人ID',
  `dept_id`         BIGINT          DEFAULT NULL            COMMENT '所属部门ID',
  `del_flag`        CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_code` (`asset_code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_current_user_id` (`current_user_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产表';

-- ---------------------------------------------------------------------------
-- 4.9 资产借用表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_asset_borrow`;
CREATE TABLE `oa_asset_borrow` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '借用ID',
  `asset_id`        BIGINT       NOT NULL                COMMENT '资产ID',
  `borrower_id`     BIGINT       NOT NULL                COMMENT '借用人ID',
  `borrow_time`     DATETIME     NOT NULL                COMMENT '借用时间',
  `expected_return` DATETIME     DEFAULT NULL            COMMENT '预计归还时间',
  `actual_return`   DATETIME     DEFAULT NULL            COMMENT '实际归还时间',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0借用中 1已归还 2逾期)',
  `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_borrower_id` (`borrower_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产借用表';

-- ---------------------------------------------------------------------------
-- 4.10 合同表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_contract`;
CREATE TABLE `oa_contract` (
  `id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_no`  VARCHAR(64)     NOT NULL                COMMENT '合同编号',
  `contract_name` VARCHAR(200)   NOT NULL                COMMENT '合同名称',
  `contract_type` CHAR(1)        NOT NULL DEFAULT '0'    COMMENT '合同类型(0采购 1销售 2服务 3劳务 4其他)',
  `party_a`      VARCHAR(200)    DEFAULT NULL            COMMENT '甲方',
  `party_b`      VARCHAR(200)    DEFAULT NULL            COMMENT '乙方',
  `amount`       DECIMAL(14,2)   DEFAULT 0               COMMENT '合同金额',
  `sign_date`    DATE            DEFAULT NULL            COMMENT '签订日期',
  `start_date`   DATE            DEFAULT NULL            COMMENT '开始日期',
  `end_date`     DATE            DEFAULT NULL            COMMENT '结束日期',
  `status`       CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0草拟 1审批中 2执行中 3已完成 4已终止)',
  `manager_id`   BIGINT          DEFAULT NULL            COMMENT '负责人ID',
  `file_url`     VARCHAR(512)    DEFAULT NULL            COMMENT '合同文件URL',
  `del_flag`     CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`    VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_type` (`contract_type`),
  KEY `idx_status` (`status`),
  KEY `idx_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同表';

-- ---------------------------------------------------------------------------
-- 4.11 预算表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_budget`;
CREATE TABLE `oa_budget` (
  `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `dept_id`     BIGINT          NOT NULL                COMMENT '部门ID',
  `budget_year` INT             NOT NULL                COMMENT '预算年度',
  `budget_month` INT            DEFAULT NULL            COMMENT '预算月份(NULL表示年度预算)',
  `amount`      DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '预算金额',
  `used_amount` DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '已用金额',
  `status`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已通过 2执行中 3已完成)',
  `version`     INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `del_flag`    CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_year_month` (`budget_year`, `budget_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预算表';

-- ---------------------------------------------------------------------------
-- 4.12 会议室表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_meeting_room`;
CREATE TABLE `oa_meeting_room` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `room_name`   VARCHAR(100) NOT NULL                COMMENT '会议室名称',
  `location`    VARCHAR(200) DEFAULT NULL            COMMENT '位置',
  `capacity`    INT          DEFAULT 0               COMMENT '容纳人数',
  `equipment`   VARCHAR(500) DEFAULT NULL            COMMENT '设备描述',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0可用 1维护中 2已停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议室表';

-- ---------------------------------------------------------------------------
-- 4.13 会议表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_meeting`;
CREATE TABLE `oa_meeting` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会议ID',
  `title`        VARCHAR(200) NOT NULL                COMMENT '会议主题',
  `room_id`      BIGINT       NOT NULL                COMMENT '会议室ID',
  `organizer_id` BIGINT       NOT NULL                COMMENT '组织人ID',
  `start_time`   DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`     DATETIME     NOT NULL                COMMENT '结束时间',
  `description`  TEXT         DEFAULT NULL            COMMENT '会议描述',
  `participants` VARCHAR(500) DEFAULT NULL            COMMENT '参会人员ID(逗号分隔)',
  `status`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待开始 1进行中 2已结束 3已取消)',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_organizer_id` (`organizer_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议表';

-- ---------------------------------------------------------------------------
-- 4.14 薪资结构表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_salary_structure`;
CREATE TABLE `oa_salary_structure` (
  `id`             BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`         BIGINT          NOT NULL                COMMENT '员工ID',
  `base_salary`    DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '基本工资',
  `post_salary`    DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '岗位工资',
  `merit_salary`   DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '绩效工资',
  `allowance`      DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '补贴',
  `effective_date` DATE            DEFAULT NULL            COMMENT '生效日期',
  `status`         CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0生效 1失效)',
  `del_flag`       CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`      VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资结构表';

-- ---------------------------------------------------------------------------
-- 4.15 薪资记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_salary_record`;
CREATE TABLE `oa_salary_record` (
  `id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`       BIGINT          NOT NULL                COMMENT '员工ID',
  `salary_month` VARCHAR(7)      NOT NULL                COMMENT '薪资月份(yyyy-MM)',
  `base_salary`  DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '基本工资',
  `post_salary`  DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '岗位工资',
  `merit_salary` DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '绩效工资',
  `allowance`    DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '补贴',
  `deduction`    DECIMAL(10,2)   NOT NULL DEFAULT 0      COMMENT '扣款',
  `actual_amount` DECIMAL(10,2)  NOT NULL DEFAULT 0      COMMENT '实发金额',
  `pay_time`     DATETIME        DEFAULT NULL            COMMENT '发放时间',
  `status`       CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0待发放 1已发放)',
  `create_by`    VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_month` (`emp_id`, `salary_month`),
  KEY `idx_salary_month` (`salary_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资记录表';

-- ---------------------------------------------------------------------------
-- 4.16 员工档案表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_emp_archive`;
CREATE TABLE `oa_emp_archive` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `emp_id`             BIGINT       NOT NULL                COMMENT '员工ID',
  `education`          VARCHAR(20)  DEFAULT NULL            COMMENT '学历(0高中 1大专 2本科 3硕士 4博士)',
  `major`              VARCHAR(100) DEFAULT NULL            COMMENT '专业',
  `graduate_school`    VARCHAR(100) DEFAULT NULL            COMMENT '毕业院校',
  `entry_date`         DATE         DEFAULT NULL            COMMENT '入职日期',
  `probation_end_date` DATE         DEFAULT NULL            COMMENT '试用期结束日期',
  `contract_start`     DATE         DEFAULT NULL            COMMENT '合同开始日期',
  `contract_end`       DATE         DEFAULT NULL            COMMENT '合同结束日期',
  `emergency_contact`  VARCHAR(64)  DEFAULT NULL            COMMENT '紧急联系人',
  `emergency_phone`    VARCHAR(20)  DEFAULT NULL            COMMENT '紧急联系人电话',
  `address`            VARCHAR(300) DEFAULT NULL            COMMENT '住址',
  `remark`             VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`           CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工档案表';


-- ============================================================================
-- 五、监控日志表 (Monitoring Tables)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 5.1 操作日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_operation_log`;
CREATE TABLE `oa_operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `emp_id`      BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `emp_name`    VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',
  `module`      VARCHAR(64)  DEFAULT NULL            COMMENT '操作模块',
  `operation`   VARCHAR(200) DEFAULT NULL            COMMENT '操作内容',
  `method`      VARCHAR(200) DEFAULT NULL            COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL            COMMENT '请求URL',
  `ip`          VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
  `status`      INT          DEFAULT 0             COMMENT '状态(0正常 1异常)',
  `cost_time`   BIGINT       DEFAULT 0               COMMENT '耗时(毫秒)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ---------------------------------------------------------------------------
-- 5.2 登录日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_login_log`;
CREATE TABLE `oa_login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `emp_id`      BIGINT       DEFAULT NULL            COMMENT '员工ID',
  `username`    VARCHAR(64)  DEFAULT NULL            COMMENT '用户名',
  `ip`          VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
  `browser`     VARCHAR(100) DEFAULT NULL            COMMENT '浏览器',
  `os`          VARCHAR(100) DEFAULT NULL            COMMENT '操作系统',
  `status`      INT          DEFAULT 0             COMMENT '状态(0成功 1失败)',
  `message`     VARCHAR(500) DEFAULT NULL            COMMENT '提示消息',
  `login_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ---------------------------------------------------------------------------
-- 5.3 审批记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_approval_record`;
CREATE TABLE `oa_approval_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
  `apply_id`        BIGINT       NOT NULL                COMMENT '业务ID',
  `business_type`   VARCHAR(32)  NOT NULL                COMMENT '业务类型',
  `approver_id`     BIGINT       NOT NULL                COMMENT '审批人ID',
  `delegator_id`    BIGINT       DEFAULT NULL            COMMENT '委托人ID（委托审批时记录原审批人）',
  `approve_status`  INT          NOT NULL                COMMENT '审批状态(0通过 1驳回 2转办 3退回 4撤回 5催办)',
  `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `approve_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `task_id`         BIGINT       DEFAULT NULL            COMMENT '任务ID',
  `node_name`       VARCHAR(100) DEFAULT NULL            COMMENT '节点名称',
  PRIMARY KEY (`id`),
  KEY `idx_apply` (`business_type`, `apply_id`),
  KEY `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批记录表';

-- ---------------------------------------------------------------------------
-- 5.4 告警规则表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rpt_alert_rule`;
CREATE TABLE `rpt_alert_rule` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_name`      VARCHAR(200) NOT NULL                COMMENT '规则名称',
  `rule_type`      VARCHAR(64)  DEFAULT NULL            COMMENT '规则类型',
  `metric`         VARCHAR(128) DEFAULT NULL            COMMENT '监控指标',
  `condition_type` VARCHAR(32)  DEFAULT NULL            COMMENT '条件类型(gt/lt/eq/between)',
  `threshold`      DECIMAL(18,2) NOT NULL               COMMENT '阈值',
  `threshold_max`  DECIMAL(18,2) DEFAULT NULL           COMMENT '上限阈值(between时使用)',
  `check_cron`     VARCHAR(64)  DEFAULT NULL            COMMENT '检查周期Cron',
  `notify_type`    VARCHAR(32)  DEFAULT 'inner'         COMMENT '通知方式(inner/email/sms)',
  `notify_targets` VARCHAR(500) DEFAULT NULL            COMMENT '通知目标',
  `status`         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`       INT          NOT NULL DEFAULT 0      COMMENT '删除标志(0存在 1删除)',
  `create_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警规则表';

-- ---------------------------------------------------------------------------
-- 5.5 告警日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `rpt_alert_log`;
CREATE TABLE `rpt_alert_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `rule_id`        BIGINT       NOT NULL                COMMENT '规则ID',
  `alert_level`    CHAR(1)      DEFAULT '0'             COMMENT '告警级别(0低 1中 2高)',
  `metric_value`   DECIMAL(18,2) DEFAULT NULL           COMMENT '指标值',
  `threshold`      DECIMAL(18,2) DEFAULT NULL           COMMENT '阈值',
  `alert_content`  TEXT         DEFAULT NULL            COMMENT '告警内容',
  `notify_status`  CHAR(1)      DEFAULT '0'             COMMENT '通知状态(0未通知 1已通知)',
  `handle_status`  CHAR(1)      DEFAULT '0'             COMMENT '处理状态(0未处理 1已处理)',
  `handler`        VARCHAR(64)  DEFAULT NULL            COMMENT '处理人',
  `handle_remark`  VARCHAR(500) DEFAULT NULL            COMMENT '处理备注',
  `alert_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
  `handle_time`    DATETIME     DEFAULT NULL            COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警日志表';

-- ============================================================================
-- 六、索引优化 (Performance Indexes)
-- ============================================================================

-- oa_leave_apply: 按员工+状态查询审批列表，按日期范围查询
ALTER TABLE `oa_leave_apply` ADD INDEX `idx_leave_emp_status` (`emp_id`, `status`);
ALTER TABLE `oa_leave_apply` ADD INDEX `idx_leave_date_range` (`start_time`, `end_time`);

-- oa_business_trip: 按员工+状态查询出差审批列表
ALTER TABLE `oa_business_trip` ADD INDEX `idx_trip_emp_status` (`emp_id`, `status`);

-- oa_expense: 按员工+状态查询报销审批列表
ALTER TABLE `oa_expense` ADD INDEX `idx_expense_emp_status` (`emp_id`, `status`);

-- oa_purchase: 按员工+状态查询采购审批列表
ALTER TABLE `oa_purchase` ADD INDEX `idx_purchase_emp_status` (`emp_id`, `status`);

-- oa_overtime: 按员工+状态查询加班审批列表
ALTER TABLE `oa_overtime` ADD INDEX `idx_overtime_emp_status` (`emp_id`, `status`);

-- oa_loan: 按员工+状态查询借款审批列表
ALTER TABLE `oa_loan` ADD INDEX `idx_loan_emp_status` (`emp_id`, `status`);
