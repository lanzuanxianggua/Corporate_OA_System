-- =============================================
-- OA系统数据库初始化脚本
-- 创建数据库和12张表，包含索引和注释说明
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE oa_system;

-- =============================================
-- 1. 部门表
-- 关联关系: parent_id 关联 sys_dept.id（上级部门）
-- =============================================
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name   VARCHAR(50)  NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父部门ID，0表示顶级部门',
    sort        INT          DEFAULT 0 COMMENT '显示排序',
    leader      VARCHAR(30)  DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    status      TINYINT      DEFAULT 1 COMMENT '状态（0停用 1正常）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- =============================================
-- 2. 员工表
-- 关联关系: dept_id 关联 sys_dept.id（所属部门）
-- =============================================
DROP TABLE IF EXISTS sys_employee;
CREATE TABLE sys_employee (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
    emp_code    VARCHAR(30)   NOT NULL COMMENT '员工编码（登录账号）',
    emp_name    VARCHAR(30)   NOT NULL COMMENT '员工姓名',
    password    VARCHAR(100)  NOT NULL COMMENT '登录密码（BCrypt加密）',
    phone       VARCHAR(20)   DEFAULT NULL COMMENT '手机号码',
    email       VARCHAR(80)   DEFAULT NULL COMMENT '邮箱地址',
    dept_id     BIGINT        DEFAULT NULL COMMENT '所属部门ID，关联sys_dept.id',
    avatar      VARCHAR(255)  DEFAULT NULL COMMENT '头像路径',
    status      TINYINT       DEFAULT 1 COMMENT '状态（0停用 1正常）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_emp_code (emp_code),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- =============================================
-- 3. 角色表
-- =============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50)  NOT NULL COMMENT '角色标识（权限字符串）',
    sort        INT          DEFAULT 0 COMMENT '显示排序',
    status      TINYINT      DEFAULT 1 COMMENT '状态（0停用 1正常）',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- =============================================
