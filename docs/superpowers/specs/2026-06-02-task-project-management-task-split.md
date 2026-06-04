# 任务与项目管理 oa-task 模块重构实施与任务拆分

> 日期: 2026-06-03
> 模块范围: 项目/任务/子任务/依赖/工时/评论/附件/日志 完整闭环
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 第 3.3 节
> 参考模板: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 模块目标

任务与项目管理是全新构建的模块，承接企业"项目→任务→子任务"三层级模型，提供看板/甘特图/列表/日历四视图、四种任务依赖类型、工时统计、人员负载、评论与@提醒、个人与项目合并任务视图等核心能力。

完成后应具备：

1. `task_*` 项目/任务/依赖/工时/评论/附件/日志 8 张核心表结构与初始数据。
2. `oa-task` 模块内项目、任务、子任务、依赖、工时、评论、附件的完整 Service。
3. 看板视图与甘特图视图的数据组装 Service 与缓存策略。
4. 个人任务 + 项目任务合并视图 API，跨项目按"我的待办/我的已完成/我负责/我参与"四维度聚合。
5. 工时统计与人员负载查询 API（部门/项目/个人三级粒度）。
6. 任务评论 @提醒联动 `oa-message` 站内通知。
7. Web 管理端：项目列表/详情/看板/甘特图/我的任务五入口。
8. 移动端：我的任务、任务详情、添加工时、评论@ 四个核心页面。
9. 后端测试通过、前端构建通过、移动端 H5 构建通过。

---

## 2. 边界

### 2.1 本模块包含

| 区域 | 内容 |
|------|------|
| 数据库 | `task_project`、`task_project_member`、`task_item`、`task_dependency`、`task_hours`、`task_comment`、`task_attachment`、`task_log` 共 8 张表 |
| 后端 | `oa-task` 模块（含 model/mapper/service/api），任务/项目/子任务/依赖/工时/评论/附件业务 Service |
| 看板与甘特图 | 数据组装 Service、缓存键、视图 DTO/VO |
| Web | 项目列表、详情、看板、甘特图、我的任务、工时统计页 |
| Mobile | 我的任务列表、任务详情、记录工时、任务评论页 |
| 消息 | 任务分配/被@/截止提醒走 `oa-message` 站内通知（不含外部短信/邮件渠道） |
| 测试 | Task Service、Dependency 检测、Gantt/Kanban 视图组装、Controller API、关键前端构建 |

### 2.2 本模块不包含

| 不包含 | 原因 |
|--------|------|
| 跨项目综合统计报表 | 留待 BI/报表模块 |
| 复杂排班算法 | 任务管理只按工时聚合，复杂排班属于 HR 排班域 |
| 与 HR 绩效强绑定 | 任务数据只暴露评估关系（如"完成率"），不写入绩效表 |
| 工作流引擎（任务审批） | 任务状态机由本模块自治，跨模块走流程的需求可调用 `oa-workflow` |
| 复杂知识库/ES 搜索 | 任务标题/描述不进 ES，由本模块用 MySQL LIKE 即可 |
| 跨项目甘特图 | 第一版仅支持"单项目甘特图"，跨项目甘特图后续迭代 |
| 前端 monorepo 改造 | 沿用现有 vue-pure-admin 目录结构，monorepo 后续统一处理 |
| 任务模板/工作日日历配置 | 第一版只读取系统默认日历 |

---

## 3. 任务波次

### Wave 1: 契约与基线

#### T1 数据库与 API 契约

| 字段 | 内容 |
|------|------|
| 目标 | 定义任务管理的数据表、接口、权限码、DTO/VO |
| 路径 | `code/backend/sql/`、`docs/superpowers/specs/2026-06-02-oa-system-redesign.md` |
| 输入 | 重构文档 3.3 节；旧 `oa_*` 表（旧仓库无该模块，新模块全新构建） |
| 输出 | 8 张表 DDL 草案、API 契约表、权限码清单、索引与 EXPLAIN 验收说明 |
| 禁止修改 | 不实现 Service/Controller 业务逻辑 |
| 验收 | 文档列出接口、字段、索引、权限码、验收命令 |

#### T2 旧实现影响分析（全新模块）

| 字段 | 内容 |
|------|------|
| 目标 | 确认旧仓库无相关表/Service/Controller/前端入口，避免破坏性命名冲突 |
| 路径 | `code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、`code/frontend/src/api/`、`code/mobile/src/api/` |
| 输出 | 旧入口扫描报告（无冲突/有冲突及处理方式） |
| 禁止修改 | 不删除旧代码 |
| 验收 | 报告或本文件追加清单 |

### Wave 2: 项目与成员

#### T3 项目/任务/依赖/工时/评论/附件/日志 8 张表 Entity + Mapper

| 字段 | 内容 |
|------|------|
| 目标 | 在 `oa-task` 模块内建立 8 张表的实体、Mapper、基础查询 |
| 路径 | `code/backend/oa-task`、`code/backend/sql/baseline/001_schema.sql` |
| 输出 | 8 个 Entity、8 个 Mapper、Mapper 测试 |
| 禁止修改 | 不修改前端、移动端、不实现业务 Service |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

#### T4 项目 Service + REST API

| 字段 | 内容 |
|------|------|
| 目标 | 项目创建/更新/详情/列表、成员增删、状态自动更新（按任务完成度） |
| 路径 | `code/backend/oa-task` |
| 输出 | ProjectService 接口/实现、ProjectController、DTO/VO、单元测试 |
| 禁止修改 | 不复制旧 Controller 逻辑（旧仓库无相关代码） |
| 验收 | `cd code/backend && mvn -pl oa-task,oa-web -am test` |

### Wave 3: 任务与子任务

#### T5 任务 Service + REST API

| 字段 | 内容 |
|------|------|
| 目标 | 任务创建/更新/详情/列表、状态流转、进度更新、子任务层级查询 |
| 路径 | `code/backend/oa-task` |
| 输出 | TaskService 接口/实现、TaskController、子任务查询、单元测试 |
| 禁止修改 | 不实现依赖检测、不实现工时 |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

#### T6 子任务 + 排序

| 字段 | 内容 |
|------|------|
| 目标 | 子任务嵌套、拖拽排序、批量改父任务、跨层级校验 |
| 路径 | `code/backend/oa-task` |
| 输出 | SubtaskService、批量改父、父子循环校验 |
| 禁止修改 | 不引入递归 CTE，先用邻接表 + 深度字段 |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

### Wave 4: 依赖与提醒

#### T7 任务依赖 Service + REST API

| 字段 | 内容 |
|------|------|
| 目标 | 四种依赖类型（FS/SS/FF/SF）的添加、删除、循环检测、依赖图查询 |
| 路径 | `code/backend/oa-task` |
| 输出 | DependencyService、DependencyController、循环检测算法（DFS 拓扑）、依赖图 API |
| 禁止修改 | 不实现依赖触发（如 FS 自动激活后续任务） |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

#### T8 任务提醒（截止/逾期）

| 字段 | 内容 |
|------|------|
| 目标 | 定时扫描即将到期和已逾期任务，发布 `TaskDueSoonEvent`、`TaskOverdueEvent` |
| 路径 | `code/backend/oa-task`、`code/backend/oa-message` |
| 输出 | TaskReminderScheduler（@Scheduled）、事件发布、消息订阅 |
| 禁止修改 | 不实现外部短信/邮件渠道，先走站内消息/WebSocket |
| 验收 | `cd code/backend && mvn -pl oa-task,oa-message,oa-web -am test` |

### Wave 5: 工时

#### T9 工时记录 + 人员负载统计

| 字段 | 内容 |
|------|------|
| 目标 | 工时记录、汇总查询（按任务/按人/按项目/按部门/按周）、人员负载 |
| 路径 | `code/backend/oa-task` |
| 输出 | HoursService、HoursController、负载算法、单元测试 |
| 禁止修改 | 不实现工时审批（工时直接累计，不走流程） |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

### Wave 6: 评论与@提醒

#### T10 任务评论 + @提醒

| 字段 | 内容 |
|------|------|
| 目标 | 任务评论增删查、回复（楼中楼）、@人员解析、@事件发布 |
| 路径 | `code/backend/oa-task`、`code/backend/oa-message` |
| 输出 | CommentService、CommentController、@解析器、消息订阅 |
| 禁止修改 | 不实现富文本/Markdown，先纯文本 + @userId |
| 验收 | `cd code/backend && mvn -pl oa-task,oa-message,oa-web -am test` |

### Wave 7: 看板与甘特图

#### T11 看板视图 Service

| 字段 | 内容 |
|------|------|
| 目标 | 按"列（状态）"分组输出任务卡片，含负责人、标签、进度、子任务数 |
| 路径 | `code/backend/oa-task` |
| 输出 | KanbanService、看板 DTO、Redis 缓存 5 分钟、单元测试 |
| 禁止修改 | 不实现拖拽持久化（拖拽走 PUT status 接口） |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

#### T12 甘特图 Service

| 字段 | 内容 |
|------|------|
| 目标 | 按项目输出甘特图数据（tasks/links/dateRange），含里程碑 |
| 路径 | `code/backend/oa-task` |
| 输出 | GanttService、GanttController、GanttData VO、Redis 缓存 10 分钟 |
| 禁止修改 | 不实现自动排期（甘特图只展示，不计算关键路径） |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

### Wave 8: 我的任务合并视图

#### T13 我的任务聚合 Service

| 字段 | 内容 |
|------|------|
| 目标 | 个人任务 + 项目任务合并，分四维度聚合（我的待办/我的已完成/我负责/我参与） |
| 路径 | `code/backend/oa-task` |
| 输出 | MyTaskService、MyTaskController、按维度过滤 |
| 禁止修改 | 不实现日历时序合并（先按优先级+截止日期） |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

#### T14 日历视图

| 字段 | 内容 |
|------|------|
| 目标 | 按"计划开始/计划结束"输出日历视图数据 |
| 路径 | `code/backend/oa-task` |
| 输出 | CalendarService、CalendarController、按月/周/日输出 |
| 禁止修改 | 不实现复杂工作日规则，按自然日展示 |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

### Wave 9: 附件与日志

#### T15 任务附件

| 字段 | 内容 |
|------|------|
| 目标 | 任务附件上传/下载/删除/列表，复用现有 uploads 目录 |
| 路径 | `code/backend/oa-task` |
| 输出 | AttachmentService、AttachmentController、文件元信息 |
| 禁止修改 | 不实现断点续传、不接入对象存储 |
| 验收 | `cd code/backend && mvn -pl oa-task,oa-web -am test` |

#### T16 任务操作日志

| 字段 | 内容 |
|------|------|
| 目标 | 任务/项目关键操作（创建/分配/状态/进度/优先级）写操作日志 |
| 路径 | `code/backend/oa-task` |
| 输出 | TaskLogService、AOP 切面（@TaskLog 注解）、日志查询 API |
| 禁止修改 | 不实现完整审计追踪，只记业务关键操作 |
| 验收 | `cd code/backend && mvn -pl oa-task -am test` |

### Wave 10: Web 与 Mobile

#### T17 Web API + 页面

| 字段 | 内容 |
|------|------|
| 目标 | Web 管理端接入任务 API：项目列表/详情、看板、甘特图、我的任务、工时统计 |
| 路径 | `code/frontend/src/api/task.ts`、`code/frontend/src/views/oa/task/` |
| 输出 | typed API、5 个核心页面、权限过滤 |
| 禁止修改 | 不做 monorepo 改造，不重构全局布局 |
| 验收 | `cd code/frontend && pnpm typecheck && pnpm build` |

#### T18 Mobile API + 页面

| 字段 | 内容 |
|------|------|
| 目标 | 移动端接入我的任务/任务详情/记录工时/任务评论 |
| 路径 | `code/mobile/src/api/task.ts`、`code/mobile/src/pages/oa/task-*.vue` |
| 输出 | typed API、4 个核心页面、@人员选择器 |
| 禁止修改 | 不实现复杂管理配置页面 |
| 验收 | `cd code/mobile && pnpm build:h5` |

### Wave 11: 验收与下线

#### T19 端到端回归

| 字段 | 内容 |
|------|------|
| 目标 | 验证"项目创建→任务分配→依赖→工时→评论@→完成"全链路 |
| 路径 | `tests/`、`code/backend/oa-task/src/test` |
| 输出 | API/E2E 测试或手工验证脚本、测试数据 |
| 禁止修改 | 不扩大到其他业务模块 |
| 验收 | 登录 → 创建项目 → 添加成员 → 创建任务 → 设置依赖 → 记录工时 → 评论@ → 状态完成 → 日志可查 |

#### T20 演示数据与上线准备

| 字段 | 内容 |
|------|------|
| 目标 | 输出 demo seed（演示项目/任务/工时），菜单/权限码上线配置 |
| 路径 | `code/backend/sql/baseline/004_seed_demo.sql`、`docs/superpowers/specs/` |
| 输出 | 演示数据脚本、菜单 JSON、权限码初始化 SQL |
| 禁止修改 | 不修改业务代码 |
| 验收 | demo seed 可在 dev 环境完整跑通 |

---

## 4. 推荐执行顺序

```
Wave 1:  T1 + T2
Wave 2:  T3 → T4
Wave 3:  T5 → T6
Wave 4:  T7 → T8
Wave 5:  T9
Wave 6:  T10
Wave 7:  T11 → T12
Wave 8:  T13 → T14
Wave 9:  T15 → T16
Wave 10: T17 与 T18 可并行
Wave 11: T19 → T20
```

T1/T2 完成前不得开始代码实现。T4 完成后 Web/Mobile 可以先做项目/任务静态结构；T11/T12 完成后 Web 才能开始看板/甘特图页面。

---

## 5. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| 任务后端 | `cd code/backend && mvn -pl oa-task -am test` |
| 任务 + Web 入口 | `cd code/backend && mvn -pl oa-task,oa-web -am test` |
| 消息联动 | `cd code/backend && mvn -pl oa-task,oa-message,oa-web -am test` |
| Web | `cd code/frontend && pnpm typecheck && pnpm build` |
| Mobile | `cd code/mobile && pnpm build:h5` |

---

## 6. 第一个可执行任务提示词

```text
请执行 oa-task 模块重构的 T1：数据库与 API 契约。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第 3.3 节
- docs/superpowers/workflows/claude-code-oa-redesign-workflow.md
- docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md（参考结构）
- docs/superpowers/specs/2026-06-02-task-project-management-task-split.md（本文件）

