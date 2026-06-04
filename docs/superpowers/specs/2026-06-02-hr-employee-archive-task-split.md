# HR 员工档案与异动子模块重构任务拆分

> 日期: 2026-06-03  
> 子模块范围: 员工档案与异动 (personnel / employee-archive) — 扩展档案、异动记录、劳动合同、证书资质、教育经历、工作经历  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> 试点参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`  

---

## 1. 子模块说明与目标

员工档案与人事异动是整个 HR 系统乃至 OA 系统的底层核心与业务终点。入职、转正、调动、晋升、离职等所有人事异动流程的终极目的都是更新员工的主档案和工作状态，进而影响后续的考勤、假期额度计算、消息通知以及薪资计算。

本子模块重构的目标是：
1. **统一数据规范**：规范员工扩展信息，淘汰旧的 `oa_emp_archive` 扁平表结构。
2. **规范化 1:N 详情表设计**：将教育经历、工作经历、证书、合同等原本通过字符串、JSON 堆叠或缺失的强关联业务，拆分为规范的 1:N 关系子表，提供完善、安全的结构化维护能力。
3. **流程与档案闭环联动**：构建基于领域事件和工作流回调的人事异动机制。员工发起“转正/调动/晋升/离职”等申请，审批通过后，由工作流回调 Handler 自动、幂等地修改员工的主表信息（`sys_employee`）以及扩展档案表（`hr_employee_ext`），实现自动化生命周期管理：
   - **入职（Entry）**：发起异动（entry 态），审批通过后在 `sys_employee` 生成正式工号，激活 `hr_employee_ext`，工作状态设为“试用（probation）”。
   - **转正（Regularization）**：试用期到期触发转正审批流，审批通过后自动更新 `hr_employee_ext` 试用期状态为“已转正（passed）”，工作状态变更为“正式（active）”。
   - **调动/晋升（Transfer/Promotion）**：审批通过后自动修改 `sys_employee` 的部门（`dept_id`）、岗位（`post_id`），同时记录异动轨迹。
   - **离职（Quit）**：离职审批通过，自动标记 `sys_employee.status = 0`（禁用状态），`hr_employee_ext.work_status = 'leave'`（离职状态），同时将关联劳动合同设为解约/失效。

---

## 2. 边界定义

### 2.1 包含范围

| 区域 | 内容 | 说明 |
|------|------|------|
| 数据库 | `hr_employee_ext`、`hr_transfer`、`hr_contract`、`hr_certificate`、`hr_education`、`hr_work_experience` | 重构这 6 张表，删除或废弃旧 `oa_emp_archive` |
| 后端 | `oa-hr` 模块内新增/修改的 Entity、DTO、VO、Enum、Mapper、Service、Controller、测试 | 包含完整的档案 CRUD、经历子表 CRUD、异动发起与状态回调逻辑 |
| 工作流 | 对接 `oa-workflow` 引擎回调 | 异动流程通过工作流回调驱动档案状态变更 |
| Web | 员工档案维护、异动申请与记录列表、劳动合同及经历管理、资质证书列表 | 管理端全员管理与员工端个人信息自维护 |
| Mobile | 移动端员工个人扩展信息查看、紧急联系人自更新、异动审批状态查看 | H5/小程序适配 |
| 测试 | 领域层单元测试 + 状态机流转集成测试 + API Controller 测试 | 保证状态转换幂等性与数据一致性 |

### 2.2 不包含范围

| 不包含 | 原因 |
|--------|------|
| 批量入职导入 (Excel) | 涉及 EasyExcel 复杂转换，留给 HR 专项工具页处理 |
| 薪资自动联动计算 | 薪资定档属于 `oa-finance` 范围，本模块仅在异动通过时发布 `EmployeeTransferEvent` 供下游订阅 |
| 社保公积金计算 | 业务链条过长，在独立福利模块实现 |
| 考勤排班联动 | 考勤模块属于独立域，本模块提供状态支持即可 |
| 档案图片/附件的直接存储 | 由统一文件服务处理，本模块仅存储 OSS 相对路径或绝对链接 |

---

## 3. 核心数据模型

### 3.1 六张表总览

| 表名 | 说明 | 核心职责 |
|------|------|----------|
| `hr_employee_ext` | 员工档案扩展表 | 扩展 `sys_employee` 表，存储试用期、技能标签、紧急联系人、身份证号等主档案属性 |
| `hr_transfer` | 员工异动记录表 | 记录员工在公司内的全生命周期轨迹（入职、转正、调动、晋升、离职）及审批状态 |
| `hr_contract` | 劳动合同表 | 存储员工历次劳动合同及试用期服务期限、电子附件等 |
| `hr_certificate` | 员工证书资质表 | 存储员工获得的专业资质、技能证书、语种证书等 1:N 关系数据 |
| `hr_education` | 教育经历表 | 存储员工专科、本科、硕士、博士等学历阶段 of 1:N 关系数据 |
| `hr_work_experience`| 工作经历表 | 存储员工入职前的历史工作经历 1:N 关系数据 |

### 3.2 表结构详细设计

#### 3.2.1 `hr_employee_ext` — 员工档案扩展表

```sql
DROP TABLE IF EXISTS `hr_employee_ext`;
CREATE TABLE `hr_employee_ext` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `probation_period`    INT          DEFAULT 3               COMMENT '试用期月数',
  `probation_start_date` DATE        DEFAULT NULL            COMMENT '试用期开始日',
  `probation_end_date`  DATE         DEFAULT NULL            COMMENT '试用期截止日',
  `probation_status`    VARCHAR(32)  DEFAULT 'probation'     COMMENT '试用期状态(probation-试用中/passed-已转正/extended-延期转正/failed-转正未通过)',
  `skills`              JSON         DEFAULT NULL            COMMENT '晋升技能/特长标签JSON数组 ["Java","Spring Boot"]',
  `emergency_contact`   VARCHAR(64)  DEFAULT NULL            COMMENT '紧急联系人',
  `emergency_phone`     VARCHAR(20)  DEFAULT NULL            COMMENT '紧急联系电话',
  `emergency_relation`  VARCHAR(32)  DEFAULT NULL            COMMENT '紧急联系人关系(spouse-配偶/parents-父母/children-子女/others-其他)',
  `political_status`    VARCHAR(32)  DEFAULT 'mass'          COMMENT '政治面貌(party_member-党员/youth_league-团员/mass-群众)',
  `marital_status`      VARCHAR(32)  DEFAULT 'single'        COMMENT '婚姻状况(single-未婚/married-已婚/divorced-离异/widowed-丧偶)',
  `id_card`             VARCHAR(32)  DEFAULT NULL            COMMENT '身份证号',
  `birth_date`          DATE         DEFAULT NULL            COMMENT '出生日期',
  `household_register`  VARCHAR(128) DEFAULT NULL            COMMENT '户籍所在地',
  `address`             VARCHAR(256) DEFAULT NULL            COMMENT '家庭现住址',
  `employee_type`       VARCHAR(32)  DEFAULT 'fulltime'      COMMENT '员工类型(fulltime-正式工/parttime-兼职/intern-实习生/contractor-外包)',
  `work_status`         VARCHAR(32)  DEFAULT 'active'        COMMENT '工作状态(active-正式在职/probation-试用在职/leave-离职/suspended-停职/retired-退休)',
  `remark`              VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_id` (`emp_id`),
  KEY `idx_work_status` (`work_status`),
  KEY `idx_probation_status` (`probation_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工档案扩展表';
