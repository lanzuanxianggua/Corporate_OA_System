# 企业 OA 办公系统项目说明书

## 1. 项目概述

企业 OA 办公系统面向企业内部办公、人事、审批、协同、资产、财务、报表和系统管理场景。系统通过统一登录、角色权限、流程审批、待办消息和数据看板，将日常办公流程线上化，减少线下审批和人工统计成本。

本项目采用前后端分离架构：

- 前端：Vue 3 + TypeScript + Vite + Element Plus，负责页面展示、交互、路由权限和接口调用。
- 后端：Spring Boot 3 + Java 17 + MyBatis-Plus，负责认证授权、业务规则、流程引擎、数据持久化、导出、通知和接口服务。
- 数据库：MySQL 8，按系统基础、OA 业务、工作流、报表预警、扩展业务分域建模。
- 缓存和在线状态：Redis，保存登录 Token、角色缓存、在线用户和登录限流计数。
- 实时通知：WebSocket，推送审批、待办和消息提醒。

设计目标是：业务模块清晰、审批流程可配置、权限可控、数据可追溯、接口统一、前端页面可快速扩展。

## 2. 项目架构设计

### 2.1 总体架构

```text
浏览器
  |
  | Hash Router / Axios / WebSocket
  v
Vue 3 前端
  |
  | REST API + JWT
  v
Spring Boot 后端
  |
  | Controller -> Service -> Mapper -> Entity
  v
MySQL + Redis + 本地上传目录
```

系统采用前后端分离，是因为 OA 系统页面多、表单多、交互强，前端独立工程更适合维护复杂管理界面；后端独立提供 REST API，便于后续接入移动端、第三方系统或统一网关。

### 2.2 后端 Maven 模块

当前父 POM 实际参与构建的模块如下：

| 模块 | 职责 | 设计原因 |
| --- | --- | --- |
| `oa-common` | 通用返回、异常、JWT、Redis、拦截器、注解、工具类 | 公共能力集中，避免各业务重复实现认证、响应和异常处理 |
| `oa-model` | Entity、DTO、VO | 数据结构集中，Controller、Service、Mapper 共用同一套模型 |
| `oa-mapper` | MyBatis-Plus Mapper、自动填充 | 数据访问层独立，便于替换 SQL 和统一扫描 |
| `oa-service` | 核心业务逻辑、事务、审批、报表统计、通知 | 业务规则不放在 Controller，保证可测试和可复用 |
| `oa-web` | 启动类、Controller、WebSocket、AOP、初始化器 | 对外暴露 HTTP/WebSocket 服务，承担应用装配职责 |

目录中还存在 `oa-hr-*`、`oa-finance`、`oa-document` 等业务命名目录，但父 POM 未纳入当前构建链路。说明书以当前可构建模块为准。

### 2.3 后端分层

后端采用经典三层到四层结构：

- Controller 层：接收请求、解析参数、读取登录用户、调用 Service、返回统一 `R<T>`。
- Service 层：处理业务校验、事务、审批流启动、状态变更、统计聚合。
- Mapper 层：继承 MyBatis-Plus `BaseMapper`，负责数据库访问。
- Model 层：Entity 映射数据库表，DTO 接收入参，VO/ExportVO 输出页面和导出数据。

这样设计的原因是 OA 系统业务多且状态复杂，如果把业务写在 Controller 中会导致接口难维护；将业务集中在 Service 层后，审批、导出、统计、通知等能力可以复用。

### 2.4 前端架构

前端核心结构：

| 目录 | 职责 |
| --- | --- |
| `src/main.ts` | Vue 应用入口，注册 Pinia、Router、Element Plus、主题 |
| `src/router/index.ts` | 静态路由、登录校验、角色访问控制 |
| `src/layout` | 主布局、侧边栏菜单、顶部栏、主题切换、未读消息 |
| `src/store` | Pinia 用户状态、主题状态 |
| `src/api` | 按业务模块封装后端接口 |
| `src/views` | 页面模块 |
| `src/components` | 公共组件，如分页、审批时间线、流程设计器 |
| `src/utils` | 请求封装、格式化、下载、图表主题、WebSocket |

前端没有使用动态后端路由，而是在 `router/index.ts` 和 `layout/menuConfig.ts` 中维护静态路由和菜单。这样实现简单、稳定，适合毕业设计或中小型 OA 项目；代价是新增页面时需要同步维护路由和菜单。

## 3. 技术选型与原因

### 3.1 前端技术

