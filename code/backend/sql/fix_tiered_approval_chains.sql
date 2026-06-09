-- Enable tiered approval chains:
-- department manager -> director -> general manager.
-- Existing process instances keep their original snapshot_node_config.

START TRANSACTION;

UPDATE sys_role
SET status = '0'
WHERE role_key IN ('DEPT_MANAGER', 'DIRECTOR', 'GM');

UPDATE wf_process_definition
SET status = '1',
    update_by = 'system',
    update_time = NOW()
WHERE process_type IN ('leave', 'trip', 'outing', 'overtime', 'purchase', 'expense', 'loan')
  AND del_flag = '0'
  AND status = '0';

INSERT INTO wf_process_definition
  (process_name, process_key, process_type, node_config, status, version, create_by, create_time, update_by, update_time, del_flag)
VALUES
('请假审批流程(逐级分级)', 'leave_tiered_chain_v2', 'leave',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_days","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按请假天数分级","branches":[{"when":"days > 3","to":"n_director"},{"when":"days <= 3","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"days > 7","skipTo":"n_gm"}],"timeoutHours":48,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":72,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_days"},{"source":"gw_days","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('出差审批流程(逐级分级)', 'trip_tiered_chain_v2', 'trip',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_days","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按出差天数分级","branches":[{"when":"days > 3","to":"n_director"},{"when":"days <= 3","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"days > 7","skipTo":"n_gm"}],"timeoutHours":48,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":72,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_days"},{"source":"gw_days","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('外出审批流程(逐级分级)', 'outing_tiered_chain_v2', 'outing',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":12,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_days","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按外出天数分级","branches":[{"when":"days > 3","to":"n_director"},{"when":"days <= 3","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"days > 7","skipTo":"n_gm"}],"timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":48,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_days"},{"source":"gw_days","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('加班审批流程(逐级分级)', 'overtime_tiered_chain_v2', 'overtime',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":12,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_hours","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按加班小时分级","branches":[{"when":"hours > 8","to":"n_director"},{"when":"hours <= 8","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"hours > 36","skipTo":"n_gm"}],"timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":48,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_hours"},{"source":"gw_hours","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('采购审批流程(逐级分级)', 'purchase_tiered_chain_v2', 'purchase',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_amount","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按采购金额分级","branches":[{"when":"amount > 5000","to":"n_director"},{"when":"amount <= 5000","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"amount > 50000","skipTo":"n_gm"}],"timeoutHours":48,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":72,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_amount"},{"source":"gw_amount","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('报销审批流程(逐级分级)', 'expense_tiered_chain_v2', 'expense',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_amount","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按报销金额分级","branches":[{"when":"amount > 5000","to":"n_director"},{"when":"amount <= 5000","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"amount > 50000","skipTo":"n_gm"}],"timeoutHours":48,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":72,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_amount"},{"source":"gw_amount","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0'),

('借款审批流程(逐级分级)', 'loan_tiered_chain_v2', 'loan',
'{"schemaVersion":2,"nodes":[{"nodeId":"start","nodeType":"start","nodeName":"开始"},{"nodeId":"n_manager","nodeType":"approval","nodeName":"部门主管审批","assigneeType":"dept_manager","assigneeValue":"dept_manager","timeoutHours":24,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"DIRECTOR"}},{"nodeId":"gw_amount","nodeType":"gateway","gatewayType":"exclusive","nodeName":"按借款金额分级","branches":[{"when":"amount > 5000","to":"n_director"},{"when":"amount <= 5000","to":"end"}]},{"nodeId":"n_director","nodeType":"approval","nodeName":"总监审批","assigneeType":"role_global","assigneeValue":"DIRECTOR","routingRules":[{"when":"amount > 50000","skipTo":"n_gm"}],"timeoutHours":48,"timeoutAction":"escalate","escalateTo":{"type":"role_global","value":"GM"}},{"nodeId":"n_gm","nodeType":"approval","nodeName":"总经理审批","assigneeType":"role_global","assigneeValue":"GM","timeoutHours":72,"timeoutAction":"auto_reject"},{"nodeId":"end","nodeType":"end","nodeName":"结束"}],"edges":[{"source":"start","target":"n_manager"},{"source":"n_manager","target":"gw_amount"},{"source":"gw_amount","target":"end"},{"source":"n_director","target":"end"},{"source":"n_gm","target":"end"}]}',
'0', 2, 'system', NOW(), 'system', NOW(), '0')
ON DUPLICATE KEY UPDATE
  process_name = VALUES(process_name),
  process_type = VALUES(process_type),
  node_config = VALUES(node_config),
  status = '0',
  version = 2,
  update_by = 'system',
  update_time = NOW(),
  del_flag = '0';

COMMIT;

SELECT id, process_type, process_key, status, version
FROM wf_process_definition
WHERE process_type IN ('leave', 'trip', 'outing', 'overtime', 'purchase', 'expense', 'loan')
  AND del_flag = '0'
ORDER BY process_type, status, version DESC, id DESC;

SELECT role_key, status
FROM sys_role
WHERE role_key IN ('DEPT_MANAGER', 'DIRECTOR', 'GM')
ORDER BY role_key;