```

#### 3.2.2 `hr_transfer` — 员工异动表

```sql
DROP TABLE IF EXISTS `hr_transfer`;
CREATE TABLE `hr_transfer` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '异动ID',
  `transfer_no`         VARCHAR(64)  NOT NULL                COMMENT '异动单号(唯一)',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `transfer_type`       VARCHAR(32)  NOT NULL                COMMENT '异动类型(entry-入职/regular-转正/transfer-调动/promotion-晋升/demotion-降职/quit-离职)',
  `from_dept_id`        BIGINT       DEFAULT NULL            COMMENT '原部门ID',
  `to_dept_id`          BIGINT       DEFAULT NULL            COMMENT '新部门ID',
  `from_dept_name`      VARCHAR(100) DEFAULT NULL            COMMENT '原部门名称',
  `to_dept_name`        VARCHAR(100) DEFAULT NULL            COMMENT '新部门名称',
  `from_post_id`        BIGINT       DEFAULT NULL            COMMENT '原岗位ID',
  `to_post_id`          BIGINT       DEFAULT NULL            COMMENT '新岗位ID',
  `from_post_name`      VARCHAR(100) DEFAULT NULL            COMMENT '原岗位名称',
  `to_post_name`        VARCHAR(100) DEFAULT NULL            COMMENT '新岗位名称',
  `reason`              VARCHAR(500) DEFAULT NULL            COMMENT '异动原因',
  `effective_date`      DATE         NOT NULL                COMMENT '生效日期',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT-草稿/RUNNING-流程中/PASSED-审批通过/REJECTED-驳回/REVOKED-撤回/EFFECTIVE-已生效)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '流程实例ID',
  `current_task_id`     BIGINT       DEFAULT NULL            COMMENT '当前任务ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_no` (`transfer_no`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_transfer_type` (`transfer_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工异动表';
```

#### 3.2.3 `hr_contract` — 劳动合同表

```sql
DROP TABLE IF EXISTS `hr_contract`;
CREATE TABLE `hr_contract` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `contract_no`         VARCHAR(64)  NOT NULL                COMMENT '合同编号(唯一)',
  `contract_type`       VARCHAR(32)  NOT NULL DEFAULT 'initial' COMMENT '合同类型(initial-首签/renew-续签/internship-实习/dispatch-派遣)',
  `term_type`           VARCHAR(32)  NOT NULL DEFAULT 'fixed' COMMENT '期限类型(fixed-固定期限/unfixed-无固定期限)',
  `sign_date`           DATE         NOT NULL                COMMENT '签署日期',
  `start_date`          DATE         NOT NULL                COMMENT '生效日期',
  `end_date`            DATE         DEFAULT NULL            COMMENT '到期日期(无固定期限可为空)',
  `probation_end_date`  DATE         DEFAULT NULL            COMMENT '试用期到期日',
  `attachment_url`      VARCHAR(512) DEFAULT NULL            COMMENT '合同附件URL',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'draft' COMMENT '合同状态(draft-草稿/active-生效中/expired-已到期/terminated-已解约)',
  `remark`              VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_start_end` (`start_date`, `end_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='劳动合同表';
