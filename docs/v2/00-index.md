# Corporate OA System v2 — 设计文档总索引

> 日期: 2026-06-07
> 状态: **v2 实施阶段（Phase 3: 核心模块收尾 / Phase 4 外围模块试点）**
> 维护人: Hermes 主 agent
> 阅读顺序: **从 §1 开始往下**，每份文档有「依赖关系」一节标前置阅读

---

## 0. 重要告示 — v1 spec 已被 v2 替代

v1 spec 系列（`docs/superpowers/specs/2026-06-02-*.md`）于 2026-06-04 标记为 **DEPRECATED**。
v2 设计完成后，新代码、新决策、新 review **必须基于 v2**。v1 仅作历史参考。

| v1 文档 | v2 替代 |
|---------|---------|
| `2026-06-02-oa-system-redesign.md` (主架构, 2171 行) | `docs/v2/01-architecture.md` |
| `2026-06-02-m0-redesign-readiness.md` (M0 准备) | 合并到 `02-database.md` §10 准备清单 |
| `2026-06-02-full-optimization-plan-complete.md` (总优化方案) | 合并到 `01-architecture.md` §6 |
| `2026-06-02-adm-seal-asset-task-split.md` | 合并到 `05-modules/05-admin.md` |
| `2026-06-02-doc-dispatch-receive-task-split.md` | 合并到 `05-modules/06-document.md` |
| `2026-06-02-fin-budget-expense-task-split.md` | 合并到 `05-modules/07-finance.md` |
| `2026-06-02-hr-attendance-task-split.md` | 合并到 `05-modules/08-hr-attendance.md` |
| `2026-06-02-hr-employee-archive-task-split.md` | 合并到 `05-modules/09-hr-employee.md` |
| `2026-06-02-hr-leave-pilot-task-split.md` (HR 试点) | 合并到 `05-modules/10-hr-leave.md` |
| `2026-06-02-hr-leave-pilot-contract.md` (我刚写的契约) | 合并到 `03-api-spec.md` §6 |
| `2026-06-02-hr-leave-pilot-impact-analysis.md` (我刚写的) | 合并到 `07-development-sop.md` §4 旧代码清理 |
| `2026-06-02-hr-performance-task-split.md` | 合并到 `05-modules/11-hr-performance.md` |
| `2026-06-02-hr-recruitment-task-split.md` | 合并到 `05-modules/12-hr-recruitment.md` |
| `2026-06-02-hr-training-task-split.md` | 合并到 `05-modules/13-hr-training.md` |
| `2026-06-02-km-knowledge-task-split.md` | 合并到 `05-modules/14-knowledge.md` |
| `2026-06-02-msg-notification-platform-task-split.md` | 合并到 `05-modules/15-message.md` |
| `2026-06-02-mt-meeting-task-split.md` | 合并到 `05-modules/16-meeting.md` |
| `2026-06-02-task-project-management-task-split.md` | 合并到 `05-modules/17-task.md` |
| `2026-06-02-wf-api-contract.md` | 合并到 `03-api-spec.md` §7 工作流 |
| `2026-06-02-wf-engine-kernel-task-split.md` | 合并到 `05-modules/18-workflow.md` |
| `docs/superpowers/workflows/claude-code-oa-redesign-workflow.md` | 合并到 `08-cicd.md` §3 |
| `docs/项目说明书.md` | 合并到 `01-architecture.md` §2 业务概览 |

**v1 包含的不可替代内容**（清理出来作为参考）：
1. `2026-06-02-hr-leave-pilot-contract.md` (我刚写的契约 v2) — API 字段对齐表，**已合并到 v2 §3-api-spec**
2. `2026-06-02-hr-leave-pilot-impact-analysis.md` (我刚写的影响分析) — 旧代码映射表，**已合并到 v2 §7-sop §4 旧代码清理**

---

## 1. v2 文档目录（8 大类，约 25 份）

