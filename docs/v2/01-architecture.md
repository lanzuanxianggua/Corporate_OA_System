# 01 - Corporate OA System v2 主架构

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**（不实现，只定义）
> 前置阅读: `00-index.md`
> 依赖文档: 本文件 → 02/03/04/05/06/07/08

---

## 1. 业务概览

**Corporate OA System** 是一套面向中大型企业（员工数 200-5000）的办公自动化系统，覆盖以下业务域：

| 业务域 | 覆盖范围 | 关键场景 |
|--------|----------|----------|
| 行政 | 印章/资产/办公用品/会议室 | 借印章、领用笔记本、订会议室 |
| 文档 | 发文/收文/签报/档案 | 发通知、收外部文件、签报审批 |
| 财务 | 预算/报销/借款/还款 | 报销差旅、申请借款、月度预算 |
| 人事 | 请假/考勤/招聘/培训/绩效/员工档案 | 请年假、打卡、招聘流程、培训记录 |
| 知识 | 知识库/文档检索/SOP | 入职必读、SOP 检索 |
| 消息 | 站内消息/系统通知/邮件 | 审批提醒、催办 |
| 任务 | 待办/项目/工时 | 个人待办、跨部门项目、工时统计 |
| 工作流 | 审批引擎/流程定义/任务委派 | 任意审批场景 |

**用户角色**（RBAC，5 种固定角色）：
- `ADMIN` 超级管理员（IT 维护）
- `HR` 人事（HR 业务全权）
- `FINANCE` 财务
- `MANAGER` 部门经理
- `EMPLOYEE` 普通员工

**数据权限**（5 级，与角色正交）：
- `SELF` 仅本人
- `DEPT` 本部门
- `DEPT_DOWN` 本部门及下级
- `COMPANY` 全公司
- `ALL` 全部（含跨公司）

---

## 2. 业务模块清单（v2 共 18 个业务模块 + 3 个平台模块）

### 2.1 平台模块（3 个）

| 模块 | Maven artifactId | 职责 | 关键依赖 |
|------|------------------|------|----------|
| `oa-platform-common` | oa-platform-common | 通用工具/异常/常量/分页/响应包装 | 无 |
| `oa-platform-security` | oa-platform-security | 安全（JWT/RBAC/数据权限/审计/限流/接口签名） | oa-platform-common |
| `oa-platform-web` | oa-platform-web | 启动模块（Application/配置/Mapper扫描/CORS） | oa-platform-security |

### 2.2 工作流模块（1 个）

| 模块 | Maven artifactId | 职责 |
|------|------------------|------|
| `oa-workflow` | oa-workflow | 自研工作流引擎（定义/实例/任务/委派/审批人解析/回调） |

### 2.3 业务模块（14 个）

| # | 模块 | Maven artifactId | 业务域 | v2 是否需要 |
|---|------|------------------|--------|------------|
| 1 | 行政 | oa-admin | 印章/资产/用品 | 是 |
| 2 | 文档 | oa-document | 发文/收文/签报 | 是 |
| 3 | 财务 | oa-finance | 预算/报销/借款 | 是 |
| 4 | HR 请假 | oa-hr-leave | 请假/假期余额/规则 | 是（**核心试点**） |
| 5 | HR 考勤 | oa-hr-attendance | 打卡/排班/统计 | 是 |
| 6 | HR 员工 | oa-hr-employee | 员工档案/合同 | 是 |
| 7 | HR 绩效 | oa-hr-performance | 绩效目标/评估 | 是 |
| 8 | HR 招聘 | oa-hr-recruitment | 招聘需求/简历 | 是 |
| 9 | HR 培训 | oa-hr-training | 培训计划/记录 | 是 |
| 10 | 知识库 | oa-knowledge | 知识条目/检索 | 是 |
| 11 | 消息 | oa-message | 站内消息/系统通知 | 是 |
| 12 | 会议 | oa-meeting | 会议室/会议 | 是 |
| 13 | 任务 | oa-task | 待办/项目/工时 | 是 |
| 14 | 系统 | oa-system | 字典/配置/审计 | 是 |

**注意**：v1 的 `oa-hr` 是一个聚合模块（考勤/请假/员工/绩效/招聘/培训全在一起），v2 拆为 6 个独立 Maven 模块（`oa-hr-leave`/`oa-hr-attendance`/`oa-hr-employee`/`oa-hr-performance`/`oa-hr-recruitment`/`oa-hr-training`），**强内聚弱耦合**。

---

## 3. 整体技术栈