```

#### 3.2.4 `hr_certificate` — 员工证书资质表

```sql
DROP TABLE IF EXISTS `hr_certificate`;
CREATE TABLE `hr_certificate` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资质ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `cert_name`           VARCHAR(128) NOT NULL                COMMENT '证书名称',
  `cert_no`             VARCHAR(64)  NOT NULL                COMMENT '证书编号',
  `authority`           VARCHAR(128) DEFAULT NULL            COMMENT '颁发机构',
  `issue_date`          DATE         NOT NULL                COMMENT '颁发日期',
  `expiry_date`         DATE         DEFAULT NULL            COMMENT '有效期至(为空表示长期有效)',
  `attachment_url`      VARCHAR(512) DEFAULT NULL            COMMENT '证书附件URL',
  `remark`              VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_cert_no` (`cert_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工证书资质表';
```

#### 3.2.5 `hr_education` — 教育经历表

```sql
DROP TABLE IF EXISTS `hr_education`;
CREATE TABLE `hr_education` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '教育经历ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `school_name`         VARCHAR(128) NOT NULL                COMMENT '学校名称',
  `major`               VARCHAR(100) DEFAULT NULL            COMMENT '专业',
  `degree`              VARCHAR(32)  NOT NULL                COMMENT '学位/学历(highschool-高中/juniorcollege-大专/bachelor-本科/master-硕士/doctor-博士)',
  `start_date`          DATE         NOT NULL                COMMENT '入学日期',
  `end_date`            DATE         NOT NULL                COMMENT '毕业日期',
  `education_type`      VARCHAR(32)  NOT NULL DEFAULT 'fulltime' COMMENT '培养类型(fulltime-全日制/parttime-非全日制)',
  `is_highest`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '是否最高学历(0否 1是)',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教育经历表';
```

#### 3.2.6 `hr_work_experience` — 工作经历表

```sql
DROP TABLE IF EXISTS `hr_work_experience`;
CREATE TABLE `hr_work_experience` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工作经历ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `company_name`        VARCHAR(128) NOT NULL                COMMENT '工作单位',
  `post_name`           VARCHAR(64)  NOT NULL                COMMENT '职务/岗位',
  `start_date`          DATE         NOT NULL                COMMENT '入职时间',
  `end_date`            DATE         NOT NULL                COMMENT '离职时间',
  `salary`              VARCHAR(32)  DEFAULT NULL            COMMENT '薪资(文字或数字范围)',
  `leaving_reason`      VARCHAR(200) DEFAULT NULL            COMMENT '离职原因',
  `achievement`         VARCHAR(1000) DEFAULT NULL           COMMENT '主要职责与业绩',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作经历表';
```

### 3.3 索引与 EXPLAIN 验收

| 查询场景 | 依赖索引 | EXPLAIN 期待结果 |
|----------|----------|------------------|
| 查询员工主扩展档案 | `uk_emp_id (emp_id)` | type=const / ref |
| 查询员工的历史经历或合同 (1:N) | `idx_emp_id` (在经历与合同子表) | type=ref, key=idx_emp_id, rows很小 |
| 按异动单号回查异动详情 | `uk_transfer_no` | type=const |
| 工作流引擎根据实例ID回查单据 | `idx_status` + `idx_emp_id` | type=ref 或 range |
| 查找生效日期临近或状态处于 probation 状态的员工 | `idx_probation_status` / `idx_work_status` | type=ref |

---

## 4. 状态枚举与数据约定

### 4.1 员工工作状态 `EmployeeWorkStatus`

| Code | 说明 |
|------|------|
| `PROBATION` | 试用期在职 |
| `ACTIVE` | 正式在职 |
| `SUSPENDED` | 停职/休假中 |
| `RETIRED` | 退休 |
| `LEAVE` | 离职 |

### 4.2 试用期转正状态 `ProbationStatus`

| Code | 说明 |
|------|------|
| `PROBATION` | 试用期中 |
| `PASSED` | 已转正 |
| `EXTENDED` | 试用期延期中 |
| `FAILED` | 试用不合格未通过 |

### 4.3 异动类型 `TransferType`

| Code | 说明 |
|------|------|
| `ENTRY` | 入职异动 |
| `REGULAR` | 试用转正 |
| `TRANSFER` | 跨部门平调 |
| `PROMOTION` | 岗位晋升 |
| `DEMOTION` | 降职降级 |
| `QUIT` | 离职解约 |

### 4.4 异动记录流程状态 `TransferStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 草稿 |
| `RUNNING` | 审批流程中 |
| `PASSED` | 审批已通过 (等待到达生效日期) |
| `REJECTED` | 审批已驳回 |
| `REVOKED` | 申请人已撤回 |
| `EFFECTIVE`| 异动已生效 (状态和部门数据已完成变更修改) |