范围：
- 只允许修改文档和 SQL 草案。
- 不实现 Java/Vue/uni-app 业务代码。

输出：
- 8 张 task_* 表结构草案
- /api/task 全部 API 契约
- 权限码清单
- 索引和 EXPLAIN 验收说明
- 后续 T3/T4/T5 需要的 DTO/VO 字段清单

完成后汇报改动文件和下一步建议。
```

---

## 7. T1 数据库与 API 契约结果

### 7.1 模块概览

oa-task 模块为全新构建，旧仓库无相关表/Service/Controller/前端入口。表前缀统一 `task_`，代码包路径 `cn.oa.task`，REST 前缀 `/api/task`。

### 7.2 SQL 草案

T1 新增 SQL 草案文件：

`code/backend/sql/baseline/005_task_module.sql`

该文件作为契约草案，不直接替换现有 baseline。确认后在后续 T3 合并进正式 baseline。

包含：

| 表 | 说明 |
|----|------|
| `task_project` | 项目表，承载项目基本信息与状态 |
| `task_project_member` | 项目成员表，记录项目成员与角色 |
| `task_item` | 任务表，承载任务基本信息，parent_task_id 标识子任务 |
| `task_dependency` | 任务依赖表，记录任务间四种依赖类型 |
| `task_hours` | 工时记录表，员工在任务上的工时投入 |
| `task_comment` | 任务评论表，支持楼中楼回复 |
| `task_attachment` | 任务附件表，记录文件元信息 |
| `task_log` | 任务操作日志表，审计关键操作 |

### 7.3 表结构要点

#### `task_project`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `name` | VARCHAR(200) | 项目名称 |
| `description` | TEXT | 项目描述 |
| `status` | VARCHAR(16) | `PLANNING/IN_PROGRESS/COMPLETED/CANCELLED/ARCHIVED` |
| `progress` | INT | 项目完成进度 0-100 |
| `owner_id` | BIGINT | 项目负责人 ID |
| `planned_start` / `planned_end` | DATE | 计划起止 |
| `actual_start` / `actual_end` | DATE | 实际起止 |
| `priority` | VARCHAR(16) | `LOW/NORMAL/HIGH/URGENT` |
| `color` | VARCHAR(16) | 项目标签颜色（看板/甘特图着色） |
| `sort_order` | INT | 列表排序号 |

#### `task_project_member`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `project_id` | BIGINT | 项目 ID |
| `emp_id` | BIGINT | 成员 ID |
| `role` | VARCHAR(16) | `OWNER/ADMIN/MEMBER/VIEWER` |
| `joined_at` | DATETIME | 加入时间 |

唯一约束：`(project_id, emp_id)`，同一成员在同一项目仅一条。

#### `task_item`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `project_id` | BIGINT | 项目 ID（NULL 表示个人任务） |
| `parent_task_id` | BIGINT | 父任务 ID（NULL 表示顶级任务） |
| `title` | VARCHAR(200) | 任务标题 |
| `description` | TEXT | 任务描述 |
| `status` | VARCHAR(16) | `TODO/IN_PROGRESS/DONE/CLOSED/OVERDUE/CANCELLED` |
| `priority` | VARCHAR(16) | `LOW/NORMAL/HIGH/URGENT` |
| `progress` | INT | 任务进度 0-100 |
| `assignee_id` | BIGINT | 负责人 ID |
| `creator_id` | BIGINT | 创建人 ID |
| `planned_start` / `planned_end` | DATE | 计划起止 |
| `actual_start` / `actual_end` | DATETIME | 实际起止 |
| `estimated_hours` | DECIMAL(6,1) | 预估工时 |
| `actual_hours` | DECIMAL(6,1) | 实际工时（汇总） |
| `tags` | VARCHAR(500) | 标签 JSON 数组 |
| `sort_order` | INT | 排序号 |
| `depth` | INT | 层级深度（0=顶级，1=子任务，2=孙任务） |
| `is_milestone` | TINYINT | 是否里程碑 |
| `del_flag` | CHAR(1) | 软删除标志 |

注：实际工时由 `task_hours` 实时汇总，`task_item.actual_hours` 为冗余字段，写入时同步。

#### `task_dependency`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 当前任务 ID |
| `depends_on_task_id` | BIGINT | 被依赖任务 ID |
| `dependency_type` | VARCHAR(16) | `FINISH_TO_START/START_TO_START/FINISH_TO_FINISH/START_TO_FINISH` |
| `lag_hours` | INT | 延滞时间（小时），可正可负 |
| `created_at` | DATETIME | 创建时间 |

唯一约束：`(task_id, depends_on_task_id)`，同一对任务仅一条依赖。

#### `task_hours`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 任务 ID |
| `emp_id` | BIGINT | 记录人 ID |
| `work_date` | DATE | 工作日期 |
| `hours` | DECIMAL(4,1) | 工时（0.5-24） |
| `description` | VARCHAR(500) | 工作内容描述 |
| `created_at` | DATETIME | 创建时间 |

约束：`hours >= 0.5 AND hours <= 24`。

#### `task_comment`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 任务 ID |
| `emp_id` | BIGINT | 评论人 ID |
| `content` | TEXT | 评论内容 |
| `reply_to_id` | BIGINT | 回复评论 ID（楼中楼） |
| `mentioned_ids` | VARCHAR(500) | 被@人员 ID 列表（JSON 数组） |
| `del_flag` | CHAR(1) | 软删除 |

#### `task_attachment`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 任务 ID |
| `file_name` | VARCHAR(200) | 原始文件名 |
| `file_path` | VARCHAR(512) | 存储相对路径（uploads/task/...） |
| `file_size` | BIGINT | 文件大小（字节） |
| `file_type` | VARCHAR(32) | MIME 类型 |
| `uploader_id` | BIGINT | 上传人 ID |
| `uploaded_at` | DATETIME | 上传时间 |
| `del_flag` | CHAR(1) | 软删除 |

#### `task_log`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 任务 ID |
| `project_id` | BIGINT | 所属项目 ID |
| `operator_id` | BIGINT | 操作人 ID |
| `action` | VARCHAR(32) | `CREATE/UPDATE/STATUS/PROGRESS/ASSIGN/DEPENDENCY/HOURS/COMMENT/ATTACHMENT` |
| `field_name` | VARCHAR(64) | 变更字段名 |
| `old_value` | VARCHAR(500) | 旧值 |
| `new_value` | VARCHAR(500) | 新值 |
| `remark` | VARCHAR(500) | 备注 |
| `created_at` | DATETIME | 操作时间 |

### 7.4 完整 DDL

```sql
-- =========================================================================
-- 任务与项目管理 oa-task 模块 DDL
-- 数据库: oa_system
-- 字符集: utf8mb4
-- 引擎: InnoDB
-- =========================================================================

