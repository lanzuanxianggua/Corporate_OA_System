# Corporate OA System -- 全面优化方案

> 生成日期: 2026-06-01
> 基于 8 维度并行审计 (后端安全/Bug/性能/前端/移动端/CI-CD/测试方案/综合方案)
> 审计范围: 后端 319 文件 + 前端 122 文件 + 移动端 39 文件 + CI/CD 3 Workflows

---

## 一、项目当前状态评估

### 1.1 项目规模

| 维度 | 指标 | 数值 |
|------|------|------|
| **后端** | Java 源文件 | 319 |
| | Controller | 35 |
| | Service 接口 + 实现 | 84 |
| | Mapper 接口 | 48 |
| | Mapper XML (自定义SQL) | 0 (全部走 MyBatis-Plus 自动映射) |
| | 模块数 | 6 (oa-common, oa-model, oa-mapper, oa-service, oa-web, oa-parent) |
| **前端** | Vue 页面 | 72 |
| | TypeScript 源文件 | 50 |
| | 路由数量 | ~40+ |
| | 菜单入口 | ~30+ |
| **移动端** | Vue 页面 (uni-app) | 21 |
| | TypeScript 源文件 | 18 |
| | 页面类型 | 登录/首页/待办/7种申请/3种列表/日程/文档/消息/个人 |
| **CI/CD** | GitHub Actions Workflows | 3 (backend.yml, frontend.yml, api-test.yml) |
| | 现有后端测试 | 26 ControllerTest + 1 BaseControllerTest |
| | 现有 E2E 测试 | 2 Playwright spec (frontend + mobile) + 1 API test shell |

### 1.2 测试覆盖率现状

| 层级 | 现状 | 严重问题 |
|------|------|---------|
| 后端单元测试 | 仅存在 Controller 切片测试 (27个) | Service 层 **0 测试**，Mapper 层 **0 测试** |
| 后端集成测试 | 不存在 | 无 @SpringBootTest 集成测试 |
| 前端 E2E | 1 个 spec 文件 (frontend-ui-test.spec.ts) | 仅覆盖基本登录，无业务场景 |
| 移动端 E2E | 1 个 spec 文件 (mobile-ui-test.spec.ts) | 仅覆盖基本登录 |
| API 测试 | 1 个 shell 脚本 | 无结构化断言 |
| 代码覆盖率 | 未配置 JaCoCo | 无量化指标 |

### 1.3 CI/CD 现状

| 维度 | 现状 | 问题 |
|------|------|------|
| 后端构建 | `mvn -Dmaven.test.skip=true` | 测试完全被跳过 |
| 前端构建 | `pnpm build` 无类型检查 | TypeScript 错误无声进入 master |
| API 测试 | 启动完整环境 + curl 断言 | 与 backend.yml 重复构建 |
| 安全扫描 | 无 Dependabot / CodeQL | 依赖漏洞无告警 |
| 代码质量 | 无 Checkstyle / SpotBugs / ESLint | 纯靠人工纪律 |

### 1.4 审计发现汇总

| 审计维度 | 发现总数 | Critical | High | Medium | Low |
|----------|---------|----------|------|--------|-----|
| 安全审计 | 19 | 5 | 6 | 5 | 3 |
| Bug 审计 | 29 | 8 | 10 | 8 | 3 |
| 性能审计 | 17 | 2 | 6 | 7 | 2 |
| 前端审计 | 26 | 7 | 11 | 5 | 3 |
| 移动端审计 | 24 | 6 | 6 | 7 | 5 |
| **合计** | **115** | **28** | **39** | **32** | **16** |

---

## 二、优化优先级矩阵 (P0-P4)

### P0: 立即修复 (安全漏洞 -- 本周内)

这些是可能导致数据泄露、权限丢失或系统被攻陷的问题，必须 **立即修复**。

| # | 任务名 | 涉及文件 | 预计工时 | 风险等级 |
|---|--------|---------|---------|---------|
| P0.1 | SystemManageController 缺少 @RequireAdmin 导致角色数据泄露 | `oa-web/.../SystemManageController.java:276,302` | 0.5h | CRITICAL |
| P0.2 | ContractController.expiring 缺少 @RequireAdmin | `oa-web/.../ContractController.java:69` | 0.3h | CRITICAL |
| P0.3 | BudgetController.getByDeptMonth 缺少 @RequireAdmin | `oa-web/.../BudgetController.java:66` | 0.3h | CRITICAL |
| P0.4 | AlertController.handleLog 缺少 @RequireAdmin | `oa-web/.../AlertController.java:84` | 0.3h | CRITICAL |
| P0.5 | JWT 签名密钥硬编码默认值 -- 移除 application.yml 默认值 | `oa-web/src/main/resources/application.yml:42` | 0.5h | CRITICAL |
| P0.6 | 密码修改后未使 Redis 旧 Token 失效 | `oa-web/.../AuthController.java:103-104` | 1h | HIGH |
| P0.7 | 禁用/删除员工后未清理 Redis 会话 | `oa-web/.../EmployeeController.java:110-111` | 1h | HIGH |
| P0.8 | 生产环境禁用 Swagger/Knife4j API 文档 | `oa-web/.../WebMvcConfig.java:32-35` | 0.5h | HIGH |
| P0.9 | 前端返回全权限通配符 *:*:* | `oa-service/.../AuthServiceImpl.java:120` | 1h | HIGH |
| **P0 小计** | | | **5.4h** | |

### P1: 本周修复 (Bug 修复 + 数据一致性)

这些 Bug 会导致数据错误、功能失效或业务逻辑混乱。

| # | 任务名 | 涉及文件 | 预计工时 | 风险等级 |
|---|--------|---------|---------|---------|
| P1.1 | 加班驳回冲销方向反了 (deductBalance 应改为 addCompensatoryBalance 取负) | `oa-service/.../OvertimeServiceImpl.java:79` | 1h | CRITICAL |
| P1.2 | 预算 usedAmount 更新添加乐观锁或原子 SQL | `oa-service/.../BudgetServiceImpl.java` + `OaBudget.java` | 3h | CRITICAL |
| P1.3 | 假期余额扣减添加乐观锁或原子 SQL | `oa-service/.../LeaveBalanceServiceImpl.java` + `OaLeaveBalance.java` | 2h | CRITICAL |
| P1.4 | 借支还款更新借款余额 + 超还校验 | `oa-service/.../LoanServiceImpl.java` | 3h | CRITICAL |
| P1.5 | 会签父/子任务同时活跃 (父任务 assigneeId 改为 null, status=1) | `oa-service/.../WorkflowServiceImpl.java` | 2h | CRITICAL |
| P1.6 | 会签并发审批 TOCTOU (添加 Redis 分布式锁) | `oa-service/.../WorkflowServiceImpl.java` | 4h | CRITICAL |
| P1.7 | 会议预定时间冲突检查 | `oa-service/.../MeetingServiceImpl.java` | 2h | HIGH |
| P1.8 | 登录校验员工 delFlag 逻辑删除状态 | `oa-service/.../AuthServiceImpl.java` | 0.5h | HIGH |
| P1.9 | 登录方法 request 为 null 时 NPE 保护 | `oa-service/.../AuthServiceImpl.java` | 0.5h | HIGH |
| P1.10 | Redis 角色缓存增加数据库回退机制 | `oa-service/.../AuthServiceImpl.java` + AuthInterceptor | 4h | HIGH |
| P1.11 | 仪表盘缺勤计算排除周末 + 统一统计口径 | `oa-service/.../StatisticsServiceImpl.java` | 2h | HIGH |
| P1.12 | 本月请假/出差统计查询改为区间重叠判断 | `oa-service/.../StatisticsServiceImpl.java:80-82` | 1h | HIGH |
| **P1 小计** | | | **25h** | |

### P2: 本月修复 (代码质量 + 性能 + 前端一致性)

| # | 任务名 | 涉及文件 | 预计工时 | 风险等级 |
|---|--------|---------|---------|---------|
| P2.1 | 出勤排行榜消除 N+1 (改为 GROUP BY SQL) | `oa-service/.../StatisticsServiceImpl.java` | 3h | CRITICAL |
| P2.2 | 迟到排行榜消除 N+1 (改为 selectBatchIds) | `oa-service/.../StatisticsServiceImpl.java` | 2h | HIGH |
| P2.3 | 仪表盘多表全表扫描优化 + Redis 缓存 | `oa-service/.../StatisticsServiceImpl.java` | 4h | HIGH |
| P2.4 | 清除报表缓存 key 不匹配 bug | `oa-service/.../ReportServiceImpl.java` | 1h | HIGH |
| P2.5 | 条件评估中 double == 改为 epsilon 比较 | `oa-service/.../WorkflowServiceImpl.java` | 1h | HIGH |
| P2.6 | 费用/采购预算校验 TOCTOU 改为原子 SQL | `oa-service/.../ExpenseServiceImpl.java` + `PurchaseServiceImpl.java` | 3h | HIGH |
| P2.7 | 委托审批时审计记录传入实际操作人而非被委托人 | `oa-service/.../BaseApprovalServiceImpl.java` | 1h | HIGH |
| P2.8 | 新增员工重复校验 (empCode/phone/email) | `oa-service/.../EmployeeServiceImpl.java` | 1h | MEDIUM |
| P2.9 | 部门树缓存 (getDeptTree 读取/写入 Redis) | `oa-service/.../DeptServiceImpl.java` | 2h | HIGH |
| P2.10 | 字典数据 + 系统配置缓存 | `DictDataServiceImpl.java` + `ConfigServiceImpl.java` | 2h | HIGH |
| P2.11 | 添加 oa_attendance (work_date, status) 索引 | `sql/oa_system_full.sql` | 0.5h | HIGH |
| P2.12 | 添加 oa_leave_apply 日期范围索引 | `sql/oa_system_full.sql` | 0.5h | MEDIUM |
| P2.13 | 在线用户列表 keys 替换为 SCAN + 批量查询 | `oa-service/.../OnlineUserServiceImpl.java` | 3h | HIGH |
| P2.14 | Redis 连接池配置 (Lettuce pool) | `application-prod.yml` | 0.5h | HIGH |
| P2.15 | HikariCP 连接池配置 (max-pool-size=30) | `application.yml` / `application-prod.yml` | 0.5h | MEDIUM |
| P2.16 | 退出登录清理 online:user 缓存 | `oa-service/.../AuthServiceImpl.java:125-131` | 0.5h | MEDIUM |
| P2.17 | 密码加密统一在 Service 层执行 | `oa-web/.../EmployeeController.java:71,93` | 1h | MEDIUM |
| P2.18 | 文件下载路径穿越防御 | `oa-web/.../DocumentController.java:87` | 1h | MEDIUM |
| P2.19 | /refresh-token 添加速率限制 | `oa-web/.../WebMvcConfig.java:22-23` | 1h | HIGH |
| P2.20 | application-prod.yml 密码环境变量移除空默认值 | `application-prod.yml:13,19` | 0.5h | LOW |
| P2.21 | 考勤导出流式处理 + 5000 行提示 | `oa-web/.../AttendanceController.java` | 3h | MEDIUM |
| P2.22 | 生成月度薪资幂等保护 | `oa-service/.../SalaryRecordServiceImpl.java` | 1h | LOW |
| P2.23 | WebSocket session 线程安全 (加锁发送) | `oa-web/.../NotificationEndpoint.java` | 2h | MEDIUM |
| P2.24 | handleTask 中 null 定义 + null nodeConfig NPE防护 | `oa-service/.../WorkflowServiceImpl.java:362-363` | 1h | MEDIUM |
| **P2 小计** | | | **35h** | |