### 4.5 合同期限类型 `ContractTermType`

| Code | 说明 |
|------|------|
| `FIXED` | 有固定期限合同 |
| `UNFIXED` | 无固定期限合同 |

### 4.6 合同状态 `ContractStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 合同草稿 |
| `ACTIVE` | 合同生效中 |
| `EXPIRED` | 合同已到期 |
| `TERMINATED` | 合同已提前解除/解约 |

---

## 5. API 契约

### 5.1 统一前缀与规范

- REST API 根路径：`/api/hr`
- 返回格式：统一使用 `R<T>`
- 数据过滤：基于当前登录用户进行自查询，或根据管理员身份/数据权限进行跨人查询。
- 权限码规范：`hr:emp:{resource}:{action}`

### 5.2 员工扩展档案与经历详情接口 (自服务/管理端通用)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/hr/employees/{empId}/ext` | `hr:emp:ext:view` | 获取指定员工主扩展档案 |
| `PUT` | `/api/hr/employees/{empId}/ext` | `hr:emp:ext:update` | 管理员更新员工扩展档案属性 |
| `PUT` | `/api/hr/my/ext` | `hr:emp:my:update` | 员工自维护部分字段 (如紧急联系人/电话/住址) |
| `GET` | `/api/hr/employees/{empId}/educations` | `hr:emp:edu:list` | 查看教育经历列表 |
| `POST` | `/api/hr/employees/{empId}/educations` | `hr:emp:edu:create` | 新增教育经历记录 |
| `PUT` | `/api/hr/educations/{id}` | `hr:emp:edu:update` | 编辑指定教育经历 |
| `DELETE` | `/api/hr/educations/{id}` | `hr:emp:edu:delete` | 删除指定教育经历 (逻辑删除) |
| `GET` | `/api/hr/employees/{empId}/work-experiences` | `hr:emp:work:list` | 查看工作经历列表 |
| `POST` | `/api/hr/employees/{empId}/work-experiences` | `hr:emp:work:create` | 新增历史工作经历 |
| `PUT` | `/api/hr/work-experiences/{id}` | `hr:emp:work:update` | 编辑历史工作经历 |
| `DELETE` | `/api/hr/work-experiences/{id}` | `hr:emp:work:delete` | 删除历史工作经历 |
| `GET` | `/api/hr/employees/{empId}/certificates` | `hr:emp:cert:list` | 获取证书资质列表 |
| `POST` | `/api/hr/employees/{empId}/certificates` | `hr:emp:cert:create` | 录入新证书 |
| `PUT` | `/api/hr/certificates/{id}` | `hr:emp:cert:update` | 编辑证书资质 |
| `DELETE` | `/api/hr/certificates/{id}` | `hr:emp:cert:delete` | 废弃证书资质 |
| `GET` | `/api/hr/employees/{empId}/contracts` | `hr:emp:contract:list` | 查询员工劳动合同记录 |
| `POST` | `/api/hr/employees/{empId}/contracts` | `hr:emp:contract:create` | 新增/签署新劳动合同 |
| `PUT` | `/api/hr/contracts/{id}` | `hr:emp:contract:update` | 编辑/调整劳动合同信息 |
| `DELETE` | `/api/hr/contracts/{id}` | `hr:emp:contract:delete` | 逻辑删除合同条目 |

