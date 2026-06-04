# HR 请假审批闭环 — 旧实现影响分析 (T2 v2)

> 日期: 2026-06-04
> 状态: 草案 v2（已对照实际代码校准）
> 关联:
> - T1 契约: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-contract.md`
> - SQL 草案: `code/backend/sql/hr_leave_contract.sql`
> - 审计报告: `.hermes/plans/2026-06-04_184336-spec-audit-report.md`
> - 主 spec §8: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md:415-512`

---

## 0. 关键发现

T2 的实际任务比 spec §8 描述的更复杂，因为**实际代码已经存在**：

1. `oa-hr` 模块已经完成 T3-T5 的核心工作（14 个 Java 文件 + 7 个测试）
2. **旧 `oa-model/cn.oa.hr/` 存在 5 个同名同前缀类**（P0-2 警告）
3. **前端 `hr-leave.ts` 命名用 `hr-` 中划线**，与后端 `/api/hr/*` 路径风格一致（这是好事）
4. **前端 `views/oa/hr/leave-apply/` 已存在**（与旧 `views/oa/leave/apply/` 是平行的两个目录）
5. **SQL 现状 DDL 在 `001_schema.sql`**，不是 spec §7.2 说的"独立 `hr_leave_contract.sql`"
6. **`BaseApprovalServiceImpl` 是请假/出差/外出/采购/报销/加班/借款 7 个业务模块共用的父类**——删之前要先确认其他 6 个模块是否还在用

---

## 1. 后端旧文件清单

### 1.1 实体 (2 个) - 必须保留到 T11

| 文件 | 当前作用 | 引用方 | 处理方式 |
|------|----------|--------|----------|
| `code/backend/oa-model/src/main/java/cn/oa/entity/OaLeaveApply.java` | 映射 `oa_leave_apply`（**注意：实际表是 `hr_leave_apply`**，这个实体可能没有对应表） | (待 T2 期间确认) | T11 期间与 `OaLeaveBalance` 一起判断是否能删 |
| `code/backend/oa-model/src/main/java/cn/oa/entity/OaLeaveBalance.java` | 映射 `oa_leave_balance`（同样表名问题） | (待 T2 期间确认) | 同上 |

**⚠ T2 期间需要确认**: 实际数据库表是 `hr_leave_apply`/`hr_leave_balance`/`hr_leave_rule`，但 `OaLeaveApply`/`OaLeaveBalance` 实体名是 `Oa*` 前缀。**这是历史遗留**：可能旧版用 `oa_*` 表名，重构后改 `hr_*` 表名但旧 Entity 没删。grep 一下 `OaLeaveApply` 引用方。

### 1.2 DTO (2 个) - 必须保留到 T11

| 文件 | 当前作用 | 处理方式 |
|------|----------|----------|
| `code/backend/oa-model/src/main/java/cn/oa/entity/dto/LeaveApplyDTO.java` | 旧申请 DTO | T11 期间确认无引用后删 |
| `code/backend/oa-model/src/main/java/cn/oa/entity/dto/LeaveBalanceInitDTO.java` | 旧余额初始化 DTO | 同上 |

### 1.3 Mapper (2 个) - 必须保留到 T11

| 文件 | 当前作用 | 处理方式 |
|------|----------|----------|
| `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveApplyMapper.java` | `BaseMapper<OaLeaveApply>` | T11 期间确认无引用后删 |
| `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveBalanceMapper.java` | `BaseMapper<OaLeaveBalance>` 含员工/部门 join | 同上 |

### 1.4 Service 接口和实现 (4 个) - 必须保留到 T11

| 文件 | 当前作用 | 处理方式 |
|------|----------|----------|
| `code/backend/oa-service/src/main/java/cn/oa/service/LeaveApplyService.java` | submit/approve/page/updateStatus 接口 | T11 期间与 `LeaveApplyController` 一起确认无引用后删 |
| `code/backend/oa-service/src/main/java/cn/oa/service/LeaveBalanceService.java` | myBalances/init/deduct/restore/compensatory | 同上 |
| `code/backend/oa-service/src/main/java/cn/oa/service/impl/LeaveApplyServiceImpl.java` | 天数计算、提交流程、审批委托、状态回调、余额/考勤联动 | 同上 |
| `code/backend/oa-service/src/main/java/cn/oa/service/impl/LeaveBalanceServiceImpl.java` | 余额初始化、扣减、恢复、调休增加 | 同上 |

