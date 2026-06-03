# Claude Code 企业OA重构工作流

> 适用项目: Corporate OA System  
> 适用日期: 2026-06-02 起  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`

---

## 1. 工作流目标

本工作流用于指导 Claude Code 在本项目中执行全面重构任务，确保每次改动都能对齐目标架构、当前仓库状态和验证门禁。

核心目标：

1. 从当前“旧聚合模块 + 新DDD模块并存”的状态，逐步迁移到务实DDD + 工作流微内核架构。
2. 控制 Claude Code 的修改范围，避免并行任务互相覆盖。
3. 每个任务都产出可验证的代码、脚本、文档或测试。
4. 保证后端、Web端、移动端、数据库脚本、CI/CD 同步演进。

---

## 2. 项目上下文速读

Claude Code 每次开始较大任务前，必须先阅读：

| 文件/目录 | 用途 |
|-----------|------|
| `CLAUDE.md` | 项目约定、技术栈、启动命令、已知注意事项 |
| `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` | 全面重构目标设计 |
| `docs/superpowers/specs/2026-06-01-full-optimization-plan-complete.md` | 当前质量、安全、性能问题清单 |
| `code/backend/pom.xml` | 后端模块清单和依赖版本 |
| `code/frontend/package.json` | Web端构建、类型检查、依赖 |
| `code/mobile/package.json` | 移动端构建、uni-app目标 |
| `.claude/workflows/` | 已有 Claude workflow 脚本 |

当前关键事实：

| 维度 | 事实 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + Maven 多模块 |
| 数据库 | MySQL 8.0，重构目标为空库重建 |
| 缓存 | Redis + Redisson |
| 搜索 | Elasticsearch 8.x，允许先以接口抽象占位 |
| Web | Vue 3 + TypeScript + Element Plus + Tailwind CSS 4 + Vite 6 |
| Mobile | uni-app Vue 3，H5 + 微信小程序 |
| 架构状态 | 新模块已经出现，但旧 `oa-model`、`oa-mapper`、`oa-service` 仍在 |

---

## 3. 总体执行模式

所有重构任务使用以下 7 阶段闭环：

```
Discover -> Decide -> Contract -> Implement -> Verify -> Document -> Handoff
```

| 阶段 | Claude Code 必做动作 | 输出 |
|------|----------------------|------|
| Discover | 阅读相关文档、POM/package、旧代码、测试、SQL | 当前状态摘要 |
| Decide | 判断是否新建、迁移、合并或删除旧实现 | 任务边界和不修改范围 |
| Contract | 先定 API、表结构、DTO/VO、事件、权限码 | 可评审契约 |
| Implement | 小步修改后端、前端、移动端、SQL、测试 | 代码和脚本 |
| Verify | 执行最小可行命令，失败则修复或记录原因 | 验证结果 |
| Document | 同步设计文档、README、接口说明、变更记录 | 文档补丁 |
| Handoff | 汇总改动、风险、未完成项、下一步建议 | 交接说明 |

---

## 4. 推荐使用的 `.claude/workflows`

本仓库已有多个 workflow 脚本。重构时建议按任务规模选择。

| 场景 | 推荐 workflow | 说明 |
|------|---------------|------|
| 需求不清或边界大 | `requirements-confirm.js` | 先生成需求确认和验收标准 |
| 大模块拆分 | `task-splitter.js` | 将一个模块拆成后端、前端、移动端、SQL、测试等子任务 |
| 普通功能开发 | `dev-to-prod.js` | 从开发到验证的完整流程 |
| 质量检查 | `qa-check.js` | 做局部质量检查 |
| Diff审查 | `review-diff.js` | 合并前审查改动风险 |
| 深度CI评估 | `deep-ci-eval.js` | 检查构建、测试、CI门禁 |
| 全面优化计划 | `full-optimization-plan.js` | 适合跨域优化规划，不适合作为单个实现任务 |
| 发布准备 | `ship-to-product.js` | 上线前整理验证和发布事项 |

注意：如果 workflow 脚本中出现非本项目路径、非本项目技术栈描述，必须以 `CLAUDE.md` 和本工作流文档为准。

---

## 5. 任务类型模板

### 5.1 后端模块迁移任务

适用：迁移 HR、Finance、Admin、Meeting、Task、Document、Knowledge、Message 等模块。

执行顺序：

1. 读取目标章节：重构文档对应模块设计。
2. 查旧实现：`oa-model`、`oa-mapper`、`oa-service`、`oa-web/src/main/java/cn/oa/controller`。
3. 查新模块：确认 `oa-{module}` 是否已有代码和 POM 依赖。
4. 先补契约：Entity、DTO、VO、Mapper、Service接口、Controller路径、权限码。
5. 再迁业务逻辑：只迁目标闭环，不顺手改无关模块。
6. 补测试：Service 单元测试 + Controller 切片或集成测试。
7. 更新 SQL：基线或 patch 脚本。

验收命令：

```bash
cd code/backend
mvn -pl oa-{module},oa-web -am test
```

如果改动了公共平台、工作流或父POM：

```bash
cd code/backend
mvn clean test
```

### 5.2 工作流引擎任务

适用：状态机、审批人解析、会签、加签、驳回、委托、回调。

执行顺序：

1. 先读第 2 章“工作流微内核引擎”。
2. 明确任务属于 model、mapper、core、api 哪一层。
3. 状态机和算法先写单元测试，再写实现。
4. 涉及业务回调时，只依赖接口或事件，不直接依赖具体业务模块实现。
5. 涉及并发审批时，必须说明锁粒度、事务边界、幂等策略。

最小测试要求：

| 能力 | 必测场景 |
|------|----------|
| 状态机 | 合法转换、非法转换、终态不可变 |
| 审批人解析 | 固定人、岗位、部门负责人、表单字段、空审批人策略 |
| 会签 | 全员通过、一人拒绝、比例通过、重复提交、并发提交 |
| 驳回 | 直接返回、逐级返回、重提后节点定位 |
| 委托 | 委托有效期、委托人审计、循环委托拒绝 |

### 5.3 数据库脚本任务

适用：建表、索引、种子数据、脚本收敛。

执行顺序：

1. 先查 `code/backend/sql/` 中是否已有表或 seed。
2. 确认脚本属于 baseline 还是 patches。
3. 所有表使用统一字段规范：`id`、`del_flag`、`create_by`、`create_time`、`update_by`、`update_time`。
4. 为列表查询、待办查询、业务关联、状态筛选补复合索引。
5. 给关键 SQL 写 `EXPLAIN` 验收说明。

验收要求：

| 项目 | 要求 |
|------|------|
| 建表 | 空库可一次执行成功 |
| seed | dev/test 可重复执行或先清理目标数据 |
| 索引 | 说明服务于哪个查询场景 |
| 回滚 | patch 必须说明反向操作 |

### 5.4 Web前端任务

适用：管理端页面、API、路由、组件、权限。

执行顺序：

1. 查 `code/frontend/src/api` 是否已有接口文件。
2. 查 `code/frontend/src/views` 是否已有页面入口。
3. API 类型先行，优先使用 `interface`。
4. 页面遵循现有 Element Plus + Vue Composition API 风格。
5. 修改路由/菜单时同步权限码和角色。
6. 新增复杂组件时，拆成可复用组件，避免页面内巨型脚本。

验收命令：

```bash
cd code/frontend
pnpm typecheck
pnpm build
```

### 5.5 移动端任务

适用：uni-app H5 / 微信小程序页面。

执行顺序：

1. 查 `code/mobile/src/pages.json`，确认页面注册方式。
2. 查 `code/mobile/src/api` 是否已有对应接口。
3. 优先实现员工端闭环，不做复杂后台配置页面。
4. 使用 `uni.request` 封装，不绕过统一请求层。
5. 注意小程序兼容性，避免使用 Web-only API。

验收命令：

```bash
cd code/mobile
pnpm build:h5
```

微信小程序相关改动还需要人工或自动化真机/开发者工具验证。

### 5.6 安全与权限任务

适用：JWT、Redis会话、RBAC、数据权限、敏感信息。

执行顺序：

1. 先检查是否涉及 P0/P1 安全项。
2. 后端接口必须标注角色或权限要求。
3. 前端隐藏菜单不能替代后端鉴权。
4. 数据权限必须在后端实现。
5. 禁止新增默认密钥、硬编码密码、明文生产凭据。

验收重点：

| 项目 | 要求 |
|------|------|
| 认证 | Token 无效、过期、登出、改密后失效 |
| 授权 | ADMIN/USER 权限隔离，越权返回 403 |
| 数据权限 | 普通用户只能看自己或授权部门数据 |
| 敏感数据 | 手机、身份证、银行卡、薪资按权限脱敏 |

---

## 6. 单个任务提示词模板

给 Claude Code 分配任务时，建议使用以下结构：

```text
目标：
迁移/实现 {模块或功能}，对齐 docs/superpowers/specs/2026-06-02-oa-system-redesign.md 的 {章节号}。

