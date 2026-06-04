# 工作流微内核 API 契约

> 日期: 2026-06-02
> 模块: `oa-workflow`
> 关联设计: `docs/superpowers/specs/2026-06-02-wf-engine-kernel-task-split.md`
> 关联DDL:  `code/backend/sql/baseline/001_schema_workflow.sql`
> Knife4j 分组: **工作流引擎**

---

## 1. 总则

* **基路径**: `/api/workflow`
* **认证**: 全局 JWT（Header `Authorization: Bearer <token>`）
* **数据格式**: 全部 `application/json;charset=UTF-8`
* **统一响应**: `{"code":0,"message":"ok","data":...}` —— `code=0` 成功，`-1` 业务错误，`401` 未认证
* **租户/角色**: ADMIN（管理视图）/ USER（员工视图）
* **权限体系**: 路径前缀 `wf:definition:*` / `wf:instance:*` / `wf:task:*` / `wf:delegation:*`

---

## 2. 权限码清单

| 权限码 | 描述 | 适用角色 |
| --- | --- | --- |
| `wf:definition:create` | 新建流程定义 | ADMIN |
| `wf:definition:update` | 修改流程定义（仅草稿） | ADMIN |
| `wf:definition:delete` | 删除流程定义（仅草稿） | ADMIN |
| `wf:definition:publish` | 发布流程定义（锁版） | ADMIN |
| `wf:definition:list`   | 查询流程定义列表 | ADMIN/USER |
| `wf:definition:get`    | 查询流程定义详情 | ADMIN/USER |
| `wf:definition:graph`  | 流程图（节点+边） | ADMIN/USER |
| `wf:instance:start`    | 启动流程实例 | USER |
| `wf:instance:list`     | 我的申请列表 | USER |
| `wf:instance:detail`   | 流程实例详情 | ADMIN/USER |
| `wf:instance:withdraw`| 撤回流程 | USER |
| `wf:instance:suspend`  | 挂起流程 | ADMIN |
| `wf:instance:resume`   | 恢复流程 | ADMIN |
| `wf:task:pending`      | 待办列表 | USER |
| `wf:task:done`         | 已办列表 | USER |
| `wf:task:detail`       | 任务详情 | ADMIN/USER |
| `wf:task:approve`      | 同意任务 | USER |
| `wf:task:reject`       | 驳回任务 | USER |
| `wf:task:transfer`     | 转办任务 | USER |
| `wf:task:addsign`      | 加签任务 | USER |
| `wf:task:urge`         | 催办任务 | USER |
| `wf:delegation:create` | 新建委托 | USER |
| `wf:delegation:update` | 修改委托 | USER |
| `wf:delegation:cancel` | 取消委托 | USER |
| `wf:delegation:list`   | 我的委托 | USER |
| `wf:delegation:active` | 查生效委托（被代理人查） | USER |
| `wf:record:list`       | 流转记录 | ADMIN/USER |
| `wf:graph:preview`     | 流程图预览 | ADMIN/USER |

---

## 3. 端点全表 (7 类 28 端点)

### 3.1 流程定义（Definition） — 8 端点

#### 3.1.1 `POST /api/workflow/definitions`
* **权限**: `wf:definition:create`
* **请求 DTO**: `WfDefinitionCreateDTO`
  ```json
  {
    "code": "WF_LEAVE",
    "name": "请假流程",
    "category": "hr",
    "formDefId": 100,
    "description": "员工请假标准流程",
    "nodes": [
      {"nodeCode":"START","nodeName":"开始","nodeType":"START","sortOrder":0},
      {"nodeCode":"MANAGER","nodeName":"直属上级审批","nodeType":"APPROVAL",
       "approvalMode":"SEQUENTIAL","timeoutHours":48,
       "emptyAssigneeStrategy":"ASSIGN_ADMIN","sortOrder":10,
       "assigneeRules":[
         {"ruleType":"DEPT_LEADER","ruleValue":"auto","sortOrder":10}
       ]},
      {"nodeCode":"END","nodeName":"结束","nodeType":"END","sortOrder":999}
    ],
    "transitions":[
      {"fromNodeCode":"START","toNodeCode":"MANAGER","sortOrder":10},
      {"fromNodeCode":"MANAGER","toNodeCode":"END","sortOrder":10}
    ]
  }
  ```
* **响应 VO**: `WfDefinitionVO`（同 §3.1.6）
* **错误码**:
  | 业务码 | 描述 |
  | --- | --- |
  | 40001 | code 已存在 |
  | 40002 | 节点定义有环 |
  | 40003 | 起始/结束节点缺失 |
