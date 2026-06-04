# 05-modules - 业务模块概要规格

> 日期: 2026-06-04
> 状态: **Phase 1 概要阶段（每份详细 spec 留到 Phase 2 启动后用 TEMPLATE.md 逐份生成）**
> 模板: `TEMPLATE.md`
> 详细 spec: 见各模块文件名（如 `05-platform-common.md` 已完成）

---

## 1. 模块清单（13 个业务模块 + 1 个工作流）

| # | 模块 | artifactId | 优先级 | 状态 |
|---|------|-----------|--------|------|
| 1 | 平台通用 | oa-platform-common | P0 | ✓ 详细 spec 已完成（05-platform-common.md） |
| 2 | 平台安全 | oa-platform-security | P0 | 概要（见下） |
| 3 | 平台启动 | oa-platform-web | P0 | 概要（见下） |
| 4 | 工作流引擎 | oa-workflow | P0 | 概要（见下） |
| 5 | 系统 | oa-system | P1 | 概要（见下） |
| 6 | 行政 | oa-admin | P1 | 概要（见下） |
| 7 | 文档 | oa-document | P2 | 概要（见下） |
| 8 | 财务 | oa-finance | P2 | 概要（见下） |
| 9 | HR 请假 | oa-hr-leave | P0 | 概要（见下） |
| 10 | HR 考勤 | oa-hr-attendance | P2 | 概要（见下） |
| 11 | HR 员工 | oa-hr-employee | P1 | 概要（见下） |
| 12 | HR 绩效 | oa-hr-performance | P3 | 概要（见下） |
| 13 | HR 招聘 | oa-hr-recruitment | P3 | 概要（见下） |
| 14 | HR 培训 | oa-hr-training | P3 | 概要（见下） |
| 15 | 知识库 | oa-knowledge | P2 | 概要（见下） |
| 16 | 消息 | oa-message | P2 | 概要（见下） |
| 17 | 会议 | oa-meeting | P2 | 概要（见下） |
| 18 | 任务 | oa-task | P2 | 概要（见下） |

---

## 2. 各模块概要规格

### 2.1 oa-platform-security（平台安全）

**职责**：JWT 认证、RBAC 权限、数据权限、接口签名、限流、审计

**核心类**：
- `JwtAuthenticationFilter` JWT 校验
- `PermissionInterceptor` 权限拦截（`@RequirePermission`）
- `DataScopeInterceptor` 数据范围拦截
- `AuthService` 认证服务
- `SignService` 接口签名校验
- `UserContextHolder` 当前用户 ThreadLocal
- `PermissionEvaluator` Spring Security 自定义

**接口**：仅内部，不暴露 REST
- `/api/v1/platform/auth/login` POST
- `/api/v1/platform/auth/logout` POST
- `/api/v1/platform/auth/refresh` POST
- `/api/v1/platform/auth/me` GET

**详细 spec**：Phase 2 启动后用 TEMPLATE 生成

---

### 2.2 oa-platform-web（启动模块）

**职责**：Spring Boot 启动、配置加载、组件扫描、Tomcat 部署

**核心类**：
- `OaSystemApplication` 启动类
- `application.yml` 主配置
- `application-{profile}.yml` 环境配置

**依赖**：所有业务模块（通过 Maven `<dependency>`）

---

### 2.3 oa-workflow（工作流引擎）

**职责**：自研工作流引擎核心（定义/实例/任务/委派/审批人解析/回调）

**核心类**：
- `WfDefinitionService` 流程定义 CRUD
- `WfInstanceService` 流程实例生命周期
- `WfTaskService` 任务分配/处理
- `WfAssigneeResolver` 审批人解析器链
- `WfCallbackDispatcher` 回调分发
- `WfEngine` 引擎主入口

**数据模型**：`wf_*` 8 张表（见 `02-database.md` §3.2）

**接口**：
- `/api/v1/workflow/definitions` 流程定义 CRUD
- `/api/v1/workflow/instances` 流程实例
- `/api/v1/workflow/tasks` 审批任务
- `/api/v1/workflow/delegations` 委托

