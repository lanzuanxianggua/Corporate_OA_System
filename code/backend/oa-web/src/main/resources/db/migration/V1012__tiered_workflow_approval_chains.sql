-- V1012: default tiered approval chains.
--
-- Design:
--   1. Every configured business starts at the base manager approval.
--   2. Higher-risk requests continue upward instead of jumping directly to the
--      highest approver.
--   3. Amount-based businesses use 5,000 / 50,000 thresholds.
--   4. Duration-based businesses use 7 days; overtime uses 36 hours.

UPDATE wf_process_definition
   SET status = '1', update_by = 'system', update_time = NOW()
 WHERE process_type IN ('leave', 'trip', 'outing', 'purchase', 'expense', 'overtime', 'loan')
   AND del_flag = '0'
   AND status = '0';

INSERT INTO wf_process_definition
  (process_name, process_key, process_type, node_config, status, version, create_by, create_time, update_by, update_time, del_flag)
VALUES
  ('请假审批流程(逐级分级)', 'leave_tiered_chain', 'leave',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_days", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按请假天数分级",
        "branches": [
          {"when": "days > 7", "to": "n_director"},
          {"when": "days <= 7", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_days"},
       {"source": "gw_days", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('出差审批流程(逐级分级)', 'trip_tiered_chain', 'trip',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_days", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按出差天数分级",
        "branches": [
          {"when": "days > 7", "to": "n_director"},
          {"when": "days <= 7", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_days"},
       {"source": "gw_days", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('外出审批流程(逐级分级)', 'outing_tiered_chain', 'outing',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_days", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按外出天数分级",
        "branches": [
          {"when": "days > 7", "to": "n_director"},
          {"when": "days <= 7", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_days"},
       {"source": "gw_days", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('加班审批流程(逐级分级)', 'overtime_tiered_chain', 'overtime',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 12, "timeoutAction": "notify_only"},
       {"nodeId": "gw_hours", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按加班小时分级",
        "branches": [
          {"when": "hours > 36", "to": "n_director"},
          {"when": "hours <= 36", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_hours"},
       {"source": "gw_hours", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('采购审批流程(逐级分级)', 'purchase_tiered_chain', 'purchase',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "role", "assigneeValue": "DEPT_MANAGER",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_amount", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按采购金额分级",
        "branches": [
          {"when": "amount > 5000", "to": "n_director"},
          {"when": "amount <= 5000", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "routingRules": [{"when": "amount > 50000", "skipTo": "n_gm"}],
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "n_gm", "nodeType": "approval", "nodeName": "总经理审批",
        "assigneeType": "role_global", "assigneeValue": "GM",
        "timeoutHours": 72, "timeoutAction": "auto_reject"},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_amount"},
       {"source": "gw_amount", "target": "end"},
       {"source": "n_director", "target": "end"},
       {"source": "n_gm", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('报销审批流程(逐级分级)', 'expense_tiered_chain', 'expense',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "role", "assigneeValue": "DEPT_MANAGER",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_amount", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按报销金额分级",
        "branches": [
          {"when": "amount > 5000", "to": "n_director"},
          {"when": "amount <= 5000", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "routingRules": [{"when": "amount > 50000", "skipTo": "n_gm"}],
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "n_gm", "nodeType": "approval", "nodeName": "总经理审批",
        "assigneeType": "role_global", "assigneeValue": "GM",
        "timeoutHours": 72, "timeoutAction": "auto_reject"},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_amount"},
       {"source": "gw_amount", "target": "end"},
       {"source": "n_director", "target": "end"},
       {"source": "n_gm", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0'),

  ('借款审批流程(逐级分级)', 'loan_tiered_chain', 'loan',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "role", "assigneeValue": "DEPT_MANAGER",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "gw_amount", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按借款金额分级",
        "branches": [
          {"when": "amount > 5000", "to": "n_director"},
          {"when": "amount <= 5000", "to": "end"}
        ]},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "routingRules": [{"when": "amount > 50000", "skipTo": "n_gm"}],
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "n_gm", "nodeType": "approval", "nodeName": "总经理审批",
        "assigneeType": "role_global", "assigneeValue": "GM",
        "timeoutHours": 72, "timeoutAction": "auto_reject"},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "n_manager"},
       {"source": "n_manager", "target": "gw_amount"},
       {"source": "gw_amount", "target": "end"},
       {"source": "n_director", "target": "end"},
       {"source": "n_gm", "target": "end"}
     ]
   }',
   '0', 2, 'system', NOW(), 'system', NOW(), '0');
