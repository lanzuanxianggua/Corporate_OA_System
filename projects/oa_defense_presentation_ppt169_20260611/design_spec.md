# 企业 OA 办公系统项目答辩 - Design Spec

> Human-readable design narrative — rationale, audience, style, color choices, content outline. Read once by downstream roles for context.
>
> Machine-readable execution contract: `spec_lock.md` (color / typography / icon / image short form). Executor re-reads `spec_lock.md` before every SVG page to resist context-compression drift. Keep both in sync; on divergence, `spec_lock.md` wins.

## I. Project Information

| Item | Value |
| ---- | ----- |
| **Project Name** | 企业 OA 办公系统项目答辩 |
| **Canvas Format** | PPT 16:9 (1280×720) |
| **Page Count** | 22 页 |
| **Design Style** | General Consulting + 学术答辩风格 |
| **Target Audience** | 答辩评委、技术专家、项目评审团 |
| **Use Case** | 毕业答辩 / 项目验收答辩 |
| **Created Date** | 2026-06-11 |

---

## II. Canvas Specification

| Property | Value |
| -------- | ----- |
| **Format** | PPT 16:9 |
| **Dimensions** | 1280×720 |
| **viewBox** | `0 0 1280 720` |
| **Margins** | left/right 60px, top/bottom 50px |
| **Content Area** | 1160×620 (safe area) |

---

## III. Visual Theme

### Theme Style

- **Style**: General Consulting + 学术答辩风格
- **Theme**: Light theme
- **Tone**: 专业、技术、严谨、现代

### Color Scheme

| Role | HEX | Purpose |
| ---- | --- | ------- |
| **Background** | `#FFFFFF` | 页面背景(白色) |
| **Secondary bg** | `#F5F7FA` | 卡片背景、区块背景 |
| **Primary** | `#1565C0` | 标题装饰、关键模块、图标(技术蓝) |
| **Accent** | `#FF6B35` | 数据亮点、创新点标注(活力橙) |
| **Secondary accent** | `#4CAF50` | 完成状态、正向指标(绿色) |
| **Body text** | `#1D1D1F` | 正文文字(深灰) |
| **Secondary text** | `#6C757D` | 注释、说明 |
| **Tertiary text** | `#ADB5BD` | 页脚、补充信息 |
| **Border/divider** | `#E9ECEF` | 卡片边框、分隔线 |
| **Success** | `#2E7D32` | 完成状态 |
| **Warning** | `#D32F2F` | 问题标注 |

### Gradient Scheme

```xml
<!-- Title gradient -->
<linearGradient id="titleGradient" x1="0%" y1="0%" x2="100%" y2="0%">
  <stop offset="0%" stop-color="#1565C0"/>
  <stop offset="100%" stop-color="#4CAF50"/>
</linearGradient>

<!-- Background decorative gradient -->
<radialGradient id="bgDecor" cx="80%" cy="20%" r="50%">
  <stop offset="0%" stop-color="#1565C0" stop-opacity="0.08"/>
  <stop offset="100%" stop-color="#1565C0" stop-opacity="0"/>
</radialGradient>
```

---

## IV. Typography System

### Font Plan

**Typography direction**: 现代 CJK 无衬线字体,专业技术风格

**Role breakdown**:

| Role | CJK Font | Latin Font | CSS Generic | Weight | Use |
| ---- | -------- | ---------- | ----------- | ------ | --- |
| Title | Microsoft YaHei | Segoe UI | sans-serif | 700 | 页面标题、章节标题 |
| Body | Microsoft YaHei | Segoe UI | sans-serif | 400 | 正文内容、列表 |
| Emphasis | Microsoft YaHei | Segoe UI | sans-serif | 600 | 强调文本、关键词 |
| Code | N/A | Consolas | monospace | 400 | 代码片段、技术术语 |

**Executor-facing stacks** (write these into SVG `font-family` attributes):

- **Title**: `"Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif`
- **Body**: `"Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif`
- **Emphasis**: `"Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif`
- **Code**: `Consolas, "Courier New", monospace`

### Font Size Hierarchy

**Body baseline**: 18px (dense academic content)

