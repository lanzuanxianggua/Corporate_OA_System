-- V992: missing business tables for the current oa-web startup module.
-- Permission seed data from the historical V992 script is intentionally not included
-- because the current project uses sys_menu/sys_role_menu, not sys_permission/sys_role_permission.

CREATE TABLE IF NOT EXISTS adm_supply_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(100) NOT NULL,
  parent_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_parent(parent_id),
  KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office supply category';

CREATE TABLE IF NOT EXISTS adm_supply (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supply_code VARCHAR(32) NOT NULL,
  supply_name VARCHAR(100) NOT NULL,
  category_id BIGINT,
  unit VARCHAR(20),
  spec VARCHAR(100),
  safety_stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_supply_code(supply_code),
  KEY idx_category(category_id),
  KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office supply';

CREATE TABLE IF NOT EXISTS adm_supply_stock (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supply_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  locked_quantity INT NOT NULL DEFAULT 0,
  location VARCHAR(100),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_supply(supply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office supply stock';

CREATE TABLE IF NOT EXISTS adm_supply_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(32) NOT NULL,
  request_type VARCHAR(16) NOT NULL DEFAULT 'OUT',
  emp_id BIGINT,
  dept_id BIGINT,
  reason VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  approve_time DATETIME,
  reject_reason VARCHAR(500),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_request_no(request_no),
  KEY idx_emp_status(emp_id, status),
  KEY idx_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office supply request';

CREATE TABLE IF NOT EXISTS adm_supply_request_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  supply_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  remark VARCHAR(200),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_request(request_id),
  KEY idx_supply(supply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office supply request item';

CREATE TABLE IF NOT EXISTS hr_employee_contract (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_id BIGINT NOT NULL,
  contract_no VARCHAR(64) NOT NULL,
  contract_type VARCHAR(20),
  start_date DATE,
  end_date DATE,
  sign_date DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  remark VARCHAR(500),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_no(contract_no),
  KEY idx_emp(emp_id),
  KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Employee contract';

CREATE TABLE IF NOT EXISTS hr_employee_change (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_id BIGINT NOT NULL,
  change_type VARCHAR(20) NOT NULL,
  before_dept_id BIGINT,
  after_dept_id BIGINT,
  before_post VARCHAR(100),
  after_post VARCHAR(100),
  effective_date DATE,
  reason VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'EFFECTIVE',
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_emp(emp_id),
  KEY idx_effective(effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Employee change';

CREATE TABLE IF NOT EXISTS hr_employee_certificate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_id BIGINT NOT NULL,
  certificate_name VARCHAR(100) NOT NULL,
  certificate_no VARCHAR(100),
  issue_org VARCHAR(100),
  issue_date DATE,
  expire_date DATE,
  attachment_url VARCHAR(500),
  status VARCHAR(16) NOT NULL DEFAULT 'VALID',
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_emp(emp_id),
  KEY idx_expire(expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Employee certificate';

CREATE TABLE IF NOT EXISTS hr_employee_education (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_id BIGINT NOT NULL,
  school_name VARCHAR(100) NOT NULL,
  major VARCHAR(100),
  degree VARCHAR(50),
  start_date DATE,
  end_date DATE,
  certificate_url VARCHAR(500),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_emp(emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Employee education';

CREATE TABLE IF NOT EXISTS fin_contract (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_no VARCHAR(64) NOT NULL,
  contract_name VARCHAR(200) NOT NULL,
  counterparty VARCHAR(200),
  amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  signed_date DATE,
  start_date DATE,
  end_date DATE,
  owner_emp_id BIGINT,
  dept_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  remark VARCHAR(500),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_contract_no(contract_no),
  KEY idx_dept_status(dept_id, status),
  KEY idx_owner(owner_emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Finance contract';

CREATE TABLE IF NOT EXISTS fin_payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_no VARCHAR(64) NOT NULL,
  contract_id BIGINT,
  expense_id BIGINT,
  payee VARCHAR(200),
  amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  planned_date DATE,
  paid_time DATETIME,
  pay_method VARCHAR(20),
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  remark VARCHAR(500),
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64),
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_payment_no(payment_no),
  KEY idx_contract(contract_id),
  KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Finance payment';