### 5.3 异动申请与处理接口

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST`| `/api/hr/transfers` | `hr:emp:transfer:create` | 发起异动申请 (创建并提交流程) |
| `GET` | `/api/hr/transfers` | `hr:emp:transfer:list` | 分页查询全员异动审批历史 (管理端) |
| `GET` | `/api/hr/transfers/{id}` | `hr:emp:transfer:detail` | 查询单条异动详情 (包括前后职位、生效日、当前流程节点) |
| `GET` | `/api/hr/employees/{empId}/transfers` | `hr:emp:transfer:view` | 查询单人异动历史轨迹 |
| `POST`| `/api/hr/transfers/{id}/actions/cancel` | `hr:emp:transfer:cancel` | 撤回未开始或待生效异动申请 |

---

## 6. DTO 与 VO 设计

### 6.1 `HrEmployeeExtUpdateDTO`

| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `emergencyContact` | String | 必填，不超过64字符 | 紧急联系人姓名 |
| `emergencyPhone` | String | 必填，合法的电话/手机格式 | 紧急联系电话 |
| `emergencyRelation`| String | 必填，枚举值(spouse/parents/...) | 关系 |
| `politicalStatus` | String | 可选，枚举值 | 政治面貌 |
| `maritalStatus` | String | 可选，枚举值 | 婚姻状况 |
| `idCard` | String | 可选，18位身份证格式正则 | 身份证号 |
| `birthDate` | LocalDate | 可选 | 出生日期 |
| `householdRegister`| String | 可选 | 户籍地址 |
| `address` | String | 必填，不超过256字符 | 家庭常住址 |
| `skills` | List<String> | 可选 | 掌握技能标签数组 |
| `remark` | String | 可选，不超过500字符 | 备注 |

### 6.2 `HrTransferCreateDTO`

| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `empId` | Long | 必填 | 被异动员工 ID |
| `transferType` | String | 必填，枚举值(entry/regular/transfer/promotion/demotion/quit) | 异动类型 |
| `toDeptId` | Long | `transferType` 属于部门平调/晋升/降职时必填 | 新目标部门 ID |
| `toPostId` | Long | `transferType` 属于晋升/平调/降职时必填 | 新目标岗位 ID |
| `reason` | String | 必填，不超过500字符 | 异动事由/离职原因说明 |
| `effectiveDate` | LocalDate | 必填，必须等于或晚于当前日期 | 期望生效日期 |

### 6.3 `HrEmployeeProfileVO`

这是员工自查或管理员查询时返回的**聚合大视图**，除主表基本信息外，包装扩展表、最高学位、合同状态、异动轨迹。

| 字段 | 类型 | 说明 |
|------|------|------|
| `empId` | Long | 员工 ID |
| `empCode` | String | 工号 |
| `realName` | String | 姓名 |
| `deptName` | String | 部门名称 |
| `postName` | String | 岗位名称 |
| `workStatus` | String | 工作状态 (active/leave/...) |
| `workStatusName` | String | 状态展示文字 |
| `employeeType` | String | 员工类型 (fulltime/intern/...) |
| `probationStatus`| String | 试用期转正状态 |
| `probationEndDate`| LocalDate | 试用期截止日期 |
| `skills` | List<String> | 技能数组 |
| `highestDegree` | String | 最高学位展示 (来自 hr_education) |
| `contractStatus` | String | 合同状态 (生效中/未签/过期) |
| `emergencyContact`| String | 紧急联系人 |
| `emergencyPhone` | String | 紧急联系人电话 |
| `address` | String | 家庭现住址 |

---

## 7. 新旧映射与演进关系

重构时旧的 `oa_emp_archive` 包含较窄的扁平字段，新模型全面兼容并在底层通过 1:N 关系解耦：

| 旧版表/字段 | 新版表/字段 | 映射策略与演进方案 |
|-------------|-------------|--------------------|
| `oa_emp_archive.education` | `hr_education` 详情表 | 升级为 1:N 子表，支持多学历按入学毕业时间排序。旧单条教育字段由新最高学历 `hr_education.is_highest = '1'` 兼容。 |
| `oa_emp_archive.major` | `hr_education.major` | 映射入教育子表对应毕业学历记录。 |
| `oa_emp_archive.graduate_school` | `hr_education.school_name` | 映射入教育经历子表。 |
| `oa_emp_archive.emergency_contact` | `hr_employee_ext.emergency_contact` | 直接映射。 |
| `oa_emp_archive.emergency_phone` | `hr_employee_ext.emergency_phone` | 直接映射。 |
| `oa_emp_archive.address` | `hr_employee_ext.address` | 直接映射。 |
| `oa_emp_archive.contract_start` / `_end` | `hr_contract` 合同子表 | 升级为 1:N 子表。一个员工可以有实习合同、首签合同、续签合同等多项，自动计算最新活跃合同同步给扩展主表视图。 |
| 无异动表 | `hr_transfer` 异动状态表 | 全新增加。提供异动的流程审批与生命周期追踪，代替原本依靠手动修改数据库的简陋管理模式。 |

---

## 8. 任务波次拆分 (Wave 1 至 Wave 5)

```
Wave 1
├── T1 + T2 (并行)     # 契约设计与旧依赖分析
Wave 2
└── T3                  # 六表 Entity + Mapper + Enum 定义与测试
Wave 3
└── T4                  # 人事核心 Service 实现、级联状态引擎、EmployeeTransferEvent 驱动
Wave 4
└── T5                  # REST Controller 控制器 + 工作流 Callback 处理器
Wave 5
├── T6 与 T7 (并行)     # Web 界面组件迁移 (Vue3) 与 Mobile H5 个人档案页
└── T8                  # 闭环集成验证与旧实现安全下线
```

### Wave 1: 契约与基线

#### T1 数据库与 API 契约
| 字段 | 内容 |
|------|------|
| **目标** | 在文档中确立扩展档案、异动、合同、证书、教育、工作经历六张表的建表规范及 REST API 规范。 |
| **路径** | `code/backend/sql/` |
| **输入** | 重构规范、基线 `001_schema.sql`、旧 `OaEmpArchive` 实体结构。 |
| **输出** | 新增建表 SQL 草案 `code/backend/sql/hr_employee_contract.sql`。 |
| **禁止修改** | 不创建 Java 类或任何 Vue 界面，保持 SQL 独立干净。 |
| **验收** | 本设计文档及建表 SQL 完备。 |

#### T2 旧实现影响分析
| 字段 | 内容 |
|------|------|
| **目标** | 找出旧 `OaEmpArchive` 和所有与档案/合同相关的旧控制器的被依赖情况，拟定下线名单与渐进下线策略。 |
| **路径** | `code/backend/`、`code/frontend/`、`code/mobile/` |
| **输入** | 全局 grep 关键字 `OaEmpArchive` / `OaEmpArchiveMapper` / `oa_emp_archive` / `OaEmpArchiveService`。 |
| **输出** | 提供废弃入口与依赖点清理清单，确定前端路由下线路径。 |
| **禁止修改** | 不做任何实质性的业务代码删除，保持编译运行正常。 |
| **验收** | 产出精确的影响分析列表和下线准备报告。 |

---

### Wave 2: 实体与数据访问

#### T3 员工档案与异动 Entity + Mapper
| 字段 | 内容 |
|------|------|
| **目标** | 在 `oa-hr` 模块中创建六表实体 (Entity)、数据传输对象 (DTO)、响应对象 (VO)、状态枚举 (Enum) 及 BaseMapper 接口。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/` (含 `entity`, `mapper`, `dto`, `vo`, `enums`) |
| **输入** | T1 DDL 契约，MyBatis-Plus 开发规范，Lombok，Validation 注解。 |
| **输出** | 6个 Entity、6个 Mapper 接口及 XML（如需复杂 Join 分页）、3个基础 Enum 及 DTO / VO。 |
| **禁止修改** | 不写复杂的 Service 业务计算，不改变 Controller。 |
| **验收** | 增加基础 JUnit 测试，运行 `cd code/backend && mvn -pl oa-hr -am test` 通过。 |