| 技术 | 用途 | 选择原因 |
| --- | --- | --- |
| Vue 3 | 构建单页应用 | Composition API 适合复杂表单和模块化逻辑 |
| TypeScript | 类型约束 | 减少接口字段、状态码、分页参数等低级错误 |
| Vite | 开发和构建 | 启动快、热更新快，适合 Vue 3 工程 |
| Vue Router | 页面路由 | 支持 Hash 模式，部署简单，不依赖服务器 history fallback |
| Pinia | 状态管理 | 轻量，适合保存用户、Token、主题等全局状态 |
| Element Plus | 管理后台 UI | 表格、表单、弹窗、分页、菜单等组件齐全 |
| Tailwind CSS | 工具类样式 | 快速实现页面间距、布局、响应式和局部样式 |
| ECharts | 图表看板 | 支持折线图、柱状图、饼图、漏斗图等报表需求 |
| Axios | HTTP 请求 | 拦截器适合统一 Token、错误、Loading、刷新 Token |
| Playwright | E2E 测试 | 可验证登录页等真实浏览器行为 |

### 3.2 后端技术

| 技术 | 用途 | 选择原因 |
| --- | --- | --- |
| Java 17 | 后端语言 | LTS 版本，适合企业项目 |
| Spring Boot 3.4 | 应用框架 | 自动配置完善，适合快速构建 REST 服务 |
| Spring MVC | Web 接口 | Controller 注解清晰，便于 REST API 开发 |
| MyBatis-Plus | ORM / CRUD | 减少重复 Mapper 和分页代码，同时保留 SQL 可控性 |
| MySQL 8 | 关系型数据库 | OA 数据关系强，适合事务、报表和状态查询 |
| Redis | 缓存和状态 | 用于 Token、角色缓存、在线用户和登录限流 |
| JWT | 登录令牌 | 前后端分离下便于无状态传递身份 |
| Knife4j / Springdoc | API 文档 | 便于调试和接口说明 |
| EasyExcel | Excel 导出 | 适合考勤、审批、财务等列表导出 |
| WebSocket | 实时通知 | 审批和消息需要及时提醒 |
| AOP | 操作日志 | 对 Controller 操作统一记录，避免业务代码重复 |
| Bean Validation | 参数校验 | 对 DTO 入参做统一约束 |
| JUnit / Spring Boot Test | 测试 | 覆盖 Controller 和 Service 关键逻辑 |

### 3.3 为什么使用 MyBatis-Plus

OA 系统包含大量标准 CRUD：员工、部门、角色、菜单、公告、资产、会议室、字典、参数、合同、预算等。MyBatis-Plus 可以通过 `ServiceImpl`、`BaseMapper`、分页插件和条件构造器减少重复代码。同时项目中审批、报表和统计又需要灵活查询，MyBatis-Plus 比纯 JPA 更容易控制 SQL 细节。

### 3.4 为什么使用 Redis

系统登录后需要判断 Token 是否有效、用户是否在线、角色是否存在缓存、登录是否过于频繁。Redis 适合保存短生命周期状态：

- `token:{empId}`：服务端保存当前有效 Token，用于踢下线和 Token 失效控制。
- `roles:{empId}`：缓存用户角色，减少每次鉴权查询数据库。
- `online:user:{empId}`：在线用户 TTL，接口访问时续期。
- `rate:login:{ip}`：登录限流计数。

## 4. 数据库设计

### 4.1 数据库总体分域

数据库按业务边界分为五类：

| 分域 | 表名前缀 | 说明 |
| --- | --- | --- |
| 系统基础 | `sys_` | 员工、部门、角色、菜单、字典、参数、岗位 |
| OA 业务 | `oa_` | 考勤、请假、公告、消息、文档、资产、会议、薪资、合同、预算等 |
| 工作流 | `wf_` | 流程定义、流程实例、审批任务、抄送、委托 |
| 报表预警 | `rpt_` | 预警规则、预警日志 |
| 扩展业务 | `adm_`、`hr_`、`fin_`、`km_`、`task_` | 用品、绩效、招聘、培训、知识库、任务、财务合同等 |

这样分域的原因：

- 表名前缀直观表达业务归属，便于维护和排查。
- 系统基础数据被多个业务复用，单独建模能减少重复。
- 工作流独立建模，使业务单据和审批运行态解耦。
- 报表预警独立建模，避免和主业务表耦合。
- 扩展业务使用独立前缀，便于后续拆分或模块化。

### 4.2 通用字段设计

多数业务表包含以下字段：

| 字段 | 含义 | 设计原因 |
| --- | --- | --- |
| `id` | 主键 | 使用自增 Long，适合 MySQL InnoDB 聚簇索引和分页查询 |
| `create_by` / `update_by` | 创建人、更新人 | 满足审计和问题追踪 |
| `create_time` / `update_time` | 创建时间、更新时间 | 支持排序、筛选、报表统计 |
| `del_flag` | 逻辑删除标记 | 避免误删业务数据，保留审计痕迹 |
| `status` | 业务状态 | 统一表示启用、禁用、待审批、已通过、已驳回等 |
| `version` | 乐观锁版本 | 对预算、库存等可能并发更新的数据做并发保护 |