-- 1. 项目表
DROP TABLE IF EXISTS `task_project`;
CREATE TABLE `task_project` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `name`             VARCHAR(200)  NOT NULL                COMMENT '项目名称',
  `code`             VARCHAR(64)   DEFAULT NULL            COMMENT '项目编码(可选)',
  `description`      TEXT          DEFAULT NULL            COMMENT '项目描述',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'PLANNING' COMMENT '状态(PLANNING/IN_PROGRESS/COMPLETED/CANCELLED/ARCHIVED)',
  `progress`         INT           NOT NULL DEFAULT 0      COMMENT '完成进度(0-100)',
  `priority`         VARCHAR(16)   NOT NULL DEFAULT 'NORMAL' COMMENT '优先级(LOW/NORMAL/HIGH/URGENT)',
  `color`            VARCHAR(16)   DEFAULT '#1890ff'       COMMENT '标签颜色',
  `owner_id`         BIGINT        NOT NULL                COMMENT '项目负责人ID',
  `dept_id`          BIGINT        DEFAULT NULL            COMMENT '所属部门ID(数据权限)',
  `planned_start`    DATE          DEFAULT NULL            COMMENT '计划开始日期',
  `planned_end`      DATE          DEFAULT NULL            COMMENT '计划结束日期',
  `actual_start`     DATE          DEFAULT NULL            COMMENT '实际开始日期',
  `actual_end`       DATE          DEFAULT NULL            COMMENT '实际结束日期',
  `sort_order`       INT           NOT NULL DEFAULT 0      COMMENT '排序号',
  `del_flag`         CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志(0-未删 1-已删)',
  `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`code`),
  KEY `idx_project_owner` (`owner_id`),
  KEY `idx_project_dept` (`dept_id`),
  KEY `idx_project_status` (`status`),
  KEY `idx_project_planned` (`planned_start`, `planned_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 2. 项目成员表
DROP TABLE IF EXISTS `task_project_member`;
CREATE TABLE `task_project_member` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id`  BIGINT       NOT NULL                COMMENT '项目ID',
  `emp_id`      BIGINT       NOT NULL                COMMENT '员工ID',
  `role`        VARCHAR(16)  NOT NULL DEFAULT 'MEMBER' COMMENT '角色(OWNER/ADMIN/MEMBER/VIEWER)',
  `joined_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_by`   VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_emp` (`project_id`, `emp_id`),
  KEY `idx_member_emp` (`emp_id`),
  KEY `idx_member_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

-- 3. 任务表
DROP TABLE IF EXISTS `task_item`;
CREATE TABLE `task_item` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `project_id`       BIGINT        DEFAULT NULL            COMMENT '项目ID(NULL=个人任务)',
  `parent_task_id`   BIGINT        DEFAULT NULL            COMMENT '父任务ID(子任务场景)',
  `title`            VARCHAR(200)  NOT NULL                COMMENT '任务标题',
  `description`      TEXT          DEFAULT NULL            COMMENT '任务描述',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'TODO'  COMMENT '状态(TODO/IN_PROGRESS/DONE/CLOSED/OVERDUE/CANCELLED)',
  `priority`         VARCHAR(16)   NOT NULL DEFAULT 'NORMAL' COMMENT '优先级(LOW/NORMAL/HIGH/URGENT)',
  `progress`         INT           NOT NULL DEFAULT 0      COMMENT '完成进度(0-100)',
  `assignee_id`      BIGINT        NOT NULL                COMMENT '负责人ID',
  `creator_id`       BIGINT        NOT NULL                COMMENT '创建人ID',
  `dept_id`          BIGINT        DEFAULT NULL            COMMENT '所属部门(数据权限)',
  `planned_start`    DATE          DEFAULT NULL            COMMENT '计划开始日期',
  `planned_end`      DATE          DEFAULT NULL            COMMENT '计划结束日期',
  `actual_start`     DATETIME      DEFAULT NULL            COMMENT '实际开始时间',
  `actual_end`       DATETIME      DEFAULT NULL            COMMENT '实际完成时间',
  `estimated_hours`  DECIMAL(6,1)  DEFAULT NULL            COMMENT '预估工时(小时)',
  `actual_hours`     DECIMAL(6,1)  NOT NULL DEFAULT 0.0    COMMENT '实际工时(冗余, 由task_hours汇总)',
  `tags`             VARCHAR(500)  DEFAULT NULL            COMMENT '标签JSON数组',
  `is_milestone`     TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否里程碑(0否 1是)',
  `depth`            INT           NOT NULL DEFAULT 0      COMMENT '层级深度(0=顶级 1=子任务 2=孙任务)',
  `has_subtask`      TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否有子任务(冗余,加速查询)',
  `sort_order`       INT           NOT NULL DEFAULT 0      COMMENT '同级排序号',
  `del_flag`         CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志',
  `create_by`        VARCHAR(64)   DEFAULT NULL,
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`        VARCHAR(64)   DEFAULT NULL,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_project_status` (`project_id`, `status`),
  KEY `idx_task_parent` (`parent_task_id`),
  KEY `idx_task_assignee_status` (`assignee_id`, `status`),
  KEY `idx_task_creator` (`creator_id`),
  KEY `idx_task_status` (`status`),
  KEY `idx_task_planned` (`planned_start`, `planned_end`),
  KEY `idx_task_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 4. 任务依赖表
DROP TABLE IF EXISTS `task_dependency`;
CREATE TABLE `task_dependency` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '依赖ID',
  `task_id`             BIGINT       NOT NULL                COMMENT '当前任务ID',
  `depends_on_task_id`  BIGINT       NOT NULL                COMMENT '被依赖任务ID',
  `dependency_type`     VARCHAR(16)  NOT NULL DEFAULT 'FINISH_TO_START' COMMENT '依赖类型(FS/SS/FF/SF)',
  `lag_hours`           INT          NOT NULL DEFAULT 0      COMMENT '延滞时间(小时, 可正可负)',
  `created_by`          BIGINT       DEFAULT NULL            COMMENT '创建人ID',
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_dependson` (`task_id`, `depends_on_task_id`),
  KEY `idx_dep_task` (`task_id`),
  KEY `idx_dep_depends_on` (`depends_on_task_id`),
  KEY `idx_dep_type` (`dependency_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖表';

-- 5. 工时记录表
DROP TABLE IF EXISTS `task_hours`;
CREATE TABLE `task_hours` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '工时ID',
  `task_id`     BIGINT        NOT NULL                COMMENT '任务ID',
  `emp_id`      BIGINT        NOT NULL                COMMENT '记录人ID',
  `work_date`   DATE          NOT NULL                COMMENT '工作日期',
  `hours`       DECIMAL(4,1)  NOT NULL                COMMENT '工时(0.5-24)',
  `description` VARCHAR(500)  DEFAULT NULL            COMMENT '工作内容描述',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hours_task` (`task_id`),
  KEY `idx_hours_emp_date` (`emp_id`, `work_date`),
  KEY `idx_hours_date` (`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时记录表';

-- 6. 任务评论表
DROP TABLE IF EXISTS `task_comment`;
CREATE TABLE `task_comment` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `task_id`       BIGINT       NOT NULL                COMMENT '任务ID',
  `emp_id`        BIGINT       NOT NULL                COMMENT '评论人ID',
  `content`       TEXT         NOT NULL                COMMENT '评论内容',
  `reply_to_id`   BIGINT       DEFAULT NULL            COMMENT '回复评论ID(楼中楼)',
  `mentioned_ids` VARCHAR(500) DEFAULT NULL            COMMENT '被@人员ID列表(JSON数组)',
  `del_flag`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_task` (`task_id`),
  KEY `idx_comment_emp` (`emp_id`),
  KEY `idx_comment_reply` (`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务评论表';

-- 7. 任务附件表
DROP TABLE IF EXISTS `task_attachment`;
CREATE TABLE `task_attachment` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `task_id`     BIGINT        NOT NULL                COMMENT '任务ID',
  `file_name`   VARCHAR(200)  NOT NULL                COMMENT '原始文件名',
  `file_path`   VARCHAR(512)  NOT NULL                COMMENT '存储相对路径(uploads/task/...)',
  `file_size`   BIGINT        NOT NULL DEFAULT 0      COMMENT '文件大小(字节)',
  `file_type`   VARCHAR(32)   DEFAULT NULL            COMMENT 'MIME类型',
  `uploader_id` BIGINT        NOT NULL                COMMENT '上传人ID',
  `uploaded_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag`    CHAR(1)       NOT NULL DEFAULT '0'    COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_attach_task` (`task_id`),
  KEY `idx_attach_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务附件表';

-- 8. 任务操作日志表
DROP TABLE IF EXISTS `task_log`;
CREATE TABLE `task_log` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `task_id`     BIGINT        DEFAULT NULL            COMMENT '任务ID',
  `project_id`  BIGINT        DEFAULT NULL            COMMENT '项目ID',
  `operator_id` BIGINT        NOT NULL                COMMENT '操作人ID',
  `action`      VARCHAR(32)   NOT NULL                COMMENT '操作动作(CREATE/UPDATE/STATUS/PROGRESS/ASSIGN/DEPENDENCY/HOURS/COMMENT/ATTACHMENT)',
  `field_name`  VARCHAR(64)   DEFAULT NULL            COMMENT '变更字段',
  `old_value`   VARCHAR(500)  DEFAULT NULL            COMMENT '旧值',
  `new_value`   VARCHAR(500)  DEFAULT NULL            COMMENT '新值',
  `remark`      VARCHAR(500)  DEFAULT NULL            COMMENT '备注',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_task` (`task_id`),
  KEY `idx_log_project` (`project_id`),
  KEY `idx_log_operator` (`operator_id`),
  KEY `idx_log_action` (`action`),
  KEY `idx_log_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务操作日志表';
