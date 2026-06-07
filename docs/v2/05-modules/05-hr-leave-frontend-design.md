# oa-hr-leave 前端页面设计报告

> 范围：5 个 Vue 页面 + 1 个 API 客户端 + 路由注册 + 组件复用 + UI 风格
> 设计基线：v2-platform 分支，`oa-hr-leave` 后端已实现提交/撤回/查询/详情/余额（commit `8c0c06b` 后）
> 本报告锁定 **2026-06-07** 当日后端实际能力，纠正了原任务清单与现状的偏差

---

## 0. 关键发现（影响设计）

| 维度 | 原任务清单假设 | 后端实际能力 | 设计结论 |
|------|---------------|-------------|---------|
| `pending list` 端点 | `GET /api/v1/hr-leave/tasks/pending` | **不存在**；待办走 `oa-workflow` 的 `GET /api/workflow/task/pending?businessType=hr_leave` | 复用 `@/api/workflow.ts` 的 `getPendingTasks`，按 `businessType` 过滤；不新增 hr-leave 端点 |
| `approve / reject` 端点 | `POST /api/v1/hr-leave/leaves/{id}/actions/{approve,reject}` | **不存在**；审批走 `oa-workflow` 的 `POST /api/workflow/task/handle {taskId, status, remark}` | 同上，复用 workflow 端点 |
| `resubmit` 端点 | `POST /leaves/{id}/actions/resubmit` | **不存在**；后端**未实现"驳回到发起人"重提** | 不实现，PendingApprovals 中"驳回"动作走 workflow handle；MyLeaves 中无 resubmit 按钮 |
| `leaveType` 类型 | `number`（1-7） | **`String`**（`ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY`） | 同步类型修复：`@/api/hr-leave.ts` 与 `@/types/api.ts` 全部由 `number` 改 `string` |
| `status` 类型 | `number`（0-3） | **`String`**（`PENDING/APPROVED/REJECTED/CANCELLED`） | 同上；并删除旧 v1 `LEAVE_TYPE_MAP / LEAVE_STATUS_MAP`（按数字） |
| `revoke` 端点 | `POST /leaves/{id}/actions/revoke` | ✅ **存在** | 沿用 |
| `getDetail` 端点 | `GET /leaves/{id}` | ✅ **存在**，但返回 `Map<String,Object>`（含 wfInstanceId 关联的审批历史） | 详情页用 `HrLeaveDetailVO` 强类型，新增 `workflow.ts.getApprovalChain` 调用 |
| `myBalances` 端点 | `GET /balances/me` | ✅ **存在** 两条路径：<br>1) `/api/v1/hr-leave/balances/me` → `List<HrLeaveBalance>`（含 year/usedDays/frozenDays/remainingDays，**更完整**）<br>2) `/api/v1/hr-leave/leaves/balances/me` → `List<Map>`（历史 v1 端点） | **统一使用路径 1**（`HrLeaveBalance` 实体，含冻结天数），移除路径 2 调用 |
| `listMy` 端点 | `/leaves/me` | **实际为 `/leaves/mine`**（`HrLeaveController#myLeaves`） | 修正 `@/api/hr-leave.ts` |
| `create` 端点 | `POST /leaves` | ✅ **存在**，返回 `R<Long>` | 沿用 |
| 路由挂载 | `/hr-leave/*` 4 条 | **尚未注册**（router 仅有 v1 风格 `oa/leave/apply` 等） | 全部新增 4 条 v2 路由 |

**设计原则**：

1. **不发明端点**：所有 axios 调用必须对应后端真实 Controller（`HrLeaveController` / `HrLeaveBalanceController` / `WfTaskController` / `WfApprovalController`）。
2. **类型对齐后端**：`leaveType/status` 一律 `string`，枚举值与 `@TableField` 值完全一致。
3. **审批与请假解耦**：PendingApprovals 页只调 `@/api/workflow.ts`，不调 `hr-leave.ts`。这样换审批引擎不影响业务模块。
4. **不引入新依赖**：仅用 Element Plus 现有组件（`el-card / el-tag / el-timeline / el-date-picker / el-form / el-dialog`）+ `@/api/workflow.ts` + `@/api/hr-leave.ts` + `@/store/user.ts`。