| Level | Size (px) | Ratio | Use |
| ----- | --------- | ----- | --- |
| Cover title | 72 | 4x | 封面主标题 |
| Section title | 48 | 2.67x | 章节开场标题 |
| Page title | 36 | 2x | 页面标题 |
| Subtitle | 27 | 1.5x | 副标题、说明 |
| **Body** | **18** | **1x** | 正文、列表 |
| Annotation | 15 | 0.83x | 注释、标注 |
| Footer | 12 | 0.67x | 页码、页脚 |

**Formula rendering policy**: `text-only` (无复杂公式需求)

---

## V. Layout Principles

### Page Structure

| Zone | Height (px) | Purpose |
| ---- | ----------- | ------- |
| Header | 100 | 页面标题、装饰线 |
| Content | 520 | 主要内容区域 |
| Footer | 40 | 页码、备注 |

### Layout Pattern Library

本项目采用以下布局模式组合：

1. **单列居中** - 封面、章节页、关键结论
2. **非对称分栏 (3:7)** - 架构图 + 说明文字
3. **三列等分** - 技术栈、模块展示
4. **四象限** - SWOT、对比分析
5. **上下分割** - 架构图 + 技术说明
6. **卡片网格** - 8大业务域展示
7. **时间轴/流程** - 工作流引擎展示

### Spacing Specification

- **Section gap**: 30px (区块间距)
- **Card gap**: 20px (卡片间距)
- **Text line-height**: 1.6 (正文行高)
- **Title line-height**: 1.3 (标题行高)
- **Border radius**: 8px (圆角)

---

## VI. Icon Usage Spec

### Icon Source

- **Primary library**: chunk-filled (几何感强,技术风格)
- **Icon count**: ~15 个核心图标
- **Placeholder syntax**: `<use href="#icon-{name}" />`

### Icon Inventory

基于内容大纲,本项目使用的图标包括:

- chunk-filled/home (系统首页)
- chunk-filled/server (后端架构)
- chunk-filled/code (前端技术)
- chunk-filled/database (数据库)
- chunk-filled/shield (安全权限)
- chunk-filled/users (用户管理)
- chunk-filled/document (文档管理)
- chunk-filled/currency (财务模块)
- chunk-filled/briefcase (行政管理)
- chunk-filled/calendar (考勤/请假)
- chunk-filled/chart-bar (数据报表)
- chunk-filled/workflow (工作流)
- chunk-filled/settings (系统配置)
- chunk-filled/check-circle (完成状态)
- chunk-filled/arrow-right (流程指向)

---

## VII. Visualization Reference List

Catalog read: 71 templates

| Page | Template | Path | Summary-quote (verbatim) | Usage |
| ---- | -------- | ---- | ------------------------ | ----- |
| P04 | vertical_list | templates/charts/vertical_list.svg | "Pick for: numbered process steps, tiered capability levels, sequential stages." | 技术栈展示(三层架构) |
| P06 | card_grid_4 | templates/charts/card_grid_4.svg | "Pick for: four equal-weight categories, benefit clusters, feature quadrants." | 8大业务域概览(2x4网格) |
| P09 | process_flow | templates/charts/process_flow.svg | "Pick for: linear workflows, stage-gate processes, approval chains." | 工作流引擎流转流程 |
| P14 | bar_chart | templates/charts/bar_chart.svg | "Pick for: category comparisons, ranking data, performance metrics across groups." | 模块功能数量对比 |

Runners-up considered:
- horizontal_timeline | rejected for P09: 工作流需要展示节点状态和条件分支,timeline 无法表达审批逻辑
- sankey_chart | rejected for P09: 工作流非数据流量展示,sankey 过于复杂
- table_highlight | rejected for P14: 需要视觉对比效果,表格不如柱状图直观

---

## VIII. Image Resource List

本项目采用 **D) 网络图片 + E) 占位符** 策略。

| Filename | Dimensions | Ratio | Layout suggestion | Layout pattern | Purpose | Type | Acquire Via | Status | Reference |
| -------- | ---------- | ----- | ----------------- | -------------- | ------- | ---- | ----------- | ------ | --------- |
| cover_bg.png | 1280x720 | 1.78 | Full-bleed background | #38 image-as-canvas + #30 gradient overlay | 封面背景 | Background | placeholder | Placeholder | 科技感背景,深蓝渐变,电路板纹理 |
| architecture_diagram.png | 1200x600 | 2.0 | Top-bottom split | #7 architecture diagram + #66 shadow depth | 技术架构图 | Diagram | placeholder | Placeholder | Spring Boot + Vue 3 架构示意图 |
| workflow_engine.png | 800x600 | 1.33 | Side-by-side right | #2 right-third + #21 rounded rectangle | 工作流引擎截图 | Screenshot | placeholder | Placeholder | 流程设计器界面截图 |
| dashboard_screenshot.png | 1000x600 | 1.67 | Top-bottom split | #38 image-as-canvas + #33 text annotation | 系统工作台 | Screenshot | placeholder | Placeholder | OA系统主界面工作台截图 |