* **示例**:
  ```bash
  curl -X POST http://localhost:8080/api/workflow/definitions \
    -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
    -d @definition-create.json
  ```

#### 3.1.2 `PUT /api/workflow/definitions/{id}`
* **权限**: `wf:definition:update`（仅 `DRAFT` 状态可改）
* **路径参数**: `id` 定义ID
* **请求 DTO**: `WfDefinitionUpdateDTO`（结构同上，`code` 不可改）

#### 3.1.3 `DELETE /api/workflow/definitions/{id}`
* **权限**: `wf:definition:delete`
* **限制**: 仅 `DRAFT` 可删；`PUBLISHED` 必须先 `DISABLED`
* **响应**: `{"code":0,"data":true}`

#### 3.1.4 `POST /api/workflow/definitions/{id}/actions/publish`
* **权限**: `wf:definition:publish`
* **行为**: 当前草稿状态变更为 `PUBLISHED`；如已存在同 code 的已发布版本，自动将老版本置为 `DISABLED`
* **响应 VO**: `WfDefinitionVO`（含 `version` 字段）

#### 3.1.5 `GET /api/workflow/definitions`
* **权限**: `wf:definition:list`
* **查询参数**: `code?, category?, status?, page=1, size=20`
* **响应 VO**: `PageResult<WfDefinitionVO>`

#### 3.1.6 `GET /api/workflow/definitions/{id}`
* **权限**: `wf:definition:get`
* **响应 VO `WfDefinitionVO`**:
  ```json
  {
    "id": 1, "code": "WF_LEAVE", "name": "请假流程",
    "version": 1, "category": "hr", "formDefId": 100,
    "status": "PUBLISHED", "description": "...",
    "createBy": "admin", "createTime": "2026-06-02T10:00:00",
    "updateBy": "admin", "updateTime": "2026-06-02T10:00:00"
  }
  ```

#### 3.1.7 `GET /api/workflow/definitions/{id}/graph`
* **权限**: `wf:definition:graph`
* **响应 VO `WfDefinitionGraphVO`**:
  ```json
  {
    "defId": 1, "version": 1,
    "nodes": [
      {"id":1,"nodeCode":"START","nodeName":"开始","nodeType":"START","sortOrder":0},
      {"id":2,"nodeCode":"MANAGER","nodeName":"直属上级","nodeType":"APPROVAL","sortOrder":10}
    ],
    "transitions": [
      {"id":1,"fromNodeId":1,"toNodeId":2,"sortOrder":10}
    ]
  }
  ```

#### 3.1.8 `POST /api/workflow/definitions/{id}/actions/clone`
* **权限**: `wf:definition:create`
* **行为**: 克隆已发布版本生成新草稿（version 递增）
* **请求 DTO**: `{"newCode":"WF_LEAVE_V2"}`
* **响应 VO**: `WfDefinitionVO`

### 3.2 流程实例（Instance） — 7 端点

#### 3.2.1 `POST /api/workflow/instances/start`
* **权限**: `wf:instance:start`（USER 即可）
* **请求 DTO `WfProcessStartDTO`**:
  ```java
  @NotBlank private String processCode;       // 对应 wf_definition.code
  @NotBlank private String businessType;      // leave/trip/expense
  @NotNull  private Long   businessId;        // 业务主键
  @NotBlank private String title;             // 流程标题
  private Map<String,Object> variables = new HashMap<>();
  ```
* **响应 VO**: `WfInstanceVO`
* **错误码**:
  | 业务码 | 描述 |
  | --- | --- |
  | 40401 | 流程定义未发布 |
  | 40402 | 业务单据已绑定流程 |

#### 3.2.2 `GET /api/workflow/instances/mine`
* **权限**: `wf:instance:list`
* **查询参数**: `status?, page=1, size=20`
* **响应**: `PageResult<WfInstanceVO>`

#### 3.2.3 `GET /api/workflow/instances/{id}`
* **权限**: `wf:instance:detail`
* **响应 VO `WfInstanceVO`**:
  ```json
  {
    "id": 1001, "defId": 1, "defVersion": 1,
    "businessType": "leave", "businessId": 5001,
    "title": "张三-2026-06-请假申请",
    "applicantId": 1001, "applicantName": "张三",
    "status": "RUNNING", "currentNodeIds": "[2]",
    "returnSourceNodeId": null, "returnStrategy": "DIRECT_RETURN",
    "startTime": "2026-06-02T10:00:00", "endTime": null,
    "createTime": "2026-06-02T10:00:00"
  }
  ```

