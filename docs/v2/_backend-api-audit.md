# 后端 API 同步下线 - 摸底报告

**日期**: 2026-06-08
**上下文**: 前端 P0 (commit `405c8d5`) 下线了 9 个 vue 文件 + 17 个路由,但**后端 API 端点 0 改动**。
本次摸底回答:**"哪些后端 controller 路径,前端已不再调用,可以下线?"**

---

## 一、关键事实(已 grep 全量验证)

### 前端对 `/api/leave/*` 的调用矩阵

| 前端文件 | 调用的后端 API | 后端真实端点 |
|---|---|---|
| `api/hr-leave.ts` (新, `apiGet/apiPost`) | `/api/leave/page`、`/api/leave/submit`、`/api/leave/{id}/actions/revoke`、`/api/leave-balance/my` | `HrLeaveController` / `HrLeaveBalanceController` |
| `api/leave.ts` (旧, `request.get/post`) | `/api/leave/page` (getLeavePage)、`/api/leave/approve` (approveLeave) | 同上 (注意:`/api/leave/submit` 在 `leave.ts` 里有定义但**0 引用**) |
| `api/leaveBalance.ts` (旧) | `/api/leave-balance/my`、`/api/leave-balance/page`、`/api/leave-balance/init` | `HrLeaveBalanceController` |

### 前端对 `/api/workflow/*` 的调用矩阵

| 前端文件 | 调用的后端 API | 后端真实端点 |
|---|---|---|
| `api/workflow.ts` (被 approval-center / hr-leave / 6 个业务申请页引用) | 14+ 端点(任务/流程/委派/cc 等) | `WfTaskController` / `WfInstanceController` / `WfDelegationController` |

### 前端对各业务 API 的调用

| 业务 | API 客户端 | 引用方 |
|---|---|---|
| 请假 | `hr-leave.ts` (新) + `leave.ts` (旧) | hr-leave/* + approval-center + workbench |
| 出差 | `businessTrip.ts` | approval-center |
| 外出 | `outing.ts` | approval-center |
| 采购 | `purchase.ts` | approval-center |
| 报销 | `expense.ts` | approval-center |
| 加班 | `overtime.ts` | approval-center |
| 借款 | `loan.ts` | approval-center |
| 工作流 | `workflow.ts` | approval-center + 6 个业务申请页 + hr-leave |

---

## 二、待删/待并/待留(分 3 档)

### 档 A:前端**可下线**的 API 客户端(它们的全部端点都被新 hr-leave 替代)

| 文件 | 状态 | 风险 |
|---|---|---|
| `code/frontend/src/api/leaveBalance.ts` | 0 引用,可直接删 | 无 |
| `code/frontend/src/api/leave.ts` (注意:`submitLeave` 0 引用,但 `getLeavePage` + `approveLeave` 被 `approval-center.ts` / `workbench` 引用,**不能直接删**) | 部分死代码 | **中** |

**`leave.ts` 处理方案**:不直接删,先迁:
1. 把 `getLeavePage` 和 `approveLeave` 函数体合并进 `hr-leave.ts` (用 `apiGet` / `apiPost` 重写)
2. `approval-center.ts` 改成 `import { getLeavePage, approveLeave } from "@/api/hr-leave"`
3. `workbench/index.vue` 改成 `import { getLeavePage } from "@/api/hr-leave"`
4. 删 `leave.ts`

### 档 B:后端**真不删**(它们仍被前端使用)

| 后端 controller | 路径 | 前端引用方 |
|---|---|---|
| `HrLeaveController` | `/api/leave/*` | hr-leave.ts (新) + approval-center/workbench (旧 leave.ts) |
| `HrLeaveBalanceController` | `/api/leave-balance/*` | hr-leave.ts (新),无前端引用方调用了 `init` / `page` (但保留备用) |
| `WfTaskController` / `WfInstanceController` / `WfDefinitionController` / `WfDelegationController` | `/api/workflow/*` | workflow.ts (真活) |
| 7 个业务 controller (出差/外出/采购/报销/加班/借款) | 各自 | 各自的 API 客户端 + approval-center |

### 档 C:**真死**的代码(0 引用,可删)

| 项 | 文件/类 | 状态 |
|---|---|---|
| `submitLeave` | `code/frontend/src/api/leave.ts` | 0 引用,函数定义内嵌 |
| 后端 6 个具体业务审批端点 (`/api/business-trip/approve` 等) | 7 个 controller 内 | 都被 `approvalTypeConfigs` 在 `approval-center.ts` 调用,**真活** (虽然审批中心也调 `workflow.ts.handleTask`) |

---

## 三、推荐执行方案(等你确认)

### 方案 1(推荐,只清前端死代码)
1. 把 `leave.ts` 的 `getLeavePage` + `approveLeave` 合并到 `hr-leave.ts`
2. 改 `approval-center.ts` 和 `workbench/index.vue` 的 import
3. 删 `leave.ts` 整个文件
4. 删 `leaveBalance.ts` 整个文件 (0 引用)
5. 验证 `pnpm type-check + build`
6. git commit (不 push)

**预期效果**:
- 前端 API 客户端:34 → 32 个文件
- 后端 0 改动
- 风险:低(纯前端内务)

### 方案 2(进阶,清前后端)
- 在方案 1 基础上,审查 `HrLeaveController` 是否有端点前端 0 引用
- 这需要逐个端点 grep,**风险中等,需要多花时间**

### 方案 3(暂不动)
- 后端 controller 0 改动,前端的死代码留在那里
- 风险:0,但代码冗余持续存在

---

## 四、**关键澄清**:我**不能**直接帮你"删后端 controller"

`HrLeaveController` 提供的 7 个端点(`/api/leave/page`、`/api/leave/submit`、`/api/leave/{id}`、`/api/leave/{id}/actions/revoke`、`/api/leave/balances/me`、`/api/leave/approve` 等)**全部被前端 `hr-leave.ts` 真实使用**——这个 controller 不能删。

**唯一能删的后端代码 = 前端对应的 0 引用 API 客户端文件**(2 个:leave.ts、leaveBalance.ts)。

---

## 五、等你回复"方案 1 / 2 / 3"
