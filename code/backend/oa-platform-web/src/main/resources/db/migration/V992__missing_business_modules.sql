-- V992: missing business modules

CREATE TABLE adm_supply_category (
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

CREATE TABLE adm_supply (
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

CREATE TABLE adm_supply_stock (
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

CREATE TABLE adm_supply_request (
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

CREATE TABLE adm_supply_request_item (
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

CREATE TABLE hr_employee_contract (
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

CREATE TABLE hr_employee_change (
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

CREATE TABLE hr_employee_certificate (
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

CREATE TABLE hr_employee_education (
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

CREATE TABLE fin_contract (
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

CREATE TABLE fin_payment (
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

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, sort_order, status, create_by)
SELECT 4, 'admin:supply', '办公用品管理', 'MENU', '/admin/supplies', 30, 'ACTIVE', 'system'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'admin:supply');

SET @admin_supply = (SELECT id FROM sys_permission WHERE perm_code = 'admin:supply' LIMIT 1);
INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by)
SELECT @admin_supply, v.perm_code, v.perm_name, 'BUTTON', v.sort_order, 'ACTIVE', 'system'
FROM (
  SELECT 'admin:supply:list' perm_code, '用品列表' perm_name, 1 sort_order UNION ALL
  SELECT 'admin:supply:create', '用品新增', 2 UNION ALL
  SELECT 'admin:supply:update', '用品修改', 3 UNION ALL
  SELECT 'admin:supply:delete', '用品删除', 4 UNION ALL
  SELECT 'admin:supply:stock', '库存调整', 5 UNION ALL
  SELECT 'admin:supply:request', '领用入库审批', 6
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, sort_order, status, create_by)
SELECT 0, v.perm_code, v.perm_name, 'BUTTON', v.sort_order, 'ACTIVE', 'system'
FROM (
  SELECT 'hr-performance:goal:list' perm_code, '绩效目标' perm_name, 4 sort_order UNION ALL
  SELECT 'hr-performance:eval:list', '绩效评估', 5 UNION ALL
  SELECT 'hr-performance:cycle:operate', '绩效流程', 6 UNION ALL
  SELECT 'hr-recruitment:candidate:list', '候选人管理', 2 UNION ALL
  SELECT 'hr-recruitment:interview:list', '面试管理', 3 UNION ALL
  SELECT 'hr-recruitment:offer:list', 'Offer管理', 4 UNION ALL
  SELECT 'hr-training:plan:list', '培训计划', 3 UNION ALL
  SELECT 'hr-training:enroll:list', '培训报名', 4 UNION ALL
  SELECT 'hr-training:record:list', '培训记录', 5 UNION ALL
  SELECT 'hr-employee:contract:list', '员工合同', 10 UNION ALL
  SELECT 'hr-employee:change:list', '员工异动', 11 UNION ALL
  SELECT 'hr-employee:certificate:list', '员工证书', 12 UNION ALL
  SELECT 'hr-employee:education:list', '教育经历', 13 UNION ALL
  SELECT 'finance:contract:list', '合同管理', 30 UNION ALL
  SELECT 'finance:payment:list', '付款管理', 31
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code);

INSERT INTO sys_role_permission (role_id, perm_id, create_by)
SELECT 1, p.id, 'system'
FROM sys_permission p
WHERE p.del_flag = '0'
  AND (
    p.perm_code LIKE 'admin:supply%'
    OR p.perm_code IN (
      'hr-performance:goal:list','hr-performance:eval:list','hr-performance:cycle:operate',
      'hr-recruitment:candidate:list','hr-recruitment:interview:list','hr-recruitment:offer:list',
      'hr-training:plan:list','hr-training:enroll:list','hr-training:record:list',
      'hr-employee:contract:list','hr-employee:change:list','hr-employee:certificate:list','hr-employee:education:list',
      'finance:contract:list','finance:payment:list'
    )
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.perm_id = p.id
  );