### 1.5 Controller (2 个) - 必须保留到 T11

| 文件 | 当前作用 | 处理方式 |
|------|----------|----------|
| `code/backend/oa-web/src/main/java/cn/oa/controller/LeaveApplyController.java` | `/api/leave/submit` `/approve` `/page` `/export` | T11 期间确认前端/移动端无引用后删 |
| `code/backend/oa-web/src/main/java/cn/oa/controller/LeaveBalanceController.java` | `/api/leave-balance/page` `/my` `/init` | 同上 |

### 1.6 旧包同名前缀冲突类 (5 个) - P0-2 修复重点

`code/backend/oa-model/src/main/java/cn/oa/hr/` 下:

| 文件 | 实际类名 | 状态 |
|------|----------|------|
| `HrLeaveRule.java` | (待 T2 确认) | 与 `oa-hr/cn/oa/hr/entity/HrLeaveRule` 同名 |
| `HrLeaveBalance.java` | (待 T2 确认) | 与 `oa-hr/cn/oa/hr/entity/HrLeaveBalance` 同名 |
| `HrTransfer.java` | (待 T2 确认) | 与 `oa-hr/cn/oa/hr/entity/HrTransfer` 同名 |
| `HrAttendance.java` | (待 T2 确认) | 与 `oa-hr/cn/oa/hr/entity/HrAttendance` 同名 |
| `HrEmployeeExt.java` | (待 T2 确认) | 与 `oa-hr/cn/oa/hr/entity/HrEmployeeExt` 同名 |

**T2 必须做的引用分析**:
```bash
grep -rE "import cn\.oa\.hr\.(HrLeaveRule|HrLeaveBalance|HrTransfer|HrAttendance|HrEmployeeExt)" \
  code/backend/ --include="*.java"
```

### 1.7 共用父类 (1 个) - 高风险

| 文件 | 当前作用 | 影响 |
|------|----------|------|
| `code/backend/oa-service/src/main/java/cn/oa/service/impl/BaseApprovalServiceImpl.java` | 7 个业务模块的审批父类（请假/出差/外出/采购/报销/加班/借款） | **删 `LeaveApplyServiceImpl` 前必须确认 `BaseApprovalServiceImpl` 中没有请假专用代码**，否则要先重构父类 |

**T2 必查**: `BaseApprovalServiceImpl` 的方法签名和方法体是否请假专用。

### 1.8 引用旧 `OaLeave*` 的非请假文件 (T2 已识别)

| 文件 | 引用方式 | 处理 |
|------|----------|------|
| `code/backend/oa-service/src/main/java/cn/oa/service/impl/StatisticsServiceImpl.java` | (T2 期间确认) | 报表统计可能用旧实体 |
| `code/backend/oa-service/src/main/java/cn/oa/service/impl/ReportServiceImpl.java` | (T2 期间确认) | 同上 |
| `code/backend/oa-web/src/test/java/cn/oa/controller/LeaveApplyControllerTest.java` | 旧 Controller 测试 | T11 期间删 |
| `code/backend/oa-web/src/test/java/cn/oa/controller/LeaveBalanceControllerTest.java` | 旧 Controller 测试 | T11 期间删 |
| `code/backend/oa-service/src/test/java/cn/oa/service/impl/LeaveApplyServiceImplTest.java` | 旧 Service 测试 | T11 期间删 |

---

## 2. 后端新文件清单（已实现，T3-T5 实际已完成）

### 2.1 Entity (3 个) - 不动

| 文件 | 用途 |
|------|------|
| `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveApply.java` | 映射 `hr_leave_apply` |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveBalance.java` | 映射 `hr_leave_balance` |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveRule.java` | 映射 `hr_leave_rule` |

### 2.2 Mapper (3 个) - 不动

| 文件 | 用途 |
|------|------|
| `code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/HrLeaveApplyMapper.java` | BaseMapper |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/HrLeaveBalanceMapper.java` | BaseMapper |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/HrLeaveRuleMapper.java` | BaseMapper |

### 2.3 Service (7 个: 3 接口 + 3 实现 + 1 callback) - 不动