```

### 7.5 任务状态机

```
       ┌─────────┐  start  ┌──────────────┐  complete  ┌──────┐  archive  ┌─────────┐
       │  TODO   ├────────►│ IN_PROGRESS  ├───────────►│ DONE ├──────────►│ CLOSED  │
       └────┬────┘         └──────┬───────┘           └──┬───┘           └─────────┘
            │                     │                      │
            │                     │ timeout              │ reopen
            │                     ▼                      │
            │                ┌──────────┐                │
            │                │ OVERDUE  │                │
            │                └────┬─────┘                │
            │                     │ resume               │
            │                     ▼                      │
            │              (回到 IN_PROGRESS)            │
            │                                            │
            │  cancel  ┌────────────┐                    │
            └─────────►│ CANCELLED  │◄───────────────────┘
                       └────────────┘
```

状态转换规则：

| 当前状态 | 允许目标 | 触发条件 |
|---------|---------|---------|
| TODO | IN_PROGRESS / CANCELLED | 手动开始/取消 |
| IN_PROGRESS | DONE / OVERDUE / CANCELLED | 完成/超时/取消 |
| OVERDUE | IN_PROGRESS / DONE / CANCELLED | 恢复/补完成/取消 |
| DONE | CLOSED / IN_PROGRESS | 归档/重新打开 |
| CLOSED | （终态） | - |
| CANCELLED | （终态） | - |

### 7.6 依赖类型

| 缩写 | 名称 | 含义 | 触发条件 |
|------|------|------|---------|
| FS | FINISH_TO_START | 完成-开始 | 前置任务完成后，当前任务才能开始 |
| SS | START_TO_START | 开始-开始 | 前置任务开始后，当前任务才能开始 |
| FF | FINISH_TO_FINISH | 完成-完成 | 前置任务完成后，当前任务才能完成 |
| SF | START_TO_FINISH | 开始-完成 | 前置任务开始后，当前任务才能完成 |

### 7.7 API 契约

统一前缀：`/api/task`

#### 项目 API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/task/projects` | `task:project:create` | 创建项目 |
| `PUT` | `/api/task/projects/{id}` | `task:project:update` | 更新项目 |
| `DELETE` | `/api/task/projects/{id}` | `task:project:delete` | 归档项目（软删除） |
| `GET` | `/api/task/projects` | `task:project:list` | 项目列表（分页+筛选） |
| `GET` | `/api/task/projects/{id}` | `task:project:detail` | 项目详情 |
| `POST` | `/api/task/projects/{id}/members` | `task:project:member:add` | 添加成员 |
| `DELETE` | `/api/task/projects/{id}/members/{empId}` | `task:project:member:remove` | 移除成员 |
| `GET` | `/api/task/projects/{id}/members` | `task:project:member:list` | 成员列表 |
| `GET` | `/api/task/projects/{id}/progress` | `task:project:progress:view` | 项目进度汇总 |
| `GET` | `/api/task/projects/{id}/kanban` | `task:project:kanban:view` | 看板数据 |
| `GET` | `/api/task/projects/{id}/gantt` | `task:project:gantt:view` | 甘特图数据 |
| `GET` | `/api/task/projects/{id}/logs` | `task:project:log:view` | 项目日志 |

#### 任务 API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/task/tasks` | `task:item:create` | 创建任务 |
| `PUT` | `/api/task/tasks/{id}` | `task:item:update` | 更新任务 |
| `DELETE` | `/api/task/tasks/{id}` | `task:item:delete` | 删除任务（软删除） |
| `GET` | `/api/task/tasks` | `task:item:list` | 任务列表（分页+多维筛选） |
| `GET` | `/api/task/tasks/{id}` | `task:item:detail` | 任务详情 |
| `PUT` | `/api/task/tasks/{id}/status` | `task:item:status:update` | 更新状态 |
| `PUT` | `/api/task/tasks/{id}/progress` | `task:item:progress:update` | 更新进度 |
| `PUT` | `/api/task/tasks/{id}/assignee` | `task:item:assign:update` | 转交负责人 |
| `PUT` | `/api/task/tasks/{id}/priority` | `task:item:priority:update` | 调整优先级 |
| `GET` | `/api/task/tasks/{id}/subtasks` | `task:item:subtask:list` | 子任务列表 |
| `POST` | `/api/task/tasks/{id}/subtasks` | `task:item:subtask:create` | 创建子任务 |
| `PUT` | `/api/task/tasks/{id}/subtasks/sort` | `task:item:subtask:sort` | 子任务排序 |
| `POST` | `/api/task/tasks/{id}/dependencies` | `task:item:dependency:add` | 添加依赖 |
| `DELETE` | `/api/task/tasks/{id}/dependencies/{depId}` | `task:item:dependency:remove` | 删除依赖 |
| `GET` | `/api/task/tasks/{id}/dependencies` | `task:item:dependency:list` | 依赖列表（前置+后置） |
| `GET` | `/api/task/tasks/{id}/dependency-graph` | `task:item:dependency:graph` | 依赖图（用于甘特图） |
| `POST` | `/api/task/tasks/{id}/hours` | `task:item:hours:add` | 记录工时 |
| `GET` | `/api/task/tasks/{id}/hours` | `task:item:hours:list` | 工时记录 |
| `DELETE` | `/api/task/tasks/{id}/hours/{hourId}` | `task:item:hours:delete` | 删除工时（仅自己/管理员） |
| `POST` | `/api/task/tasks/{id}/comments` | `task:item:comment:add` | 添加评论 |
| `GET` | `/api/task/tasks/{id}/comments` | `task:item:comment:list` | 评论列表 |
| `DELETE` | `/api/task/tasks/{id}/comments/{commentId}` | `task:item:comment:delete` | 删除评论 |
| `POST` | `/api/task/tasks/{id}/attachments` | `task:item:attachment:add` | 上传附件 |
| `GET` | `/api/task/tasks/{id}/attachments` | `task:item:attachment:list` | 附件列表 |
| `DELETE` | `/api/task/tasks/{id}/attachments/{attId}` | `task:item:attachment:delete` | 删除附件 |
| `GET` | `/api/task/tasks/{id}/logs` | `task:item:log:view` | 任务日志 |
| `POST` | `/api/task/tasks/batch-update-status` | `task:item:status:batch` | 批量改状态 |

#### 我的任务 API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/task/my/tasks` | `task:my:list` | 我的任务（合并视图） |
| `GET` | `/api/task/my/tasks/todo` | `task:my:todo` | 我的待办（未完成+未关闭） |
| `GET` | `/api/task/my/tasks/done` | `task:my:done` | 我的已完成 |
| `GET` | `/api/task/my/tasks/owned` | `task:my:owned` | 我负责的（assignee=me） |
| `GET` | `/api/task/my/tasks/joined` | `task:my:joined` | 我参与的（assignee/creator/cc） |
| `GET` | `/api/task/my/calendar` | `task:my:calendar` | 我的日历（月/周/日） |
| `GET` | `/api/task/my/hours` | `task:my:hours:view` | 我的工时统计 |
| `GET` | `/api/task/my/workload` | `task:my:workload:view` | 我的工作负载 |

#### 工时统计 API

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/task/hours/by-project` | `task:hours:by-project` | 按项目汇总 |
| `GET` | `/api/task/hours/by-employee` | `task:hours:by-employee` | 按人员汇总 |
| `GET` | `/api/task/hours/by-dept` | `task:hours:by-dept` | 按部门汇总 |
| `GET` | `/api/task/hours/team-load` | `task:hours:team-load` | 团队负载（按周） |

### 7.8 DTO/VO 字段

#### `TaskProjectCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `name` | String | 必填，长度 1-200 |
| `code` | String | 可选，唯一 |
| `description` | String | 可选，最长 5000 |
| `priority` | String | 枚举，默认 NORMAL |
| `color` | String | 默认 `#1890ff` |
| `ownerId` | Long | 必填 |
| `deptId` | Long | 可选 |
| `plannedStart` / `plannedEnd` | LocalDate | 可选，end >= start |
| `memberIds` | List\<Long\> | 可选，创建时一并加入 |

#### `TaskItemCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `projectId` | Long | 可选，NULL 表示个人任务 |
| `parentTaskId` | Long | 可选，子任务 |
| `title` | String | 必填，长度 1-200 |
| `description` | String | 可选 |
| `priority` | String | 枚举，默认 NORMAL |
| `assigneeId` | Long | 必填 |
| `plannedStart` / `plannedEnd` | LocalDate | 可选 |
| `estimatedHours` | BigDecimal | 可选 |
| `tags` | List\<String\> | 可选 |
| `isMilestone` | Boolean | 默认 false |

#### `TaskHoursCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `taskId` | Long | 必填 |
| `workDate` | LocalDate | 必填，不能晚于今天 |
| `hours` | BigDecimal | 必填，0.5-24 |
| `description` | String | 可选，最长 500 |

#### `TaskCommentCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `taskId` | Long | 必填 |
| `content` | String | 必填，长度 1-2000 |
| `replyToId` | Long | 可选 |
| `mentionedIds` | List\<Long\> | 可选，服务端二次校验 |

