-- V982__hr_performance_tables.sql
CREATE TABLE hr_perf_template (
  id BIGINT AUTO_INCREMENT, template_name VARCHAR(100) NOT NULL, description TEXT,
  dimensions JSON COMMENT '维度配置[{name,weight,maxScore}]', status VARCHAR(16) DEFAULT 'ACTIVE',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效模板';

CREATE TABLE hr_perf_cycle (
  id BIGINT AUTO_INCREMENT, cycle_name VARCHAR(100) NOT NULL,
  template_id BIGINT NOT NULL, year INT NOT NULL, quarter INT,
  start_date DATE NOT NULL, end_date DATE NOT NULL,
  goal_start DATE, goal_end DATE, eval_start DATE, eval_end DATE,
  status VARCHAR(16) DEFAULT 'DRAFT',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效周期';

CREATE TABLE hr_perf_goal (
  id BIGINT AUTO_INCREMENT, cycle_id BIGINT NOT NULL, emp_id BIGINT NOT NULL,
  goal_content TEXT, target_value VARCHAR(200), weight DECIMAL(5,2),
  status VARCHAR(16) DEFAULT 'DRAFT',
  score DECIMAL(5,2), grade VARCHAR(8),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_cycle_emp(cycle_id, emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效目标';

CREATE TABLE hr_perf_eval (
  id BIGINT AUTO_INCREMENT, goal_id BIGINT NOT NULL, evaluator_id BIGINT NOT NULL,
  eval_type VARCHAR(16) DEFAULT 'SELF', score DECIMAL(5,2), comment TEXT,
  status VARCHAR(16) DEFAULT 'DRAFT',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_goal(goal_id), KEY idx_evaluator(evaluator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效评估';

CREATE TABLE hr_perf_result (
  id BIGINT AUTO_INCREMENT, cycle_id BIGINT NOT NULL, emp_id BIGINT NOT NULL,
  total_score DECIMAL(5,2), grade VARCHAR(8), ranking INT,
  status VARCHAR(16) DEFAULT 'PENDING',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_cycle(cycle_id), KEY idx_emp(emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绩效结果';
