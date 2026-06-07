-- ============================================
-- V972__doc_workflow_defs.sql
-- 增量: 文档模块工作流定义 (发文 + 签报)
-- 模式参考 V962__fin_workflow_defs.sql
-- ============================================

-- 发文审批流程 (document_dispatch)
INSERT INTO `wf_definitions` (`def_key`, `def_name`, `version`, `status`, `description`) VALUES
('document_dispatch', '发文审批', 1, 'ACTIVE', '公文发文审批流程，含部门负责人 + 主管副总二级审批');

SET @dispatch_def_id = LAST_INSERT_ID();

INSERT INTO `wf_nodes` (`def_id`, `node_key`, `node_name`, `node_type`, `sort_order`) VALUES
(@dispatch_def_id, 'start', '开始', 'START', 1),
(@dispatch_def_id, 'dept_lead_approve', '部门负责人审批', 'APPROVAL', 2),
(@dispatch_def_id, 'leader_approve', '主管领导审批', 'APPROVAL', 3),
(@dispatch_def_id, 'end', '结束', 'END', 4);

INSERT INTO `wf_transitions` (`def_id`, `from_node_id`, `to_node_id`, `action`)
SELECT @dispatch_def_id, n1.id, n2.id, 'APPROVE'
FROM `wf_nodes` n1 JOIN `wf_nodes` n2 ON n1.def_id = n2.def_id
WHERE n1.def_id = @dispatch_def_id AND n2.def_id = @dispatch_def_id
  AND ((n1.node_key = 'start' AND n2.node_key = 'dept_lead_approve')
    OR (n1.node_key = 'dept_lead_approve' AND n2.node_key = 'leader_approve')
    OR (n1.node_key = 'leader_approve' AND n2.node_key = 'end'));

-- 签报审批流程 (document_sign_report)
INSERT INTO `wf_definitions` (`def_key`, `def_name`, `version`, `status`, `description`) VALUES
('document_sign_report', '签报审批', 1, 'ACTIVE', '签报审批流程，含部门负责人 + 主管副总二级审批');

SET @sign_report_def_id = LAST_INSERT_ID();

INSERT INTO `wf_nodes` (`def_id`, `node_key`, `node_name`, `node_type`, `sort_order`) VALUES
(@sign_report_def_id, 'start', '开始', 'START', 1),
(@sign_report_def_id, 'dept_lead_approve', '部门负责人审批', 'APPROVAL', 2),
(@sign_report_def_id, 'leader_approve', '主管领导审批', 'APPROVAL', 3),
(@sign_report_def_id, 'end', '结束', 'END', 4);

INSERT INTO `wf_transitions` (`def_id`, `from_node_id`, `to_node_id`, `action`)
SELECT @sign_report_def_id, n1.id, n2.id, 'APPROVE'
FROM `wf_nodes` n1 JOIN `wf_nodes` n2 ON n1.def_id = n2.def_id
WHERE n1.def_id = @sign_report_def_id AND n2.def_id = @sign_report_def_id
  AND ((n1.node_key = 'start' AND n2.node_key = 'dept_lead_approve')
    OR (n1.node_key = 'dept_lead_approve' AND n2.node_key = 'leader_approve')
    OR (n1.node_key = 'leader_approve' AND n2.node_key = 'end'));