#### `TaskItemVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 任务ID |
| `projectId` / `projectName` | Long/String | 项目 |
| `parentTaskId` | Long | 父任务 |
| `title` | String | 标题 |
| `status` / `statusName` | String | 状态 |
| `priority` / `priorityName` | String | 优先级 |
| `progress` | Integer | 进度 |
| `assigneeId` / `assigneeName` | Long/String | 负责人 |
| `creatorId` / `creatorName` | Long/String | 创建人 |
| `plannedStart` / `plannedEnd` | LocalDate | 计划时间 |
| `actualStart` / `actualEnd` | LocalDateTime | 实际时间 |
| `estimatedHours` / `actualHours` | BigDecimal | 工时 |
| `tags` | List\<String\> | 标签 |
| `isMilestone` | Boolean | 是否里程碑 |
| `hasSubtask` | Boolean | 是否有子任务 |
| `subtaskCount` | Integer | 子任务数 |
| `subtaskDoneCount` | Integer | 已完成子任务数 |
| `dependencyCount` | Integer | 依赖数 |
| `commentCount` | Integer | 评论数 |
| `attachmentCount` | Integer | 附件数 |
| `canEdit` / `canDelete` / `canAssign` | Boolean | 当前用户权限 |
| `overdue` | Boolean | 是否已逾期 |

#### `KanbanColumnVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | String | 列对应状态 |
| `statusName` | String | 列名 |
| `color` | String | 列颜色 |
| `tasks` | List\<TaskItemVO\> | 任务卡片 |
| `total` | Integer | 总数 |

#### `KanbanDataVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `projectId` | Long | 项目ID |
| `projectName` | String | 项目名 |
| `columns` | List\<KanbanColumnVO\> | 状态列 |
| `totalCount` | Integer | 任务总数 |
| `overdueCount` | Integer | 逾期数 |

#### `GanttTaskVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 节点 ID |
| `name` | String | 节点名 |
| `start` / `end` | LocalDate | 起止 |
| `progress` | Integer | 进度 |
| `type` | String | `project/task/milestone` |
| `assignee` | String | 负责人 |
| `dependencies` | List\<String\> | 依赖任务 ID |
| `children` | List\<GanttTaskVO\> | 子节点（子任务） |
| `color` | String | 颜色 |
| `parent` | String | 父节点 ID |

#### `GanttDataVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `tasks` | List\<GanttTaskVO\> | 任务节点 |
| `links` | List\<GanttLinkVO\> | 依赖连线 |
| `dateRange` | GanttDateRangeVO | 日期范围 |

#### `WorkloadVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `empId` | Long | 员工ID |
| `empName` | String | 员工名 |
| `deptId` / `deptName` | Long/String | 部门 |
| `weekStart` / `weekEnd` | LocalDate | 周起止 |
| `totalHours` | BigDecimal | 总工时 |
| `taskCount` | Integer | 任务数 |
| `loadPercent` | Integer | 负载百分比（相对 40h/周） |

### 7.9 索引与 EXPLAIN 验收

| 查询场景 | 索引 | 验收 |
|---------|------|------|
| 我的待办（assignee + status） | `idx_task_assignee_status(assignee_id,status,planned_end)` | EXPLAIN 命中，不全表扫 |
| 项目任务列表（project + status） | `idx_task_project_status(project_id,status,sort_order)` | 命中 |
| 项目看板（按状态分组） | `idx_task_project_status` | 命中 |
| 项目甘特图（按计划起止） | `idx_task_planned(planned_start,planned_end)` | 命中 |
| 我的日历（按计划起止 + assignee） | `idx_task_assignee_status` + `idx_task_planned` | 命中 |
| 部门数据权限 | `idx_task_dept(dept_id,status)` | 命中 |
| 依赖查询（双向） | `uk_task_dependson + idx_dep_depends_on` | 命中 |
| 工时按员工 + 日期 | `idx_hours_emp_date(emp_id,work_date)` | 命中 |
| 项目进度汇总 | `idx_task_project_status` | 命中 |
| 评论列表 | `idx_comment_task(task_id,created_at)` | 命中 |
| 任务日志 | `idx_log_task(task_id,created_at)` | 命中 |

### 7.10 后续任务输入

T3/T4/T5 实现时必须使用以上契约：

| 旧项 | 新项 |
|------|------|
| 无 | `task_project` |
| 无 | `task_project_member` |
| 无 | `task_item` |
| 无 | `task_dependency` |
| 无 | `task_hours` |
| 无 | `task_comment` |
| 无 | `task_attachment` |
| 无 | `task_log` |
| 无 | `/api/task/*` |

第一版无任何旧入口需要兼容。

---

## 8. T2 旧实现影响分析

### 8.1 旧后端文件扫描

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Entity | `code/backend/oa-model/src/main/java/cn/oa/entity/*Task*.java` | 扫描后未发现 | 无冲突 |
| Entity | `code/backend/oa-model/src/main/java/cn/oa/entity/*Project*.java` | 扫描后未发现 | 无冲突 |
| Service | `code/backend/oa-service/src/main/java/cn/oa/service/impl/TaskServiceImpl.java` 等 | 旧 `oa_todo` 待办与新 `task_*` 任务不同域，但服务名可能含 Task 字样 | 重命名旧 `TodoService` 不再使用 `Task*` 命名 |
| Controller | `code/backend/oa-web/src/main/java/cn/oa/controller/TaskController.java` | 旧 `oa_todo` 控制器 | 旧入口保持 `oa_todo`，新任务模块路径 `/api/task`，无路径冲突 |
| Mapper | `code/backend/oa-mapper/src/main/java/cn/oa/mapper/*Task*.java` | 旧 oa_todo 实体映射 | 保留旧 Mapper 不动 |

### 8.2 旧前端与移动端文件扫描

| 类型 | 旧文件 | 当前作用 | 处理方式 |
|------|--------|----------|----------|
| Web API | `code/frontend/src/api/task.ts` | 旧 `oa_todo` 待办 API | 新任务模块改用 `taskProject.ts`/`taskItem.ts` |
| Web 页面 | `code/frontend/src/views/oa/todo/` | 旧待办页面 | 保留 |
| Web 页面 | `code/frontend/src/views/oa/task/` | 旧任务页面 | 现有页面若基于 `oa_todo` 则保留；新 `task_*` 模块新增 `src/views/oa/task-project/` |
| Mobile API | `code/mobile/src/api/task.ts` | 旧待办 | 同 Web |
| Mobile 页面 | `code/mobile/src/pages/oa/todo*` | 旧待办 | 保留；新 `task-*` 页面新增到 `pages/oa/task-*.vue` |

### 8.3 命名隔离策略

| 维度 | 旧命名 | 新命名 | 隔离方式 |
|------|--------|--------|----------|
| 后端包路径 | `cn.oa.controller.TodoController` | `cn.oa.task.controller.*` | 包路径不同 |
| 后端表前缀 | `oa_todo` | `task_*` | 表前缀不同 |
| 前端路径 | `/api/todo` | `/api/task` | 路径不同 |
| 前端页面 | `views/oa/todo` | `views/oa/task-project` | 目录不同 |
| 移动端页面 | `pages/oa/todo*` | `pages/oa/task-*` | 文件名前缀不同 |

### 8.4 风险点

| 风险 | 影响 | 缓解 |
|------|------|------|
| 旧 `oa_todo` 与新 `task_*` 用户认知冲突 | 用户分不清"待办"和"任务" | T19 端到端说明文档明确边界；菜单命名区分"待办中心"与"任务项目" |
| 旧 `TaskController` 占用控制器名 | 包路径扫描时易混淆 | 旧控制器保留 `TodoController` 别名；新模块命名 `ProjectController`/`TaskItemController` |
| `task_*` 与工作流 `wf_task` 表名前缀冲突 | 误读 | 重构文档统一为 `wf_task`（工作流任务）、`task_item`（项目任务），前端 API 路径 `/api/wf/tasks` vs `/api/task/tasks` |
| 实际工时冗余字段与 `task_hours` 汇总不一致 | 显示不准 | 写入 `task_hours` 时同步更新 `task_item.actual_hours`；T9 用事务保证 |
| 依赖循环检测性能 | 复杂项目可能递归深 | 限制项目任务数 < 5000，深度 < 5；T7 用 DFS + 早停 |
| 看板/甘特图大数据量 | 单项目上千任务卡顿 | 缓存 + 字段精简（卡片只返回必要字段） |

### 8.5 回滚方式

| 回滚场景 | 操作 |
|----------|------|
| T3 8 张表失败 | 移除 `005_task_module.sql`；不启动 oa-task 服务，旧 oa_todo 不受影响 |
| T4 项目 Service 失败 | 停止引用 `oa-task` Controller，菜单隐藏任务项目入口 |
| T11/T12 看板/甘特图失败 | 保留基础项目/任务功能，回退到只展示列表视图 |
| T17/T18 前端失败 | 前端回退到旧菜单结构，旧 `oa_todo` 待办不受影响 |

---

## 9. T3 Claude Code 任务单：oa-task Entity + Mapper

### 9.1 任务目标

在 `oa-task` 模块内建立 8 张表对应的 Entity、DTO/VO、Mapper 基础结构，对齐 `005_task_module.sql`，但不实现业务 Service 和 Controller。

### 9.2 必须先阅读

```text
CLAUDE.md
docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第 3.3 节
docs/superpowers/workflows/claude-code-oa-redesign-workflow.md
docs/superpowers/specs/2026-06-02-task-project-management-task-split.md（本文件）
code/backend/sql/baseline/005_task_module.sql
code/backend/oa-task/pom.xml
code/backend/oa-model/src/main/java/cn/oa/entity/OaTodo.java（参考实体风格）
code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaTodoMapper.java
```

### 9.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 Entity、DTO、VO、Enum、Mapper |
| `code/backend/oa-task/src/test/java/**` | 新增 Mapper/模型相关测试 |
| `code/backend/oa-task/pom.xml` | 仅在缺少必要依赖时修改 |
| `docs/superpowers/specs/2026-06-02-task-project-management-task-split.md` | 记录执行结果 |

### 9.4 禁止修改

```text
code/backend/oa-service/**
code/backend/oa-web/src/main/java/cn/oa/controller/TodoController.java
code/backend/oa-web/src/main/java/cn/oa/controller/TaskController.java
code/frontend/**
code/mobile/**
code/backend/sql/baseline/001_schema.sql
code/backend/sql/baseline/002_seed_system.sql
```

### 9.5 产出物

建议类名：