### P3: 下月修复 (前端体验 + 移动端)

| # | 任务名 | 涉及文件 | 预计工时 | 风险等级 |
|---|--------|---------|---------|---------|
| P3.1 | 统一前端 API 调用模式 (所有端点 /api 前缀) | `frontend/src/api/*.ts` | 8h | SEVERE |
| P3.2 | Vite 代理 22 条规则简化为 1 条 | `frontend/vite.config.ts` | 1h | SEVERE |
| P3.3 | 消除前端 80% 的 `any` 类型使用 | `frontend/src/**/*.vue` + `*.ts` | 16h | SEVERE |
| P3.4 | 提取共享 clearAuthState 函数 (request.ts + router) | `frontend/src/utils/request.ts` + `router/index.ts` | 2h | SEVERE |
| P3.5 | Token 刷新使用带拦截器的 axios 实例 | `frontend/src/utils/request.ts:66` | 1h | SEVERE |
| P3.6 | 消除审批中心 14 个重复 el-descriptions 模板 | `frontend/.../approval-center/index.vue` | 4h | HIGH |
| P3.7 | WebSocket 通知替代轮询 (setInterval -> onmessage) | `frontend/.../layout/index.vue:188` | 2h | MEDIUM |
| P3.8 | 仪表盘 9 个 ECharts 延迟渲染 | `frontend/.../dashboard/index.vue:173-182` | 2h | MEDIUM |
| P3.9 | Element Plus 图标按需引入 | `frontend/src/main.ts:13-15` | 2h | MEDIUM |
| P3.10 | 移动端字段名不匹配 (leave/出差/经费/加班 全部修复) | `mobile/src/pages/oa/*.vue` 4个文件 | 4h | CRITICAL |
| P3.11 | 移动端 Token 刷新队列泄漏修复 | `mobile/src/utils/request.ts:96-117` | 2h | CRITICAL |
| P3.12 | 移动端 uni.chooseFile 微信小程序兼容 | `mobile/src/pages/oa/document.vue:68` | 2h | CRITICAL |
| P3.13 | 移动端 6 种缺失申请记录列表页面 | `mobile/src/pages/oa/*-list.vue` | 8h | MAJOR |
| P3.14 | 移动端 Schedule API 路由冲突 (add/update 同 URL) | `mobile/src/api/schedule.ts:5-6` | 0.5h | MAJOR |
| **P3 小计** | | | **55.5h** | |

### P4: 持续改进 (测试 + CI/CD)

| # | 任务名 | 涉及文件 | 预计工时 | 风险等级 |
|---|--------|---------|---------|---------|
| P4.1 | CI 中启用后端单元测试 (删除 -Dmaven.test.skip=true) | `.github/workflows/backend.yml` | 1h | CRITICAL |
| P4.2 | 添加 JaCoCo 覆盖率测量 | `pom.xml` | 2h | CRITICAL |
| P4.3 | CI 中启用前端 typecheck (`pnpm typecheck`) | `.github/workflows/frontend.yml` | 0.5h | CRITICAL |
| P4.4 | 编写 Service 层单元测试 (50+ 测试类) | `oa-service/src/test/**` | 80h | CRITICAL |
| P4.5 | 添加 Dependabot + CodeQL 安全扫描 | `.github/dependabot.yml` + 新 workflow | 2h | HIGH |
| P4.6 | 添加 Checkstyle + SpotBugs + PMD 静态分析 | `pom.xml` | 3h | HIGH |
| P4.7 | 前端 pnpm 缓存配置 | `.github/workflows/frontend.yml` | 0.5h | HIGH |
| P4.8 | API 测试复用后端构建产物 (避免重复构建) | `.github/workflows/api-test.yml` | 2h | HIGH |
| P4.9 | 添加 Playwright E2E CI 集成 | `.github/workflows/api-test.yml` | 4h | HIGH |
| P4.10 | 添加 ESList/Prettier 配置 | `frontend/` + `mobile/` | 3h | HIGH |
| P4.11 | 添加 Dockerfile + docker-compose | 项目根目录 | 4h | MEDIUM |
| P4.12 | 添加部署流水线 (deploy.yml) | `.github/workflows/deploy.yml` | 4h | MEDIUM |
| P4.13 | 添加作业超时配置 | `.github/workflows/*.yml` | 0.5h | LOW |
| P4.14 | 添加 CODEOWNERS + 分支保护规则 | `.github/CODEOWNERS` | 1h | LOW |
| P4.15 | 添加发布自动化 (release.yml) | `.github/workflows/release.yml` | 2h | LOW |
| **P4 小计** | | | **110h** | |

### 优先级矩阵总览

| 级别 | 任务数 | 预计总工时 | 主要关注点 |
|------|--------|-----------|-----------|
| P0 | 9 | 5.4h | 安全漏洞修补 |
| P1 | 12 | 25h | Bug 修复 + 数据一致性 |
| P2 | 24 | 35h | 代码质量 + 性能优化 |
| P3 | 14 | 55.5h | 前端/移动端一致性 |
| P4 | 15 | 110h | 测试体系 + CI/CD 建设 |
| **总计** | **74** | **~231h** | |

---

## 三、测试体系建设

### 3.1 后端单元测试 (50+ 测试类)

#### 3.1.1 系统管理模块

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `EmployeeServiceImplTest` | 85% | CRUD、分页、密码加密、角色分配、重复校验、状态变更 | 15 |
| `DeptServiceImplTest` | 80% | 部门树构建、CRUD、移动、级联删除 | 10 |
| `RoleServiceImplTest` | 80% | CRUD、菜单权限分配、角色去重 | 10 |
| `MenuServiceImplTest` | 75% | 菜单树、CRUD、角色菜单查询 | 8 |
| `ConfigServiceImplTest` | 80% | CRUD、缓存读取/失效、按key查询 | 8 |
| `DictDataServiceImplTest` | 80% | CRUD、字典类型缓存、级联删除 | 8 |
| `OperationLogServiceImplTest` | 75% | 分页查询、清理、按条件筛选 | 5 |
| **小计** | | | **64** |

#### 3.1.2 OA 核心模块

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `AttendanceServiceImplTest` | 85% | 打卡(正常/迟到/重复)、签退(正常/早退)、自动标记、分页、历史 | 20 |
| `LeaveApplyServiceImplTest` | 85% | 提交(有余额/无余额)、审批通过(扣余额+标记考勤)、驳回、天数计算(跨月跨周末) | 15 |
| `BusinessTripServiceImplTest` | 80% | 提交、审批、自动标记考勤、出差时长计算 | 10 |
| `OutingServiceImplTest` | 80% | 提交、审批、外出时长计算 | 8 |
| `OvertimeServiceImplTest` | 85% | 提交、审批通过(加调休余额)、驳回(冲减方向)、加班时长计算 | 10 |
| `NoticeServiceImplTest` | 80% | 发布、分页、已读标记、未读计数 | 8 |
| `MeetingServiceImplTest` | 80% | 预定(冲突检测/无冲突)、取消、CRUD | 10 |
| `MessageServiceImplTest` | 80% | 发送、阅读、未读数、已发送列表 | 8 |
| `ScheduleServiceImplTest` | 75% | 日程CRUD、按日期范围查询 | 6 |
| `TodoServiceImplTest` | 80% | 待办列表、标记完成、忽略 | 6 |
| **小计** | | | **101** |

#### 3.1.3 工作流引擎模块 (最高优先级)

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `WorkflowServiceImplTest` | 90% | 启动流程(有定义/无定义/解析失败)、审批(单人/会签/或签/委派/转办/退回/催办/撤回/撤回驳回)、条件分支(大额/小额)、定义管理(创建/更新/列表)、审批人解析(按角色/按部门) | 30 |
| `BaseApprovalServiceImplTest` | 90% | doSubmit、doApprove(授权/越权/委派)、状态回调(7种业务)、分页填充(员工姓名+审批意见) | 15 |
| `TaskServiceImplTest` | 85% | 任务创建、完成、转移、超时等待 | 10 |
| `ConditionEvaluatorTest` | 90% | 浮点数==比较(epsilon)、字符串比较、布尔条件、组合条件、null 阈值 | 10 |
| **小计** | | | **65** |

#### 3.1.4 业务模块

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `ExpenseServiceImplTest` | 80% | 提交、审批(预算校验/原子扣减)、驳回(预算回滚)、导出 | 10 |
| `PurchaseServiceImplTest` | 80% | 提交、审批(预算校验)、验收、驳回 | 10 |
| `LoanServiceImplTest` | 85% | 借款、还款(余额更新/超还校验)、审批 | 10 |
| `SalaryRecordServiceImplTest` | 75% | 月度生成(幂等)、我的薪资、分页 | 8 |
| `ContractServiceImplTest` | 75% | CRUD、到期列表(边界条件)、导出 | 8 |
| `AssetServiceImplTest` | 75% | 资产CRUD、借用、归还、状态变更 | 8 |
| `BudgetServiceImplTest` | 80% | CRUD、usedAmount 原子更新(乐观锁)、按部门月份查询 | 10 |
| `LeaveBalanceServiceImplTest` | 85% | 年度初始化、扣减(乐观锁)、恢复、调休余额添加 | 10 |
| `AlertServiceImplTest` | 75% | 规则CRUD、日志查询、处理预警 | 6 |
| **小计** | | | **80** |

