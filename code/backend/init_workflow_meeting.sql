-- =====================================================
-- 工作流引擎 + 待办中心 + 会议管理 建表脚本
-- =====================================================

-- 流程定义表
CREATE TABLE IF NOT EXISTS `wf_process_definition` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `process_name` VARCHAR(100) NOT NULL COMMENT '流程名称',
    `process_key` VARCHAR(50) DEFAULT NULL COMMENT '流程标识',
    `process_type` VARCHAR(30) NOT NULL COMMENT '流程类型: leave/trip/outing/purchase/expense/contract/overtime/loan',
    `node_config` TEXT DEFAULT NULL COMMENT '审批节点配置JSON数组: [{nodeIndex, nodeName, assigneeType, assigneeValue}]',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-启用 1-禁用',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- 流程实例表
CREATE TABLE IF NOT EXISTS `wf_process_instance` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `process_id` BIGINT NOT NULL COMMENT '流程定义ID',
    `business_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
    `business_id` BIGINT NOT NULL COMMENT '业务ID',
    `initiator_id` BIGINT NOT NULL COMMENT '发起人ID',
    `current_node` INT DEFAULT 0 COMMENT '当前节点索引',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-运行中 1-已通过 2-已驳回 3-已取消',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_initiator` (`initiator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- 工作流任务表
CREATE TABLE IF NOT EXISTS `wf_task` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
    `process_id` BIGINT NOT NULL COMMENT '流程定义ID',
    `node_index` INT NOT NULL COMMENT '节点索引',
    `node_name` VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
    `assignee_id` BIGINT NOT NULL COMMENT '处理人ID',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-待处理 1-已通过 2-已驳回 3-已转办',
    `action_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance` (`instance_id`),
    KEY `idx_assignee` (`assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流任务表';

-- 待办事项表
CREATE TABLE IF NOT EXISTS `oa_todo` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `emp_id` BIGINT NOT NULL COMMENT '员工ID',
    `title` VARCHAR(200) NOT NULL COMMENT '待办标题',
    `todo_type` VARCHAR(30) NOT NULL COMMENT '待办类型: approval/meeting/notice/task',
    `business_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `business_type` VARCHAR(30) DEFAULT NULL COMMENT '关联业务类型',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-待处理 1-已完成 2-已忽略',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `done_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_emp_status` (`emp_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';

-- 会议室表
CREATE TABLE IF NOT EXISTS `oa_meeting_room` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `room_name` VARCHAR(100) NOT NULL COMMENT '会议室名称',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置',
    `capacity` INT DEFAULT NULL COMMENT '容纳人数',
    `equipment` VARCHAR(500) DEFAULT NULL COMMENT '设备描述',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-可用 1-停用',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室表';

-- 会议表
CREATE TABLE IF NOT EXISTS `oa_meeting` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `title` VARCHAR(200) NOT NULL COMMENT '会议标题',
    `room_id` BIGINT DEFAULT NULL COMMENT '会议室ID',
    `organizer_id` BIGINT NOT NULL COMMENT '组织者ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `description` TEXT DEFAULT NULL COMMENT '会议描述',
    `participants` TEXT DEFAULT NULL COMMENT '参会人ID的JSON数组',
    `status` CHAR(1) DEFAULT '0' COMMENT '状态: 0-已预约 1-进行中 2-已完成 3-已取消',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `del_flag` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_organizer` (`organizer_id`),
    KEY `idx_room_time` (`room_id`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议表';

-- =====================================================
-- 初始化流程定义数据
-- =====================================================

-- 请假审批流程
INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`) VALUES
(1, '请假审批流程', 'leave_approval', 'leave',
 '[{"nodeIndex":0,"nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager"}]',
 '0', 1);

-- 出差审批流程
INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`) VALUES
(2, '出差审批流程', 'trip_approval', 'trip',
 '[{"nodeIndex":0,"nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager"}]',
 '0', 1);

-- 外出审批流程
INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`) VALUES
(3, '外出审批流程', 'outing_approval', 'outing',
 '[{"nodeIndex":0,"nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager"}]',
 '0', 1);

-- 采购审批流程
INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`) VALUES
(4, '采购审批流程', 'purchase_approval', 'purchase',
 '[{"nodeIndex":0,"nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager"},{"nodeIndex":1,"nodeName":"总经理审批","assigneeType":"specific","assigneeValue":"1"}]',
 '0', 1);

-- 报销审批流程
INSERT INTO `wf_process_definition` (`id`, `process_name`, `process_key`, `process_type`, `node_config`, `status`, `version`) VALUES
(5, '报销审批流程', 'expense_approval', 'expense',
 '[{"nodeIndex":0,"nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager"}]',
 '0', 1);

-- =====================================================
-- 初始化会议室数据
-- =====================================================

INSERT INTO `oa_meeting_room` (`id`, `room_name`, `location`, `capacity`, `equipment`, `status`) VALUES
(1, '第一会议室', 'A栋3楼301', 10, '投影仪、白板', '0'),
(2, '第二会议室', 'A栋3楼302', 20, '投影仪、视频会议系统、白板', '0'),
(3, '多功能报告厅', 'B栋1楼', 100, '投影仪、音响、麦克风、视频会议系统', '0'),
(4, '小型讨论室', 'A栋4楼401', 6, '白板、电视', '0');