#### 3.2.4 `POST /api/workflow/instances/{id}/actions/withdraw`
* **权限**: `wf:instance:withdraw`（校验当前用户为申请人）
* **请求 DTO**: `WfInstanceWithdrawDTO { reason: String(<=500) }`
* **行为**: 实例状态置为 `REVOKED`；`PENDING` 任务置为 `CANCELED`

#### 3.2.5 `POST /api/workflow/instances/{id}/actions/suspend`
* **权限**: `wf:instance:suspend`
* **请求 DTO**: `{ "reason": "..." }`
* **限制**: 仅 `RUNNING` 可挂起

#### 3.2.6 `POST /api/workflow/instances/{id}/actions/resume`
* **权限**: `wf:instance:resume`
* **限制**: 仅 `SUSPENDED` 可恢复

#### 3.2.7 `GET /api/workflow/instances/{id}/records`
* **权限**: `wf:record:list`
* **响应 VO**: `List<WfRecordVO>`
  ```json
  {
    "id": 9001, "instanceId": 1001, "taskId": 8001,
    "operatorId": 2001, "operatorName": "李四",
    "action": "APPROVE",
    "fromNodeId": 2, "toNodeId": 3,
    "opinion": "同意",
    "createdAt": "2026-06-02T10:30:00"
  }
  ```

### 3.3 审批任务（Task） — 8 端点

#### 3.3.1 `GET /api/workflow/tasks/pending`
* **权限**: `wf:task:pending`
* **查询参数**: `page=1, size=20`
* **响应**: `PageResult<WfTaskVO>`
  ```json
  {
    "taskId": 8001, "instanceId": 1001,
    "processName": "请假流程", "title": "张三-2026-06-请假申请",
    "applicantName": "张三", "nodeName": "部门负责人审批",
    "status": "PENDING",
    "createTime": "2026-06-02T10:00:00", "dueTime": "2026-06-04T10:00:00"
  }
  ```

#### 3.3.2 `GET /api/workflow/tasks/done`
* **权限**: `wf:task:done`
* **查询参数**: `page=1, size=20`

#### 3.3.3 `GET /api/workflow/tasks/{id}`
* **权限**: `wf:task:detail`

#### 3.3.4 `POST /api/workflow/tasks/{id}/actions/approve`
* **权限**: `wf:task:approve`（校验任务审批人归属）
* **请求 DTO `WfTaskApproveDTO`**:
  ```java
  @NotBlank @Size(max=500) private String opinion;
  private String signature;                          // Base64/URL
  private Map<String,Object> variables = new HashMap<>();
  ```
* **行为**: 推进状态机；如当前节点 `evaluateSign` 满足 → 流转；满足最终节点 → 实例变 `PASSED`

#### 3.3.5 `POST /api/workflow/tasks/{id}/actions/reject`
* **权限**: `wf:task:reject`
* **请求 DTO `WfTaskRejectDTO`**:
  ```java
  @NotBlank @Size(max=500) private String opinion;
  @NotNull private Long returnSourceNodeId;          // 驳回到哪个节点
  private String returnStrategy = "DIRECT_RETURN";   // DIRECT/SEQUENTIAL
  ```
* **行为**: 实例变 `REJECTED`，任务变 `REJECTED`

#### 3.3.6 `POST /api/workflow/tasks/{id}/actions/transfer`
* **权限**: `wf:task:transfer`
* **请求 DTO `WfTaskTransferDTO`**:
  ```java
  @NotNull private Long targetAssigneeId;
  @NotBlank @Size(max=500) private String reason;
  ```
* **行为**: 当前任务变 `TRANSFERRED`，生成新 `PENDING` 任务给目标人

#### 3.3.7 `POST /api/workflow/tasks/{id}/actions/add-sign`
* **权限**: `wf:task:addsign`
* **请求 DTO `WfTaskAddSignDTO`**:
  ```java
  @NotNull  private Long addSignAssigneeId;
  @NotBlank @Pattern(regexp="^(PRE_ADD_SIGN|POST_ADD_SIGN)$") private String signType;
  @NotBlank private String reason;
  ```
* **行为**:
  * `PRE_ADD_SIGN`: 原任务 `SUSPENDED`；先审批新加签人
  * `POST_ADD_SIGN`: 当前审批人通过后再触发新加签任务

#### 3.3.8 `POST /api/workflow/tasks/{id}/actions/urge`
* **权限**: `wf:task:urge`（仅申请人）
* **限流**: 同任务 2 小时内最多 1 次
* **请求 DTO**: `{ "message": "请尽快审批" }`
* **行为**: 发送 WebSocket/IM；`remind_count += 1`；写 `URGE` 记录

