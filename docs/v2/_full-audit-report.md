# 全项目体检报告 (P0/P1/P2 排序)

**日期**: 2026-06-08
**范围**: `code/backend/` (18 Maven 模块) + `code/frontend/src/` (Vue 3 + TS)
**方式**: mvn compile + grep 全量扫描 + 类型检查
**目的**: 摸清项目"上线前/发版前"还剩多少问题,按严重度排序

---

## 体检范围 & 方法

| 维度 | 方法 | 结果 |
|---|---|---|
| 后端编译 | `mvn -DskipTests compile` | **❌ 失败 1 个模块** |
| Flyway 迁移脚本 | 扫 38 个 V9xx 文件 | ✅ 0 重复版本号 |
| 权限码对齐 | grep 161 个 `@RequirePermission` vs DB 105 个 | **❌ 56 个未注册** |
| Mapper `@Param` | 扫 78 个 Mapper | ✅ 71/78 合规 (其余单参) |
| 异常抛类 | 扫 `throw new RuntimeException` | ✅ 0 业务代码 |
| 前端 API 路径 | 144 个 vs 后端 52 个 | **❌ 128 个无对应** |
| 前端 type-check | `pnpm type-check` | ✅ 0 错误 |

---

## P0 - 阻塞性问题(必须先修,否则不能发布)

### P0-1 ❌ `oa-hr-attendance` 模块编译失败

**症状**:
```
[ERROR] HrAttendanceRecordService.java:[27,29] 找不到方法 getClockTime()
[ERROR] HrAttendanceRecordService.java:[28,31] 找不到方法 getMethod()
[ERROR] HrAttendanceGroupService.java:[17,95] 找不到方法 getStatus()
```

**根因**:
- `HrAttendanceRecordCreateDTO` 只有 `clockDate` 字段,但 `HrAttendanceRecordService.clockIn` 调 `dto.getClockTime()` 和 `dto.getMethod()`
- `HrAttendanceGroup` entity 是**空文件 (9 行,0 字段)**,但 `HrAttendanceGroupService.list()` 调 `HrAttendanceGroup::getStatus`

**影响**:
- **整个项目无法 mvn compile / package**
- **整个项目无法启动**(任何 `spring-boot:run` 都会失败)
- 之前 git history 显示有 `M` 标记在 attendance 相关文件,但**没人验证能编译**

**修复方向**(需要你确认):
- 选项 A: 在 DTO 上加 `clockTime` + `method` 字段,entity 加 `status` 字段
- 选项 B: 改 Service 用现有字段(如 `setClockInTime(LocalDateTime.now())` 直接用,entity 用 `name` 替代 `status`)
- 选哪个需要看后端 service 的业务语义,**不**能凭报告改

### P0-2 ❌ 56 个 `@RequirePermission` 权限码未在 DB 注册

**症状**:
- 161 个 `@RequirePermission("xxx:yyy:zzz")` 注解
- DB 迁移脚本中**只定义了 105 个** perm_code
- 缺 56 个,涉及:admin/asset-loan/asset-loan-*/seal-apply/hr-employee/hr-training 等多个模块

**影响**:
- `PermissionInterceptor` 不阻断(只校验 JWT 是否有这个 perm),但**admin 用户的 JWT 里不会有这些 perm** → 任何用户访问这些端点会 403
- 即 `oa-hr-attendance` 编译过了,运行时这些端点也对所有人 403

**修复**:
- 在 V992 或新建 V993 添加缺失的 perm_code 注册,并 `INSERT INTO sys_role_permission` 绑给 SUPER_ADMIN
- 涉及 56 个 perm,可能需要在 V9xx 末尾追加,需要你确认范围

### P0-3 ❌ 6 个前端 downloadFile export API 后端不存在

**症状**:
- 6 个 vue 文件调 `downloadFile("/api/{business-trip,expense,loan,outing,overtime,purchase}/export")`
- 后端 0 controller 提供这些端点
- grep `export` 整个 `oa-finance` `oa-hr-leave` 等模块无果

**影响**:
- 用户在 6 个业务申请列表页点"导出"按钮 → 404
- 体验性 bug,不影响主流程但用户一定踩到

**修复**:
- 选项 A: 前端删"导出"按钮(各 -1 行)
- 选项 B: 后端补 6 个 export 端点(返回 Excel 文件流)
- 选哪个需要业务侧定

---

## P1 - 严重问题(影响核心功能但非阻塞)

### P1-1 ⚠️ 前端 API 路径风格不统一,与后端 controller 错配