---

## 1. 目录结构

```
code/frontend/src/
├── api/
│   ├── hr-leave.ts                  (A) 重写：类型修正 + 调用 v2 端点
│   └── workflow.ts                  (M) 不改，复用 getPendingTasks / handleTask / getApprovalChain
├── constants/
│   └── hr-leave.ts                  (A) 业务常量集中
├── views/hr-leave/
│   ├── MyLeaves.vue                 (A) 我的请假列表
│   ├── LeaveFormDialog.vue          (A) 申请/编辑弹窗
│   ├── PendingApprovals.vue         (A) 待我审批
│   ├── LeaveDetail.vue              (A) 详情 + 时间线
│   └── MyBalances.vue               (A) 假期余额
├── router/index.ts                  (M) 注册 4 条 /hr-leave/* 路由
└── types/
    └── api.ts                       (M) 修正 HrLeaveVO/HrLeaveCreateVO/LeaveBalanceVO 字段类型
```

---

## 2. 5 个 Vue 页面设计

### 2.1 `MyLeaves.vue` — 我的请假列表

**目标**：申请人主入口，看历史 + 撤回 + 跳转申请

**布局**（卡片式，不用 el-table，与原任务清单 §5 一致）：

```
┌──────────────────────────────────────────────────────────┐
│ 我的请假                  [ + 申请请假 ] [ 余额入口 ]    │ ← 顶部操作条
├──────────────────────────────────────────────────────────┤
│ [▼ 状态] [▼ 类型] [日期范围] [搜索]                       │ ← 筛选区
├──────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────┐    │
│ │ [PENDING] 年假  6/10 ~ 6/12 (3天)               │    │ ← 一行一卡片
│ │ 事由: 家庭旅行  申请人: 张三  提交: 06-07 14:23  │    │
│ │                          [详情] [撤回]            │    │
│ └──────────────────────────────────────────────────┘    │
│ ┌──────────────────────────────────────────────────┐    │
│ │ [APPROVED] 病假  ...                              │    │
│ └──────────────────────────────────────────────────┘    │
│  ...                                                      │
│              [el-pagination]                              │
└──────────────────────────────────────────────────────────┘
```

**Props/Emits**：无（顶层路由组件）

**State**：
```ts
const filter = reactive({ status: '', leaveType: '', pageNum: 1, pageSize: 10 });
const list = ref<HrLeaveVO[]>([]);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const editingLeave = ref<HrLeaveVO | null>(null);
```

**Methods**：
- `loadList()` → `leaveApi.listMine(filter)`
- `onCreate()` → `dialogVisible=true; editingLeave=null`
- `onEdit(row)` → 仅 PENDING 状态可编辑（弹同一 `LeaveFormDialog`）
- `onRevoke(row)` → `ElMessageBox.confirm` → `leaveApi.revoke(row.id)` → 刷新
- `onViewDetail(row)` → `router.push({ name: 'LeaveDetail', params: { id: row.id } })`

**Status 颜色映射**（与原任务清单 §5 一致）：
| status | 中文 | tag type | 色值 |
|--------|------|---------|------|
| PENDING | 待审批 | warning | #E6A23C |
| APPROVED | 已通过 | success | #67C23A |
| REJECTED | 已拒绝 | danger | #F56C6C |
| CANCELLED | 已撤回 | info | #909399 |

**权限 meta**：`hr-leave:leave:list`

**空态**：`<el-empty description="还没有请假记录" />`

---

### 2.2 `LeaveFormDialog.vue` — 申请/编辑弹窗

**Props**：
```ts
defineProps<{
  modelValue: boolean;       // 必填，v-model 显隐
  editing?: HrLeaveVO | null; // 可空，null=新增，有值=编辑
}>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; 'success': [] }>();
```

