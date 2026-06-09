# P1-1.1 — A 类 10 客户端路径对齐 - Target Map

> 后端端点已存在,只需改前端路径。**今天的目标**。
> 每个客户端的"旧路径 → 新路径"对照表。

---

## 1. `src/api/budget.ts` (3 paths)

后端 `FinBudgetController @ /api/finance/budgets`:`/api/finance/budgets` + `/{id}`

```ts
// 改前 → 改后
"/api/budget"        → "/api/finance/budgets"
"/api/budget/page"   → "/api/finance/budgets"  // 用 GET 列表代替独立 /page
"/api/budget/${id}"  → "/api/finance/budgets/${id}"
```

## 2. `src/api/contract.ts` (5 paths)

后端 `FinContractController @ /api/finance/contracts`:
`/api/finance/contracts` + `/{id}` + `/{id}/activate` + `/{id}/close`

```ts
"/api/contract"        → "/api/finance/contracts"
"/api/contract/page"   → "/api/finance/contracts"
"/api/contract/${id}"  → "/api/finance/contracts/${id}"
"/api/contract/${id}/activate" → "/api/finance/contracts/${id}/activate"
"/api/contract/expiring"        → v2.1 占位 (后端无此端点;GET /api/finance/contracts?status=EXPIRING 替代)
```

## 3. `src/api/expense.ts` (3 paths)

后端 `FinExpenseController @ /api/finance/expenses`:
`POST /api/finance/expenses` (create) + `/{id}/actions/approve` + `/{id}/actions/reject` + `/{id}/actions/withdraw` + `/{id}/actions/resubmit`

```ts
"/api/expense/submit"  → "/api/finance/expenses"  // POST
"/api/expense/page"    → "/api/finance/expenses"  // GET (无独立 page 后端)
"/api/expense/approve" → "/api/finance/expenses/${id}/actions/approve"  // 需 id,业务流里应传入
// 注意:前端当前是 POST /api/expense/approve 不带 id,后端要求 id;改成函数带 id
```

## 4. `src/api/loan.ts` (4 paths)

后端 `FinLoanController @ /api/finance/loans`:
`POST /api/finance/loans` + `/{id}/actions/approve` + `/{id}/actions/reject` + `/{id}/actions/repay`

```ts
"/api/loan/submit"     → "/api/finance/loans"
"/api/loan/page"       → "/api/finance/loans"
"/api/loan/approve"    → "/api/finance/loans/${id}/actions/approve"
"/api/loan/repayment"  → "/api/finance/loans/${id}/actions/repay"
```

## 5. `src/api/asset.ts` (4 paths)

后端:
- `AdmAssetController @ /api/admin/assets`:`/api/admin/assets` + `/{id}` (只有 GET 列表和 GET by id,**无 POST/PUT/DELETE**)
- `AdmAssetLoanController @ /api/admin/asset-loans`:`POST /api/admin/asset-loans` + `/{id}/actions/submit` + `/{id}/actions/return`

```ts
"/api/asset"          → "/api/admin/assets"  // GET 列表
"/api/asset/page"     → "/api/admin/assets"  // GET 列表
"/api/asset/borrow"   → "/api/admin/asset-loans"  // POST 借用申请
"/api/asset/borrow/page" → "/api/admin/asset-loans"  // GET 列表
// 注: 后端无 /api/admin/assets 的 POST/PUT/DELETE,改前端会暴露更多 404;但读列表能 work
```

## 6. `src/api/document.ts` (2 paths)

后端 4 个 controller:archives / dispatches / receives / sign-reports (无统一 /page,无 /upload)

```ts
"/api/document/page"  → v2.1 占位 reject(后端无统一 list)
// 各具体子端点(archives/dispatches/...)需新建对应 API 客户端函数
"/api/document/upload" → v2.1 占位 reject(后端无文件上传端点)
```

## 7. `src/api/empArchive.ts` (2 paths)

后端 `SysEmpController @ /api/system/emps` 已有员工 CRUD 雏形,但实际是 oa-system 的"用户"不是"员工档案"。`/api/hr/employees` 在 controller 里被 `oa-hr-employee` 用,具体接口格式需查证。