项目在 MyBatis-Plus 中配置了逻辑删除字段 `delFlag`，删除操作默认不物理删除数据，这是 OA 系统常见设计，因为审批、考勤、合同等数据需要保留历史。

### 4.3 系统基础表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `sys_employee` | `emp_code`、`emp_name`、`password`、`phone`、`email`、`dept_id`、`post_id`、`status` | 员工账号和基础资料 |
| `sys_dept` | `dept_name`、`parent_id`、`sort`、`leader`、`status` | 部门树 |
| `sys_post` | `post_code`、`post_name`、`post_sort`、`status` | 岗位 |
| `sys_role` | `role_name`、`role_key`、`sort`、`status` | 角色 |
| `sys_emp_role` | `emp_id`、`role_id` | 员工和角色多对多关系 |
| `sys_menu` | `parent_id`、`menu_name`、`path`、`component`、`perms`、`menu_type`、`icon` | 菜单和权限标识 |
| `sys_role_menu` | `role_id`、`menu_id` | 角色和菜单多对多关系 |
| `sys_dict_type` | `dict_name`、`dict_type`、`status` | 字典类型 |
| `sys_dict_data` | `dict_type`、`dict_label`、`dict_value`、`dict_sort` | 字典数据 |
| `sys_config` | `config_key`、`config_value`、`config_type` | 系统参数 |

设计说明：

- 员工表既是人事基础数据，也是登录账号表，减少账号和员工资料重复维护。
- 部门使用 `parent_id` 自关联，支持多级组织结构。
- 角色、员工、菜单均使用中间表表达多对多关系，符合 RBAC 模型。
- 菜单表同时保存路由和权限标识，方便从同一份配置扩展页面权限和按钮权限。
- 字典表将枚举值从代码中抽离，适合状态、类型等可配置数据。

### 4.4 工作流表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `wf_process_definition` | `process_key`、`process_type`、`node_config`、`status`、`version` | 流程定义，`node_config` 保存流程图 JSON |
| `wf_process_instance` | `process_id`、`business_type`、`business_id`、`initiator_id`、`condition_context`、`status`、`snapshot_node_config` | 某个业务单据的一次流程实例 |
| `wf_task` | `instance_id`、`node_id`、`node_name`、`assignee_id`、`task_type`、`parent_task_id`、`status`、`due_time`、`complete_time` | 审批任务 |
| `oa_approval_record` | `apply_id`、`business_type`、`approver_id`、`approve_status`、`remark`、`task_id`、`node_name` | 审批历史记录 |
| `wf_cc_record` | `instance_id`、`task_id`、`cc_emp_id`、`business_type`、`status` | 抄送记录 |
| `wf_delegation` | `delegator_id`、`delegate_to_id`、`process_category`、`start_date`、`end_date`、`status` | 审批委托 |

工作流独立建模的原因：

- 业务表只关心单据本身，如请假、报销、采购；审批任务、流程节点、处理人等运行态放在 `wf_*` 中。
- `wf_process_instance.business_type + business_id` 可以关联任意业务单据，避免为每种业务单独设计一套审批表。
- `node_config` 用 JSON 保存流程图，前端流程设计器可以直接编辑，后端解析为运行路径。
- `snapshot_node_config` 保存流程实例启动时的流程快照，防止流程定义后续变更影响历史审批。
- `wf_task.parent_task_id` 支持会签、或签、多审批人等场景。
- `due_time`、`remind_count`、`escalation_count` 支持超时提醒、自动通过、自动驳回、升级审批。

### 4.5 审批类业务表设计

审批类业务表都保留 `status` 和 `process_instance_id`：

| 表 | 业务 | 关键字段 |
| --- | --- | --- |
| `oa_leave_apply` | 请假 | `emp_id`、`leave_type`、`start_time`、`end_time`、`days`、`reason`、`status`、`process_instance_id` |
| `oa_business_trip` | 出差 | `emp_id`、`destination`、`purpose`、`start_time`、`end_time`、`status`、`process_instance_id` |
| `oa_outing` | 外出 | `emp_id`、`reason`、`destination`、`start_time`、`end_time`、`status`、`process_instance_id` |
| `oa_overtime` | 加班 | `emp_id`、`overtime_date`、`start_time`、`end_time`、`hours`、`reason`、`status`、`process_instance_id` |
| `oa_expense` | 报销 | `emp_id`、`title`、`amount`、`category`、`description`、`status`、`process_instance_id` |
| `oa_purchase` | 采购 | `emp_id`、`item_name`、`quantity`、`amount`、`reason`、`status`、`process_instance_id` |
| `oa_loan` | 借支 | `emp_id`、`loan_amount`、`repaid_amount`、`loan_reason`、`repayment_plan`、`status`、`process_instance_id` |

状态约定：

- `0`：待审批
- `1`：已通过
- `2`：已驳回
- `3`：已撤回或取消

这样设计的原因：