---

### Wave 3: 核心逻辑层

#### T4 人事核心业务 Service
| 字段 | 内容 |
|------|------|
| **目标** | 实现扩展档案的自维护与管理、4张 1:N 经历子表（教育、工作、证书、合同）的完整 CRUD、以及异动单据（转正/调动/晋升/离职）的生成校验与生命周期生效动作。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/service/` (接口与 `impl`) |
| **输入** | T3 的 Mapper，人事生命周期状态规则。 |
| **输出** | 5个核心 Service 接口及实现类。异动完成时发布领域事件 `EmployeeTransferEvent` 的机制，完整的 Service 单元测试（使用 H2 数据库或 Mock 框架）。 |
| **禁止修改** | 不编写 Controller 逻辑，不直接调用 `wf_*` 引擎底层表，通过工作流暴露的 API 抽象交互。 |
| **核心业务防重** | 1. 员工扩展档案记录对 `emp_id` 是唯一的（数据库 Unique Key 保证，代码校验）。<br>2. 异动单号通过唯一分布式发号器（或 UUID 保证），确保无法重复提交。<br>3. 离职或生效动作必须保证**幂等性**：再次调用相同异动 ID，不应导致状态重复应用或历史重复修改。 |
| **单元测试用例** | 1. `testSaveEducation_HighestFlagReset`：添加新学历并设为最高时，原本最高学历的 `is_highest` 应自动回滚为 `0`。<br>2. `testTransfer_Promotion_Effective`：异动审批通过并到期生效时，系统主表 `sys_employee` 对应的 `post_id` 应成功被覆写，并推送 `EmployeeTransferEvent`。<br>3. `testTransfer_Quit_Effective`：离职异动生效时，员工状态在主表被修改为禁用，合同转为已解约。<br>4. `testTransfer_Regular_ProbationStatusChanged`：转正异动审批通过，扩展表的 `probation_status` 成功变成 `passed`。 |
| **验收** | 在 `code/backend/` 下运行 `mvn -pl oa-hr -am test` 100% 通过。 |

---

### Wave 4: 契约接口与流程接入

#### T5 人事 REST API 与工作流回调
| 字段 | 内容 |
|------|------|
| **目标** | 暴露人事子模块 REST 接口，验证入参、登录上下文获取，并开发 `WorkflowCallback` Handler。当异动流程终定时，自动回调触发 Service 层的生效动作。 |
| **路径** | `code/backend/oa-hr/` (含 `controller`, `callback`), `oa-workflow/` |
| **输入** | T4 核心 Service、T1 API 契约、`WorkflowCallbackDispatcher` 的接入范式。 |
| **输出** | Controller 控制器类（含 `@RequirePermission` 与 `@OperationLog`）、人事工作流回调 Handler、以及 mockMvc 集成测试。 |
| **禁止修改** | 旧 `LeaveApplyController` 等，保证其他已重构模块不受丝毫干扰。 |
| **验收** | `cd code/backend && mvn -pl oa-hr,oa-web -am test` 完美成功。 |

---

### Wave 5: 界面实现与集成验证

#### T6 Web 员工档案与异动页面
| 字段 | 内容 |
|------|------|
| **目标** | 迁移/重构 Web 管理端与员工个人端：员工档案自维护卡片，包含合同、教育、工作经历的标签页。支持管理员发起异动流，和异动轨迹历史。 |
| **路径** | `code/frontend/src/views/hr/personnel/` 目录, `code/frontend/src/api/personnel.ts` |
| **输入** | T5 后端接口契约，Element Plus、Tailwind CSS。 |
| **输出** | 档案详情 Vue 页面、子经历维护弹窗组件、异动流发起面板、API 调用 TS 封装。 |
| **禁止修改** | 不做 monorepo 的无关重构，不污染全局 CSS 与布局。 |
| **验收** | 运行 `cd code/frontend && pnpm typecheck && pnpm build` 顺畅成功。 |

#### T7 移动端档案查看页面
| 字段 | 内容 |
|------|------|
| **目标** | 移动端（H5 + 小程序）接入个人信息卡片，方便员工修改住址、手机号和紧急联系人。以及接收并查看自己的转正、调动审批状态。 |
| **路径** | `code/mobile/src/pages/hr/personnel/` |
| **输入** | T5 后端接口，uni-app 开发标准。 |
| **输出** | 移动端个人扩展档案页面、异动详情组件、移动端 Request 映射。 |
| **禁止修改** | 保持原有移动端公共拦截器、Vuex/Pinia 状态不损坏，不在移动端写大批量管理操作。 |
| **验收** | 运行 `cd code/mobile && pnpm build:h5` 编译打包无警告、无错误。 |

#### T8 闭环集成验证与旧实现安全下线
| 字段 | 内容 |
|------|------|
| **目标** | 进行联合回归测试。验证员工“入职、试用、申请转正、调动、离职”的生命周期，检验系统账户状态、合同状态、工作经历记录的完好。最后下线并物理清理旧的 `oa_emp_archive` 数据库及无用 Java 类。 |
| **路径** | `code/backend/`、`code/frontend/`、`code/mobile/` |
| **输入** | E2E 联合手工与自动化验证方案，旧代码删除清单。 |
| **输出** | 物理删除 `OaEmpArchive.java`、`OaEmpArchiveMapper.java` 及其 XML。移除前端旧档案路由，彻底更新菜单表为新路径。 |
| **禁止修改** | 在回归测试通过、权限链条确保完整前，不允许物理删除任何旧资产。 |
| **验收** | 1. 跑通全人事生命周期，主档案流转顺畅。<br>2. 系统构建（前后移动端）无任何未处理编译错误，平滑完成替换。 |

---

## 9. 最小验收矩阵

| 重构区域 | 验收验收命令 | 预期指标 |
|----------|--------------|----------|
| **HR后端编译** | `cd code/backend && mvn -pl oa-hr -am compile` | 编译成功，无类型、依赖错误 |
| **HR后端测试** | `cd code/backend && mvn -pl oa-hr -am test` | 单元测试 100% 通过 |
| **HR + API入口测试**| `cd code/backend && mvn -pl oa-hr,oa-web -am test` | 模块接口级集成测试成功 |
| **Web 前端构建** | `cd code/frontend && pnpm typecheck && pnpm build` | TS 类型检查无误，生产包构建成功 |
| **移动端 H5 编译** | `cd code/mobile && pnpm build:h5` | 移动端产物成功导出 |

---

## 10. 约束与红线

### 10.1 开发红线

1. **零旧数据兼容设计**：不需要编写任何数据迁移（Migration）SQL，因为重构的核心决策是“不保留旧数据，重构由空库开始”。
2. **唯一与外键语义**：新表的所有关联关系设计，必须严格通过 `emp_id` 与主表 `sys_employee.id` 保持绝对的一致性关系（由逻辑层通过级联和事务强约束，必要时辅助物理约束）。
3. **流程必经**：严禁在非特殊维护（如拼写订正）场景下，由 Controller 绕过 `oa-workflow` 审批引擎直接更新异动的有效状态（`status = EFFECTIVE`）。转正、晋升、离职等变动**必须**走工作流实例状态。
4. **事件隔离**：人事档案子模块发布 `EmployeeTransferEvent` 之后，不允许直接在此 Service 内调用财务薪资或考勤模块的更新，必须由消息中台或下游模块自行订阅该事件进行响应（务实 DDD 解耦红线）。

---

## 11. Claude Code 可执行提示词 (Prompts)

### 11.1 T3: Entity + Mapper + Enum 设计

```text
请执行 员工档案与异动(personnel) 子模块重构的 T3 任务：Entity + Mapper + Enum 阶段。

