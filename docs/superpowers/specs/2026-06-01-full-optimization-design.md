# 全面优化：全端审计驱动优化规格文档

> 版本: 1.0 | 日期: 2026-06-01 | 覆盖: 后端 316 文件 + 前端 121 文件 + 移动端 uni-app

## 1. 优化目标

对 Corporate OA System 进行全端系统性优化，覆盖安全、Bug、性能、代码质量、架构、测试、前端体验、文档 8 个维度。采用 **审计→修复→验证** 三阶段策略，使用 Workflow 多 Agent 并行执行。

## 2. 项目全景

| 层 | 技术栈 | 规模 |
|----|--------|------|
| 后端 | Spring Boot 3.4.5, MyBatis-Plus 3.5.9, Redis, JWT | 35 Controllers, 42 Services, 70+ Entities/DTOs/VOs |
| 前端 | Vue 3 + Element Plus + TypeScript + Tailwind CSS 4 | 121 文件, 37 API 模块 |
| 移动端 | uni-app (Vue 3), H5 + 微信小程序 | 12 API 模块, 20 页面 |
| 数据库 | MySQL 8.0, 约 50 表 | 4 大域: 系统/OA/工作流/业务扩展 |

## 3. 审计阶段详细清单

### 3.1 后端安全审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| S-01 | 越权检查覆盖 | Controller 方法缺少 `@RequireAdmin` 或 `@RequireRole` | **高** |
| S-02 | isAdmin() 实现缺陷 | `roles.toString().contains("ADMIN")` 子串匹配可能误判 (如 "SUPER_ADMIN" 角色) | **高** |
| S-03 | isAdmin() 重复实现 | 8+ Controller 中重复相同的 isAdmin 方法 | 中 |
| S-04 | 两种 admin 检查路径不一致 | Controller 用 Redis isAdmin() vs WorkflowServiceImpl 用 DB isAdminUser() | **高** |
| S-05 | 硬编码权限映射 | AuthInterceptor 中静态 Map, 非数据库驱动 | 中 |
| S-06 | @RequirePermission 无效 | LoginVO 返回 `permissions = ["*:*:*"]` 使前端权限过滤失效 | 中 |
| S-07 | 密码字段泄露 | SysEmployee 返回体含 password 字段 | **高** |
| S-08 | 文件上传安全 | 路径穿越、类型白名单、大小限制 | **高** |
| S-09 | SQL 注入 | MyBatis `${}` 拼接使用情况 | **高** |
| S-10 | JWT 弱密钥 | 默认 `application.yml` 中 fallback 密钥 | 中 |
| S-11 | Access/Refresh Token 相同 | 同一 JWT 用作 access 和 refresh token | 中 |
| S-12 | WebSocket 认证 | 连接时 JWT 验证完整性 | 中 |
| S-13 | WebSocket 来源硬编码 | `setAllowedOrigins` 硬编码 localhost 地址 | 低 |

### 3.2 后端 Bug 审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| B-01 | 多角色权限检查短路 | AuthInterceptor 权限循环在第一个角色后 break | **高** |
| B-02 | 条件引擎浮点比较 | numCheck() 使用 `==` 比较 double, 整数条件可能误判 | 中 |
| B-03 | 审批人非确定性选择 | resolveByRole() 始终选第一个匹配用户 | 中 |
| B-04 | N+1 查询 | WorkflowServiceImpl.myPendingTasks() 等遍历 selectById | **高** |
| B-05 | 静默吞异常 | resolveDeptManager 中 `catch (NumberFormatException ignored)` | 中 |
| B-06 | WebSocket 会话泄漏 | 传输错误未清理 sessions 映射 | 中 |
| B-07 | WebSocket 无心跳 | 无 keepalive, 代理可能关闭连接 | 中 |
| B-08 | 导出静默截断 | 5000 条上限, 超量静默丢弃 | 中 |
| B-09 | 死参数 | BusinessTripServiceImpl.approve() 有未使用的 taskId 参数 | 低 |
| B-10 | 状态码重复注释 | createSubprocess 中 "Find the subprocess definition" 注释重复 | 低 |

### 3.3 后端性能审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| P-01 | N+1 循环查库 | 审批历史、待办任务列表逐条查 employee/instance | **高** |
| P-02 | 索引缺失 | 按 empId/status 查询的大表缺少索引 | 中 |
| P-03 | 缓存穿透 | 热点数据(部门树、字典)未缓存或缓存未生效 | 中 |
| P-04 | 大查询无分页 | 导出方法查 5000 条一次性加载到内存 | 中 |
| P-05 | Redis key 未规范 | 检查 Redis key 模式是否统一 | 低 |