**详细 spec**：Phase 2 启动后用 TEMPLATE 生成

---

### 2.4 oa-system（系统）

**职责**：字典/参数配置/审计/附件/导入导出

**核心表**：
- `sys_dict_types` / `sys_dict_data` 字典
- `sys_configs` 参数配置
- `oa_operation_logs` 操作日志
- `sys_attachments` 附件
- `oa_import_tasks` 导入任务
- `oa_export_tasks` 导出任务

**接口**：~15 个（字典/配置/审计/附件/导入导出）

---

### 2.5 oa-admin（行政）

**职责**：印章/资产/办公用品

**核心表**：
- `adm_seals` 印章
- `adm_seal_usages` 用印记录
- `adm_assets` 资产
- `adm_asset_borrows` 资产借用
- `adm_supplies` 办公用品

**接口**：~20 个

**关键场景**：
- 用印申请 → 审批 → 用印记录
- 资产领用 → 审批 → 资产状态变更
- 用品申领 → 审批 → 库存扣减

---

### 2.6 oa-document（文档）

**职责**：发文/收文/签报/档案

**核心表**：
- `doc_dispatches` 发文
- `doc_receives` 收文
- `doc_sign_reports` 签报
- `doc_sign_report_items` 签报明细
- `doc_archives` 档案
- `doc_archive_files` 档案附件

**接口**：~30 个

**关键场景**：
- 发文拟稿 → 多级审核 → 发文编号 → 归档
- 收文登记 → 部门分发 → 办理反馈
- 签报申请 → 多级审批

---

### 2.7 oa-finance（财务）

**职责**：预算/报销/借款/还款

**核心表**：
- `fin_budgets` / `fin_budget_items` 预算
- `fin_expenses` / `fin_expense_items` 报销
- `fin_loans` 借款
- `fin_loan_repayments` 还款
- `fin_payments` / `fin_receipts` 收付款

**接口**：~30 个

**关键场景**：
- 月度预算编制 → 审批 → 预算控制
- 差旅报销 → 审批 → 财务付款
- 借款申请 → 审批 → 借款记录
- 还款登记 → 冲销借款

---

### 2.8 oa-hr-leave（HR 请假）**P0 核心**

**职责**：请假/假期余额/规则

**核心表**：
- `hr_leaves` 请假申请
- `hr_leave_balances` 假期余额
- `hr_leave_rules` 假期规则
- `hr_leave_adjustments` 余额调整
- `hr_leave_holidays` 节假日

**接口**：~11 个（详见 TEMPLATE 模板）
- 创建/列表/详情/撤回/重新提交
- 余额查询/初始化/调整
- 规则查询/更新

**关键场景**：
- 员工提交请假 → 余额检查 → 规则校验 → 工作流启动 → 经理审批 → 余额冻结 → 通过 → 余额扣减

**详细 spec**：Phase 3 启动时按 TEMPLATE 生成

---

### 2.9 oa-hr-attendance（HR 考勤）

**职责**：打卡/排班/统计

**核心表**：
- `hr_attendance_records` 打卡记录
- `hr_attendance_schedules` 排班
- `hr_attendance_statistics` 月度统计
- `hr_attendance_exceptions` 异常（迟到/早退/缺勤）

**接口**：~15 个

**关键场景**：
- 每日打卡 → 异常检测 → 月度统计 → 联动薪资

---

### 2.10 oa-hr-employee（HR 员工）

**职责**：员工档案/合同/异动

**核心表**：
- `hr_employee_profiles` 员工档案
- `hr_employee_contracts` 合同
- `hr_employee_changes` 异动记录

**接口**：~15 个

**关键场景**：
- 入职登记 → 档案建立 → 合同签订
- 部门调动 → 档案更新
- 离职登记 → 档案归档

---

### 2.11 oa-hr-performance（HR 绩效）

**职责**：绩效目标/评估/结果