#### 3.1.5 报表与统计模块

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `ReportServiceImplTest` | 80% | 个人考勤汇总/趋势/请假汇总、管理员汇总/部门对比/排名/今日概览、环比、缓存击穿 | 15 |
| `StatisticsServiceImplTest` | 85% | 仪表盘(今日/本周/本月/全量)、出勤排行榜(消除 N+1)、缺勤计算(排除周末)、迟到排行榜 | 12 |
| **小计** | | | **27** |

#### 3.1.6 认证与授权模块

| 测试类 | 目标覆盖率 | 核心测试点 | 预估用例数 |
|--------|-----------|-----------|-----------|
| `AuthServiceImplTest` | 90% | 登录(成功/失败/逻辑删除)、退出(缓存清理)、Token刷新(有效/过期/类型)、验证码校验、登录日志、角色缓存回退 | 15 |
| `AuthInterceptorTest` | 85% | 放行路径、Token 校验、@RequireAdmin 拦截、角色缓存空回退 | 8 |
| `RateLimitInterceptorTest` | 80% | 限流阈值、超过限制、白名单 | 5 |
| **小计** | | | **28** |

#### 3.1.7 Controller 集成测试 (补充现有 27 个)

现有的 27 个 ControllerTest 需要增强或补充以下 Controller:

| 测试类 | 补充测试点 | 预估新增用例数 |
|--------|-----------|-----------|
| `SystemManageControllerTest` | **新增**: 权限分配、角色管理、用户查询 | 10 |
| `ReportControllerTest` | **新增**: 各类报表、日期范围 | 10 |
| `StatisticsControllerTest` | **新增**: 仪表盘 | 8 |
| `MenuControllerTest` | **新增**: 菜单树、权限分配 | 8 |
| `ConfigControllerTest` | **新增**: 参数CRUD、缓存刷新 | 6 |
| `DictControllerTest` | **新增**: 字典类型+数据、级联 | 8 |
| `MonitorControllerTest` | **新增**: 在线用户、日志 | 6 |
| `SalaryControllerTest` | **新增**: 我的薪资、月度生成 | 6 |
| `AlertControllerTest` | **增强**: 预警规则CRUD | 6 |
| 其余 18 个已有 ControllerTest | **增强**: 边界条件、异常路径 | 每类 +3 = 54 |
| **小计** | | **122** |

#### 测试类总览

| 模块 | 测试类数 | 预估用例数 | 目标覆盖率 |
|------|---------|-----------|-----------|
| 系统管理 | 6 | 64 | 75-85% |
| OA 核心 | 10 | 101 | 75-85% |
| 工作流引擎 | 4 | 65 | 85-90% |
| 业务模块 | 9 | 80 | 75-85% |
| 报表统计 | 2 | 27 | 80-85% |
| 认证授权 | 3 | 28 | 80-90% |
| Controller 增强 | 27+ | 122 | 60-80% |
| **总计** | **~61** | **~487** | **平均 80%** |

### 3.2 前端 E2E 测试

#### 3.2.1 新增测试文件结构

```
tests/
  e2e/
    employee-day.spec.ts      # 场景1: 员工的一天
    admin-day.spec.ts         # 场景2: 管理员的一天
    approval-flows.spec.ts    # 场景3: 审批全流程 (7种)
    system-admin.spec.ts      # 场景4: 系统管理
  frontend-ui-test.spec.ts    # 原有: 基础登录 + 导航
```

#### 3.2.2 场景详细设计

**场景1: 员工的一天** (覆盖 8 个子场景)

| 步骤 | 操作 | 断言 | 数据前置 |
|------|------|------|---------|
| 1 | 登录 | 跳转首页 | 员工账号已存在 |
| 2 | 查看Dashboard | 待办数 > 0 | 有审批中申请 |
| 3 | 打卡 (9:00前模拟) | 打卡成功, status=正常 | 今日无记录 |
| 4 | 查看公告列表 | 列表渲染 | 有已发布公告 |
| 5 | 提交请假申请 | 成功提交, 状态=审批中 | 有余额 |
| 6 | 查看我的申请 | 新记录出现在列表中 | 刚提交 |
| 7 | 查看未读消息 | 消息列表更新 | 有未读消息 |
| 8 | 签退 (18:00后模拟) | 签退成功 | 已打卡 |

**场景2: 管理员的一天** (覆盖 7 个子场景)

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 管理员登录 | 跳转管理员工作台 |
| 2 | 审批中心 — 通过一个请假 | 已处理列表更新 |
| 3 | 审批中心 — 驳回一个申请 | 状态=驳回 |
| 4 | 员工管理 — 搜索/修改 | 修改生效 |
| 5 | 发布公告 | 列表新增 |
| 6 | 查看报表 (考勤汇总/部门对比/排名) | 图表渲染, 数字正确 |
| 7 | 查看数据看板 | 图表渲染 |

**场景3: 7种审批类型全流程**

| 审批类型 | 测试名 | 特有验证点 |
|---------|--------|-----------|
| 请假 | leave-approve.spec | 余额扣减、考勤标记 |
| 出差 | trip-approve.spec | 出差考勤标记 |
| 外出 | outing-approve.spec | 外出时长 |
| 加班 | overtime-approve.spec | 调休余额增加 |
| 报销 | expense-approve.spec | 预算扣减 |
| 采购 | purchase-approve.spec | 采购验收流程 |
| 借款 | loan-approve.spec | 借款记录、还款 |

**场景4: 系统管理**

| 测试 | 操作链 |
|------|--------|
| 员工CRUD | 新增 -> 修改 -> 搜索 -> 删除 |
| 角色管理 | 新增 -> 分配菜单 -> 修改 -> 删除 |
| 菜单管理 | 新增 -> 修改 -> 删除 -> 验证树 |
| 部门管理 | 新增子部门 -> 移动 -> 删除 |
| 字典管理 | 新增类型 -> 新增字典项 -> 修改 -> 删除 |

### 3.3 移动端 E2E 测试

#### 3.3.1 新增测试文件

```
tests/
  e2e-mobile/
    punch-clock.spec.ts       # 场景1: 通勤打卡
    mobile-approval.spec.ts   # 场景2: 移动审批
    form-submits.spec.ts      # 场景3: 7种表单提交
    tab-navigation.spec.ts    # 场景4: Tab 导航完整流程
  mobile-ui-test.spec.ts      # 原有: 基础登录
```

#### 3.3.2 场景详细设计

**场景1: 通勤打卡**

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 登录移动端 | 跳转首页 |
| 2 | 查看今日考勤卡片 | 显示"未打卡" |
| 3 | 点击打卡 (mock GPS 公司坐标) | 成功提示 |
| 4 | 重复点击打卡 | "今日已打卡" |
| 5 | 查看月度考勤日历 | 当天标记已打卡 |
| 6 | 签退 | 签退成功 |

**场景2: 移动审批**

| 步骤 | 操作 | 断言 |
|------|------|------|
| 1 | 管理员登录移动端 | 首页显示待办数 |
| 2 | 进入待办列表 | 列表不为空 |
| 3 | 查看请假详情 | 显示申请信息 |
| 4 | 填写意见并"通过" | 待办消失 |
| 5 | 已办列表出现记录 | 状态=已通过 |
| 6 | 切回员工账号 | 我的申请状态更新 |

**场景3: 7种表单提交**

| 表单 | 填写字段 | 提交验证 |
|------|---------|---------|
| 请假 | leaveType/startTime/endTime/reason | 提交成功, 列表出现 |
| 出差 | destination/startTime/endTime/purpose | 提交成功 |
| 外出 | destination/startTime/endTime/reason | 提交成功 |
| 加班 | overtimeDate/startTime/endTime/reason/hours | 提交成功 |
| 报销 | title/category/amount/description | 提交成功 |
| 采购 | title/category/amount/description | 提交成功 |
| 借款 | amount/reason | 提交成功 |

### 3.4 测试数据策略

#### 3.4.1 测试数据库设计

| 环境 | 数据库类型 | 初始化方式 | 隔离策略 |
|------|-----------|-----------|---------|
| 单元测试 | Mock (Mockito) | 方法内构造 POJO | 完全隔离 |
| 集成测试 | MySQL 8.0 (Testcontainers) | seed_data.sql + @Sql | @Transactional 复位 |
| E2E 测试 | MySQL 8.0 (CI Service) | oa_system_full.sql + seed_data.sql | 每次运行重建 |
| API 测试 | MySQL 8.0 (CI Service) | oa_system_full.sql + seed_data.sql | 测试后清理 |

#### 3.4.2 种子数据脚本

现有 `seed_data.sql` 需要增强以支持所有测试场景:

```
-- 必须包含的测试数据
1. 管理员账号: admin/admin123 (已启用, 系统管理员角色)
2. 普通员工: zhangsan/123456 (已启用, 普通员工角色)
3. 部门: 技术部、人事部、财务部 (含层级关系)
4. 菜单: 全量菜单数据 (系统管理/OA/报表/个人中心)
5. 角色: 系统管理员(SYS_ADMIN)、部门经理(DEPT_MANAGER)、普通员工(USER)
6. 假期余额: 张三 年假10天、事假5天、病假5天
7. 预算: 技术部 2026年度 差旅费 100000, used=20000
8. 流程定义: 请假(单人审批)、报销(部门经理+财务)、采购(部门经理+总经理)
```

#### 3.4.3 测试数据隔离方案

```
                    CI 运行流程
                         │
              ┌──────────┴──────────┐
              │    Testcontainers    │
              │   (Integration Test) │
              │   独立 MySQL 容器      │
              └──────────┬──────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    seed_data.sql    seed_data.sql   seed_data.sql
         │               │               │
    WorkflowIT      AttendanceIT    LeaveIT
   (@Transactional) (@Transactional) (@Transactional)
         │               │               │
    Rollback         Rollback        Rollback
```

