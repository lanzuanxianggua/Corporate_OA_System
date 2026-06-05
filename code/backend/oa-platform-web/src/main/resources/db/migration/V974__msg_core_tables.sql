-- ============================================
-- V974__msg_core_tables.sql
-- 增量: 消息模块补充表
-- V100 已建 msg_notification / msg_notification_recipient
-- 本迁移只补缺少的 msg_email_logs / msg_sms_logs
-- ============================================

-- 1) msg_email_logs 邮件日志表
CREATE TABLE `msg_email_logs` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` BIGINT                DEFAULT NULL COMMENT '关联通知 id',
  `recipient_email` VARCHAR(200) NOT NULL                COMMENT '收件邮箱',
  `subject`         VARCHAR(200)          DEFAULT NULL COMMENT '邮件主题',
  `content`         TEXT                   DEFAULT NULL COMMENT '邮件内容',
  `send_status`     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态: PENDING/SENT/FAILED',
  `fail_reason`     VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
  `sent_time`       DATETIME              DEFAULT NULL COMMENT '发送时间',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_notification` (`notification_id`),
  KEY `idx_status` (`send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件日志';

-- 2) msg_sms_logs 短信日志表
CREATE TABLE `msg_sms_logs` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notification_id` BIGINT                DEFAULT NULL COMMENT '关联通知 id',
  `recipient_phone` VARCHAR(20) NOT NULL                COMMENT '收信号码',
  `content`         TEXT                   DEFAULT NULL COMMENT '短信内容',
  `send_status`     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态: PENDING/SENT/FAILED',
  `fail_reason`     VARCHAR(500)          DEFAULT NULL COMMENT '失败原因',
  `sent_time`       DATETIME              DEFAULT NULL COMMENT '发送时间',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)           DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT          NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_notification` (`notification_id`),
  KEY `idx_status` (`send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信日志';
