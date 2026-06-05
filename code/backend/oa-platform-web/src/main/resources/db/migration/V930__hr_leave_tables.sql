-- ============================================
-- V930__hr_leave_tables.sql
-- 增量: HR 请假业务表 (hr_leave) + 业务权限
-- 对应 D-2 阶段: oa-hr-leave 模块
-- ============================================

-- 1) hr_leave 表 (请假申请单)
CREATE TABLE `hr_leave` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `emp_id`          BIGINT      NOT NULL                COMMENT '申请人 emp_id',
  `leave_type`      VARCHAR(20) NOT NULL                COMMENT '请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY',
  `start_date`      DATE        NOT NULL                COMMENT '开始日期',
  `end_date`        DATE        NOT NULL                COMMENT '结束日期',
  `total_days`      DECIMAL(5,1) NOT NULL               COMMENT '请假天数',
  `reason`          VARCHAR(500)         DEFAULT NULL   COMMENT '请假事由',
  `status`          CHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/CANCELLED',
  `wf_instance_id`  BIGINT              DEFAULT NULL   COMMENT '流程实例 ID',
  `del_flag`        CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '删除标记',
  `create_by`       VARCHAR(50)         DEFAULT NULL   COMMENT '创建人',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)         DEFAULT NULL   COMMENT '更新人',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version`         INT         NOT NULL DEFAULT 0     COMMENT '乐观锁',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_status` (`status`),
  KEY `idx_wf_instance_id` (`wf_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请单';
