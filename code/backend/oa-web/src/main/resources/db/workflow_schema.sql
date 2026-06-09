-- Canonical persistent workflow tables for Corporate OA System.
-- Safe to run repeatedly. This script creates missing MySQL tables only.
-- It never drops, truncates, or inserts mock data.

CREATE TABLE IF NOT EXISTS wf_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  instance_id BIGINT NOT NULL,
  node_id BIGINT DEFAULT NULL,
  node_name VARCHAR(100) DEFAULT NULL,
  assignee_id BIGINT NOT NULL,
  task_type VARCHAR(20) NOT NULL DEFAULT 'TODO',
  parent_task_id BIGINT DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '0',
  opinion VARCHAR(500) DEFAULT NULL,
  signature VARCHAR(200) DEFAULT NULL,
  due_time DATETIME DEFAULT NULL,
  complete_time DATETIME DEFAULT NULL,
  remind_count INT NOT NULL DEFAULT 0,
  escalation_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_instance_id (instance_id),
  KEY idx_assignee_id (assignee_id),
  KEY idx_node_id (node_id),
  KEY idx_due_time (due_time),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wf_delegation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  delegator_id BIGINT NOT NULL,
  delegate_id BIGINT NOT NULL,
  process_category VARCHAR(32) DEFAULT NULL,
  notify_delegator TINYINT(1) NOT NULL DEFAULT 1,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_delegator_id (delegator_id),
  KEY idx_delegate_id (delegate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
