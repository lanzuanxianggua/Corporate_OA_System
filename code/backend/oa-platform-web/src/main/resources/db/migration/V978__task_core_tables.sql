-- ============================================
-- V978__task_core_tables.sql
-- 增量: 任务协作 4 表 (task_projects / task_items / task_hours / task_comments)
-- 对应模块: oa-task (菜单 id=10 '任务协作' /api/v1/task/*)
-- ============================================

-- -------------------------------------------
-- 1. task_projects 项目表
-- -------------------------------------------
CREATE TABLE `task_projects` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_name`    VARCHAR(200) NOT NULL                COMMENT '项目名称',
  `project_code`    VARCHAR(32)           DEFAULT NULL   COMMENT '项目编码 (唯一)',
  `description`     TEXT                  DEFAULT NULL   COMMENT '项目描述',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/FROZEN/COMPLETED/ARCHIVED',
  `start_date`      DATE                 DEFAULT NULL   COMMENT '开始日期',
  `end_date`        DATE                 DEFAULT NULL   COMMENT '结束日期',
  `dept_id`         BIGINT               DEFAULT NULL   COMMENT '所属部门 id',
  `owner_emp_id`    BIGINT       NOT NULL                COMMENT '负责人 emp_id',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT '0'    COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_owner` (`owner_emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目';

-- -------------------------------------------
-- 2. task_items 任务表
-- -------------------------------------------
CREATE TABLE `task_items` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`      BIGINT       NOT NULL                COMMENT '所属项目 id',
  `task_name`       VARCHAR(200) NOT NULL                COMMENT '任务名称',
  `description`     TEXT                  DEFAULT NULL   COMMENT '任务描述',
  `assignee_id`     BIGINT               DEFAULT NULL   COMMENT '负责人 emp_id',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'TODO' COMMENT '状态: TODO/IN_PROGRESS/DONE/CLOSED',
  `priority`        VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: HIGH/NORMAL/LOW',
  `plan_start_date` DATE                 DEFAULT NULL   COMMENT '计划开始日期',
  `plan_end_date`   DATE                 DEFAULT NULL   COMMENT '计划结束日期',
  `actual_start`    DATETIME             DEFAULT NULL   COMMENT '实际开始时间',
  `actual_end`      DATETIME             DEFAULT NULL   COMMENT '实际结束时间',
  `progress`        INT          NOT NULL DEFAULT '0'    COMMENT '进度百分比 (0-100)',
  `parent_task_id`  BIGINT               DEFAULT NULL   COMMENT '父任务 id (子任务用)',
  `sort_order`      INT          NOT NULL DEFAULT '0'    COMMENT '排序号',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT '0'    COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_assignee` (`assignee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_parent` (`parent_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务';

-- -------------------------------------------
-- 3. task_hours 工时表
-- -------------------------------------------
CREATE TABLE `task_hours` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`         BIGINT       NOT NULL                COMMENT '任务 id',
  `work_date`       DATE         NOT NULL                COMMENT '工作日期',
  `hours`           DECIMAL(4,1) NOT NULL                COMMENT '工时数',
  `description`     VARCHAR(500)          DEFAULT NULL   COMMENT '工作描述',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工 emp_id',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT '0'    COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_emp_date` (`emp_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工时';

-- -------------------------------------------
-- 4. task_comments 评论表
-- -------------------------------------------
CREATE TABLE `task_comments` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id`           BIGINT       NOT NULL                COMMENT '任务 id',
  `content`           TEXT         NOT NULL                COMMENT '评论内容',
  `emp_id`            BIGINT       NOT NULL                COMMENT '评论人 emp_id',
  `parent_comment_id` BIGINT              DEFAULT NULL     COMMENT '回复的评论 id',
  `del_flag`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`         VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`           INT          NOT NULL DEFAULT '0'    COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论';