| 维度 | 选型 | 版本 | 理由 |
|------|------|------|------|
| JDK | OpenJDK | 17 LTS | 性能好、生态成熟、长期支持 |
| 构建 | Maven | 3.9+ | 多模块构建稳定 |
| 框架 | Spring Boot | 3.4.x | Java 17 原生支持、Jakarta EE 9 |
| ORM | MyBatis-Plus | 3.5.x | 国产、动态 SQL 友好、代码生成 |
| 数据库 | MySQL | 8.0+ | 主流、字符集/事务成熟 |
| 连接池 | Druid | 1.2.24 | 阿里巴巴 Druid，监控+防SQL注入 |
| 缓存 | Redis | 7.x | 高性能、丰富数据结构 |
| 消息 | Redis Pub/Sub | 7.x | v2 暂不引入 MQ |
| WebSocket | Spring WebFlux | 6.x | 消息推送 |
| 工具 | Lombok/Hutool/MapStruct | - | 减少样板代码 |
| 测试 | JUnit 5/Mockito/Testcontainers | - | v2 强制 |
| 前端 | Vue 3 + TypeScript | Vue 3.5+ | 组合式 API、类型安全 |
| 前端构建 | Vite | 6.x | 快速 |
| UI 库 | Element Plus | 2.x | 兼容 vue-pure-admin |
| 状态 | Pinia | 2.x | 官方推荐 |
| 路由 | Vue Router | 4.x | - |
| HTTP | Axios | 1.x | 拦截器/取消 |
| 移动端 | uni-app + Vue 3 | - | 跨端 |

**架构决策**：
- **不使用 Spring Security** — 项目自研 JWT + Servlet Filter + Spring Interceptor 安全栈。原因：Spring Security 的 filter chain 模型与项目的 InterceptorRegistry 冲突，且 UserContext（ThreadLocal）与 SecurityContextHolder 不兼容。如需 Spring Security 功能，需先在设计 review 中讨论。

---

## 4. 分层架构（Clean Architecture 风格）

```
┌──────────────────────────────────────────────────────────────┐
│  Controller (REST API, 薄)                                    │
│  - 接收 DTO/参数解析/权限校验/响应包装                         │
│  - 禁止: 业务逻辑/事务/数据库操作                              │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  Service (业务逻辑, 厚)                                       │
│  - 业务规则/状态机/计算/校验/事务                              │
│  - 跨 Manager 调用, 跨模块通过事件/接口                         │
│  - 所有写操作必须经过 Service                                  │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  Manager (横切关注点, 可选)                                    │
│  - IdGen/RedisLock/CacheManager/RateLimiter                   │
│  - 可在 Service 间复用, 不依赖业务                              │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  Mapper (MyBatis-Plus BaseMapper)                              │
│  - 单表 CRUD 用 BaseMapper                                     │
│  - 复杂 SQL 用 @Select/@Update 注解或 XML                      │
│  - 禁止: 在 Controller 手写 SQL                                │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  Entity (PO) - DTO - VO - BO                                   │
│  - Entity: 表映射                                              │
│  - DTO: 入参 (Controller 接收)                                │
│  - VO: 出参 (Controller 返回)                                  │
│  - BO: 内部 Service 间传递 (可跨表聚合)                        │
└──────────────────────────────────────────────────────────────┘
```

**禁止**:
- Controller 直接调用 Mapper（绕过 Service）
- Service 直接返回 Entity 给 Controller
- Mapper 跨模块调用（必须通过 Service 间接调用）

---

## 5. Maven 模块依赖图

```
oa-platform-web (启动)
  ├── oa-platform-security
  │     └── oa-platform-common
  ├── oa-workflow
  │     ├── oa-platform-common
  │     └── oa-platform-security
  ├── oa-admin
  │     ├── oa-platform-common
  │     ├── oa-platform-security
  │     └── oa-workflow
  ├── oa-document / oa-finance / oa-hr-* / oa-knowledge
  │     ├── oa-platform-common
  │     ├── oa-platform-security
  │     └── oa-workflow
  ├── oa-message / oa-meeting / oa-task
  │     ├── oa-platform-common
  │     ├── oa-platform-security
  │     └── oa-workflow
  └── oa-system
        └── oa-platform-common
```

**禁止**：
- 业务模块横向依赖（如 `oa-hr-leave` 不能直接依赖 `oa-finance`）
- 业务模块依赖 oa-platform-web
- 业务模块依赖 oa-system（除 oa-system 提供的字典/配置接口）

**业务模块横向通信**：通过 Spring Event（`ApplicationEventPublisher`）异步解耦，或通过 `oa-system` 提供的统一接口（如审批回调）。