**Image-as-canvas coverage**: P04 技术架构页使用 #38 + #30 模式,满足覆盖要求。其他页面为卡片式内容展示,无需 image-as-canvas。

---

## IX. Content Outline

### 章节 1: 开场 (P01-P02)

#### P01 - 封面
- **Layout**: 单列居中 + 全出血背景
- **Title**: 企业 OA 办公自动化系统
- **Content**: 
  - 副标题: 基于 Spring Boot + Vue 3 的企业级办公管理平台
  - 答辩人: [姓名]
  - 指导老师: [教师姓名]
  - 答辩日期: 2026年6月
- **Visual**: 深蓝渐变科技背景,电路板纹理

#### P02 - 项目概述
- **Layout**: 左右分栏 (4:6)
- **Title**: 项目概述
- **Content**:
  - **左侧**: 系统定位
    - 面向 200-5000 人企业的办公自动化系统
    - 覆盖 8 大业务域:行政、人事、财务、审批、协同、资产、知识、报表
    - 核心价值:减少线下审批和人工统计成本,实现办公流程线上化
  - **右侧**: 核心目标
    - ✓ 业务模块清晰,职责分明
    - ✓ 审批流程可配置,灵活适配企业需求
    - ✓ 权限可控、数据可追溯
    - ✓ 接口统一、前端页面可快速扩展

---

### 章节 2: 技术架构 (P03-P05)

#### P03 - 技术选型
- **Layout**: 三列等分
- **Title**: 技术选型
- **Content**:
  - **前端技术栈**:
    - Vue 3 (Composition API)
    - TypeScript 4.9+
    - Element Plus (UI组件库)
    - Vite 5 (构建工具)
    - Pinia (状态管理)
  - **后端技术栈**:
    - Java 17
    - Spring Boot 3.4
    - MyBatis-Plus 3.5
    - MySQL 8.0
    - Redis 7
  - **基础设施**:
    - Docker Compose (本地开发)
    - GitHub Actions (CI/CD)
    - Flyway (数据库版本管理)
    - MailHog (邮件测试)

#### P04 - 系统架构
- **Layout**: 上下分割
- **Title**: 系统架构设计
- **Visualization**: vertical_list
- **Content**:
  - **三层架构展示**:
    - 表现层: Vue 3 前端 + Element Plus UI
    - 业务层: Spring Boot 后端 + 自研工作流引擎
    - 数据层: MySQL + Redis + Flyway
  - **关键特性**:
    - 前后端分离,RESTful API
    - JWT 认证 + 自研权限拦截器
    - 统一响应封装 R<T>
    - 分布式 TraceID 日志追踪

#### P05 - Maven 模块结构
- **Layout**: 卡片网格
- **Title**: Maven 多模块设计
- **Content**:
  - **平台层模块**:
    - oa-platform-common (统一响应/异常/TraceContext)
    - oa-platform-security (JWT认证/权限拦截)
    - oa-platform-web (应用启动/配置/Flyway脚本)
  - **引擎层**:
    - oa-workflow (自研工作流引擎)
  - **业务层** (14个模块):
    - oa-system (用户/角色/部门)
    - oa-admin (印章/资产/办公用品)
    - oa-document (发文/收文/签报)
    - oa-finance (预算/报销/借款/合同)
    - oa-hr-* (请假/员工/考勤/绩效等)
  - **设计原则**: 业务模块横向隔离,跨模块通信走 Spring Event

---

### 章节 3: 核心功能 (P06-P08)