### 3.4 后端代码质量审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| Q-01 | isAdmin() 重复代码 | 8+ Controller 重复同样方法 | **高** |
| Q-02 | 分页授权检查重复 | 7 个 Controller page() 方法相同授权逻辑 | **高** |
| Q-03 | 导出方法重复 | 7 个 Controller 导出方法结构相同 | 中 |
| Q-04 | STATUS_TEXT 数组重复 | 7 个 Controller 各自定义状态映射, 内容有差异 | 中 |
| Q-05 | 魔法数字 | 工作流状态码 "0"-"5" 散布在代码中 | 中 |
| Q-06 | 长方法 | WorkflowServiceImpl.startProcess() 130 行, handleTask() 115 行 | 中 |
| Q-07 | 大型类 | WorkflowServiceImpl 1367 行 | 中 |
| Q-08 | 多段自动批准逻辑重复 | startProcess() 中 4 段类似自动批准路径 | 中 |
| Q-09 | 条件引擎内联 | ConditionEvaluator 嵌入 WorkflowServiceImpl | 中 |
| Q-10 | 字段注入 | NotificationServiceImpl 使用 @Autowired 字段注入 | 低 |
| Q-11 | 状态类型不一致 | 实体 Integer status vs 工作流 String status | 中 |
| Q-12 | @JsonFormat 不一致 | 有的 LocalDateTime 有注解, 有的没有 | 中 |

### 3.5 后端架构审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| A-01 | API 设计一致性 | 统一 R 响应、统一分页参数、统一异常处理 | 低 |
| A-02 | 模块依赖合规 | common→model→mapper→service→web 方向 | 低 |
| A-03 | DTO/VO 使用规范 | Controller 参数是否使用 DTO, 返回是否用 VO | 中 |
| A-04 | 工作流引擎健壮性 | 节点配置验证、循环检测、死锁预防 | 中 |

### 3.6 前端审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| F-01 | JWT 解析重复且不一致 | store/user.ts (含 Unicode) vs request.ts (无 Unicode) vs router/index.ts (无 Unicode) | **高** |
| F-02 | Token 刷新孤立请求 | 刷新失败时 pendingRequests 的 Promise 永不 resolve | **高** |
| F-03 | 类型全部可选 | types/api.ts 中 PageResult/Role/Menu 等所有字段带 `?` | 中 |
| F-04 | 字段重复 | Role.name vs roleName, Menu.sort vs orderNum, Notice.publisher vs publisherName | 中 |
| F-05 | create/update 同 URL | createDefinition 和 updateDefinition 用相同 POST URL | 中 |
| F-06 | baseURL 硬编码 | 空字符串, 无环境变量配置 | 中 |
| F-07 | Token 双重存储 | 同时存在 Pinia state 和 localStorage, XSS 风险 | 中 |
| F-08 | 路由守卫不刷新 Token | Token 过期直接踢到登录, 不尝试 refresh | 中 |
| F-09 | API 代码风格不一致 | leave.ts 有 return vs workflow.ts 无 return | 低 |
| F-10 | 分页类型缺失字段 | PageResult 缺少 pageNum/pageSize/pages | 低 |

### 3.7 移动端审计

| # | 检查项 | 预期发现 | 风险等级 |
|---|--------|----------|----------|
| M-01 | API 全部为 any 类型 | 无泛型, 无类型安全 | **高** |
| M-02 | 上传不刷新 Token | upload() 收到 401 直接跳登录, 不尝试刷新 | 中 |
| M-03 | 无全局加载指示器 | 无 loading 消抖处理 | 低 |
| M-04 | 无共享类型 | API 响应类型未定义 | 中 |

## 4. 修复阶段详细方案

### 4.1 P0: 安全漏洞修复

#### S-01: 越权检查全覆盖
- 扫描所有 Controller 中需要鉴权但缺少 `@RequireAdmin`/`@RequireRole` 的方法
- 对涉及他人数据查询的 page 方法添加数据权限检查
- **涉及文件**: `oa-web/controller/*Controller.java` (35 个)

#### S-02: isAdmin() 子串匹配修复 + S-03: 提取公共方法
- 在 `WebUtil` 或 `AuthUtil` 中添加公共的 `isAdmin(Long empId)` 静态方法
- 使用精确匹配 (JSON 解析或 Set 比较) 替代 `contains()`
- 在所有 Controller 中替换重复实现
- **涉及文件**:
  - 创建: `oa-common/src/main/java/cn/oa/common/utils/AuthUtil.java`
  - 修改: BusinessTripController, EmployeeController, ExpenseController, LeaveApplyController, LoanController, OutingController, OvertimeController, PurchaseController, WorkflowController

#### S-04: 统一 Admin 检查机制
- 将 WorkflowServiceImpl.isAdminUser() 改为使用 Redis (与 Controller 一致)
- 确保角色变更时 Redis 缓存刷新
- **涉及文件**: `WorkflowServiceImpl.java`

#### S-07: 密码字段保护
- 在 SysEmployee 中添加 `@JsonProperty(access = WRITE_ONLY)` (已在当前 WIP)
- 确保所有返回用户的接口都不泄露密码
- **涉及文件**: `SysEmployee.java`

#### S-08: 文件上传安全
- 添加文件类型白名单、大小限制、路径穿越防护 (已在当前 WIP)
- **涉及文件**: `DocumentServiceImpl.java`