- 每个业务保留独立表，字段贴合业务，查询和导出简单。
- 审批公共逻辑抽象在 `BaseApprovalServiceImpl`，提交时保存业务单据并启动流程，审批时通过 `WorkflowService` 处理任务。
- 业务表的 `status` 便于列表快速筛选；完整审批历史从 `wf_task` 和 `oa_approval_record` 查询。
- 金额、天数、小时数等字段同时作为业务字段和流程条件上下文，用于分级审批。

### 4.6 考勤与假期表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `oa_attendance` | `emp_id`、`work_date`、`clock_in`、`clock_out`、`status`、`ip`、`address` | 员工每日考勤记录 |
| `oa_attendance_group` | `group_name`、`work_start`、`work_end`、`late_threshold`、`status` | 考勤组 |
| `oa_attendance_group_emp` | `group_id`、`emp_id` | 考勤组员工关系 |
| `oa_leave_balance` | `emp_id`、`leave_type`、`year`、`total_days`、`used_days`、`remaining_days` | 假期余额 |

设计说明：

- 考勤记录按员工和日期建模，适合查询个人历史和管理员统计。
- 考勤组将工作时间和迟到阈值抽离，便于不同部门或员工使用不同考勤规则。
- 假期余额单独建表，审批通过后扣减余额，避免每次查询都从请假记录实时计算。

### 4.7 办公协同表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `oa_notice` | `title`、`content`、`notice_type`、`publisher_id`、`status`、`is_top` | 通知公告 |
| `oa_notice_read` | `notice_id`、`emp_id`、`read_time` | 公告阅读记录 |
| `oa_message` | `sender_id`、`receiver_id`、`msg_type`、`title`、`content`、`is_read` | 内部消息 |
| `oa_document` | `doc_name`、`file_path`、`file_size`、`file_type`、`category_id`、`download_count`、`uploader_id` | 文档中心 |
| `oa_document_category` | `name`、`parent_id`、`sort` | 文档分类 |
| `oa_schedule` | `emp_id`、`title`、`content`、`start_time`、`end_time`、`remind_time`、`status` | 日程 |
| `oa_todo` | `emp_id`、`title`、`todo_type`、`business_id`、`business_type`、`status`、`done_time` | 待办事项 |
| `oa_meeting_room` | `room_name`、`location`、`capacity`、`equipment`、`status` | 会议室 |
| `oa_meeting` | `title`、`room_id`、`organizer_id`、`start_time`、`end_time`、`participants`、`status` | 会议预约 |

设计说明：

- 公告和阅读记录拆分，避免在公告表中保存复杂阅读列表。
- 消息表以收发人为核心，便于计算未读数和分页查询。
- 文档只保存文件元数据，真实文件存储在上传目录，数据库避免存大文件。
- 待办表独立于工作流，可承载审批待办，也可承载普通任务提醒。
- 会议室和会议预约分表，便于会议室资源复用和预约冲突检查。

### 4.8 资产、合同、预算、薪资、人事表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `oa_asset` | `asset_code`、`asset_name`、`category`、`purchase_price`、`status`、`current_user_id`、`dept_id` | 资产台账 |
| `oa_asset_borrow` | `asset_id`、`borrower_id`、`borrow_time`、`expected_return`、`actual_return`、`status` | 资产借还 |
| `oa_contract` | `contract_no`、`contract_name`、`party_a`、`party_b`、`amount`、`start_date`、`end_date`、`manager_id` | 合同管理 |
| `oa_budget` | `dept_id`、`budget_year`、`budget_month`、`amount`、`used_amount`、`status`、`version` | 部门预算 |
| `oa_salary_structure` | `emp_id`、`base_salary`、`post_salary`、`merit_salary`、`allowance`、`effective_date` | 薪资结构 |
| `oa_salary_record` | `emp_id`、`salary_month`、`base_salary`、`deduction`、`actual_amount`、`pay_time` | 薪资记录 |
| `oa_emp_archive` | `emp_id`、`education`、`entry_date`、`contract_start`、`contract_end`、`emergency_contact` | 员工档案 |
| `oa_loan_repayment` | `loan_id`、`amount`、`repay_time`、`remark` | 借支还款 |

设计说明：

- 资产台账和借还记录拆分，资产当前状态可快速查，历史借还可追溯。
- 预算表按部门、年月建模，符合企业预算管控维度。
- 薪资结构和薪资记录拆分，结构表示规则，记录表示某月实际结果。
- 员工档案和员工账号拆分，账号表保持登录字段简洁，档案表保存扩展人事资料。

### 4.9 报表与预警表设计

| 表 | 关键字段 | 功能 |
| --- | --- | --- |
| `rpt_alert_rule` | `rule_name`、`rule_type`、`metric`、`condition_type`、`threshold`、`notify_type`、`notify_targets`、`status` | 预警规则 |
| `rpt_alert_log` | `rule_id`、`alert_level`、`metric_value`、`threshold`、`alert_content`、`notify_status`、`handle_status` | 预警日志 |

