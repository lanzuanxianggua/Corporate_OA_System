-- V984__hr_recruitment_tables.sql
CREATE TABLE hr_recruit_job (
  id BIGINT AUTO_INCREMENT, job_title VARCHAR(200) NOT NULL, dept_id BIGINT,
  headcount INT DEFAULT 1, requirement TEXT, responsibility TEXT,
  salary_min DECIMAL(10,2), salary_max DECIMAL(10,2),
  status VARCHAR(16) DEFAULT 'OPEN',
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_status(status), KEY idx_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘岗位';

CREATE TABLE hr_recruit_candidate (
  id BIGINT AUTO_INCREMENT, job_id BIGINT NOT NULL, name VARCHAR(50) NOT NULL, phone VARCHAR(20),
  email VARCHAR(100), resume_url TEXT, status VARCHAR(16) DEFAULT 'NEW',
  source VARCHAR(20), interviewer_id BIGINT, interview_score DECIMAL(5,2),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_job(job_id), KEY idx_phone(phone), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人';

CREATE TABLE hr_recruit_interview (
  id BIGINT AUTO_INCREMENT, candidate_id BIGINT NOT NULL, round INT DEFAULT 1,
  interview_date DATETIME, interviewer_id BIGINT, score DECIMAL(5,2),
  evaluation TEXT, result VARCHAR(16),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_candidate(candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试记录';

CREATE TABLE hr_recruit_offer (
  id BIGINT AUTO_INCREMENT, candidate_id BIGINT NOT NULL,
  offer_salary DECIMAL(10,2), offer_date DATE, onboard_date DATE,
  status VARCHAR(16) DEFAULT 'PENDING',
  wf_instance_id BIGINT, reject_reason VARCHAR(500), remark VARCHAR(500),
  del_flag CHAR(1) DEFAULT '0', create_by VARCHAR(64), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT DEFAULT 0, PRIMARY KEY(id), KEY idx_candidate(candidate_id), KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer记录';