```
docs/v2/
├── 00-index.md                                  (本文件)
├── 01-architecture.md                           (主架构: 模块清单/分层/命名)
├── 02-database.md                               (数据库 v2: 全表/索引/事务/字符集)
├── 03-api-spec.md                               (API 规范 v2: RESTful/错误码/权限码/版本/限流/幂等)
├── 04-frontend.md                               (前端架构 v2: vue3+ts 目录/状态/路由/API 客户端/主题/国际化)
├── 05-modules/                                  (25 个模块详细设计，每个 1 份)
│   ├── 05-platform-common.md                    (平台: common/security/web)
│   ├── 06-workflow.md                           (工作流引擎)
│   ├── 07-admin.md                              (印章/资产/用品)
│   ├── 08-document.md                           (发文/收文/签报)
│   ├── 09-finance.md                            (预算/报销/借款)
│   ├── 10-hr-leave.md                           (请假)
│   ├── 11-hr-attendance.md                      (考勤)
│   ├── 12-hr-employee.md                        (员工档案)
│   ├── 13-hr-performance.md                     (绩效)
│   ├── 14-hr-recruitment.md                     (招聘)
│   ├── 15-hr-training.md                        (培训)
│   ├── 16-knowledge.md                          (知识库)
│   ├── 17-message.md                            (消息/通知)
│   ├── 18-meeting.md                            (会议室/会议)
│   └── 19-task.md                               (任务/项目)
├── 06-testing.md                                (测试规范 v2: 单元/集成/E2E/覆盖率/工具链)
├── 07-cicd.md                                   (CI/CD v2: 本地/提交/CI/release/回滚)
└── 08-development-sop.md                        (开发-测试-review 闭环 SOP)
```

**共 25 份文档**（4 份主规范 + 14 份模块详细设计 + 4 份工程实践 + 3 份主索引/总览）

---

## 2. 文档依赖图（阅读顺序）

```
00-index (本文件)
  └─> 01-architecture (主架构, 必读)
       ├─> 02-database (DB, 必读)
       ├─> 03-api-spec (API, 必读)
       ├─> 04-frontend (前端, 前端开发者必读)
       ├─> 05-modules/* (按需)
       ├─> 06-testing (测试, 必读)
       ├─> 07-cicd (CI/CD, 必读)
       └─> 08-development-sop (闭环 SOP, 必读)
```

