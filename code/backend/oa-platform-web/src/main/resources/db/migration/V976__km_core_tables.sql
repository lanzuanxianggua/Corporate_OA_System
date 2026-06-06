-- ============================================
-- V976__km_core_tables.sql
-- 知识库核心表: 分类/条目/版本历史
-- 对应模块: oa-knowledge
-- ============================================

-- -----------------------------------------------------------------------
-- 1) km_categories — 知识分类
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `km_categories` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `category_name` VARCHAR(100)  NOT NULL                 COMMENT '分类名称',
  `parent_id`     BIGINT        NOT NULL DEFAULT 0       COMMENT '父分类ID, 0=根分类',
  `sort_order`    INT           NOT NULL DEFAULT 0       COMMENT '排序号',
  `description`   VARCHAR(500)  DEFAULT NULL             COMMENT '分类描述',
  `create_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '创建人',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '更新人',
  `update_time`   DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`      CHAR(1)       NOT NULL DEFAULT '0'     COMMENT '软删除标志, 0=正常 1=删除',
  `version`       INT           NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识分类';

-- -----------------------------------------------------------------------
-- 2) km_entries — 知识条目
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `km_entries` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `title`         VARCHAR(200)  NOT NULL                 COMMENT '标题',
  `content`       TEXT          DEFAULT NULL             COMMENT '正文/Markdown',
  `summary`       VARCHAR(500)  DEFAULT NULL             COMMENT '摘要',
  `category_id`   BIGINT        DEFAULT NULL             COMMENT '分类ID',
  `tags`          VARCHAR(200)  DEFAULT NULL             COMMENT '标签, 逗号分隔',
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
  `view_count`    INT           NOT NULL DEFAULT 0       COMMENT '浏览量',
  `create_emp_id` BIGINT        NOT NULL                 COMMENT '创建人emp_id',
  `create_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '创建人',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '更新人',
  `update_time`   DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`      CHAR(1)       NOT NULL DEFAULT '0'     COMMENT '软删除标志, 0=正常 1=删除',
  `version`       INT           NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_emp` (`create_emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识条目';

-- -----------------------------------------------------------------------
-- 3) km_versions — 版本历史
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `km_versions` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `entry_id`      BIGINT        NOT NULL                 COMMENT '知识条目ID',
  `version_no`    INT           NOT NULL                 COMMENT '版本号',
  `title`         VARCHAR(200)  DEFAULT NULL             COMMENT '标题(快照)',
  `content`       TEXT          DEFAULT NULL             COMMENT '正文(快照)',
  `summary`       VARCHAR(500)  DEFAULT NULL             COMMENT '摘要(快照)',
  `change_note`   VARCHAR(500)  DEFAULT NULL             COMMENT '变更说明',
  `create_emp_id` BIGINT        NOT NULL                 COMMENT '创建人emp_id',
  `create_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '创建人',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT NULL             COMMENT '更新人',
  `update_time`   DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`      CHAR(1)       NOT NULL DEFAULT '0'     COMMENT '软删除标志, 0=正常 1=删除',
  `version`       INT           NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_entry_version` (`entry_id`, `version_no`),
  KEY `idx_entry` (`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='版本历史';

-- ============================================
-- End V976__km_core_tables.sql
-- ============================================