#### 3.4.4 时间敏感测试策略

```
// 使用 Clock 依赖注入，避免时间硬编码
@Service
public class AttendanceServiceImpl {
    private final Clock clock;
    
    public AttendanceServiceImpl(Clock clock) {
        this.clock = clock;
    }
    
    // 测试时注入固定 Clock
}

// 测试类
@Test
void clockIn_Late() {
    Clock fixedClock = Clock.fixed(
        LocalDateTime.of(2026, 6, 1, 9, 30).toInstant(ZoneOffset.UTC),
        ZoneId.systemDefault()
    );
    // 注入 fixedClock
    // 9:30 打卡 -> 应标记为迟到
}
```

### 3.5 测试覆盖率门禁

| 层级 | 行覆盖率目标 | 硬性门禁 | 测量工具 |
|------|------------|---------|---------|
| Service 层 | >= 80% | >= 70% | JaCoCo |
| Controller 层 | >= 70% | >= 60% | JaCoCo |
| Mapper 层 | >= 50% | >= 40% | JaCoCo |
| Model 层 | >= 50% | >= 40% | JaCoCo |
| 前端 (组件) | >= 60% | >= 40% | Vitest |

门禁在 CI 中强制执行:

```yaml
# CI 步骤
- name: Enforce coverage
  run: |
    # 从 JaCoCo CSV 抽取行覆盖率
    python3 -c "
    import csv
    with open('target/site/jacoco/jacoco.csv') as f:
        reader = csv.DictReader(f)
        total_covered = sum(int(r['COVERED_LINE']) for r in reader)
        total_missed = sum(int(r['MISSED_LINE']) for r in reader)
    rate = total_covered / (total_covered + total_missed) * 100
    if rate < 70.0:
        print(f'FAIL: Coverage {rate:.2f}% < 70%')
        exit(1)
    "
```

---

## 四、CI/CD 改进方案

### 4.1 工作流架构总览

```
                    GitHub Actions
                    ─────────────
                        
  PR/Master Push
       │
       ├──→ [backend-test.yml]        Maven 编译 + 单元测试 + JaCoCo(30min)
       │       │
       │       ├──→ [integration-test.yml]  (workflow_run) Testcontainers 集成测试(15min)
       │       │
       │       └──→ [api-test.yml]          (workflow_run) API E2E + Playwright(15min)
       │
       ├──→ [frontend-e2e.yml]        pnpm typecheck + build + Playwright(10min)
       │
       ├──→ [security-scan.yml]       Trivy + CodeQL + Dependabot(5min)
       │
       └──→ [code-quality.yml]        Checkstyle + SpotBugs + ESLint(8min)
       
  Tag v* Push
       └──→ [release.yml]             构建 Docker 镜像 + GitHub Release(20min)
       
  Manual Trigger
       └──→ [deploy.yml]              部署到 Dev/Staging/Prod(15min)
```

### 4.2 backend.yml (现有 -- 需修改)

**改动点:**

```yaml
# 改动1: 移除 -Dmaven.test.skip=true
- 之前: mvn clean package -Dmaven.test.skip=true -B -V
- 改为: mvn clean verify -B -V

# 改动2: 添加 MySQL + Redis 服务容器
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_ROOT_PASSWORD: test_password
      MYSQL_DATABASE: oa_system_test
    ports:
      - 3306:3306
    options: --health-cmd="mysqladmin ping -h localhost" --health-interval=10s

  redis:
    image: redis:7
    ports:
      - 6379:6379
    options: --health-cmd="redis-cli ping" --health-interval=10s

# 改动3: 初始化测试数据库
steps:
  - name: Initialize test database
    run: |
      mysql -h 127.0.0.1 -u root -ptest_password oa_system_test < code/backend/sql/oa_system_full.sql
      mysql -h 127.0.0.1 -u root -ptest_password oa_system_test < code/backend/sql/seed_data.sql

# 改动4: 添加 JaCoCo 报告上传
  - name: Upload JaCoCo report
    uses: actions/upload-artifact@v4
    with:
      name: jacoco-report
      path: code/backend/**/target/site/jacoco/

# 改动5: 添加 CI 配置 profile
  - name: Run tests
    working-directory: code/backend
    run: mvn clean verify -P ci -B -V
```

**示例 `application-ci.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oa_system_test?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: test_password
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

jwt:
  secret: test-jwt-secret-for-ci-only
```

### 4.3 frontend.yml (现有 -- 需修改)

```yaml
# 改动1: 添加 pnpm store 缓存
- uses: actions/cache@v4
  with:
    path: ~/.local/share/pnpm/store
    key: pnpm-${{ hashFiles('frontend/pnpm-lock.yaml') }}
    restore-keys: pnpm-

# 改动2: 添加类型检查步骤
- name: TypeScript type check
  working-directory: code/frontend
  run: pnpm typecheck

# 改动3: 添加前端单元测试 (如使用 Vitest)
- name: Run frontend unit tests
  working-directory: code/frontend
  run: pnpm test:unit -- --coverage

# 改动4: CI 环境变量验证
- name: Verify env variables
  run: |
    grep "VITE_API_URL" code/frontend/.env.production || echo "WARNING: VITE_API_URL not set"
```

### 4.4 api-test.yml (现有 -- 需增强)

```yaml
# 改动1: 复用后端构建产物
# 删除重新构建步骤，改为下载 backend.yml 上传的 artifact
- uses: actions/download-artifact@v4
  with:
    name: oa-web-jar
    path: code/backend/oa-web/target/

# 改动2: 添加 Playwright E2E
- name: Install Playwright browsers
  run: npx playwright install chromium

- name: Start application
  run: |
    java -jar code/backend/oa-web/target/*.jar --spring.profiles.active=ci &
    sleep 30
    # 等待就绪

- name: Run Frontend E2E tests
  run: npx playwright test tests/frontend-ui-test.spec.ts --config=tests/playwright.config.ts

- name: Run Mobile E2E tests
  run: npx playwright test tests/mobile-ui-test.spec.ts --config=tests/playwright.config.ts

- name: Generate HTML test report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: playwright-report
    path: tests/test-results/
```

### 4.5 新增 workflow: integration-test.yml

```yaml
name: "Integration Tests"

on:
  workflow_run:
    workflows: ["Backend Build & Test"]
    types: [completed]
    branches: [master]

jobs:
  integration-test:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    timeout-minutes: 20
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test_password
          MYSQL_DATABASE: oa_system_test
        ports:
          - 3306:3306
      redis:
        image: redis:7
        ports:
          - 6379:6379
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Init DB
        run: |
          mysql -h 127.0.0.1 -u root -ptest_password oa_system_test < code/backend/sql/oa_system_full.sql
          mysql -h 127.0.0.1 -u root -ptest_password oa_system_test < code/backend/sql/seed_data.sql
      - name: Run integration tests
        working-directory: code/backend
        run: mvn verify -P integration-test -B
      - name: Upload IT report
        uses: actions/upload-artifact@v4
        with:
          name: integration-test-reports
          path: code/backend/**/target/failsafe-reports/
```

### 4.6 新增 workflow: security-scan.yml

```yaml
name: "Security Scan"

on:
  schedule:
    - cron: '0 2 * * 1'   # 每周一凌晨2点
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  codeql:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      - uses: github/codeql-action/init@v3
        with:
          languages: java, javascript, typescript
      - uses: github/codeql-action/analyze@v3
  
  trivy-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
```

### 4.7 现有 workflow 改进总结

| Workflow | 修改类型 | 具体变更 |
|----------|---------|---------|
| backend.yml | **增强** | 删除 `-Dmaven.test.skip=true`，添加 MySQL/Redis 服务，添加 JaCoCo 报告，添加 CI profile |
| frontend.yml | **增强** | 添加 pnpm 缓存，添加 `pnpm typecheck` 步骤，添加 CI 环境变量验证 |
| api-test.yml | **重构** | 复用后端构建产物，添加 Playwright E2E，添加 HTML 测试报告上传 |
| integration-test.yml | **新增** | Testcontainers 集成测试，failsafe 报告上传 |
| security-scan.yml | **新增** | CodeQL + Trivy 定期扫描 |
| release.yml | **新增** | Git tag 触发 -> 构建 Docker + GitHub Release |
| deploy.yml | **新增** | 手动触发 -> 部署到 Dev/Staging/Prod |

---

## 五、具体执行计划 (5 周)

### 第 1 周: 安全漏洞修补 + 关键 Bug 修复

| 日期 | 任务 | 负责人角色 | 交付物 |
|------|------|-----------|--------|
| Day 1 | P0.1-P0.5: 5 个 @RequireAdmin + JWT 默认值移除 | 后端开发 | PR #1: 安全修补 |
| Day 2 | P0.6-P0.7: 密码修改/员工禁用后 Redis 清理 | 后端开发 | PR #2: 会话安全 |
| Day 3 | P0.8-P0.9: 生产禁用 Swagger + 权限通配符修复 | 后端开发 | PR #3: 信息泄露防护 |
| Day 4 | P1.1-P1.3: 加班冲销方向 + Budget/LeaveBalance 乐观锁 | 后端开发 | PR #4: 数据一致性 |
| Day 5 | P1.4-P1.5: 借支还款余额更新 + 会签父任务修复 | 后端开发 | PR #5: 业务逻辑修复 |
| **里程碑** | 所有 P0 问题修复完成，P1 完成 60% | | **安全基线建立** |

**验收标准:**
- 所有 CRITICAL 安全漏洞已关闭
- JWT 密钥在生产环境中再无默认值
- 密码修改后旧 Token 立即失效
- 预算/余额扣减在并发场景下不再丢失

### 第 2 周: Bug 修复 + 工作流强化

| 日期 | 任务 | 负责人角色 | 交付物 |
|------|------|-----------|--------|
| Day 1 | P1.6: 会签并发 TOCTOU 分布式锁 | 后端开发 | |
| Day 2 | P1.7-P1.9: 会议冲突检查 + 登录校验 delFlag + NPE | 后端开发 | PR #6: 业务Bug修复 |
| Day 3 | P1.10-P1.11: Redis 角色缓存回退 + 缺勤计算修正 | 后端开发 | PR #7: 缓存健壮性 |
| Day 4 | P1.12 + P2.1-P2.2: 区间查询 + 出勤/迟到 N+1 | 后端开发 | PR #8: 性能修复A |
| Day 5 | P2.3-P2.4: 仪表盘优化 + 报表缓存 key 修复 | 后端开发 | PR #9: 性能修复B |
| **里程碑** | 所有 P1 问题修复完成 | | **数据一致性基线建立** |