报表本身主要从业务表实时聚合，不单独落统计宽表。这样设计适合当前数据规模，能保证数据实时性。预警规则和预警日志单独建表，是因为规则需要配置，日志需要留痕和处理闭环。

### 4.10 扩展业务表设计

`business_schema.sql` 在应用启动时自动执行，创建扩展业务表：

| 分组 | 表 | 功能 |
| --- | --- | --- |
| 办公用品 | `adm_supply_category`、`adm_supply`、`adm_supply_stock`、`adm_supply_request`、`adm_supply_request_item` | 用品分类、库存、领用申请、明细 |
| 人事扩展 | `hr_employee_contract`、`hr_employee_change`、`hr_employee_certificate`、`hr_employee_education` | 员工合同、异动、证书、教育经历 |
| 财务扩展 | `fin_contract`、`fin_payment` | 财务合同、付款 |
| 绩效 | `hr_perf_goal`、`hr_perf_eval`、`hr_perf_result` | 绩效目标、评价、结果 |
| 招聘 | `hr_recruit_job`、`hr_recruit_candidate`、`hr_recruit_interview`、`hr_recruit_offer` | 岗位、候选人、面试、Offer |
| 培训 | `hr_train_course`、`hr_train_plan`、`hr_train_session`、`hr_train_enroll`、`hr_train_record` | 课程、计划、场次、报名、培训记录 |
| 知识库 | `km_category`、`km_entry` | 分类、知识条目 |
| 任务协作 | `task_project`、`task_item` | 项目、任务 |

这些表普遍包含 `del_flag`、`create_time`、`update_time`、`version`，并在常用查询字段上建立索引，如 `status`、`emp_id`、`dept_id`、`category_id`。这样设计可以支持列表分页、状态筛选和并发更新。

## 5. 核心业务模块设计

### 5.1 登录认证模块

主要功能：

- 获取验证码。
- 用户登录。
- 刷新 Token。
- 退出登录。
- 修改密码。
- 保存登录日志和在线状态。

设计方式：

- 前端登录页调用 `/api/auth/captcha` 和 `/api/auth/login`。
- 后端校验用户名、密码、验证码。
- 登录成功后生成 `accessToken` 和 `refreshToken`。
- Token 写入 Redis，接口访问时 `AuthInterceptor` 校验请求头和 Redis 中的 Token 是否一致。
- 登录接口使用 Redis 做 IP 限流，降低暴力破解风险。

为什么这样设计：

- JWT 适合前后端分离，前端可以在请求头携带认证信息。
- Redis 保存 Token 可以实现服务端主动失效，例如退出登录、强制下线。
- 验证码和限流能降低密码攻击风险。

### 5.2 权限与系统管理模块

包含页面：

- 用户管理
- 部门管理
- 角色管理
- 菜单管理
- 字典管理
- 参数配置
- 岗位管理

功能设计：

- 用户管理：员工分页、新增、编辑、删除、重置密码、分配角色。
- 部门管理：部门树、新增子部门、编辑、删除、启停。
- 角色管理：角色分页、新增、编辑、删除、分配菜单。
- 菜单管理：菜单树、路由路径、组件、权限标识、图标、排序。
- 字典管理：字典类型和字典数据分开维护。
- 参数配置：通过 `config_key` 查询配置值。
- 岗位管理：维护岗位编码、岗位名称和排序。

设计原因：

- 使用 RBAC 是企业系统常见权限模型，角色将员工和菜单权限解耦。
- 部门使用树结构，因为组织架构天然是层级结构。
- 菜单和权限标识放在同一张表，便于菜单权限和按钮权限统一扩展。
- 字典和配置表将可变枚举、系统参数从代码中抽离，降低后续变更成本。

### 5.3 考勤管理模块

包含页面：

- 考勤打卡
- 考勤记录
- 考勤管理
- 考勤组管理
- 假期余额

功能设计：

- 员工打卡：上班打卡、下班打卡，生成或更新当天考勤记录。
- 个人记录：按日期范围查询本人考勤。
- 管理员记录：按员工、日期、状态查询全员考勤并导出。
- 考勤组：配置上班时间、下班时间、迟到阈值，并分配员工。
- 假期余额：初始化员工年度假期，审批通过后扣减。

设计原因：

- 打卡记录以员工和日期为核心，可以支撑个人查询、部门统计和月度报表。
- 考勤组使规则配置化，避免所有员工只能使用同一上班时间。
- 假期余额独立存储，审批通过时同步扣减，比每次实时计算更高效。

### 5.4 审批中心与工作流模块

包含页面：

- 审批中心
- 待办任务
- 已办任务
- 我的申请
- 抄送我的
- 审批委托
- 流程定义

