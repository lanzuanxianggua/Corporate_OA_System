# HR 招聘管理子模块重构任务拆分

> 日期: 2026-06-03  
> 子模块范围: 招聘管理 (recruitment) — 岗位发布、候选人、面试安排与评估、Offer管理、招聘流转轨迹  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> 试点参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`  
> 档案参考: `docs/superpowers/specs/2026-06-02-hr-employee-archive-task-split.md`

---

## 1. 子模块说明与目标

招聘管理是企业引进人才的入口。重构招聘管理子模块的终极目标是实现**从简历投递到确认入职的全生命周期线上化、规范化，并与员工档案、合同管理、工作流及通知系统实现完美闭环**。

重构后的招聘管理子模块应具备以下核心目标：
1. **统一招聘规范**：废弃零散、缺乏流转轨迹的旧模式，基于 5 张核心表构建严密的招聘状态机流转。
2. **多轮面试管理与灵活打分**：支持对候选人发起初试、复试、终审等不同轮次的面试安排，为面试官（系统内员工）指派任务，并支持在线给出 10 分制综合评分及评价意见。
3. **Offer审批与工作流联动**：候选人面试通过后，HR 可发起 Offer 提请，联动工作流引擎（`oa-workflow`）进入多级审批（支持审批通过、驳回、撤回）。
4. **一键确认入职（跨模块闭环的核心价值）**：候选人接受 Offer 并在入职当天办理登记时，HR 点击“确认入职”应实现事务内/级联的操作闭环：
   - 在系统主表 `sys_employee` 自动生成一条新的正式员工记录，基于规则生成全新唯一的工号（`emp_code`），分配默认角色并生成账号。
   - 在员工档案扩展表 `hr_employee_ext` 自动生成对应的主扩展档案记录，状态初始化为“试用在职”（`work_status = 'probation'`, `probation_status = 'probation'`）。
   - 在劳动合同表 `hr_contract` 中自动生成一份关联首签合同记录（处于草稿或待执行态）。
   - 同时将招聘候选人状态标记为 `entry`（已入职），关闭关联的招聘岗位发布计数（`actual_num = actual_num + 1`），完美闭环入职全链路。

---

## 2. 边界定义

### 2.1 包含范围

| 区域 | 内容 | 说明 |
|------|------|------|
| **数据库** | `hr_recruit_job`、`hr_recruit_candidate`、`hr_recruit_interview`、`hr_recruit_offer`、`hr_recruit_pipeline` | 重构这 5 张表，所有外键必须使用 `BIGINT` 且保证数据类型与主表 ID 严格一致。 |
| **后端** | `oa-hr` 模块内新增的 Entity、DTO、VO、Enum、Mapper、Service、Controller、测试 | 包含完整的岗位 CRUD、简历筛选/流转、多轮面试评分、Offer 发起与工作流回调、确认入职联动。 |
| **工作流** | 对接 `oa-workflow` 模块 | 接入 Offer 审批流，通过工作流回调驱动 Offer 状态及候选人流转状态更新。 |
| **Web端** | 岗位发布看板、简历库流转面板、面试安排、Offer审批与录用看板、待入职管理 | 管理端 HR 的日常招聘管理视图。 |
| **移动端** | 面试官打分面板、异动/入职审批详情查看 | 面试官通过手机对候选人快速录入评价意见。 |
| **测试** | 状态机流转集成测试 + 入职联动大事务测试 + 权限控制 Mock 验证 | 保证招聘全流程的一致性。 |

### 2.2 不包含范围

| 不包含 | 原因 |
|--------|------|
| **第三方招聘渠道深度集成** | 涉及复杂的 API 对接和平台收费限制，前期只支持通过后台手动录入和 Excel 批量导入。 |
| **AI 简历解析 (OCR)** | 不进行简历文本自动匹配，附件直接存储 PDF/Word。 |
| **邮件/短信真实发送** | 限制外部依赖，只进行系统内通知、Web 消息及模拟发送记录。 |
| **外部候选人投递门户** | 候选人无法直接注册账号，全部由 HR 在 OA 后台代操作。 |

---

## 3. 核心数据模型

### 3.1 五张表总览

| 表名 | 说明 | 核心职责 |
|------|------|----------|
| `hr_recruit_job` | 招聘岗位发布表 | 记录企业发布的招聘计划，包括需求部门、系统职位、招聘人数、薪资范围、学历经验要求及发布状态。 |
| `hr_recruit_candidate` | 候选人简历表 | 存储候选人基本信息、渠道、当前所处招聘流程中的状态（新投递、筛选通过、面试中、已发Offer、已入职、淘汰）。 |
| `hr_recruit_interview` | 面试安排与评估表 | 记录历次面试轮次、安排时间、面试官员工列表、面试形式（线上/线下）、打分、评语及面试结论。 |
| `hr_recruit_offer` | 录用Offer表 | 存储录用待遇细节（试用期/转正薪资、期限、预计入职日、Offer附件）、工作流审批状态等。 |
| `hr_recruit_pipeline` | 招聘流程流转记录表| 记录候选人在每一个环节流转的轨迹，包含变更前、变更后、操作人、备注及时间，用于流程追踪及漏斗统计。 |

### 3.2 表结构详细设计 (MySQL DDL)

该 DDL 可作为 `code/backend/sql/hr_recruitment_contract.sql` 的直接草案，待测试完备后合入 baseline。

```sql
-- 1. 招聘岗位发布表
DROP TABLE IF EXISTS `hr_recruit_job`;
CREATE TABLE `hr_recruit_job` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位发布ID',
  `job_code`        VARCHAR(64)  NOT NULL                COMMENT '岗位编码(唯一)',
  `job_title`       VARCHAR(100) NOT NULL                COMMENT '岗位名称',
  `dept_id`         BIGINT       NOT NULL                COMMENT '需求部门ID',
  `post_id`         BIGINT       DEFAULT NULL            COMMENT '关联系统岗位ID(sys_post)',
  `job_type`        VARCHAR(32)  NOT NULL DEFAULT 'fulltime' COMMENT '工作性质(fulltime-全职/parttime-兼职/intern-实习/contract-外包)',
  `recruit_num`     INT          NOT NULL DEFAULT 1      COMMENT '计划招聘人数',
  `actual_num`      INT          NOT NULL DEFAULT 0      COMMENT '实际录用人数',
  `work_location`   VARCHAR(100) DEFAULT NULL            COMMENT '工作地点',
  `salary_min`      DECIMAL(10,2) DEFAULT NULL           COMMENT '薪资起(元/月)',
  `salary_max`      DECIMAL(10,2) DEFAULT NULL           COMMENT '薪资止(元/月)',
  `experience_req`  VARCHAR(64)  DEFAULT NULL            COMMENT '工作年限要求',
  `education_req`   VARCHAR(64)  DEFAULT NULL            COMMENT '学历要求(highschool/juniorcollege/bachelor/master/doctor)',
  `job_description` TEXT         DEFAULT NULL            COMMENT '岗位职责描述',
  `job_requirement` TEXT         DEFAULT NULL            COMMENT '岗位要求条件',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'draft' COMMENT '招聘状态(draft-草稿/published-发布中/suspended-暂停/closed-已关闭)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_code` (`job_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招聘岗位发布表';