**布局**（`el-dialog`，width 560px，title 动态）：
```
┌─ 申请请假 / 编辑请假单 ────────────────── × ┐
│                                              │
│  请假类型 *  [▼ 年假]                         │
│  开始日期 *  [2026-06-10]                     │
│  结束日期 *  [2026-06-12]  共 3 天            │  ← 自动算 totalDays
│  请假事由    [____________________________]  │  ← el-input type=textarea, maxlength=500
│                                              │
│              [取消]  [确定提交]              │
└──────────────────────────────────────────────┘
```

**字段约束**（与后端 `HrLeaveCreateDTO` 一致）：
| 字段 | 控件 | 校验 | 默认值 |
|------|------|------|--------|
| leaveType | `el-select` | 必填，枚举 | `''` |
| startDate | `el-date-picker` (value-format="YYYY-MM-DD") | 必填，不能早于今天 | today |
| endDate | `el-date-picker` | 必填，`>= startDate` | today |
| reason | `el-input` (textarea, rows=3) | 选填，<=500 字 | `''` |

**关键逻辑**：
- `watch([startDate, endDate])` → 自动 `Math.max(0, (end - start + 1))` 显示天数
- 提交：`editing` 为空时调 `create(dto)`，否则调 `update(dto)`（**后端目前无 update 端点**，故先仅支持新增；编辑按钮在 `MyLeaves` 暂不渲染，避免误用）

> 设计取舍：原任务清单第 2 项要求支持"申请/编辑"。但后端无 update 端点。本设计**先实现"申请"**，MyLeaves 的"编辑"按钮暂不暴露（避免 mock）；待后端补 `PUT /leaves/{id}` 再开放。

**确定按钮**：
- 校验 → `formRef.value?.validate()` → `await leaveApi.create(form.value)` → 触发 `success` 事件 → 父页 `loadList()` → `ElMessage.success('已提交')`

**权限 meta**：`hr-leave:leave:create`

---

### 2.3 `PendingApprovals.vue` — 待我审批

**数据源**：不调 `hr-leave.ts`，统一调 `@/api/workflow.ts`

**布局**（与 MyLeaves 同款卡片）：
```
┌──────────────────────────────────────────────────────────┐
│ 待我审批（请假）                  [▼ 紧急程度] [刷新]     │
├──────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────┐    │
│ │ 张三 / 技术部 / 年假 6/10~6/12 (3天)            │    │
│ │ 事由: 家庭旅行                                    │    │
│ │ 当前节点: 部门经理审批  到达时间: 06-07 14:25     │    │
│ │              [查看详情] [批准] [驳回]              │    │
│ └──────────────────────────────────────────────────┘    │
│  ...                                                      │
└──────────────────────────────────────────────────────────┘
```

**State**：
```ts
const filter = reactive({ pageNum: 1, pageSize: 10, businessType: 'hr_leave' });
const list = ref<WorkflowTask[]>([]);
const total = ref(0);
const loading = ref(false);
```

**关键差异**（与原任务清单对照）：
- `listPendingApprovals` → **复用** `getPendingTasks({ businessType: 'hr_leave' })`，不调 `hr-leave/tasks/pending`
- 卡片内"请假类型/日期"从 `WorkflowTask.businessData` 中读（后端 `wf_tasks` 表中 `wf_task.business_data` JSON 字段含 `leaveType/startDate/endDate/days/reason` 摘要）

**操作**：
- `onApprove(row)` → `ElMessageBox.prompt` 收集意见 → `handleTask({ taskId: row.id, status: 1, remark })`
- `onReject(row)` → 同上 + `status: 2`
- `onViewDetail(row)` → `router.push({ name: 'LeaveDetail', params: { id: row.businessId } })`

**权限 meta**：`workflow:task:approve`（**复用工作流权限**，非 hr-leave 域权限）

**为什么不用 hr-leave 域权限**：避免在 `sys_permission` 表中重复登记"请假审批"码；统一通过 workflow 权限管控审批人。HrLeaveController 的 `submit / revoke / list` 使用 `hr-leave:*` 即可。

---

### 2.4 `LeaveDetail.vue` — 请假详情