功能设计：

- 流程定义：使用前端 `FlowDesigner` 设计流程节点、网关、审批人、超时动作。
- 流程提交：业务表保存单据后，调用 `WorkflowService.startProcess` 创建流程实例和任务。
- 待办任务：查询当前审批人待处理任务。
- 已办任务：查询当前用户处理过的任务。
- 审批处理：通过、驳回、退回、转办、催办。
- 审批时间线：查询审批链路和历史任务。
- 抄送：流程节点可生成抄送记录，用户可标记已读。
- 委托：审批人可设置委托人，委托期间任务可由代理人处理。
- 超时任务：定时任务每 5 分钟检查超时任务，按配置提醒、自动通过、自动驳回或升级。

设计原因：

- 工作流引擎自研并使用 JSON 配置，是因为项目审批场景有限，不需要引入重量级 BPMN 引擎。
- `business_type + business_id` 让一套工作流支持请假、报销、采购等多业务。
- 流程实例保存定义快照，保证历史流程不受新流程配置影响。
- 任务和审批记录分开保存：任务表示当前运行态，审批记录表示审计历史。
- 超时升级、委托、转办可以覆盖企业审批中常见的无人处理场景。

### 5.5 请假、出差、外出、加班、报销、采购、借支模块

这些模块采用统一审批模式：

1. 用户填写申请表。
2. 后端保存业务单据，状态置为待审批。
3. 后端根据业务类型启动对应流程。
4. 审批人处理任务。
5. 流程结束后回调业务状态。
6. 用户可查看审批时间线、撤回、催办、导出。

各模块差异：

| 模块 | 关键字段 | 流程条件 |
| --- | --- | --- |
| 请假 | 请假类型、开始时间、结束时间、天数、原因 | 天数 |
| 出差 | 目的地、事由、开始时间、结束时间 | 天数 |
| 外出 | 外出地点、原因、开始时间、结束时间 | 天数 |
| 加班 | 加班日期、开始时间、结束时间、小时数 | 小时数 |
| 报销 | 标题、金额、类别、说明 | 金额 |
| 采购 | 物品、数量、金额、原因 | 金额 |
| 借支 | 借支金额、原因、还款计划 | 金额 |

设计原因：

- 相同生命周期抽象到 `BaseApprovalServiceImpl`，减少重复代码。
- 不同业务保留独立字段和独立表，便于导出、筛选和报表。
- 金额、天数、小时数作为流程条件，可实现分级审批。例如金额超过 5000 进入总监审批，超过 50000 可进入总经理审批。
- 借支额外设计还款记录表，支持后续分次还款和已还金额统计。

### 5.6 办公协同模块

包含页面：

- 通知公告
- 公告管理
- 文档中心
- 文档管理
- 内部消息
- 发送消息
- 会议管理
- 会议室管理
- 我的日程
- 日程总览
- 工作台
- 待办中心

功能设计：

- 通知公告：管理员发布公告，员工查看并标记已读。
- 文档中心：上传、下载、删除文档，记录下载次数和文件元数据。
- 内部消息：点对点发送消息，统计未读数。
- 会议管理：预约会议室，取消会议，查看会议列表。
- 日程管理：新增、编辑、删除日程，按个人或全局总览展示。
- 工作台：聚合待办、消息、公告、日程、考勤等常用入口。
- 待办中心：将审批和业务提醒统一展示。

设计原因：

- 公告、消息、待办分别建表，是因为三者生命周期不同：公告是一对多发布，消息是一对一通知，待办是任务型提醒。
- 文档文件不直接存数据库，数据库只保存元数据，降低数据库体积和备份压力。
- 工作台聚合多个模块数据，符合 OA 系统“首页处理日常工作”的使用习惯。

### 5.7 资产、合同、预算模块

功能设计：

- 资产管理：资产台账新增、编辑、删除、查询。
- 资产借用：员工借用资产、归还资产，资产状态联动更新。
- 合同管理：维护合同编号、双方、金额、起止日期、附件地址；支持即将到期查询。
- 预算管理：按部门和年月维护预算、已用金额，支持预算状态管理。

设计原因：

- 资产和借用记录拆分，资产表保存当前状态，借用表保存历史。
- 合同按编号唯一，便于企业合同台账管理。
- 预算使用 `version` 乐观锁字段，适合后续控制并发扣减预算。

### 5.8 人事、薪资、绩效、招聘、培训模块

功能设计：

- 员工档案：维护教育经历、入职时间、合同期限、紧急联系人等扩展资料。
- 薪资管理：维护薪资结构和薪资记录。
- 我的薪资：员工查看自己的薪资记录。
- 绩效管理：目标制定、评价提交、结果生成。
- 招聘管理：岗位、候选人、面试、Offer、入职。
- 培训管理：课程、计划、培训场次、报名、签到、评分、培训记录。

设计原因：