**验收标准:**
- 会签场景在并发 10 线程下无竞态
- Redis 重启后角色鉴权通过数据库回退照常工作
- 出勤排行榜从 1001 SQL 降至 2 SQL
- 仪表盘在 10000 条考勤数据下响应 < 500ms

### 第 3 周: 性能优化 + 配置管理

| 日期 | 任务 | 负责人角色 | 交付物 |
|------|------|-----------|--------|
| Day 1 | P2.5-P2.6: 浮点 epsilon 比较 + 预算原子更新 | 后端开发 | |
| Day 2 | P2.7-P2.8: 委托审批审计修复 + 员工重复校验 | 后端开发 | PR #10: 业务修复B |
| Day 3 | P2.9-P2.12: 部门树/字典/配置缓存 + 数据库索引 | 后端开发 | PR #11: 缓存层 |
| Day 4 | P2.13-P2.15: 在线用户 SCAN + Redis/HikariCP 连接池 | 后端开发 | PR #12: 基础设施 |
| Day 5 | P2.16-P2.24: 其余 Medium/HIGH 修复 | 后端开发 | PR #13: 综合修复 |
| **里程碑** | 后端全部 P2 修复完成 | | **性能基线建立** |

**验收标准:**
- 部门树加载从每次查库改为 Redis 命中
- 字典/配置 90% 读取命中缓存
- 新加索引使月份范围查询从全表扫描转为索引范围
- 在线用户列表不再使用 Redis KEYS 命令

### 第 4 周: 前端 + 移动端修复

| 日期 | 任务 | 负责人角色 | 交付物 |
|------|------|-----------|--------|
| Day 1 | P3.1-P3.2: API 统一前缀 + Vite 代理简化 | 前端开发 | |
| Day 2 | P3.3: 消除 80% any 类型 | 前端开发 | PR #14: 前端类型安全 |
| Day 3 | P3.4-P3.5: Token 刷新 + clearAuthState | 前端开发 | PR #15: 前端安全 |
| Day 4 | P3.6-P3.8: 审批中心模板 + 轮询替代 + ECharts 延迟 | 前端开发 | PR #16: 前端性能 |
| Day 5 | P3.10-P3.14: 移动端全部修复 | 移动端开发 | PR #17: 移动端修复 |
| **里程碑** | 前端 + 移动端全部修复完成 | | **端到端一致性建立** |

**验收标准:**
- 所有 API 端点统一到 `/api` 前缀
- Vite 代理从 22 条规则降至 1 条
- 前端编译中再无 `any` 类型 (至少消除 80%)
- 移动端 4 个 Critical 字段名不匹配全部修复
- Token 刷新队列泄漏修复

### 第 5 周: 测试体系 + CI/CD 建设

| 日期 | 任务 | 负责人角色 | 交付物 |
|------|------|-----------|--------|
| Day 1 | P4.1-P4.3: CI 基础增强 (测试启用 + JaCoCo + typecheck) | DevOps | PR #18: CI 基础 |
| Day 2 | P4.5-P4.6: 安全扫描 + 静态分析 | DevOps | PR #19: 质量门禁 |
| Day 3 | P4.4部分: 编写核心 Service 测试 (Workflow + Auth + Attendance 前 5 类) | QA | PR #20: 测试A |
| Day 4 | P4.4部分: Service 测试 (Leave + Expense + BusinessTrip + BaseApproval 4 类) | QA | PR #21: 测试B |
| Day 5 | P4.7-P4.9: CI 缓存 + 产物复用 + Playwright 集成 | DevOps | PR #22: CI 完善 |
| **里程碑** | CI/CD 流水线完整运行 + 30% 测试覆盖 | | **质量基线建立** |

**验收标准:**
- `backend.yml` 运行单元测试 + JaCoCo 报告
- `frontend.yml` 运行 `pnpm typecheck` 并失败时阻断
- API 测试复用构建产物，不再重复编译
- 10+ 个 Service 层测试类在 CI 中通过
- Dependabot + CodeQL 配置完成

---

## 六、预期收益

### 6.1 量化目标

| 指标 | 当前值 | 5周后目标 | 提升幅度 |
|------|--------|----------|---------|
| **CRITICAL 安全漏洞** | 5 | 0 | 100% |
| **CRITICAL Bug** | 8 | 0 | 100% |
| **后端代码行覆盖率** | ~0% | >= 70% | 极大提升 |
| **Service 层方法覆盖率** | 0% | >= 85% | 极大提升 |
| **CI 测试执行** | 已跳过 | 全部执行 | 启用 |
| **CI 构建时间 (后端)** | ~8min (无测试) | ~15min (含测试) | 新增质量保障 |
| **前端构建类型检查** | 未运行 | v-check 自动检查 | 新增 |
| **前端 `any` 类型数** | 60+ | <= 10 | 减少 80%+ |
| **仪表盘加载时间** | 3-5s (5000条) | < 500ms | 提升 10x |
| **出勤排行榜查询** | 1001 SQL | 2 SQL | 减少 99.8% |
| **在线用户查询** | N+1 + Redis KEYS | 2 SQL + SCAN | 阻塞消除 |
| **Vite 代理规则** | 22 | 1 | 减少 95% |
| **移动端回归 Bug** | 6 (字段不匹配) | 0 | 100% |

### 6.2 非量化收益

| 维度 | 收益描述 |
|------|---------|
| **维护性** | 角色缓存数据库回退机制消除了"Redis 挂了系统就不能用"的单点故障 |
| **可测试性** | Service 层 50+ 测试类使重构不再盲目，回归覆盖有保障 |
| **安全性** | 5 个 CRITICAL 授权漏洞修复消除了数据泄露风险 |
| **团队效率** | CI 中统一代码风格 + 静态分析减少了 Code Review 中的风格争论 |
| **部署信心** | CI 流水线 + 测试门禁使每次合并到 master 都经过充分验证 |
| **开发者体验** | 统一 API 前缀 + 减少 any 类型 + 统一密码加密位置降低开发心智负担 |

### 6.3 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 单元测试编写超出 5 周周期 | 中 | 高 | 优先覆盖率最高的模块 (Workflow/Auth/Leave)，剩余作为持续任务 |
| 工作流分布式锁引入死锁 | 低 | 高 | 使用 Redisson 实现，配置合理的锁超时和重试策略，编写死锁恢复测试 |
| 缓存引入后数据一致性问题 | 中 | 中 | 部门树缓存 30min TTL + 修改清理机制；字典缓存 30min TTL + 修改清理 |
| 乐观锁引入后日志中大量版本冲突异常 | 中 | 低 | 设置合理重试策略 (3次)，记录冲突次数监控 |
| CI 测试导致构建时间翻倍 | 高 | 中 | 首先确保单元测试快速 (<1min/类)，并行化测试执行，JaCoCo 只生成聚合报告 |

---

### Critical Files for Implementation

- `E:\JavaProject\Corporate_OA_System\code\backend\oa-service\src\main\java\cn\oa\service\impl\WorkflowServiceImpl.java` — 工作流核心逻辑，包含会签TOCTOU、父/子任务双活跃、条件浮点比较、NPE 等 6 个 Bug 需要修复
- `E:\JavaProject\Corporate_OA_System\code\backend\oa-service\src\main\java\cn\oa\service\impl\BaseApprovalServiceImpl.java` — 7 种审批业务的模板方法基类，委托审计记录错误需修复
- `E:\JavaProject\Corporate_OA_System\code\backend\oa-web\src\main\java\cn\oa\controller\SystemManageController.java` — 最严重的未授权接口，行 276 和 302 需要添加 @RequireAdmin
- `E:\JavaProject\Corporate_OA_System\code\backend\pom.xml` — 需要添加 JaCoCo、Checkstyle、SpotBugs 插件，修改 CI profile 配置
- `E:\JavaProject\Corporate_OA_System\.github\workflows\backend.yml` — 关键 CI 工作流，需删除 `-Dmaven.test.skip=true` 并添加 MySQL/Redis 服务

---

## 附录: 详细测试方案

## 2. 层级一：后端单元测试 (JUnit 5 + Mockito)

### 2.1 测试目录结构

```
code/backend/oa-service/src/test/java/cn/oa/service/
  impl/
    WorkflowServiceImplTest.java
    AttendanceServiceImplTest.java
    LeaveApplyServiceImplTest.java
    AuthServiceImplTest.java
    EmployeeServiceImplTest.java
    ReportServiceImplTest.java
    StatisticsServiceImplTest.java
    BaseApprovalServiceImplTest.java
    NoticeServiceImplTest.java
    MeetingServiceImplTest.java
    MessageServiceImplTest.java
```

### 2.2 Service层单元测试分模块设计

#### 2.2.1 工作流引擎 -- WorkflowServiceImplTest (最高优先级)

**核心逻辑覆盖矩阵：**

