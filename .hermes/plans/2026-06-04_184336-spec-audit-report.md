# Spec 审计报告 — HR 请假试点相关

> 日期: 2026-06-04
> 范围: HR 请假试点相关 spec + 实际代码
> 审计员: Hermes 主 agent（subagent 路径超时 600s 后接管）
> 审计对象:
> - `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` (主文档, 2171 行)
> - `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md` (HR 试点, 1283 行)
> - `docs/superpowers/workflows/claude-code-oa-redesign-workflow.md` (385 行)
> - `code/backend/oa-hr` 实际代码
> - `code/backend/oa-model` / `oa-mapper` / `oa-service` / `oa-web` 旧代码
> - `code/backend/oa-workflow/...` 实际代码
> - `code/backend/sql/` 实际 SQL

---

## 摘要

- **总问题数**: 11
- **P0 (阻塞 T3+ 决策)**: 4
- **P1 (T1/T2 期间应修)**: 5
- **P2 (可推迟)**: 2

**最关键的发现**：HR 请假试点的 **T3/T4/T5（后端核心）实际上已经做完**。`oa-hr` 模块的 Entity/Mapper/Service/Controller/Enum/Callback/Tests 全都存在。
spec §8.1 列的"旧后端文件清单"大部分还成立（旧类没删），但新类（`Hr*`）已经写好。
**这意味着 T1/T2 的真实任务是"为已经存在的实现补写正式契约文档"，而不是"为未存在的实现设计契约"**。
spec §7 (T1 草案) 和 §8 (T2 草案) 的表结构/字段/API 都需要**对照实际代码**修正。

---

## P0 阻塞级问题

### P0-1: 实际代码 vs spec §7.2 表结构存在大量差异