| 类型 | 建议名称 |
|------|----------|
| Entity | `TaskProject`、`TaskProjectMember`、`TaskItem`、`TaskDependency`、`TaskHours`、`TaskComment`、`TaskAttachment`、`TaskLog` |
| Enum | `TaskProjectStatus`、`TaskItemStatus`、`TaskItemPriority`、`TaskDependencyType`、`TaskLogAction` |
| DTO | `TaskProjectCreateDTO`、`TaskProjectUpdateDTO`、`TaskItemCreateDTO`、`TaskItemUpdateDTO`、`TaskItemQueryDTO`、`TaskHoursCreateDTO`、`TaskCommentCreateDTO`、`TaskStatusUpdateDTO`、`TaskProgressUpdateDTO` |
| VO | `TaskProjectVO`、`TaskProjectDetailVO`、`TaskItemVO`、`TaskItemDetailVO`、`TaskHoursVO`、`TaskCommentVO`、`TaskAttachmentVO`、`TaskLogVO`、`KanbanColumnVO`、`KanbanDataVO`、`GanttTaskVO`、`GanttDataVO`、`WorkloadVO` |
| Mapper | `TaskProjectMapper`、`TaskProjectMemberMapper`、`TaskItemMapper`、`TaskDependencyMapper`、`TaskHoursMapper`、`TaskCommentMapper`、`TaskAttachmentMapper`、`TaskLogMapper` |

字段必须对齐 `005_task_module.sql`。如果现有 `oa-model/src/main/java/cn/oa/task` 已有同名/近似类，必须先说明冲突并选择合并路径，不得新增第三套重复模型。

### 9.6 完成标准

1. Entity 字段和表字段完整对应。
2. 枚举覆盖 T1 中所有项目状态、任务状态、任务优先级、依赖类型、日志动作。
3. DTO 包含基本 Jakarta Validation 注解。
4. Mapper 使用 MyBatis-Plus `BaseMapper`。
5. 不引入业务逻辑。
6. 不删除任何旧实现。

### 9.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task -am test
```

如果 `oa-task` 当前还没有测试框架或依赖导致命令失败，Claude Code 必须说明失败原因，并给出最小修复建议，不得跳过不报。

### 9.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T3：Entity + Mapper。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 9 章。

只允许新增/修改 oa-task 模块内的 Entity、DTO、VO、Enum、Mapper 和必要测试。
禁止修改旧 oa-service、旧 oa-web Controller、frontend、mobile、正式 SQL baseline。

完成后运行：
cd code/backend
mvn -pl oa-task -am test

最终汇报：
- 新增/修改文件
- 是否发现已有重复 task 模型
- 验收命令结果
- T4 需要注意的问题
```

---

## 9.9 T3 执行结果

> 模板章节，Claude Code 执行 T3 后填写。

| 项 | 内容 |
|----|------|
| 新增文件 | （由 Claude Code 填写） |
| 是否发现重复 | （是/否） |
| 验收命令结果 | （mvn test 输出） |
| T4 注意事项 | （由 Claude Code 填写） |

---

## 10. T4 Claude Code 任务单：项目 Service + REST API

### 10.1 任务目标

在 `oa-task` 模块内实现项目 Service：创建/更新/详情/列表/成员增删/状态自动更新，并暴露 REST API。

### 10.2 必须先阅读

```text
T1/T2/T3 结果
code/backend/sql/baseline/005_task_module.sql
code/backend/oa-web/src/main/java/cn/oa/controller/ProjectController.java（如有）
```

### 10.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 ProjectService、ProjectMemberService、Controller |
| `code/backend/oa-task/src/test/java/**` | 新增 Service/Controller 单元测试 |
| `docs/superpowers/specs/2026-06-02-task-project-management-task-split.md` | 记录执行结果 |

### 10.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
旧 oa_todo 相关实现
```

### 10.5 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `createProject` | 校验 code 唯一、创建时一并加入 owner 到成员表、状态默认 PLANNING |
| `updateProject` | 校验项目存在和当前用户有权限（OWNER/ADMIN） |
| `getProjectDetail` | 含项目基本信息、成员列表、任务统计（总数/已完成/进行中） |
| `pageQueryProject` | 支持按状态/优先级/负责人/部门/关键字筛选，按数据权限过滤 |
| `archiveProject` | 软删除，状态置为 ARCHIVED |
| `addMember` | 校验项目权限；同项目同员工唯一 |
| `removeMember` | 校验项目权限；OWNER 不可移除自己 |
| `updateMemberRole` | 调整成员角色 |
| `recomputeProjectProgress` | 汇总项目下所有任务进度加权平均，写入 `task_project.progress` |
| `getProjectLogs` | 分页查询项目操作日志 |

### 10.6 Controller 要求

1. 统一返回 `R<T>` 响应。
2. DTO 参数必须 `@Valid`。
3. 从当前认证上下文获取 `empId`，普通用户不能伪造项目负责人。
4. 管理接口必须有管理员或权限注解。
5. OpenAPI/Knife4j 注解完整。
6. Controller 不写业务逻辑。

### 10.7 API 必须实现

| 方法 | 路径 | 调用 Service | 权限码 |
|------|------|--------------|--------|
| `POST` | `/api/task/projects` | `createProject` | `task:project:create` |
| `PUT` | `/api/task/projects/{id}` | `updateProject` | `task:project:update` |
| `DELETE` | `/api/task/projects/{id}` | `archiveProject` | `task:project:delete` |
| `GET` | `/api/task/projects` | `pageQueryProject` | `task:project:list` |
| `GET` | `/api/task/projects/{id}` | `getProjectDetail` | `task:project:detail` |
| `POST` | `/api/task/projects/{id}/members` | `addMember` | `task:project:member:add` |
| `DELETE` | `/api/task/projects/{id}/members/{empId}` | `removeMember` | `task:project:member:remove` |
| `PUT` | `/api/task/projects/{id}/members/{empId}/role` | `updateMemberRole` | `task:project:member:role:update` |
| `GET` | `/api/task/projects/{id}/members` | `listMembers` | `task:project:member:list` |
| `GET` | `/api/task/projects/{id}/progress` | `recomputeProjectProgress` | `task:project:progress:view` |
| `GET` | `/api/task/projects/{id}/logs` | `getProjectLogs` | `task:project:log:view` |

### 10.8 测试要求

至少覆盖：

| 测试 | 场景 |
|------|------|
| 创建项目 | code 重复抛业务异常；成功返回 ID |
| 更新项目 | 非 OWNER/ADMIN 抛无权限 |
| 详情查询 | 含成员 + 任务统计 |
| 列表查询 | 按状态/部门/关键字过滤 |
| 添加成员 | 已存在抛异常；成功写入 |
| 移除成员 | OWNER 不可移除自己 |
| 项目进度 | 平均算法正确 |
| 归档项目 | 状态置为 ARCHIVED，逻辑删除 |
| Controller | 11 个接口基本调用 |

### 10.9 验收命令

```powershell
cd code/backend
mvn -pl oa-task,oa-web -am test
```

### 10.10 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T4：项目 Service + REST API。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 10 章。

前置条件：
- T3 已完成 Entity/DTO/VO/Enum/Mapper。

实现重点：
- 项目 CRUD + 成员管理 + 状态自动更新
- 数据权限过滤（按部门）
- 进度加权平均算法
- Controller 测试

完成后运行：
cd code/backend
mvn -pl oa-task,oa-web -am test

最终汇报：
- 新增/修改文件
- 核心业务方法
- 测试覆盖场景
- 验收命令结果
- T5 任务 Service 需要注意的问题
```

---

## 10.11 T4 执行结果

> 模板章节，Claude Code 执行 T4 后填写。

| 项 | 内容 |
|----|------|
| 新增文件 | （由 Claude Code 填写） |
| 核心方法 | （由 Claude Code 填写） |
| 验收命令结果 | （mvn test 输出） |
| T5 注意事项 | （由 Claude Code 填写） |

---

## 11. T5 Claude Code 任务单：任务 Service + REST API

### 11.1 任务目标

实现任务创建/更新/详情/列表、状态流转、进度更新、子任务层级查询 API，但不实现依赖检测和工时。

### 11.2 必须先阅读

```text
T1/T2/T3/T4 结果
code/backend/sql/baseline/005_task_module.sql
T7 任务依赖设计（预读，不实现）
```

### 11.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 TaskItemService、Controller、StatusMachine |
| `code/backend/oa-task/src/test/java/**` | 新增 Service/Controller 单元测试 |
| `docs/superpowers/specs/2026-06-02-task-project-management-task-split.md` | 记录执行结果 |

### 11.4 禁止修改

```text
code/backend/oa-web/**
code/frontend/**
code/mobile/**
workflow core 算法文件
```

### 11.5 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `createTask` | 校验项目存在和成员权限；`parent_task_id` 非空时校验同项目同部门；生成任务编码 |
| `updateTask` | 校验任务存在、负责人/创建人/项目 OWNER/ADMIN 可改 |
| `getTaskDetail` | 含任务信息、负责人/创建人姓名、子任务数、评论数、附件数、依赖数 |
| `pageQueryTask` | 支持按项目/状态/优先级/负责人/创建人/标签/日期范围/关键字筛选 |
| `updateTaskStatus` | 状态机校验：见 7.5 状态转换矩阵；记录 `task_log` |
| `updateTaskProgress` | 0-100 整数；完成 100 自动改状态为 DONE |
| `updateTaskAssignee` | 校验目标用户在项目成员内；触发消息通知 |
| `updateTaskPriority` | HIGH/URGENT 触发关注人通知 |
| `batchUpdateStatus` | 批量改状态，逐个走状态机 |
| `recomputeTaskHasSubtask` | 增删子任务时同步 `has_subtask` 和父任务进度 |
| `onTaskCompleted` | 父任务进度重新计算；项目进度重算 |
| `recomputeParentProgress` | 父任务 = 子任务进度平均值 |

### 11.6 状态机实现