| 测试方法 | 测试点 | 输入 | 预期结果 |
|---------|--------|------|---------|
| `startProcess_NoDefinition_AutoApprove` | 无流程定义时自动通过 | 业务类型无匹配定义 | 实例status=1, callback触发 |
| `startProcess_WithDefinition_Success` | 有流程定义时创建实例+任务 | 有效定义+单节点 | 实例待审批, 任务指派给审批人 |
| `startProcess_ParseNodeConfigError` | nodeConfig JSON解析失败时自动通过 | 无效JSON | 实例status=1, 错误日志记录 |
| `startProcess_EmptyNodeConfig` | 节点配置为空时自动通过 | nodes=[] | 实例status=1 |
| `startProcess_ConditionContext` | 条件分支场景 | conditionContext含days>3 | 进入高级审批节点 |
| `handleTask_Approve_SingleApprover` | 单人审批通过 | taskId, status=1 | 任务完成, 流程结束, callback触发 |
| `handleTask_Reject` | 审批驳回 | taskId, status=3 | 任务完成, status=3 |
| `handleTask_MultiType_OrSign` | 或签：一人同意即通过 | 多节点, 任意一人同意 | 整节点通过 |
| `handleTask_MultiType_AndSign` | 会签：所有人同意才通过 | 多节点, 第一人同意 | 节点不完全, 等待其余 |
| `handleTask_AdminOverride` | 管理员覆盖审批 | 管理员处理他人任务 | 通过 |
| `handleTask_Delegation` | 代理审批 | delegate处理delegator任务 | 通过 |
| `handleTask_Unauthorized` | 未授权用户处理 | 普通用户处理他人任务 | BusinessException |
| `findPendingTask_DirectMatch` | 直接匹配待办任务 | 准确assigneeId | 返回匹配任务 |
| `findPendingTask_DelegationMatch` | 代理匹配 | delegator查找delegate的任务 | 返回匹配任务 |
| `findPendingTask_ReverseDelegation` | 反向代理匹配 | delegate处理delegator任务 | 返回匹配任务 |
| `withdrawProcess_BeforeApproval` | 审批前撤回 | 实例无已处理任务 | status=4(已撤回) |
| `withdrawProcess_AfterFirstApproval` | 部分审批后撤回 | 已有审批记录 | 抛出异常 |
| `transferTask_Success` | 转办任务 | 从A转至B | 任务assignee改为B |
| `returnTask_ToPreviousNode` | 退回上一节点 | taskId, returnTarget=prev | 上一节点重新激活 |
| `returnTask_ToInitiator` | 退回发起人 | taskId, returnTarget=initiator | 流程回退到发起人 |
| `urgeTask_Success` | 催办 | 有效instanceId | 通知审批人 |
| `urgeTask_NoApprover` | 催办-无审批人 | 无效instanceId | 不报错, 日志警告 |
| `saveDefinition_Create` | 创建流程定义 | 完整定义对象 | 入库, 版本号=1 |
| `saveDefinition_Update` | 更新流程定义 | 已存在定义+新nodeConfig | 版本号+1 |
| `listDefinitions_WithData` | 列出所有定义 | 无参数 | 返回列表 |
| `listDefinitions_Empty` | 列表为空 | 数据库无数据 | 返回空列表 |
| `delegateTask_Success` | 设置审批委派 | 有效时间段+被委派人 | 入库 |

#### 2.2.2 考勤服务 -- AttendanceServiceImplTest

| 测试方法 | 测试点 | 预期 |
|---------|--------|------|
| `clockIn_Normal_FirstTime` | 首次正常打卡(9:00前) | status=0(正常) |
| `clockIn_Late` | 迟到打卡(9:00后) | status=1(迟到) |
| `clockIn_AlreadyClockedIn` | 重复打卡 | BusinessException("今日已打卡") |
| `clockIn_AfterLeaveMark` | 请假标记后补打卡 | 保留请假status, 记录打卡时间 |
| `clockOut_Normal` | 正常签退(18:00后) | status不变 |
| `clockOut_EarlyLeave` | 早退(18:00前, 且正常出勤) | status=2(早退) |
| `clockOut_NoClockIn` | 未签到直接签退 | BusinessException |
| `clockOut_AlreadyClockedOut` | 重复签退 | BusinessException |
| `getTodayAttendance_Exists` | 查询当日记录 | 返回记录 |
| `getTodayAttendance_NotExists` | 当日无记录 | 返回null |
| `getAttendanceHistory` | 历史记录查询 | 返回日期范围内记录 |
| `markLeaveAttendance` | 请假自动标记考勤 | 工作日被标记status=5 |
| `markLeaveAttendance_SkipWeekend` | 请假自动标记-跳过周末 | 周末不标记 |
| `markTripAttendance` | 出差自动标记 | 工作日标记status=6 |
| `removeMarkedAttendance` | 清除自动标记 | 指定范围内的标记被删除 |
| `adminPage_WithFilters` | 管理员分页-带筛选 | 按条件过滤 |
| `adminPage_EmptyResult` | 管理员分页-无结果 | 返回空页 |
| `adminPage_NoFilters` | 管理员分页-无筛选 | 返回全部 |

#### 2.2.3 请假服务 -- LeaveApplyServiceImplTest

| 测试方法 | 测试点 | 预期 |
|---------|--------|------|
| `submit_Success` | 提交申请 | status=0, 触发工作流 |
| `submit_NullStartOrEndTime` | 起止时间为空 | BusinessException |
| `approve_Success` | 审批通过 | 触发onUpdateStatus, 扣余额, 标记考勤 |
| `approve_Reject` | 审批驳回 | 不扣余额 |
| `calculateLeaveDays_FullDay` | 全天请假天数 | 精确计算工作日 |
| `calculateLeaveDays_HalfDay` | 半天请假 | 返回0.5 |
| `calculateLeaveDays_CrossWeekend` | 跨周末请假 | 只计工作日 |
| `calculateLeaveDays_SameDay_Half` | 同一天半天 | 0.5 |
| `onUpdateStatus_Approve` | 审批通过后回调 | 扣减余额+标记考勤 |
| `onUpdateStatus_Reversal` | 从已通过变为驳回 | 恢复余额+清除标记 |
| `pageList_ByEmpId` | 按员工分页查询 | 返回本人记录 |
| `pageList_ByStatus` | 按状态筛选 | 返回匹配状态的记录 |
| `pageList_WithoutFilter` | 无条件查询 | 返回所有(带分页) |

#### 2.2.4 BaseApprovalServiceImpl 模板测试

覆盖7种审批类型的公共逻辑：

| 测试方法 | 测试点 |
|---------|--------|
| `doSubmit_SetsStatusAndSaves` | 提交设置status=0并保存 |
| `doSubmit_StartsWorkflow` | 提交后启动工作流 |
| `doApprove_FindPendingTask` | 审批查找待办任务 |
| `doApprove_NoPendingTask_Throws` | 无待办任务抛异常 |
| `doApprove_Delegation_Authorized` | 委派场景授权 |
| `doApprove_Unauthorized_Throws` | 越权操作抛异常 |
| `doUpdateStatus_StatusChange` | 状态变更触发回调 |
| `doUpdateStatus_NullId` | 空ID不操作 |
| `doPageList_EmpIdAndStatusFilter` | 分页+筛选 |
| `doPageList_FillEmpNamesAndRemarks` | 填充姓名和审批意见 |
| `fillEmpNames_Batch` | 批量员工名填充 |
| `fillRemarks_ByApprovalRecord` | 从审批记录填充备注 |

#### 2.2.5 认证服务 -- AuthServiceImplTest

| 测试方法 | 测试点 |
|---------|--------|
| `login_UserNotFound` | 用户名不存在 |
| `login_WrongPassword` | 密码错误 |
| `login_Success` | 登录成功, 返回token+角色 |
| `login_Success_NoRole_DefaultUser` | 无角色时默认USER角色 |
| `login_RecordsLoginLog` | 记录登录日志 |
| `logout_ClearsRedis` | 退出清除Redis中的token |
| `refreshToken_Valid` | 有效刷新token获取新token |
| `refreshToken_InvalidType` | 非refresh类型token |
| `refreshToken_StaleToken` | Redis中已不存在 |
| `register_HashesPassword` | 注册密码BCrypt加密 |

#### 2.2.6 报表服务 -- ReportServiceImplTest

| 测试方法 | 测试点 |
|---------|--------|
| `getPersonalAttendanceSummary` | 个人考勤汇总 |
| `getPersonalAttendanceTrend` | 个人考勤趋势 |
| `getPersonalLeaveSummary` | 个人请假汇总 |
| `getAdminAttendanceSummary` | 管理员考勤汇总 |
| `getAdminDeptCompare` | 部门对比 |
| `getAdminEmployeeRanking` | 员工排名 |
| `getAdminTodayOverview` | 今日概览 |
| `getMonthlyCompare_WithPreviousMonth` | 环比上月 |
| `getDeptAttendanceTrend` | 部门趋势 |

#### 2.2.7 统计服务 -- StatisticsServiceImplTest

| 测试方法 | 测试点 |
|---------|--------|
| `getDashboard_Today` | 今日仪表盘 |
| `getDashboard_Weekly` | 本周仪表盘 |
| `getDashboard_Monthly` | 本月仪表盘 |
| `getDashboard_AllTime` | 全量仪表盘 |
| `getDashboard_EmptyData` | 无数据时的默认值 |

#### 2.2.8 通知公告服务 -- NoticeServiceImplTest

| 测试方法 | 测试点 |
|---------|--------|
| `publishNotice_SetsStatusAndTime` | 发布公告设置状态和时间 |
| `getNoticePage_WithReadStatus` | 分页查询+已读状态 |
| `markAsRead_NewRecord` | 首次阅读创建阅读记录 |
| `markAsRead_AlreadyRead` | 重复阅读不报错 |

### 2.3 单元测试技术规范

**框架与依赖：**
```
JUnit 5 (Spring Boot Starter Test 自带)
Mockito 5 (@Mock, @InjectMocks, @Spy)
AssertJ (fluent assertion)
```

**模板代码示例：**

```java
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private OaAttendanceMapper attendanceMapper;
    @Mock
    private SysEmployeeMapper employeeMapper;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Test
    @DisplayName("迟到打卡 - 9:00后打卡标记为迟到")
    void clockIn_Late() {
        // 准备
        when(attendanceMapper.selectOne(any())).thenReturn(null);
        // 通过反射设置固定时间（使用Clock或LocalDateTime的封装）
        
        // 执行
        attendanceService.clockIn(1L);
        
        // 验证
        ArgumentCaptor<OaAttendance> captor = ArgumentCaptor.forClass(OaAttendance.class);
        verify(attendanceMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }
}
```

**边界条件覆盖清单（每方法至少覆盖）：**
- 正常输入
- null/空值
- 边界值（如9:00整、18:00整）
- 重复操作
- 无权限（越权）
- 无数据（空列表）
- 非法状态流转

---

## 3. 层级二：后端集成测试 (Spring Boot Test)

### 3.1 测试目录结构

