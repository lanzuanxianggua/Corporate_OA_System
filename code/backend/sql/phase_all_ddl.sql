-- =============================================
-- OA系统 全量DDL脚本
-- 包含：27张新增表 + 已有后加表DDL + ALTER语句
-- 数据库：MySQL 8.0, oa_system, utf8mb4
-- ID策略：Snowflake (ASSIGN_ID), BIGINT AUTO_INCREMENT
-- =============================================

CREATE DATABASE IF NOT EXISTS oa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE oa_system;

-- =============================================
-- 第一部分：已有后加表的DDL（已有实体类但未在init.sql中定义）
-- =============================================

-- ---------------------------------------------
-- oa_operation_log - 操作日志表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_operation_log;
CREATE TABLE oa_operation_log (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT        DEFAULT NULL COMMENT '操作人ID',
    emp_name    VARCHAR(30)   DEFAULT NULL COMMENT '操作人姓名',
    module      VARCHAR(50)   DEFAULT NULL COMMENT '操作模块',
    operation   VARCHAR(200)  DEFAULT NULL COMMENT '操作描述',
    method      VARCHAR(200)  DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(255)  DEFAULT NULL COMMENT '请求URL',
    ip          VARCHAR(50)   DEFAULT NULL COMMENT 'IP地址',
    status      INT           DEFAULT 1 COMMENT '操作状态（1成功 0失败）',
    cost_time   BIGINT        DEFAULT NULL COMMENT '耗时(ms)',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ---------------------------------------------
-- oa_login_log - 登录日志表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_login_log;
CREATE TABLE oa_login_log (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT        DEFAULT NULL COMMENT '员工ID',
    username    VARCHAR(30)   DEFAULT NULL COMMENT '登录账号',
    ip          VARCHAR(50)   DEFAULT NULL COMMENT '登录IP',
    browser     VARCHAR(50)   DEFAULT NULL COMMENT '浏览器',
    os          VARCHAR(50)   DEFAULT NULL COMMENT '操作系统',
    status      INT           DEFAULT 1 COMMENT '登录状态（1成功 0失败）',
    message     VARCHAR(200)  DEFAULT NULL COMMENT '提示消息',
    login_time  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ---------------------------------------------
-- oa_document_category - 文档分类表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_document_category;
CREATE TABLE oa_document_category (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50)   NOT NULL COMMENT '分类名称',
    parent_id   BIGINT        DEFAULT 0 COMMENT '父分类ID，0表示顶级',
    sort        INT           DEFAULT 0 COMMENT '排序号',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分类表';

-- ---------------------------------------------
-- oa_business_trip - 出差申请表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_business_trip;
CREATE TABLE oa_business_trip (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT        NOT NULL COMMENT '申请人ID',
    destination VARCHAR(100)  DEFAULT NULL COMMENT '目的地',
    purpose     VARCHAR(500)  DEFAULT NULL COMMENT '出差目的',
    start_time  DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME      DEFAULT NULL COMMENT '结束时间',
    status      INT           DEFAULT 0 COMMENT '状态（0待审批 1已通过 2已驳回 3已撤回）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差申请表';

-- ---------------------------------------------
-- oa_outing - 外出申请表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_outing;
CREATE TABLE oa_outing (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT        NOT NULL COMMENT '申请人ID',
    reason      VARCHAR(500)  DEFAULT NULL COMMENT '外出事由',
    destination VARCHAR(100)  DEFAULT NULL COMMENT '外出地点',
    start_time  DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME      DEFAULT NULL COMMENT '结束时间',
    status      INT           DEFAULT 0 COMMENT '状态（0待审批 1已通过 2已驳回 3已撤回）',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外出申请表';

-- ---------------------------------------------
-- oa_purchase - 采购申请表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_purchase;
CREATE TABLE oa_purchase (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT          NOT NULL COMMENT '申请人ID',
    item_name   VARCHAR(200)    DEFAULT NULL COMMENT '采购物品名称',
    quantity    INT             DEFAULT 1 COMMENT '数量',
    amount      DECIMAL(12,2)   DEFAULT NULL COMMENT '金额',
    reason      VARCHAR(500)    DEFAULT NULL COMMENT '采购原因',
    status      INT             DEFAULT 0 COMMENT '状态（0待审批 1已通过 2已驳回 3已撤回）',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请表';

-- ---------------------------------------------
-- oa_expense - 报销申请表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_expense;
CREATE TABLE oa_expense (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT          NOT NULL COMMENT '申请人ID',
    title       VARCHAR(200)    DEFAULT NULL COMMENT '报销标题',
    amount      DECIMAL(12,2)   DEFAULT NULL COMMENT '报销金额',
    category    VARCHAR(30)     DEFAULT NULL COMMENT '报销类别',
    description VARCHAR(500)    DEFAULT NULL COMMENT '报销说明',
    status      INT             DEFAULT 0 COMMENT '状态（0待审批 1已通过 2已驳回 3已撤回）',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销申请表';


-- =============================================
-- 第二部分：System Tables (Phase 1) - 系统基础表
-- =============================================

-- ---------------------------------------------
-- 1. sys_dict_type - 数据字典类型表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    dict_id     BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '字典类型ID',
    dict_name   VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典名称',
    dict_type   VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典类型（唯一标识）',
    status      CHAR(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

-- ---------------------------------------------
-- 2. sys_dict_data - 数据字典数据表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
    data_id     BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    dict_type   VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典类型',
    dict_label  VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典键值',
    dict_sort   INT          DEFAULT 0 COMMENT '字典排序',
    status      CHAR(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典数据表';

-- ---------------------------------------------
-- 3. sys_config - 系统参数配置表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    config_id   BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
    config_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数名称',
    config_key  VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数键名（唯一）',
    config_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参数键值',
    config_type CHAR(1)      DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

-- ---------------------------------------------
-- 4. sys_post - 岗位表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
    post_id     BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    post_code   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '岗位编码',
    post_name   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '岗位名称',
    post_sort   INT          DEFAULT 0 COMMENT '显示排序',
    status      CHAR(1)      DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_post_code (post_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- ---------------------------------------------
-- 5. sys_menu - 菜单权限表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    menu_id     BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id   BIGINT        DEFAULT 0 COMMENT '父菜单ID，0为顶级',
    menu_name   VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '菜单名称',
    path        VARCHAR(200)  DEFAULT '' COMMENT '路由地址',
    component   VARCHAR(255)  DEFAULT '' COMMENT '组件路径',
    perms       VARCHAR(100)  DEFAULT '' COMMENT '权限标识',
    menu_type   CHAR(1)       DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
    icon        VARCHAR(100)  DEFAULT '' COMMENT '菜单图标',
    order_num   INT           DEFAULT 0 COMMENT '显示排序',
    status      CHAR(1)       DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by   VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    update_time DATETIME      DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)       DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ---------------------------------------------
-- 6. sys_role_menu - 角色菜单关联表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    UNIQUE INDEX uk_role_menu (role_id, menu_id),
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';


-- =============================================
-- 第三部分：Workflow Tables (Phase 2) - 工作流表
-- =============================================

-- ---------------------------------------------
-- 7. wf_process_definition - 流程定义表
-- ---------------------------------------------
DROP TABLE IF EXISTS wf_process_definition;
CREATE TABLE wf_process_definition (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_name  VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '流程名称',
    process_key   VARCHAR(100)  NOT NULL DEFAULT '' COMMENT '流程唯一标识',
    process_type  VARCHAR(30)   DEFAULT '' COMMENT '流程类型（leave/trip/outing/purchase/expense/contract/...）',
    node_config   TEXT          DEFAULT NULL COMMENT '审批节点链配置（JSON格式）',
    status        CHAR(1)       DEFAULT '0' COMMENT '状态（0启用 1停用）',
    version       INT           DEFAULT 1 COMMENT '版本号',
    create_by     VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    create_time   DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    update_time   DATETIME      DEFAULT NULL COMMENT '更新时间',
    del_flag      CHAR(1)       DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_process_key (process_key),
    INDEX idx_process_type (process_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ---------------------------------------------
-- 8. wf_process_instance - 流程实例表
-- ---------------------------------------------
DROP TABLE IF EXISTS wf_process_instance;
CREATE TABLE wf_process_instance (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_id     BIGINT       NOT NULL COMMENT '流程定义ID',
    business_type  VARCHAR(30)  DEFAULT '' COMMENT '业务类型（leave/trip/outing/...）',
    business_id    BIGINT       DEFAULT NULL COMMENT '业务表主键ID',
    initiator_id   BIGINT       NOT NULL COMMENT '发起人ID',
    current_node   INT          DEFAULT 0 COMMENT '当前审批节点序号',
    condition_context TEXT       DEFAULT NULL COMMENT '条件上下文JSON',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态（0运行中 1已通过 2已驳回 3已撤回）',
    start_time     DATETIME     DEFAULT NULL COMMENT '发起时间',
    end_time       DATETIME     DEFAULT NULL COMMENT '结束时间',
    create_by      VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time    DATETIME     DEFAULT NULL COMMENT '创建时间',
    del_flag       CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_process_id (process_id),
    INDEX idx_business (business_type, business_id),
    INDEX idx_initiator_id (initiator_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- ---------------------------------------------
-- 9. wf_task - 工作流任务表
-- ---------------------------------------------
DROP TABLE IF EXISTS wf_task;
CREATE TABLE wf_task (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    instance_id      BIGINT       NOT NULL COMMENT '流程实例ID',
    node_id          BIGINT       DEFAULT NULL COMMENT '节点ID',
    node_name        VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
    assignee_id      BIGINT       NOT NULL COMMENT '审批人ID',
    task_type        VARCHAR(20)  NOT NULL DEFAULT 'TODO' COMMENT '任务类型',
    parent_task_id   BIGINT       DEFAULT NULL COMMENT '父任务ID',
    status           VARCHAR(20)  NOT NULL DEFAULT '0' COMMENT '状态：0待审批 1通过 2驳回 3转办 4撤回 5退回',
    opinion          VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    signature        VARCHAR(200) DEFAULT NULL COMMENT '电子签名',
    due_time         DATETIME     DEFAULT NULL COMMENT '截止时间',
    complete_time    DATETIME     DEFAULT NULL COMMENT '完成时间',
    remind_count     INT          NOT NULL DEFAULT 0 COMMENT '催办次数',
    escalation_count INT          NOT NULL DEFAULT 0 COMMENT '升级次数',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_instance_id (instance_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_node_id (node_id),
    INDEX idx_due_time (due_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流任务表';


-- =============================================
-- 第四部分：Todo & Collaboration Tables (Phase 2)
-- =============================================

-- ---------------------------------------------
-- 10. oa_todo - 统一待办中心表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_todo;
CREATE TABLE oa_todo (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id        BIGINT       NOT NULL COMMENT '待办人ID',
    title         VARCHAR(200) NOT NULL DEFAULT '' COMMENT '待办标题',
    todo_type     VARCHAR(30)  DEFAULT '' COMMENT '待办类型（approval/notice/meeting/task）',
    business_id   BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    business_type VARCHAR(30)  DEFAULT '' COMMENT '关联业务类型',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态（0待办 1已办 2已忽略）',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    done_time     DATETIME     DEFAULT NULL COMMENT '完成时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status),
    INDEX idx_todo_type (todo_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一待办中心表';

-- ---------------------------------------------
-- 11. oa_meeting_room - 会议室表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_meeting_room;
CREATE TABLE oa_meeting_room (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    room_name   VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '会议室名称',
    location    VARCHAR(100)  DEFAULT '' COMMENT '位置',
    capacity    INT           DEFAULT 0 COMMENT '容纳人数',
    equipment   VARCHAR(200)  DEFAULT '' COMMENT '设备说明',
    status      CHAR(1)       DEFAULT '0' COMMENT '状态（0可用 1停用）',
    create_by   VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    create_time DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_by   VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    update_time DATETIME      DEFAULT NULL COMMENT '更新时间',
    del_flag    CHAR(1)       DEFAULT '0' COMMENT '删除标志（0存在 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';

-- ---------------------------------------------
-- 12. oa_meeting - 会议表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_meeting;
CREATE TABLE oa_meeting (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title         VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '会议标题',
    room_id       BIGINT        DEFAULT NULL COMMENT '会议室ID',
    organizer_id  BIGINT        NOT NULL COMMENT '组织者ID',
    start_time    DATETIME      DEFAULT NULL COMMENT '开始时间',
    end_time      DATETIME      DEFAULT NULL COMMENT '结束时间',
    description   TEXT          DEFAULT NULL COMMENT '会议描述',
    participants  VARCHAR(500)  DEFAULT NULL COMMENT '参会人员ID（JSON数组）',
    status        CHAR(1)       DEFAULT '0' COMMENT '状态（0未开始 1进行中 2已结束 3已取消）',
    create_by     VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    create_time   DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    update_time   DATETIME      DEFAULT NULL COMMENT '更新时间',
    del_flag      CHAR(1)       DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_room_id (room_id),
    INDEX idx_organizer_id (organizer_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议表';


-- =============================================
-- 第五部分：HR Tables (Phase 3) - 人力资源表
-- =============================================

-- ---------------------------------------------
-- 13. oa_attendance_group - 考勤组表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_attendance_group;
CREATE TABLE oa_attendance_group (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_name       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '考勤组名称',
    work_start       TIME         DEFAULT NULL COMMENT '上班时间',
    work_end         TIME         DEFAULT NULL COMMENT '下班时间',
    late_threshold   INT          DEFAULT 15 COMMENT '迟到阈值（分钟）',
    status           CHAR(1)      DEFAULT '0' COMMENT '状态（0启用 1停用）',
    create_by        VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time      DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by        VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time      DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag         CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

-- ---------------------------------------------
-- 14. oa_attendance_group_emp - 考勤组员工关联表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_attendance_group_emp;
CREATE TABLE oa_attendance_group_emp (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '考勤组ID',
    emp_id   BIGINT NOT NULL COMMENT '员工ID',
    UNIQUE INDEX uk_group_emp (group_id, emp_id),
    INDEX idx_emp_id (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组员工关联表';

-- ---------------------------------------------
-- 15. oa_leave_balance - 假期余额表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_leave_balance;
CREATE TABLE oa_leave_balance (
    id              BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id          BIGINT         NOT NULL COMMENT '员工ID',
    leave_type      INT            DEFAULT 0 COMMENT '假期类型（1事假 2病假 3年假 4调休 5婚假 6产假）',
    year            INT            DEFAULT NULL COMMENT '年份',
    total_days      DECIMAL(6,1)   DEFAULT 0 COMMENT '总额（天）',
    used_days       DECIMAL(6,1)   DEFAULT 0 COMMENT '已用（天）',
    remaining_days  DECIMAL(6,1)   DEFAULT 0 COMMENT '剩余（天）',
    create_time     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT NULL COMMENT '更新时间',
    INDEX idx_emp_id (emp_id),
    UNIQUE INDEX uk_emp_type_year (emp_id, leave_type, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额表';

-- ---------------------------------------------
-- 16. oa_overtime - 加班记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_overtime;
CREATE TABLE oa_overtime (
    id            BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id        BIGINT          NOT NULL COMMENT '员工ID',
    overtime_date DATE            DEFAULT NULL COMMENT '加班日期',
    start_time    DATETIME        DEFAULT NULL COMMENT '加班开始时间',
    end_time      DATETIME        DEFAULT NULL COMMENT '加班结束时间',
    hours         DECIMAL(4,1)    DEFAULT 0 COMMENT '加班时长（小时）',
    reason        VARCHAR(500)    DEFAULT NULL COMMENT '加班原因',
    status        CHAR(1)         DEFAULT '0' COMMENT '状态（0待审批 1已通过 2已驳回）',
    create_by     VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time   DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time   DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag      CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_emp_id (emp_id),
    INDEX idx_overtime_date (overtime_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录表';

-- ---------------------------------------------
-- 17. oa_salary_structure - 薪资结构表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_salary_structure;
CREATE TABLE oa_salary_structure (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id          BIGINT          NOT NULL COMMENT '员工ID',
    base_salary     DECIMAL(12,2)   DEFAULT 0 COMMENT '基本工资',
    post_salary     DECIMAL(12,2)   DEFAULT 0 COMMENT '岗位工资',
    merit_salary    DECIMAL(12,2)   DEFAULT 0 COMMENT '绩效工资',
    allowance       DECIMAL(12,2)   DEFAULT 0 COMMENT '津贴补贴',
    effective_date  DATE            DEFAULT NULL COMMENT '生效日期',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0启用 1停用）',
    create_by       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_emp_id (emp_id),
    INDEX idx_effective_date (effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资结构表';

-- ---------------------------------------------
-- 18. oa_salary_record - 薪资发放记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_salary_record;
CREATE TABLE oa_salary_record (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id          BIGINT          NOT NULL COMMENT '员工ID',
    salary_month    VARCHAR(7)      NOT NULL DEFAULT '' COMMENT '薪资月份（yyyy-MM）',
    base_salary     DECIMAL(12,2)   DEFAULT 0 COMMENT '基本工资',
    post_salary     DECIMAL(12,2)   DEFAULT 0 COMMENT '岗位工资',
    merit_salary    DECIMAL(12,2)   DEFAULT 0 COMMENT '绩效工资',
    allowance       DECIMAL(12,2)   DEFAULT 0 COMMENT '津贴补贴',
    deduction       DECIMAL(12,2)   DEFAULT 0 COMMENT '扣款',
    actual_amount   DECIMAL(12,2)   DEFAULT 0 COMMENT '实发金额',
    pay_time        DATETIME        DEFAULT NULL COMMENT '发放时间',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0草稿 1已发放）',
    create_by       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_salary_month (salary_month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资发放记录表';

-- ---------------------------------------------
-- 19. oa_emp_archive - 员工档案表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_emp_archive;
CREATE TABLE oa_emp_archive (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id              BIGINT        NOT NULL COMMENT '员工ID',
    education           VARCHAR(20)   DEFAULT NULL COMMENT '学历',
    major               VARCHAR(50)   DEFAULT NULL COMMENT '专业',
    graduate_school     VARCHAR(100)  DEFAULT NULL COMMENT '毕业院校',
    entry_date          DATE          DEFAULT NULL COMMENT '入职日期',
    probation_end_date  DATE          DEFAULT NULL COMMENT '试用期截止日期',
    contract_start      DATE          DEFAULT NULL COMMENT '合同开始日期',
    contract_end        DATE          DEFAULT NULL COMMENT '合同结束日期',
    emergency_contact   VARCHAR(50)   DEFAULT NULL COMMENT '紧急联系人',
    emergency_phone     VARCHAR(20)   DEFAULT NULL COMMENT '紧急联系电话',
    address             VARCHAR(200)  DEFAULT NULL COMMENT '住址',
    remark              TEXT          DEFAULT NULL COMMENT '备注',
    create_by           VARCHAR(64)   DEFAULT '' COMMENT '创建者',
    create_time         DATETIME      DEFAULT NULL COMMENT '创建时间',
    update_by           VARCHAR(64)   DEFAULT '' COMMENT '更新者',
    update_time         DATETIME      DEFAULT NULL COMMENT '更新时间',
    del_flag            CHAR(1)       DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_emp_id (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案表';


-- =============================================
-- 第六部分：Asset & Contract Tables (Phase 3)
-- =============================================

-- ---------------------------------------------
-- 20. oa_asset - 资产管理表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_asset;
CREATE TABLE oa_asset (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    asset_code      VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '资产编号',
    asset_name      VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '资产名称',
    category        VARCHAR(30)     DEFAULT '' COMMENT '资产分类',
    specification   VARCHAR(100)    DEFAULT '' COMMENT '规格型号',
    purchase_date   DATE            DEFAULT NULL COMMENT '购置日期',
    purchase_price  DECIMAL(12,2)   DEFAULT 0 COMMENT '购置价格',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0闲置 1在用 2维修 3报废）',
    current_user_id BIGINT          DEFAULT NULL COMMENT '当前使用人ID',
    dept_id         BIGINT          DEFAULT NULL COMMENT '所属部门ID',
    create_by       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_asset_code (asset_code),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_current_user_id (current_user_id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产管理表';

-- ---------------------------------------------
-- 21. oa_asset_borrow - 资产借用记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_asset_borrow;
CREATE TABLE oa_asset_borrow (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    asset_id         BIGINT       NOT NULL COMMENT '资产ID',
    borrower_id      BIGINT       NOT NULL COMMENT '借用人ID',
    borrow_time      DATETIME     DEFAULT NULL COMMENT '借用时间',
    expected_return  DATETIME     DEFAULT NULL COMMENT '预计归还时间',
    actual_return    DATETIME     DEFAULT NULL COMMENT '实际归还时间',
    status           CHAR(1)      DEFAULT '0' COMMENT '状态（0借出 1已还）',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_asset_id (asset_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产借用记录表';

-- ---------------------------------------------
-- 22. oa_contract - 合同管理表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_contract;
CREATE TABLE oa_contract (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    contract_no     VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '合同编号',
    contract_name   VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '合同名称',
    contract_type   VARCHAR(30)     DEFAULT '' COMMENT '合同类型',
    party_a         VARCHAR(100)    DEFAULT '' COMMENT '甲方',
    party_b         VARCHAR(100)    DEFAULT '' COMMENT '乙方',
    amount          DECIMAL(14,2)   DEFAULT 0 COMMENT '合同金额',
    sign_date       DATE            DEFAULT NULL COMMENT '签订日期',
    start_date      DATE            DEFAULT NULL COMMENT '合同开始日期',
    end_date        DATE            DEFAULT NULL COMMENT '合同结束日期',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0草稿 1生效 2即将到期 3已过期 4已终止）',
    manager_id      BIGINT          DEFAULT NULL COMMENT '负责人ID',
    file_url        VARCHAR(500)    DEFAULT NULL COMMENT '合同文件路径',
    create_by       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    UNIQUE INDEX uk_contract_no (contract_no),
    INDEX idx_contract_type (contract_type),
    INDEX idx_status (status),
    INDEX idx_manager_id (manager_id),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同管理表';


-- =============================================
-- 第七部分：Financial Tables (Phase 3)
-- =============================================

-- ---------------------------------------------
-- 23. oa_budget - 预算管理表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_budget;
CREATE TABLE oa_budget (
    id           BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dept_id      BIGINT          NOT NULL COMMENT '部门ID',
    budget_year  INT             DEFAULT NULL COMMENT '预算年度',
    budget_month INT             DEFAULT NULL COMMENT '预算月份',
    amount       DECIMAL(14,2)   DEFAULT 0 COMMENT '预算金额',
    used_amount  DECIMAL(14,2)   DEFAULT 0 COMMENT '已使用金额',
    status       CHAR(1)         DEFAULT '0' COMMENT '状态（0草稿 1已审批）',
    create_by    VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time  DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by    VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time  DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag     CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_dept_id (dept_id),
    INDEX idx_budget_year_month (budget_year, budget_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理表';

-- ---------------------------------------------
-- 24. oa_loan - 借款管理表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_loan;
CREATE TABLE oa_loan (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    emp_id          BIGINT          NOT NULL COMMENT '借款人ID',
    loan_amount     DECIMAL(12,2)   DEFAULT 0 COMMENT '借款金额',
    loan_reason     VARCHAR(500)    DEFAULT NULL COMMENT '借款原因',
    repayment_plan  TEXT            DEFAULT NULL COMMENT '还款计划',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0待审批 1已通过 2已驳回 3部分还款 4已还清）',
    create_by       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_emp_id (emp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借款管理表';

-- ---------------------------------------------
-- 25. oa_loan_repayment - 借款还款记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS oa_loan_repayment;
CREATE TABLE oa_loan_repayment (
    id           BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    loan_id      BIGINT          NOT NULL COMMENT '借款ID',
    amount       DECIMAL(12,2)   DEFAULT 0 COMMENT '还款金额',
    repay_time   DATETIME        DEFAULT NULL COMMENT '还款时间',
    remark       VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_time  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_loan_id (loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借款还款记录表';


-- =============================================
-- 第八部分：Alert Tables (Phase 4) - 预警规则表
-- =============================================

-- ---------------------------------------------
-- 26. rpt_alert_rule - 预警规则表
-- ---------------------------------------------
DROP TABLE IF EXISTS rpt_alert_rule;
CREATE TABLE rpt_alert_rule (
    id             BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_name      VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '规则名称',
    rule_type      VARCHAR(30)     DEFAULT '' COMMENT '规则类型',
    metric         VARCHAR(50)     DEFAULT '' COMMENT '监控指标',
    condition_type VARCHAR(10)     DEFAULT '' COMMENT '比较条件（gt/lt/eq/between）',
    threshold      DECIMAL(12,2)   DEFAULT 0 COMMENT '阈值',
    threshold_max  DECIMAL(12,2)   DEFAULT NULL COMMENT '阈值上限（between时使用）',
    check_cron     VARCHAR(50)     DEFAULT '' COMMENT '检查周期（Cron表达式）',
    notify_type    VARCHAR(30)     DEFAULT '' COMMENT '通知方式（email/sms/system）',
    notify_targets VARCHAR(500)    DEFAULT '' COMMENT '通知目标（JSON）',
    status         CHAR(1)         DEFAULT '0' COMMENT '状态（0启用 1停用）',
    create_by      VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    create_time    DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by      VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    update_time    DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag       CHAR(1)         DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    INDEX idx_rule_type (rule_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- ---------------------------------------------
-- 27. rpt_alert_log - 预警日志表
-- ---------------------------------------------
DROP TABLE IF EXISTS rpt_alert_log;
CREATE TABLE rpt_alert_log (
    id             BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_id        BIGINT          NOT NULL COMMENT '规则ID',
    alert_level    CHAR(1)         DEFAULT '0' COMMENT '预警级别（0普通 1重要 2紧急）',
    metric_value   DECIMAL(12,2)   DEFAULT 0 COMMENT '实际指标值',
    threshold      DECIMAL(12,2)   DEFAULT 0 COMMENT '阈值',
    alert_content  VARCHAR(500)    DEFAULT '' COMMENT '预警内容',
    notify_status  CHAR(1)         DEFAULT '0' COMMENT '通知状态（0未通知 1已通知 2通知失败）',
    handle_status  CHAR(1)         DEFAULT '0' COMMENT '处理状态（0未处理 1已处理）',
    handler        VARCHAR(30)     DEFAULT '' COMMENT '处理人',
    handle_remark  VARCHAR(200)    DEFAULT '' COMMENT '处理备注',
    alert_time     DATETIME        DEFAULT NULL COMMENT '预警时间',
    handle_time    DATETIME        DEFAULT NULL COMMENT '处理时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_alert_level (alert_level),
    INDEX idx_alert_time (alert_time),
    INDEX idx_handle_status (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警日志表';


-- =============================================
-- 第九部分：ALTER 现有表 — 补充通用字段
-- =============================================

-- sys_employee: 添加 del_flag, create_by, update_by, post_id
ALTER TABLE sys_employee
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    ADD COLUMN post_id    BIGINT       DEFAULT NULL COMMENT '岗位ID';

-- sys_dept: 添加 del_flag, create_by, update_by
ALTER TABLE sys_dept
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_notice: 添加 del_flag, create_by, update_by
ALTER TABLE oa_notice
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_leave_apply: 添加 del_flag, create_by, update_by
ALTER TABLE oa_leave_apply
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_attendance: 添加 del_flag, create_by, update_by
ALTER TABLE oa_attendance
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_document: 添加 del_flag, create_by, update_by
ALTER TABLE oa_document
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_schedule: 添加 del_flag, create_by, update_by
ALTER TABLE oa_schedule
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_message: 添加 del_flag, create_by, update_by
ALTER TABLE oa_message
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_business_trip: 添加 del_flag, create_by, update_by
ALTER TABLE oa_business_trip
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_outing: 添加 del_flag, create_by, update_by
ALTER TABLE oa_outing
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_purchase: 添加 del_flag, create_by, update_by
ALTER TABLE oa_purchase
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';

-- oa_expense: 添加 del_flag, create_by, update_by
ALTER TABLE oa_expense
    ADD COLUMN del_flag   CHAR(1)      DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    ADD COLUMN create_by  VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    ADD COLUMN update_by  VARCHAR(64)  DEFAULT '' COMMENT '更新者';