| 文件 | 用途 |
|------|------|
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/HrLeaveService.java` | 创建/查询/撤回/重提 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/impl/HrLeaveServiceImpl.java` | 实现 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/leave/impl/HrLeaveServiceImpl.java` | **⚠ 重复!** 与上面同包不同子目录，待 T2 确认 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/leave/HrLeaveService.java` | **⚠ 重复!** |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/HrLeaveBalanceService.java` | 余额管理 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/impl/HrLeaveBalanceServiceImpl.java` | 实现 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/HrLeaveRuleService.java` | 规则管理 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/service/impl/HrLeaveRuleServiceImpl.java` | 实现 |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/callback/HrLeaveCallbackHandler.java` | 工作流回调 |

**⚠ 重大发现**: `HrLeaveService`/`HrLeaveServiceImpl` 存在两份：
- `cn.oa.hr.service.HrLeaveService`（旧位置）
- `cn.oa.hr.service.leave.HrLeaveService`（新位置，子包）

这是 P0-2 的同类型问题——包路径重复。**T2 必须确认哪份是 active 引用方，哪份可以删**。

### 2.4 Controller (3 个) - 不动

| 文件 | 路径前缀 |
|------|----------|
| `code/backend/oa-hr/src/main/java/cn/oa/hr/controller/HrLeaveController.java` | `/api/hr/leaves` |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/controller/HrLeaveBalanceController.java` | `/api/hr/leave-balances` |
| `code/backend/oa-hr/src/main/java/cn/oa/hr/controller/HrLeaveRuleController.java` | `/api/hr/leave-rules` |

### 2.5 DTO/VO (7 个) - 不动

| 文件 | 类型 |
|------|------|
| `cn.oa.hr.dto.HrLeaveCreateDTO` | 创建 DTO |
| `cn.oa.hr.dto.HrLeaveSubmitDTO` | **⚠ 存在但未在 Controller 中使用**（待 T2 确认） |
| `cn.oa.hr.dto.HrLeaveQueryDTO` | 查询 DTO |
| `cn.oa.hr.dto.HrLeaveBalanceInitDTO` | 余额初始化 DTO |
| `cn.oa.hr.dto.HrLeaveBalanceAdjustDTO` | 余额调整 DTO |
| `cn.oa.hr.vo.HrLeaveVO` | 申请 VO |
| `cn.oa.hr.vo.HrLeaveBalanceVO` | 余额 VO |
| `cn.oa.hr.vo.HrLeaveRuleVO` | 规则 VO |

### 2.6 Enum (3 个) - 不动

| 文件 | 用途 |
|------|------|
| `cn.oa.hr.enums.HrLeaveType` | 9 个假期类型 |
| `cn.oa.hr.enums.HrLeaveStatus` | 5 个状态（含 `canRevoke()`/`canResubmit()`/`isFinal()`） |
| `cn.oa.hr.enums.HrLeavePeriod` | FULL/AM/PM 含 `getDaysMultiplier()` |

### 2.7 Test (7 个) - 不动，但需要确认是否全跑通

| 文件 | 用途 |
|------|------|
| `cn.oa.hr.service.HrLeaveServiceTest` | Service 单元测试 |
| `cn.oa.hr.service.HrLeaveBalanceSemanticsTest` | 余额语义测试 |
| `cn.oa.hr.enums.HrLeaveEnumsTest` | 枚举测试 |
| `cn.oa.hr.entity.HrLeaveRuleTest` | 规则实体测试 |
| `cn.oa.hr.entity.HrLeaveBalanceTest` | 余额实体测试 |
| `cn.oa.hr.entity.HrLeaveApplyTest` | 申请实体测试 |
| `cn.oa.hr.controller.HrLeaveControllerTest` | Controller 测试 |

**T3 验收命令**:
```bash
cd code/backend && mvn -pl oa-hr -am test -Dtest="HrLeave*Test"
```

---

## 3. 前端文件清单

### 3.1 API 层 (4 个)

| 文件 | 路径风格 | 状态 | 处理方式 |
|------|----------|------|----------|
| `code/frontend/src/api/hr-leave.ts` | 新 (中划线) | ✓ 已存在 | T8 期间扩充 |
| `code/frontend/src/api/leave.ts` | 旧 | 保留期 | T11 期间删 |
| `code/frontend/src/api/leaveBalance.ts` | 旧 | 保留期 | T11 期间删 |
| `code/frontend/src/api/approval-center.ts` | 旧（含 leave 引用） | 保留期 | T8 改造 |
| `code/frontend/src/api/report.ts` | 旧（含 leave 引用） | 保留期 | T8 改造 |