### 3.4 委托（Delegation） — 5 端点

#### 3.4.1 `POST /api/workflow/delegations`
* **权限**: `wf:delegation:create`
* **请求 DTO `WfDelegationCreateDTO`**:
  ```java
  @NotNull private Long delegateId;
  @NotBlank private String processCategory;     // * 表示全局
  @NotNull private LocalDate startDate;
  @NotNull private LocalDate endDate;
  private Boolean notifyDelegator = true;
  ```

#### 3.4.2 `PUT /api/workflow/delegations/{id}`
* **权限**: `wf:delegation:update`
* **限制**: 仅 `ACTIVE` 可改

#### 3.4.3 `DELETE /api/workflow/delegations/{id}`
* **权限**: `wf:delegation:cancel`
* **行为**: 软删 — `status` 置为 `CANCELLED`

#### 3.4.4 `GET /api/workflow/delegations/mine`
* **权限**: `wf:delegation:list`
* **响应**: `List<WfDelegationVO>`

#### 3.4.5 `GET /api/workflow/delegations/active`
* **权限**: `wf:delegation:active`
* **查询参数**: `empId, processCategory?, today=now()`
* **响应**: `List<WfDelegationVO>`

### 3.5 流转记录（Record） — 1 端点

#### 3.5.1 `GET /api/workflow/records/instance/{instanceId}`
* **权限**: `wf:record:list`
* **响应**: `List<WfRecordVO>`

### 3.6 流程图（Graph Preview） — 1 端点

#### 3.6.1 `POST /api/workflow/graph/preview`
* **权限**: `wf:graph:preview`
* **请求 DTO**: 临时节点+边 JSON（不落库）
* **响应**: `WfDefinitionGraphVO`（含合法性校验结果）

### 3.7 字典与辅助（Misc） — 1 端点

#### 3.7.1 `GET /api/workflow/dict/categories`
* **权限**: 公开
* **响应**: `List<DictItemVO>`（取自 `sys_dict_data WHERE dict_code='workflow_category'`）

---

## 4. DTO/VO 详细字段

### 4.1 `WfDefinitionCreateDTO` / `WfDefinitionUpdateDTO`
| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| code | String | 是 | 长度 ≤ 64 |
| name | String | 是 | 长度 ≤ 128 |
| category | String | 是 | hr/finance/admin/common/biz |
| formDefId | Long | 否 | 关联表单ID |
| description | String | 否 | 长度 ≤ 500 |
| nodes | List<NodeDTO> | 是 | 至少 2 个（含 START/END） |
| transitions | List<TransitionDTO> | 否 | 边集合 |

### 4.2 `WfNodeDTO`
| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| nodeCode | String | 是 | 长度 ≤ 64 |
| nodeName | String | 是 | 长度 ≤ 128 |
| nodeType | String | 是 | START/END/APPROVAL/SUBPROCESS/CONDITION/GATEWAY |
| approvalMode | String | 否 | SEQUENTIAL/COUNTERSIGN/ORSIGN/PROPORTIONAL/VOTE |
| passRatio | BigDecimal | 否 | 0.00-100.00 |
| timeoutHours | Integer | 否 | > 0 |
| timeoutAction | String | 否 | AUTO_PASS/AUTO_REJECT/NOTIFY |
| fieldPermission | JSONObject | 否 | JSON 矩阵 |
| emptyAssigneeStrategy | String | 否 | 默认 AUTO_PASS |
| sortOrder | Integer | 是 | ≥ 0 |
| assigneeRules | List<WfAssigneeRuleDTO> | 否 | 至少 1 条（APPROVAL） |

### 4.3 `WfAssigneeRuleDTO`
| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| ruleType | String | 是 | FIXED_USER/POST/DEPT_LEADER/REPORT_LINE/FORM_SELECT/API |
| ruleValue | String | 是 | 长度 ≤ 500 |
| sortOrder | Integer | 是 | ≥ 0 |

### 4.4 `WfTransitionDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| fromNodeCode | String | 是 |
| toNodeCode | String | 是 |
| expression | String | 否 |
| sortOrder | Integer | 否（默认 10） |

### 4.5 `WfProcessStartDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| processCode | String | 是 |
| businessType | String | 是 |
| businessId | Long | 是 |
| title | String | 是 |
| variables | Map | 否 |

### 4.6 `WfTaskApproveDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| opinion | String | 是（≤500） |
| signature | String | 否 |
| variables | Map | 否 |