**路由**：`/hr-leave/detail/:id`

**布局**（左右分栏，sticky 时间线）：
```
┌──────────────────────────────────────────────────────────┐
│ ← 返回  请假单 #12345                  [PENDING]  2026-06-07 │
├──────────────────────────────┬───────────────────────────┤
│ 申请人: 张三                │ 审批历史                  │
│ 部门: 技术部                 │ ┌────────────────────┐    │
│ 类型: 年假                   │ │ ● 张三 提交         │    │
│ 时间: 2026-06-10~06-12 (3天)│ │ │  06-07 14:23       │    │
│ 事由: 家庭旅行               │ │ ● 李四 已通过       │    │
│ 流程实例 ID: 45678          │ │ │  06-07 15:10       │    │
│ 提交时间: 06-07 14:23       │ │ ◌ 王五 部门经理审批 │    │
│                              │ │   (待处理)          │    │
│                              │ └────────────────────┘    │
└──────────────────────────────┴───────────────────────────┘
```

**State**：
```ts
const id = computed(() => Number(route.params.id));
const detail = ref<HrLeaveDetailVO | null>(null);
const timeline = ref<ApprovalRecord[]>([]);
const loading = ref(false);
```

**数据加载**：
```ts
const [d, t] = await Promise.all([
  leaveApi.getDetail(id.value),
  getApprovalChain({ businessType: 'hr_leave', businessId: id.value })
]);
detail.value = d;
timeline.value = t;
```

**时间线**：`<el-timeline>` 渲染 `timeline`，节点状态用 `tag`（PENDING=warning / APPROVED=success / REJECTED=danger）

**权限 meta**：`hr-leave:leave:view`

---

### 2.5 `MyBalances.vue` — 我的假期余额

**数据源**：`HrLeaveBalanceController.myBalances()` → `GET /api/v1/hr-leave/balances/me`

**布局**（卡片网格 + 进度条）：
```
┌──────────────────────────────────────────────────────────┐
│ 我的假期余额 (2026)                                        │
├──────────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│ │ 年假      │ │ 事假      │ │ 病假      │ │ 调休      │     │
│ │          │ │          │ │          │ │          │     │
│ │   10     │ │    3     │ │   15     │ │    0     │     │
│ │          │ │          │ │          │ │          │     │
│ │ ▓▓▓░░░░ │ │ ▓░░░░░░ │ │ ▓▓▓▓░░░ │ │ ░░░░░░░░ │     │
│ │ 已用 4   │ │ 已用 0   │ │ 已用 8   │ │ 已用 0   │     │
│ │ 剩余 6   │ │ 剩余 3   │ │ 剩余 7   │ │ 剩余 0   │     │
│ │ (冻结 1) │ │          │ │          │ │          │     │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
└──────────────────────────────────────────────────────────┘
```

**字段**（与 `HrLeaveBalanceVO` 完全一致）：
| 字段 | 来源 | 渲染 |
|------|------|------|
| leaveType | DB String | 经 `LEAVE_TYPE_LABEL[leaveType]` 映射显示 |
| totalDays | BigDecimal | 大字号显示 |
| usedDays | BigDecimal | "已用" 文字 |
| remainingDays | BigDecimal | "剩余" 文字 |
| frozenDays | BigDecimal | 副标 "(冻结 x)" |
| year | Integer | 顶部 "2026" 标题 |

**颜色规则**：
- `remainingDays <= 0` → 红色 `#F56C6C`
- `remainingDays / totalDays < 0.3` → 警告 `#E6A23C`
- 否则主色 `#409EFF`

**空态**：`<el-empty description="暂未配置假期余额，请联系 HR" />`

**权限 meta**：`hr-leave:leave-balance:view`（沿用后端 `RequirePermission` 字符串）

---

## 3. API 客户端 — `src/api/hr-leave.ts`（重写）

