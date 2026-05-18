
Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

Tradeoff: These guidelines bias toward caution over speed. For trivial tasks, use judgment.

1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.
2. Simplicity First
Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, 和 clarifying questions come before implementation rather than after mistakes.


# Corporate OA System - 企业OA办公系统

## 项目概述

企业内部OA办公系统，分员工端和管理员端。后端 Spring Boot 3 + MyBatis-Plus + Redis + MySQL，前端 vue-pure-admin (Vue 3 + Element Plus + Tailwind CSS)。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.4.5 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | - |
| 认证 | JWT (jjwt) | 0.12.5 |
| 工具库 | Hutool | 5.8.35 |
| API文档 | Knife4j (OpenAPI3) | 4.5.0 |
| 前端框架 | Vue 3 + TypeScript | - |
| UI组件 | Element Plus | - |
| 样式 | Tailwind CSS 4 | - |
| 路由 | Vue Router 5 | - |
| 状态管理 | Pinia | - |
| 构建工具 | Vite 6 | - |
| 包管理 | pnpm | - |
| Java | JDK 17 | - |

## 项目结构

```
code/
├── backend/                        # 后端 Maven 多模块
│   ├── pom.xml                     # 父POM，统一版本管理
│   ├── oa-common/                  # 通用模块（拦截器、工具类、异常处理、Redis工具）
│   ├── oa-model/                   # 实体/DTO/VO 模块
│   ├── oa-mapper/                  # MyBatis-Plus Mapper 接口
│   ├── oa-service/                 # 业务 Service 接口及实现
│   ├── oa-web/                     # Controller + 启动类 + 配置
│   └── init_test_data.sql          # 测试数据初始化脚本
│
└── frontend/                       # 前端 vue-pure-admin
    ├── src/
    │   ├── api/                    # API 请求封装
    │   │   ├── oa/                 # OA业务API（attendance/leave/notice/document/schedule/message/statistics/report/employee/dept）
    │   │   ├── routes.ts           # 动态路由API
    │   │   ├── user.ts             # 登录/用户API
    │   │   └── system.ts           # 系统管理API
    │   ├── views/
    │   │   ├── oa/                 # OA业务页面（17个）
    │   │   ├── system/             # 系统管理页面（user/role/menu/dept）
    │   │   ├── monitor/            # 系统监控页面
    │   │   ├── login/              # 登录页
    │   │   └── welcome/            # 首页/工作台
    │   ├── router/                 # 路由配置（动态路由从后端加载）
    │   ├── store/                  # Pinia 状态管理
    │   ├── layout/                 # 布局组件
    │   ├── components/             # 通用组件
    │   ├── utils/                  # 工具函数
    │   └── config/                 # 全局配置（从platform-config.json加载）
    ├── public/
    │   └── platform-config.json    # 前端平台配置（主题、布局等）
    ├── .env                        # 环境变量
    └── vite.config.ts              # Vite配置（含proxy代理）
```

## 后端模块说明

### oa-common — 通用模块
- `interceptor/AuthInterceptor.java` — JWT认证拦截器，校验Token + Redis会话
- `interceptor/RateLimitInterceptor.java` — 登录限流（5次/分钟/IP）
- `annotation/OperationLog.java` — 操作日志注解
- `service/RedisService.java` — Redis通用工具类
- `result/R.java` — 统一响应封装（`code:0` 成功，`code:-1` 失败，`code:401` 未授权）
- `config/WebMvcConfig.java` — 拦截器注册，排除 `/login`、`/refresh-token`、swagger路径
- `utils/JwtUtil.java` — JWT工具类
- `exception/GlobalExceptionHandler.java` — 全局异常处理

### oa-model — 实体/DTO
- `entity/Sys*.java` — 系统表实体（Employee, Dept, Role, EmpRole）
- `entity/Oa*.java` — OA业务实体（Attendance, LeaveApply, Notice, Document, Schedule, Message等）
- `entity/*VO.java` — 视图对象（LoginVO, DashboardVO, PersonalReportVO, AdminReportVO等）
- `entity/*DTO.java` — 数据传输对象（LoginDTO, LeaveApplyDTO, ReportQueryDTO等）

### oa-mapper — 数据访问层
- MyBatis-Plus Mapper接口，对应每张数据库表

### oa-service — 业务逻辑层
- `AuthService` — 登录/登出/刷新Token、角色分配、在线用户写入Redis
- `AttendanceService` — 考勤打卡（Redis去重）、查询、统计
- `LeaveApplyService` — 请假申请/审批
- `NoticeService` — 公告管理、已读标记（Redis Set）
- `ReportService` — 数据报表（个人/管理员，含Redis缓存）
- `StatisticsService` — 统计仪表盘数据（Redis缓存）
- 其他：Dept、Document、Employee、Message、Schedule、OnlineUser、OperationLog