---

## 6. 关键决策（v2 与 v1 的差异）

### 6.1 包名重整
- **v1**: `cn.oa.common.*` `cn.oa.entity.*` `cn.oa.hr.*`（同名前缀冲突）
- **v2**: `cn.oa.platform.*`（平台） / `cn.oa.{module}.*`（业务）
- **冲突解决**: 旧 `cn.oa.hr.Hr*` 同名类全部删除或重命名

### 6.2 表名复数化
- **v1**: `hr_leave_apply` / `hr_leave_balance`（单数混合）
- **v2**: `hr_leaves` / `hr_leave_balances` / `hr_leave_rules`（全复数）

### 6.3 类名前缀
- **v1**: `HrLeaveApply`（混合大小写）
- **v2**: `HrLeave` + 表 `hr_leaves`（Entity 名与表 entity 部分对应）

### 6.4 平台层独立
- **v1**: `oa-common` 包含安全/工具/异常/常量（无平台层概念）
- **v2**: `oa-platform-common` + `oa-platform-security` 明确分层

### 6.5 工作流引擎内置
- **v1**: `oa-workflow` + `oa-workflow-api` + `oa-workflow-callback` + `oa-workflow-dispatcher`（拆分过细）
- **v2**: `oa-workflow`（一个模块，引擎 + 回调 + 分发器内部分包）

### 6.6 HR 模块拆分
- **v1**: `oa-hr` 一个模块 6 个业务域
- **v2**: `oa-hr-leave` `oa-hr-attendance` `oa-hr-employee` `oa-hr-performance` `oa-hr-recruitment` `oa-hr-training`（6 个独立 Maven 模块）

### 6.7 字符集
- **v1**: `utf8mb4` / `utf8mb4_general_ci`（混合）
- **v2**: 全 `utf8mb4` / `utf8mb4_unicode_ci`

### 6.8 API 版本
- **v1**: 多数无版本（部分用 `v1` 路径）
- **v2**: 强制 `/api/v1/...`，v2 升级时切 `/api/v2/...`

### 6.9 错误码
- **v1**: 简单字符串（"操作成功"/"-1"）
- **v2**: 5 位编码（模块 2 位 + 错误 3 位），参考 `03-api-spec.md` §4

### 6.10 数据权限
- **v1**: 4 级（本人/本部门/本公司/全部）
- **v2**: 5 级（本人/本部门/本部门及下级/本公司/全部）

### 6.11 Lombok
- **v1**: 部分模块用 Lombok，部分手写
- **v2**: Lombok 1.18.34 已验证可用（2026-06-05 javap 验证通过）。@Getter/@Setter/@Slf4j/@Builder 等注解可直接使用。旧文档中 "Lombok 禁用" 的规定已撤销。

---

## 7. 不在 v2 范围

v2 暂不实现以下功能（文档留位，Phase 5+ 考虑）：

1. **分布式事务（TCC/SAGA）** — 跨服务调用暂用同步事务
2. **消息队列（RabbitMQ/Kafka）** — 暂用 Redis Pub/Sub
3. **全文检索（Elasticsearch）** — 暂用 MySQL LIKE（性能差时再升级）
4. **多租户（Multi-Tenant）** — 暂支持单租户
5. **国际化（i18n）** — 前端留位，后端 messages.properties 不实现
6. **移动 App** — 仅 uni-app H5
7. **审计日志归档** — `oa_operation_log` 暂只保留 1 年
8. **低代码/可视化审批设计器** — 暂用 JSON 配置

---

## 8. 实施优先级（Phase 3+）

| 优先级 | 模块 | 理由 |
|--------|------|------|
| P0 | oa-platform-common/security/web | 平台层，所有模块依赖 |
| P0 | oa-workflow | 工作流引擎是核心 |
| P0 | oa-hr-leave | HR 请假试点（已在 v1 完整） |
| P1 | oa-system | 字典/配置/审计 |
| P1 | oa-admin | 印章/资产/用品 |
| P1 | oa-hr-employee | 员工主数据 |
| P2 | oa-document | 发文/收文 |
| P2 | oa-finance | 财务 |
| P2 | oa-hr-attendance | 考勤 |
| P2 | oa-knowledge | 知识库 |
| P2 | oa-message | 消息 |
| P2 | oa-meeting | 会议 |
| P2 | oa-task | 任务 |
| P3 | oa-hr-performance | 绩效 |
| P3 | oa-hr-recruitment | 招聘 |
| P3 | oa-hr-training | 培训 |

