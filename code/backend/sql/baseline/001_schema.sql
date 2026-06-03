-- ============================================================================
-- 企业OA办公系统 — 数据库基线 DDL
-- Database: oa_system | Charset: utf8mb4 | Collation: utf8mb4_general_ci
-- MySQL 8.0+ | Engine: InnoDB
--
-- 说明：
--   所有表统一使用 BIGINT AUTO_INCREMENT 主键，
--   del_flag 为逻辑删除（0=存在, 1=删除），
--   create_by / create_time / update_by / update_time 为审计字段。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `oa_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `oa_system`;

-- ============================================================================
-- 第一类：系统管理表 (sys_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1.1 员工表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_employee`;
CREATE TABLE `sys_employee` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `emp_code`    VARCHAR(32)  NOT NULL                COMMENT '工号(登录账号)',
  `emp_name`    VARCHAR(64)  NOT NULL                COMMENT '姓名',
  `password`    VARCHAR(128) NOT NULL                COMMENT '密码(BCrypt加密)',
  `real_name`   VARCHAR(64)  DEFAULT NULL            COMMENT '真实姓名',
  `email`       VARCHAR(128) DEFAULT NULL            COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `avatar`      VARCHAR(512) DEFAULT NULL            COMMENT '头像URL',
  `gender`      CHAR(1)      DEFAULT '0'             COMMENT '性别(0男 1女 2未知)',
  `dept_id`     BIGINT       DEFAULT NULL            COMMENT '部门ID',
  `post_id`     BIGINT       DEFAULT NULL            COMMENT '岗位ID',
  `status`      INT          NOT NULL DEFAULT 1      COMMENT '状态(1正常 0停用)',
  `hire_date`   DATE         DEFAULT NULL            COMMENT '入职日期',
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
  `leader_id`   BIGINT       DEFAULT NULL            COMMENT '部门负责人ID(关联sys_employee)',
  `leader`      VARCHAR(64)  DEFAULT NULL            COMMENT '负责人姓名',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
  `status`      INT          NOT NULL DEFAULT 0      COMMENT '状态(0正常 1停用)',
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
  `role_code`   VARCHAR(64)  NOT NULL                COMMENT '角色编码',
  `role_name`   VARCHAR(64)  NOT NULL                COMMENT '角色名称',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '角色描述',
  `role_sort`   INT          DEFAULT 0               COMMENT '显示顺序',
  `status`      INT          NOT NULL DEFAULT 0      COMMENT '状态(0正常 1停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_name` (`role_name`)
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
  `menu_type`   CHAR(1)      NOT NULL                COMMENT '菜单类型(M目录 C菜单 F按钮)',
  `path`        VARCHAR(200) DEFAULT NULL            COMMENT '路由地址',
  `component`   VARCHAR(200) DEFAULT NULL            COMMENT '组件路径',
  `perms`       VARCHAR(100) DEFAULT NULL            COMMENT '权限标识',
  `icon`        VARCHAR(100) DEFAULT NULL            COMMENT '菜单图标',
  `order_num`   INT          DEFAULT 0               COMMENT '显示顺序',
  `roles`       JSON         DEFAULT NULL            COMMENT '可见角色标识数组JSON',
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
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_name`   VARCHAR(100) NOT NULL                COMMENT '字典名称',
  `dict_code`   VARCHAR(100) NOT NULL                COMMENT '字典编码(唯一标识)',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `remark`      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- ---------------------------------------------------------------------------
-- 1.8 字典数据表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_code`   VARCHAR(100) NOT NULL                COMMENT '字典编码(关联sys_dict_type.dict_code)',
  `dict_label`  VARCHAR(200) NOT NULL                COMMENT '字典标签',
  `dict_value`  VARCHAR(200) NOT NULL                COMMENT '字典值',
  `sort_order`  INT          DEFAULT 0               COMMENT '排序',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `default_flag` CHAR(1)     DEFAULT '0'             COMMENT '是否默认(0否 1是)',
  `remark`      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- ---------------------------------------------------------------------------
-- 1.9 系统配置表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name`  VARCHAR(100) NOT NULL                COMMENT '配置名称',
  `config_key`   VARCHAR(100) NOT NULL                COMMENT '配置键名',
  `config_value` TEXT         DEFAULT NULL            COMMENT '配置值',
  `config_type`  CHAR(1)      DEFAULT '0'             COMMENT '系统内置(0是 1否)',
  `remark`       VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `status`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `del_flag`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- ---------------------------------------------------------------------------
-- 1.10 岗位表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code`   VARCHAR(64)  NOT NULL                COMMENT '岗位编码',
  `post_name`   VARCHAR(100) NOT NULL                COMMENT '岗位名称',
  `sort_order`  INT          DEFAULT 0               COMMENT '显示顺序',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';