-- 2. 候选人简历表
DROP TABLE IF EXISTS `hr_recruit_candidate`;
CREATE TABLE `hr_recruit_candidate` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '候选人ID',
  `candidate_name`  VARCHAR(64)  NOT NULL                COMMENT '候选人姓名',
  `gender`          CHAR(1)      DEFAULT NULL            COMMENT '性别(M-男 F-女 U-未知)',
  `phone`           VARCHAR(20)  NOT NULL                COMMENT '联系手机号',
  `email`           VARCHAR(100) DEFAULT NULL            COMMENT '联系邮箱',
  `birth_date`      DATE         DEFAULT NULL            COMMENT '出生日期',
  `education`       VARCHAR(64)  DEFAULT NULL            COMMENT '最高学历(highschool/juniorcollege/bachelor/master/doctor)',
  `major`           VARCHAR(100) DEFAULT NULL            COMMENT '所学专业',
  `school_name`     VARCHAR(128) DEFAULT NULL            COMMENT '毕业学校',
  `experience_years` INT         DEFAULT NULL            COMMENT '工作年限',
  `current_company` VARCHAR(128) DEFAULT NULL            COMMENT '当前工作单位',
  `current_post`    VARCHAR(64)  DEFAULT NULL            COMMENT '当前岗位名称',
  `resume_url`      VARCHAR(512) DEFAULT NULL            COMMENT '简历附件URL',
  `source`          VARCHAR(32)  DEFAULT 'boss'          COMMENT '渠道(job51-前程无忧/liepin-猎聘/zhaopin-智联/boss-直聘/referral-内推/other-其他)',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'new' COMMENT '流转状态(new-新投递/resume_pass-筛选通过/interview-面试中/interview_pass-面试通过/offer-offer审批中/offer_sent-offer已发/offer_accept-接受offer/offer_reject-拒绝offer/entry-已入职/eliminate-已淘汰)',
  `job_id`          BIGINT       NOT NULL                COMMENT '申请招聘岗位ID',
  `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注/HR评价',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='候选人简历表';

-- 3. 面试安排与评估表
DROP TABLE IF EXISTS `hr_recruit_interview`;
CREATE TABLE `hr_recruit_interview` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '面试ID',
  `candidate_id`    BIGINT       NOT NULL                COMMENT '候选人ID',
  `job_id`          BIGINT       NOT NULL                COMMENT '招聘岗位ID',
  `round`           INT          NOT NULL DEFAULT 1      COMMENT '面试轮次(1-初试/2-复试/3-终审)',
  `interview_time`  DATETIME     NOT NULL                COMMENT '面试时间',
  `interviewers`    JSON         NOT NULL                COMMENT '面试官员工ID列表(JSON [1, 2])',
  `interview_type`  VARCHAR(32)  NOT NULL DEFAULT 'online' COMMENT '面试形式(online-线上/offline-线下)',
  `location`        VARCHAR(256) DEFAULT NULL            COMMENT '面试地点或视频会议链接',
  `score`           DECIMAL(4,1) DEFAULT NULL            COMMENT '综合打分(10分制)',
  `evaluation`      TEXT         DEFAULT NULL            COMMENT '面试评语/评价意见',
  `result`          VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '面试结论(pending-待面试/pass-面试通过/fail-面试不通过)',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'scheduled' COMMENT '安排状态(scheduled-已安排/completed-已面试/canceled-已取消)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='面试安排与评估表';

-- 4. 录用Offer表
DROP TABLE IF EXISTS `hr_recruit_offer`;
CREATE TABLE `hr_recruit_offer` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'OfferID',
  `offer_no`            VARCHAR(64)  NOT NULL                COMMENT 'Offer单号(唯一)',
  `candidate_id`        BIGINT       NOT NULL                COMMENT '候选人ID',
  `job_id`              BIGINT       NOT NULL                COMMENT '招聘岗位ID',
  `dept_id`             BIGINT       NOT NULL                COMMENT '录用部门ID',
  `post_id`             BIGINT       NOT NULL                COMMENT '录用职位ID(sys_post)',
  `probation_salary`    DECIMAL(10,2) DEFAULT NULL           COMMENT '试用期薪资(元/月)',
  `regular_salary`      DECIMAL(10,2) DEFAULT NULL           COMMENT '正式薪资(元/月)',
  `probation_period`    INT          DEFAULT 3               COMMENT '试用期限(月)',
  `estimated_entry_date` DATE        NOT NULL                COMMENT '预计入职日期',
  `offer_url`           VARCHAR(512) DEFAULT NULL            COMMENT 'Offer电子档存储URL',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'draft' COMMENT 'Offer状态(draft-草稿/running-审批中/passed-审批通过/rejected-驳回/revoked-撤回/sent-已发送/accepted-已接受/rejected_by_candidate-候选人谢绝/entry-已入职)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '审批流程实例ID',
  `current_task_id`     BIGINT       DEFAULT NULL            COMMENT '当前审批任务ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offer_no` (`offer_no`),
  UNIQUE KEY `uk_candidate_id` (`candidate_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='录用Offer表';

-- 5. 招聘流程流转记录表
DROP TABLE IF EXISTS `hr_recruit_pipeline`;
CREATE TABLE `hr_recruit_pipeline` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流转记录ID',
  `candidate_id`    BIGINT       NOT NULL                COMMENT '候选人ID',
  `job_id`          BIGINT       NOT NULL                COMMENT '招聘岗位ID',
  `from_status`     VARCHAR(32)  NOT NULL                COMMENT '变更前状态',
  `to_status`       VARCHAR(32)  NOT NULL                COMMENT '变更后状态',
  `operator_id`     BIGINT       NOT NULL                COMMENT '操作人ID(sys_employee.id)',
  `operator_name`   VARCHAR(64)  NOT NULL                COMMENT '操作人姓名',
  `reason`          VARCHAR(500) DEFAULT NULL            COMMENT '评语/变更原因说明',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_candidate_id` (`candidate_id`),
  KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招聘流程流转记录表';
```

### 3.3 索引与 EXPLAIN 验收说明

| 查询场景 | 依赖索引 | EXPLAIN 期待结果 |
|----------|----------|------------------|
| 查询岗位唯一编码 | `uk_job_code` | type=const / eq_ref |
| 根据手机号排重或搜索候选人 | `idx_phone` | type=ref / range |
| 按状态分页检索候选人列表 | `idx_status` + `idx_job_id` | type=ref |
| 获取指定候选人的多轮面试记录 | `idx_candidate_id` (在面试表) | type=ref |
| 审批链根据实例ID检索 Offer 详情 | `uk_candidate_id` / `uk_offer_no` | type=const |
| 工作流根据进程查 Offer | `idx_status` (在 Offer 表) | type=ref |

---

## 4. 状态枚举与数据约定

### 4.1 招聘岗位状态 `RecruitJobStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 岗位计划草稿 |
| `PUBLISHED`| 招聘进行中 (已发布) |
| `SUSPENDED`| 招聘暂停 |
| `CLOSED`   | 招聘已结束 (名额招满或计划撤销) |

### 4.2 候选人流转状态 `CandidateStatus`

| Code | 说明 |
|------|------|
| `NEW` | 新投递/新录入 |
| `RESUME_PASS`| 简历筛选通过 |
| `INTERVIEW`  | 面试进行中 |
| `INTERVIEW_PASS`| 所有面试已通关 (可发起 Offer) |
| `OFFER`      | Offer 流程审批中 |
| `OFFER_SENT` | Offer 审批通过，已发候选人 |
| `OFFER_ACCEPT`| 候选人已接受 (待入职) |
| `OFFER_REJECT`| 候选人已拒绝录用 |
| `ENTRY`      | 已经确认入职 (联动转入正式员工) |
| `ELIMINATE`  | 已淘汰 (适用于任何中途环节) |

### 4.3 面试结论 `InterviewResult`

| Code | 说明 |
|------|------|
| `PENDING` | 待面试/未开始 |
| `PASS` | 本轮面试通过 |
| `FAIL` | 本轮面试不通过 |

### 4.4 面试状态 `InterviewStatus`

| Code | 说明 |
|------|------|
| `SCHEDULED` | 已安排面试并通知 |
| `COMPLETED` | 面试官已打分完成 |
| `CANCELED`  | 面试已被取消 |

### 4.5 录用 Offer 状态 `OfferStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 录用待遇草稿 |
| `RUNNING` | Offer 工作流审批中 |
| `PASSED` | 审批已通过 (待发) |
| `REJECTED` | 审批被驳回 |
| `REVOKED` | 申请人撤回 |
| `SENT` | 电子版 Offer 已发候选人 |
| `ACCEPTED` | 候选人接受 Offer |
| `REJECTED_BY_CANDIDATE` | 候选人谢绝 Offer |
| `ENTRY` | 已成功入职 |

---

## 5. API 契约

### 5.1 统一前缀与规范

- REST API 根路径：`/api/hr/recruitment`
- 响应格式统一：`{"code": 0, "message": "操作成功", "data": ...}`
- 操作权限码前缀：`hr:recruit:`

### 5.2 核心 REST API 清单

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| **GET** | `/api/hr/recruitment/jobs` | `hr:recruit:job:list` | 分页检索招聘岗位发布列表 |
| **POST**| `/api/hr/recruitment/jobs` | `hr:recruit:job:publish` | 发布/创建新招聘岗位 |
| **PUT** | `/api/hr/recruitment/jobs/{id}`| `hr:recruit:job:update` | 编辑岗位及调整名额要求 |
| **POST**| `/api/hr/recruitment/candidates`| `hr:recruit:candidate:create`| 录入/导入候选人简历 |
| **GET** | `/api/hr/recruitment/candidates`| `hr:recruit:candidate:list`| 招聘看板候选人列表 (按状态聚合/检索) |
| **PUT** | `/api/hr/recruitment/candidates/{id}/status`| `hr:recruit:candidate:status`| 手动淘汰候选人或变更简单状态 |
| **POST**| `/api/hr/recruitment/interviews`| `hr:recruit:interview:schedule`| 安排面试 (指定时间、面试官JSON、轮次) |
| **PUT** | `/api/hr/recruitment/interviews/{id}/evaluation`| `hr:recruit:interview:evaluate`| 面试官对候选人进行打分和录入评价 |
| **POST**| `/api/hr/recruitment/offers` | `hr:recruit:offer:create` | 为通关候选人发起录用待遇 DTO (创建并启动审批流) |
| **GET** | `/api/hr/recruitment/offers/{id}`| `hr:recruit:offer:detail` | 查询录用待遇明细与工作流审批当前进程 |
| **POST**| `/api/hr/recruitment/offers/{id}/actions/send`| `hr:recruit:offer:send` | 审批通过的 Offer 正式标记发送给候选人 |
| **POST**| `/api/hr/recruitment/offers/{id}/actions/reply`| `hr:recruit:offer:reply` | 登记候选人反馈 (接受/谢绝录用) |
| **POST**| `/api/hr/recruitment/offers/{id}/actions/entry`| `hr:recruit:offer:entry` | **确认入职登记**：触发大事务，级联生成正式员工、扩展档案及合同草稿 |
| **GET** | `/api/hr/recruitment/candidates/{id}/pipeline`| `hr:recruit:pipeline:view`| 获取指定候选人的流转历史轨迹 |

---

## 6. DTO 与 VO 设计

### 6.1 `HrRecruitJobCreateDTO`
| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `jobTitle` | String | 必填，不超过100字 | 岗位名称 |
| `deptId` | Long | 必填 | 需求部门 ID |
| `postId` | Long | 可选 | 关联的系统岗位 (sys_post) |
| `jobType` | String | 必填，枚举值 | fulltime/parttime/intern/contract |
| `recruitNum` | Integer | 必填，大于0 | 计划招聘人数 |
| `workLocation` | String | 可选 | 工作地点 |
| `salaryMin` | BigDecimal| 可选，大于0 | 薪资下限 |
| `salaryMax` | BigDecimal| 可选，且必须大于或等于 `salaryMin` | 薪资上限 |
| `experienceReq`| String | 可选 | 工作年限要求 |
| `educationReq` | String | 必填，枚举值 | 最低学历要求 |
| `jobDescription`| String | 可选 | 岗位职责 |
| `jobRequirement`| String | 可选 | 任职要求 |

### 6.2 `HrCandidateCreateDTO`
| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `candidateName`| String | 必填，不超过64字 | 候选人姓名 |
| `gender` | String | 必填，单字枚举 (M/F/U) | 性别 |
| `phone` | String | 必填，国内手机号码正则 | 联系手机号 |
| `email` | String | 可选，合法的邮件格式 | 邮箱 |
| `birthDate` | LocalDate| 可选 | 出生日期 |
| `education` | String | 必填，学历枚举 | 最高学历 |
| `major` | String | 可选 | 专业 |
| `schoolName` | String | 可选 | 毕业院校 |
| `experienceYears`| Integer| 可选，不低于0 | 经验年数 |
| `resumeUrl` | String | 可选，URL 格式 | 简历文件地址 |
| `source` | String | 必填，渠道枚举 | 渠道来源 |
| `jobId` | Long | 必填 | 申请招聘的岗位 ID |

### 6.3 `HrInterviewScheduleDTO`
| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `candidateId` | Long | 必填 | 候选人 ID |
| `jobId` | Long | 必填 | 招聘岗位 ID |
| `round` | Integer | 必填，1-10 范围 | 面试轮次 |
| `interviewTime`| LocalDateTime| 必填，必须晚于当前系统时间 | 面试时间 |
| `interviewers` | List<Long>| 必填，最少指派1名面试官 | 面试官员工 ID 集合 |
| `interviewType`| String | 必填，枚举 | online/offline |
| `location` | String | 可选，不超过256字 | 地址或线上腾讯会议、Zoom链接 |

### 6.4 `HrInterviewEvaluationDTO`
| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `score` | BigDecimal| 必填，0.0 到 10.0 之间，支持一位小数 | 综合评分 |
| `evaluation` | String | 必填，不超过1000字 | 综合评价意见 |
| `result` | String | 必填，枚举 | pass-本轮通过 / fail-本轮淘汰 |

### 6.5 `HrOfferCreateDTO`
| 字段 | 类型 | 校验规则 | 说明 |
|------|------|----------|------|
| `candidateId` | Long | 必填 | 候选人 ID |
| `jobId` | Long | 必填 | 申请岗位 ID |
| `deptId` | Long | 必填 | 录用部门 ID |
| `postId` | Long | 必填 | 录用系统职位 ID (sys_post) |
| `probationSalary`| BigDecimal| 可选，大于0 | 试用期薪水 |
| `regularSalary` | BigDecimal| 必填，大于0 | 转正后薪水 |
| `probationPeriod`| Integer | 可选，默认 3，范围 0-6 | 试用期限(月) |
| `estimatedEntryDate`| LocalDate| 必填，不能早于当前日期 | 期望入职日期 |
| `offerUrl` | String | 可选 | Offer 电子版附件 |

---

## 7. 任务波次拆分 (Wave 1 至 Wave 5)

```
Wave 1: 契约基线
├── T1: 数据结构契约与 SQL 落地 (hr_recruitment_contract.sql)
└── T2: 旧有业务及残留影响评估
Wave 2: 后端核心
├── T3: 招聘五表 Entity / Enum / Mapper 自动生成与基础单元测试
├── T4: 招聘生命周期 Service (核心天职：候选人状态流转与入职级联大事务)
└── T5: 招聘 REST API 控制层开发 (Controller + Knife4j / Jakarta 验证)
Wave 3: 流程与消息
├── T6: Offer 审批流 Callback Handler 处理器 (对接 IWorkflowCallback)
└── T7: 面试官派单待办与系统内通知推送
Wave 4: 前端适配
├── T8: Web 招聘管理后台组件迁移 (看板流转卡片、岗位/简历维护 Vue)
└── T9: 移动端面试官便捷打分与评估页面 H5 接入
Wave 5: 验证收尾
├── T10: 全流程入职回归集成测试 (从简历到新员工生成)
└── T11: 旧入口清理下线清单与彻底迁移
```

### Wave 1: 契约与基线

#### T1 数据库与 API 契约
| 字段 | 内容 |
|------|------|
| **目标** | 编写招聘模块 5 张表的 SQL DDL 并确保字段、主键、索引、delFlag、COMMENT 标准完备；在 Split 文档确立接口规范。 |
| **路径** | `code/backend/sql/` |
| **输入** | 本 Split 文档、项目主库 DDL 规范。 |
| **输出** | 新增 `code/backend/sql/hr_recruitment_contract.sql` 草案文件。 |
| **禁止修改** | 不写 Java 逻辑代码。 |
| **验收** | 执行 SQL 在测试 H2/MySQL 库成功运行，索引 EXPLAIN 契约校验合格。 |

#### T2 旧实现影响分析
| 字段 | 内容 |
|------|------|
| **目标** | 调查原 OA 系统中是否存在简陋的“岗位招聘”、“简历登记”字段，排除由于本次重构导致的其他子系统编译或逻辑报错风险。 |
| **路径** | `code/backend/`、`code/frontend/` |
| **输入** | 全局 Grep 检索：`recruit`, `candidate`, `interview`, `offer`。 |
| **输出** | 残留废弃清单。若无任何残留，则声明为“100%纯新增能力”。 |
| **禁止修改** | 不做任何实质性的物理删除。 |
| **验收** | 分析报告产出。 |

---

### Wave 2: 后端核心

#### T3 招聘模块实体与 Mapper
| 字段 | 内容 |
|------|------|
| **目标** | 基于 MyBatis-Plus，在 `oa-hr` 模块的 `cn.oa.hr` 包中，生成 5 个实体类、DTO、VO、Mapper 接口及对应的 XML。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/` (含 `entity`, `mapper`, `dto`, `vo`, `enums`) |
| **输入** | T1 的建表 SQL、MyBatis-Plus 开发规范。 |
| **输出** | 5个 Entity、5个 Mapper 接口及 XML 文件。全部状态、轮次等强制为字符串枚举。 |
| **禁止修改** | 绝不编写具体的 Service 计算。 |
| **验收** | 编写 JUnit 解析与实体对应校验。运行 `cd code/backend && mvn -pl oa-hr -am test` 通过。 |

#### T4 招聘业务 Service (核心关键点)
| 字段 | 内容 |
|------|------|
| **目标** | 实现招聘各阶段的生命周期业务逻辑，包含简历流转、状态推进防冲突、面试打分及最为核心的“一键确认入职级联事务”。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/service/` |
| **输入** | T3 Mapper、其他模块 Service 接口 (如员工主表修改 `EmployeeService`、扩展档案修改 `HrEmployeeExtService`、合同修改 `HrContractService`)。 |
| **输出** | 5个 Service 接口及其实现类。一键入职需支持 Spring `@Transactional` 大事务保护，并生成 `CandidateEntryEvent`（包含新入员工 ID 等）。 |
| **核心业务防重** | 1. 简历手机号查重：不允许在同一招聘岗位下，重复录入相同手机号的候选人。<br>2. 幂等入职操作：已经办理过入职的 Offer（状态变更为 `entry`）绝不接受二次点击确认入职，避免 sys_employee 发生重复记录插入。<br>3. 面试官评分幂等：一名面试官对特定轮次的面试打分提交后，状态改为 completed，不可重复覆盖评分。 |
| **单元测试用例** | 1. `testCreateCandidate_DuplicatePhoneOnSameJob`：在同一个岗位下，传入已存在的手机号，验证是否正确抛出“候选人已投递该职位”的业务异常。<br>2. `testInterview_SubmitEvaluation_StatusChanged`：面试官打分通过后，验证面试单状态变为 completed，候选人状态相应推进到 interview_pass（或下一轮 interview）。<br>3. `testOffer_ConfirmEntry_CascadedCreation`：测试一键确认入职核心事务。调用 `confirmCandidateEntry(offerId)` 后：<br>&nbsp;&nbsp;&nbsp;&nbsp;- `sys_employee` 应成功被 insert，且生成规则合法的工号（如 EMP2026xxxx）。<br>&nbsp;&nbsp;&nbsp;&nbsp;- `hr_employee_ext` 应自动 insert，试用状态、工作状态分别被设为 probation。<br>&nbsp;&nbsp;&nbsp;&nbsp;- `hr_contract` 自动生成一条对应的劳动合同草稿，候选人状态最终正确变更为 `entry`。 |
| **验收** | `cd code/backend && mvn -pl oa-hr -am test` 100% 成功。 |

#### T5 招聘 REST API 控制器
| 字段 | 内容 |
|------|------|
| **目标** | 为前端组件开发提供完备、合法的 REST 接口。获取登录上下文、进行权限拦截，保证数据安全。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/controller/` |
| **输入** | T4 核心 Service、T1 设计的 API 契约与操作权限码。 |
| **输出** | Controller 控制器，均应用 `@RequirePermission`、`@Valid`、`@OperationLog`。 |
| **禁止修改** | 绝不修改其他已上线模块的 Controller 接口。 |
| **验收** | 控制器 MockMvc 测试编写并执行。运行 `cd code/backend && mvn -pl oa-hr,oa-web -am test` 全盘成功。 |

---

### Wave 3: 工作流与消息联动

#### T6 Offer 工作流回调接入
| 字段 | 内容 |
|------|------|
| **目标** | 候选人通过面试后，HR 发起 Offer 审批流程。接入 `IWorkflowCallback` 规范，实现 Offer 审批的回调处理。 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/callback/` |
| **输入** | `oa-workflow` 回调分发拦截、T4 OfferService。 |
| **输出** | `OfferWorkflowCallbackHandler`。<br>- `onApproved`：Offer 审批通过，自动将 Offer 状态改为 `passed`，候选人状态改为 `offer`（待发）。<br>- `onRejected` / `onWithdrawn`：自动将 Offer 状态改为拒绝或撤回，候选人状态退回到 `interview_pass`（或中途淘汰状态）。 |
| **禁止修改** | 禁止修改 `oa-workflow` 底层的引擎状态流转算法，只挂载接口。 |
| **验收** | 集成 Mock 工作流引擎发出完成事件，验证 Offer 的状态变更无误。 |

#### T7 面试官派单待办与系统通知
| 字段 | 内容 |
|------|------|
| **目标** | 面试安排提交时，给 JSON 配置中的面试官员工推送系统通知与 Web Socket 弹窗；Offer 最终被候选人接受或入职时，通知需求部门负责人。 |
| **路径** | `code/backend/oa-hr/`、`oa-message/` |
| **输入** | 消息中台发送接口、WebSocket 消息发送逻辑。 |
| **输出** | 面试指派事件派发器及通知服务订阅。 |
| **禁止修改** | 不对底层 WebSocket 核心连接池做任何性能重构。 |
| **验收** | 运行测试，验证消息记录已正确写入 `oa_message` 数据库中。 |

---

### Wave 4: 前端实现与页面适配

#### T8 Web 招聘管理端页面
| 字段 | 内容 |
|------|------|
| **目标** | 迁移与实现 Web 端 HR 管理视图，包含精美的“招聘候选人流转漏斗/看板” (利用 Element Plus 的 Drag-and-Drop 或常规步骤流转)、岗位管理页、面试安排面板、一键办理入职表单。 |
| **路径** | `code/frontend/src/views/hr/recruitment/` 目录, `code/frontend/src/api/recruitment.ts` |
| **输入** | T5 REST 接口契约、Element Plus。 |
| **输出** | 看板 Vue 页面、流转状态更新封装、API 统一 TS typed 封装。 |
| **禁止修改** | 不变动全局样式或路由骨架。 |
| **验收** | `cd code/frontend && pnpm typecheck && pnpm build` 100% 成功。 |

#### T9 移动端面试官评估 H5 页面
| 字段 | 内容 |
|------|------|
| **目标** | 面试官多为业务部门主管，需要支持在移动端快速打开“面试评估”待办，对被指派的候选人进行 10 分制打分、填写专业评语并提交。 |
| **路径** | `code/mobile/src/pages/hr/recruitment/` |
| **输入** | 移动端 `uni.request` 封装、uni-app 组件。 |
| **输出** | 移动端打分评估页面、移动端 API 路由。 |
| **禁止修改** | 保持移动端路由与权限拦截器的兼容不破坏。 |
| **验收** | `cd code/mobile && pnpm build:h5` 顺畅无警告编译通过。 |

---

### Wave 5: 验证与下线准备

#### T10 招聘闭环端到端集成测试
| 字段 | 内容 |
|------|------|
| **目标** | 进行全链条回归测试。涵盖：发布岗位 -> 录入候选人 -> 安排初试并打分通过 -> 安排复试并打分通过 -> 提请 Offer -> 模拟工作流回调通过 -> 登记接受 Offer -> 点击确认入职大事务生成新员工。 |
| **路径** | `code/backend/`、`code/frontend/` |
| **输入** | E2E 联合测试场景设计。 |
| **输出** | `RecruitmentE2EIntegrationTest.java` 覆盖全链路。 |
| **禁止修改** | 在测试 100% 通过前，不允许下发生产部署。 |
| **验收** | 后端测试全部通过，控制台无报错。 |

#### T11 旧入口彻底下线清单
| 字段 | 内容 |
|------|------|
| **目标** | 对可能存在的原简历表、旧 API 入口或过期页面进行彻底排查并从代码中物理移除，升级菜单表路由路径。 |
| **路径** | `code/backend/`、`code/frontend/` |
| **输入** | T2 旧残留分析报告。 |
| **输出** | 物理删除旧代码；提交 SQL 清理物理旧表结构（如有）；提供无缝平滑替换的变更清单。 |
| **禁止修改** | 确保不破坏除招聘外的任何 HR 其他子模块。 |
| **验收** | 系统整体打包构建测试完美通过。 |

---

## 8. 推荐执行顺序

```
Wave 1 (T1, T2)   ──>   Wave 2 (T3 ──> T4 ──> T5)   ──>   Wave 3 (T6, T7)   ──>   Wave 4 (T8, T9)   ──>   Wave 5 (T10 ──> T11)
```

在 T1 契约建表及 T2 影响分析完全锁定并由团队评审通过前，不允许动手进行 T3/T4 的 Java 编码。在 T5 API 接口没有全面通过 JUnit 验收前，禁止进行 T8/T9 前端大面积逻辑对接。

---

## 9. 最小验收矩阵

| 重构区域 | 验收验收命令 | 预期指标 |
|----------|--------------|----------|
| **HR后端编译** | `cd code/backend && mvn -pl oa-hr -am compile` | 编译成功，无任何包、类型缺失 |
| **HR后端测试** | `cd code/backend && mvn -pl oa-hr -am test` | 所有单元测试 100% 通过 (不低于40个招聘场景用例) |
| **API与集成级测试**| `cd code/backend && mvn -pl oa-hr,oa-web -am test` | API 级别集成测试 100% 成功 |
| **Web 前端构建** | `cd code/frontend && pnpm typecheck && pnpm build` | TS 类型检查无误，生产包构建成功 |
| **移动端 H5 编译** | `cd code/mobile && pnpm build:h5` | 移动端产物成功导出 |

---

## 10. 约束与红线

### 10.1 开发红线

1. **唯一与排重保护**：候选人的手机号、Offer的Offer单号、岗位的岗位发布编码属于天然强校验，必须通过数据库的 `UNIQUE KEY` 予以兜底，同时在 Service 层写明防御性代码。
2. **入职大事务保护**：一键入职（T4 阶段核心天职）由于跨越 `sys_employee`、`hr_employee_ext`、`hr_contract`、`hr_recruit_candidate` 及 `hr_recruit_job` 5 张表的操作，**必须**在同一个 `@Transactional(rollbackFor = Exception.class)` 数据库事务中运行。任何一步失败，必须全盘回滚，禁止产生脏员工数据或断链候选人。
3. **流程不可直接篡改**：严禁在 Controller 层提供“一键将 Offer 审批通过”的绕过接口。除测试环境外，Offer 的 `status = PASSED` 必须由 `oa-workflow` 审批回调 Handler 更改。
4. **事件通信 DDD 隔离**：招聘子模块入职大事务成功后，应通过 `ApplicationEventPublisher` 发布 `CandidateEntryEvent`。如果后续考勤、考勤组或培训系统需要对其进行初始化，应当通过监听该事件实现，不允许在招聘 Service 直接依赖注入并强调用其他模块代码。

---

## 11. Claude Code 可执行提示词 (Prompts)

### 11.1 T3: Entity + Mapper + Enum 设计阶段

```text
请执行 招聘管理(recruitment) 子模块重构的 T3 任务：Entity + Mapper + Enum 阶段。

在开始前，请务必仔细阅读：
- CLAUDE.md (项目架构、开发规范与目录原则)
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md (重构规范)
- 新招聘五表 DDL 设计：docs/superpowers/specs/2026-06-02-hr-recruitment-task-split.md (第3.2节，包含 hr_recruit_job, hr_recruit_candidate, hr_recruit_interview, hr_recruit_offer, hr_recruit_pipeline)

工作范围：
1. 在 oa-hr 模块的 cn.oa.hr 包下创建 entity, mapper, dto, vo, enums 目录。
2. 规范定义 5 个 Entity 类：
   - HrRecruitJob (岗位发布计划、各部门职位关联、薪资经验学历、招聘状态)
   - HrRecruitCandidate (候选人基本档案、手机邮箱学历学校、来源渠道、当前流转状态)
   - HrRecruitInterview (面试轮次、安排时间、面试官JSON、线上/线下、打分、评语结论)
   - HrRecruitOffer (Offer单号、预计入职日期、各试用转正薪资、流程关联、状态)
   - HrRecruitPipeline (流转前状态、流转后状态、操作人ID及姓名、原因评语、时间)
3. 状态、类型、面试形式及结论等一律在 enums 包中创建对应规范的“字符串枚举”类。
4. 创建对应 Mapper 接口 (继承自 MyBatis-Plus BaseMapper)，并完善 DTO 与 VO（完全对齐第6章 DTO/VO 定义）。
5. 在 oa-hr 模块下的 src/test/java 中，编写关于这些 Entity 与 MyBatis-Plus 映射关联的基础 JUnit 解析测试。

禁止行为：
- 不用编写任何 Service 计算。
- 绝不修改 Controller，不修改任何前端 (frontend / mobile) 文件。
- 绝不修改正式 SQL baseline 生产库，不删除已有 hr_* 类。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr -am test
并向我汇报编译测试结果及新增修改的文件清单。
```

### 11.2 T4: Service 核心业务逻辑层 (状态流转与入职大事务)

```text
请执行 招聘管理(recruitment) 子模块重构的 T4 任务：Service 核心业务逻辑层。

在开始前，请仔细阅读：
- T3 任务中你所建立的招聘 5 表 Entity 与 Mapper。
- 边界定义 (第2.1、2.2节) 与核心防重、事务保护及单元测试用例设计 (第7章 T4 任务表)。

工作范围：
1. 实现以下 5 个 Service 接口及其实现类：
   - HrRecruitJobService：岗位计划 CRUD，发布状态控制，更新录用名额计数。
   - HrRecruitCandidateService：候选人筛选流转、淘汰、按岗位及手机号防重添加，及获取流转 pipeline 轨迹。
   - HrRecruitInterviewService：面试安排（校验时间及指派面试官不为空）、面试官打分、面试结论录入。
   - HrRecruitOfferService：提请录用 Offer、候选人意见答复、以及最为核心的：
     * **一键确认入职办理大事务** `confirmCandidateEntry(Long offerId)`:
       a) 在 sys_employee 中 insert 生成正式员工 (含自动生成编号，如 EMP+年+月+自增等规范，且保证唯一性)。
       b) 在 hr_employee_ext 扩展表自动初始化对应试用档案记录 (状态设为 probation)。
       c) 在 hr_contract 中 insert 一份对应的劳动合同草稿 (同步预计入职、薪资试用期等参数)。
       d) 更新当前 offer 的状态为 entry，更新候选人 candidate 的状态为 entry。
       e) 发布 CandidateEntryEvent 领域事件供下游消费。
       f) 此操作必须使用 @Transactional 强事务保护，保证要么全部成功，要么全盘回滚。
   - HrRecruitPipelineService：对每一次状态迁移，均要通过此服务向 hr_recruit_pipeline 自动 insert 轨迹记录。
2. 编写充分的单元测试覆盖：
   - 同岗位相同手机号防投递测试。
   - 面试评价提交后状态自动前推至 interview_pass / 淘汰测试。
   - 一键确认入职大事务联合测试 (重点断言员工表、扩展表、合同表、Offer表的数据级联一致性)。

禁止行为：
- 严禁写任何 REST Controller 层代码。
- 严禁改动任何 Vue 页面。
- 不得在 Service 内部直连 `wf_*` 底层工作流引擎数据库表，只能对外通过事件/接口抽象沟通。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr -am test
并向我汇报测试通过情况以及对后续 T5 接口控制层的建议。
```

### 11.3 T5: REST API 控制层与工作流 Callback 处理器开发

```text
请执行 招聘管理(recruitment) 子模块重构的 T5 任务：REST API 控制层与工作流 Callback 处理器开发。

在开始前，请阅读：
- T4 Service 已实现的基础业务能力。
- API 契约设计 (第5.2节) 与 权限码/统一响应 R 规范。
- 工作流回调 `WorkflowCallbackDispatcher` 的注册规范。

工作范围：
1. 编写 `/api/hr/recruitment` 开头的 REST Controller (可以分别创建岗位、候选人、面试、Offer 控制器类)：
   - 包含岗位 CRUD、候选人筛选与流转看板检索、面试安排、面试评估录入、Offer 详情。
   - DTO 参数上必须标注 @Valid，并在 Controller 方法入参前添加 @Valid。
   - 使用 WebUtil / UserContext 获取操作人信息，并采用 @RequirePermission(权限码) 拦截未授权行为。
   - 返回格式必须统一为 R<T> 或项目中约定的返回。
2. 编写人事 Offer 流程专用工作流回调处理器 `OfferWorkflowCallbackHandler`。
   - 实现工作流引擎定义的回调契约。
   - 监听对应的审批通过/驳回/撤销事件：
     * `onApproved`：回调 OfferService 将该录用待遇改为已通过 (passed)，修改候选人为待发 offer (offer_sent / offer)。
     * `onRejected` / `onWithdrawn`：回调将 offer 改为已驳回/已撤销，候选人退回到筛选通过或面试通过待重新提请状态。
3. 编写 Controller 的 MockMvc 单元测试，重点测试未授权直接拒绝和正确入参校验通过。

禁止行为：
- 原有的 Controller 文件决不修改或删除。
- 绝不触碰前端 Vue 或 H5 代码。

完成后，在 code/backend 目录运行：
mvn -pl oa-hr,oa-web -am test
并向我汇报集成测试结果。
```

### 11.4 T6-T7: Web 前端管理看板与 Mobile H5 打分页面开发 (聚合)

```text
请执行 招聘管理(recruitment) 子模块重构的 T6/T7 任务：Web 前端与 Mobile 页面开发阶段。

在开始前，请确保：
- 后端 T5 所有 REST 接口已经在 oa-web 中启动且测试通过。
- 阅读前端 TS 类型规则、Element Plus 的使用规范。

工作范围：
1. 编写 Web 管理端组件 (code/frontend/src/views/hr/recruitment/)：
   - **岗位发布页**：发布招聘计划、部门岗位要求，查看名额与已招人数。
   - **简历库流转看板页**：可以采用精美的“招聘漏斗看板”形式 (列出新投递、筛选过、面试中、通过、Offer中、已入职等列，支持 HR 将候选人拖拽或快捷按钮前推，并能录入淘汰、指派面试、发起Offer弹窗)。
   - **面试面板组件**：选择面试官员工、指定时间与视频链接。
   - **确认入职办理页**：点击确认入职时弹出确认框，输入工号或自动默认，确认后调用 entry 接口成功后提示并跳转到员工档案页面。
2. 编写 Mobile 端页面 (code/mobile/src/pages/hr/recruitment/)：
   - 针对面试官角色：可在移动端“待办审批/工作台”中，打开被指派给自己的面试评估打分表单，输入 10 分制评分、选择 pass/fail 结论、输入专业面试评语并提交。
3. 统一在前端和移动端封装 API 请求至 `recruitment.ts` 并在相关类型声明文件中定义 typed DTO/VO。

禁止行为：
- 不重构全局 CSS 结构。
- 保持原移动端的 Bearer JWT Token 请求头拦截体系。

验收方式：
在前端运行：cd code/frontend && pnpm typecheck && pnpm build
在移动端运行：cd code/mobile && pnpm build:h5
并向我汇报前后端整体打包无错、无警告的结果。
```

---

## 12. 变更日志

| 日期 | 版本 | 变更人 | 变更内容 |
|------|------|--------|----------|
| 2026-06-03 | v1.0 | Claude | 初始化【招聘管理】(recruitment) 子模块的重构与任务拆分详尽方案，支持入职大事务级联联动 |