范围：
- 后端路径：
- 前端路径：
- 移动端路径：
- SQL路径：
- 测试路径：

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md
- 相关旧实现：
- 相关新模块：

禁止修改：
- 与本任务无关的模块
- 未提及的 CI/CD 文件
- 用户已有未提交改动

输出要求：
- API契约
- 实现代码
- 数据库脚本
- 测试
- 文档更新

验收命令：
- cd code/backend && mvn -pl ... -am test
- cd code/frontend && pnpm typecheck

完成后汇报：
- 改了什么
- 如何验证
- 剩余风险
```

---

## 7. 并行开发规则

适合并行的任务：

| 可并行项 | 条件 |
|----------|------|
| 不同业务模块后端 | 表前缀、包路径、权限码不冲突 |
| Web页面和后端API | API契约已冻结 |
| 移动端页面和Web页面 | 共享接口已冻结 |
| 测试补充和文档补充 | 不修改同一实现文件 |

不适合并行的任务：

| 不并行项 | 原因 |
|----------|------|
| 父POM依赖调整 | 容易造成构建冲突 |
| 统一响应/异常/鉴权改造 | 影响所有模块 |
| 数据库基线DDL大改 | 多人改同一脚本冲突高 |
| 工作流状态机核心 | 并发修改算法风险高 |
| 路由和菜单树重构 | 前后端权限容易漂移 |

并行任务必须指定 owner：

| owner | 负责范围 |
|-------|----------|
| backend-owner | Entity、Mapper、Service、Controller、测试 |
| db-owner | DDL、索引、seed、脚本说明 |
| web-owner | Web API、页面、路由、组件 |
| mobile-owner | uni-app API、页面、pages.json |
| qa-owner | 测试计划、E2E、回归清单 |
| docs-owner | 设计文档、交接说明、变更记录 |

---

## 8. 提交前检查清单

每次 Claude Code 完成任务前必须检查：

| 检查项 | 结果 |
|--------|------|
| 是否只修改了任务范围内文件 | 必须 |
| 是否保留用户已有未提交改动 | 必须 |
| 是否新增重复实体/Service/API | 禁止 |
| 是否有硬编码密钥、密码、Token | 禁止 |
| 是否有未解释的跳过测试 | 禁止 |
| 是否更新对应文档或注释 | 必须 |
| 是否运行最小验收命令 | 必须，不能运行需说明原因 |
| 是否记录剩余风险和下一步 | 必须 |

---

## 9. 推荐里程碑

| 里程碑 | 目标 | 主要产出 |
|--------|------|----------|
| M0 | 重构准备 | 文档补强、workflow、模块清单、数据库脚本收敛计划 |
| M1 | 平台与工作流可用 | platform、security、workflow 核心、测试 |
| M2 | 核心业务闭环 | HR请假、Finance报销、Message通知、Todo待办 |
| M3 | Web管理端闭环 | 登录、菜单、待办、审批、核心业务页面 |
| M4 | 移动端员工闭环 | 登录、首页、申请、审批、消息 |
| M5 | 质量门禁 | CI、E2E、安全扫描、性能基线 |
| M6 | 发布准备 | Docker、环境配置、监控、回滚文档 |

---

## 10. 常用验证命令

```bash
# 后端全量测试
cd code/backend
mvn clean test

# 后端指定模块测试
cd code/backend
mvn -pl oa-hr,oa-web -am test

# 后端跳过测试构建，仅用于定位编译问题
cd code/backend
mvn clean package -DskipTests

# Web 类型检查与构建
cd code/frontend
pnpm typecheck
pnpm build

# 移动端 H5 构建
cd code/mobile
pnpm build:h5
```

PowerShell 环境下如果使用 `&&` 兼容性不稳定，应分开执行 `cd` 和命令。

---

## 11. 最终交接格式

Claude Code 完成重构任务后，最终回复应包含：

```text
完成内容：
- ...

验证：
- 已运行 ...
- 未运行 ...，原因 ...

风险：
- ...

下一步：
- ...
```

不要只说“已完成”。必须说明可验证证据和剩余风险。