#### P06 - 8大业务域概览
- **Layout**: 卡片网格 (2行4列)
- **Title**: 8大业务域全景
- **Visualization**: card_grid_4 (改为 2x4)
- **Content**: 每个卡片展示一个业务域
  1. **系统管理**: 用户/角色/部门/权限/字典/岗位管理
  2. **行政管理**: 印章使用/资产管理/办公用品申领
  3. **文档管理**: 发文/收文/签报/文档归档
  4. **财务管理**: 预算编制/报销审批/借款管理/合同付款
  5. **人事管理**: 员工档案/入职离职/薪资社保
  6. **考勤请假**: 打卡签到/请假审批/加班调休/额度管理
  7. **知识协同**: 知识库/会议管理/任务分配
  8. **消息报表**: 站内消息/邮件通知/数据报表

#### P07 - 典型业务流程示例
- **Layout**: 左右分栏 (5:5)
- **Title**: 典型业务流程:请假审批
- **Content**:
  - **左侧**: 流程步骤
    1. 员工提交请假申请
    2. 系统创建工作流实例
    3. 工作流引擎根据配置路由审批人
    4. 审批通过后扣减年假额度
    5. 发送消息通知相关人员
  - **右侧**: 技术实现要点
    - 使用 Spring Event 解耦工作流与业务回调
    - 审批路由支持条件表达式(天数/金额/层级)
    - 额度扣减使用悲观锁防并发
    - 消息通知异步发送,支持站内信/邮件/钉钉

#### P08 - 核心功能亮点
- **Layout**: 左右分栏 (4:6)
- **Title**: 核心功能亮点
- **Content**:
  - **统一审批抽象**: 所有审批业务(请假/报销/借款/合同...)共享同一套工作流引擎
  - **灵活路由策略**: 支持固定审批人/角色/部门主管/条件路由
  - **实时消息推送**: WebSocket + 轮询降级,保证消息必达
  - **数据权限隔离**: 5级数据权限(本人/本部门/本部门及下级/全公司/全部)
  - **全链路追踪**: 分布式 TraceID,快速定位问题

---

### 章节 4: 自研工作流引擎 (P09-P11)

#### P09 - 工作流引擎架构
- **Layout**: 上下分割
- **Title**: 自研工作流引擎设计
- **Visualization**: process_flow
- **Content**:
  - **核心表设计**:
    - wf_process_definitions (流程定义)
    - wf_process_instances (流程实例)
    - wf_tasks (审批任务)
    - wf_task_delegations (任务委派)
  - **流转逻辑**:
    - 发起 → 创建实例 → 生成任务 → 审批处理 → 路由下一节点 → 完成/驳回
  - **支持特性**:
    - ✓ 条件路由(金额/天数/层级)
    - ✓ 会签/或签
    - ✓ 任务委派/转交
    - ✓ 超时提醒
    - ✓ 流程撤回

#### P10 - 工作流引擎创新点
- **Layout**: 三列等分
- **Title**: 工作流引擎技术创新
- **Content**:
  - **图结构定义**:
    - 支持 schemaVersion 2 图格式
    - nodes + edges 描述流程拓扑
    - 4维分层路由(金额/天数/工时/层级)
  - **业务回调解耦**:
    - 使用 Spring Event 发布流程事件
    - 业务模块监听事件更新状态
    - 工作流引擎零依赖业务代码
  - **动态路由算法**:
    - role_chain 角色链路由
    - initiator_level_match 发起人层级匹配
    - escalateTo 超时升级路由

#### P11 - 为什么自研而非开源
- **Layout**: 左右分栏 (5:5)
- **Title**: 为什么选择自研工作流引擎?
- **Content**:
  - **左侧**: 开源方案的问题
    - Activiti/Flowable: 过于重量级,BPMN 2.0 学习成本高
    - Camunda: 企业版收费,社区版功能受限
    - 通用引擎: 需要大量适配代码,业务耦合严重
  - **右侧**: 自研方案的优势
    - ✓ 轻量级设计,4张核心表即可运行
    - ✓ 完全掌控路由逻辑,易于扩展
    - ✓ 与业务解耦,使用 Spring Event 通信
    - ✓ 学习成本低,团队可快速上手
    - ✓ 无第三方依赖,部署简单

---

### 章节 5: 数据库设计 (P12-P13)

#### P12 - 数据库架构
- **Layout**: 左右分栏 (4:6)
- **Title**: 数据库设计与版本管理
- **Content**:
  - **左侧**: Flyway 版本管理
    - V100: 平台基础表(用户/角色/部门/权限)
    - V200: 工作流引擎核心表
    - V900: 基础种子数据
    - V910+: 平台升级与补丁
    - V930+: 各业务模块增量
  - **右侧**: 设计规范
    - 表名: {module}_{entity_plural} 蛇形复数
    - 主键: Snowflake ID(ASSIGN_ID)
    - 审计字段: create_by/create_time/update_by/update_time
    - 逻辑删除: del_flag (String "0"/"1")
    - 乐观锁: version (Integer)

