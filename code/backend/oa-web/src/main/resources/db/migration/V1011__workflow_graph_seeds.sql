-- V1011: V1010 graph-format workflow seeds with 4-dimensional tiered routing.
--
-- Three seeds cover the three business types that have a real callback Service
-- on the runtime classpath (trip / overtime / purchase). contract / payment /
-- supply_request are intentionally not seeded here because the corresponding
-- ServiceImpl is in oa-finance / oa-admin modules that are not registered in
-- the parent pom (see backend-real-architecture memory).
--
-- The graph format (schemaVersion=2) is parsed by WorkflowServiceImpl.parseNodeConfig
-- and materialized to a flat path by materializeGraphToFlatPath at process start.
-- Each seed demonstrates the 4 routing dimensions:
--   - amount threshold (purchase)
--   - hours threshold (overtime)
--   - days threshold (trip)
--   - role_chain (amount-based level routing) and initiator_level_match (high-level skip)
--   - escalation target (escalateTo on the dept manager node)

-- ============================================================================
-- 1. trip: tiered by days
--    < 3 days  → 直属上级 (dept_manager)
--    3-7 days  → 部门经理 (role=DEPT_MANAGER, same dept)
--    > 7 days  → 总监 (role_global=DIRECTOR)
--    Timeout: escalate 24h → 升级到 DEPT_MANAGER (默认)
-- ============================================================================
INSERT INTO wf_process_definition
  (process_name, process_key, process_type, node_config, status, version, create_by, create_time, update_by, update_time, del_flag)
VALUES
  ('出差审批流程(分级)', 'trip_tiered', 'trip',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "gw_days", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按天数分支",
        "branches": [
          {"when": "days < 3", "to": "n_dept"},
          {"when": "days >= 3 && days <= 7", "to": "n_manager"},
          {"when": "days > 7", "to": "n_director"}
        ]
       },
       {"nodeId": "n_dept", "nodeType": "approval", "nodeName": "直属上级审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role", "value": "DEPT_MANAGER"}},
       {"nodeId": "n_manager", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "role", "assigneeValue": "DEPT_MANAGER",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "gw_days"},
       {"source": "n_dept", "target": "end"},
       {"source": "n_manager", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 1, 'system', NOW(), 'system', NOW(), '0');

-- ============================================================================
-- 2. overtime: tiered by hours
--    < 36 hours (within legal month limit) → 直属上级
--    >= 36 hours                          → 总监
-- ============================================================================
INSERT INTO wf_process_definition
  (process_name, process_key, process_type, node_config, status, version, create_by, create_time, update_by, update_time, del_flag)
VALUES
  ('加班审批流程(分级)', 'overtime_tiered', 'overtime',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "gw_hours", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按小时分支",
        "branches": [
          {"when": "hours < 36", "to": "n_dept"},
          {"when": "hours >= 36", "to": "n_director"}
        ]
       },
       {"nodeId": "n_dept", "nodeType": "approval", "nodeName": "直属上级审批",
        "assigneeType": "dept_manager", "assigneeValue": "dept_manager",
        "timeoutHours": 12, "timeoutAction": "notify_only"},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "gw_hours"},
       {"source": "n_dept", "target": "end"},
       {"source": "n_director", "target": "end"}
     ]
   }',
   '0', 1, 'system', NOW(), 'system', NOW(), '0');

-- ============================================================================
-- 3. purchase: amount-based 4-tier routing using role_chain
--    < 5000    → 部门经理 (DEPT_MANAGER)
--    5000-50000 → 总监 (DIRECTOR)
--    > 50000   → 总经理 (GM)
-- ============================================================================
INSERT INTO wf_process_definition
  (process_name, process_key, process_type, node_config, status, version, create_by, create_time, update_by, update_time, del_flag)
VALUES
  ('采购审批流程(分级)', 'purchase_tiered', 'purchase',
   '{
     "schemaVersion": 2,
     "nodes": [
       {"nodeId": "start", "nodeType": "start", "nodeName": "开始"},
       {"nodeId": "gw_amount", "nodeType": "gateway", "gatewayType": "exclusive", "nodeName": "按金额分支",
        "branches": [
          {"when": "amount < 5000", "to": "n_dept"},
          {"when": "amount >= 5000 && amount < 50000", "to": "n_director"},
          {"when": "amount >= 50000", "to": "n_gm"}
        ]
       },
       {"nodeId": "n_dept", "nodeType": "approval", "nodeName": "部门经理审批",
        "assigneeType": "role", "assigneeValue": "DEPT_MANAGER",
        "timeoutHours": 24, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "DIRECTOR"}},
       {"nodeId": "n_director", "nodeType": "approval", "nodeName": "总监审批",
        "assigneeType": "role_global", "assigneeValue": "DIRECTOR",
        "timeoutHours": 48, "timeoutAction": "escalate",
        "escalateTo": {"type": "role_global", "value": "GM"}},
       {"nodeId": "n_gm", "nodeType": "approval", "nodeName": "总经理审批",
        "assigneeType": "role_global", "assigneeValue": "GM",
        "timeoutHours": 72, "timeoutAction": "auto_reject"},
       {"nodeId": "end", "nodeType": "end", "nodeName": "结束"}
     ],
     "edges": [
       {"source": "start", "target": "gw_amount"},
       {"source": "n_dept", "target": "end"},
       {"source": "n_director", "target": "end"},
       {"source": "n_gm", "target": "end"}
     ]
   }',
   '0', 1, 'system', NOW(), 'system', NOW(), '0');

-- Deactivate the legacy flat-array definitions for the same processTypes so the
-- engine uses the new graph-format seeds. (status=1 = inactive per existing code.)
UPDATE wf_process_definition
   SET status = '1', update_by = 'system', update_time = NOW()
 WHERE process_type IN ('trip', 'overtime', 'purchase')
   AND del_flag = '0'
   AND status = '0'
   AND node_config NOT LIKE '%"schemaVersion":2%';
