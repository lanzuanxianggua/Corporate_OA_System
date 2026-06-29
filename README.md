# Corporate OA System

企业办公自动化系统（Corporate Office Automation），面向 **200–5000 人**规模企业，提供 PC 管理后台、移动端（H5 / 微信小程序）与统一 REST API，覆盖审批工作流、人事、考勤、财务、行政、文档、消息、报表等 **12+ 业务域**。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 功能概览

| 模块 | 能力 |
|------|------|
| 系统管理 | 用户、部门、角色、菜单、字典、参数、岗位、在线用户 |
| 工作流 | 图结构流程定义、分级路由、动态审批人、加签/委派、业务回调 |
| 审批业务 | 请假、出差、外出、加班、报销、借款、采购等 |
| 人事 / 考勤 | 员工档案、薪资、打卡、考勤记录、考勤组 |
| 财务 / 行政 | 预算、合同、资产、会议、文档 |
| 协同 | 待办、消息、公告、日程、报表、预警规则 |

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Java 17、Spring Boot 3.4.5、MyBatis-Plus 3.5.9、MySQL 8、Redis、JWT、Flyway、Knife4j |
| PC 前端 | Vue 3、TypeScript、Vite 6、Element Plus、Pinia、Tailwind CSS v4、ECharts、Vue Flow |
| 移动端 | uni-app 3.x（H5 + 微信小程序） |

后端采用 **5 模块扁平分层**（`oa-common` → `oa-model` → `oa-mapper` → `oa-service` → `oa-web`），业务代码统一在 `cn.oa` 包下按领域组织。

## 仓库结构

```
Corporate_OA_System/
├── code/
│   ├── backend/          # Maven 多模块后端
│   ├── frontend/         # PC 管理端
│   └── mobile/           # uni-app 移动端
├── docs/                 # 需求、架构、部署、用户手册、SQL 等
├── tests/                # API 脚本与 Playwright UI 测试
├── CLAUDE.md             # 开发约定（与代码冲突时以代码为准）
└── README.md
```

文档索引：[项目需求](docs/项目需求文档.md) · [技术架构](docs/技术架构文档.md) · [部署说明](docs/项目部署文档.md) · [核心实现总结](docs/核心业务代码实现总结.md) · [用户手册](docs/USER_MANUAL.md)

## 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| pnpm | 8+ |
| MySQL | 8.0+（utf8mb4） |
| Redis | 6+ |

## 快速开始

### 1. 克隆

```bash
git clone https://github.com/lanzuanxianggua/Corporate_OA_System.git
cd Corporate_OA_System
```

### 2. 数据库与 Redis

```sql
CREATE DATABASE IF NOT EXISTS oa_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

默认配置见 `code/backend/oa-web/src/main/resources/application.yml`：

- MySQL：`127.0.0.1:3306/oa_system`（用户/密码默认 `oa_v2`，请按环境修改）
- Redis：`127.0.0.1:6379`

> Windows 下建议使用 `127.0.0.1` 而非 `localhost`，避免 IPv6 解析导致连接变慢。

首次启动后端时 Flyway 会执行 `db/migration/` 迁移；也可参考 `docs/oa_system_dump.sql`。

### 3. 后端

```bash
cd code/backend
mvn -pl oa-web -DskipTests spring-boot:run
```

- API：`http://127.0.0.1:8080`
- 文档：`http://127.0.0.1:8080/doc.html`

### 4. PC 前端

```bash
cd code/frontend
pnpm install
pnpm dev
```

开发地址默认：`http://localhost:8848`（Vite 代理 API 到 `8080`）。

### 5. 移动端（可选）

```bash
cd code/mobile
pnpm install
pnpm dev:h5
pnpm dev:mp-weixin   # 微信开发者工具打开 dist/build/mp-weixin
```

## 常用命令

```bash
# 后端编译
cd code/backend && mvn -DskipTests compile

# 修改 Entity/Mapper 后
mvn -pl oa-model,oa-mapper -DskipTests install

# 后端测试
mvn test

# 前端类型检查 / 构建
cd code/frontend && pnpm typecheck && pnpm build
```

## 架构要点

- **认证**：JWT + 拦截器，RBAC（`@RequirePermission` / `@RequireRole`）
- **工作流**：自研图引擎，Spring Event 业务回调
- **持久层**：MyBatis-Plus 注解 SQL，逻辑删除 `delFlag`
- **API**：统一响应 `R<T>`

## 测试

```bash
cd code/backend && mvn test
```

根目录 `tests/` 含 Playwright 与 API 脚本（需先启动前后端）。

## 贡献

提交前建议：`mvn -pl oa-web -am -DskipTests compile` 与 `pnpm typecheck`。

## 许可证

[MIT License](LICENSE)

## 仓库

- GitHub: https://github.com/lanzuanxianggua/Corporate_OA_System
