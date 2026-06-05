-- ============================================
-- V970__doc_core_tables.sql
-- 增量: 文档管理核心业务表
-- 对应模块: oa-document (发文/收文/签报/档案)
-- ============================================

-- 1) doc_dispatches 发文表
CREATE TABLE `doc_dispatches` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_no`           VARCHAR(32)          DEFAULT NULL COMMENT '文号 (唯一)',
  `title`            VARCHAR(200) NOT NULL               COMMENT '标题',
  `subject_word`     VARCHAR(200)         DEFAULT NULL COMMENT '主题词',
  `send_to_dept`     VARCHAR(500)         DEFAULT NULL COMMENT '主送部门',
  `copy_to_dept`     VARCHAR(500)         DEFAULT NULL COMMENT '抄送部门',
  `urgency`          VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT '紧急程度: URGENT/VERY_URGENT/NORMAL',
  `security_level`   VARCHAR(8)   NOT NULL DEFAULT 'PUBLIC' COMMENT '密级: TOP_SECRET/SECRET/CONFIDENTIAL/PUBLIC',
  `content`          TEXT                  DEFAULT NULL COMMENT '正文',
  `attachment_ids`   JSON                 DEFAULT NULL COMMENT '附件ID列表',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/PUBLISHED/ARCHIVED',
  `wf_instance_id`   BIGINT               DEFAULT NULL COMMENT '流程实例 ID',
  `dept_id`          BIGINT       NOT NULL               COMMENT '所属部门 ID',
  `create_emp_id`    BIGINT       NOT NULL               COMMENT '创建人 emp_id',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_no` (`doc_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_emp` (`create_emp_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发文表';

-- 2) doc_receives 收文表
CREATE TABLE `doc_receives` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `receive_no`       VARCHAR(32)          DEFAULT NULL COMMENT '收文号 (唯一)',
  `source_dept`      VARCHAR(200) NOT NULL               COMMENT '来文单位',
  `doc_title`        VARCHAR(200) NOT NULL               COMMENT '来文标题',
  `doc_date`         DATE                 DEFAULT NULL COMMENT '来文日期',
  `receive_date`     DATE         NOT NULL               COMMENT '收文日期',
  `urgent_level`     VARCHAR(8)   NOT NULL DEFAULT 'NORMAL' COMMENT '紧急程度: URGENT/VERY_URGENT/NORMAL',
  `content`          TEXT                  DEFAULT NULL COMMENT '正文/摘要',
  `process_opinion`  VARCHAR(500)         DEFAULT NULL COMMENT '拟办意见',
  `dept_id`          BIGINT               DEFAULT NULL COMMENT '承办部门 ID',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/COMPLETED/ARCHIVED',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receive_no` (`receive_no`),
  KEY `idx_status` (`status`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收文表';

-- 3) doc_sign_reports 签报表
CREATE TABLE `doc_sign_reports` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_no`        VARCHAR(32)          DEFAULT NULL COMMENT '签报编号 (唯一)',
  `title`            VARCHAR(200) NOT NULL               COMMENT '标题',
  `report_type`      VARCHAR(16)  NOT NULL               COMMENT '签报类型: GENERAL/URGENT/SPECIAL',
  `content`          TEXT                  DEFAULT NULL COMMENT '正文',
  `attachment_ids`   JSON                 DEFAULT NULL COMMENT '附件ID列表',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED/ARCHIVED',
  `wf_instance_id`   BIGINT               DEFAULT NULL COMMENT '流程实例 ID',
  `dept_id`          BIGINT       NOT NULL               COMMENT '所属部门 ID',
  `create_emp_id`    BIGINT       NOT NULL               COMMENT '创建人 emp_id',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_emp` (`create_emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签报表';

-- 4) doc_sign_report_items 签报审批记录明细
CREATE TABLE `doc_sign_report_items` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id`        BIGINT       NOT NULL               COMMENT '签报 ID',
  `opinion`          VARCHAR(500)         DEFAULT NULL COMMENT '审批意见',
  `approver_id`      BIGINT       NOT NULL               COMMENT '审批人 emp_id',
  `approve_order`    INT                   DEFAULT NULL COMMENT '审批顺序',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_report` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签报审批记录明细';

-- 5) doc_archives 档案表
CREATE TABLE `doc_archives` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `archive_no`       VARCHAR(32)          DEFAULT NULL COMMENT '档案编号 (唯一)',
  `archive_type`     VARCHAR(16)  NOT NULL               COMMENT '档案类型: DISPATCH/RECEIVE/SIGN_REPORT',
  `source_id`        BIGINT       NOT NULL               COMMENT '关联业务单 ID',
  `archive_date`     DATE         NOT NULL               COMMENT '归档日期',
  `title`            VARCHAR(200)         DEFAULT NULL COMMENT '档案标题',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/FROZEN/DESTROYED',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_archive_no` (`archive_no`),
  KEY `idx_type_source` (`archive_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案表';

-- 6) doc_archive_files 档案附件表
CREATE TABLE `doc_archive_files` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `archive_id`       BIGINT       NOT NULL               COMMENT '档案 ID',
  `file_name`        VARCHAR(200) NOT NULL               COMMENT '文件名',
  `file_path`        VARCHAR(500) NOT NULL               COMMENT '文件路径/存储 key',
  `file_size`        BIGINT               DEFAULT NULL COMMENT '文件大小 (字节)',
  `file_type`        VARCHAR(50)          DEFAULT NULL COMMENT '文件类型 (MIME/扩展名)',
  `del_flag`         CHAR(1)      NOT NULL DEFAULT '0'   COMMENT '删除标记',
  `create_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '创建人',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(50)          DEFAULT NULL   COMMENT '更新人',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`          INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_archive` (`archive_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档案附件表';
