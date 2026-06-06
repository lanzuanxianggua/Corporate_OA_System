-- V980__hr_attendance_tables.sql
CREATE TABLE hr_attendance_group (
  id BIGINT AUTO_INCREMENT, group_name VARCHAR(100) NOT NULL, group_type VARCHAR(16) DEFAULT 'FIXED',
  clock_in_time VARCHAR(8), clock_out_time VARCHAR(8), late_minutes INT DEFAULT 0, early_minutes INT DEFAULT 0,
  work_days VARCHAR(50) DEFAULT '1,2,3,4,5', status VARCHAR(16) DEFAULT 'ACTIVE', dept_id BIGINT,
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_dept(dept_id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤组';

CREATE TABLE hr_attendance_group_emp (
  id BIGINT AUTO_INCREMENT, group_id BIGINT NOT NULL, emp_id BIGINT NOT NULL,
  effective_date DATE NOT NULL, expire_date DATE,
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_group(group_id), KEY idx_emp(emp_id),
  UNIQUE KEY uk_group_emp(group_id, emp_id, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤组成员';

CREATE TABLE hr_attendance_record (
  id BIGINT AUTO_INCREMENT, emp_id BIGINT NOT NULL, clock_date DATE NOT NULL,
  clock_in_time DATETIME, clock_out_time DATETIME,
  clock_in_method VARCHAR(16), clock_out_method VARCHAR(16),
  status VARCHAR(16) DEFAULT 'NORMAL', work_hours DECIMAL(4,1),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_emp_date(emp_id, clock_date),
  UNIQUE KEY uk_emp_date(emp_id, clock_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录';

CREATE TABLE hr_attendance_exception (
  id BIGINT AUTO_INCREMENT, record_id BIGINT, emp_id BIGINT NOT NULL,
  exception_date DATE NOT NULL, exception_type VARCHAR(20),
  status VARCHAR(16) DEFAULT 'PENDING', reason VARCHAR(500),
  appeal_content TEXT, appeal_time DATETIME,
  handle_emp_id BIGINT, handle_comment VARCHAR(500), handle_time DATETIME,
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_emp_status(emp_id, status), KEY idx_date(exception_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤异常';

CREATE TABLE hr_attendance_stat (
  id BIGINT AUTO_INCREMENT, emp_id BIGINT NOT NULL, stat_date DATE NOT NULL,
  stat_type VARCHAR(8) DEFAULT 'DAY', work_days INT, actual_days INT,
  late_count INT DEFAULT 0, early_count INT DEFAULT 0, absent_count INT DEFAULT 0,
  total_work_hours DECIMAL(6,1),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_emp_date(emp_id, stat_date),
  UNIQUE KEY uk_emp_type_date(emp_id, stat_type, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤统计';