```ts
import { apiGet, apiPost } from "@/utils/http";

// ── 类型（与后端 VO/DTO 严格对齐）───────────────────────────────────

export type LeaveType = "ANNUAL" | "SICK" | "PERSONAL" | "MARRIAGE" | "MATERNITY";
export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface HrLeaveVO {
  id: number;
  empId: number;
  empName?: string;       // 详情时由后端补
  deptName?: string;      // 详情时由后端补
  leaveType: LeaveType;
  startDate: string;      // YYYY-MM-DD
  endDate: string;
  totalDays: number;
  reason?: string;
  status: LeaveStatus;
  wfInstanceId?: number;
  createTime: string;
}

export interface HrLeaveDetailVO extends HrLeaveVO {
  /** 详情专用：含流程实例概要，wfInstance 关联的 approval chain 单独调 */
  wfInstanceSummary?: {
    instanceId: number;
    currentNodeName?: string;
    currentAssigneeName?: string;
  };
}

export interface HrLeaveBalanceVO {
  id: number;
  empId: number;
  leaveType: LeaveType;
  year: number;
  totalDays: number;
  usedDays: number;
  frozenDays: number;
  remainingDays: number;
  status: string;
}

export interface HrLeaveCreateDTO {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface HrLeaveQueryDTO {
  status?: LeaveStatus;
  leaveType?: LeaveType;
  pageNum?: number;
  pageSize?: number;
}

// ── 枚举映射（与 src/constants/hr-leave.ts 同步）────────────────────

export const LEAVE_TYPE_LABEL: Record<LeaveType, string> = {
  ANNUAL: "年假",
  SICK: "病假",
  PERSONAL: "事假",
  MARRIAGE: "婚假",
  MATERNITY: "产假",
};

export const LEAVE_STATUS_TAG: Record<LeaveStatus, "warning" | "success" | "danger" | "info"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "danger",
  CANCELLED: "info",
};

// ── API 方法（仅暴露后端真实存在的端点）─────────────────────────────

export const leaveApi = {
  /** POST /api/v1/hr-leave/leaves — 提交请假 */
  create(data: HrLeaveCreateDTO) {
    return apiPost<number>("/api/v1/hr-leave/leaves", data);
  },

  /** POST /api/v1/hr-leave/leaves/{id}/actions/revoke — 撤回 */
  revoke(id: number) {
    return apiPost<void>(`/api/v1/hr-leave/leaves/${id}/actions/revoke`, {});
  },

  /** GET /api/v1/hr-leave/leaves/mine — 我的请假分页 */
  listMine(params: HrLeaveQueryDTO) {
    return apiGet<PageResult<HrLeaveVO>>("/api/v1/hr-leave/leaves/mine", params);
  },

  /** GET /api/v1/hr-leave/leaves/{id} — 详情 */
  getDetail(id: number) {
    return apiGet<HrLeaveDetailVO>(`/api/v1/hr-leave/leaves/${id}`);
  },

  /** GET /api/v1/hr-leave/balances/me — 我的假期余额（v2 标准端点） */
  listBalances() {
    return apiGet<HrLeaveBalanceVO[]>("/api/v1/hr-leave/balances/me");
  },
};
```

> **不再实现** `approve / reject / resubmit / listPendingApprovals`——这些是任务清单的预想但后端未提供。PendingApprovals 改用 `@/api/workflow.ts`。

---

## 4. 业务常量 — `src/constants/hr-leave.ts`（新增）

```ts
import type { LeaveType, LeaveStatus } from "@/api/hr-leave";

export const LEAVE_TYPE_OPTIONS: { value: LeaveType; label: string }[] = [
  { value: "ANNUAL", label: "年假" },
  { value: "SICK", label: "病假" },
  { value: "PERSONAL", label: "事假" },
  { value: "MARRIAGE", label: "婚假" },
  { value: "MATERNITY", label: "产假" },
];

export const LEAVE_STATUS_LABEL: Record<LeaveStatus, string> = {
  PENDING: "待审批",
  APPROVED: "已通过",
  REJECTED: "已拒绝",
  CANCELLED: "已撤回",
};

export const HR_LEAVE_PERMS = {
  LIST: "hr-leave:leave:list",
  CREATE: "hr-leave:leave:create",
  APPROVE: "hr-leave:leave:approve",
  VIEW: "hr-leave:leave:view",
  BALANCE_VIEW: "hr-leave:leave-balance:view",
} as const;
```