- 员工账号和员工档案分离，避免登录账号表过于臃肿。
- 薪资结构和薪资记录分离，结构用于规则维护，记录用于月度结果沉淀。
- 绩效、招聘、培训作为扩展 HR 子域独立建表，避免核心员工表承载过多可变业务。

### 5.9 财务、知识库、任务协作、办公用品模块

功能设计：

- 财务合同：维护财务合同、激活、关闭。
- 付款管理：创建付款、提交付款、标记已支付。
- 知识库：分类树、条目创建、发布、归档。
- 任务协作：项目、任务、状态变更、进度更新。
- 办公用品：分类、用品、库存、库存调整、领用申请、审批或驳回。

设计原因：

- 扩展业务使用独立表前缀，便于后期按模块拆分服务。
- 库存类数据使用 `version`，为并发库存调整保留乐观锁能力。
- 任务协作和知识库属于协同能力，和审批主流程解耦。

### 5.10 报表与数据看板模块

包含页面：

- 个人报表
- 管理报表
- 数据看板
- 预警规则
- 预警记录

功能设计：

- 个人报表：个人考勤汇总、考勤趋势、请假统计、月度对比。
- 管理报表：部门考勤对比、考勤趋势、请假分析、员工排名、今日概览。
- 数据看板：汇总考勤、审批、业务指标、趋势图、漏斗图、热力图。
- 预警规则：配置指标、阈值、通知方式、通知对象。
- 预警记录：查看预警日志并处理。

设计原因：

- 报表从业务表聚合，保证实时性，避免同步宽表。
- 复杂图表逻辑前端使用 ECharts 展示，后端只提供结构化数据。
- 预警规则和日志分离，规则可配置，日志可追踪处理闭环。

### 5.11 系统监控与日志模块

包含页面：

- 在线用户
- 登录日志
- 操作日志
- 系统日志

功能设计：

- 在线用户：基于 Redis 在线 Key 查询当前用户，可强制下线。
- 登录日志：记录用户名、IP、浏览器、系统、登录状态和时间。
- 操作日志：通过 AOP 和 `@OperationLog` 注解记录模块、操作、方法、URL、IP、耗时、状态。
- 系统日志：提供系统运行日志查看能力。

设计原因：

- 在线用户放 Redis，TTL 自然过期，适合会话状态。
- 操作日志用 AOP 实现，不侵入具体业务代码。
- 登录日志和操作日志分开，便于安全审计和业务审计分别查询。

## 6. 接口设计

### 6.1 统一响应格式