### 4.7 `WfTaskRejectDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| opinion | String | 是 |
| returnSourceNodeId | Long | 是 |
| returnStrategy | String | 否（默认 DIRECT_RETURN） |

### 4.8 `WfTaskTransferDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| targetAssigneeId | Long | 是 |
| reason | String | 是 |

### 4.9 `WfTaskAddSignDTO`
| 字段 | 类型 | 必填 |
| --- | --- | --- |
| addSignAssigneeId | Long | 是 |
| signType | String | 是（PRE/POST） |
| reason | String | 是 |

---

## 5. 错误码（顶层）

| 业务码 | 描述 |
| --- | --- |
| 0 | 成功 |
| -1 | 通用业务异常 |
| 401 | 未认证 / Token 过期 |
| 403 | 权限不足 |
| 40001-40099 | 流程定义相关 |
| 40101-40199 | 流程实例相关 |
| 40201-40299 | 任务操作相关 |
| 40301-40399 | 委托相关 |
| 40401-40499 | 资源不存在 |
| 50001-50099 | 引擎内部异常 |

---

## 6. 响应 VO 字段汇总

| VO | 字段 |
| --- | --- |
| WfDefinitionVO | id, code, name, version, category, formDefId, status, description, createBy, createTime, updateBy, updateTime |
| WfDefinitionGraphVO | defId, version, nodes, transitions |
| WfInstanceVO | id, defId, defVersion, businessType, businessId, title, applicantId, applicantName, status, currentNodeIds, returnSourceNodeId, returnStrategy, startTime, endTime, createTime |
| WfTaskVO | taskId, instanceId, processName, title, applicantName, nodeName, status, createTime, dueTime |
| WfRecordVO | id, instanceId, taskId, operatorId, operatorName, action, fromNodeId, toNodeId, opinion, createdAt |
| WfDelegationVO | id, delegatorId, delegateId, processCategory, startDate, endDate, notifyDelegator, status, createTime |

---

## 7. 端点总览（28 端点）

| # | 方法 | 路径 | 权限 |
| -- | --- | --- | --- |
| 1 | POST | /api/workflow/definitions | wf:definition:create |
| 2 | PUT | /api/workflow/definitions/{id} | wf:definition:update |
| 3 | DELETE | /api/workflow/definitions/{id} | wf:definition:delete |
| 4 | POST | /api/workflow/definitions/{id}/actions/publish | wf:definition:publish |
| 5 | GET | /api/workflow/definitions | wf:definition:list |
| 6 | GET | /api/workflow/definitions/{id} | wf:definition:get |
| 7 | GET | /api/workflow/definitions/{id}/graph | wf:definition:graph |
| 8 | POST | /api/workflow/definitions/{id}/actions/clone | wf:definition:create |
| 9 | POST | /api/workflow/instances/start | wf:instance:start |
| 10 | GET | /api/workflow/instances/mine | wf:instance:list |
| 11 | GET | /api/workflow/instances/{id} | wf:instance:detail |
| 12 | POST | /api/workflow/instances/{id}/actions/withdraw | wf:instance:withdraw |
| 13 | POST | /api/workflow/instances/{id}/actions/suspend | wf:instance:suspend |
| 14 | POST | /api/workflow/instances/{id}/actions/resume | wf:instance:resume |
| 15 | GET | /api/workflow/instances/{id}/records | wf:record:list |
| 16 | GET | /api/workflow/tasks/pending | wf:task:pending |
| 17 | GET | /api/workflow/tasks/done | wf:task:done |
| 18 | GET | /api/workflow/tasks/{id} | wf:task:detail |
| 19 | POST | /api/workflow/tasks/{id}/actions/approve | wf:task:approve |
| 20 | POST | /api/workflow/tasks/{id}/actions/reject | wf:task:reject |
| 21 | POST | /api/workflow/tasks/{id}/actions/transfer | wf:task:transfer |
| 22 | POST | /api/workflow/tasks/{id}/actions/add-sign | wf:task:addsign |
| 23 | POST | /api/workflow/tasks/{id}/actions/urge | wf:task:urge |
| 24 | POST | /api/workflow/delegations | wf:delegation:create |
| 25 | PUT | /api/workflow/delegations/{id} | wf:delegation:update |
| 26 | DELETE | /api/workflow/delegations/{id} | wf:delegation:cancel |
| 27 | GET | /api/workflow/delegations/mine | wf:delegation:list |
| 28 | GET | /api/workflow/delegations/active | wf:delegation:active |

附加工具类端点（字典/图预览/记录）共 3 个，由 §3.5-3.7 给出。