**最小必读集**（新人 2 天入门）：00 → 01 → 02 → 03 → 06 → 08（6 份）
**前端开发者**：+ 04-frontend
**后端开发者**：+ 05-modules/* 自己负责的模块
**DevOps**：+ 07-cicd

---

## 3. 关键设计原则（v2 全局）

### 3.1 命名
- **包名**: `cn.oa.platform.*`（平台层） / `cn.oa.{module}.*`（业务模块，**复数** `cn.oa.hr.*` `cn.oa.finance.*`）
- **类名前缀**: 平台用 `Platform*`（PlatformSecurity/PlatformRedis/PlatformIdGen），业务用模块名 `Hr*`/`Fin*`/`Adm*`/`Wf*`
- **表名**: `{module}_{entity}` 蛇形，复数（`hr_leaves` 而不是 `hr_leave`，`wf_tasks` 而不是 `wf_task`）
- **字段**: 蛇形，主键 `id`，外键 `{entity}_id`，软删除 `del_flag`，时间 `create_time`/`update_time`/`create_by`/`update_by`
- **路径**: `/api/v1/{module}/{resource-plural}`
- **权限码**: `{module}:{resource}:{action}` 三段式，`{action}` 来自白名单（`view`/`list`/`create`/`update`/`delete`/`export`/`approve`/`reject`/`revoke`/`transfer`）

### 3.2 分层
- **Controller**: 薄层（参数解析/权限校验/响应包装），禁止业务逻辑
- **Service**: 业务逻辑（事务边界/状态机/计算/校验），**所有写操作必须经过 Service**
- **Manager/Helper**: 跨 Service 复用（如 IdGen/RedisLock/CacheManager）
- **Mapper**: MyBatis-Plus BaseMapper，**禁止手写 SQL 在 Controller**（除极少数统计场景）
- **Entity/PO/DO**: 严格 POJO，对应表
- **DTO**: 入参（Controller 接收），**禁止把 Entity 直接当 DTO**
- **VO**: 出参（Controller 返回），**禁止把 Entity 直接当 VO**
- **BO**: 内部 Service 间传递的业务对象，可跨表聚合

### 3.3 错误与日志
- 错误码 5 位：业务模块编码 2 位 + 子错误 3 位（如 `HR_001` 请假类型无效）
- 统一 `R<T>` 响应：`{code, message, data, traceId}`
- 日志：MDC 注入 `traceId`/`empId`/`requestUri`，INFO/WARN/ERROR 三级

### 3.4 事务
- `@Transactional` 标注在 **Service 方法**，Controller 不开事务
- 默认 `REQUIRED`，跨服务调用用 `TCC` 或 `SAGA`（v2 暂未实现，文档留位）
- 写操作必须捕获 `BusinessException`，事务回滚

### 3.5 安全
- JWT（无状态）+ Refresh Token（HttpOnly Cookie）
- 接口签名：写操作加 `X-Sign` 头（HMAC-SHA256）（Phase 3+ 可选实现，当前未启用）
- 数据权限：5 级（本人/本部门/本部门及下级/本公司/全部），由 `@DataPermission` 注解实现
- 操作日志：所有写操作记录 `oa_operation_log`（aop 自动）

### 3.6 性能
- 慢查询 > 500ms 进慢日志
- 热点数据 Redis 缓存（带 `@Cacheable`），写时 `@CacheEvict`
- 分页统一 `pageNum`+`pageSize`，`pageSize <= 100`，超过报错
- 列表接口必须 `EXPLAIN` 验证，禁用 `SELECT *`

---

## 4. v2 路线图（5 阶段）

| 阶段 | 任务 | 预计工时 | 状态 |
|------|------|----------|------|
| **Phase 1** | 写 v2 设计文档（25 份） | 2-3 天 | **已完成** |
| **Phase 2** | 建项目骨架（删除 v1 旧代码，按 v2 重搭） | 3-5 天 | **已完成** |
| **Phase 3** | 实施核心模块（HR/Workflow/Admin） | 10-15 天 | **收尾验证中** |
| **Phase 4** | 实施外围模块（Finance/Document/Meeting/...） | 10-15 天 | **部分试点实现** |
| **Phase 5** | E2E 测试 + 性能调优 + 上线 | 5-7 天 | 未开始 |

---

## 5. 当前进度（2026-06-07）

- [x] v1 spec 摸底（已读 20 份 spec + 1 份 workflow + 项目说明书）
- [x] 当前代码摸底（25 模块/65 Controller/77 Service/74 Vue view/40 API/20 mobile page/30+ 表）
- [x] v1 5 项 spec 内部不一致修复（已合并到 v2）
- [x] **Phase 1 写 v2 文档**（已完成）
  - [x] 00-index.md (本文件)
  - [x] 01-architecture.md
  - [x] 02-database.md
  - [x] 03-api-spec.md
  - [x] 04-frontend.md
  - [x] 05-modules/* (14 份)
  - [x] 06-testing.md
  - [x] 07-cicd.md
  - [x] 08-development-sop.md
- [x] v1 文档批量加 deprecation 头（20 份）
- [x] 用户审 v2 设计文档
- [x] **Phase 2 项目骨架**（已完成）
  - [x] 父 POM + 18 个 Maven 模块
  - [x] oa-platform-common（23 类）
  - [x] oa-platform-security（8 类）
  - [x] oa-platform-web（启动模块 + Flyway 迁移）
  - [x] oa-workflow（26 类）
  - [x] oa-system（16 类）
- [/] **Phase 3 核心模块实施**
  - [x] oa-hr-leave 骨架（Entity/Mapper/Service/Controller）
  - [x] oa-hr-employee 骨架
  - [x] oa-hr-leave 完善（DTO/VO/单元测试/审批流测试）
  - [x] 权限/认证错误响应按 API 规范返回 HTTP 401/403/404/422 等状态码
  - [x] 启动测试日志噪声清理（Druid H2 校验语句、mapper 扫描 WARN 降噪）
  - [/] oa-hr-employee 完善
  - [/] oa-admin
  - [/] 其余外围业务模块试点页面/API 已存在，待按 v2 契约验收收口
- [/] **Phase 4 外围模块试点**
  - [x] Finance/Document/Meeting/Message/Task 等模块已有前后端雏形
  - [ ] 按 v2 API、权限码、DTO/VO、测试规范逐模块验收
- [/] **工程质量与构建**
  - [x] 后端核心测试通过：`oa-hr-leave`、`oa-platform-web -am`
  - [x] 前端登录页清理未实现入口，保留真实登录链路
  - [x] 前端 TypeScript 输出污染清理，`tsconfig` 禁止源目录 emit
  - [x] 前端 Element Plus 按需引入、ECharts 模块化导入，生产构建不再出现大 chunk 告警
  - [ ] 全量后端 `mvn test -DskipITs` 与前端构建需作为每轮收口验证

---

## 6. 阅读建议

**先看 5 分钟摘要**：
1. 本文 §1 文档目录
2. `01-architecture.md` §1 业务概览
3. `01-architecture.md` §3 模块清单
4. `01-architecture.md` §6 关键决策

**再按需深入**：
- 后端开发：01→02→03→05-modules/你负责的→06→08
- 前端开发：01→03→04→05-modules/你负责的→06→08
- 测试/DevOps：01→02→03→06→07→08
