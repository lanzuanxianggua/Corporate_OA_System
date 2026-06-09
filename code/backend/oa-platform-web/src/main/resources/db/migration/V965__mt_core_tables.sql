-- ============================================
-- V972__mt_core_tables.sql
-- 增量: 会议管理核心业务表
-- 对应模块: oa-meeting (会议室/预约/会议记录/决议)
-- ============================================

-- 1) mt_rooms 会议室
CREATE TABLE `mt_rooms` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_name`       VARCHAR(100) NOT NULL                COMMENT '会议室名称',
  `room_code`       VARCHAR(32)           DEFAULT NULL   COMMENT '会议室编号 (唯一)',
  `floor`           VARCHAR(20)           DEFAULT NULL   COMMENT '所在楼层',
  `capacity`        INT                   DEFAULT 10     COMMENT '容纳人数',
  `facility`        VARCHAR(500)          DEFAULT NULL   COMMENT '设备:投影仪/视频会议等',
  `location`        VARCHAR(200)          DEFAULT NULL   COMMENT '位置描述',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/MAINTENANCE/DISABLED',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code` (`room_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室表';

-- 2) mt_bookings 预约
CREATE TABLE `mt_bookings` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id`         BIGINT       NOT NULL                COMMENT '会议室 ID',
  `book_emp_id`     BIGINT       NOT NULL                COMMENT '预订人 emp_id',
  `book_date`       DATE         NOT NULL                COMMENT '预订日期',
  `start_time`      TIME         NOT NULL                COMMENT '开始时间',
  `end_time`        TIME         NOT NULL                COMMENT '结束时间',
  `meeting_title`   VARCHAR(200)          DEFAULT NULL   COMMENT '会议标题',
  `meeting_desc`    VARCHAR(500)          DEFAULT NULL   COMMENT '会议描述',
  `participant_ids` JSON                  DEFAULT NULL   COMMENT '参与人 emp_id 列表',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED',
  `wf_instance_id`  BIGINT                DEFAULT NULL   COMMENT '流程实例 ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_time` (`room_id`, `book_date`, `start_time`),
  KEY `idx_room_date` (`room_id`, `book_date`),
  KEY `idx_book_emp` (`book_emp_id`),
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议室预约表';

-- 3) mt_meetings 会议记录
CREATE TABLE `mt_meetings` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `booking_id`      BIGINT                DEFAULT NULL   COMMENT '关联预约 ID',
  `meeting_title`   VARCHAR(200)          DEFAULT NULL   COMMENT '会议标题',
  `actual_start`    DATETIME              DEFAULT NULL   COMMENT '实际开始时间',
  `actual_end`      DATETIME              DEFAULT NULL   COMMENT '实际结束时间',
  `meeting_status`  VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED' COMMENT '状态: SCHEDULED/ONGOING/COMPLETED/CANCELLED',
  `summary`         TEXT                  DEFAULT NULL   COMMENT '会议纪要',
  `create_emp_id`   BIGINT       NOT NULL                COMMENT '创建人 emp_id',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_status` (`meeting_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议记录表';

-- 4) mt_resolutions 决议
CREATE TABLE `mt_resolutions` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `meeting_id`          BIGINT       NOT NULL                COMMENT '关联会议 ID',
  `resolution_title`    VARCHAR(200)          DEFAULT NULL   COMMENT '决议标题',
  `content`             TEXT                  DEFAULT NULL   COMMENT '决议内容',
  `responsible_emp_id`  BIGINT                DEFAULT NULL   COMMENT '责任人 emp_id',
  `deadline`            DATE                  DEFAULT NULL   COMMENT '截止日期',
  `priority`            VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: HIGH/NORMAL/LOW',
  `status`              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`           VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`             INT          NOT NULL DEFAULT 0      COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_meeting` (`meeting_id`),
  KEY `idx_responsible` (`responsible_emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议决议表';
