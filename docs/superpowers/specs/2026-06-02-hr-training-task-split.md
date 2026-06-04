# HR 培训管理子模块重构任务拆分

> 日期: 2026-06-03  
> 子模块范围: 培训管理 (training) — 培训课程、培训计划、培训班级、学员报名、培训通过与学分点亮技能树  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> 试点参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`  
> 绩效参考: `docs/superpowers/specs/2026-06-02-hr-performance-task-split.md`

---

## 1. 子模块说明与目标

培训管理是企业人力资源开发（HRD）的核心环节。本子模块的重构目标是**将传统的、孤立的培训登记，升级为与“员工入职”和“人事档案技能树”深度联动、由工作流驱动的全周期数字化内训系统**。

重构后的培训管理子模块应实现以下两个核心特色闭环业务场景：

1. **新员工入职强制触发岗前培训**：
   - 监听员工入职领域事件（由招聘模块的“确认入职”或人事异动模块的“入职异动生效”发布 `EmployeeEntryEvent`）。
   - 监听到事件后，系统自动检索针对该员工部门、岗位的、类型为“岗前培训”（`plan_type = 'new_employee'`）且为强制必修（`is_mandatory = '1'`）的有效培训计划。
   - 自动匹配或创建适用的培训班级（`hr_train_session`），静默生成报名记录（`hr_train_enroll`），其报名类型设为“强制指派”（`enroll_type = 'mandatory'`），状态直接为“已报名”（`status = 'APPROVED'`），无需人工审批，实现入职即入训的自动化管理。

2. **考核通过点亮档案技能树**：
   - 培训班级结课后，讲师或管理员对学员录入考核成绩（百分制，通过分可由课程或班级自定义，默认为60分）。
   - 成绩录入后，若考核结果为“通过”（`result_status = 'pass'`），系统在授予该课程对应学分的同时，自动读取该课程所关联的核心技能标签（`hr_train_course.major_skills`，JSON 格式，例如 `["Java", "Spring Boot"]`）。
   - 系统将以原子操作、去重追加的形式，将这些技能标签点亮并同步到员工扩展档案表（`hr_employee_ext.skills`）中，同时将该考核记录的技能点亮状态置为“已点亮”（`is_skills_unlocked = '1'`），打通“培训 -> 考核 -> 档案技能沉淀”的人才发展闭环。

---

## 2. 边界定义

### 2.1 包含范围

| 区域 | 内容 | 说明 |
|------|------|------|
| **数据库** | `hr_train_course`、`hr_train_plan`、`hr_train_session`、`hr_train_enroll`、`hr_train_record` | 重构与新建 5 张核心培训表。逻辑删除使用 `del_flag` (0=有效, 1=删除)。 |
| **后端** | `oa-hr` 模块内新增的 Entity、DTO、VO、Enum、Mapper、Service、Controller、测试 | 完整的培训课程 CRUD、培训计划/班级管理、自主报名及工作流审批、扫码签到、批量录入成绩。 |
| **工作流** | 计划审批工作流 + 员工自主报名审批工作流 | 计划状态（`DRAFT` -> `PASSED`）、报名状态（`DRAFT` -> `APPROVED`）通过工作流回调驱动。 |
| **消息/待办** | WebSocket 实时推送 + 系统消息写入 + 待办产生 | 审批阶段通知、报名成功通知、班级开课提醒，及强制岗前培训生成待办。 |
| **Web端** | 课程库配置、计划与班级看板、学员报名管理、扫码签到看板、成绩录入与结课、全员学分与技能报表 | 管理端功能，提供完整的可视化管理及统计分析。 |
| **移动端** | 可报名培训展示、自主报名申请、扫码签到面板、我的培训成绩与学分列表 | 员工自助端（uni-app H5），侧重报名、扫码与自我发展数据查询。 |
| **测试** | 状态机流转集成测试 + 入职事件联动大事务测试 + 技能树原子追加单元测试 | 确保高并发报名扣额、追加技能无数据污染。 |

### 2.2 不包含范围

| 不包含 | 原因 |
|--------|------|
| **在线视频流转码与防盗链** | 避免引入过多流媒体中间件。视频、课件直接存放在 OSS，前端提供基础的 `<video>` 简易播放，不记录视频精确到秒级的播放进度。 |
| **复杂在线题库与考试系统** | 暂不引入题库、智能组卷、错题本等复杂逻辑。系统仅提供考核结果的分数及状态录入接口，具体考试在第三方系统或线下进行。 |
| **外部培训机构与合同收付款** | 涉及到外部发票、付款、预算扣减等财务审批流程，不属于培训核心业务闭环，相关预算仅做纯金额文本记录。 |

---

## 3. 核心数据模型

### 3.1 五张表总览

| 表名 | 说明 | 核心职责 |
|------|------|----------|
| `hr_train_course` | 培训课程表 | 定义课程基础属性、授课类型、自带学分、关联的核心技能标签等。 |
| `hr_train_plan` | 培训计划表 | 管理企业的年度/季度培训项目计划，控制预算和覆盖目标，通过工作流审批。 |
| `hr_train_session` | 培训班级/期数表 | 隶属于培训计划与课程的实例化，管理讲师、开闭课时间、最大容纳人数、已报名人数、扫码签到码。 |
| `hr_train_enroll` | 学员报名表 | 记录员工（学员）的报名申请、审核流转状态、扫码签到记录等，实现高并发报名控制。 |
| `hr_train_record` | 培训通过与学分记录表 | 记录最终的考核成绩、通过状态、授予的学分，驱动技能树的点亮。 |

### 3.2 表结构详细设计 (MySQL DDL)

```sql
-- 1. 培训课程表
DROP TABLE IF EXISTS `hr_train_course`;
CREATE TABLE `hr_train_course` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_code`     VARCHAR(64)  NOT NULL                COMMENT '课程编码(唯一)',
  `course_name`     VARCHAR(200) NOT NULL                COMMENT '课程名称',
  `course_type`     VARCHAR(32)  NOT NULL DEFAULT 'online' COMMENT '课程类型(online-线上/offline-线下/external-外部培训)',
  `major_skills`    JSON         DEFAULT NULL            COMMENT '关联核心技能JSON数组(例如: ["Java","Spring Boot"])',
  `credit`          DECIMAL(5,2) NOT NULL DEFAULT 0.00   COMMENT '认定学分',
  `duration_hours`  DECIMAL(5,1) NOT NULL DEFAULT 0.0    COMMENT '课时(小时)',
  `course_url`      VARCHAR(512) DEFAULT NULL            COMMENT '线上课程学习地址(视频/课件链接)',
  `description`     VARCHAR(1000) DEFAULT NULL           COMMENT '课程描述/大纲',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_course_type` (`course_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训课程表';

-- 2. 培训计划表
DROP TABLE IF EXISTS `hr_train_plan`;
CREATE TABLE `hr_train_plan` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `plan_code`           VARCHAR(64)  NOT NULL                COMMENT '计划编码(唯一)',
  `plan_name`           VARCHAR(200) NOT NULL                COMMENT '计划名称',
  `plan_type`           VARCHAR(32)  NOT NULL DEFAULT 'regular' COMMENT '计划类型(new_employee-岗前培训/regular-常规培训/professional-专业提升/manager-管理晋升)',
  `start_date`          DATE         NOT NULL                COMMENT '计划开始日期',
  `end_date`            DATE         NOT NULL                COMMENT '计划结束日期',
  `target_dept_ids`     JSON         DEFAULT NULL            COMMENT '目标部门ID列表JSON数组(为空表示面向全员)',
  `is_mandatory`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '是否强制必修(0否 1是)',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '流程审批状态(DRAFT-草稿/RUNNING-审批中/PASSED-审批通过/REJECTED-已驳回/CANCELLED-已取消)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '关联流程实例ID',
  `budget`              DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '预算费用(元)',
  `description`         VARCHAR(1000) DEFAULT NULL           COMMENT '培训目标/计划说明',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_plan_type` (`plan_type`),
  KEY `idx_start_end` (`start_date`, `end_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训计划表';

-- 3. 培训班级/期数表
DROP TABLE IF EXISTS `hr_train_session`;
CREATE TABLE `hr_train_session` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `plan_id`         BIGINT       NOT NULL                COMMENT '关联培训计划ID',
  `course_id`       BIGINT       NOT NULL                COMMENT '关联课程ID',
  `session_name`    VARCHAR(200) NOT NULL                COMMENT '班级名称(如: 2026年第一期Java岗前培训)',
  `lecturer`        VARCHAR(100) NOT NULL                COMMENT '讲师名称',
  `start_time`      DATETIME     NOT NULL                COMMENT '开课时间',
  `end_time`        DATETIME     NOT NULL                COMMENT '结课时间',
  `location`        VARCHAR(200) DEFAULT NULL            COMMENT '培训地点(线下则填写物理地点;线上则填写URL或"线上直播")',
  `max_capacity`    INT          NOT NULL DEFAULT 50     COMMENT '最大容纳人数',
  `enrolled_num`    INT          NOT NULL DEFAULT 0      COMMENT '已报名人数',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'draft' COMMENT '班级状态(draft-草稿/enrolling-报名中/ongoing-培训中/completed-已结课/cancelled-已取消)',
  `sign_code`       VARCHAR(64)  DEFAULT NULL            COMMENT '扫码签到随机校验码',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训班级/期数表';

-- 4. 学员报名表
DROP TABLE IF EXISTS `hr_train_enroll`;
CREATE TABLE `hr_train_enroll` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `session_id`          BIGINT       NOT NULL                COMMENT '关联培训班级ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `dept_id`             BIGINT       NOT NULL                COMMENT '部门ID(报名时冗余)',
  `enroll_type`         VARCHAR(32)  NOT NULL DEFAULT 'self' COMMENT '报名类型(self-员工自助报名/mandatory-强制指派)',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '报名审批状态(DRAFT-草稿/RUNNING-审批中/APPROVED-已报名/REJECTED-已驳回/CANCELLED-已取消)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '审批流程实例ID',
  `sign_status`         CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '签到状态(0未签到 1已签到)',
  `sign_time`           DATETIME     DEFAULT NULL            COMMENT '签到时间',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_emp` (`session_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学员报名表';

-- 5. 培训通过与学分记录表
DROP TABLE IF EXISTS `hr_train_record`;
CREATE TABLE `hr_train_record` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `enroll_id`           BIGINT       NOT NULL                COMMENT '关联报名ID',
  `session_id`          BIGINT       NOT NULL                COMMENT '关联班级ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `course_id`           BIGINT       NOT NULL                COMMENT '关联课程ID',
  `score`               DECIMAL(5,2) DEFAULT NULL            COMMENT '考试/考核成绩',
  `result_status`       VARCHAR(32)  NOT NULL DEFAULT 'fail' COMMENT '考核结果(pass-通过/fail-不通过)',
  `credit_granted`      DECIMAL(5,2) NOT NULL DEFAULT 0.00   COMMENT '实发学分',
  `grant_time`          DATETIME     DEFAULT NULL            COMMENT '学分授予时间',
  `is_skills_unlocked`  CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '是否已点亮技能树(0否 1是)',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_emp_rec` (`session_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_result_status` (`result_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训通过与学分记录表';
```

### 3.3 索引说明与 EXPLAIN 验证

| 查询场景 | 依赖索引 | EXPLAIN 期望结果 |
|----------|----------|------------------|
| 查询学员报名状态 | `uk_session_emp` | `type = const`，直接命中唯一记录。 |
| 查询员工个人所有培训记录 | `idx_emp_id` | `type = ref`，高频自查询秒级响应。 |
| 查询班级学员签到名单 | `idx_status` + `idx_dept_id` | `type = ref / range`，大班级名单快速加载。 |
| 查找生效日期临近或计划中的培训计划 | `idx_start_end` + `idx_status` | `type = range`，定时任务能高效扫表。 |

---

## 4. 状态枚举与数据约定

### 4.1 培训课程类型 `TrainCourseType`

| Code | 说明 |
|------|------|
| `online` | 线上课程（看视频/自学） |
| `offline` | 线下课程（面授/集中培训） |
| `external` | 外部培训（派外参训） |

### 4.2 培训计划类型 `TrainPlanType`

| Code | 说明 |
|------|------|
| `new_employee` | 岗前培训（新员工强制触发） |
| `regular` | 常规培训（通用技能提升） |
| `professional` | 专业提升（岗位深度定制） |
| `manager` | 管理晋升（梯队人才培养） |

### 4.3 培训计划流程状态 `TrainPlanStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 计划草稿 |
| `RUNNING` | 审批流程中 |
| `PASSED` | 审批通过（生效，可用于开班） |
| `REJECTED` | 审批驳回 |
| `CANCELLED`| 计划取消 |

### 4.4 培训班级状态 `TrainSessionStatus`

| Code | 说明 |
|------|------|
| `draft` | 班级草稿 |
| `enrolling` | 报名中（学员可见，可进行报名） |
| `ongoing` | 培训中（报名入口关闭，开放签到） |
| `completed` | 已结课（培训结束，开放成绩录入与学分评定） |
| `cancelled` | 班级取消 |

### 4.5 学员报名审批状态 `TrainEnrollStatus`

| Code | 说明 |
|------|------|
| `DRAFT` | 报名草稿（未提交审批） |
| `RUNNING` | 报名审批流程中 |
| `APPROVED` | 审批通过/已成功报名（强制报名直接进入此状态） |
| `REJECTED` | 报名被驳回 |
| `CANCELLED`| 报名取消 |

### 4.6 报名渠道类型 `TrainEnrollType`

| Code | 说明 |
|------|------|
| `self` | 员工在移动端/Web自主发起报名（需启动审批流） |
| `mandatory` | 管理员强制指派（无需审批直接生效，并作为入职必修课程来源） |

### 4.7 考核结果状态 `TrainResultStatus`

| Code | 说明 |
|------|------|
| `pass` | 考核通过（授予学分，点亮核心技能树） |
| `fail` | 考核未通过（无学分发放，需重修） |

---

## 5. API 契约

### 5.1 统一前缀与规范

- REST API 根路径：`/api/hr/training`
- 返回格式：统一使用 `cn.oa.common.R<T>` （包含 code, message, data 属性）
- 数据过滤：普通员工仅能操作自主报名和个人培训记录；HR/管理员具备课程库、计划、班级的完整 CRUD 与考核批量评定权。
- 权限码规范：`hr:train:{resource}:{action}`

### 5.2 管理员端核心接口

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/hr/training/courses` | `hr:train:course:create` | 创建新培训课程 |
| `PUT` | `/api/hr/training/courses/{id}` | `hr:train:course:update` | 更新培训课程配置 |
| `DELETE`| `/api/hr/training/courses/{id}` | `hr:train:course:delete` | 逻辑删除指定课程 |
| `GET` | `/api/hr/training/courses` | `hr:train:course:list` | 课程库列表（支持按技能、状态、类型搜索分页） |
| `POST` | `/api/hr/training/plans` | `hr:train:plan:create` | 新增培训计划（处于 DRAFT 状态） |
| `POST` | `/api/hr/training/plans/{id}/submit` | `hr:train:plan:submit` | 提交培训计划审核（启动审批流） |
| `GET` | `/api/hr/training/plans` | `hr:train:plan:list` | 培训计划列表与多维度检索 |
| `POST` | `/api/hr/training/sessions` | `hr:train:session:create` | 针对生效计划+课程创建班级期数 |
| `PUT` | `/api/hr/training/sessions/{id}/status` | `hr:train:session:manage` | 更新班级状态（enrolling / ongoing / completed） |
| `POST` | `/api/hr/training/sessions/{id}/assign` | `hr:train:session:manage` | 强制批量指派员工报名（直接置为 APPROVED） |
| `GET` | `/api/hr/training/sessions/{id}/enrolled` | `hr:train:session:manage` | 查看班级已报名学员名单与签到详情 |
| `POST` | `/api/hr/training/sessions/{id}/sign-code` | `hr:train:session:manage` | 刷新并生成班级的实时签到二维码内容 |
| `POST` | `/api/hr/training/records/batch-submit` | `hr:train:record:evaluate` | 批量录入班级学员考核成绩，触发学分与技能解锁 |
| `GET` | `/api/hr/training/reports/credits` | `hr:train:report:view` | 全员累计学分与技能分布统计列表 |

### 5.3 员工自助端接口 (Mobile / Web 复合)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/hr/training/my/available-sessions` | `hr:train:my:view` | 可报名班级列表（班级为 enrolling，且我属于覆盖部门） |
| `POST` | `/api/hr/training/my/enroll` | `hr:train:my:enroll` | 自主申请报名指定班级（提交后启动报名审批工作流） |
| `GET` | `/api/hr/training/my/sessions` | `hr:train:my:view` | 我的参训班级记录（包含进行中、已结课等） |
| `POST` | `/api/hr/training/my/sessions/{sessionId}/sign` | `hr:train:my:sign` | 移动端扫码签到（校验 `signCode`） |
| `GET` | `/api/hr/training/my/records` | `hr:train:my:view` | 我的学分明细、解锁技能历史与考核结果汇总 |

---

## 6. DTO 与 VO 设计

### 6.1 `TrainCourseCreateDTO`
```json
{
  "courseCode": "C-2026-001",
  "courseName": "高并发系统重构实战",
  "courseType": "online",
  "majorSkills": ["Java", "Spring Boot", "MyBatis-Plus"],
  "credit": 3.00,
  "durationHours": 12.0,
  "courseUrl": "https://oss.oa.company/videos/refactor-1.mp4",
  "description": "深入浅出，讲解三端架构重构技巧与实操"
}
```

### 6.2 `TrainPlanCreateDTO`
```json
{
  "planCode": "P-2026-HR01",
  "planName": "2026年技术中台人才发展项目",
  "planType": "professional",
  "startDate": "2026-06-10",
  "endDate": "2026-12-31",
  "targetDeptIds": [10, 11, 12],
  "isMandatory": "0",
  "budget": 50000.00,
  "description": "旨在培育具备全面系统重构能力的骨干员工"
}
```

### 6.3 `TrainSessionCreateDTO`
```json
{
  "planId": 1,
  "courseId": 10,
  "sessionName": "2026技术中台重构第一期班",
  "lecturer": "张三(高级技术专家)",
  "startTime": "2026-06-15 09:00:00",
  "endTime": "2026-06-15 18:00:00",
  "location": "A栋5楼多功能多媒体会议室",
  "maxCapacity": 100
}
```

### 6.4 `TrainEnrollSubmitDTO`
```json
{
  "sessionId": 5
}
```

### 6.5 `TrainRecordBatchEvaluateDTO`
```json
{
  "sessionId": 5,
  "evaluations": [
    {
      "empId": 1001,
      "score": 92.50,
      "resultStatus": "pass"
    },
    {
      "empId": 1002,
      "score": 55.00,
      "resultStatus": "fail"
    }
  ]
}
```

### 6.6 `TrainMyCreditVO`
```json
{
  "empId": 1001,
  "totalCredits": 12.50,
  "skillsUnlocked": ["Java", "Spring Boot", "Element Plus"],
  "completedSessionsCount": 4,
  "records": [
    {
      "courseId": 10,
      "courseName": "高并发系统重构实战",
      "sessionName": "2026技术中台重构第一期班",
      "score": 92.50,
      "resultStatus": "pass",
      "creditGranted": 3.00,
      "grantTime": "2026-06-16 10:00:00",
      "isSkillsUnlocked": "1",
      "skills": ["Java", "Spring Boot"]
    }
  ]
}
```

---

## 7. 任务波次拆分

按照请假试点的 Wave 1 到 Wave 5 的格式，任务由 T1 细化到 T11：

### Wave 1: 契约与基线

#### T1 数据库与 API 契约
- **目标**：定义培训管理五张表结构（DDL）、初始系统种子数据（Menu和权限配置）、DTO/VO结构与 Controller 路径和权限码规范。
- **路径**：`code/backend/sql/`、`docs/superpowers/specs/2026-06-02-hr-training-task-split.md`。
- **输入**：本重构任务拆分文档第 3-6 章，系统 DDL 风格规范。
- **输出**：生成名为 `005_hr_training_schema.sql` 的脚本（暂放置在 `code/backend/sql/` 目录下）、完整的契约代码接口骨架（不含实现）。
- **禁止修改**：不实现任何底层 Service 或 Mapper 业务，保障契约纯净度。
- **验收**：通过 Maven 编译契约骨架无红线。

#### T2 旧实现影响分析
- **目标**：全面排查当前旧数据库中是否存在 `oa_train` 或课程、计划等表，定位是否有冗余旧类，确保本轮是“纯粹新增、绿色集成”。
- **路径**：`code/backend/` 各模块、前端 `src/api/`、移动端 `src/api/`。
- **输出**：在当前文档后追加“旧请假/培训冲突性分析报告”。若完全无冲突，明确定义绿色新增包路径（`cn.oa.hr.training` 系列）。
- **禁止修改**：禁止大范围删除或改动已有的无关模块代码。
- **验收**：确认包路径命名及 API 前缀在全局不产生任何冲突冲突。

---

### Wave 2: 后端核心

#### T3 HR 培训模块实体与 Mapper
- **目标**：在 `oa-hr`（或规划的 HR 扩展包中）完整建立上述 5 张核心表的 Entity 实体类、Mapper 接口及对应的 XML Mapper 映射。
- **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/training/{entity,mapper,enums}/`。
- **输入**：T1 生成的 SQL，MyBatis-Plus 标准 Entity/Mapper 代码规范。
- **输出**：5 个完整的 Entity 实体类、5 个 Mapper 接口与对应的 XML 映射文件，提供字符串常量枚举。
- **禁止修改**：禁止在此阶段混杂业务 Service 逻辑。
- **验收**：通过命令 `cd code/backend && mvn clean compile` 编译无错。

#### T4 HR 培训业务 Service
- **目标**：实现课程库 CRUD、培训计划提审与生效控制、班级状态推进、签到校验、学员报名人数高并发原子操作、结课考核批量录入与学分授予、**核心点亮技能树（原子去重追加到 `hr_employee_ext.skills` 中）**的完整业务核心。
- **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/training/service/`。
- **输入**：T3 后端实体、MyBatis-Plus ORM 能力。
- **输出**：Service 接口与实现类，包含健壮的业务校验（如开班人数超限、重复报名限制、非 completed 班级禁录成绩、去重原子更新技能列表）。
- **禁止修改**：禁止在此层实现前端展示逻辑；严禁越过 Service 之间直接连表修改工作流核心表（一律通过 Workflow 预留的 API 进行调用）。
- **验收**：针对每项核心业务，编写并运行单元测试：`cd code/backend && mvn -pl oa-hr -am test`，覆盖率需达 80% 以上。

#### T5 HR REST API
- **目标**：为 Web 端及移动自助端暴露契约中所列的所有 RESTful 接口，接入 Spring Security / 自定义 Token 权限注解，拦截越权行为。
- **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/training/controller/`。
- **输入**：T4 Service 层，Knife4j/OpenAPI3 框架，系统鉴权验证切面。
- **输出**：2 个 Controller 控制类（`HrTrainingAdminController` 与 `HrTrainingMyController`），完成规范的出参入参校验。
- **禁止修改**：禁止把本该留在 Service 的业务流程放到 Controller 内拼装。
- **验收**：在 `oa-web` 中集成编译，并保证 `mvn -pl oa-hr,oa-web -am test` 全套测试通过。

---

### Wave 3: 工作流与消息联动

#### T6 工作流回调接入
- **目标**：编写 `TrainingWorkflowHandler`。将“培训计划审批”（`TrainPlan`）与“自主报名审批”（`TrainEnroll`）接入工作流，实现流程启动，并作为回调接收方处理 `onApproved`、`onRejected` 事件：
  - 培训计划审批通过 -> 状态变更为 `PASSED`；
  - 员工自助报名审批通过 -> 状态变更为 `APPROVED`，**并原子递增班级报名人数 `enrolled_num`**（必须有并发控制，防超限）。
- **路径**：`code/backend/oa-workflow/` 扩展、`code/backend/oa-hr/`。
- **输入**：`WorkflowCallbackDispatcher`，`wf_process_definition` 配置。
- **输出**：可靠的高幂等审批 Handler 实现。
- **禁止修改**：不破坏 `oa-workflow` 的核心引擎代码，不改动已有的请假审批回调。
- **验收**：Mock 工作流通过集成测试：`mvn -pl oa-workflow,oa-hr -am test`。

#### T7 待办与消息联动及入职监听
- **目标**：实现以下三类事件的通知与联动：
  - 1. 当报名被上级审批通过，或班级发布报名时，通过 WebSocket (向 `NotificationEndpoint` 推送实时在线通知) 并且入库 `oa_message`。
  - 2. **新员工入职强制报名监听器**：订阅 `EmployeeEntryEvent`（员工入职事件），自动查询有效的岗前培训计划，生成直接置为 `APPROVED` 的 `mandatory` 类型学员报名记录，并产生系统待办。
- **路径**：`code/backend/oa-message/`、`code/backend/oa-hr/`。
- **输入**：WebSocket 模块，入职事件。
- **输出**：消息监听器与待办事件发布类，实现高健壮的入职静默自动报名流程。
- **禁止修改**：禁止引入除 WebSocket 之外的外部短信和邮件付费硬件渠道。
- **验收**：执行 `mvn -pl oa-message,oa-hr -am test` 测试成功。

---

### Wave 4: Web 与移动端

#### T8 Web API 与页面迁移
- **目标**：在前端 Web 管理端开发培训课程管理、培训计划与审批发起、培训班级管理、扫码签到看板、批量考核录分等 5 个功能页，对接全新的 `/api/hr/training` 后端。
- **路径**：`code/frontend/src/views/hr/training/`、`code/frontend/src/api/training.ts`。
- **输入**：T5 控制器 RESTful API、Element Plus、Vite 6。
- **输出**：完整的、排版美观且支持中英文适配的 Web 配置界面与报表。
- **禁止修改**：不破坏已有的系统菜单布局，严禁引入未经许可的第三方图表框架。
- **验收**：通过 `cd code/frontend && pnpm typecheck && pnpm build` 验收前端编译无警告。

#### T9 Mobile API 与页面迁移
- **目标**：在移动端 H5 视图开发：课程中心、近期可自主报名的班级列表、一键提审报名、开课详情展示、扫码签到面板、我的培训档案与学分展示。
- **路径**：`code/mobile/src/pages/hr/training/`、`code/mobile/src/api/training.ts`。
- **输入**：uni-app (Vue 3)、H5 规范。
- **输出**：适配良好、触控精准的移动端页面和扫码仿真操作组件。
- **禁止修改**：禁止去兼容复杂的各家小程序端摄像头硬件，扫码采用前端仿真文本录入或简易弹窗，确保 H5 通用性。
- **验收**：执行 `cd code/mobile && pnpm build:h5` 构建成功。

---

### Wave 5: 验证与下线准备

#### T10 端到端回归
- **目标**：覆盖完整的培训链路验收。包括：“入职 -> 触发强制岗前培训 -> 扫码签到 -> 考核打分及解锁档案技能点亮”；以及“员工自主报名 -> 工作流审批 -> 学分获取 -> 状态更新”。
- **路径**：`code/backend/` 集成测试、页面端 E2E 手工验证。
- **输入**：完整的本地三端联调环境。
- **输出**：E2E 流程跑通测试用例、满意的学分技能统计看板。
- **验收**：端到端用例无一报错。

#### T11 集成测试与上线清单
- **目标**：编写最终合入 master 并在生产部署的清单。含系统初始化数据库 DDL 权限菜单项数据、定时任务配置（开课提示）。
- **路径**：`code/backend/sql/005_hr_training_schema.sql`、`docs/superpowers/specs/`。
- **输出**：上线指南。
- **验收**：最终通过一键 Maven 命令成功启动服务并导入数据：`mvn clean package -Dmaven.test.skip=true`。

---

## 8. 推荐执行顺序

```
Wave 1: T1 & T2 (分析及建表契约骨架，确保绿色零冲突)
  └── Wave 2: T3 (Entity/Mapper) -> T4 (Service 实现) -> T5 (Controller)
        └── Wave 3: T6 (工作流审批闭环) -> T7 (待办入职事件自动报名)
              └── Wave 4: T8 (Web管理端) & T9 (Mobile学员自服务端) 并行开发
                    └── Wave 5: T10 (端到端E2E联合测试) -> T11 (上线及权限归纳)
```

---

## 9. 最小验收矩阵

| 模块区域 | 验收命令 / 检查手段 |
|----------|-------------------|
| **数据库** | `code/backend/sql/005_hr_training_schema.sql` 无语法错误，能直接在 MySQL 8.0 运行。 |
| **后端编译** | `cd code/backend && mvn -pl oa-hr -am compile` 通过 |
| **后端测试** | `cd code/backend && mvn -pl oa-hr -am test` 单元测试通过，核心逻辑全覆盖 |
| **系统集成** | `cd code/backend && mvn -pl oa-hr,oa-web,oa-workflow,oa-message -am test` 通过 |
| **前端打包** | `cd code/frontend && pnpm typecheck && pnpm build` 运行成功无 error |
| **移动打包** | `cd code/mobile && pnpm build:h5` 编译打包 H5 成功 |

---

## 10. 核心开发提示词 (可直接供 Claude 执行)

本章节提供用于执行 T3、T4、T5 以及工作流、事件联动的高级提示词，可直接拷贝用作后续步骤。

### 10.1 实体与 Mapper 开发 (T3) 提示词
```markdown
请在 `oa-hr` 模块中创建培训管理的核心实体与 Mapper：
1. **包路径划分**：实体类在 `cn.oa.hr.training.entity`，Mapper 在 `cn.oa.hr.training.mapper`，XML文件在 `classpath*:mapper/training/*.xml`，枚举在 `cn.oa.hr.training.enums`。
2. **实现以下 5 个实体类（对接 MyBatis-Plus 3.5.9）**：
   - `HrTrainCourse` (hr_train_course)
   - `HrTrainPlan` (hr_train_plan)
   - `HrTrainSession` (hr_train_session)
   - `HrTrainEnroll` (hr_train_enroll)
   - `HrTrainRecord` (hr_train_record)
3. **编写要求**：
   - 继承项目中的公共基础父类（如果存在，请查询包含 id, delFlag, createBy, createTime, updateBy, updateTime 字段的 BaseEntity，若无则直接声明）。
   - 实体的 JSON 字段（如 major_skills, target_dept_ids）使用 MyBatis-Plus 提供的 JacksonTypeHandler 实现自动序列化。
   - 确保使用 `@TableName`、`@TableId(type = IdType.AUTO)`。
   - 状态使用字符串枚举形式提供，如 `TrainPlanStatus`、`TrainSessionStatus`、`TrainEnrollStatus`、`TrainResultStatus`、`TrainCourseType`、`TrainPlanType`、`TrainEnrollType`，声明为 `@EnumValue` 保证在数据库和 Java 对象间完美序列化。
4. **验证**：运行 `mvn -pl oa-hr compile`，保证编译成功无报错。
```

### 10.2 核心 Service 与技能点亮原子逻辑 (T4) 提示词
```markdown
请实现 `HrTrainingService`，核心关注高并发报名防超限、入职自动报名与考核通过点亮技能树的原子去重写入：
1. **点亮技能树核心逻辑**：
   - 提供方法 `evaluateParticipant(Long sessionId, Long empId, BigDecimal score, String resultStatus)`。
   - 当 `resultStatus == 'pass'` 时：
     - 查询 `hr_train_record` 确保幂等：若已生成过 'pass' 记录且 `is_skills_unlocked = '1'`，则直接返回，不重复点亮。
     - 查询该班级对应的 `HrTrainCourse.major_skills` 技能列表（如 `["Java", "Spring Boot"]`）。
     - 调用 `hr_employee_ext` 服务，原子查询该员工现有的 `skills` 列表。由于存在高并发与覆盖风险，使用 `SELECT skills FROM hr_employee_ext WHERE emp_id = ? FOR UPDATE`。
     - 在 Java 中将旧技能列表与课程的新技能列表合并去重。
     - 更新该员工扩展档案：`UPDATE hr_employee_ext SET skills = ? WHERE emp_id = ?`，并更新培训记录的 `is_skills_unlocked = '1'` 和学分累加。
     - 以上一系列操作必须包裹在强 `@Transactional` 事务内。
2. **高并发报名防超卖逻辑**：
   - 员工报名、指派报名时需要扣减班级可容纳人数。
   - 在 XML 中编写一条自研的高效 SQL 更新班级已报名人数：
     `UPDATE hr_train_session SET enrolled_num = enrolled_num + 1 WHERE id = #{sessionId} AND enrolled_num < max_capacity AND del_flag = '0'`。
   - 在 Java 中判断该 Update 执行结果的 rows：若返回 1 表示报名成功；若返回 0 则直接抛出“该班级报名人数已满”业务异常，保证彻底避免超限现象。
3. **单元测试编写**：
   - 使用 `@SpringBootTest` 编写测试用例，涵盖正常打分点亮、重复打分幂等、高并发报名扣减测试、不通过不解锁等场景，运行 `mvn -pl oa-hr -am test` 通过。
```

### 10.3 控制器与鉴权 (T5) 提示词
```markdown
请为培训管理子模块开发 REST API 控制器：
1. **控制器命名与路径**：
   - 管理端控制器：`HrTrainingAdminController`，主路径为 `/api/hr/training/admin`
   - 自助端控制器：`HrTrainingMyController`，主路径为 `/api/hr/training/my`
2. **安全要求**：
   - 严格使用项目中的鉴权注解（如 `@PreAuthorize` 或 `@RequiresPermissions`）限制权限范围。
   - 接口入参使用 `@Valid` 结合 Spring Boot Validator 进行强类型校验（如学分不能为负，日期起止不能倒置，计划必修非空校验）。
   - OpenAPI3 / Knife4j 注解要全面，每个方法和 DTO 必须带中文文档备注。
3. **数据隔离**：
   - 在 `HrTrainingMyController` 中，绝不能让学员自主传入 `empId`。必须通过 `SecurityUtils.getUserId()` 强制获取当前登陆人 ID 进行绑定和数据过滤，拦截越权风险。
4. **测试**：
   - 编写 MockMvc 测试覆盖各接口，验证越权拒绝逻辑。
```

### 10.4 工作流与事件联动 (T6, T7) 提示词
```markdown
请为培训管理模块进行工作流与事件联动闭环集成：
1. **工作流回调实现**：
   - 编写 `TrainingWorkflowHandler` 并注入工作流调度中心。
   - 针对培训计划审批：
     - `onApproved(Long planId)`: 修改计划状态为 `PASSED`，并触发实时 WebSocket 消息推送提醒关联部门人员“有新培训计划发布”。
     - `onRejected(Long planId)`: 修改计划状态为 `REJECTED`。
   - 针对员工自主报名审批：
     - `onApproved(Long enrollId)`: 变更学员报名状态为 `APPROVED`，同时调用 `HrTrainSession` 的高并发原子增 1 逻辑。若已满员，工作流应能捕获到该异常，优雅提示审批人满员，并回滚状态。
2. **新员工入职强制报名事件监听**：
   - 创建 `EmployeeEntryEventListener`，监听 `EmployeeEntryEvent`。
   - 当获取到新员工信息后，查询适用于其所属部门、岗位且强制必修的计划类型为 `new_employee` 且有效的计划 `HrTrainPlan`。
   - 为该计划下的关联班级 `HrTrainSession` 创建一条报名类型为 `mandatory`（强制指派）且状态为 `APPROVED` 的 `HrTrainEnroll` 记录。
   - 将对应必修待办写入待办任务表中。
3. **集成测试验证**：
   - 运行 `mvn -pl oa-workflow,oa-message,oa-hr -am test` 确保流程闭环无卡点。
```