在开始前，请务必先仔细阅读：
- CLAUDE.md (项目架构及目录原则)
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md (系统总览和重构规范)
- 新人事表 DDL 详尽设计 (第3章及3.2节，包含 hr_employee_ext、hr_transfer、hr_contract、hr_certificate、hr_education、hr_work_experience 6张表)

工作范围：
1. 在 oa-hr 模块的 cn.oa.hr 包下创建 entity、enums、mapper、dto、vo 目录。
2. 规范定义 6 个 Entity 类：
   - HrEmployeeExt (试用期、技能JSON、紧急联系人、婚姻政治身份证信息)
   - HrTransfer (异动单号、各原/新岗位部门、生效期、流程关联、状态)
   - HrContract (合同号、类型、签署期、起止日、附件、状态)
   - HrCertificate (证书名、编号、颁发时间、有效期、附件)
   - HrEducation (学校、专业、学历、入学时间、毕业时间、全日制、最高标志)
   - HrWorkExperience (单位、职务、入职时间、离职时间、业绩)
3. 状态与类型一律使用规范的"字符串枚举"，在 enums 包中创建对应枚举类。
4. 创建对应 Mapper 接口 (继承自 MyBatis-Plus BaseMapper)，并完善 DTO 与 VO。
5. 在 oa-hr 模块下的 src/test/java 中，编写关于这些 Entity 的解析与字段测试。