```
code/backend/oa-web/src/test/java/cn/oa/
  integration/
    controller/       (补充现有WebMvcTest, 用@SpringBootTest + TestRestTemplate)
    repository/        (MyBatis Mapper集成, @MybatisPlusTest + H2)
    cache/             (Redis集成, @SpringBootTest + TestRedisConfiguration)
    workflow/          (完整审批流程集成)
    file/              (文件上传下载集成)
  config/
    TestRedisConfig.java
    TestSecurityConfig.java
```

### 3.2 Controller层集成测试

基于现有的 `BaseControllerTest` + `@WebMvcTest` 模式扩展。

**需增强的Controller测试（填补现有空白）：**

| Controller | 增加的关键测试 |
|-----------|--------------|
| `MenuControllerTest` | 菜单树构建、角色菜单分配、权限校验 |
| `ConfigControllerTest` | 参数CRUD、缓存刷新 |
| `DictControllerTest` | 字典类型+数据CRUD、级联删除 |
| `EmployeeControllerTest` | 分页、CRUD、密码重置、状态变更 |
| `ReportControllerTest` | 各类报表查询、日期范围、无数据 |
| `StatisticsControllerTest` | 仪表盘、时间段聚合 |
| `ScheduleControllerTest` | 日程CRUD、时间冲突检测 |
| `SalaryControllerTest` | 薪资结构CRUD、我的薪资 |
| `AlertControllerTest` | 预警规则+日志、处理预警 |
| `AssetControllerTest` | 资产+借用CRUD、归还流程 |
| `DeptControllerTest` | 部门树、CRUD、移动 |
| `PostControllerTest` | 岗位CRUD |
| `ExpenseControllerTest` | 提交+审批、导出 |
| `LoanControllerTest` | 借款+还款流程 |
| `PurchaseControllerTest` | 采购提交+审批 |
| `BusinessTripControllerTest` | 出差提交+审批 |
| `OutingControllerTest` | 外出提交+审批 |
| `OvertimeControllerTest` | 加班提交+审批 |
| `DocumentControllerTest` | 上传、下载、删除 |
| `MeetingControllerTest` | 会议室+会议CRUD、取消、冲突检测 |
| `MessageControllerTest` | 发送、阅读、未读数 |
| `TodoControllerTest` | 待办列表、完成、忽略 |
| `MonitorControllerTest` | 在线用户、日志查询 |
| `SystemManageControllerTest` | 用户管理、角色分配、菜单分配 |
| `AttendanceGroupControllerTest` | 考勤组CRUD、员工分配 |

**每Controller测试用例数：** 每个现有测试类保持5-15个 `@Test` 方法，覆盖所有API端点。

### 3.3 数据库交互测试 (@SpringBootTest + Testcontainers)

使用 Testcontainers 启动真实MySQL进行集成测试：

| 测试类 | 验证点 |
|--------|--------|
| `EmployeeRepositoryIT` | MyBatis Mapper CRUD, 分页插件 |
| `WorkflowRepositoryIT` | 流程定义/实例/任务表的CRUD+复杂查询 |
| `LeaveApplyRepositoryIT` | 请假表与审批记录联合查询 |
| `AttendanceRepositoryIT` | 考勤记录按日期范围批量查询 |
| `MessageRepositoryIT` | 消息发送+已读状态查询 |