```java
// TaskItemStatusMachine
public class TaskItemStatusMachine {
    private static final Map<TaskItemStatus, Set<TaskItemStatus>> TRANSITIONS = Map.of(
        TODO, Set.of(IN_PROGRESS, CANCELLED),
        IN_PROGRESS, Set.of(DONE, OVERDUE, CANCELLED),
        OVERDUE, Set.of(IN_PROGRESS, DONE, CANCELLED),
        DONE, Set.of(CLOSED, IN_PROGRESS),
        CLOSED, Set.of(),
        CANCELLED, Set.of()
    );
    
    public boolean canTransit(TaskItemStatus from, TaskItemStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

### 11.7 测试要求

至少覆盖：

| 测试 | 场景 |
|------|------|
| 创建任务 | 项目/父任务校验；返回 ID |
| 状态机 | 合法转换；非法转换抛业务异常 |
| 进度更新 | 100 自动改 DONE；父任务进度更新 |
| 详情查询 | 包含统计字段 |
| 列表查询 | 多条件组合 |
| 批量改状态 | 部分失败回滚 |
| Controller | 11+ 个接口 |

### 11.8 验收命令

```powershell
cd code/backend
mvn -pl oa-task,oa-web -am test
```

### 11.9 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T5：任务 Service + REST API。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 11 章。

实现重点：
- 任务 CRUD + 状态机
- 父子任务进度联动
- 批量操作 + 事务
- Controller 测试

完成后运行：
cd code/backend
mvn -pl oa-task,oa-web -am test

最终汇报：
- 新增/修改文件
- 核心业务方法
- 测试覆盖场景
- 验收命令结果
- T6/T7/T9 任务依赖/工时前置条件
```

---

## 11.10 T5 执行结果

> 模板章节，Claude Code 执行 T5 后填写。

| 项 | 内容 |
|----|------|
| 新增文件 | （由 Claude Code 填写） |
| 核心方法 | （由 Claude Code 填写） |
| 验收命令结果 | （mvn test 输出） |
| T6 注意事项 | （由 Claude Code 填写） |

---

## 12. T7 Claude Code 任务单：任务依赖 Service + REST API

### 12.1 任务目标

实现四种依赖类型（FS/SS/FF/SF）的添加、删除、循环检测、依赖图查询。

### 12.2 必须先阅读

```text
T1-T6 结果
7.6 依赖类型定义
重构文档 3.3.3 甘特图数据结构
```

### 12.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 DependencyService、循环检测算法 |
| `code/backend/oa-task/src/test/java/**` | 新增测试 |

### 12.4 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `addDependency` | 校验任务存在；不能依赖自己；同对任务唯一；循环检测 |
| `removeDependency` | 校验权限 |
| `listPredecessors` | 当前任务的前置任务 |
| `listSuccessors` | 当前任务的后置任务 |
| `getDependencyGraph` | 返回完整图：节点 + 边，用于甘特图 |
| `checkCycle` | DFS 拓扑排序检测环；返回是否成环和成环路径 |
| `lagHours` | 延滞时间校验：-720 ~ 720（30 天范围） |

### 12.5 循环检测算法

```java
public CycleResult checkCycle(Long taskId, Long dependsOnTaskId) {
    // 从 dependsOnTaskId 开始 DFS，看能否回到 taskId
    // 限制最大深度 5000
    
    Set<Long> visited = new HashSet<>();
    Deque<Long> path = new ArrayDeque<>();
    
    boolean hasCycle = dfs(dependsOnTaskId, taskId, visited, path, new HashSet<>());
    if (hasCycle) {
        return CycleResult.cycle(path.stream().toList());
    }
    return CycleResult.noCycle();
}
```

### 12.6 测试要求

至少覆盖：

| 测试 | 场景 |
|------|------|
| 添加依赖 | 自依赖抛异常；同对重复抛异常；正常 |
| 删除依赖 | 权限校验 |
| 循环检测 | A→B→C→A 检出；A→B 正常 |
| 依赖图 | 返回节点 + 边 |
| 前置/后置列表 | 双向查询 |
| 延滞时间 | 范围校验 |

### 12.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task -am test
```

### 12.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T7：任务依赖 Service + REST API。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 12 章。

实现重点：
- 四种依赖类型（FS/SS/FF/SF）
- DFS 循环检测（限制深度 5000）
- 依赖图查询（甘特图用）
- 测试覆盖循环、深度、类型校验

完成后运行：
cd code/backend
mvn -pl oa-task -am test

最终汇报：
- 新增/修改文件
- 核心算法（循环检测、依赖图组装）
- 测试覆盖场景
- 验收命令结果
```

---

## 12.9 T7 执行结果

> 模板章节，Claude Code 执行 T7 后填写。

---

## 13. T9 Claude Code 任务单：工时 Service + REST API

### 13.1 任务目标

实现工时记录、汇总查询（按任务/按人/按项目/按部门/按周）、人员负载。

### 13.2 必须先阅读

```text
T1-T8 结果
重构文档 3.3 工时相关
```

### 13.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 HoursService、WorkloadService |
| `code/backend/oa-task/src/test/java/**` | 新增测试 |

### 13.4 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `recordHours` | 校验任务存在、当前用户是任务负责人/成员；`hours` 0.5-24；写入后同步 `task_item.actual_hours` |
| `deleteHours` | 仅本人/项目管理员可删 |
| `listTaskHours` | 任务下所有工时记录 |
| `listMyHours` | 当前用户工时（按日期范围） |
| `summaryByTask` | 按任务汇总 |
| `summaryByEmployee` | 按员工 + 项目 + 日期范围汇总 |
| `summaryByDept` | 按部门汇总 |
| `getTeamLoad` | 按周返回每位成员负载百分比（相对 40h/周） |

### 13.5 同步实际工时（关键事务）

```java
@Transactional
public Long recordHours(TaskHoursCreateDTO dto) {
    // 1. 插入 task_hours
    TaskHours hours = new TaskHours();
    hours.setTaskId(dto.getTaskId());
    hours.setEmpId(WebUtil.getEmpId());
    hours.setWorkDate(dto.getWorkDate());
    hours.setHours(dto.getHours());
    hours.setDescription(dto.getDescription());
    hoursMapper.insert(hours);
    
    // 2. 同步 task_item.actual_hours
    taskItemMapper.addActualHours(dto.getTaskId(), dto.getHours());
    
    // 3. 写 task_log
    taskLogService.logAction(dto.getTaskId(), null, HOURS, null, null, dto.getHours().toString(), "记录工时");
    
    // 4. 触发父任务和项目进度重算
    taskItemService.recomputeUpward(dto.getTaskId());
    
    return hours.getId();
}
```

### 13.6 测试要求

| 测试 | 场景 |
|------|------|
| 记录工时 | 非成员抛异常；成功；actual_hours 同步 |
| 删除工时 | 权限校验 |
| 按任务汇总 | 累计值正确 |
| 按人员 + 日期 | 范围过滤 |
| 团队负载 | 百分比计算 |
| 跨日跨周汇总 | 时间分组 |