### oa-web — 控制层
- `AuthController.java` — `POST /login`、`POST /refresh-token`、`POST /logout`
- `RouteController.java` — `GET /get-async-routes` 返回动态路由（按角色过滤）
- `SystemManageController.java` — 系统管理（用户/角色/菜单/部门CRUD）
- `MonitorController.java` — 系统监控（在线用户、日志）
- OA业务Controller：Attendance、LeaveApply、Notice、Document、Schedule、Message、Report、Statistics
- `aspect/OperationLogAspect.java` — 操作日志AOP切面

## 前端关键机制

### 动态路由
- 登录后前端调用 `GET /get-async-routes` 获取路由配置
- 后端 `RouteController` 返回带 `meta.roles` 的路由树
- 前端 `filterNoPermissionTree()` 自动过滤无权限菜单
- OA管理端路由设 `roles: ["admin"]`，员工端不设 roles

### 角色体系
- `ADMIN`（管理员）：看到数据看板、员工管理、考勤管理、请假审批、公告管理、文档管理、管理员报表等
- `USER`（普通员工）：看到工作台、我的考勤、请假申请、公告通知、文档中心、我的日程、消息中心、个人报表等

### API请求
- 统一通过 `src/utils/http/` 封装的 axios 实例
- Token 放在 `Authorization: Bearer <token>` 请求头
- 前端代理配置在 `vite.config.ts`，`/api`、`/login`、`/get-async-routes` 等代理到后端 8080

## 数据库

- 数据库名：`oa_system`
- 12张核心表：`sys_dept`、`sys_employee`、`sys_role`、`sys_emp_role`、`oa_attendance`、`oa_leave_apply`、`oa_notice`、`oa_notice_read`、`oa_document`、`oa_schedule`、`oa_message`、`oa_approval_record`
- 新增表：`oa_operation_log`、`oa_login_log`、`oa_document_category`
- 字符集：`utf8mb4`
- 连接信息在 `application-local.yml`（不纳入版本控制）

## Redis使用场景

| Key模式 | 用途 | TTL |
|---------|------|-----|
| `token:{empId}` | Token会话管理 | 2h |
| `attendance:{empId}:{date}` | 打卡去重 | 24h |
| `notice:read:{noticeId}` | 公告已读用户Set | - |
| `cache:dept:tree` | 部门树缓存 | 30min |
| `cache:stats:dashboard:{period}` | 仪表盘统计缓存 | 5min |
| `cache:report:personal:{empId}:{month}` | 员工个人报表缓存 | 10min |
| `cache:report:admin:{type}:{period}` | 管理员报表缓存 | 10min |
| `msg:unread:{empId}` | 消息未读计数 | - |
| `online:user:{empId}` | 在线用户Hash | 30min(续期) |
| `rate:login:{ip}` | 登录限流计数 | 60s |

## 统一响应格式

```json
{"code": 0, "message": "操作成功", "data": ...}
```
- `code: 0` — 成功（vue-pure-admin 前端判断 `code === 0`）
- `code: -1` — 业务失败
- `code: 401` — 未授权

## 开发命令

```bash
# 后端构建
cd code/backend
mvn clean package -Dmaven.test.skip=true

# 后端运行
java -jar oa-web/target/oa-web-1.0.0.jar

# 前端安装依赖
cd code/frontend
pnpm install

# 前端运行
pnpm dev

# 后端API文档
# http://localhost:8080/doc.html
```

## 端口

| 服务 | 端口 |
|------|------|
| 后端 Spring Boot | 8080 |
| 前端 Vite Dev | 8848 |

## 环境变量

### 前端 (.env)
- `VITE_PORT=8848`
- `VITE_ROUTER_HISTORY=hash`
- `VITE_USE_MOCK=false`

### 后端
- `application.yml` — 公共配置（端口、MyBatis、Jackson等）
- `application-local.yml` — **敏感信息**（数据库密码、Redis地址），已加入 `.gitignore`，不纳入版本控制

## 注意事项

- 修改 `application-local.yml` 中的数据库密码后才能启动后端
- Node.js 版本需 >= 20.13，Vite 6 最高支持 Node 20.x
- 前端 `package.json` 中的 `dev` 脚本已改为 `vite`（原版使用 `NODE_OPTIONS=--max-old-space-size=4096` 在 Windows 不兼容）
- `layout/hooks/useDataThemeChange.ts` 和 `store/modules/app.ts` 已添加空值保护，防止 localStorage 未初始化时报错
- 密码使用 Hutool BCrypt 加密：`cn.hutool.crypto.digest.BCrypt.hashpw("123456", BCrypt.gensalt())`