---

## 5. 路由注册 — `src/router/index.ts`（增量修改）

在 `children: [ ... ]` 数组 **"人事管理"小节之后** 插入 4 条 v2 路由：

```ts
// ── HR 请假管理（v2）─────────────────────────────────────────────
{
  path: "hr-leave/my-leaves",
  name: "HrLeaveMyLeaves",
  component: () => import("@/views/hr-leave/MyLeaves.vue"),
  meta: {
    title: "我的请假",
    icon: "Tickets",
    permission: "hr-leave:leave:list",
  },
},
{
  path: "hr-leave/pending",
  name: "HrLeavePendingApprovals",
  component: () => import("@/views/hr-leave/PendingApprovals.vue"),
  meta: {
    title: "待我审批（请假）",
    icon: "CircleCheck",
    permission: "workflow:task:approve",
  },
},
{
  path: "hr-leave/balances",
  name: "HrLeaveMyBalances",
  component: () => import("@/views/hr-leave/MyBalances.vue"),
  meta: {
    title: "假期余额",
    icon: "Coin",
    permission: "hr-leave:leave-balance:view",
  },
},
{
  path: "hr-leave/detail/:id",
  name: "HrLeaveDetail",
  component: () => import("@/views/hr-leave/LeaveDetail.vue"),
  meta: {
    title: "请假详情",
    icon: "Document",
    permission: "hr-leave:leave:view",
    hidden: true, // 不在菜单中显示，仅作为跳转目标
  },
},
```

> 保留原有 v1 路由（`oa/leave/apply` 等）——任务清单 §survey 提到"前端 v1/v2 双轨"，本设计**新增加 v2 路由但不动 v1**，避免破坏其他引用 v1 的页面。

**权限守卫**：
- 当前 `router.beforeEach` 只检查 `meta.roles`，**未检查 `meta.permission`**。本次设计**不修改守卫**（避免动 `router/index.ts` 的认证流）；页面内自检 `useUserStore().permissions.includes(HR_LEAVE_PERMS.LIST)`，无权限显示 `el-result 403`。

---

## 6. 类型修正 — `src/types/api.ts`（增量修改）

定位到 262 / 277 两处 `leaveType?: number`（位于 v1 的 `LeaveApplyRequest / LeaveApproveRequest`），**保留不动**（v1 路由仍在用）；同时在本文件追加 hr-leave v2 强类型（避免污染 v1 命名空间）：

```ts
// ── v2 hr-leave 模块 ──────────────────────────────────────────────
export type V2LeaveType = "ANNUAL" | "SICK" | "PERSONAL" | "MARRIAGE" | "MATERNITY";
export type V2LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface V2HrLeaveVO {
  id: number;
  empId: number;
  leaveType: V2LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason?: string;
  status: V2LeaveStatus;
  wfInstanceId?: number;
  createTime: string;
}
```

`@/api/hr-leave.ts` 使用 `V2HrLeaveVO` 而不复用旧 v1 `LeaveVO`（避免 v1 强类型 `leaveType: number` 污染）。

---

## 7. 组件复用与依赖

| 复用资源 | 路径 | 用法 |
|---------|------|------|
| `useUserStore` | `@/store/user.ts` | `userStore.userInfo?.empId` 显示当前申请人；`permissions.includes(...)` 做按钮级显隐 |
| `request.get/post` | `@/utils/http.ts`（apiGet/apiPost）| 所有 axios 调用统一入口 |
| `ApprovalTimeline.vue` | `@/components/ApprovalTimeline.vue` | **复用**渲染 `LeaveDetail.vue` 审批时间线 |
| `Element Plus` 组件 | `el-card / el-tag / el-timeline / el-date-picker / el-form / el-dialog / el-pagination / el-empty / el-progress / el-message-box` | **全部已在依赖中**（见 `package.json` element-plus），不新增 |
| `@/api/workflow.ts` | `getPendingTasks / handleTask / getApprovalChain` | PendingApprovals 与 LeaveDetail 复用 |
| `@/api/hr-leave.ts` | `leaveApi.*` | MyLeaves/LeaveFormDialog/MyBalances 复用 |
| 业务常量 | `@/constants/hr-leave.ts` | 5 个页面共享 |