#### S-10/S-11: JWT 安全
- 分离 Access Token (2h) 和 Refresh Token (7d)
- 强制生产环境设置 JWT_SECRET 环境变量
- **涉及文件**: `JwtUtil.java`, `AuthServiceImpl.java`

### 4.2 P1: Bug 修复

#### B-01: 多角色权限检查修复
- 修复 AuthInterceptor 中权限检查循环的 break 问题
- 改为遍历所有角色, 任一角色有权限即可
- **涉及文件**: `AuthInterceptor.java`

#### B-02: 条件引擎 double 比较修复
- 使用 BigDecimal 或阈值比较替代 `==` 操作符
- **涉及文件**: `WorkflowServiceImpl.java`

#### B-05: 日志吞异常修复
- 为静默 catch 添加日志输出
- **涉及文件**: `WorkflowServiceImpl.java`

#### B-06: WebSocket 会话清理
- 添加 `handleTransportError` 覆盖, 清理失效会话
- **涉及文件**: `NotificationEndpoint.java`

### 4.3 P2: 性能优化

#### P-01: N+1 查询修复
- WorkflowServiceImpl 中 myPendingTasks/myHandledTasks/getApprovalHistory 改用 `selectBatchIds`
- **涉及文件**: `WorkflowServiceImpl.java`

#### P-03: 缓存策略优化
- 部门树、字典等热点数据添加 @Cacheable
- 检查 Redis key 过期时间配置
- **涉及文件**: `DeptServiceImpl.java`, `DictDataServiceImpl.java`

### 4.4 P3: 代码质量与架构

#### Q-01/Q-02/Q-03: 重复代码提取
- `AuthUtil.isAdmin()` 公共方法
- `WebUtil.enforceOwnDataAccess(request, empId)` 公共授权检查
- `WebUtil.buildEmployeeNameMap(mapper, ids)` 公共员工名映射
- **涉及文件**: 多个 Controller

#### Q-06/Q-07: 大型方法/类拆分
- 从 WorkflowServiceImpl 提取 `ConditionEvaluator`, `TaskResolver`, `NodeRouter`
- **涉及文件**: `WorkflowServiceImpl.java`, 新文件 3 个

#### Q-08: 合并自动批准路径
- 统一 startProcess() 中 4 段自动批准路径为单一方法
- **涉及文件**: `WorkflowServiceImpl.java`

#### Q-11/Q-12: 类型一致性
- 统一 Integer status (建议业务实体使用 Integer, 工作流实体统一为 Integer)
- 统一 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") 覆盖所有 LocalDateTime
- **涉及文件**: 实体类批量调整

### 4.5 P4: 前端修复

#### F-01: JWT 解析提取公共函数
- 创建 `src/utils/jwt.ts` 工具函数, 包含 Unicode 支持
- 替换 3 处重复实现
- **涉及文件**: `store/user.ts`, `utils/request.ts`, `router/index.ts`, 新 `utils/jwt.ts`

#### F-02: Token 刷新失败处理修复
- 刷新失败时遍历 pendingRequests 全部 reject
- **涉及文件**: `utils/request.ts`

#### F-05: create/update 同 URL 修复
- 确认后端是否区分 RESTful PUT vs POST
- **涉及文件**: `api/workflow.ts`

#### F-07: Token 存储简化
- 移除 userInfo 中的 token 字段, 仅存 localStorage token 键
- **涉及文件**: `store/user.ts`, `types/api.ts`

### 4.6 P4: 移动端修复

#### M-01: API 添加类型
- 创建共享类型定义, 添加泛型支持
- **涉及文件**: `mobile/src/api/*.ts`

#### M-02: 上传函数添加刷新
- upload() 函数收到 401 时尝试刷新 token
- **涉及文件**: `mobile/src/utils/request.ts`

## 5. 修复原则

1. **最小改动**: 只改动需要修复的代码, 不做无关重构
2. **保持风格**: 遵循项目现有编码风格
3. **编译保障**: 每次变更后执行 `mvn compile` 验证
4. **分批提交**: 每批修复有明确 scope 和 commit message
5. **不宜变动**: 工作流引擎核心逻辑 (节点流转、条件评估) 只修复 Bug, 不重构设计

## 6. 验证阶段

1. Maven 编译: `mvn compile -pl oa-web -am`
2. 前端构建: `cd code/frontend && pnpm build`
3. 变更汇总: 按优先级列出所有变更
4. 未修复说明: 需要人工决策的事项

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 审计漏报 | 多角度交叉审计 (安全/Bug/质量重叠覆盖) |
| 修复引入新 Bug | 编译验证 + 修复范围最小化 |
| 并行修复冲突 | 每个修复使用独立 agent 上下文, 不互相干扰 |
| 工作流核心逻辑破坏 | 只修 Bug, 不改设计 |

## 8. 输出物

- 审计报告 (每个 agent 的发现汇总)
- 修复 commit (按 P0-P4 分批)
- 变更汇总表