### 3.2 页面层 (5 个)

| 文件 | 路径 | 状态 | 处理方式 |
|------|------|------|----------|
| `code/frontend/src/views/oa/hr/leave-apply/index.vue` | 新 (`oa/hr/leave-apply/`) | ✓ 已存在 | T8 期间对接 `/api/hr/leaves` |
| `code/frontend/src/views/oa/leave/apply/index.vue` | 旧 | 保留期 | T11 期间删 |
| `code/frontend/src/views/oa/leave/approval/index.vue` | 旧 | 保留期 | T11 期间删 |
| `code/frontend/src/views/oa/leave-balance/index.vue` | 旧 | 保留期 | T11 期间删 |
| `code/frontend/src/views/oa/dashboard/index.vue` | 含 leave 引用 | 保留 | T8 期间统一调用 `/api/hr/leaves` |
| `code/frontend/src/views/oa/workbench/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/welcome/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/oa/workflow/mine/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/oa/workflow/definition/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/oa/report/admin/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/oa/report/personal/index.vue` | 含 leave 引用 | 保留 | 同上 |
| `code/frontend/src/views/oa/approval-center/index.vue` | 含 leave 引用 | 保留 | 同上 |

### 3.3 类型/常量/路由/菜单 (5 个)

| 文件 | 用途 | 处理方式 |
|------|------|----------|
| `code/frontend/src/types/api.ts` | 旧 `LeaveApply`/`LeaveBalance` 类型 | T8 期间迁移到 `HrLeave*` |
| `code/frontend/src/utils/constants.ts` | 旧假期类型/状态枚举 | T8 期间同步 |
| `code/frontend/src/utils/format.ts` | 含 leave 格式化 | T8 期间保留 |
| `code/frontend/src/layout/menuConfig.ts` | 旧菜单路径 | T8 期间指向 `oa/hr/leave-apply` |
| `code/frontend/src/router/index.ts` | 旧路由 | T8 期间更新 |
| `code/frontend/src/auto-imports.d.ts` | 自动导入 | 自动生成，不动 |

---

## 4. Mobile 文件清单 (8 个)

| 文件 | 用途 | 处理方式 |
|------|------|----------|
| `code/mobile/src/api/leave.ts` | 旧 API | T9 期间改造 |
| `code/mobile/src/api/report.ts` | 含 leave 引用 | T9 期间 |
| `code/mobile/src/pages/oa/leave-apply.vue` | 旧申请页 | T9 期间重写为 `HrLeave*` |
| `code/mobile/src/pages/oa/leave-list.vue` | 旧列表 | T9 期间重写 |
| `code/mobile/src/utils/constants.ts` | 旧枚举 | T9 期间同步 |
| `code/mobile/src/pages.json` | 路由配置 | T9 期间更新 |
| `code/mobile/src/pages/mine/index.vue` | 含 leave 引用 | T9 期间 |
| `code/mobile/src/pages/home/index.vue` | 含 leave 引用 | T9 期间 |

---

## 5. 数据库

| 文件 | 内容 | 处理方式 |
|------|------|----------|
| `code/backend/sql/baseline/001_schema.sql` | 3 张 `hr_leave_*` 表 DDL（line 1015-1098） | T3 期间按 `hr_leave_contract.sql` 草案校准 |
| `code/backend/sql/hr_leave_contract.sql` | T1 草案 DDL v2 | T3 期间合并到 `001_schema.sql` |
| `code/backend/sql/hr_leave_seed.sql` | T1 seed 草案 9 条规则 | T3 期间追加到 baseline |

**没有 `hr_leave_seed.sql` 的旧版本**——这是新增。

---

## 6. 新旧接口映射（更新版）