**核心表**：
- `hr_performance_goals` 绩效目标
- `hr_performance_evaluations` 评估
- `hr_performance_results` 结果

**接口**：~10 个

---

### 2.12 oa-hr-recruitment（HR 招聘）

**职责**：招聘需求/简历/面试/Offer

**核心表**：
- `hr_recruitment_demands` 招聘需求
- `hr_resumes` 简历
- `hr_interviews` 面试记录
- `hr_offers` Offer

**接口**：~12 个

---

### 2.13 oa-hr-training（HR 培训）

**职责**：培训计划/记录/反馈

**核心表**：
- `hr_training_plans` 培训计划
- `hr_training_records` 培训记录
- `hr_training_feedback` 反馈

**接口**：~10 个

---

### 2.14 oa-knowledge（知识库）

**职责**：知识条目/分类/检索/版本

**核心表**：
- `km_entries` 知识条目
- `km_versions` 版本历史
- `km_categories` 分类

**接口**：~10 个

**关键场景**：
- 知识录入 → 分类 → 全文检索
- 版本管理（每次更新产生新版本）

---

### 2.15 oa-message（消息）

**职责**：站内消息/系统通知/邮件/短信

**核心表**：
- `msg_notifications` 站内消息
- `msg_notification_recipients` 接收人
- `msg_email_logs` 邮件日志
- `msg_sms_logs` 短信日志

**接口**：~10 个

**关键场景**：
- 审批提醒推送（系统通知 + 邮件）
- 我的消息箱
- 实时推送（WebSocket）

---

### 2.16 oa-meeting（会议）

**职责**：会议室/会议/决议

**核心表**：
- `mt_rooms` 会议室
- `mt_bookings` 预约
- `mt_meetings` 会议
- `mt_resolutions` 决议

**接口**：~12 个

---

### 2.17 oa-task（任务）

**职责**：待办/项目/工时

**核心表**：
- `task_projects` 项目
- `task_items` 任务
- `task_hours` 工时
- `task_comments` 评论

**接口**：~15 个

---

## 3. 依赖关系图

```
oa-platform-web
  ├─→ oa-platform-security → oa-platform-common
  ├─→ oa-workflow → oa-platform-common, oa-platform-security
  ├─→ oa-system → oa-platform-common, oa-platform-security
  └─→ [所有业务模块] → oa-platform-common, oa-platform-security, oa-workflow, oa-system(部分)
```

**业务模块横向无依赖**，跨模块通信通过：
1. `oa-workflow`（业务触发流程）
2. `oa-message`（发送通知）
3. Spring `ApplicationEventPublisher`（业务事件）
4. `oa-system` 提供的字典/配置接口

---

## 4. Phase 2 启动后立即生成的详细 spec 清单

按优先级，Phase 2/3/4 启动后逐份生成（用 TEMPLATE.md）：

| Phase | 模块 | spec 文件 |
|-------|------|----------|
| Phase 2 | oa-platform-security | `05-platform-security.md` |
| Phase 2 | oa-workflow | `05-workflow.md` |
| Phase 2 | oa-system | `05-system.md` |
| Phase 3 | oa-hr-leave | `05-hr-leave.md` |
| Phase 3 | oa-hr-employee | `05-hr-employee.md` |
| Phase 3 | oa-admin | `05-admin.md` |
| Phase 4 | oa-document | `05-document.md` |
| Phase 4 | oa-finance | `05-finance.md` |
| Phase 4 | oa-hr-attendance | `05-hr-attendance.md` |
| Phase 4 | oa-knowledge | `05-knowledge.md` |
| Phase 4 | oa-message | `05-message.md` |
| Phase 4 | oa-meeting | `05-meeting.md` |
| Phase 4 | oa-task | `05-task.md` |
| Phase 5 | oa-hr-performance | `05-hr-performance.md` |
| Phase 5 | oa-hr-recruitment | `05-hr-recruitment.md` |
| Phase 5 | oa-hr-training | `05-hr-training.md` |

**共 16 份详细 spec**（含已完成的 oa-platform-common）
