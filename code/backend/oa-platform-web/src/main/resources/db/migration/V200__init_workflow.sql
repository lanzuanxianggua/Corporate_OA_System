-- ============================================
-- V200__init_workflow.sql
-- oa-workflow 模块 - 8 张 wf_* 表
-- ============================================

CREATE TABLE `wf_definitions` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '定义ID',
  `def_key`     VARCHAR(64)  NOT NULL                     COMMENT '定义 KEY (业务唯一)',
  `def_name`    VARCHAR(128) NOT NULL                     COMMENT '定义名称',
  `version`     INT          NOT NULL DEFAULT 1           COMMENT '版本号',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'    COMMENT 'ACTIVE/DEPRECATED',
  `description` VARCHAR(500) DEFAULT NULL                 COMMENT '描述',
  `create_emp`  BIGINT       DEFAULT NULL                 COMMENT '创建人',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    TINYINT(1)   NOT NULL DEFAULT 0           COMMENT '软删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_key_version` (`def_key`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义';

CREATE TABLE `wf_nodes` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '节点ID',
  `def_id`      BIGINT       NOT NULL                     COMMENT '定义ID',
  `node_key`    VARCHAR(64)  NOT NULL                     COMMENT '节点 KEY (定义内唯一)',
  `node_name`   VARCHAR(128) NOT NULL                     COMMENT '节点名称',
  `node_type`   VARCHAR(16)  NOT NULL                     COMMENT 'START/APPROVAL/END',
  `assignee_rule_id` BIGINT  DEFAULT NULL                 COMMENT '审批人规则',
  `sort_order`  INT          NOT NULL DEFAULT 0           COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag`    TINYINT(1)   NOT NULL DEFAULT 0           COMMENT '软删除',
  PRIMARY KEY (`id`),
  KEY `idx_def_id` (`def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点';

CREATE TABLE `wf_transitions` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '流转ID',
  `def_id`         BIGINT       NOT NULL                  COMMENT '定义ID',
  `from_node_id`   BIGINT       NOT NULL                  COMMENT '源节点ID',
  `to_node_id`     BIGINT       NOT NULL                  COMMENT '目标节点ID',
  `condition_expr` VARCHAR(500) DEFAULT NULL              COMMENT '条件表达式 (简单 = "days>3" 形式)',
  `action`         VARCHAR(16)  NOT NULL DEFAULT 'APPROVE' COMMENT '触发动作 APPROVE/REJECT',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_node` (`from_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程流转';

CREATE TABLE `wf_assignee_rules` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '规则ID',
  `def_id`      BIGINT       NOT NULL                     COMMENT '定义ID',
  `rule_type`   VARCHAR(16)  NOT NULL                     COMMENT 'ROLE/LEADER/SELF/FIXED',
  `rule_target` VARCHAR(500) NOT NULL                     COMMENT '目标值: ROLE 存 roleCode,LEADER 存 leader 级别,FIXED 存 empId,SELF 自动取发起人',
  `priority`    INT          NOT NULL DEFAULT 0           COMMENT '优先级 (多个规则时取高优先级)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_def_id` (`def_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批人规则';