**新增依赖**：无

**新增 npm 包**：无

---

## 8. UI 风格落地

| 项 | 值 | 来源 |
|----|---|------|
| 主色 | `#409EFF` | Element Plus 默认（与 login 页 `.btn-primary` 蓝调一致） |
| 辅色 | 状态色 | 见 §2.1 表格 |
| 字体 | 系统默认 + 数字大字 `font-size: 28px; font-weight: 600;` 余额 | 与 login 页保持同一字体栈 |
| 圆角 | `--el-border-radius-base: 4px` | 卡片 `border-radius: 8px`（更柔和） |
| 阴影 | `box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05)` | Element Plus 默认卡片 |
| 列表形态 | **卡片式**（每行一卡） | 原任务清单 §5 |
| 状态标识 | `el-tag` 圆角胶囊 | 与 login 页 `.eyebrow` 视觉一致 |

**关键视觉一致性**（与 login 页 5 角色舞台）：
- 卡片用 `border-radius: 8px` + 淡阴影，不走 Material 锐角
- 主操作按钮用 Element Plus `type="primary"`（不自定义），避免与 login 页 `.btn-primary` 冲突
- 配色严格走 Element Plus `--el-color-*` 变量，方便后期主题切换

---

## 9. 实现工时估算

| 阶段 | 任务 | 估时 |
|------|------|------|
| 1 | 修正 `@/api/hr-leave.ts` 类型 + 端点（**M 必修**） | 0.5h |
| 2 | 新增 `@/constants/hr-leave.ts` | 0.2h |
| 3 | `MyLeaves.vue` | 0.7h |
| 4 | `LeaveFormDialog.vue` | 0.6h |
| 5 | `PendingApprovals.vue`（复用 workflow） | 0.7h |
| 6 | `LeaveDetail.vue`（含时间线） | 0.6h |
| 7 | `MyBalances.vue` | 0.4h |
| 8 | 路由 4 条 + 权限 meta | 0.2h |
| 9 | 端到端联调（含 `useUserStore` empId 注入） | 1.0h |
| 10 | `pnpm typecheck` + `pnpm lint` + `pnpm build` | 0.3h |
| **合计** | | **5.2h** |

比原任务清单 5.5h 少 0.3h（避免 resubmit mock），缓冲 0.3h 用于联调时的字段微调。

---

## 10. 联调冒烟测试清单

1. **登录** → 进入 `/hr-leave/my-leaves` → 看到 200 OK + 空态
2. **申请请假** → 选年假 + 日期 + 事由 → 提交 → 列表多一条 PENDING
3. **撤回** → PENDING 行点撤回 → 弹框确认 → 状态变 CANCELLED，余额冻结 -1
4. **审批** → 切换到有审批权的账号 → `/hr-leave/pending` → 看到 1 条 → 批准 → 流程回写 hr_leave.status=APPROVED，余额 usedDays +1
5. **详情** → 任意列表点详情 → 跳 `/hr-leave/detail/:id` → 时间线显示"提交/审批"节点
6. **余额** → `/hr-leave/balances` → 看到 4 个类型卡片 + 进度条
7. **无权限** → 切到无 `hr-leave:leave:list` 权限的账号 → 4 个路由都进不去或显示 403

---

## 11. 风险与未决项