### 13.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task -am test
```

### 13.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T9：工时 Service + REST API。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 13 章。

实现重点：
- 工时记录 + task_item.actual_hours 事务同步
- 四维度汇总（任务/员工/部门/项目）
- 团队负载算法（40h/周基线）
- 写日志 + 触发进度重算

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

---

## 13.9 T9 执行结果

> 模板章节，Claude Code 执行 T9 后填写。

---

## 14. T10 Claude Code 任务单：任务评论 + @提醒

### 14.1 任务目标

实现任务评论增删查、楼中楼回复、@人员解析、@事件发布。

### 14.2 必须先阅读

```text
T1-T9 结果
重构文档 第四章 消息中台
```

### 14.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 CommentService、@MentionParser |
| `code/backend/oa-message/**` | 订阅 @事件，写入站内消息 |
| `code/backend/oa-task/src/test/java/**` | 新增测试 |

### 14.4 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `addComment` | 校验任务存在；解析 content 中 `@userId` 格式；写入 `mentioned_ids`；发布 `TaskCommentMentionedEvent` |
| `deleteComment` | 仅本人/项目管理员可删；逻辑删除 |
| `listComments` | 任务下评论列表（含被@的人员姓名） |
| `parseMention` | 解析 `@张三` 或 `@1001` 形式，转换为 empId 列表 |
| `onMentioned` | 事件订阅：写入 oa_message 站内消息 |

### 14.5 @解析

```java
public class MentionParser {
    // 匹配 @张三 或 @1001 形式
    private static final Pattern PATTERN = Pattern.compile("@([\\u4e00-\\u9fa5A-Za-z0-9_]+|\\d+)");
    
    public List<Long> parse(String content, Map<String, Long> userNameToId) {
        List<Long> ids = new ArrayList<>();
        Matcher m = PATTERN.matcher(content);
        while (m.find()) {
            String token = m.group(1);
            if (token.matches("\\d+")) {
                ids.add(Long.parseLong(token));
            } else {
                Long id = userNameToId.get(token);
                if (id != null) ids.add(id);
            }
        }
        return ids.stream().distinct().toList();
    }
}
```

### 14.6 测试要求

| 测试 | 场景 |
|------|------|
| 添加评论 | 解析@；写入 mentioned_ids |
| 删除评论 | 权限校验 |
| 楼中楼 | reply_to_id 关联 |
| @解析 | 多种格式 |
| 消息订阅 | 事件触发消息写入 |

### 14.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task,oa-message,oa-web -am test
```

### 14.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T10：评论 + @提醒。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 14 章。

实现重点：
- 评论增删查
- @解析（@姓名 和 @userId 两种格式）
- 发布 TaskCommentMentionedEvent
- oa-message 订阅事件

完成后运行：
cd code/backend
mvn -pl oa-task,oa-message,oa-web -am test
```

---

## 14.9 T10 执行结果

> 模板章节，Claude Code 执行 T10 后填写。

---

## 15. T11 Claude Code 任务单：看板视图 Service

### 15.1 任务目标

实现看板数据组装：按"列（状态）"分组输出任务卡片，含负责人、标签、进度、子任务数。Redis 缓存 5 分钟。

### 15.2 必须先阅读

```text
T1-T10 结果
7.8 KanbanDataVO 定义
```

### 15.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 KanbanService、KanbanController |

### 15.4 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `getKanbanData` | 单项目看板：6 列（TODO/IN_PROGRESS/OVERDUE/DONE/CLOSED/CANCELLED），每列返回精简 TaskItemVO |
| `getMultiProjectKanban` | 跨项目看板：所有可见项目的聚合（管理端） |
| `invalidateKanbanCache` | 项目任务变更时调用，清除缓存 |

### 15.5 缓存策略

| 缓存键 | 过期 | 失效触发 |
|--------|------|----------|
| `task:kanban:project:{projectId}` | 5 分钟 | 任务增删改、状态变化、子任务增删 |
| `task:kanban:multi:{empId}:{deptId}` | 3 分钟 | 任意项目任务变化（粗粒度） |

### 15.6 测试要求

| 测试 | 场景 |
|------|------|
| 单项目看板 | 6 列分组正确 |
| 跨项目看板 | 数据权限过滤 |
| 缓存命中 | 第二次查询 Redis |
| 缓存失效 | 任务变化后失效 |

### 15.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task -am test
```

### 15.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T11：看板 Service。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 15 章。

实现重点：
- 按状态分 6 列
- Redis 缓存 5 分钟
- 缓存失效触发
- 精简 VO 字段

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

---

## 15.9 T11 执行结果

> 模板章节，Claude Code 执行 T11 后填写。

---

## 16. T12 Claude Code 任务单：甘特图 Service

### 16.1 任务目标

实现甘特图数据组装：按项目输出 tasks/links/dateRange，含里程碑。Redis 缓存 10 分钟。

### 16.2 必须先阅读

```text
T1-T11 结果
7.8 GanttDataVO 定义
重构文档 3.3.3 甘特图数据结构
```

### 16.3 允许修改

| 路径 | 说明 |
|------|------|
| `code/backend/oa-task/src/main/java/**` | 新增 GanttService、GanttController |

### 16.4 必须实现的服务能力

| 能力 | 要求 |
|------|------|
| `getGanttData` | 单项目甘特图：根节点=项目，子节点=顶级任务，叶子=子任务/孙任务；links 为依赖连线 |
| `computeDateRange` | 根据项目计划起止 + 实际任务起止动态计算 |
| `invalidateGanttCache` | 任务/依赖变化时清除 |

### 16.5 数据组装规则

```
根节点: GanttTaskVO(type=project)
  ├─ 顶级任务（parent_task_id=null）
  │    ├─ 子任务（parent_task_id=顶级任务）
  │    └─ 子任务
  └─ 顶级任务
       └─ 子任务

links: 
  - 当前任务 → 依赖任务（带 dependency_type 描述）

dateRange:
  - start = min(项目 planned_start, 所有任务 planned_start)
  - end = max(项目 planned_end, 所有任务 planned_end)
```

### 16.6 测试要求

| 测试 | 场景 |
|------|------|
| 单项目甘特图 | 树结构正确 |
| 依赖连线 | links 正确 |
| 日期范围 | 动态计算 |
| 缓存命中 | 第二次查询 Redis |
| 里程碑任务 | type=milestone |

### 16.7 验收命令

```powershell
cd code/backend
mvn -pl oa-task -am test
```

### 16.8 可直接交给 Claude Code 的提示词

```text
请执行 oa-task 模块重构 T12：甘特图 Service。

严格遵循 docs/superpowers/specs/2026-06-02-task-project-management-task-split.md 第 16 章。

实现重点：
- 三层树结构（项目→任务→子任务）
- 依赖连线 links
- 动态 dateRange
- Redis 缓存 10 分钟
- 里程碑 type

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

---

## 16.9 T12 执行结果

> 模板章节，Claude Code 执行 T12 后填写。

---

## 17. 后续波次任务单提示词索引

### T6 子任务 + 排序

```text
请执行 oa-task 模块重构 T6：子任务 + 排序。

实现重点：
- 子任务创建/批量改父/排序
- 父子循环校验（A 是 B 的父，B 不能是 A 的父）
- depth 字段维护
- has_subtask 冗余字段同步

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

### T8 任务提醒

```text
请执行 oa-task 模块重构 T8：任务提醒。

实现重点：
- @Scheduled 定时任务，每小时扫描
- 即将到期（planned_end - now <= 24h 且未完成）发布 TaskDueSoonEvent
- 已逾期（planned_end < today 且 status != DONE）发布 TaskOverdueEvent
- oa-message 订阅事件，发送站内消息

完成后运行：
cd code/backend
mvn -pl oa-task,oa-message,oa-web -am test
```

### T13 我的任务合并视图

```text
请执行 oa-task 模块重构 T13：我的任务聚合。

实现重点：
- 四维度过滤（TODO/DONE/OWNED/JOINED）
- assignee_id=me OR creator_id=me OR mentioned in comment
- 支持多维筛选 + 排序
- 分页 + 索引命中

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

### T14 日历视图

```text
请执行 oa-task 模块重构 T14：日历视图。

实现重点：
- 按 planned_start/planned_end 输出
- 月/周/日三种模式
- 自然日展示（暂不处理工作日规则）
- 任务跨日展示

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

### T15 任务附件

```text
请执行 oa-task 模块重构 T15：任务附件。

实现重点：
- 上传/下载/删除/列表
- 复用现有 uploads/task/ 目录
- 单文件 ≤ 50MB
- 文件类型白名单

完成后运行：
cd code/backend
mvn -pl oa-task,oa-web -am test
```

### T16 任务操作日志

```text
请执行 oa-task 模块重构 T16：任务操作日志。

实现重点：
- @TaskLog 注解 + AOP 切面
- CREATE/UPDATE/STATUS/PROGRESS/ASSIGN/DEPENDENCY/HOURS/COMMENT/ATTACHMENT
- 字段级变更追踪
- 日志查询 API（分页 + 过滤）

完成后运行：
cd code/backend
mvn -pl oa-task -am test
```

### T17 Web 接入

```text
请执行 oa-task 模块重构 T17：Web 接入。

实现重点：
- src/api/task.ts typed API（10+ 接口）
- 5 个核心页面：项目列表/详情、看板、甘特图、我的任务、工时统计
- 权限过滤
- 拖拽看板持久化（PUT status）

完成后运行：
cd code/frontend
pnpm typecheck && pnpm build
```

### T18 Mobile 接入

```text
请执行 oa-task 模块重构 T18：Mobile 接入。

实现重点：
- src/api/task.ts typed API
- 4 个核心页面：我的任务列表、任务详情、记录工时、任务评论
- @人员选择器（移动端）
- 评论输入框

完成后运行：
cd code/mobile
pnpm build:h5
```

### T19 端到端回归

```text
请执行 oa-task 模块重构 T19：端到端回归。

测试场景：
1. 登录 → 创建项目 → 添加成员
2. 创建任务（含子任务） → 设置依赖 → 记录工时
3. 评论@ → 消息通知
4. 状态完成 → 父任务进度更新 → 项目进度更新
5. 看板拖拽 → 状态变更
6. 甘特图渲染 → 依赖线显示
7. 我的任务聚合视图正确
8. 附件上传/下载
9. 操作日志可查

通过条件：所有场景通过；CommandLineRunner 测试脚本输出 PASS。
```

### T20 演示数据与上线准备

```text
请执行 oa-task 模块重构 T20：演示数据 + 上线准备。

输出：
- code/backend/sql/baseline/006_seed_task_demo.sql
  - 1 个演示项目 + 3 个成员
  - 10 个任务（含子任务和依赖）
  - 5 条工时记录
  - 3 条评论
  - 2 个附件元信息
  - 1 条操作日志
- 菜单 JSON：任务项目根节点 + 5 个子菜单
- 权限码初始化 SQL
- 上线检查清单
```

---

## 18. 风险与回滚

### 18.1 全局风险

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| 看板/甘特图性能 | 中 | 中 | 缓存 + 字段精简 + 限制项目任务数 |
| 依赖循环检测深递归 | 低 | 中 | 限制深度 5000 + 早停 |
| 工时与实际进度同步 | 中 | 中 | 事务保证 + 写日志 |
| 旧 oa_todo 用户认知冲突 | 中 | 低 | 菜单命名区分 + 文档说明 |
| 附件上传/下载权限 | 中 | 中 | 复用现有 auth + 任务权限 |

### 18.2 回滚点

| 阶段 | 回滚方式 |
|------|----------|
| T3 失败 | 移除 005_task_module.sql，停止 oa-task 服务 |
| T4-T7 失败 | 不暴露 /api/task 路由，菜单不显示 |
| T11/T12 失败 | 保留基础 CRUD，回退到列表视图 |
| T17/T18 失败 | 前端隐藏任务菜单，回退到 oa_todo |

---

## 19. 附录

### 19.1 关键决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 表前缀 | `task_*` | 统一模块前缀 |
| 实际工时存储 | 独立 `task_hours` 表 + 冗余字段 | 既支持详细记录又加速查询 |
| 子任务层级 | 邻接表 + depth 字段 | 避免递归 CTE，便于分页 |
| 依赖类型 | 4 种（FS/SS/FF/SF） | 项目管理标准做法 |
| 依赖循环检测 | DFS + 深度限制 | 实现简单，性能可控 |
| 看板/甘特图 | 后端组装 + Redis 缓存 | 前端无状态，易扩展 |
| 工时审批 | 不实现 | 任务管理简化，不走流程 |
| 跨项目甘特图 | 不实现 | 复杂度高，后续迭代 |
| 工作日历 | 不实现 | 第一版按自然日 |

### 19.2 关联模块

| 模块 | 关系 |
|------|------|
| oa-message | 任务分配/被@/截止提醒 |
| oa-workflow | 任务状态变更可触发流程（后续迭代） |
| oa-hr | 员工信息只读引用 |
| oa-finance | 工时统计可对接报销（后续） |

### 19.3 上线顺序建议

1. dev 环境跑通 T1-T16 完整闭环
2. T17 Web 灰度，先开放项目列表 + 任务 CRUD
3. T18 Mobile 灰度，先开放我的任务
4. 全量上线看板 + 甘特图
5. 收集反馈后迭代 V1.1

### 19.4 不在本模块范围的 V1.1+ 候选

- 工作日历与节假日
- 任务模板
- 自动排期（关键路径）
- 任务审批流（接入 oa-workflow）
- 跨项目甘特图
- 工时审批 + 报销对接
- ES 全文检索
- 任务 AI 助手
- 子任务自动拆解

