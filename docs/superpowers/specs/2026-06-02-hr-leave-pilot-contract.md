# HR 请假审批闭环 — API 契约与索引验收

> 日期: 2026-06-04
> 状态: 草案 v2（已对照实际代码校准）
> 关联:
> - 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`
> - 试点拆分: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`
> - SQL 草案: `code/backend/sql/hr_leave_contract.sql`
> - 审计报告: `.hermes/plans/2026-06-04_184336-spec-audit-report.md`

---

## 1. 范围

- **包含**: HR 请假申请、余额管理、规则管理共 11 个接口
- **不包含**: 审批动作接口（走统一工作流任务 API `/api/wf/tasks/{taskId}/actions/*`，见 §6）

---

## 2. 统一约定

| 维度 | 约定 |
|------|------|
| 路径前缀 | `/api/hr/{resource-plural}` |
| `{resource}` 复数规范 | `leaves`（不是 `leave`）、`leave-balances`（带连字符）、`leave-rules` |
| HTTP 方法 | `POST` 创建/动作，`GET` 查询，`PUT` 更新，`DELETE` 删除 |
| 业务动作 | `POST /{id}/actions/{action}`（如 `/{id}/actions/revoke`） |
| 响应格式 | `{"code":0,"message":"操作成功","data":...}` |
| 分页参数 | `pageNum`(默认1)、`pageSize`(默认10) |
| 权限码 | `hr:leave:*`、`hr:leave-balance:*`、`hr:leave-rule:*` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss` (LocalDateTime) |
| 日期格式 | `yyyy-MM-dd` (LocalDate) |
| 错误码 | 业务 `-1`，未认证 `401`，无权限 `403`，系统 `500` |

---

## 3. 接口契约

### 3.1 `POST /api/hr/leaves` — 创建并提交请假申请

- **权限码**: `hr:leave:create`
- **状态**: ✓ **已实现**（`HrLeaveController.createAndSubmit`）
- **请求体**: `HrLeaveCreateDTO`

| 字段 | 类型 | 必填 | 校验 |
|------|------|------|------|
| leaveType | String | 是 | 枚举值(PERSONAL/ANNUAL/SICK/MARRIAGE/FUNERAL/MATERNITY/PATERNITY/COMPENSATORY/OTHER) |
| startTime | LocalDateTime | 是 | `yyyy-MM-dd HH:mm:ss` |
| endTime | LocalDateTime | 是 | 必须 > startTime |
| leavePeriod | String | 是 | 枚举值(FULL/AM/PM) |
| reason | String | 是 | 最长 500 |
| attachments | String (JSON) | 否 | 文件ID数组 |

- **响应**: `R<Long>` (新申请 ID)
- **业务行为**:
  1. 校验假期类型、时段、时间合法性
  2. 计算请假天数（工作日计算，半天乘以 0.5）
  3. 如需扣余额，冻结 `hr_leave_balance.frozen_days`
  4. 启动工作流实例（`WorkflowEngine.startProcess`）
  5. 写 `wf_task` 任务
  6. 写 `oa_todo` 待办（如果已有）
  7. 发站内消息（如果已有）

### 3.2 `GET /api/hr/leaves` — 分页查询请假申请

- **权限码**: `hr:leave:list`
- **状态**: ✓ **已实现**（`HrLeaveController.pageQuery`）
- **查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 默认 1 |
| pageSize | Integer | 否 | 默认 10 |
| empId | Long | 否 | 员工ID |
| deptId | Long | 否 | 部门ID |
| status | String | 否 | 状态枚举 |
| leaveType | String | 否 | 假期类型 |
| startDate | String | 否 | 起始日期 |
| endDate | String | 否 | 结束日期 |

- **响应**: `R<PageResult<HrLeaveVO>>`
- **数据权限规则**:
  - 普通用户（USER）: 强制 empId=currentEmpId，忽略 query.empId
  - 管理员（ADMIN）: 按 query.empId/query.deptId 过滤

### 3.3 `GET /api/hr/leaves/{id}` — 查询请假详情

- **权限码**: `hr:leave:detail`
- **状态**: ✓ **已实现**（`HrLeaveController.getDetail`）
- **路径参数**: `id` (Long)
- **响应**: `R<HrLeaveVO>`

### 3.4 `POST /api/hr/leaves/{id}/actions/revoke` — 撤回请假申请

- **权限码**: `hr:leave:revoke`
- **状态**: ✓ **已实现**（`HrLeaveController.revoke`）
- **路径参数**: `id` (Long)
- **业务规则**:
  - 申请人本人或管理员可撤回
  - 状态必须为 `RUNNING`（看 `HrLeaveStatus.canRevoke()`）
  - 撤回后: status=REVOKED, 释放 frozen_days, 通知工作流

### 3.5 `POST /api/hr/leaves/{id}/actions/resubmit` — 驳回后重新提交

- **权限码**: `hr:leave:resubmit`
- **状态**: ✓ **已实现**（`HrLeaveController.resubmit`）
- **请求体**: `HrLeaveCreateDTO`（同创建）
- **业务规则**:
  - 状态必须为 `REJECTED`（看 `HrLeaveStatus.canResubmit()`）
  - 重新启动工作流，更新 process_instance_id
  - 释放旧的 frozen_days，重新冻结新申请

### 3.6 `GET /api/hr/leaves/my-balances` — 查询当前用户假期余额

- **权限码**: `hr:leave-balance:view`
- **状态**: ✓ **已实现**（`HrLeaveController.getMyBalances`）
- **查询参数**: `year` (Integer, 可选)
- **响应**: `R<List<HrLeaveBalanceVO>>`
- **VO 字段**:
  - empId, empName, deptName
  - leaveType, leaveTypeName
  - year, totalDays, usedDays, frozenDays
  - remainingDays（账面剩余，含冻结）
  - **availableDays（实际可申请 = remainingDays - frozenDays）** ← 计算字段
  - expireDate, status, updateTime

### 3.7 `GET /api/hr/leave-balances` — 管理端分页查询假期余额

- **权限码**: `hr:leave-balance:list`
- **状态**: ✓ **已实现**（`HrLeaveBalanceController.pageQuery`）
- **查询参数**: empId, year, pageNum, pageSize
- **响应**: `R<PageResult<HrLeaveBalanceVO>>`

### 3.8 `POST /api/hr/leave-balances/actions/init` — 初始化员工年度假期余额

- **权限码**: `hr:leave-balance:init`
- **状态**: ✓ **已实现**（`HrLeaveBalanceController.initBalance`）
- **请求体**: `HrLeaveBalanceInitDTO`

| 字段 | 类型 | 必填 | 校验 |
|------|------|------|------|
| empId | Long | 是 | 员工ID |
| leaveType | String | 是 | 假期类型 |
| year | Integer | 是 | 年度 |
| totalDays | BigDecimal | 是 | > 0 |
| expireDate | String | 否 | `yyyy-MM-dd` |

### 3.9 `PUT /api/hr/leave-balances/{id}` — 调整假期余额

- **权限码**: `hr:leave-balance:update`
- **状态**: ✓ **已实现**（`HrLeaveBalanceController.adjustBalance`）
- **路径参数**: `id` (Long)
- **请求体**: `HrLeaveBalanceAdjustDTO`

| 字段 | 类型 | 必填 | 校验 |
|------|------|------|------|
| id | Long | 是 | 余额ID |
| adjustType | String | 是 | ADD / SUB / SET |
| adjustDays | BigDecimal | 是 | 调整天数 |
| reason | String | 是 | 调整原因 |

### 3.10 `GET /api/hr/leave-rules` — 查询活跃假期规则

- **权限码**: `hr:leave-rule:list`
- **状态**: ✓ **已实现**（`HrLeaveRuleController.listActiveRules`）
- **响应**: `R<List<HrLeaveRuleVO>>`

### 3.11 `PUT /api/hr/leave-rules/{id}` — 更新假期规则

- **权限码**: `hr:leave-rule:update`
- **状态**: ✓ **已实现**（`HrLeaveRuleController.updateRule`）
- **请求体**: `HrLeaveRule` (Entity 直传, ⚠ T1 期间建议改为专用 DTO)

---

## 4. 权限码清单

| 权限码 | 用途 | 出现位置 |
|--------|------|----------|
| `hr:leave:create` | 创建请假 | 3.1 |
| `hr:leave:list` | 分页查询 | 3.2 |
| `hr:leave:detail` | 详情 | 3.3 |
| `hr:leave:revoke` | 撤回 | 3.4 |
| `hr:leave:resubmit` | 重新提交 | 3.5 |
| `hr:leave-balance:view` | 我的余额 | 3.6 |
| `hr:leave-balance:list` | 管理端余额 | 3.7 |
| `hr:leave-balance:init` | 初始化余额 | 3.8 |
| `hr:leave-balance:update` | 调整余额 | 3.9 |
| `hr:leave-rule:list` | 查询规则 | 3.10 |
| `hr:leave-rule:update` | 更新规则 | 3.11 |

**注意点**:
- `hr:leave:balance:view` 用 `:` 分隔 4 段（其他都是 3 段）——这是 spec §7.5 草案的写法，但与 spec §11.6 的 `{module}:{resource}:{action}` 三段式不一致。**T1 期间建议统一为 `hr:leave-balance:view`（去掉中间段或合并）**。详见审计报告 P1-? 待决。
- 角色映射: `ADMIN` 拥有所有 `hr:leave*` 权限，`USER` 只拥有 `hr:leave:create`/`list`/`detail`/`balance:view`/`revoke`/`resubmit`

---

## 5. DTO/VO 字段

### 5.1 `HrLeaveCreateDTO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/dto/HrLeaveCreateDTO.java`）

6 字段：leaveType, startTime, endTime, leavePeriod, reason, attachments
校验：NotBlank/NotNull/Size(max=500)

### 5.2 `HrLeaveQueryDTO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/dto/HrLeaveQueryDTO.java`）

9 字段：pageNum, pageSize, empId, deptId, status, leaveType, startDate, endDate, sortField, sortOrder

### 5.3 `HrLeaveVO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/vo/HrLeaveVO.java`）

20 字段：id, applyNo, empId, empName, deptId, deptName, leaveType, leaveTypeName, startTime, endTime, leavePeriod, days, reason, attachments, status, statusName, processInstanceId, currentTaskId, approvedTime, rejectReason, createTime, canRevoke, canResubmit

**非库字段**（业务层填充）: empName, deptName, leaveTypeName, statusName, canRevoke, canResubmit

### 5.4 `HrLeaveBalanceVO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/vo/HrLeaveBalanceVO.java`）

15 字段：id, empId, empName, deptName, leaveType, leaveTypeName, year, totalDays, usedDays, frozenDays, remainingDays, availableDays(计算), expireDate, status, updateTime

### 5.5 `HrLeaveRuleVO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/vo/HrLeaveRuleVO.java`）

11 字段：id, ruleName, leaveType, leaveTypeName, minUnit, maxDaysPerApply, deductBalance, deductSalary, requireAttachment, ruleScript, status, updateTime

### 5.6 `HrLeaveBalanceInitDTO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/dto/HrLeaveBalanceInitDTO.java`）

5 字段：empId, leaveType, year, totalDays, expireDate

### 5.7 `HrLeaveBalanceAdjustDTO` ✓ 已实现
（见 `code/backend/oa-hr/src/main/java/cn/oa/hr/dto/HrLeaveBalanceAdjustDTO.java`）

4 字段：id, adjustType(ADD/SUB/SET), adjustDays, reason

---

## 6. 审批动作（走工作流任务 API，不在 HR 模块）

| 动作 | 路径 | 权限码 | spec 引用 |
|------|------|--------|-----------|
| 同意 | `POST /api/wf/tasks/{taskId}/actions/approve` | `workflow:task:approve` | spec §7.5 |
| 驳回 | `POST /api/wf/tasks/{taskId}/actions/reject` | `workflow:task:reject` | spec §7.5 |
| 转办 | `POST /api/wf/tasks/{taskId}/actions/transfer` | `workflow:task:transfer` | spec §7.5 |

**T1 期间不实现**（属于 T6 工作流回调接入任务）

---

## 7. 索引与 EXPLAIN 验收

（详见 `code/backend/sql/hr_leave_contract.sql` 第 4 节）

| 场景 | SQL | 期望索引 | 期望 EXPLAIN |
|------|-----|----------|--------------|
| 我的请假列表 | `WHERE emp_id=? AND status=? ORDER BY create_time DESC` | `idx_emp_status_time` | type=ref, key=idx_emp_status_time |
| 部门管理列表 | `WHERE dept_id=? AND status=? ORDER BY create_time DESC` | `idx_dept_status_time` | type=ref |
| 工作流回查 | `WHERE process_instance_id=?` | `idx_process_instance` | type=const 或 ref |
| 日期范围 | `WHERE start_time BETWEEN ? AND ?` | `idx_time_range` | type=range |
| 余额唯一 | `UNIQUE (emp_id, leave_type, year)` | `uk_emp_type_year` | INSERT 防重复 |

---

## 8. 与主 spec 的差异 / 修复项

> 这些差异是 T1 期间发现并标记，**用户拍板后再修改主 spec**

### 8.1 spec §7.3 表结构 vs 实际 Entity

- spec 草案没列 `hr_leave_apply.approved_time` / `reject_reason`（实际 Entity 有）
- spec 草案没列 `hr_leave_balance.expire_date` / `status`（实际 Entity 有）
- spec 草案没列 `hr_leave_balance.availableDays`（实际 Entity 注释强调是计算字段）
- spec 草案把 `hr_leave_rule.deduct_balance` 写 `0否1是`（实际 Entity 用 Integer，SQL 用 CHAR(1)）

**建议**: T1 期间把 spec §7.3 §7.4 §7.6 三节**重写为"实际代码字段表"**（与 Entity 对齐），把"草案"明确改名为"已实现"。

### 8.2 spec §7.5 路径前缀规则

- 实际全部用复数（`/api/hr/leaves`）
- spec §11.6 没明说"必须复数"

**建议**: spec §11.6 增加一行："`{resource}` 推荐用复数（`leaves` 而非 `leave`），新接口必须遵守。"

### 8.3 spec §7.5 权限码 4 段 vs 3 段

- `hr:leave:balance:view` 4 段
- 其他都是 `hr:leave:*` 3 段

**建议**: 统一为 3 段 `hr:leave-balance:view`（与 `hr:leave-balance:list` 风格一致）

### 8.4 字符集

- 实际 DDL 用 `utf8mb4_general_ci`
- spec §11.5 要求统一为 `utf8mb4`

**建议**: 实际 DDL 改为 `utf8mb4_unicode_ci`（T1 期间执行）

### 8.5 spec §2.4 vs §7.3 状态对齐

- spec §2.2 实例状态: DRAFT, RUNNING, PASSED, REJECTED, REVOKED, ABORTED, SUSPENDED (7)
- 实际申请单 HrLeaveStatus: DRAFT, RUNNING, PASSED, REJECTED, REVOKED (5)

**建议**: spec 明确"申请单只承载 5 个状态；ABORTED/SUSPENDED 是工作流实例状态，不由申请单表达"。

---

## 9. 验收命令

```bash
# 后端编译 (T1 期间不跑, T3 跑)
cd code/backend && mvn -pl oa-hr,oa-web -am -DskipTests compile