-- ============================================================================
-- 第二类：工作流表 (wf_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 2.1 流程定义表
-- 存储流程模版的元信息，不再混合存储节点JSON配置。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_definition`;
CREATE TABLE `wf_definition` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `def_name`      VARCHAR(200) NOT NULL                COMMENT '流程定义名称',
  `def_key`       VARCHAR(64)  NOT NULL                COMMENT '流程定义唯一标识(编码)',
  `process_type`  VARCHAR(32)  NOT NULL                COMMENT '流程类型(leave/trip/outing/purchase/expense/overtime/loan/contract)',
  `category`      VARCHAR(64)  DEFAULT NULL            COMMENT '流程分类',
  `version`       INT          NOT NULL DEFAULT 1      COMMENT '版本号',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `remarks`       TEXT         DEFAULT NULL            COMMENT '描述/备注',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_key` (`def_key`),
  KEY `idx_process_type` (`process_type`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程定义表';

-- ---------------------------------------------------------------------------
-- 2.2 流程节点表
-- 每个节点独立记录，支持条件路由、多实例、超时、抄送等配置。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_node`;
CREATE TABLE `wf_node` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `def_id`          BIGINT       NOT NULL                COMMENT '流程定义ID',
  `node_index`      INT          NOT NULL                COMMENT '节点序号(从0开始)',
  `node_name`       VARCHAR(100) NOT NULL                COMMENT '节点名称',
  `node_type`       VARCHAR(32)  NOT NULL DEFAULT 'approval' COMMENT '节点类型(start/approval/subprocess/condition/gateway/end)',
  `assignee_type`   VARCHAR(32)  DEFAULT 'specific'      COMMENT '审批人类型(specific/role/role_global/dept_manager/dept_leader/self)',
  `assignee_ids`    VARCHAR(500) DEFAULT NULL            COMMENT '指定审批人ID列表(逗号分隔)',
  `role_codes`      VARCHAR(200) DEFAULT NULL            COMMENT '角色编码列表(逗号分隔)',
  `multi_type`      VARCHAR(32)  DEFAULT NULL            COMMENT '多人审批方式(countersign/orsign/null)',
  `timeout_hours`   INT          DEFAULT NULL            COMMENT '超时阈值(小时)',
  `timeout_action`  VARCHAR(32)  DEFAULT NULL            COMMENT '超时动作(escalate/auto_pass/notify)',
  `allow_return`    CHAR(1)      DEFAULT '0'             COMMENT '允许退回(0否 1是)',
  `return_target`   INT          DEFAULT NULL            COMMENT '退回目标节点索引',
  `cc_list`         TEXT         DEFAULT NULL            COMMENT '抄送人JSON(数组)',
  `form_permission` TEXT         DEFAULT NULL            COMMENT '表单字段权限JSON',
  `sort_order`      INT          DEFAULT 0               COMMENT '排序(同节点序号下)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_def_id` (`def_id`),
  KEY `idx_node_index` (`def_id`, `node_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程节点表';

-- ---------------------------------------------------------------------------
-- 2.3 流转连线表
-- 定义节点之间的有向边，以及条件路由规则。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_transition`;
CREATE TABLE `wf_transition` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '连线ID',
  `def_id`          BIGINT       NOT NULL                COMMENT '流程定义ID',
  `from_node_index` INT          NOT NULL                COMMENT '来源节点序号',
  `to_node_index`   INT          NOT NULL                COMMENT '目标节点序号',
  `condition_type`  VARCHAR(32)  DEFAULT NULL            COMMENT '条件类型(expression/groovy/always)',
  `condition_expr`  TEXT         DEFAULT NULL            COMMENT '条件表达式(SpEL或脚本)',
  `priority`        INT          DEFAULT 0               COMMENT '优先级(值越小优先级越高)',
  `description`     VARCHAR(200) DEFAULT NULL            COMMENT '连线描述',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_def_id` (`def_id`),
  KEY `idx_from_node` (`def_id`, `from_node_index`),
  KEY `idx_to_node` (`def_id`, `to_node_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流转连线表';

-- ---------------------------------------------------------------------------
-- 2.4 审批人规则表
-- 弹性配置审批人查找规则，支持动态维度组合。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_assignee_rule`;
CREATE TABLE `wf_assignee_rule` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `def_id`          BIGINT       NOT NULL                COMMENT '流程定义ID',
  `node_index`      INT          NOT NULL                COMMENT '节点序号',
  `rule_type`       VARCHAR(32)  NOT NULL                COMMENT '规则类型(specific/role/dept_leader/dept_manager/org_head/self/department/post)',
  `rule_source`     VARCHAR(500) DEFAULT NULL            COMMENT '规则源(角色编码/岗位编码/固定人员ID)',
  `selector`        VARCHAR(64)  DEFAULT NULL            COMMENT '选择器(可选: first/multi/all)',
  `exclude_self`    CHAR(1)      DEFAULT '0'             COMMENT '排除发起人(0否 1是)',
  `priority`        INT          DEFAULT 0               COMMENT '优先级(值越小优先级越高)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_def_node` (`def_id`, `node_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批人规则表';

-- ---------------------------------------------------------------------------
-- 2.5 流程实例表
-- 运行时流程实例，追踪执行状态和上下文。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_instance`;
CREATE TABLE `wf_instance` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `def_id`             BIGINT       NOT NULL                COMMENT '流程定义ID',
  `def_version`        INT          NOT NULL DEFAULT 1      COMMENT '流程定义版本号(快照)',
  `business_type`      VARCHAR(32)  NOT NULL                COMMENT '业务类型(leave/trip/...)',
  `business_id`        BIGINT       NOT NULL                COMMENT '业务ID',
  `initiator_id`       BIGINT       NOT NULL                COMMENT '发起人ID',
  `current_node_index` INT          DEFAULT NULL            COMMENT '当前节点序号',
  `active_nodes`       VARCHAR(500) DEFAULT NULL            COMMENT '活动节点序号列表(并行,逗号分隔)',
  `status`             CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0运行中 1已通过 2已驳回 3已撤回 4已终止)',
  `condition_context`  JSON         DEFAULT NULL            COMMENT '条件上下文(JSON,用于条件路由计算)',
  `snapshot_config`    JSON         DEFAULT NULL            COMMENT '启动时节点配置快照',
  `start_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `end_time`           DATETIME     DEFAULT NULL            COMMENT '结束时间',
  `parent_instance_id` BIGINT       DEFAULT NULL            COMMENT '父流程实例ID(子流程场景)',
  `del_flag`           CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_def_id` (`def_id`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_parent_instance` (`parent_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流程实例表';

-- ---------------------------------------------------------------------------
-- 2.6 工作任务表
-- 每个待办审批事项对应一条记录。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_task`;
CREATE TABLE `wf_task` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id`       BIGINT       NOT NULL                COMMENT '流程实例ID',
  `def_id`            BIGINT       NOT NULL                COMMENT '流程定义ID',
  `node_index`        INT          NOT NULL                COMMENT '节点序号',
  `node_name`         VARCHAR(100) DEFAULT NULL            COMMENT '节点名称',
  `assignee_id`       BIGINT       NOT NULL                COMMENT '审批人ID',
  `original_assignee_id` BIGINT    DEFAULT NULL            COMMENT '原审批人ID(转办时记录)',
  `parent_task_id`    BIGINT       DEFAULT NULL            COMMENT '父任务ID(会签/或签场景)',
  `multi_type`        VARCHAR(32)  DEFAULT NULL            COMMENT '多人审批方式(countersign/orsign)',
  `multi_count`       INT          DEFAULT NULL            COMMENT '会签总人数',
  `multi_completed`   INT          DEFAULT 0               COMMENT '会签已完成数',
  `status`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已通过 2已驳回 3已转办 4已退回 5已取消)',
  `action_time`       DATETIME     DEFAULT NULL            COMMENT '操作时间',
  `remark`            VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `deadline`          DATETIME     DEFAULT NULL            COMMENT '处理截止时间',
  `remind_count`      INT          DEFAULT 0               COMMENT '催办次数',
  `last_remind_time`  DATETIME     DEFAULT NULL            COMMENT '最后催办时间',
  `transfer_reason`   VARCHAR(500) DEFAULT NULL            COMMENT '转办/退回原因',
  `del_flag`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`         VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_parent_task` (`parent_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作任务表';

-- ---------------------------------------------------------------------------
-- 2.7 流转记录表
-- 统一记录审批、转办、退回、催办、抄送等所有操作历史。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_record`;
CREATE TABLE `wf_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `instance_id`   BIGINT       NOT NULL                COMMENT '流程实例ID',
  `task_id`       BIGINT       DEFAULT NULL            COMMENT '任务ID',
  `record_type`   VARCHAR(32)  NOT NULL                COMMENT '记录类型(approve/reject/transfer/return/remind/cc/cancel/delegate)',
  `operator_id`   BIGINT       NOT NULL                COMMENT '操作人ID',
  `operator_name` VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',
  `target_id`     BIGINT       DEFAULT NULL            COMMENT '目标人ID(转办/退回/委托)',
  `node_index`    INT          DEFAULT NULL            COMMENT '节点序号',
  `node_name`     VARCHAR(100) DEFAULT NULL            COMMENT '节点名称',
  `content`       TEXT         DEFAULT NULL            COMMENT '操作内容/审批意见',
  `extra_info`    JSON         DEFAULT NULL            COMMENT '扩展信息JSON',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='流转记录表';

-- ---------------------------------------------------------------------------
-- 2.8 审批委托表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `wf_delegation`;
CREATE TABLE `wf_delegation` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `delegator_id`   BIGINT       NOT NULL                COMMENT '委托人ID',
  `delegate_id`    BIGINT       NOT NULL                COMMENT '被委托人ID',
  `business_type`  VARCHAR(32)  DEFAULT NULL            COMMENT '业务类型(NULL表示全部)',
  `process_type`   VARCHAR(32)  DEFAULT NULL            COMMENT '流程类型(NULL表示全部)',
  `start_time`     DATETIME     NOT NULL                COMMENT '生效时间',
  `end_time`       DATETIME     NOT NULL                COMMENT '失效时间',
  `reason`         VARCHAR(500) DEFAULT NULL            COMMENT '委托原因',
  `status`         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0生效中 1已失效 2已取消)',
  `del_flag`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_delegator_id` (`delegator_id`),
  KEY `idx_delegate_id` (`delegate_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批委托表';


-- ============================================================================
-- 第三类：公文管理表 (doc_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 发文表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_dispatch`;
CREATE TABLE `doc_dispatch` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发文ID',
  `doc_no`          VARCHAR(64)  DEFAULT NULL            COMMENT '文号',
  `title`           VARCHAR(500) NOT NULL                COMMENT '公文标题',
  `urgency`         CHAR(1)      DEFAULT '0'             COMMENT '紧急程度(0普通 1加急 2特急)',
  `security_level`  CHAR(1)      DEFAULT '0'             COMMENT '密级(0普通 1机密 2秘密 3绝密)',
  `keywords`        VARCHAR(200) DEFAULT NULL            COMMENT '关键词',
  `content`         LONGTEXT     DEFAULT NULL            COMMENT '正文内容(HTML或Markdown)',
  `attachment_ids`  TEXT         DEFAULT NULL            COMMENT '附件ID列表(JSON数组)',
  `issuer`          VARCHAR(64)  DEFAULT NULL            COMMENT '签发人',
  `issuer_dept`     BIGINT       DEFAULT NULL            COMMENT '发文部门ID',
  `issuer_date`     DATE         DEFAULT NULL            COMMENT '签发日期',
  `copies`          INT          DEFAULT 0               COMMENT '印发份数',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0草稿 1待签发 2已签发 3已归档 4已撤回)',
  `process_instance_id` BIGINT   DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_no` (`doc_no`),
  KEY `idx_status` (`status`),
  KEY `idx_issuer_dept` (`issuer_dept`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发文表';

-- ---------------------------------------------------------------------------
-- 3.2 收文表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_receive`;
CREATE TABLE `doc_receive` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '收文ID',
  `doc_no`          VARCHAR(64)  DEFAULT NULL            COMMENT '收文编号',
  `source_doc_no`   VARCHAR(64)  DEFAULT NULL            COMMENT '来文编号(外部)',
  `title`           VARCHAR(500) NOT NULL                COMMENT '公文标题',
  `source_org`      VARCHAR(200) DEFAULT NULL            COMMENT '来文单位',
  `urgency`         CHAR(1)      DEFAULT '0'             COMMENT '紧急程度(0普通 1加急 2特急)',
  `security_level`  CHAR(1)      DEFAULT '0'             COMMENT '密级',
  `content`         LONGTEXT     DEFAULT NULL            COMMENT '正文内容',
  `attachment_ids`  TEXT         DEFAULT NULL            COMMENT '附件ID列表(JSON数组)',
  `receive_date`    DATETIME     DEFAULT NULL            COMMENT '收文日期',
  `receiver_id`     BIGINT       DEFAULT NULL            COMMENT '收文人ID',
  `filing_status`   CHAR(1)      DEFAULT '0'             COMMENT '归档状态(0未归档 1已归档)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待处理 1办理中 2已办结 3已归档)',
  `process_instance_id` BIGINT   DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_no` (`doc_no`),
  KEY `idx_status` (`status`),
  KEY `idx_source_org` (`source_org`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收文表';

-- ---------------------------------------------------------------------------
-- 3.3 文号表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_serial`;
CREATE TABLE `doc_serial` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `serial_code`     VARCHAR(64)  NOT NULL                COMMENT '文号编码(如 OFD-GS-[2025])',
  `serial_name`     VARCHAR(200) DEFAULT NULL            COMMENT '文号名称',
  `current_seq`     INT          NOT NULL DEFAULT 0      COMMENT '当前序号',
  `seq_length`      INT          DEFAULT 4               COMMENT '序号长度(不足补0)',
  `year`            INT          DEFAULT NULL            COMMENT '年份',
  `pattern`         VARCHAR(100) DEFAULT NULL            COMMENT '文号模板(如 {code}[{year}]{seq}号)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_serial_code_year` (`serial_code`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文号表';

-- ---------------------------------------------------------------------------
-- 3.4 公文修订记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_revision`;
CREATE TABLE `doc_revision` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '修订ID',
  `doc_type`        VARCHAR(32)  NOT NULL                COMMENT '公文类型(dispatch/receive)',
  `doc_id`          BIGINT       NOT NULL                COMMENT '公文ID',
  `revision_no`     INT          NOT NULL DEFAULT 1      COMMENT '修订版本号',
  `content_before`  LONGTEXT     DEFAULT NULL            COMMENT '修订前内容',
  `content_after`   LONGTEXT     DEFAULT NULL            COMMENT '修订后内容',
  `change_summary`  VARCHAR(500) DEFAULT NULL            COMMENT '变更摘要',
  `operator_id`     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc` (`doc_type`, `doc_id`),
  KEY `idx_revision_no` (`doc_id`, `revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文修订记录表';


-- ============================================================================
-- 第四类：知识库表 (km_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 4.1 知识条目表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `km_entry`;
CREATE TABLE `km_entry` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '条目ID',
  `title`           VARCHAR(200)  NOT NULL                COMMENT '标题',
  `content`         LONGTEXT      DEFAULT NULL            COMMENT '内容(Markdown或HTML)',
  `summary`         VARCHAR(500)  DEFAULT NULL            COMMENT '摘要',
  `category`        VARCHAR(64)   DEFAULT NULL            COMMENT '分类编码',
  `entry_type`      VARCHAR(32)   DEFAULT 'article'       COMMENT '条目类型(article/file/link/video)',
  `file_url`        VARCHAR(512)  DEFAULT NULL            COMMENT '附件URL',
  `cover_url`       VARCHAR(512)  DEFAULT NULL            COMMENT '封面图URL',
  `author_id`       BIGINT        DEFAULT NULL            COMMENT '作者ID',
  `author_name`     VARCHAR(64)   DEFAULT NULL            COMMENT '作者姓名',
  `status`          CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '状态(0草稿 1已发布 2已归档)',
  `is_top`          CHAR(1)       DEFAULT '0'             COMMENT '是否置顶(0否 1是)',
  `view_count`      INT           DEFAULT 0               COMMENT '浏览次数',
  `like_count`      INT           DEFAULT 0               COMMENT '点赞次数',
  `publish_time`    DATETIME      DEFAULT NULL            COMMENT '发布时间',
  `del_flag`        CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_author_id` (`author_id`),
  FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识条目表';

-- ---------------------------------------------------------------------------
-- 4.2 知识版本表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `km_version`;
CREATE TABLE `km_version` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `entry_id`        BIGINT       NOT NULL                COMMENT '知识条目ID',
  `version_no`      INT          NOT NULL DEFAULT 1      COMMENT '版本号',
  `title`           VARCHAR(200) DEFAULT NULL            COMMENT '版本标题',
  `content`         LONGTEXT     DEFAULT NULL            COMMENT '版本内容(快照)',
  `summary`         VARCHAR(500) DEFAULT NULL            COMMENT '版本摘要',
  `change_log`      VARCHAR(500) DEFAULT NULL            COMMENT '变更日志',
  `file_url`        VARCHAR(512) DEFAULT NULL            COMMENT '版本附件',
  `operator_id`     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `operator_name`   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_entry_id` (`entry_id`),
  KEY `idx_version` (`entry_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识版本表';

-- ---------------------------------------------------------------------------
-- 4.3 知识标签表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `km_tag`;
CREATE TABLE `km_tag` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name`    VARCHAR(64)  NOT NULL                COMMENT '标签名称',
  `tag_color`   VARCHAR(32)  DEFAULT NULL            COMMENT '标签颜色(HEX)',
  `usage_count` INT          DEFAULT 0               COMMENT '使用次数',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识标签表';

-- ---------------------------------------------------------------------------
-- 4.4 知识关联表
-- 维护知识条目之间的关联关系（父子/引用/推荐）。
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `km_relation`;
CREATE TABLE `km_relation` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `entry_id`      BIGINT       NOT NULL                COMMENT '知识条目ID(源)',
  `related_id`    BIGINT       NOT NULL                COMMENT '关联条目ID(目标)',
  `relation_type` VARCHAR(32)  NOT NULL DEFAULT 'reference' COMMENT '关联类型(parent/child/reference/recommend)',
  `sort_order`    INT          DEFAULT 0               COMMENT '排序',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_related` (`entry_id`, `related_id`, `relation_type`),
  KEY `idx_related_id` (`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识关联表';


-- ============================================================================
-- 第五类：会议管理表 (mt_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 5.1 会议室表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `mt_room`;
CREATE TABLE `mt_room` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会议室ID',
  `name`        VARCHAR(100) NOT NULL                COMMENT '会议室名称',
  `capacity`    INT          DEFAULT 0               COMMENT '容纳人数',
  `devices`     VARCHAR(500) DEFAULT NULL            COMMENT '设备列表(投影仪/视频会议/白板等,逗号分隔)',
  `location`    VARCHAR(200) DEFAULT NULL            COMMENT '位置',
  `gps`         VARCHAR(64)  DEFAULT NULL            COMMENT 'GPS坐标',
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
-- 5.2 会议预订表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `mt_booking`;
CREATE TABLE `mt_booking` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预订ID',
  `room_id`       BIGINT       NOT NULL                COMMENT '会议室ID',
  `title`         VARCHAR(200) NOT NULL                COMMENT '会议主题',
  `description`   TEXT         DEFAULT NULL            COMMENT '会议描述',
  `organizer_id`  BIGINT       NOT NULL                COMMENT '组织者ID',
  `start_time`    DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`      DATETIME     NOT NULL                COMMENT '结束时间',
  `participants`  JSON         DEFAULT NULL            COMMENT '参会人员ID数组JSON',
  `meeting_type`  VARCHAR(32)  DEFAULT 'normal'        COMMENT '会议类型(normal/online/hybrid)',
  `meeting_url`   VARCHAR(512) DEFAULT NULL            COMMENT '线上会议链接',
  `attachment_ids` TEXT        DEFAULT NULL            COMMENT '附件ID列表(JSON)',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待开始 1进行中 2已结束 3已取消)',
  `cancel_reason` VARCHAR(500) DEFAULT NULL            COMMENT '取消原因',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_organizer_id` (`organizer_id`),
  KEY `idx_time_range` (`start_time`, `end_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议预订表';

-- ---------------------------------------------------------------------------
-- 5.3 会议决议/待办表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `mt_resolution`;
CREATE TABLE `mt_resolution` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '决议ID',
  `booking_id`    BIGINT       NOT NULL                COMMENT '会议预订ID',
  `content`       TEXT         NOT NULL                COMMENT '决议/待办内容',
  `assignee_id`   BIGINT       DEFAULT NULL            COMMENT '负责人ID',
  `due_date`      DATE         DEFAULT NULL            COMMENT '截止日期',
  `priority`      CHAR(1)      DEFAULT '0'             COMMENT '优先级(0普通 1重要 2紧急)',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待办 1进行中 2已完成 3已关闭)',
  `task_id`       BIGINT       DEFAULT NULL            COMMENT '关联任务ID(task_item)',
  `complete_time` DATETIME     DEFAULT NULL            COMMENT '完成时间',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议决议/待办表';

-- ---------------------------------------------------------------------------
-- 5.4 会议签到表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `mt_signin`;
CREATE TABLE `mt_signin` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `booking_id`    BIGINT       NOT NULL                COMMENT '会议预订ID',
  `emp_id`        BIGINT       NOT NULL                COMMENT '员工ID',
  `signin_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `signin_type`   VARCHAR(32)  DEFAULT 'normal'        COMMENT '签到类型(normal/late/leave_early/absent)',
  `location`      VARCHAR(200) DEFAULT NULL            COMMENT '签到地点',
  `device`        VARCHAR(200) DEFAULT NULL            COMMENT '签到设备',
  `remarks`       VARCHAR(200) DEFAULT NULL            COMMENT '备注',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_emp` (`booking_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会议签到表';


-- ============================================================================
-- 第六类：任务项目管理表 (task_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 6.1 项目表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_project`;
CREATE TABLE `task_project` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_name`  VARCHAR(200) NOT NULL                COMMENT '项目名称',
  `project_code`  VARCHAR(64)  DEFAULT NULL            COMMENT '项目编码',
  `description`   TEXT         DEFAULT NULL            COMMENT '项目描述',
  `start_date`    DATE         DEFAULT NULL            COMMENT '开始日期',
  `end_date`      DATE         DEFAULT NULL            COMMENT '结束日期',
  `owner_id`      BIGINT       DEFAULT NULL            COMMENT '负责人ID',
  `priority`      CHAR(1)      DEFAULT '0'             COMMENT '优先级(0普通 1重要 2紧急)',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0未开始 1进行中 2已完成 3已暂停 4已取消)',
  `progress`      INT          DEFAULT 0               COMMENT '进度百分比(0-100)',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目表';

-- ---------------------------------------------------------------------------
-- 6.2 项目成员表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_project_member`;
CREATE TABLE `task_project_member` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id`  BIGINT       NOT NULL                COMMENT '项目ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '员工ID',
  `role`        VARCHAR(32)  DEFAULT 'member'        COMMENT '角色(owner/manager/member/guest)',
  `join_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_emp` (`project_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目成员表';

-- ---------------------------------------------------------------------------
-- 6.3 任务表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_item`;
CREATE TABLE `task_item` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id`    BIGINT       DEFAULT NULL            COMMENT '项目ID',
  `parent_id`     BIGINT       DEFAULT NULL            COMMENT '父任务ID',
  `title`         VARCHAR(200) NOT NULL                COMMENT '任务标题',
  `description`   TEXT         DEFAULT NULL            COMMENT '任务描述',
  `assignee_id`   BIGINT       DEFAULT NULL            COMMENT '负责人ID',
  `priority`      CHAR(1)      DEFAULT '0'             COMMENT '优先级(0普通 1重要 2紧急)',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待办 1进行中 2已完成 3已暂停 4已取消)',
  `progress`      INT          DEFAULT 0               COMMENT '进度百分比(0-100)',
  `start_date`    DATE         DEFAULT NULL            COMMENT '开始日期',
  `due_date`      DATE         DEFAULT NULL            COMMENT '截止日期',
  `completed_date` DATETIME    DEFAULT NULL            COMMENT '完成时间',
  `story_points`  DECIMAL(5,1) DEFAULT NULL            COMMENT '故事点',
  `sort_order`    INT          DEFAULT 0               COMMENT '排序',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_assignee_id` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务表';

-- ---------------------------------------------------------------------------
-- 6.4 任务依赖表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_dependency`;
CREATE TABLE `task_dependency` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `task_id`         BIGINT       NOT NULL                COMMENT '当前任务ID',
  `depends_on_id`   BIGINT       NOT NULL                COMMENT '依赖任务ID(前置任务)',
  `dependency_type` VARCHAR(32)  DEFAULT 'finish_to_start' COMMENT '依赖类型(finish_to_start/start_to_start/finish_to_finish)',
  `lag_days`        INT          DEFAULT 0               COMMENT '滞后天数',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_dep` (`task_id`, `depends_on_id`),
  KEY `idx_depends_on` (`depends_on_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务依赖表';

-- ---------------------------------------------------------------------------
-- 6.5 任务工时表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_hours`;
CREATE TABLE `task_hours` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `task_id`     BIGINT       NOT NULL                COMMENT '任务ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '员工ID',
  `work_date`   DATE         NOT NULL                COMMENT '工作日期',
  `hours`       DECIMAL(5,1) NOT NULL                COMMENT '工时(小时)',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '工作内容描述',
  `overtime_flag` CHAR(1)    DEFAULT '0'             COMMENT '是否加班(0否 1是)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_work_date` (`work_date`),
  UNIQUE KEY `uk_task_emp_date` (`task_id`, `emp_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务工时表';

-- ---------------------------------------------------------------------------
-- 6.6 任务评论表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `task_comment`;
CREATE TABLE `task_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `task_id`     BIGINT       NOT NULL                COMMENT '任务ID',
  `parent_id`   BIGINT       DEFAULT NULL            COMMENT '父评论ID(回复场景)',
  `content`     TEXT         NOT NULL                COMMENT '评论内容',
  `commenter_id` BIGINT      DEFAULT NULL            COMMENT '评论人ID',
  `attachment_ids` TEXT      DEFAULT NULL            COMMENT '附件ID列表(JSON)',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_commenter_id` (`commenter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务评论表';


-- ============================================================================
-- 第七类：人力资源表 (hr_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 7.1 员工扩展信息表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_employee_ext`;
CREATE TABLE `hr_employee_ext` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`            BIGINT       NOT NULL                COMMENT '员工ID',
  `entry_date`        DATE         DEFAULT NULL            COMMENT '入职日期',
  `probation_end`     DATE         DEFAULT NULL            COMMENT '试用期截止日',
  `contract_end`      DATE         DEFAULT NULL            COMMENT '合同到期日',
  `emergency_contact` VARCHAR(64)  DEFAULT NULL            COMMENT '紧急联系人',
  `emergency_phone`   VARCHAR(20)  DEFAULT NULL            COMMENT '紧急联系电话',
  `education`         VARCHAR(32)  DEFAULT NULL            COMMENT '学历(高中/大专/本科/硕士/博士)',
  `major`             VARCHAR(100) DEFAULT NULL            COMMENT '专业',
  `graduate_school`   VARCHAR(200) DEFAULT NULL            COMMENT '毕业院校',
  `national_id`       VARCHAR(32)  DEFAULT NULL            COMMENT '身份证号',
  `birth_date`        DATE         DEFAULT NULL            COMMENT '出生日期',
  `address`           VARCHAR(300) DEFAULT NULL            COMMENT '家庭住址',
  `emergency_relation` VARCHAR(32) DEFAULT NULL            COMMENT '紧急联系人关系',
  `employee_type`     VARCHAR(32)  DEFAULT 'fulltime'      COMMENT '员工类型(fulltime/parttime/intern/contractor)',
  `work_status`       VARCHAR(32)  DEFAULT 'active'        COMMENT '工作状态(active/leave/suspended/retired)',
  `remark`            VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`         VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_id` (`emp_id`),
  KEY `idx_work_status` (`work_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工扩展信息表';

-- ---------------------------------------------------------------------------
-- 7.2 员工异动表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_transfer`;
CREATE TABLE `hr_transfer` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '异动ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `transfer_type`   VARCHAR(32)  NOT NULL                COMMENT '异动类型(transfer/promotion/demotion/rotation/quit)',
  `from_dept_id`    BIGINT       DEFAULT NULL            COMMENT '原部门ID',
  `to_dept_id`      BIGINT       DEFAULT NULL            COMMENT '新部门ID',
  `from_dept_name`  VARCHAR(100) DEFAULT NULL            COMMENT '原部门名称',
  `to_dept_name`    VARCHAR(100) DEFAULT NULL            COMMENT '新部门名称',
  `from_post_id`    BIGINT       DEFAULT NULL            COMMENT '原岗位ID',
  `to_post_id`      BIGINT       DEFAULT NULL            COMMENT '新岗位ID',
  `from_post_name`  VARCHAR(100) DEFAULT NULL            COMMENT '原岗位名称',
  `to_post_name`    VARCHAR(100) DEFAULT NULL            COMMENT '新岗位名称',
  `reason`          VARCHAR(500) DEFAULT NULL            COMMENT '异动原因',
  `effective_date`  DATE         NOT NULL                COMMENT '生效日期',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待生效 1已生效 2已取消)',
  `process_instance_id` BIGINT   DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_transfer_type` (`transfer_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工异动表';

-- ---------------------------------------------------------------------------
-- 7.3 考勤打卡记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance`;
CREATE TABLE `hr_attendance` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `emp_id`        BIGINT       NOT NULL                COMMENT '员工ID',
  `clock_date`    DATE         NOT NULL                COMMENT '考勤日期',
  `clock_in_time` DATETIME     DEFAULT NULL            COMMENT '签到时间',
  `clock_out_time` DATETIME    DEFAULT NULL            COMMENT '签退时间',
  `clock_in_type` VARCHAR(32)  DEFAULT 'normal'        COMMENT '签到类型(normal/late/early/none)',
  `clock_out_type` VARCHAR(32) DEFAULT 'normal'        COMMENT '签退类型(normal/late/early/none)',
  `work_status`   VARCHAR(32)  DEFAULT 'present'       COMMENT '出勤状态(present/absent/leave/business_trip/outing/overtime)',
  `dept_id`       BIGINT       DEFAULT NULL            COMMENT '部门ID',
  `remark`        VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date` (`emp_id`, `clock_date`),
  KEY `idx_clock_date` (`clock_date`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_work_status` (`work_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤打卡记录表';

-- ---------------------------------------------------------------------------
-- 7.4 请假申请表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_leave_apply`;
CREATE TABLE `hr_leave_apply` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `apply_no`            VARCHAR(64)  DEFAULT NULL            COMMENT '申请单号',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `dept_id`             BIGINT       DEFAULT NULL            COMMENT '部门ID',
  `leave_type`          VARCHAR(32)  NOT NULL                COMMENT '请假类型(annual/sick/personal/marriage/maternity/paternity/funeral/compassionate)',
  `start_time`          DATETIME     NOT NULL                COMMENT '开始时间',
  `end_time`            DATETIME     NOT NULL                COMMENT '结束时间',
  `leave_period`        VARCHAR(32)  DEFAULT 'fullday'       COMMENT '请假时段(fullday/morning/afternoon/custom)',
  `days`                DECIMAL(4,1) NOT NULL DEFAULT 0      COMMENT '请假天数',
  `reason`              VARCHAR(500) DEFAULT NULL            COMMENT '请假原因',
  `attachments`         TEXT         DEFAULT NULL            COMMENT '附件URL(JSON数组)',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态(pending/approved/rejected/canceled/withdrawn)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '流程实例ID',
  `current_task_id`     BIGINT       DEFAULT NULL            COMMENT '当前任务ID',
  `approved_time`       DATETIME     DEFAULT NULL            COMMENT '审批通过时间',
  `reject_reason`       VARCHAR(500) DEFAULT NULL            COMMENT '驳回原因',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='请假申请表';

-- ---------------------------------------------------------------------------
-- 7.5 假期余额表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_leave_balance`;
CREATE TABLE `hr_leave_balance` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`          BIGINT         NOT NULL                COMMENT '员工ID',
  `leave_type`      VARCHAR(32)    NOT NULL                COMMENT '假期类型(annual/sick/personal/marriage/...)',
  `year`            INT            NOT NULL                COMMENT '年度',
  `total_days`      DECIMAL(6,1)   NOT NULL DEFAULT 0      COMMENT '总额度(天)',
  `used_days`       DECIMAL(6,1)   NOT NULL DEFAULT 0      COMMENT '已用天数',
  `frozen_days`     DECIMAL(6,1)   DEFAULT 0               COMMENT '冻结天数(审批中占用)',
  `remaining_days`  DECIMAL(6,1)   GENERATED ALWAYS AS (total_days - used_days - frozen_days) STORED COMMENT '剩余天数(计算列)',
  `last_updated`    DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_year` (`emp_id`, `leave_type`, `year`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='假期余额表';

-- ---------------------------------------------------------------------------
-- 7.6 请假规则表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_leave_rule`;
CREATE TABLE `hr_leave_rule` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `leave_type`      VARCHAR(32)  NOT NULL                COMMENT '请假类型',
  `rule_name`       VARCHAR(100) DEFAULT NULL            COMMENT '规则名称',
  `min_days`        DECIMAL(4,1) DEFAULT 0               COMMENT '最小申请天数',
  `max_days`        DECIMAL(4,1) DEFAULT NULL            COMMENT '最大申请天数(NULL不限制)',
  `need_attachment` CHAR(1)      DEFAULT '0'             COMMENT '是否需要附件(0否 1是)',
  `deduct_balance`  CHAR(1)      DEFAULT '1'             COMMENT '是否扣减余额(0否 1是)',
  `unit`            VARCHAR(16)  DEFAULT 'day'           COMMENT '请假单位(day/hour/half_day)',
  `min_unit`        VARCHAR(16)  DEFAULT '0.5'           COMMENT '最小请假单位(0.5/1)',
  `max_consecutive` INT          DEFAULT NULL            COMMENT '最大连续天数(NULL不限制)',
  `allow_half_day`  CHAR(1)      DEFAULT '1'             COMMENT '允许半天请假(0否 1是)',
  `gender_restrict` VARCHAR(32)  DEFAULT NULL            COMMENT '性别限制(NULL不限)',
  `rule_script`     TEXT         DEFAULT NULL            COMMENT '规则脚本(Groovy/SpEL,动态校验)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `sort_order`      INT          DEFAULT 0               COMMENT '排序',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_leave_type` (`leave_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='请假规则表';


-- ============================================================================
-- 第八类：综合行政表 (adm_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 8.1 印章表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_seal`;
CREATE TABLE `adm_seal` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '印章ID',
  `seal_name`   VARCHAR(100) NOT NULL                COMMENT '印章名称',
  `seal_type`   VARCHAR(32)  NOT NULL                COMMENT '印章类型(company/chapter/finance/legal/personal)',
  `image_url`   VARCHAR(512) DEFAULT NULL            COMMENT '印章图片URL',
  `keeper_id`   BIGINT       DEFAULT NULL            COMMENT '保管人ID',
  `status`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用 2挂失 3报废)',
  `description` VARCHAR(500) DEFAULT NULL            COMMENT '备注说明',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_keeper_id` (`keeper_id`),
  KEY `idx_seal_type` (`seal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章表';

-- ---------------------------------------------------------------------------
-- 8.2 印章使用记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_seal_usage`;
CREATE TABLE `adm_seal_usage` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '使用ID',
  `seal_id`       BIGINT       NOT NULL                COMMENT '印章ID',
  `applicant_id`  BIGINT       NOT NULL                COMMENT '申请人ID',
  `document_name` VARCHAR(200) NOT NULL                COMMENT '盖章文件名称',
  `usage_count`   INT          DEFAULT 1               COMMENT '盖章次数',
  `usage_date`    DATE         NOT NULL                COMMENT '用印日期',
  `purpose`       VARCHAR(500) DEFAULT NULL            COMMENT '用印事由',
  `approver_id`   BIGINT       DEFAULT NULL            COMMENT '审批人ID',
  `status`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待审批 1已批准 2已驳回 3已用印)',
  `process_instance_id` BIGINT DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章使用记录表';

-- ---------------------------------------------------------------------------
-- 8.3 办公用品目录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_supply`;
CREATE TABLE `adm_supply` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用品ID',
  `supply_name`   VARCHAR(200) NOT NULL                COMMENT '用品名称',
  `specification` VARCHAR(200) DEFAULT NULL            COMMENT '规格型号',
  `unit`          VARCHAR(32)  DEFAULT NULL            COMMENT '计量单位(个/箱/包/盒)',
  `category`      VARCHAR(64)  DEFAULT NULL            COMMENT '分类',
  `unit_price`    DECIMAL(10,2) DEFAULT NULL           COMMENT '单价',
  `description`   VARCHAR(500) DEFAULT NULL            COMMENT '描述',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_supply_name` (`supply_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='办公用品目录表';

-- ---------------------------------------------------------------------------
-- 8.4 办公用品库存表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_supply_stock`;
CREATE TABLE `adm_supply_stock` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `supply_id`      BIGINT       NOT NULL                COMMENT '用品ID',
  `total_qty`      INT          NOT NULL DEFAULT 0      COMMENT '总库存数量',
  `available_qty`  INT          NOT NULL DEFAULT 0      COMMENT '可用数量',
  `locked_qty`     INT          NOT NULL DEFAULT 0      COMMENT '锁定数量(已申请未出库)',
  `alert_threshold` INT         DEFAULT 10              COMMENT '库存预警阈值',
  `version`        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `del_flag`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supply_id` (`supply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='办公用品库存表';

-- ---------------------------------------------------------------------------
-- 8.5 固定资产表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_asset`;
CREATE TABLE `adm_asset` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '资产ID',
  `asset_code`      VARCHAR(64)     NOT NULL                COMMENT '资产编号',
  `asset_name`      VARCHAR(200)    NOT NULL                COMMENT '资产名称',
  `sn`              VARCHAR(128)    DEFAULT NULL            COMMENT '序列号(SN)',
  `brand`           VARCHAR(100)    DEFAULT NULL            COMMENT '品牌',
  `model`           VARCHAR(100)    DEFAULT NULL            COMMENT '型号',
  `category`        VARCHAR(64)     DEFAULT NULL            COMMENT '资产分类(computer/printer/desk/vehicle/...)',
  `status`          CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0闲置 1在用 2维修 3报废 4已出库)',
  `purchase_date`   DATE            DEFAULT NULL            COMMENT '购买日期',
  `price`           DECIMAL(12,2)   DEFAULT 0               COMMENT '资产原值',
  `current_user_id` BIGINT          DEFAULT NULL            COMMENT '当前使用人ID',
  `dept_id`         BIGINT          DEFAULT NULL            COMMENT '所属部门ID',
  `location`        VARCHAR(200)    DEFAULT NULL            COMMENT '存放位置',
  `warranty_end`    DATE            DEFAULT NULL            COMMENT '保修截止日期',
  `supplier`        VARCHAR(200)    DEFAULT NULL            COMMENT '供应商',
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
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_sn` (`sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='固定资产表';

-- ---------------------------------------------------------------------------
-- 8.6 资产操作日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `adm_asset_log`;
CREATE TABLE `adm_asset_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `asset_id`      BIGINT       NOT NULL                COMMENT '资产ID',
  `operation`     VARCHAR(32)  NOT NULL                COMMENT '操作类型(allocate/borrow/return/repair/scrap/maintain)',
  `operator_id`   BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `from_user_id`  BIGINT       DEFAULT NULL            COMMENT '原使用人ID',
  `to_user_id`    BIGINT       DEFAULT NULL            COMMENT '新使用人ID',
  `remark`        VARCHAR(500) DEFAULT NULL            COMMENT '操作备注',
  `operation_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operation` (`operation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产操作日志表';


-- ============================================================================
-- 第九类：费控报销表 (fin_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 9.1 预算表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_budget`;
CREATE TABLE `fin_budget` (
  `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `dept_id`           BIGINT          DEFAULT NULL            COMMENT '部门ID',
  `project_id`        BIGINT          DEFAULT NULL            COMMENT '项目ID',
  `expense_category`  VARCHAR(64)     NOT NULL                COMMENT '费用类别(travel/office/entertainment/transport/other)',
  `year`              INT             NOT NULL                COMMENT '年度',
  `month`             INT             DEFAULT NULL            COMMENT '月份(NULL表示年度预算)',
  `amount`            DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '预算总额',
  `occupied_amount`   DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '占用金额(审批中)',
  `executed_amount`   DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '已执行金额(已报销)',
  `control_strategy`  VARCHAR(32)     DEFAULT 'soft'          COMMENT '预算控制策略(soft=预警 hard=禁止 none=不控制)',
  `status`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '状态(0草稿 1已启用 2已冻结 3已完成)',
  `version`           INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `del_flag`          CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`         VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_category_year` (`expense_category`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预算表';

-- ---------------------------------------------------------------------------
-- 9.2 费用报销表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_expense`;
CREATE TABLE `fin_expense` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '报销ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `title`               VARCHAR(200)    NOT NULL                COMMENT '报销标题',
  `total_amount`        DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '报销总金额',
  `related_business_trip_id` BIGINT     DEFAULT NULL            COMMENT '关联出差ID',
  `related_loan_id`     BIGINT          DEFAULT NULL            COMMENT '关联借款ID',
  `description`         TEXT            DEFAULT NULL            COMMENT '报销说明',
  `status`              VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '状态(pending/approved/rejected/withdrawn/paid)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `paid_time`           DATETIME        DEFAULT NULL            COMMENT '付款时间',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_business_trip` (`related_business_trip_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='费用报销表';

-- ---------------------------------------------------------------------------
-- 9.3 费用报销明细表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_expense_detail`;
CREATE TABLE `fin_expense_detail` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `expense_id`    BIGINT          NOT NULL                COMMENT '报销单ID',
  `expense_date`  DATE            DEFAULT NULL            COMMENT '费用发生日期',
  `expense_type`  VARCHAR(64)     NOT NULL                COMMENT '费用类型(travel/accommodation/meal/transport/office/other)',
  `amount`        DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '金额',
  `description`   VARCHAR(500)    DEFAULT NULL            COMMENT '费用说明',
  `invoice_no`    VARCHAR(64)     DEFAULT NULL            COMMENT '发票号',
  `invoice_amount` DECIMAL(14,2)  DEFAULT NULL            COMMENT '发票金额',
  `invoice_url`   VARCHAR(512)    DEFAULT NULL            COMMENT '发票附件URL',
  `budget_id`     BIGINT          DEFAULT NULL            COMMENT '关联预算ID',
  `del_flag`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_expense_id` (`expense_id`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_invoice_no` (`invoice_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='费用报销明细表';

-- ---------------------------------------------------------------------------
-- 9.4 借款表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_loan`;
CREATE TABLE `fin_loan` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '借款ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `loan_amount`         DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '借款金额',
  `repaid_amount`       DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '已还金额',
  `purpose`             VARCHAR(500)    DEFAULT NULL            COMMENT '借款用途',
  `repayment_method`    VARCHAR(32)     DEFAULT 'lump_sum'      COMMENT '还款方式(lump_sum=一次性 installment=分期)',
  `expected_repay_date` DATE            DEFAULT NULL            COMMENT '预计还款日期',
  `status`              VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '状态(pending/approved/rejected/withdrawn/repaying/paid_off)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_expected_repay` (`expected_repay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='借款表';

-- ---------------------------------------------------------------------------
-- 9.5 还款记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_loan_repayment`;
CREATE TABLE `fin_loan_repayment` (
  `id`          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '还款ID',
  `loan_id`     BIGINT          NOT NULL                COMMENT '借款ID',
  `repay_amount` DECIMAL(14,2)  NOT NULL DEFAULT 0      COMMENT '还款金额',
  `repay_date`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '还款时间',
  `repay_type`  VARCHAR(32)     DEFAULT 'manual'        COMMENT '还款类型(manual=手动 offset=报销冲抵)',
  `remark`      VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  `del_flag`    CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`   VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_loan_id` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='还款记录表';

-- ---------------------------------------------------------------------------
-- 9.6 合同管理表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_contract`;
CREATE TABLE `fin_contract` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_no`   VARCHAR(64)     NOT NULL                COMMENT '合同编号',
  `contract_name` VARCHAR(200)    NOT NULL                COMMENT '合同名称',
  `party_a_id`    BIGINT          DEFAULT NULL            COMMENT '甲方ID(内部部门/公司)',
  `party_a_name`  VARCHAR(200)    DEFAULT NULL            COMMENT '甲方名称',
  `party_b`       VARCHAR(200)    DEFAULT NULL            COMMENT '乙方名称',
  `party_b_contact` VARCHAR(64)   DEFAULT NULL            COMMENT '乙方联系人',
  `party_b_phone` VARCHAR(20)     DEFAULT NULL            COMMENT '乙方联系电话',
  `amount`        DECIMAL(16,2)   DEFAULT 0               COMMENT '合同金额',
  `paid_amount`   DECIMAL(16,2)   DEFAULT 0               COMMENT '已付金额',
  `start_date`    DATE            DEFAULT NULL            COMMENT '开始日期',
  `end_date`      DATE            DEFAULT NULL            COMMENT '结束日期',
  `sign_date`     DATE            DEFAULT NULL            COMMENT '签订日期',
  `contract_type` VARCHAR(32)     DEFAULT 'purchase'      COMMENT '合同类型(purchase/sales/service/labor/lease/other)',
  `category`      VARCHAR(64)     DEFAULT NULL            COMMENT '合同分类',
  `status`        VARCHAR(32)     NOT NULL DEFAULT 'draft' COMMENT '状态(draft/pending/active/expiring/expired/terminated)',
  `manager_id`    BIGINT          DEFAULT NULL            COMMENT '负责人ID',
  `attachment`    VARCHAR(512)    DEFAULT NULL            COMMENT '合同文件URL',
  `remark`        TEXT            DEFAULT NULL            COMMENT '备注',
  `del_flag`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_contract_type` (`contract_type`),
  KEY `idx_status` (`status`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_end_date` (`end_date`),
  KEY `idx_party_a_id` (`party_a_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同管理表';

-- ---------------------------------------------------------------------------
-- 9.7 付款记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_payment`;
CREATE TABLE `fin_payment` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '付款ID',
  `contract_id`   BIGINT          DEFAULT NULL            COMMENT '合同ID',
  `expense_id`    BIGINT          DEFAULT NULL            COMMENT '报销单ID',
  `payee`         VARCHAR(200)    NOT NULL                COMMENT '收款方',
  `payee_account` VARCHAR(64)     DEFAULT NULL            COMMENT '收款账号',
  `payee_bank`    VARCHAR(100)    DEFAULT NULL            COMMENT '收款银行',
  `amount`        DECIMAL(14,2)   NOT NULL DEFAULT 0      COMMENT '付款金额',
  `pay_date`      DATETIME        DEFAULT NULL            COMMENT '付款日期',
  `payment_type`  VARCHAR(32)     DEFAULT 'bank_transfer' COMMENT '付款方式(cash/check/bank_transfer/online/alipay/wechat)',
  `remark`        VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  `status`        VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '状态(pending/completed/failed/reversed)',
  `operator_id`   BIGINT          DEFAULT NULL            COMMENT '经办人ID',
  `approver_id`   BIGINT          DEFAULT NULL            COMMENT '审批人ID',
  `del_flag`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_expense_id` (`expense_id`),
  KEY `idx_payee` (`payee`),
  KEY `idx_status` (`status`),
  KEY `idx_pay_date` (`pay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='付款记录表';


-- ============================================================================
-- 第十类：消息通知表 (msg_*)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 10.1 用户通知偏好表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `msg_user_preference`;
CREATE TABLE `msg_user_preference` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `msg_type`        VARCHAR(64)  NOT NULL                COMMENT '消息类型(approval/notice/meeting/task/system/leave)',
  `channel`         VARCHAR(32)  NOT NULL DEFAULT 'system_inbox' COMMENT '通知渠道(system_inbox/email/sms/wechat/push)',
  `enabled`         CHAR(1)      NOT NULL DEFAULT '1'    COMMENT '是否启用(0否 1是)',
  `digest_enabled`  CHAR(1)      DEFAULT '0'             COMMENT '是否聚合摘要(0否 1是)',
  `quiet_start`     TIME         DEFAULT NULL            COMMENT '免打扰开始时间',
  `quiet_end`       TIME         DEFAULT NULL            COMMENT '免打扰结束时间',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_channel` (`emp_id`, `msg_type`, `channel`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户通知偏好表';

-- ---------------------------------------------------------------------------
-- 10.2 通知消息表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `msg_notification`;
CREATE TABLE `msg_notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '接收人ID',
  `msg_type`    VARCHAR(64)  NOT NULL                COMMENT '消息类型(approval/notice/meeting/task/system/leave/warning)',
  `title`       VARCHAR(200) NOT NULL                COMMENT '消息标题',
  `content`     TEXT         DEFAULT NULL            COMMENT '消息内容',
  `channel`     VARCHAR(32)  DEFAULT 'system_inbox'  COMMENT '通知渠道(system_inbox/email/sms)',
  `is_read`     CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '是否已读(0未读 1已读)',
  `read_time`   DATETIME     DEFAULT NULL            COMMENT '阅读时间',
  `biz_id`      BIGINT       DEFAULT NULL            COMMENT '业务ID',
  `biz_type`    VARCHAR(32)  DEFAULT NULL            COMMENT '业务类型',
  `sender_id`   BIGINT       DEFAULT NULL            COMMENT '发送人ID',
  `priority`    CHAR(1)      DEFAULT '0'             COMMENT '优先级(0普通 1重要 2紧急)',
  `expire_time` DATETIME     DEFAULT NULL            COMMENT '过期时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_msg_type` (`msg_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知消息表';


-- ============================================================================
-- 第十一类：监控日志表 (保留原oa_system_full中的监控表)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 11.1 操作日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_operation_log`;
CREATE TABLE `oa_operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `emp_id`      BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `emp_name`    VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',
  `module`      VARCHAR(64)  DEFAULT NULL            COMMENT '操作模块',
  `operation`   VARCHAR(200) DEFAULT NULL            COMMENT '操作描述',
  `method`      VARCHAR(200) DEFAULT NULL            COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL            COMMENT '请求URL',
  `ip`          VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
  `status`      INT          DEFAULT 0               COMMENT '状态(0正常 1异常)',
  `cost_time`   BIGINT       DEFAULT 0               COMMENT '耗时(毫秒)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ---------------------------------------------------------------------------
-- 11.2 登录日志表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `oa_login_log`;
CREATE TABLE `oa_login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `emp_id`      BIGINT       DEFAULT NULL            COMMENT '员工ID',
  `username`    VARCHAR(64)  DEFAULT NULL            COMMENT '用户名',
  `ip`          VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
  `browser`     VARCHAR(100) DEFAULT NULL            COMMENT '浏览器类型',
  `os`          VARCHAR(100) DEFAULT NULL            COMMENT '操作系统',
  `status`      INT          DEFAULT 0               COMMENT '状态(0成功 1失败)',
  `message`     VARCHAR(500) DEFAULT NULL            COMMENT '提示消息',
  `login_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ---------------------------------------------------------------------------
-- 11.3 告警规则表
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
  `check_cron`     VARCHAR(64)  DEFAULT NULL            COMMENT '检查Cron表达式',
  `notify_type`    VARCHAR(32)  DEFAULT 'inner'         COMMENT '通知方式(inner/email/sms)',
  `notify_targets` VARCHAR(500) DEFAULT NULL            COMMENT '通知目标JSON',
  `status`         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警规则表';

-- ---------------------------------------------------------------------------
-- 11.4 告警日志表
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