#### P13 - 核心表结构
- **Layout**: 卡片网格
- **Title**: 核心业务表设计
- **Content**:
  - **平台层**: sys_employees, sys_departments, sys_roles, sys_permissions, sys_role_permission
  - **工作流层**: wf_process_definitions, wf_process_instances, wf_tasks, wf_task_delegations
  - **人事层**: hr_leaves, hr_leave_balances, hr_employee_profiles, hr_attendances
  - **财务层**: fin_budgets, fin_expenses, fin_loans, fin_contracts, fin_payments
  - **行政层**: admin_seals, admin_assets, admin_supplies
  - **文档层**: doc_outgoing, doc_incoming, doc_reports
  - **统计**: 总计 60+ 张业务表

---

### 章节 6: 安全与权限 (P14-P15)

#### P14 - 安全架构
- **Layout**: 上下分割
- **Title**: 安全架构设计
- **Content**:
  - **自研安全方案**(无 Spring Security/Shiro):
    - JWT (HS256) Token 认证
    - Servlet Filter + Spring Interceptor 双层拦截
    - @RequirePermission 注解式权限控制
    - UserContext ThreadLocal 存储当前用户
  - **为什么自研**:
    - 避免 Spring Security 的复杂配置和学习成本
    - 避免与其他拦截器的 DI 循环依赖
    - 完全掌控认证流程,易于调试和扩展

#### P15 - 权限体系
- **Layout**: 左右分栏 (5:5)
- **Title**: 权限体系设计
- **Content**:
  - **左侧**: 功能权限
    - 权限码格式: {module}:{resource}:{action}
    - 示例: hr-leave:leave:create
    - 角色关联权限,用户关联角色
    - 支持权限继承和组合
  - **右侧**: 数据权限(5级)
    - SELF: 仅本人数据
    - DEPT: 本部门数据
    - DEPT_DOWN: 本部门及下级部门
    - COMPANY: 全公司数据
    - ALL: 全部数据
  - **安全加固**:
    - SQL 防注入(Druid内置)
    - XSS 过滤
    - CSRF Token
    - 慢 SQL 审计

---

### 章节 7: 项目亮点 (P16-P18)

#### P16 - 技术亮点
- **Layout**: 卡片网格 (2x2)
- **Title**: 项目技术亮点
- **Visualization**: card_grid_4
- **Content**:
  1. **自研工作流引擎**: 轻量级设计,4维分层路由,业务解耦
  2. **统一审批抽象**: 所有审批业务共享同一套流程引擎
  3. **自研安全框架**: JWT + 自研拦截器,避免 Spring Security 复杂性
  4. **Maven 多模块架构**: 18个模块,职责清晰,横向隔离

#### P17 - 工程实践亮点
- **Layout**: 左右分栏 (5:5)
- **Title**: 工程实践亮点
- **Content**:
  - **左侧**: 开发规范
    - 统一响应封装 R<T>
    - 分布式 TraceID 日志追踪
    - Snowflake 主键 + 审计字段自动填充
    - 逻辑删除 + 乐观锁
    - MapStruct 对象映射
  - **右侧**: 质量保障
    - Flyway 数据库版本管理
    - GitHub Actions CI/CD 自动化
    - Spotless 代码格式化
    - Checkstyle 静态检查
    - 单元测试 + 集成测试

#### P18 - 系统规模与完成度
- **Layout**: 三列等分
- **Title**: 系统规模与完成度
- **Visualization**: bar_chart
- **Content**:
  - **代码规模**:
    - 后端: 8000+ 行 Java 代码
    - 前端: 6000+ 行 TypeScript 代码
    - 数据库: 60+ 张业务表
    - 接口: 100+ 个 REST API
  - **功能完成度**:
    - ✓ 8大业务域核心功能完成
    - ✓ 工作流引擎核心功能完整
    - ✓ 请假→审批→扣减额度全链路打通
    - ✓ 前后端分离,API 完整对接
  - **测试覆盖**:
    - 单元测试覆盖核心业务逻辑
    - CI/CD 流水线自动化测试
    - 本地开发环境完整可运行