# SQL 语法静态检查 (无 MySQL client 时只能用文本检查)
grep -E "^\s*CREATE TABLE" code/backend/sql/hr_leave_contract.sql
# 期望输出 3 行 (hr_leave_apply, hr_leave_balance, hr_leave_rule)

# 索引数量
grep -cE "^\s*KEY \`" code/backend/sql/hr_leave_contract.sql
# 期望: 至少 5 个 KEY（每个表至少 1-2 个）

# 权限注解在 Controller 中的覆盖率
grep -c "@RequirePermission" code/backend/oa-hr/src/main/java/cn/oa/hr/controller/*.java
# 期望: 至少 11 个 (对应 11 个接口)

# 权限码一致性
grep -h "@RequirePermission" code/backend/oa-hr/src/main/java/cn/oa/hr/controller/*.java \
  | grep -oE '"hr:[^"]+"' | sort -u
# 期望: 11 个不同的权限码
```

---

## 10. T1 完成判据

- [x] `code/backend/sql/hr_leave_contract.sql` 草案 v2 已写
- [x] 本契约文档已写
- [x] 11 个接口路径、权限码、DTO/VO 字段已与实际代码对照
- [x] spec §8 修复建议清单已列出
- [ ] **用户拍板**: spec §7 §11.6 修复项是否可以写入主 spec
- [ ] **用户拍板**: SQL 草案是否合并到 `001_schema.sql`（属于 T3 任务）
- [ ] **用户拍板**: 权限码 4 段 vs 3 段是否统一