| 旧接口 | 新接口 | 迁移说明 |
|--------|--------|----------|
| `POST /api/leave/submit` | `POST /api/hr/leaves` | 创建并提交 |
| `GET /api/leave/page` | `GET /api/hr/leaves` | 分页查询 |
| `POST /api/leave/approve` | `POST /api/wf/tasks/{taskId}/actions/approve` | 审批走工作流 |
| `GET /api/leave/{id}` | `GET /api/hr/leaves/{id}` | 详情 |
| `POST /api/leave/revoke` | `POST /api/hr/leaves/{id}/actions/revoke` | 撤回 |
| `GET /api/leave/export` | 暂缓迁移 | T8 保留旧导出 |
| `GET /api/leave-balance/my` | `GET /api/hr/leaves/my-balances` | 注意: 新接口挂在 `/leaves` 下，不是 `/leave-balances` |
| `GET /api/leave-balance/page` | `GET /api/hr/leave-balances` | 管理端余额 |
| `POST /api/leave-balance/init` | `POST /api/hr/leave-balances/actions/init` | 初始化 |
| `PUT /api/leave-balance/{id}` | `PUT /api/hr/leave-balances/{id}` | 调整 |

---

## 7. 字段映射（更新版）

| 旧字段 | 新字段 | 迁移说明 |
|--------|--------|----------|
| `oa_leave_apply.id` | `hr_leave_apply.id` | 主键，新库从 1 开始 |
| 无 | `hr_leave_apply.apply_no` | 申请单号 (LVyyyyMMddHHmmssXXXX) |
| `emp_id` | `emp_id` | 保留 |
| 无 | `dept_id` | 新增冗余字段 |
| 数字 1/2/3 | `leave_type` 字符串枚举 | 9 个值 (PERSONAL/ANNUAL/SICK/...) |
| `start_time`/`end_time` | 同名 | 保留 |
| `leavePeriod=full/morning/afternoon` | `leave_period=FULL/AM/PM` | 新枚举 |
| `days` | `days` | 类型 `DECIMAL(6,1)` (旧 4,1) |
| `status=0/1/2/3` | `status=DRAFT/RUNNING/PASSED/REJECTED/REVOKED` | 新枚举 |
| `process_instance_id` | 同名 | 保留 |
| 无 | `current_task_id` | 新增 |
| 无 | `approved_time` | 新增 |
| 无 | `reject_reason` | 新增 |

**平衡字段差异**:
| 旧 | 新 |
|----|----|
| `total_days` | `total_days` |
| `used_days` | `used_days` |
| 无 | `frozen_days` (审批中冻结) |
| 无 | `remaining_days` (可写，业务层维护，**不**用 GENERATED 列) |
| 无 | `availableDays` (非库字段，业务层计算) |
| 无 | `expire_date` (DATE) |
| 无 | `status` (ACTIVE/INACTIVE) |

**规则字段差异**:
| 旧 DDL | 新 DDL | 原因 |
|--------|--------|------|
| `min_days` | `minUnit` | 命名调整 |
| `max_days` | `maxDaysPerApply` | 命名调整 |
| `need_attachment` (CHAR) | `requireAttachment` (TINYINT) | 类型+命名调整 |
| 无 | `deduct_salary` | 新增 |
| `unit` (VARCHAR) | 删 | Entity 无此字段 |
| `max_consecutive` | 删 | Entity 无此字段 |
| `allow_half_day` | 删 | Entity 无此字段 |
| `gender_restrict` | 删 | Entity 无此字段 |
| `sort_order` | 删 | Entity 无此字段 |
| `status` (CHAR '0'/'1') | `status` (VARCHAR 'ACTIVE'/'INACTIVE') | 枚举字符串化 |

---

## 8. 保留、迁移、下线策略

### 8.1 阶段策略

| 阶段 | 策略 |
|------|------|
| T1-T2 | 旧 `OaLeave*` 全部保留，新 `HrLeave*` 已在 `oa-hr` 模块实现 |
| T3 | 合并 SQL 草案到 `001_schema.sql`，按校准后字段修正 |
| T4 | 验证 `oa-hr` 模块测试通过 |
| T5 | 验证 Controller API 路径/权限码/响应符合契约 |
| T6 | 接入 `WfCallbackDispatcher` → `HrLeaveCallbackHandler` |
| T7 | 接入待办/消息 |
| T8 | 前端 `hr-leave.ts` 完整对接 `/api/hr/leaves`，旧 API 标 deprecated |
| T9 | 移动端同步 |
| T10 | 端到端测试 |
| T11 | 旧代码下线 |

### 8.2 T11 删除清单（按风险从低到高）

