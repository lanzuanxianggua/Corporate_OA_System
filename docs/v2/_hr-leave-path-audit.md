# hr-leave 路径错配 + 后端 unused 端点 - 摸底清单

**日期**: 2026-06-08
**上下文**: 前端 hr-leave.ts 调的是 `/api/v1/hr-leave/leaves/*` 风格,后端 `HrLeaveController` 实际是 `/api/leave/*` 短路径 → **前端调不通后端**

---

## 一、前端调用 vs 后端真实端点(完整对照表)

### HrLeaveController (后端 `@RequestMapping("/api/leave")`)

| 后端端点 | 权限码 | 用途 | 前端调用方 | 错配? |
|---|---|---|---|---|
| `POST /api/leave` 或 `/api/leave/submit` | `hr-leave:leave:create` | 提交请假 | 前端 `POST /api/v1/hr-leave/leaves` | **❌ 错配** |
| `POST /api/leave/{id}/actions/revoke` 或 `/api/leave/revoke/{id}` | `hr-leave:leave:create` | 撤回请假 | 前端 `POST /api/v1/hr-leave/leaves/{id}/actions/revoke` | **❌ 错配** |
| `GET /api/leave/page` 或 `/api/leave/mine` | `hr-leave:leave:list` | 我的请假列表 | 前端 `GET /api/v1/hr-leave/leaves/mine` | **❌ 错配** |
| `GET /api/leave/{id}` | `hr-leave:leave:list` | 详情 | 前端 `GET /api/v1/hr-leave/leaves/{id}` | **❌ 错配** |
| `GET /api/leave/balances/me` | `hr-leave:leave:list` | 我的余额 (返回 Map) | 前端 `GET /api/v1/hr-leave/balances/me` | **❌ 错配** |
| `POST /api/leave/approve` | (无权限注解) | 请假审批 | 前端 `POST /api/leave/approve` (旧 leave.ts → 迁到 hr-leave.ts) | ✅ 正确 |

### HrLeaveBalanceController (后端 `@RequestMapping("/api/leave-balance")`)

| 后端端点 | 权限码 | 用途 | 前端调用方 | 错配? |
|---|---|---|---|---|
| `GET /api/leave-balance/my` | `hr-leave:leave-balance:view` | 我的余额 (返回 List) | **0 前端引用** | ✅ 无调用方 |
| `GET /api/leave-balance/page` | `hr-leave:leave-balance:list` | 余额分页 | **0 前端引用** | ✅ 无调用方 |
| `POST /api/leave-balance/init` | `hr-leave:leave-balance:init` | 初始化余额 (admin) | **0 前端引用** | ✅ 无调用方 |
| `POST /api/leave-balance/{id}/adjustments` | `hr-leave:leave-balance:adjust` | 调整余额 (admin) | **0 前端引用** | ✅ 无调用方 |

### 总结

- **5 个端点路径错配**(前端 v1 风格,后端短路径) → **前端所有 hr-leave 页面现在都打不通后端**
- **4 个后端端点 0 前端引用**:`HrLeaveBalanceController` 整个 controller 0 调用方
- **1 个端点对得上**:`/api/leave/approve` (走的是我刚迁过去的兼容函数)

---

## 二、推荐修复方案(等你确认)

### 方案 1(推荐,只改前端路径,零后端改动)
- `hr-leave.ts` 5 个 v1 路径全部改为后端短路径
  - `/api/v1/hr-leave/leaves/mine` → `/api/leave/mine` (或 `/api/leave/page`)
  - `/api/v1/hr-leave/leaves/{id}` → `/api/leave/{id}`
  - `/api/v1/hr-leave/leaves` (POST) → `/api/leave/submit`
  - `/api/v1/hr-leave/leaves/{id}/actions/revoke` → `/api/leave/{id}/actions/revoke`
  - `/api/v1/hr-leave/balances/me` → `/api/leave/balances/me`
- 后端 0 改动
- 前端 5 个页面恢复可用
- 风险:低,纯字符串替换

### 方案 2(进阶,改前端路径 + 删 unused 后端)
- 方案 1 的所有改动
- 删 `HrLeaveBalanceController` 4 个端点中**前端 0 引用**的部分(但 controller 本身要保留,因为可能 admin 后台调)
- 删未使用 controller 类(整个 `HrLeaveBalanceController.java`)—— **需先确认 admin 端无引用**
- 风险:中,需要确认 admin 端是否真无引用

### 方案 3(更激进,统一迁到 v1 路径)
- 后端加 v1 路由,`@RequestMapping` 改成 `/api/v1/hr-leave/leaves` 等
- 前端 0 改动
- 风险:高,后端路径风格化,需要 admin 端 + 所有现有调用方同步

---

## 三、关于 `HrLeaveBalanceController` 4 个端点

需要再细查:
1. 是否被**后端**其他 service 调用(如 `HrLeaveService.submit()` 内部是否调 `initBalance`)?  让我看:
2. 是否被 admin 后台(若有)调用?
3. Flyway 迁移脚本 `V930__hr_leaves.sql` 是否初始化了数据(用 init 端点)?

**这部分需要额外摸底,等用户回复方案 1/2/3 后再展开**

---

## 四、等用户回复"方案 1 / 2 / 3"