后端统一返回：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {}
}
```

分页返回：

```json
{
  "total": 100,
  "list": []
}
```

设计原因：

- 前端请求拦截器可以统一判断 `code === 0`。
- 错误信息统一展示，不需要每个页面重复处理。
- 分页字段固定为 `total` 和 `list`，前端表格和分页组件可复用。

### 6.2 主要接口分组

| 分组 | 路径示例 | 功能 |
| --- | --- | --- |
| 认证 | `/api/auth/login`、`/refresh-token`、`/logout` | 登录、续期、退出 |
| 系统 | `/api/employee`、`/api/dept`、`/api/menu`、`/role` | 用户、部门、菜单、角色 |
| 考勤 | `/api/attendance`、`/api/attendance-group` | 打卡、考勤记录、考勤组 |
| 审批业务 | `/api/leave`、`/api/expense`、`/api/purchase` | 提交、审批、分页、导出 |
| 工作流 | `/api/workflow` | 流程定义、任务、历史、委托、抄送 |
| 协同 | `/api/notice`、`/api/message`、`/api/document`、`/api/meeting` | 公告、消息、文档、会议 |
| 报表 | `/api/report`、`/api/statistics` | 个人报表、管理报表、看板 |
| 监控 | `/online-logs`、`/login-logs`、`/operation-logs` | 在线、登录、操作日志 |
| 扩展业务 | `/api/admin/supplies`、`/api/hr-performance`、`/api/task` | 用品、绩效、任务等 |

接口整体接近 REST 风格，标准 CRUD 使用 `GET/POST/PUT/DELETE`，审批、导出、激活、取消等动作使用动作路径。这是管理系统常见折中：资源型接口清晰，动作型业务语义也容易理解。

## 7. 安全与权限设计

### 7.1 登录安全

- 密码通过工具类加密校验。
- 登录需要验证码。
- 登录接口按 IP 限流。
- Token 存在 Redis 中，退出或强制下线后服务端可使 Token 失效。

### 7.2 接口鉴权

`AuthInterceptor` 完成以下工作：

- 检查 `Authorization` 请求头。
- 解析 JWT。
- 校验 Redis 中保存的 Token。
- 将 `empId`、`empName` 写入请求上下文。
- 校验 `@RequireAdmin`、`@RequireRole`、`@RequirePermission`。
- 刷新在线用户 TTL。

设计原因：

- 拦截器统一鉴权，避免每个 Controller 重复校验。
- 注解式权限便于在敏感接口上声明访问要求。
- Redis Token 校验让 JWT 不完全无状态，系统具备主动失效能力。

### 7.3 前端权限

前端路由 `meta.roles` 控制页面访问，菜单项也配置 `roles` 控制显示。前端权限用于用户体验，后端权限用于安全边界。即使用户绕过前端访问接口，后端拦截器仍会校验。

## 8. 通知与实时能力设计

系统同时使用三种提醒渠道：

- 待办表 `oa_todo`：保存可查询、可完成的任务提醒。
- 消息表 `oa_message`：保存内部消息和未读状态。
- WebSocket：实时推送审批结果、待办和消息。

这样设计的原因：

- WebSocket 只负责实时性，不适合做唯一存储。
- 待办和消息落库后，即使用户离线也不会丢失。
- 前端顶部未读数通过定时查询和 WebSocket 增量更新结合，兼顾实时性和可靠性。

## 9. 文件上传与导出设计

### 9.1 文件上传

文档模块上传文件时，后端保存文件到 `oa.upload.path` 配置目录，数据库 `oa_document` 保存文件名、路径、大小、类型、上传人、下载次数。

这样设计是因为数据库适合保存元数据，不适合直接保存大文件。文件系统存储实现简单，适合单体项目；后续可替换为对象存储。

### 9.2 Excel 导出

审批和考勤模块提供导出接口，后端使用 EasyExcel 输出文件，前端 `downloadFile` 工具处理 Blob 和文件名。

导出放在后端实现，是因为后端拥有完整查询条件、权限和字段格式，能避免前端导出数据不完整或越权。

## 10. 运行与部署设计

### 10.1 开发环境

后端默认端口：`8080`  
前端默认端口：`8848`  
数据库：`oa_system`  
Redis：`127.0.0.1:6379`

前端 Vite 代理将 `/api`、`/login`、`/logout`、`/refresh-token`、`/user` 等路径转发到后端 `localhost:8080`。

### 10.2 生产环境

`application-prod.yml` 使用环境变量配置：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `UPLOAD_PATH`

这样设计可以避免生产密码硬编码到代码仓库。

### 10.3 构建验证

前端本地验证方式：

```bash
node .\node_modules\vue-tsc\bin\vue-tsc.js -b
node .\node_modules\vite\bin\vite.js build
```

当前前端可通过类型检查和生产构建。构建时存在大 chunk 警告，主要来自 ECharts 和图表主题相关代码，后续可通过手动分包优化。

## 11. 测试设计

后端包含 Controller 和 Service 测试，例如：

- 登录认证测试
- 员工、部门、公告、消息、会议、文档等 Controller 测试
- 请假、考勤、工作流、审批基类等 Service 测试

前端包含：

- 单元测试：业务 API 调用参数验证。
- E2E 测试：登录页基础控件渲染。

测试重点放在认证、审批、考勤、导出、分页和核心 Controller，是因为这些是系统主流程，回归风险最高。

## 12. 设计优点与后续优化

### 12.1 当前设计优点

- 模块边界清晰，系统、业务、流程、报表分域明确。
- 通用审批抽象减少重复代码。
- 工作流和业务表解耦，多个业务可复用同一套流程引擎。
- Redis + JWT 兼顾前后端分离和服务端主动失效。
- 前端组件和 API 按模块拆分，新增页面成本较低。
- 数据库保留审计字段和逻辑删除，符合 OA 历史追踪需求。

### 12.2 可优化点

- 路由和菜单当前分开维护，后续可从菜单表动态生成路由。
- 部分接口路径仍存在 `/user`、`/role` 和 `/api/*` 混用，后续可统一为 `/api/system/*`。
- 前端和后端部分代码使用 `any` 或弱类型 Map，后续可增强 DTO/VO 类型。
- ECharts 相关构建 chunk 较大，后续可按页面懒加载图表模块。
- 文件目前存本地目录，生产环境可迁移到对象存储。
- 工作流 JSON 自研实现适合当前规模，若后续流程复杂到需要 BPMN 标准，可考虑接入 Flowable 或 Camunda。

## 13. 总结

本项目以企业 OA 的高频场景为核心，采用前后端分离和单体后端分层架构。数据库围绕组织权限、业务单据、流程运行态、协同办公和统计预警进行分域设计。系统使用 MySQL 保证业务数据一致性，使用 Redis 管理短生命周期状态，使用 WebSocket 提供实时通知，使用自研轻量工作流支撑多类审批业务。

整体设计适合中小型企业 OA、课程设计、毕业设计或内部管理系统原型。其最大特点是业务覆盖完整、审批流程统一、模块结构清晰，并保留了继续扩展为动态菜单、对象存储、复杂流程引擎和服务拆分的空间。