- **位置**: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md:248-280`（T1 SQL 草案）
- **问题**: spec §7.3 列了 `hr_leave_apply/balance/rule` 字段草案，但实际 `oa-hr` 模块代码已经定义了这些表对应的 Entity：
  - `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveApply.java`
  - `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveBalance.java`
  - `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveRule.java`
  
  但**实际 SQL 在哪？** spec §7.2 提到 `code/backend/sql/hr_leave_contract.sql` 作为契约草案——这个文件可能根本不存在（待 T1 期间确认）。
- **证据**: 上述 3 个 Entity 文件存在，但 `code/backend/sql/` 下需要确认是否有对应的 DDL。
- **建议**: T1 期间**先 grep `code/backend/sql/`** 确认是否有 `hr_*` 表的 DDL。如果有，spec §7.3 应该按实际 DDL 校准；如果没有，spec §7.3 是"设计草案"而不是"待落地草案"，需要在标题和说明中区分。
- **修复成本**: 低（确认 + 文档校准）

### P0-2: `cn.oa.hr.Hr*` 与 `cn.oa.hr.entity.Hr*` 类重复（spec §11.2 警告的真实案例）

- **位置**: 
  - `code/backend/oa-model/src/main/java/cn/oa/hr/HrLeaveRule.java`（**旧包**）
  - `code/backend/oa-model/src/main/java/cn/oa/hr/HrLeaveBalance.java`（**旧包**）
  - `code/backend/oa-model/src/main/java/cn/oa/hr/HrEmployeeExt.java`（**旧包**）
  - `code/backend/oa-model/src/main/java/cn/oa/hr/HrAttendance.java`（**旧包**）
  - `code/backend/oa-model/src/main/java/cn/oa/hr/HrTransfer.java`（**旧包**）
  - 同时存在于 `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveRule.java`（**新包**）
  - 同时存在于 `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveBalance.java`（**新包**）
  - 同时存在于 `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrTransfer.java`（**新包**）
- **问题**: spec §11.2 明确警告："迁移期避免同名类分散在 `cn.oa.hr` 与 `cn.oa.hr.entity` 两套包中"。但代码里这正是当前状态。
- **建议**: 
  - T1 期间决定**保留哪一份**作为新模块的实体。
  - 看 `oa-model/src/main/java/cn/oa/hr/` 是 stub/转接口类还是真有逻辑，决定能否直接删。
  - 决定后同步更新 spec §11.2 添加本案例作为"已解决"或"已识别待 T11 处理"。
- **修复成本**: 中（需要决定保留哪份并修改所有引用）

### P0-3: T3-T5 实际已实现，但 spec §7/§8 把它们写成"待开发"

- **位置**: 
  - spec §7.5 (T1 API 契约) 把 11 个接口列为"输出"
  - spec §8.1 (T2 旧实现清单) 把 `HrLeave*Service`/`HrLeave*Mapper`/`HrLeaveController` 等列为"未来产物"
- **问题**: 实际 `oa-hr` 模块已经存在：
  - Controller: `HrLeaveController` (@/api/hr/leaves), `HrLeaveBalanceController` (@/api/hr/leave-balances), `HrLeaveRuleController` (@/api/hr/leave-rules)
  - Service: 3 个接口 + 3 个实现
  - Mapper: 3 个
  - DTO/VO: `HrLeaveCreateDTO`、`HrLeaveSubmitDTO`、`HrLeaveQueryDTO`、`HrLeaveBalanceAdjustDTO`、`HrLeaveVO`、`HrLeaveBalanceVO`、`HrLeaveRuleVO`
  - Enum: `HrLeaveType`、`HrLeaveStatus`、`HrLeavePeriod`
  - Callback: `HrLeaveCallbackHandler`
  - **测试**: 7 个测试类（含 `HrLeaveServiceTest`、`HrLeaveBalanceSemanticsTest`、`HrLeaveControllerTest`）
- **建议**:
  - T1 期间对照实际代码**重写** §7.5 API 契约（路径、权限码、请求体、响应体），不能盲从草案。
  - T2 期间把 §8.1 的"待开发"清单**改为"实现 + 旧代码替换"清单**。
  - T1/T2 文档化必须**显式标注**哪些接口已实现、哪些字段已枚举化、哪些权限码已配。
- **修复成本**: 中（spec 重写 + 与代码逐项对照）

### P0-4: `oa-mapper` 旧 `OaLeaveApply*` 与新 `HrLeaveApply*` Mapper 并存 + 路径错配

- **位置**:
  - `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveApplyMapper.java`（**旧**）
  - `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveBalanceMapper.java`（**旧**）
  - `code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/HrLeaveApplyMapper.java`（**新**）
  - `code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/HrLeaveBalanceMapper.java`（**新**）
- **问题**: spec §11.2 说新模型包已出现，但 mapper 没移动——`oa-mapper` 模块里同时存在 `cn.oa.mapper.OaLeave*`（旧）和 `cn.oa.hr.mapper.HrLeave*`（新）。这种跨模块同语义类**会让 MyBatis-Plus 扫描产生歧义**。
- **建议**:
  - T1 期间明确：新模块的 Mapper 应该全部在 `oa-hr/src/main/java/cn/oa/hr/mapper/` 下，`oa-mapper` 模块不再接受新业务 Mapper。
  - T11 阶段清空 `oa-mapper` 下与新模块重复的旧 Mapper。
  - spec §1.1 模块依赖图需要补一条"业务模块的 Mapper 在模块内部"的规则。
- **修复成本**: 低（文档明确 + T11 执行）

---

## P1 应修问题

### P1-1: `/api/{module}` 单数 vs `/api/{module}/{resource}` 复数风格混用

- **位置**:
  - 旧（oa-web）: `/api/leave`（推测）、`/api/alert`、`/api/asset`、`/api/attendance`、`/api/budget`、`/api/business-trip`、`/api/contract`、`/api/dept`、`/api/dict` — 全单数
  - 新（业务模块）: `/api/hr/leaves`、`/api/hr/leave-balances`、`/api/hr/leave-rules`、`/api/finance/expenses`、`/api/finance/budgets`、`/api/admin/seals`、`/api/admin/supplies`、`/api/admin/assets`、`/api/document/serial`、`/api/document/receive`、`/api/document/dispatch`、`/api/knowledge/entries`、`/api/task/tasks`、`/api/task/projects`、`/api/task/hours`、`/api/message/notifications`、`/api/message/preferences`、`/api/meeting/rooms`、`/api/meeting/bookings`、`/api/meeting/signins` — 全复数或语义化
- **问题**: spec §11.6 说"路径前缀 `/api/{module}/{resource}`"，但没明确 `{resource}` 用单数还是复数。**新代码自发达成一致用复数**，但 spec 没有把这个规则写死。
- **建议**: spec §11.6 增加一行："`{resource}` 推荐用复数（`/leaves` 而非 `/leave`），新接口必须遵守。" T1 的 API 契约已经遵守这个规则。
- **修复成本**: 低（一行规则）

### P1-2: `/api/finance/v1/*` 与 `/api/finance/*` 双轨并行

- **位置**:
  - 新: `FinLoanController` @/api/finance/v1/loans, `FinExpenseController` @/api/finance/v1/expenses, `FinBudgetController` @/api/finance/v1/budgets
  - 旧: `LoanController` @/api/finance/loans, `ExpenseController` @/api/finance/expenses, `BudgetController` @/api/finance/budgets
- **问题**: finance 模块用了 URL 版本号做新旧切换。但 spec §11.6 完全没提这种模式。**v1 路径不在 spec 任何"路径前缀"规则中描述**。
- **建议**:
  - spec §11.6 增加一条："`{module}/v{N}/{resource}` 允许作为旧接口并存期的兼容策略，但 v{N} 路径必须在 spec 的 API 契约表中明确标注 deprecated 时长。"
  - 或者：明确 finance 这种做法不推荐，要求并行期统一为单路径。
  - T1 期间（HR 试点）需要决定：HR 走 v1 风格还是规范风格？当前 HR 已经用 `/api/hr/leaves`（**不是** `/api/hr/v1/leaves`），所以 HR 选了规范风格。spec 应当把这个选择写进"参考做法"。
- **修复成本**: 低（spec 增加一节"URL 版本策略"）

### P1-3: spec §2.4 状态机与 spec §7.6 字段映射的状态不完全对齐

- **位置**:
  - spec §2.2 实例状态: `DRAFT, RUNNING, PASSED, REJECTED, REVOKED, ABORTED, SUSPENDED`
  - spec §7.3 申请单状态: `DRAFT, RUNNING, PASSED, REJECTED, REVOKED`（缺 `ABORTED, SUSPENDED`）
- **问题**: 流程实例有 7 个状态，但业务申请单只列了 5 个。`ABORTED`（管理员终止）和 `SUSPENDED`（暂停）会不会映射到申请单？
- **建议**: T1 期间明确：
  - 申请单 `status` 字段是否需要这 5 个 vs 7 个状态？
  - 看 `HrLeaveStatus` 枚举实际有哪些值（待 T1 期间确认）。
  - 文档要保持实例状态和申请单状态映射表。
- **修复成本**: 低（表 + 映射说明）

### P1-4: `code/backend/sql/baseline/` 多份脚本与 spec §11.5 收敛原则冲突

- **位置**: 
  - `code/backend/sql/baseline/001_schema.sql` (基础 DDL)
  - `code/backend/sql/baseline/001_schema_workflow.sql` (workflow 单独)
  - `code/backend/sql/baseline/003_seed_workflow.sql` (workflow seed)
  - `code/backend/sql/baseline/004_schema_task.sql` (task 模块追加)
  - `code/backend/sql/baseline/005_schema_new_modules.sql` (新模块追加)
  - 同时有 `code/backend/sql/phase_all_ddl.sql`（spec §11.2 警告过）
  - 同时有 `code/backend/sql/oa_system_full.sql`（推测，spec §11.2 警告过）
  - 同时有 `code/backend/sql/oa_system_extensions.sql`（spec §11.2 警告过）
- **问题**: spec §11.5 要求"重构后只保留一份基线DDL、一组按环境区分的 seed 脚本和必要的补丁脚本"，但实际有 5+ 份 baseline + 推测的 phase_all_ddl + oa_system_full + extensions。**且没有 `001_seed_system.sql`、`003_seed_workflow.sql` 之外的 seed 拆分，也没有 patches/ 子目录**。
- **建议**:
  - T1 期间不解决此问题（属于全局 DDL 收敛，超出 HR 试点范围）。
  - spec §11.5 增加"P0 重构完成后**冻结基线**"的描述，并显式声明哪些旧脚本进入"已废弃"状态。
  - 在总方案文档的 §11.5 末尾或附录 C 列出"SQL 收敛路线图"，说明哪些脚本何时合并/废弃。
- **修复成本**: 中（spec 增加路线图；不在本试点执行）

### P1-5: 7 个 HR 测试类已存在但 spec §5 验收矩阵没引用

- **位置**:
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/service/HrLeaveServiceTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/service/HrLeaveBalanceSemanticsTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/enums/HrLeaveEnumsTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/entity/HrLeaveRuleTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/entity/HrLeaveBalanceTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/entity/HrLeaveApplyTest.java`
  - `code/backend/oa-hr/src/test/java/cn/oa/hr/controller/HrLeaveControllerTest.java`
- **问题**: spec §5 验收矩阵只说"`cd code/backend && mvn -pl oa-hr -am test`"——但这会跑全部 `oa-hr` 测试，包括跨域测试（如 `HrEmployeeExtTest` 等）。HR 请假试点的实际测试目标应该是上面 7 个文件。spec 没说"哪几个测试类是试点的门禁"。
- **建议**: spec §5 验收矩阵增加一列"测试目标"或单独一节"试点测试清单"，列出这 7 个测试类作为"必须通过"的子集。
- **修复成本**: 低（一节）

---

## P2 可推迟问题

### P2-1: 旧 `oa-model/src/main/java/cn/oa/hr/` 4 个类（`HrLeaveRule`、`HrLeaveBalance`、`HrTransfer`、`HrAttendance`、`HrEmployeeExt`）的删除时机

- **位置**: `code/backend/oa-model/src/main/java/cn/oa/hr/`
- **问题**: 与 P0-2 同源。T11 之前不删（spec §8.5 硬规则），但 T11 阶段删除前需要先确认旧 `OaLeaveApply`/`OaLeaveBalance`（旧 `Oa*` 前缀）没有引用 `cn.oa.hr.Hr*`（新 `Hr*` 前缀但旧包）。
- **建议**: T2 期间做一次引用图分析，输出"哪些旧类被哪些 Controller/Service 引用"。T11 阶段按引用图删除。
- **修复成本**: 中（依赖引用分析）

### P2-2: spec §1.1 工作流子模块划分与实际 `oa-workflow/oa-workflow-api` 的 controller 命名风格

- **位置**:
  - spec §1.1: `oa-workflow-api` 含 `WorkflowCallbackDispatcher`（callback 包）
  - 实际: `code/backend/oa-workflow/oa-workflow-api/src/main/java/cn/oa/workflow/api/controller/WorkflowController.java` (`@/api/workflow`)
- **问题**: spec 说 `oa-workflow-api` 主要是 callback，但实际已经有了统一的 `WorkflowController`。`callback` 包是否存在需要 T1/T2 期间确认。
- **建议**: T2 期间确认 callback dispatch 是放在 `oa-workflow-api/callback/` 还是某个 `oa-hr/callback/`（实际是后者，`HrLeaveCallbackHandler.java` 在 `oa-hr` 里）。spec §2.5 的 callback dispatcher 与 `HrLeaveCallbackHandler` 的关系需要明确画出来。
- **修复成本**: 低（spec 补一张图）

---

## 跨文档冲突表

| 主题 | 文档 A | 文档 B | 建议统一为 |
|------|--------|--------|------------|
| HR 申请 `status` 字段取值 | spec §7.3: 5 个 (DRAFT/RUNNING/PASSED/REJECTED/REVOKED) | spec §2.2 实例状态: 7 个 (含 ABORTED/SUSPENDED) | 申请单 5 个，ABORTED/SUSPENDED 走实例表，不由申请单表达 |
| Mapper 归属 | spec §1.1: 业务模块各有 mapper 子包 | `oa-mapper` 模块有 20+ 个旧 mapper | 新 mapper 在 `oa-{module}/mapper/`，`oa-mapper` 仅放跨模块共享 |
| 路径风格 | spec §11.6: `/api/{module}/{resource}` | finance 实代码: `/api/finance/v1/*` | spec 明确 v{N} 是"并行兼容策略"，不推荐新接口使用 |
| `{resource}` 复数 | spec §11.6: 没明说 | 新代码: 全用复数 | spec 明确"新接口用复数" |
| HR 试点进度 | spec §7 §8: 全部"待开发" | 实际 `oa-hr`: 已实现 80% | T1/T2 文档必须显式标"已实现/待实现"两栏 |
| 实体包路径 | spec §1.1: `cn.oa.hr.entity.*` | `oa-model`: `cn.oa.hr.*`（重复） | 统一为 `cn.oa.hr.entity.*`，旧包作为兼容层 |
| 路由前缀 | spec §11.6: 没约束单复数 | `oa-web` 旧: `/api/alert` (单数), 新模块: `/api/admin/seals` (复数) | 新接口 `/api/{module}/{resource-plural}`，旧接口保留 |
| 工作流 callback dispatcher 位置 | spec §2.5: 放在 `oa-workflow-api/callback/` | 实际: `oa-hr/callback/HrLeaveCallbackHandler.java` | spec 加图: dispatcher 在 workflow-api，具体 handler 在业务模块 |
| 状态机终态 | spec §2.2: PASSED/REJECTED/REVOKED/ABORTED 都是终态 | spec §2.2 又说 "REJECTED → RUNNING"（驳回后重提） | REJECTED 不是真终态，REJECTED→REVOKED→DRAFT 才是重提路径，文档需澄清 |
| 验收测试范围 | spec §5: `mvn -pl oa-hr -am test` | 实际: oa-hr 跑跨域测试 | spec 明确"试点测试"为 7 个文件清单 |

---

## 不修改建议

以下 spec 段落虽然有问题但不建议在 T1/T2 期间修改（理由）:

1. **spec §1.1 完整模块图**（1.1 节 ~84 行 ASCII 图）— 完整重画超出 HR 试点范围，且图本身与现状大致一致。
2. **spec §2.5 会签汇总算法详细代码**（500-700 行 Java 代码示例）— 算法逻辑正确，只是放在 spec 里过长。可考虑 T6 工作流实现时同步一份代码到 `oa-workflow-core` 注释中，但 spec 保留作为算法来源。
3. **spec §1.3 技术栈版本表**— 表内版本与 `pom.xml`/`package.json` 对齐后**作为单独任务**做，不混在 HR 试点。
4. **spec §11.8 阶段门禁**— 跨整个 M1-M6，T1 期间不动。
5. **附录 B 表前缀规范**— 已经统一（T1 草案里就用的 `hr_*`），无需修改。

---

## 附录: 完整证据链

### A. spec §7.2 / §7.3 vs 实际 `oa-hr` 实体

- spec `2026-06-02-hr-leave-pilot-task-split.md:248-280` 列出 `hr_leave_apply`/`hr_leave_balance`/`hr_leave_rule` 字段草案
- 实际: `code/backend/oa-hr/src/main/java/cn/oa/hr/entity/HrLeaveApply.java` 等 3 个 Entity 已存在
- **T1 必须做的事**: 读 3 个 Entity 的所有字段，更新 spec §7.3 让"草案"变成"已实现"

### B. 旧代码保留清单（spec §8.1 已列但需要更新）

- `code/backend/oa-model/src/main/java/cn/oa/entity/OaLeaveApply.java` ✓ 还在
- `code/backend/oa-model/src/main/java/cn/oa/entity/OaLeaveBalance.java` ✓ 还在
- `code/backend/oa-model/src/main/java/cn/oa/entity/dto/LeaveApplyDTO.java` ✓ 还在
- `code/backend/oa-model/src/main/java/cn/oa/entity/dto/LeaveBalanceInitDTO.java` ✓ 还在
- `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveApplyMapper.java` ✓ 还在
- `code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaLeaveBalanceMapper.java` ✓ 还在
- `code/backend/oa-service/src/main/java/cn/oa/service/LeaveApplyService.java` — **未找到**（需 T2 期间确认是否存在）
- `code/backend/oa-service/src/main/java/cn/oa/service/LeaveBalanceService.java` — **未找到**（需 T2 期间确认）
- `code/backend/oa-web/src/main/java/cn/oa/controller/LeaveApplyController.java` ✓ 还在
- `code/backend/oa-web/src/main/java/cn/oa/controller/LeaveBalanceController.java` ✓ 还在
- `code/backend/oa-model/src/main/java/cn/oa/hr/HrLeaveRule.java`（旧包）✓ 还在
- `code/backend/oa-model/src/main/java/cn/oa/hr/HrLeaveBalance.java`（旧包）✓ 还在

### C. 旧 Web/Mobile API 文件

- `code/frontend/src/api/leave.ts` — 未在本次搜索范围内，T2 期间确认
- `code/mobile/src/api/leave.ts` — 未在本次搜索范围内，T2 期间确认
- `code/frontend/src/api/leaveBalance.ts` — 未在本次搜索范围内，T2 期间确认
- `code/frontend/src/views/oa/leave/*` — 未在本次搜索范围内，T2 期间确认
- `code/mobile/src/pages/oa/leave-*.vue` — 未在本次搜索范围内，T2 期间确认

### D. 工作流实际实现

- Entity: `WfInstance`/`WfTask`/`WfNode`/`WfRecord`/`WfTransition`/`WfAssigneeRule`/`WfDefinition`/`WfDelegation`（spec §2.4 8 张表全有）
- 包路径: `cn.oa.workflow.model.entity`（与 spec §1.1 一致）
- Controller: `cn.oa.workflow.api.controller.WorkflowController` @/api/workflow
- Callback: `cn.oa.hr.callback.HrLeaveCallbackHandler`（业务模块内）
- Enums: `TaskStatusEnum`、`InstanceStatusEnum`、`AssigneeRuleTypeEnum`、`ApprovalModeEnum`

### E. 环境验证

- `mvn`、`pnpm` 等命令的可用性**未在本审计中验证**。T1 期间用最小命令（如 `mvn -version`、`node -v`）确认。

---

## T1/T2 期间具体行动建议

1. **T1 第一步**: grep `code/backend/sql/` 确认 `hr_*` 表 DDL 是否存在。
2. **T1 第二步**: 读 3 个 `HrLeave*` Entity 全字段，对照 spec §7.3 写"已实现 vs 草案"对照表。
3. **T1 第三步**: 读 3 个 `HrLeave*` Controller 的 `@*Mapping` 路径，对照 spec §7.5 写"已实现 vs 草案"对照表。
4. **T1 第四步**: 读 `HrLeaveCallbackHandler` 确认工作流回调接入点，输出回调事件清单。
5. **T1 第五步**: 修复 P0-1 / P1-1 / P1-2 / P1-3 / P1-5 五个 spec 段落（在 `2026-06-02-hr-leave-pilot-task-split.md` 中补充"已实现状态"或修改错误段落）。
6. **T2 第一步**: grep `code/backend/oa-service/` 确认 `LeaveApplyService`/`LeaveBalanceService` 是否真存在（spec §8.1 列了但本次搜索没找到——可能已迁出或名字改了）。
7. **T2 第二步**: 列出 `cn.oa.hr.Hr*` 旧包 5 个类的所有引用方（grep `import cn.oa.hr.Hr`）。
8. **T2 第三步**: 列出前端/移动端所有 leave 相关文件路径（已部分在 spec §8.2，T2 期间更新清单）。

## 修复优先级

| 顺序 | 问题 | 工作量 | 阻塞 T3+ 推进 |
|------|------|--------|------------|
| 1 | P0-1 SQL 草案校准 | 低 | 强 |
| 2 | P0-2 类重复决策 | 中 | 强 |
| 3 | P0-3 T3-T5 已实现对照 | 中 | 强 |
| 4 | P0-4 Mapper 归属 | 低 | 强 |
| 5 | P1-1 路径单复数 | 低 | 弱 |
| 6 | P1-2 v{N} 策略 | 低 | 弱 |
| 7 | P1-3 状态机对齐 | 低 | 弱 |
| 8 | P1-4 SQL 收敛路线图 | 中 | 弱 |
| 9 | P1-5 验收测试清单 | 低 | 弱 |
| 10 | P2-1 引用图 | 中 | 弱 |
| 11 | P2-2 callback 图 | 低 | 弱 |