---

### 章节 8: 系统演示 (P19-P20)

#### P19 - 系统工作台
- **Layout**: 图文上下分割
- **Title**: 系统演示:工作台首页
- **Content**:
  - **功能展示**:
    - 待办任务中心
    - 快捷入口(发起请假/报销/借款)
    - 最近访问
    - 消息通知
    - 数据统计看板
  - **技术实现**: Vue 3 + Element Plus + Echarts

#### P20 - 审批流程演示
- **Layout**: 图文上下分割
- **Title**: 系统演示:审批流程
- **Content**:
  - **流程截图**: 请假申请 → 审批中 → 已通过
  - **关键功能**:
    - 流程进度可视化
    - 审批意见填写
    - 任务委派/转交
    - 流程撤回
    - 消息实时推送

---

### 章节 9: 总结与展望 (P21-P22)

#### P21 - 项目总结
- **Layout**: 左右分栏 (5:5)
- **Title**: 项目总结
- **Content**:
  - **左侧**: 完成成果
    - ✓ 构建了完整的企业 OA 办公自动化系统
    - ✓ 覆盖 8 大业务域,60+ 张业务表
    - ✓ 自研工作流引擎,支持灵活路由
    - ✓ 自研安全框架,完整权限体系
    - ✓ 前后端分离,100+ REST API
  - **右侧**: 技术收获
    - 深入理解企业级应用架构设计
    - 掌握 Spring Boot + Vue 3 全栈开发
    - 实践 Maven 多模块项目管理
    - 学习工作流引擎设计思想
    - 积累 CI/CD 自动化经验

#### P22 - 未来展望
- **Layout**: 单列居中
- **Title**: 未来展望
- **Content**:
  - **功能扩展方向**:
    - 移动端 H5/App 支持
    - BI 数据分析看板
    - 流程设计器可视化配置
    - 多租户 SaaS 模式
  - **技术优化方向**:
    - 引入 Redis 缓存提升性能
    - ElasticSearch 全文检索
    - 消息队列异步解耦
    - 微服务化拆分(按需)
  - **结束语**: 感谢各位评委老师的聆听与指导!

---

## X. Speaker Notes Requirements

- **File naming**: 匹配 SVG 文件名 (`01_cover.md`, `02_overview.md`, ...)
- **Presentation duration**: 15-20 分钟
- **Notes style**: 正式学术风格,结论先行
- **Presentation purpose**: 展示技术实力,说服评委认可项目价值

### 演讲稿结构要求

每页演讲稿包含:
1. **开场语**(仅首页): 简短问候和主题引入
2. **页面核心观点**: 一句话总结本页要传达的信息
3. **详细说明**: 2-3 段展开说明,支撑核心观点
4. **过渡语**(非末页): 自然衔接到下一页

### 重点页面演讲要点

- P09-P11 工作流引擎: 重点强调自研的必要性和技术创新
- P14-P15 安全架构: 说明为什么不用 Spring Security
- P16-P18 项目亮点: 突出技术难点和工程实践
- P21 项目总结: 呼应开场,强调完成度和收获

---

## XI. Technical Constraints Reminder

### SVG Generation Rules

1. **Mandatory attributes**:
   - Root `<svg>` must have `viewBox="0 0 1280 720"`, `width="1280"`, `height="720"`
   - Text elements must specify `font-family`, `font-size`, `fill`
   - All colors from `spec_lock.md` color section

2. **Text rendering**:
   - Use `<text>` for single-line text
   - Use `<text>` with multiple `<tspan dy="...">` for multi-line
   - Line-height = font-size × 1.6 (body) or × 1.3 (title)

3. **Icon usage**:
   - Define icons in `<defs>` section
   - Reference via `<use href="#icon-{name}" />`
   - Icon size typically 32-48px

4. **Layout precision**:
   - All coordinates in integer pixels
   - Content safe area: x=60, y=50, width=1160, height=620

### PPT Compatibility Rules

1. **Font fallback**: Every font stack must end with pre-installed font
2. **No CSS3 features**: No CSS animations, transforms, filters in SVG
3. **Color format**: HEX only, no `rgb()` or `rgba()`
4. **Image embedding**: External images via `<image href="..." />`
5. **Group organization**: Top-level `<g id="...">` for animation grouping