CREATE TABLE `wf_instances` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT     COMMENT '实例ID',
  `def_id`       BIGINT       NOT NULL                    COMMENT '定义ID',
  `def_key`      VARCHAR(64)  NOT NULL                    COMMENT '定义 KEY (冗余)',
  `business_key` VARCHAR(128) NOT NULL                    COMMENT '业务单 KEY (如 hr_leave:123)',
  `initiator_id` BIGINT       NOT NULL                    COMMENT '发起人 emp_id',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'RUNNING'  COMMENT 'RUNNING/APPROVED/REJECTED/CANCELED',
  `current_node_id` BIGINT    DEFAULT NULL                COMMENT '当前节点ID',
  `vars`         JSON         DEFAULT NULL                COMMENT '流程变量',
  `start_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '启动时间',
  `end_time`     DATETIME     DEFAULT NULL                COMMENT '结束时间',
  `del_flag`     TINYINT(1)   NOT NULL DEFAULT 0          COMMENT '软删除',
  PRIMARY KEY (`id`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_status` (`status`),
  KEY `idx_initiator` (`initiator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例';

CREATE TABLE `wf_tasks` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '任务ID',
  `instance_id`   BIGINT       NOT NULL                   COMMENT '流程实例ID',
  `node_id`       BIGINT       NOT NULL                   COMMENT '节点ID',
  `assignee_id`   BIGINT       NOT NULL                   COMMENT '当前审批人 emp_id',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/TRANSFERRED/SKIPPED',
  `action`        VARCHAR(16)  DEFAULT NULL               COMMENT '动作(APPROVE/REJECT/TRANSFER)',
  `action_time`   DATETIME     DEFAULT NULL               COMMENT '动作时间',
  `action_emp_id` BIGINT       DEFAULT NULL               COMMENT '实际操作人(可能=assignee,也可能=delegate)',
  `comment`       VARCHAR(1000) DEFAULT NULL              COMMENT '审批意见',
  `attachments`   JSON         DEFAULT NULL               COMMENT '附件',
  `delegated_from` BIGINT      DEFAULT NULL               COMMENT '委托来源 emp_id',
  `due_time`      DATETIME     DEFAULT NULL               COMMENT '超时时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag`      TINYINT(1)   NOT NULL DEFAULT 0         COMMENT '软删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance` (`instance_id`),
  KEY `idx_assignee_status` (`assignee_id`, `status`),
  KEY `idx_due_time` (`due_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批任务';

CREATE TABLE `wf_records` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT      COMMENT '记录ID',
  `instance_id` BIGINT       NOT NULL                     COMMENT '流程实例ID',
  `node_id`     BIGINT       DEFAULT NULL                 COMMENT '节点ID',
  `emp_id`      BIGINT       NOT NULL                     COMMENT '操作人 emp_id',
  `action`      VARCHAR(16)  NOT NULL                     COMMENT 'START/APPROVE/REJECT/TRANSFER/DELEGATE/END',
  `comment`     VARCHAR(1000) DEFAULT NULL                COMMENT '意见',
  `action_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批历史';

CREATE TABLE `wf_delegations` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '委托ID',
  `from_emp_id`  BIGINT       NOT NULL                   COMMENT '委托人 emp_id',
  `to_emp_id`    BIGINT       NOT NULL                   COMMENT '被委托人 emp_id',
  `start_time`   DATETIME     NOT NULL                   COMMENT '委托开始',
  `end_time`     DATETIME     NOT NULL                   COMMENT '委托结束',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'  COMMENT 'ACTIVE/EXPIRED/REVOKED',
  `reason`       VARCHAR(500) DEFAULT NULL               COMMENT '委托原因',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_emp` (`from_emp_id`, `status`),
  KEY `idx_to_emp` (`to_emp_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批委托';

-- 种子数据: HR 请假流程定义
INSERT INTO `wf_definitions` (`def_key`, `def_name`, `version`, `status`, `description`, `create_emp`)
VALUES ('hr_leave', 'HR 请假审批', 1, 'ACTIVE', '员工请假审批流程, 直属上级 + HR 审批', 1);

SET @def_id = LAST_INSERT_ID();

INSERT INTO `wf_nodes` (`def_id`, `node_key`, `node_name`, `node_type`, `sort_order`) VALUES
  (@def_id, 'start',   '开始',          'START',    0),
  (@def_id, 'manager', '直属上级审批',  'APPROVAL', 1),
  (@def_id, 'hr',      'HR 审批',       'APPROVAL', 2),
  (@def_id, 'end',     '结束',          'END',      3);

SET @start_node   = (SELECT id FROM wf_nodes WHERE def_id=@def_id AND node_key='start');
SET @manager_node = (SELECT id FROM wf_nodes WHERE def_id=@def_id AND node_key='manager');
SET @hr_node      = (SELECT id FROM wf_nodes WHERE def_id=@def_id AND node_key='hr');
SET @end_node     = (SELECT id FROM wf_nodes WHERE def_id=@def_id AND node_key='end');

INSERT INTO `wf_transitions` (`def_id`, `from_node_id`, `to_node_id`, `action`) VALUES
  (@def_id, @start_node,   @manager_node, 'APPROVE'),
  (@def_id, @manager_node, @hr_node,      'APPROVE'),
  (@def_id, @hr_node,      @end_node,     'APPROVE');

INSERT INTO `wf_assignee_rules` (`def_id`, `rule_type`, `rule_target`, `priority`) VALUES
  (@def_id, 'LEADER', '1', 0),  -- manager 节点: 发起人的直属上级
  (@def_id, 'ROLE',   'HR', 0); -- hr 节点: 角色为 HR 的员工