| 删除顺序 | 文件 | 风险 | 前提 |
|----------|------|------|------|
| 1 | `cn.oa.hr.HrLeaveRule`/`HrLeaveBalance`/`HrTransfer`/`HrAttendance`/`HrEmployeeExt` (5 个) | **中**：要先确认无引用 | 引用分析显示仅 0 处 |
| 2 | `cn.oa.entity.OaLeaveApply`/`OaLeaveBalance` (2 个) | 中 | 同上 |
| 3 | `cn.oa.entity.dto.LeaveApplyDTO`/`LeaveBalanceInitDTO` (2 个) | 低 | 同上 |
| 4 | `cn.oa.mapper.OaLeaveApplyMapper`/`OaLeaveBalanceMapper` (2 个) | 低 | 同上 |
| 5 | `cn.oa.service.LeaveApplyService`/`LeaveBalanceService` + impl (4 个) | **高**：要拆 `BaseApprovalServiceImpl` | 父类已重构 |
| 6 | `cn.oa.controller.LeaveApplyController`/`LeaveBalanceController` (2 个) | 高 | 前端已切换 |
| 7 | 前端 `leave.ts`/`leaveBalance.ts` | 低 | 新 API 切换完 |
| 8 | 前端 `views/oa/leave/*` (3 个) | 中 | 路由/菜单已切 |
| 9 | Mobile `api/leave.ts` + `pages/oa/leave-*.vue` | 低 | 移动端已切 |

### 8.3 不允许删的硬规则（spec §8.5）

下列文件 T11 之前**绝对不能删**：
1. `OaLeaveApply`、`OaLeaveBalance` 实体
2. `OaLeaveApplyMapper`、`OaLeaveBalanceMapper`
3. `LeaveApplyServiceImpl`、`LeaveBalanceServiceImpl`
4. `LeaveApplyController`、`LeaveBalanceController`
5. 旧 Web/Mobile API 文件
6. `cn.oa.hr.Hr*` 5 个旧包同名前缀类（要先做引用分析再决定，**不能盲目删**）

---

## 9. 风险点

| 风险 | 影响 | 缓解 |
|------|------|------|
| 281 个 uncommitted changes 中可能影响请假代码 | T3+ 编辑冲突 | T1/T2 完成后 `git status` 一次 |
| `cn.oa.hr.service.leave.HrLeaveService` 与 `cn.oa.hr.service.HrLeaveService` 两份 | 编译二义性、行为不一致 | T2 期间 grep `@Autowired`/`@Resource` 决定哪份 active |
| `BaseApprovalServiceImpl` 是 7 个业务模块共用 | 删 `LeaveApplyServiceImpl` 前要看父类 | T2 必查 |
| `HrLeaveSubmitDTO` 在 Controller 没引用 | 死代码 | T2 必查引用方 |
| 旧 `Oa*` 实体在 `StatisticsServiceImpl`/`ReportServiceImpl` 引用 | 删前要先迁报表 | T2 必查 |
| SQL DDL 与 Entity 字段不一致（见 T1 §3 变更说明） | 运行时报错 | T3 期间修复 `001_schema.sql` |
| `hr_leave_balance.remaining_days` 是 GENERATED 列但 Entity 写为普通字段 | MyBatis-Plus 插入报错 | T3 期间 SQL 改为普通列 |
| 字符集 `utf8mb4_general_ci` vs spec 要求 `utf8mb4_unicode_ci` | 排序/比较结果差异 | T3 期间统一 |

---

## 10. T2 完成判据

- [x] 旧实体 2 个 + 旧 DTO 2 个 + 旧 Mapper 2 个 + 旧 Service 4 个 + 旧 Controller 2 个 列全
- [x] 新模块 14 个文件列全
- [x] 旧包同名前缀 5 个类识别
- [x] 共用父类 1 个识别
- [x] 前端 5 个页面 + 4 个 API + 5 个辅助文件
- [x] Mobile 8 个文件
- [x] 新旧接口映射 + 字段映射
- [x] 保留/迁移/下线策略
- [x] 风险点 8 个
- [ ] **用户拍板**: T2 是否进入"细化引用分析"模式（执行 §1.6/§1.7/§1.8 的 grep）
- [ ] **用户拍板**: §2.3 重复的 `HrLeaveService` 哪份保留
- [ ] **用户拍板**: §2.5 死代码 `HrLeaveSubmitDTO` 是否删除
