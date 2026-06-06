-- V986__hr_training_tables.sql
CREATE TABLE hr_train_course (
  id BIGINT AUTO_INCREMENT, course_name VARCHAR(200) NOT NULL, course_type VARCHAR(20),
  credit DECIMAL(4,1) DEFAULT 0, total_hours INT DEFAULT 0,
  description TEXT, status VARCHAR(16) DEFAULT 'ACTIVE',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训课程';

CREATE TABLE hr_train_plan (
  id BIGINT AUTO_INCREMENT, plan_name VARCHAR(200) NOT NULL, year INT,
  course_id BIGINT NOT NULL, total_budget DECIMAL(12,2),
  status VARCHAR(16) DEFAULT 'DRAFT',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_course(course_id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训计划';

CREATE TABLE hr_train_session (
  id BIGINT AUTO_INCREMENT, plan_id BIGINT NOT NULL, session_name VARCHAR(200),
  start_time DATETIME, end_time DATETIME, location VARCHAR(200),
  max_capacity INT DEFAULT 30, enrolled_num INT DEFAULT 0,
  trainer VARCHAR(100), sign_code VARCHAR(16),
  status VARCHAR(16) DEFAULT 'PENDING',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_plan(plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训班级';

CREATE TABLE hr_train_enroll (
  id BIGINT AUTO_INCREMENT, session_id BIGINT NOT NULL, emp_id BIGINT NOT NULL,
  enroll_time DATETIME, attendance VARCHAR(16) DEFAULT 'PENDING',
  sign_time DATETIME, score DECIMAL(5,2), credit_granted DECIMAL(4,1),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_session(session_id), KEY idx_emp(emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训报名';

CREATE TABLE hr_train_record (
  id BIGINT AUTO_INCREMENT, emp_id BIGINT NOT NULL, course_id BIGINT,
  session_id BIGINT, total_credit DECIMAL(4,1),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_emp(emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训记录';