**症状**(已 grep 全量验证):
- 后端 18 个业务模块 controller 一律用 `/api/{module-plural}/{resource-plural}` 风格,如:
  - `HrLeaveController` @ `/api/leave`
  - `HrAttendanceRecordController` @ `/api/hr-attendance/records`
  - `FinBudgetController` @ `/api/finance/budgets`
  - `DocDispatchController` @ `/api/document/dispatches`
- 前端 API 客户端和 vue 文件**混用 3 种风格**:
  - 短路径 `/api/leave/...` (部分正确,如 hr-leave)
  - 模块前缀 `/api/hr-attendance/...` (与后端一致)
  - 旧版错配 `/api/attendance/...` `/api/hr-attendance/...` 都有,后者正确

**实测有 128/144 前端硬编码路径与后端 controller 路径不对得上**——其中 100+ 是风格不统一,**不是后端缺端点**。

**影响**:
- 用户访问业务页面大量 404
- `hr-leave` 模块我刚修了 (commit 544a223),**还有 17 个业务模块的 API 客户端没修**

**修复**:
- 17 个 API 客户端逐一核对
- 估时 4-6h

### P1-2 ⚠️ `oa-hr-attendance` 之前 `mvn install` 没跑过

**症状**:
- git status 显示大量 `M` 修改在 attendance 相关的 controller/dto/service
- 但 `mvn compile` 失败 → 说明这些 `M` 修改**从来没编译过**
- 你的工作流(CLAUDE.md)写:"改 entity/mapper 后每次启动前都要 install",但 attendance 模块**根本没 install 成功过**

**影响**:
- 你以为 attendance 模块在跑,实际**从没跑过**
- `oa-hr-attendance` 端到端从未经过验证

**修复**:
- 修 P0-1 → install → 启动 → smoke test 验证

---

## P2 - 中等问题(可继续优化)

### P2-1 路由 `oa/leave-balance` 已下线但前端 `oa/leave-balance` 路由元信息之前在 meta.ts
**说明**: 我 P0-1 (commit 405c8d5) 已删,确认

### P2-2 前端 `views/auth/LoginView.vue` 没用上
**说明**: router 没引用,但 `views/login/index.vue` 是真的。需要确认 LoginView 是否真死代码

### P2-3 前端 60+ `M` 标记的后端文件未提交
**说明**: 这些是用户工作区私货,本报告不展开

### P2-4 前端 65 个 untracked 文件(v2 收口期产物)
**说明**: supply/contract/recruitment/training/performance + Flyway 补丁 + 前端 7 个 view + E2E 脚手架,未 commit

---

## 后端 `oa-hr-attendance` 编译失败 - 详细分析

| 文件 | 行 | 调用的不存在方法 | 该字段应当的位置 |
|---|---|---|---|
| `HrAttendanceRecordService.java` | 27,29 | `dto.getClockTime()` | 应在 `HrAttendanceRecordCreateDTO` 加 `private LocalDateTime clockTime;` |
| `HrAttendanceRecordService.java` | 28,31 | `dto.getMethod()` | 应在 `HrAttendanceRecordCreateDTO` 加 `private String method;` |
| `HrAttendanceGroupService.java` | 17,95 | `HrAttendanceGroup::getStatus` | 应在 `HrAttendanceGroup` entity 加字段 + 整个 entity 重建(目前空文件) |

**Entity `HrAttendanceGroup` 当前内容(9 行,完全空)**:
```java
package cn.oa.hr.attendance.entity;
import ...;
@TableName("hr_attendance_group")
public class HrAttendanceGroup extends BaseEntity {
    // 0 字段!
}
```

**这意味着 v1→v2 重构时,有人删了字段但忘了改 service 调用**。修复需要看 git blame + 后端 service 业务语义。

---

## 等你决策(按优先级)

| 序号 | 任务 | 估时 | 风险 |
|---|---|---|---|
| **A** | 修 P0-1 (attendance 编译失败) | 1-2h | 中(需懂业务语义) |
| **B** | 修 P0-2 (56 个 perm 注册) | 1h | 低(纯 SQL) |
| **C** | 修 P0-3 (6 个 export 端点) | 决策 | 中(后端补 6 个端点 or 前端删按钮) |
| **D** | 修 P1-1 (17 个 API 客户端对齐) | 4-6h | 低(字符串替换) |
| **E** | 修 P1-2 (install + smoke test) | 30min | 低 |
| F | P2 几项 | 1h | 低 |

请回复:
- "做 A" / "做 ABC" / "做全部" / "只先做 A" / "列出 A 的修复方案让我选"
- P0-1 的 DTO 修复方向(A 加字段 vs B 改 Service 调现有字段)
- P0-3 的方向(后端补端点 vs 前端删按钮)
