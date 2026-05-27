-- Phase 1.3: Withdrawal
ALTER TABLE wf_task ADD COLUMN action_source VARCHAR(20) DEFAULT 'assignee' COMMENT 'assignee/initiator/system';

-- Phase 3: Multi-assignee & Transfer
ALTER TABLE wf_task ADD COLUMN parent_task_id BIGINT COMMENT '会签/或签的父任务ID';
ALTER TABLE wf_task ADD COLUMN multi_type VARCHAR(10) COMMENT 'countersign/orsign/null';
ALTER TABLE wf_task ADD COLUMN transfer_from_id BIGINT COMMENT '原审批人';
ALTER TABLE wf_task ADD COLUMN transfer_reason VARCHAR(500) COMMENT '转办原因';

-- Phase 4: Advanced features
ALTER TABLE wf_process_instance ADD COLUMN process_version INT COMMENT '启动时的版本号';

ALTER TABLE wf_task ADD COLUMN deadline DATETIME COMMENT '超时时间';
ALTER TABLE wf_task ADD COLUMN remind_count INT DEFAULT 0 COMMENT '催办次数';
ALTER TABLE wf_task ADD COLUMN last_remind_time DATETIME COMMENT '最后催办时间';

-- CC records table
CREATE TABLE IF NOT EXISTS wf_cc_record (
    id BIGINT PRIMARY KEY,
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    task_id BIGINT COMMENT '关联任务ID',
    cc_emp_id BIGINT NOT NULL COMMENT '抄送人ID',
    status VARCHAR(2) DEFAULT '0' COMMENT '0-未读 1-已读',
    create_time DATETIME,
    INDEX idx_instance (instance_id),
    INDEX idx_emp (cc_emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程抄送记录';

-- Delegation table
CREATE TABLE IF NOT EXISTS wf_delegation (
    id BIGINT PRIMARY KEY,
    delegator_id BIGINT NOT NULL COMMENT '委托人(原审批人)',
    delegate_to_id BIGINT NOT NULL COMMENT '被委托人(代理人)',
    start_time DATETIME NOT NULL COMMENT '生效开始时间',
    end_time DATETIME NOT NULL COMMENT '生效结束时间',
    status VARCHAR(2) DEFAULT '0' COMMENT '0-生效中 1-已取消',
    create_time DATETIME,
    INDEX idx_delegator (delegator_id),
    INDEX idx_delegate_to (delegate_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批委托';