```ts
"/api/emp-archive"     → v2.1 占位
"/api/emp-archive/page"→ v2.1 占位
// 真实业务应该是 GET /api/system/emps (员工用户) 或 GET /api/hr/employees (员工档案实体)
// 当前 oa-hr-employee 模块我没扫 controller,留待 P1-1.4 验证
```

## 8. `src/api/attendance.ts` (5 paths)

后端 `HrAttendanceRecordController @ /api/hr-attendance/records`:
`/api/hr-attendance/records/clock-in` + `/clock-out` + `GET /` (list) + `GET /api/hr-attendance/records` (list)

```ts
"/api/attendance/clock-in"  → "/api/hr-attendance/records/clock-in"
"/api/attendance/clock-out" → "/api/hr-attendance/records/clock-out"
"/api/attendance/today"     → "/api/hr-attendance/records/today"  // 后端无 today,需 GET list + filter by today
"/api/attendance/history"   → "/api/hr-attendance/records"        // 后端无 history 端点;同 list
"/api/attendance/admin/page" → "/api/hr-attendance/records"       // admin 看全部
```

## 9. `src/api/attendanceGroup.ts` (2 paths)

后端 **无 controller**(`HrAttendanceGroupService` 存在但没 Controller)。需要新 controller 或留 v2.1。

```ts
"/api/attendance-group"      → v2.1 占位
"/api/attendance-group/page" → v2.1 占位
```

## 10. `src/api/workflow.ts` (16 paths)

后端:
- `WfDefinitionController @ /api/workflow/definitions`
- `WfDelegationController @ /api/workflow/delegations`
- `WfInstanceController @ /api/workflow/instances`
- `WfTaskController @ /api/workflow/tasks`

```ts
"/api/workflow/definition"          → "/api/workflow/definitions"
"/api/workflow/definition/list"     → "/api/workflow/definitions"
"/api/workflow/definition/activate" → "/api/workflow/definitions/{id}/activate"  // 需 id
"/api/workflow/definition/{id}"     → "/api/workflow/definitions/{id}"

"/api/workflow/delegation/my"       → "/api/workflow/delegations/my"
"/api/workflow/delegation/set"      → "/api/workflow/delegations"  // POST

"/api/workflow/task/pending"        → "/api/workflow/tasks/pending"
"/api/workflow/task/handled"        → "/api/workflow/tasks/handled"
"/api/workflow/task/find/{id}"      → "/api/workflow/tasks/{id}"
"/api/workflow/task/handle"         → "/api/workflow/tasks/{id}/handle"  // 需 id
"/api/workflow/task/return"         → "/api/workflow/tasks/{id}/return"
"/api/workflow/task/transfer"       → "/api/workflow/tasks/{id}/transfer"
"/api/workflow/task/urge"           → "/api/workflow/tasks/{id}/urge"

"/api/workflow/cc/my"               → v2.1 (后端无 cc 端点;通常用 /api/workflow/instances?cc=true)
"/api/workflow/history"             → v2.1 (后端无 history 端点;按 instance 查)
"/api/workflow/withdraw"            → "/api/workflow/instances/{id}/withdraw"  // 需 id
"/api/workflow/approval-chain"      → v2.1 (后端无 chain 端点)
```

---

## 待办 (Phase 1.1)

| 客户端 | 后端已有? | 改法 |
|---|---|---|
| budget.ts | ✅ | 字符串替换 3 处 |
| contract.ts | ✅ | 替换 4 处 + expiring 占位 1 处 |
| expense.ts | ✅ | 替换 3 处 + approve 函数签名加 id |
| loan.ts | ✅ | 替换 4 处 + approve/repay 函数签名加 id |
| asset.ts | ✅(部分) | 列表 2 处可改;POST/PUT/DELETE 后端缺,留 v2.1 |
| document.ts | ❌(无统一 /page/upload) | 全部 v2.1 占位 |
| empArchive.ts | ❌(需先确认模块归属) | 全部 v2.1 占位 |
| attendance.ts | ✅(records) | 替换 5 处 |
| attendanceGroup.ts | ❌(无 controller) | 全部 v2.1 占位 |
| workflow.ts | ✅(tasks/instances/...) | 替换 ~12 处,余 4 处 v2.1 占位 |

**总修改:10 客户端,~50 处路径,1 个新报告增量,0 后端改动**

验证:
- pnpm type-check → 0 errors
- 后端编译 0 改动 → 18/18 SUCCESS 仍