**Testcontainers配置：**

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("it")
class WorkflowRepositoryIT {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("oa_system_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry reg) {
        reg.add("spring.datasource.url", mysql::getJdbcUrl);
        reg.add("spring.datasource.username", mysql::getUsername);
        reg.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### 3.4 Redis集成测试

| 测试类 | 验证点 |
|--------|--------|
| `RedisCacheIT` | token存储与过期、captcha存储与获取、角色缓存 |
| `RedisSessionIT` | 会话管理、并发登录控制 |
| `RedisRateLimitIT` | 限流计数器 |

使用 `@SpringBootTest` + 嵌入式Redis(`redis.embedded.RedisServer`) 或 Testcontainers Redis。

### 3.5 文件上传/下载集成测试

| 测试类 | 验证点 |
|--------|--------|
| `FileUploadDownloadIT` | multipart上传、文件保存到磁盘、文件下载、删除文件、文件不存在处理、路径穿越防护 |

### 3.6 工作流完整流程集成测试

**场景覆盖：**

| 场景 | 步骤 | 断言 |
|------|------|------|
| 单人审批-通过 | 提交->审批通过 | status=1, 回调触发 |
| 单人审批-驳回 | 提交->审批驳回 | status=3 |
| 会签-全部通过 | 提交->A同意->B同意->C同意 | 全部通过后流程结束 |
| 会签-部分驳回 | 提交->A同意->B驳回 | status=3 |
| 或签-一人通过 | 提交->A同意 | 节点跳过其余人 |
| 条件分支-大额 | amount>10000进入高级审批 | 高级审批节点 |
| 撤回-审批前 | 提交->发起人撤回 | status=4 |
| 转办 | 提交->管理员转办B->B审批 | 流程正常结束 |
| 委派 | A设置委派给B->提交->B审批 | 流程正常结束 |
| 退回-上一节点 | 提交->A同意->B退回给A | A重新待办 |
| 催办 | 提交->发起人催办 | 审批人收到通知 |

---

## 4. 层级三：前端Web E2E测试 (Playwright)

### 4.1 场景1: 员工的一天

业务描述：普通员工张三从上班到下班的全流程操作。

```
@Test("员工的一天 - 打卡 -> 查看公告 -> 提交请假 -> 接收消息")
步骤:
  1. 登录页: 输入用户名密码验证码登录
  2. 首页/工作台: 验证dashboard渲染, 查看今日待办数
  3. 考勤打卡: 
     - 点击打卡按钮
     - 验证打卡成功提示
     - 查看今日考勤状态
  4. 通知公告: 
     - 进入公告列表
     - 查看最新公告
     - 标记已读
  5. 请假申请:
     - 选择请假类型(年假)
     - 填写起止日期
     - 填写请假原因
     - 提交申请
     - 验证提交成功
  6. 我的申请:
     - 查看刚提交的请假记录
     - 验证状态为"审批中"
  7. 内部消息:
     - 查看未读消息
     - 点击消息查看详情
     - 验证消息列表刷新
  8. 签退:
     - 点击签退按钮
     - 验证签退成功
```

### 4.2 场景2: 管理员的一天

业务描述：管理员admin处理待办、管理员工、发布公告、查看报表。

```
@Test("管理员的一天 - 审批 -> 管理 -> 公告 -> 报表")
步骤:
  1. 以管理员身份登录
  2. 工作台: 查看待办审批数、预警数、公告数
  3. 审批中心:
     - 查看所有待审批列表
     - 通过第一个请假申请
     - 驳回第二个请假申请
     - 验证已处理任务列表更新
  4. 员工管理:
     - 查看员工列表
     - 搜索员工
     - 编辑员工信息(修改电话)
     - 验证修改生效
  5. 发布公告:
     - 填写公告标题和内容
     - 选择公告类型
     - 发布
     - 验证公告列表新增
  6. 管理报表:
     - 查看考勤汇总报表
     - 查看部门对比
     - 查看员工排名
     - 切换月份查看历史
  7. 数据看板:
     - 验证图表正确渲染
     - 验证数据数字正确显示
```

### 4.3 场景3: 审批全流程 (7种审批类型)

```
@Test("7种审批类型全流程")
describe("请假审批") {
  test("员工提交->管理员审批->验证状态")
}
describe("出差审批") {
  test("员工提交->部门经理审批->验证状态")
}
describe("外出审批") {
  test("员工提交->审批通过->验证状态")
}
describe("加班审批") {
  test("员工提交->审批通过->自动记录加班时长")
}
describe("报销审批") {
  test("员工提交->财务审批->验证金额核算")
}
describe("采购审批") {
  test("员工提交->部门审批->采购验收")
}
describe("借款审批") {
  test("员工提交->审批通过->验证借款记录")
}
```

每种审批类型测试包含：
1. 填写并提交申请
2. 管理员侧查看待办
3. 执行通过/驳回
4. 员工侧查看申请状态变化
5. 验证审批链中的历史记录

### 4.4 场景4: 系统管理

```
@Test("系统管理 - 员工/角色/菜单/部门/字典/配置")
describe("员工管理") {
  test("CRUD: 新增员工 -> 修改 -> 搜索 -> 删除")
  test("权限: 分配角色 -> 验证菜单权限生效")
}
describe("角色管理") {
  test("CRUD: 新增角色 -> 分配菜单权限 -> 修改 -> 删除")
}
describe("菜单管理") {
  test("CRUD: 新增菜单 -> 修改 -> 删除 -> 验证树结构")
}
describe("部门管理") {
  test("CRUD: 新增子部门 -> 移动 -> 删除")
  test("树结构: 展开折叠验证")
}
describe("字典管理") {
  test("字典类型CRUD: 新增类型 -> 新增字典项 -> 修改 -> 删除")
  test("字典数据级联删除")
}
describe("参数配置") {
  test("CRUD + 缓存刷新")
}
```

---

## 5. 层级四：移动端E2E测试 (Playwright)

### 5.1 场景1: 通勤打卡

```
@Test("移动端通勤打卡")
步骤:
  1. 登录移动端
  2. 首页(Tab1): 查看今日考勤状态
  3. 点击打卡按钮:
     - 使用GPS位置mock(固定公司坐标)
     - 验证打卡成功
  4. 再次点击打卡按钮:
     - 验证"今日已打卡"提示
  5. 访问考勤页面:
     - 查看月度考勤日历
     - 验证打卡记录展示
  6. 签退:
     - 点击签退
     - 验证成功
```

### 5.2 场景2: 移动审批

```
@Test("移动端审批全流程")
步骤:
  1. 以管理员身份登录移动端
  2. 首页查看待办数(红点/数字)
  3. 进入待办列表:
     - 查看所有待办
     - 选择一条请假审批
  4. 审批处理:
     - 查看申请详情(事由/时间/天数)
     - 填写审批意见
     - 点击"通过"
  5. 验证:
     - 列表刷新，该待办消失
     - 已办列表中出现该记录
  6. 切换为员工身份:
     - 查看我的申请
     - 验证审批状态更新
```

### 5.3 场景3: 移动端表单提交

覆盖全部7种申请类型，每种一个测试：

```
@Test("移动端请假申请")
@Test("移动端出差申请")
@Test("移动端外出申请")
@Test("移动端加班申请")
@Test("移动端报销申请")
@Test("移动端采购申请")
@Test("移动端借款申请")
```

每种测试结构：
1. 导航到对应申请页面
2. 填充表单字段（日期选择、下拉选择、文本输入）
3. 提交申请
4. 验证提交成功
5. 在"我的申请"列表中验证

### 5.4 场景4: Tab导航完整流程

```
@Test("移动端完整Tab导航")
步骤:
  1. 验证底部4个Tab: 首页、待办、OA、我的
  2. Tab1-首页: 
     - 验证考勤卡片
     - 验证待办统计
     - 验证公告列表
     - 验证快捷入口(打卡、请假、审批等)
  3. Tab2-待办:
     - 验证待办列表
     - 上拉加载更多
     - 点击进入详情
  4. Tab3-OA:
     - 验证功能入口网格(请假、出差、外出、加班、报销、采购、借款、公告、日程、文档)
     - 点击每个入口验证跳转
     - 返回OA主页
  5. Tab4-我的:
     - 验证用户信息
     - 查看我的申请
     - 查看已办
     - 查看抄送
     - 验证退出登录
```

---

## 6. 层级五：CI集成与质量门禁

### 6.1 GitHub Actions 工作流改造

#### 6.1.1 新增 workflow: unit-test.yml

```yaml
name: "Unit Tests & Coverage"

on:
  push:
    branches: [master]
    paths: ['code/backend/**']
  pull_request:
    branches: [master]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test_password
          MYSQL_DATABASE: oa_system_test
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping -h localhost" --health-interval=10s --health-timeout=5s --health-retries=5
      redis:
        image: redis:7
        ports:
          - 6379:6379
        options: --health-cmd="redis-cli ping" --health-interval=10s --health-timeout=5s --health-retries=5

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Initialize database
        run: |
          sed 's/oa_system/oa_system_test/g' code/backend/sql/oa_system_full.sql | mysql -h 127.0.0.1 -u root -ptest_password
          sed 's/oa_system/oa_system_test/g; s/CREATE DATABASE[^;]*;//g' code/backend/sql/seed_data.sql | mysql --force -h 127.0.0.1 -u root -ptest_password oa_system_test
      - name: Run unit tests with coverage
        working-directory: code/backend
        run: mvn test -B -Dspring.profiles.active=ci
      - name: Upload JaCoCo coverage report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: code/backend/**/target/site/jacoco/
      - name: Enforce coverage threshold
        run: |
          # Extract coverage from jacoco CSV and fail if below 80%
          python3 -c "
          import csv, sys
          with open('code/backend/oa-service/target/site/jacoco/jacoco.csv') as f:
              reader = csv.DictReader(f)
              total_covered = total_missed = 0
              for row in reader:
                  total_covered += int(row['COVERED_LINE'])
                  total_missed += int(row['MISSED_LINE'])
          rate = total_covered / (total_covered + total_missed) * 100
          print(f'Line coverage: {rate:.2f}%')
          if rate < 80.0:
              print(f'FAIL: Coverage {rate:.2f}% < 80%')
              sys.exit(1)
          "
```

#### 6.1.2 修改 backend.yml (原有)

改为执行完整Maven构建（含编译+运行单元测试），不再 skip test：

```yaml
# 改动点只有一处:
# 之前: mvn clean package -Dmaven.test.skip=true -B -V
# 改为: mvn clean package -B -V
# 注：需要提供MySQL/Redis服务
```

#### 6.1.3 新增 workflow: integration-test.yml

```yaml
name: "Integration Tests"

on:
  workflow_run:
    workflows: ["Unit Tests & Coverage"]
    types: [completed]
    branches: [master]

jobs:
  integration-test:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    services:
      mysql: ...
      redis: ...
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - name: Init DB with test data
        run: ...
      - name: Run integration tests
        working-directory: code/backend
        run: mvn verify -B -Pintegration-test
      - name: Upload integration test report
        uses: actions/upload-artifact@v4
        with:
          name: integration-test-report
          path: code/backend/oa-web/target/failsafe-reports/
```

#### 6.1.4 修改 api-test.yml (增强)

在现有API测试基础上增加：
1. Playwright前端E2E测试（并行执行）
2. Playwright移动端E2E测试
3. 统一的HTML报告生成

```yaml
# 在 api-test.yml 的 "Run API tests" 步骤后增加:
- name: Install Playwright browsers
  run: npx playwright install chromium

- name: Run Frontend E2E tests
  run: npx playwright test tests/frontend-ui-test.spec.ts --config=tests/playwright.config.ts
  
- name: Run Mobile E2E tests
  run: npx playwright test tests/mobile-ui-test.spec.ts --config=tests/playwright.config.ts

- name: Generate HTML test report
  if: always()
  run: npx playwright show-report test-results --host=0.0.0.0 --port=9323 &
```

### 6.2 测试报告与可视化

| 报告类型 | 工具 | 产出位置 |
|---------|------|---------|
| 单元测试覆盖率 | JaCoCo | `target/site/jacoco/` |
| 集成测试报告 | Maven Failsafe | `target/failsafe-reports/` |
| E2E测试报告 | Playwright HTML Reporter | `test-results/` |
| 汇总仪表盘 | GitHub Pages + badges | 整合到README |

**覆盖度量标准：**

| 阈值 | Service层 | Controller层 | Model层 |
|------|-----------|-------------|---------|
| 行覆盖率目标 | >= 80% | >= 70% | >= 50% |
| 方法覆盖率目标 | >= 85% | >= 75% | >= 60% |
| 分支覆盖率目标 | >= 75% | N/A | N/A |
| 硬性门禁 | >= 70% | >= 60% | >= 40% |

### 6.3 质量门禁配置

```yaml
# 门禁规则（CI自动检查）
quality-gates:
  - name: "Unit test pass rate"
    rule: "100% tests passing"
    action: "block merge"
  - name: "Service layer coverage"
    rule: ">= 70% line coverage"
    action: "warn in PR comment"
  - name: "No regression in coverage"
    rule: "coverage >= base branch coverage"
    action: "block merge"
```

### 6.4 测试数据管理

| 数据 | 来源 | 维护策略 |
|------|------|---------|
| 单元测试Mock数据 | test-fixture内联构造 | 每方法独立 |
| 集成测试种子数据 | `seed_data.sql` | 版本管理 |
| E2E测试数据 | API创建 + seed_data | 开始前准备, 结束后清理 |
| 数据库初始化 | CI中的sed替换 | 随SQL脚本更新 |

---

## 7. 优先级与实施路线图

### Phase 1 (核心覆盖 - 2周)

| 任务 | 工作量 | 交付物 |
|------|--------|--------|
| WorkflowServiceImpl 单元测试(20断言) | 3天 | 测试类, >85%覆盖率 |
| AttendanceServiceImpl 单元测试(15断言) | 2天 | 测试类, >80%覆盖率 |
| LeaveApplyServiceImpl 单元测试(12断言) | 2天 | 测试类, >80%覆盖率 |
| AuthServiceImpl 单元测试(10断言) | 1天 | 测试类, >80%覆盖率 |
| BaseApprovalServiceImpl 单元测试(10断言) | 2天 | 测试类, >85%覆盖率 |
| CI unit-test.yml workflow | 1天 | 稳定运行的CI流水线 |

### Phase 2 (集成测试 - 2周)

| 任务 | 工作量 | 交付物 |
|------|--------|--------|
| Testcontainers配置 | 1天 | 可复用的IT基础设施 |
| Workflow完整流程集成测试(10场景) | 3天 | 真实审批场景验证 |
| Controller测试补充(20个类各+5用例) | 4天 | 全部35Controller覆盖 |
| Redis集成测试 | 1天 | 缓存+会话测试 |
| 文件上传下载测试 | 1天 | 文件操作测试 |

### Phase 3 (前端E2E增强 - 1周)

| 任务 | 工作量 | 交付物 |
|------|--------|--------|
| 场景1: 员工的一天 | 1天 | 新spec文件 |
| 场景2: 管理员的一天 | 1天 | 新spec文件 |
| 场景3: 7种审批流程 | 2天 | 7个审批场景测试 |
| 场景4: 系统管理 | 1天 | 管理功能E2E |
| 移动端场景增强 | 1天 | 完善移动端测试 |

### Phase 4 (CI完善 - 1周)

| 任务 | 工作量 | 交付物 |
|------|--------|--------|
| 集成测试CI workflow | 1天 | integration-test.yml |
| JaCoCo覆盖率门禁 | 1天 | PR自动检查覆盖率 |
| Playwright E2E CI集成 | 2天 | 前端UI测试自动化 |
| README + badges | 1天 | 可视化项目状态 |

---

## 8. 附录：测试数据管理策略

### 8.1 单元测试数据

- 使用 Mockito 模拟所有外部依赖（DB、Redis）
- 测试数据在方法体内构造 POJO，使用 `ReflectionTestUtils` 设置ID
- 时间敏感测试使用 `Clock` 依赖注入或 `LocalDateTime` 的封装

### 8.2 集成测试数据

- 基础数据从 `seed_data.sql` 加载
- Testcontainers 每次运行前执行SQL初始化
- 每个测试类使用 `@Sql` 注解按需加载额外数据
- 使用 `@Transactional` 保证测试间数据隔离

### 8.3 E2E测试数据

- 每次运行前通过API创建所需数据（先登录->发请求）
- 测试完成后通过API清理创建的数据
- 数据创建使用独立前缀（如 `E2E_TEST_`）便于清理
- 测试间不共享状态，按 `test.describe.configure({ mode: 'serial' })` 串行

---

### Critical Files for Implementation

- `E:\JavaProject\Corporate_OA_System\code\backend\oa-service\src\main\java\cn\oa\service\impl\WorkflowServiceImpl.java`
- `E:\JavaProject\Corporate_OA_System\code\backend\oa-service\src\main\java\cn\oa\service\impl\BaseApprovalServiceImpl.java`
- `E:\JavaProject\Corporate_OA_System\code\backend\oa-web\src\test\java\cn\oa\controller\BaseControllerTest.java`
- `E:\JavaProject\Corporate_OA_System\tests\api-test.sh`
- `E:\JavaProject\Corporate_OA_System\code\backend\pom.xml`