| 风险 | 缓解 |
|------|------|
| `workflow.task.handle` 中 `status: 1=通过 / 2=驳回` 需与后端 `WfTaskStatus` 枚举对齐 | 联调阶段打 `OPTIONS /api/workflow/task/handle` 或查 `wf_tasks` 表注释确认；本设计先按 1/2 假设 |
| `getApprovalChain` 端点路径 `/api/workflow/approval-chain` 在 `workflow.ts` 已有 | 复用，无需新增 |
| `HrLeaveVO.empName / deptName` 后端可能未填充 | 详情页用 `empId` 调 `oa-system` 的 `getUserInfo` 补全（如有需要，本设计先 fallback 显示 empId） |
| 旧 v1 路由 `oa/leave/apply` 仍在 router 中，是否一并隐藏 | **不动 v1 路由**（任务清单 survey 提到 v1/v2 双轨问题，本设计**只增不删**，待全部业务模块 v2 化后统一清理） |
| `useUserStore.empId` 字段不存在（目前 v2 store 暴露的是 `userInfo.id = userInfo.empId`） | 模板中统一用 `userStore.userInfo?.empId` 兜底 |

---

## 12. 与原任务清单 §3 路由权限对照

| 原清单权限 | 实际 meta.permission | 备注 |
|----------|---------------------|------|
| `hr-leave:leave:list` | `hr-leave:leave:list` | 完全一致 |
| `hr-leave:leave:create` | `hr-leave:leave:create` | 完全一致 |
| `hr-leave:leave:approve` | `workflow:task:approve` | **替换**为 workflow 域（更合规） |
| `hr-leave:leave:view` | `hr-leave:leave:view` | 完全一致 |
| （未列）`hr-leave:leave-balance:view` | 同左 | 新增 MyBalances 用 |

> 后端 `HrLeaveController` 中 `RequirePermission("hr-leave:leave:list")` 注解在 `GET /leaves/balances/me` 处也用了 list 权限——本设计将 `MyBalances.vue` 的 meta 改为 `hr-leave:leave-balance:view`（**更细粒度**），但用户有 list 权限时仍能通过（前端不做硬拦截，依靠后端 403 提示）。这是有意的设计，避免前端权限树与后端 `RequirePermission` 字符串耦合过紧。

---

## 附录 A：相关文件路径（绝对路径）

- 后端 Controller：`E:\JavaProject\Corporate_OA_System\code\backend\oa-hr-leave\src\main\java\cn\oa\hr\leave\controller\HrLeaveController.java`
- 后端 Balance Controller：`E:\JavaProject\Corporate_OA_System\code\backend\oa-hr-leave\src\main\java\cn\oa\hr\leave\controller\HrLeaveBalanceController.java`
- 后端 Service：`E:\JavaProject\Corporate_OA_System\code\backend\oa-hr-leave\src\main\java\cn\oa\hr\leave\service\HrLeaveService.java`
- 后端 DTO：`E:\JavaProject\Corporate_OA_System\code\backend\oa-hr-leave\src\main\java\cn\oa\hr\leave\dto\HrLeaveCreateDTO.java`
- 后端 VO：`E:\JavaProject\Corporate_OA_System\code\backend\oa-hr-leave\src\main\java\cn\oa\hr\leave\vo\HrLeaveVO.java`、`HrLeaveBalanceVO.java`
- 现有 v1 API 客户端（待重写）：`E:\JavaProject\Corporate_OA_System\code\frontend\src\api\hr-leave.ts`
- 现有 v1 路由（保留不动）：`E:\JavaProject\Corporate_OA_System\code\frontend\src\router\index.ts`
- 复用 workflow API：`E:\JavaProject\Corporate_OA_System\code\frontend\src\api\workflow.ts`
- 复用 userStore：`E:\JavaProject\Corporate_OA_System\code\frontend\src\store\user.ts`
- 复用 ApprovalTimeline：`E:\JavaProject\Corporate_OA_System\code\frontend\src\components\ApprovalTimeline.vue`
- 登录页（品牌风格参考）：`E:\JavaProject\Corporate_OA_System\code\frontend\src\views\login\index.vue`
- 现有占位 `IndexView.vue`（将被 5 个新页替代，**保留作为 fallback**）：`E:\JavaProject\Corporate_OA_System\code\frontend\src\views\hr-leave\IndexView.vue`