禁止行为：
- 不用实现 Service 的复杂逻辑。
- 严禁修改 Controller，严禁改动任何前端 (frontend / mobile) 文件。
- 严禁物理删除旧的 oa_emp_archive 任何类。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr -am test
并向我汇报编译测试结果及新增修改的文件清单。
```

### 11.2 T4: Service 核心层设计 (级联与事件驱动)

```text
请执行 员工档案与异动(personnel) 子模块重构的 T4 任务：Service 核心业务逻辑层。

在开始前，请仔细阅读：
- T3 任务中你所建立的 Entity 与 Mapper。
- 边界定义 (第2.1、2.2章) 与核心防重、幂等、单元测试要求 (第8章T4任务表)。

工作范围：
1. 实现以下 5 个 Service 接口及其实现类：
   - HrEmployeeExtService：档案属性查看与修改，员工自服务部分属性修改。
   - HrEducationService / HrWorkExperienceService / HrCertificateService：1:N 经历维护 CRUD。添加或编辑学历为最高学历时，级联修改其他同员工记录。
   - HrContractService：劳动合同 CRUD，维护签署历史。
   - HrTransferService：异动提请。
     * 提供生效逻辑 `makeTransferEffective(Long transferId)`，根据异动类型 (entry/regular/transfer/promotion/demotion/quit) 对应去修改主档案 `sys_employee` 字段 (status, dept_id, post_id) 以及扩展主表 `hr_employee_ext` 的 (work_status, probation_status)。
     * 必须保证此方法的幂等性，不可多次执行导致混乱。
     * 生效后，通过 Spring Context / EventPublisher 发布 `EmployeeTransferEvent` 领域事件。
2. 在单元测试中编写对上述业务能力的充分覆盖。重点测试最高学历级联重置、试用转正、平调、晋升以及离职时主附表的数据一致性。

禁止行为：
- 严禁写 Controller 代码。
- 严禁修改任何 Vue 页面。
- 严禁在 Service 内直连 `wf_*` 底层工作流引擎数据库表，仅做业务状态流转与回调处理。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr -am test
向我汇报单元测试成功数及下一步 T5 接入工作流的注意要点。
```

### 11.3 T5: REST API 控制器与工作流 Callback 处理器

```text
请执行 员工档案与异动(personnel) 子模块重构的 T5 任务：REST API 控制器与工作流 Callback 处理器。

在开始前，请阅读：
- T4 Service 已实现的能力。
- API 契约设计 (第5章) 与 权限码要求。
- 试点重构关于工作流回调 `WorkflowCallbackDispatcher` 的接入方式。

工作范围：
1. 编写 `/api/hr` 开头的 Controller API：
   - HrEmployeeController (自维护档案更新，管理员更新、档案聚合大 VO 查询)
   - HrEducationController、HrWorkExperienceController、HrCertificateController、HrContractController 的 CRUD 入口
   - HrTransferController (发起异动、查询单人/全员历史异动、撤销待生效异动)
   - 必须通过 `@Valid` 限制入参，并使用 `@RequirePermission` 做鉴权。
2. 开发人事异动流程专用回调 Handler `EmployeeTransferWorkflowCallback`。
   - 接入 `IWorkflowCallback` 规范。
   - 监听转正、调动、晋升、离职等事件：
     * `onApproved`：审批成功后，将 `hr_transfer` 改为 `PASSED`。若生效日期等于当天，直接调用 T4 的生效接口；若晚于当天，等待定时器（暂模拟）或直接生效，使状态流转至 `EFFECTIVE`。
     * `onRejected` / `onWithdrawn`：回调将状态转换为驳回/撤回，解冻预扣状态。
3. 编写 mockMvc 测试类，重点测试接口的非法越权访问拒绝及正确状态转换回调。

禁止行为：
- 旧 Controller 决不删除。
- 严禁在此处触碰前端。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr,oa-web -am test
向我汇报测试通过情况，并说明 T6 前端页面可能需要调用的 API 接口清单。
```

---

## 12. 变更日志

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-06-03 | v1.0 | 初始化员工档案与异动(personnel)重构及细化任务波次拆分方案 |
