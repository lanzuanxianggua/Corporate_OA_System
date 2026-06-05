-- ============================================
-- V962__fin_workflow_defs.sql
-- 增量: 财务模块工作流定义 (报销 + 借款)
-- ============================================

-- 费用报销审批流程
INSERT INTO `wf_definitions` (`def_key`, `def_name`, `version`, `status`, `description`) VALUES
('finance_expense', '费用报销审批', 1, 'ACTIVE', '员工费用报销审批流程，含经理 + 财务二级审批');

SET @expense_def_id = LAST_INSERT_ID();

INSERT INTO `wf_nodes` (`def_id`, `node_key`, `node_name`, `node_type`, `sort_order`) VALUES
(@expense_def_id, 'start', '开始', 'START', 1),
(@expense_def_id, 'manager_approve', '经理审批', 'APPROVAL', 2),
(@expense_def_id, 'finance_approve', '财务审批', 'APPROVAL', 3),
(@expense_def_id, 'end', '结束', 'END', 4);

INSERT INTO `wf_transitions` (`def_id`, `from_node_id`, `to_node_id`, `action`)
SELECT @expense_def_id, n1.id, n2.id, 'APPROVE'
FROM `wf_nodes` n1 JOIN `wf_nodes` n2 ON n1.def_id = n2.def_id
WHERE n1.def_id = @expense_def_id AND n2.def_id = @expense_def_id
  AND ((n1.node_key = 'start' AND n2.node_key = 'manager_approve')
    OR (n1.node_key = 'manager_approve' AND n2.node_key = 'finance_approve')
    OR (n1.node_key = 'finance_approve' AND n2.node_key = 'end'));

-- 借款审批流程
INSERT INTO `wf_definitions` (`def_key`, `def_name`, `version`, `status`, `description`) VALUES
('finance_loan', '借款审批', 1, 'ACTIVE', '员工借款审批流程，含经理 + 财务二级审批');

SET @loan_def_id = LAST_INSERT_ID();

INSERT INTO `wf_nodes` (`def_id`, `node_key`, `node_name`, `node_type`, `sort_order`) VALUES
(@loan_def_id, 'start', '开始', 'START', 1),
(@loan_def_id, 'manager_approve', '经理审批', 'APPROVAL', 2),
(@loan_def_id, 'finance_approve', '财务审批', 'APPROVAL', 3),
(@loan_def_id, 'end', '结束', 'END', 4);

INSERT INTO `wf_transitions` (`def_id`, `from_node_id`, `to_node_id`, `action`)
SELECT @loan_def_id, n1.id, n2.id, 'APPROVE'
FROM `wf_nodes` n1 JOIN `wf_nodes` n2 ON n1.def_id = n2.def_id
WHERE n1.def_id = @loan_def_id AND n2.def_id = @loan_def_id
  AND ((n1.node_key = 'start' AND n2.node_key = 'manager_approve')
    OR (n1.node_key = 'manager_approve' AND n2.node_key = 'finance_approve')
    OR (n1.node_key = 'finance_approve' AND n2.node_key = 'end'));