-- 4. 员工角色关联表
-- 关联关系: emp_id 关联 sys_employee.id，role_id 关联 sys_role.id
-- =============================================
DROP TABLE IF EXISTS sys_emp_role;
CREATE TABLE sys_emp_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id  BIGINT NOT NULL COMMENT '员工ID，关联sys_employee.id',
    role_id BIGINT NOT NULL COMMENT '角色ID，关联sys_role.id',
    UNIQUE INDEX uk_emp_role (emp_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工角色关联表';

-- =============================================
-- 5. 公告通知表
-- 关联关系: publisher_id 关联 sys_employee.id（发布人）
-- =============================================
DROP TABLE IF EXISTS oa_notice;
CREATE TABLE oa_notice (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title        VARCHAR(200) NOT NULL COMMENT '公告标题',
    content      TEXT         DEFAULT NULL COMMENT '公告内容',
    notice_type  TINYINT      DEFAULT 0 COMMENT '公告类型（0通知 1公告）',
    publisher_id BIGINT       DEFAULT NULL COMMENT '发布人ID，关联sys_employee.id',
    status       TINYINT      DEFAULT 0 COMMENT '状态（0未发布 1已发布 2已撤回）',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知表';

-- =============================================
-- 6. 公告已读表
-- 关联关系: notice_id 关联 oa_notice.id，emp_id 关联 sys_employee.id
-- =============================================
DROP TABLE IF EXISTS oa_notice_read;
CREATE TABLE oa_notice_read (
    id        BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    notice_id BIGINT   NOT NULL COMMENT '公告ID，关联oa_notice.id',
    emp_id    BIGINT   NOT NULL COMMENT '员工ID，关联sys_employee.id',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    UNIQUE INDEX uk_notice_emp (notice_id, emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读表';

-- =============================================
-- 7. 请假申请表
-- 关联关系: emp_id 关联 sys_employee.id（申请人）
-- =============================================
DROP TABLE IF EXISTS oa_leave_apply;
CREATE TABLE oa_leave_apply (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    emp_id     BIGINT       NOT NULL COMMENT '申请人ID，关联sys_employee.id',
    leave_type TINYINT      DEFAULT 0 COMMENT '请假类型（0事假 1病假 2年假 3婚假 4产假 5其他）',
    start_time DATETIME     NOT NULL COMMENT '开始时间',
    end_time   DATETIME     NOT NULL COMMENT '结束时间',
    reason     VARCHAR(500) DEFAULT NULL COMMENT '请假原因',
    status     TINYINT      DEFAULT 0 COMMENT '状态（0待审批 1审批通过 2审批驳回 3已撤回）',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_status (emp_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- =============================================
-- 8. 审批记录表
-- 关联关系: apply_id 关联 oa_leave_apply.id，approver_id 关联 sys_employee.id
-- =============================================
DROP TABLE IF EXISTS oa_approval_record;
CREATE TABLE oa_approval_record (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    apply_id       BIGINT       NOT NULL COMMENT '申请ID，关联oa_leave_apply.id',
    approver_id    BIGINT       NOT NULL COMMENT '审批人ID，关联sys_employee.id',
    approve_status TINYINT      DEFAULT 0 COMMENT '审批结果（0待审批 1通过 2驳回）',
    remark         VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
    approve_time   DATETIME     DEFAULT NULL COMMENT '审批时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- =============================================
-- 9. 考勤打卡表
-- 关联关系: emp_id 关联 sys_employee.id（打卡员工）
-- =============================================
DROP TABLE IF EXISTS oa_attendance;
CREATE TABLE oa_attendance (
    id        BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '考勤ID',
    emp_id    BIGINT       NOT NULL COMMENT '员工ID，关联sys_employee.id',
    work_date DATE         NOT NULL COMMENT '工作日期',
    clock_in  DATETIME     DEFAULT NULL COMMENT '上班打卡时间',
    clock_out DATETIME     DEFAULT NULL COMMENT '下班打卡时间',
    status    TINYINT      DEFAULT 0 COMMENT '考勤状态（0正常 1迟到 2早退 3缺勤 4请假）',
    remark    VARCHAR(200) DEFAULT NULL COMMENT '备注',
    UNIQUE INDEX uk_emp_date (emp_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡表';

-- =============================================
-- 10. 文档表
-- 关联关系: uploader_id 关联 sys_employee.id（上传人）
-- =============================================
DROP TABLE IF EXISTS oa_document;
CREATE TABLE oa_document (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    doc_name    VARCHAR(200) NOT NULL COMMENT '文档名称',
    file_path   VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size   BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    file_type   VARCHAR(20)  DEFAULT NULL COMMENT '文件类型（扩展名）',
    category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
    uploader_id BIGINT       DEFAULT NULL COMMENT '上传人ID，关联sys_employee.id',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- =============================================
-- 11. 日程表
-- 关联关系: emp_id 关联 sys_employee.id（日程所属员工）
-- =============================================
DROP TABLE IF EXISTS oa_schedule;
CREATE TABLE oa_schedule (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '日程ID',
    emp_id      BIGINT        NOT NULL COMMENT '员工ID，关联sys_employee.id',
    title       VARCHAR(200)  NOT NULL COMMENT '日程标题',
    content     VARCHAR(1000) DEFAULT NULL COMMENT '日程内容',
    start_time  DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME      DEFAULT NULL COMMENT '结束时间',
    remind_time DATETIME      DEFAULT NULL COMMENT '提醒时间',
    status      TINYINT       DEFAULT 0 COMMENT '状态（0未完成 1已完成）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程表';

-- =============================================
-- 12. 消息通知表
-- 关联关系: sender_id 关联 sys_employee.id（发送人），receiver_id 关联 sys_employee.id（接收人）
-- =============================================
DROP TABLE IF EXISTS oa_message;
CREATE TABLE oa_message (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    sender_id   BIGINT        DEFAULT NULL COMMENT '发送人ID，关联sys_employee.id',
    receiver_id BIGINT        NOT NULL COMMENT '接收人ID，关联sys_employee.id',
    msg_type    TINYINT       DEFAULT 0 COMMENT '消息类型（0系统通知 1审批通知 2公告通知）',
    title       VARCHAR(200)  DEFAULT NULL COMMENT '消息标题',
    content     VARCHAR(1000) DEFAULT NULL COMMENT '消息内容',
    is_read     TINYINT       DEFAULT 0 COMMENT '是否已读（0未读 1已读）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入默认部门：总公司
INSERT INTO sys_dept (id, dept_name, parent_id, sort, leader, phone, status)
VALUES (1, '总公司', 0, 0, NULL, NULL, 1);

-- 插入默认角色
INSERT INTO sys_role (id, role_name, role_key, sort, status)
VALUES (1, '管理员', 'ADMIN', 0, 1);
INSERT INTO sys_role (id, role_name, role_key, sort, status)
VALUES (2, '普通用户', 'USER', 1, 1);

-- 插入管理员账号（密码: 123456，BCrypt加密）
INSERT INTO sys_employee (emp_code, emp_name, password, dept_id, status)
VALUES ('ADMIN', '管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 1);

-- 管理员绑定管理员角色
INSERT INTO sys_emp_role (emp_id, role_id)
VALUES (1, 1);

-- =============================================
-- 测试员工数据（密码均为 123456，BCrypt加密）
-- =============================================

-- 插入测试部门
INSERT INTO sys_dept (id, dept_name, parent_id, sort, status) VALUES (2, '技术部', 1, 1, 1);
INSERT INTO sys_dept (id, dept_name, parent_id, sort, status) VALUES (3, '市场部', 1, 2, 1);
INSERT INTO sys_dept (id, dept_name, parent_id, sort, status) VALUES (4, '财务部', 1, 3, 1);
INSERT INTO sys_dept (id, dept_name, parent_id, sort, status) VALUES (5, '人事部', 1, 4, 1);

-- 插入测试员工
INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status)
VALUES (2, 'zhangsan', '张三', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000001', 'zhangsan@oa.com', 2, 1);

INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status)
VALUES (3, 'lisi', '李四', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000002', 'lisi@oa.com', 3, 1);

INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status)
VALUES (4, 'wangwu', '王五', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000003', 'wangwu@oa.com', 4, 1);

INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status)
VALUES (5, 'zhaoliu', '赵六', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000004', 'zhaoliu@oa.com', 5, 1);

INSERT INTO sys_employee (id, emp_code, emp_name, password, phone, email, dept_id, status)
VALUES (6, 'sunqi', '孙七', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000005', 'sunqi@oa.com', 2, 1);

-- 绑定普通用户角色
INSERT INTO sys_emp_role (emp_id, role_id) VALUES (2, 2);
INSERT INTO sys_emp_role (emp_id, role_id) VALUES (3, 2);
INSERT INTO sys_emp_role (emp_id, role_id) VALUES (4, 2);
INSERT INTO sys_emp_role (emp_id, role_id) VALUES (5, 2);
INSERT INTO sys_emp_role (emp_id, role_id) VALUES (6, 2);
