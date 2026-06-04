# HR 绩效管理子模块重构任务拆分

> 日期: 2026-06-03
> 子模块范围: 绩效管理(Perf/Performance) — 考核模版、考核周期、目标设定、绩效评估、结果归档
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`
> 试点参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 子模块说明与目标

### 1.1 为什么是"子模块"

绩效管理属于 `oa-hr` 模块的第二轮业务能力扩展，不单独拆分 Maven 模块，沿用 `oa-hr` 已有的 Entity/DTO/VO/Mapper/Service/Controller 分层结构。休假闭环 (T1-T11) 已验证了 `oa-hr` 模块的基础设施，本轮直接复用该模块的依赖、包规范和测试框架。

### 1.2 完成后应具备的能力

1. 管理员可配置多维度考核模板（KPI/OKR/360度等），含权重、评分维度、等级标准。
2. 按年度/季度自动或手动创建考核周期，设定目标填报、自评、他评、结果确认各阶段时间窗口。
3. 员工可在目标填报期提交个人/团队绩效目标，直属上级审批。
4. 考核期进入评估阶段后，员工自评 + 上级/同事/下级（360度）打分，系统按模板权重自动计算总分。
5. 绩效结果生成后，支持员工确认/申诉、上级复核、HR终审定级。
6. 结果归档后可与薪资、晋升、培训等模块联动（本阶段只输出事件，不实现下游消费）。
7. 管理员可查看部门/全员绩效统计报表。

---

## 2. 边界定义

### 2.1 包含范围

| 区域 | 内容 |
|------|------|
| 数据库 | `hr_perf_template`、`hr_perf_cycle`、`hr_perf_goal`、`hr_perf_eval`、`hr_perf_result` |
| 后端 | `oa-hr` 模块内新增 Entity、DTO、VO、Enum、Mapper、Service、Controller、测试 |
| Web | 管理端：模板配置、周期管理、结果统计；员工端：目标填报、自评、结果查看 |
| Mobile | 员工端目标填报、自评、结果查看（只读+简单编辑） |
| 测试 | 各层单元测试 + Controller 测试 + 考核流程集成测试 |

### 2.2 不包含范围

| 不包含 | 原因 |
|--------|------|
| 薪资联动 | 属于 `oa-finance` 范围，本模块只发布 `PerformanceResultPublishedEvent` |
| 培训推荐联动 | 属于 `oa-knowledge` 或其他后续模块 |
| 晋升/异动联动 | 晋升属于 `oa-hr` 后续扩展；异动已在 `hr_transfer` 中，本模块只发布事件 |
| 复杂报表/BI | 只做基础统计列表；复杂可视化留给数据报表专项 |
| 考勤数据自动填充KPI | 依赖完整的考勤迁移，后续迭代 |
| Elasticsearch 全文检索 | 本模块无需要 |
| 前端 monorepo 改造 | 沿用现有 `code/frontend` 结构 |

---

## 3. 核心数据模型

### 3.1 五张表总览

| 表名 | 说明 | 核心职责 |
|------|------|----------|
| `hr_perf_template` | 考核模板表 | 定义考核维度、维度权重、评分标准、等级划分（优秀/良好/合格/待改进） |
| `hr_perf_cycle` | 考核周期表 | 定义考核年度/季度，管理各阶段（目标填报/评估/结果确认）开放时间 |
| `hr_perf_goal` | 绩效目标表 | 员工在目标填报期提交的具体目标项，含目标内容、权重、衡量标准 |
| `hr_perf_eval` | 绩效评估表 | 多评估人（自评/上级/同事/下级）对各目标的打分记录 |
| `hr_perf_result` | 绩效结果表 | 最终汇总得分、等级、评语、确认状态、申诉记录 |

### 3.2 表结构详细设计

#### 3.2.1 `hr_perf_template` — 考核模板表

```sql
CREATE TABLE `hr_perf_template` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code`   VARCHAR(64)  NOT NULL                COMMENT '模板编码(唯一)',
  `template_name`   VARCHAR(200) NOT NULL                COMMENT '模板名称',
  `template_type`   VARCHAR(32)  NOT NULL DEFAULT 'kpi' COMMENT '模板类型(kpi/okr/360/comprehensive)',
  `dimensions`      JSON         NOT NULL                COMMENT '考核维度配置JSON [{"code":"work","name":"工作业绩","weight":60,"maxScore":100},...]',
  `grade_config`    JSON         NOT NULL                COMMENT '等级标准JSON [{"grade":"A","label":"优秀","minScore":90},...]',
  `scoring_type`    VARCHAR(32)  DEFAULT 'hundred'       COMMENT '评分制(hundred/b-five/a-five/ten)',
  `evaluator_types` JSON         DEFAULT NULL            COMMENT '评估人类型JSON ["self","superior","peer","subordinate"]',
  `is_default`      CHAR(1)      DEFAULT '0'             COMMENT '是否默认模板(0否 1是)',
  `description`     VARCHAR(500) DEFAULT NULL            COMMENT '描述',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考核模板表';
```

**设计意图**：模板是绩效管理的配置中心，维度、权重、等级通过 JSON 灵活配置，不采用子表拆分以简化初期实现。

---

#### 3.2.2 `hr_perf_cycle` — 考核周期表

```sql
CREATE TABLE `hr_perf_cycle` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '周期ID',
  `cycle_name`      VARCHAR(200) NOT NULL                COMMENT '周期名称(如 2026年度Q1考核)',
  `cycle_year`      INT          NOT NULL                COMMENT '考核年度',
  `cycle_quarter`   INT          DEFAULT NULL            COMMENT '考核季度(1-4, NULL为年度考核)',
  `template_id`     BIGINT       NOT NULL                COMMENT '关联模板ID',
  `goal_start_date` DATE         NOT NULL                COMMENT '目标填报开始日',
  `goal_end_date`   DATE         NOT NULL                COMMENT '目标填报截止日',
  `eval_start_date` DATE         NOT NULL                COMMENT '评估开始日',
  `eval_end_date`   DATE         NOT NULL                COMMENT '评估截止日',
  `result_start_date` DATE       NOT NULL                COMMENT '结果确认开始日',
  `result_end_date` DATE         NOT NULL                COMMENT '结果确认截止日',
  `current_phase`   VARCHAR(32)  DEFAULT 'draft'         COMMENT '当前阶段(draft/goal/eval/result/archived)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0待启动 1进行中 2已归档 3已取消)',
  `cover_dept_ids`  JSON         DEFAULT NULL            COMMENT '覆盖部门ID列表(NULL=全公司)',
  `cover_emp_ids`   JSON         DEFAULT NULL            COMMENT '覆盖人员ID列表(NULL=按部门)',
  `auto_notify`     CHAR(1)      DEFAULT '1'             COMMENT '阶段切换时自动通知(0否 1是)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cycle_year` (`cycle_year`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_current_phase` (`current_phase`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考核周期表';
```

**设计意图**：考核周期是时间轴控制器，通过阶段字段驱动流程推进。当 `current_phase` 发生变更时，由业务层触发阶段切换检查，决定是否允许目标填报/评估/结果确认。

---

#### 3.2.3 `hr_perf_goal` — 绩效目标表

```sql
CREATE TABLE `hr_perf_goal` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '目标ID',
  `cycle_id`        BIGINT       NOT NULL                COMMENT '考核周期ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `dept_id`         BIGINT       DEFAULT NULL            COMMENT '部门ID(冗余)',
  `goal_type`       VARCHAR(32)  DEFAULT 'personal'      COMMENT '目标类型(personal/team/okr)',
  `dimension_code`  VARCHAR(32)  NOT NULL                COMMENT '所属考核维度编码',
  `content`         VARCHAR(1000) NOT NULL               COMMENT '目标内容',
  `weight`          DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '权重(0-100)',
  `measure_std`     VARCHAR(500) DEFAULT NULL            COMMENT '衡量标准',
  `target_value`    VARCHAR(200) DEFAULT NULL            COMMENT '目标值(可数字/可文字)',
  `actual_value`    VARCHAR(200) DEFAULT NULL            COMMENT '实际完成值',
  `deadline`        DATE         DEFAULT NULL            COMMENT '目标截止日',
  `status`          VARCHAR(32)  DEFAULT 'draft'         COMMENT '状态(draft/submitted/approved/rejected/modified)',
  `approval_opinion` VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  `superior_id`     BIGINT       DEFAULT NULL            COMMENT '直属上级ID',
  `sort_order`      INT          DEFAULT 0               COMMENT '排序',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_cycle_emp` (`cycle_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绩效目标表';
```

**设计意图**：目标填报期员工提交的每一条目标。`dimension_code` 关联模板维度编码，`weight` 在单维度内可再分配。上级审批通过后方可进入评估期。

---

#### 3.2.4 `hr_perf_eval` — 绩效评估表

```sql
CREATE TABLE `hr_perf_eval` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评估ID',
  `cycle_id`        BIGINT       NOT NULL                COMMENT '考核周期ID',
  `goal_id`         BIGINT       NOT NULL                COMMENT '目标ID',
  `evaluated_emp_id` BIGINT      NOT NULL                COMMENT '被评估人ID',
  `evaluator_emp_id` BIGINT      NOT NULL                COMMENT '评估人ID',
  `evaluator_type`  VARCHAR(32)  NOT NULL                COMMENT '评估人类型(self/superior/peer/subordinate/hr)',
  `dimension_code`  VARCHAR(32)  NOT NULL                COMMENT '维度编码',
  `score`           DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '评分(0-100或对应评分制)',
  `weight`          DECIMAL(5,2) DEFAULT 100              COMMENT '评估人权重(默认100%)',
  `comment`         VARCHAR(1000) DEFAULT NULL            COMMENT '评语',
  `status`          VARCHAR(32)  DEFAULT 'draft'         COMMENT '状态(draft/submitted/confirmed)',
  `submit_time`     DATETIME     DEFAULT NULL            COMMENT '提交时间',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cycle_goal_evaluator` (`cycle_id`, `goal_id`, `evaluator_emp_id`, `evaluator_type`),
  KEY `idx_cycle_evaluated` (`cycle_id`, `evaluated_emp_id`),
  KEY `idx_evaluator` (`evaluator_emp_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绩效评估表';
```

**设计意图**：每个评估人对每条目标的打分记录。通过 `UNIQUE KEY` 保证同一评估人不对同一目标重复打分。`weight` 用于不同评估人的权重差异（如上级60%，同事30%，下级10%）。

---

#### 3.2.5 `hr_perf_result` — 绩效结果表

```sql
CREATE TABLE `hr_perf_result` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '结果ID',
  `cycle_id`        BIGINT       NOT NULL                COMMENT '考核周期ID',
  `emp_id`          BIGINT       NOT NULL                COMMENT '员工ID',
  `dept_id`         BIGINT       DEFAULT NULL            COMMENT '部门ID',
  `template_id`     BIGINT       NOT NULL                COMMENT '模板ID',
  `total_score`     DECIMAL(6,2) NOT NULL DEFAULT 0      COMMENT '综合得分',
  `dimension_scores` JSON        DEFAULT NULL            COMMENT '各维度得分JSON {"work":92,"attitude":85}',
  `grade`           VARCHAR(16)  DEFAULT NULL            COMMENT '绩效等级(A/B/C/D/E)',
  `grade_label`     VARCHAR(32)  DEFAULT NULL            COMMENT '等级标签(优秀/良好/合格/待改进)',
  `summary`         VARCHAR(2000) DEFAULT NULL           COMMENT '综合评语',
  `status`          VARCHAR(32)  DEFAULT 'pending'     COMMENT '状态(pending/confirmed/appealed/adjusted/final)',
  `confirm_time`    DATETIME     DEFAULT NULL            COMMENT '员工确认时间',
  `appeal_reason`   VARCHAR(1000) DEFAULT NULL            COMMENT '申诉理由',
  `appeal_time`     DATETIME     DEFAULT NULL            COMMENT '申诉时间',
  `hr_adjust_score` DECIMAL(6,2) DEFAULT NULL            COMMENT 'HR调整后的得分',
  `hr_adjust_reason` VARCHAR(500) DEFAULT NULL            COMMENT 'HR调整理由',
  `hr_adjust_time`  DATETIME     DEFAULT NULL            COMMENT 'HR调整时间',
  `rank_in_dept`    INT          DEFAULT NULL            COMMENT '部门内排名',
  `rank_in_company` INT        DEFAULT NULL            COMMENT '公司内排名',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cycle_emp` (`cycle_id`, `emp_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_grade` (`grade`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='绩效结果表';
```

**设计意图**：最终结果汇总表，包含完整生命周期（待确认 -> 已确认/申诉 -> 调整 -> 终定）。`dimension_scores` 保留各维度原始得分，用于后续分析。排名字段按需计算填充，非核心。

---

### 3.3 索引验收说明

| 查询场景 | 依赖索引 | EXPLAIN 验收要求 |
|----------|----------|------------------|
| 按周期查某员工所有目标 | `idx_cycle_emp` | type=ref, 无filesort |
| 按周期+被评估人查所有打分 | `idx_cycle_evaluated` | type=ref |
| 同一评估人不可重复打分 | `uk_cycle_goal_evaluator` | 数据库唯一约束 |
| 周期内每人只一条结果 | `uk_cycle_emp` | 数据库唯一约束 |
| 部门绩效统计 | `idx_dept_id` + `idx_grade` | type=ref, 可能需索引合并 |
| 我的考核结果列表 | `idx_emp_id` | type=ref |

---

## 4. 状态枚举与数据约定

### 4.1 考核周期阶段 `PerfCyclePhase`

| code | 说明 |
|------|------|
| `DRAFT` | 草稿，尚未发布 |
| `GOAL` | 目标填报期 |
| `EVAL` | 评估打分期 |
| `RESULT` | 结果确认期 |
| `ARCHIVED` | 已归档 |

### 4.2 考核周期状态 `PerfCycleStatus`

| code | 说明 |
|------|------|
| `PENDING` | 待启动 |
| `ACTIVE` | 进行中 |
| `ARCHIVED` | 已归档 |
| `CANCELLED` | 已取消 |

### 4.3 绩效目标状态 `PerfGoalStatus`

| code | 说明 |
|------|------|
| `DRAFT` | 草稿（员工编辑中） |
| `SUBMITTED` | 已提交待审批 |
| `APPROVED` | 上级已审批通过 |
| `REJECTED` | 上级已驳回 |
| `MODIFIED` | 驳回后修改中 |

### 4.4 评估状态 `PerfEvalStatus`

| code | 说明 |
|------|------|
| `DRAFT` | 草稿（评估中） |
| `SUBMITTED` | 已提交 |
| `CONFIRMED` | 已确认不可修改 |

### 4.5 评估人类型 `PerfEvaluatorType`

| code | 权重角色 | 说明 |
|------|----------|------|
| `SELF` | 自评 | 员工本人 |
| `SUPERIOR` | 直属上级 | 默认权重最高 |
| `PEER` | 同事 | 平级评估人 |
| `SUBORDINATE` | 下级 | 向下评估 |
| `HR` | HR专员 | 复核调整 |

### 4.6 绩效等级（模板可配置，以下为默认）

| grade | label | minScore | maxScore |
|-------|-------|----------|----------|
| A | 优秀 | 90 | 100 |
| B+ | 良好+ | 80 | 89.99 |
| B | 良好 | 70 | 79.99 |
| C | 合格 | 60 | 69.99 |
| D | 待改进 | 0 | 59.99 |

### 4.7 结果状态 `PerfResultStatus`

| code | 说明 |
|------|------|
| `PENDING` | 待员工确认 |
| `CONFIRMED` | 员工已确认 |
| `APPEALED` | 员工已申诉 |
| `ADJUSTED` | HR已调整 |
| `FINAL` | 终定（不可再改） |

---

## 5. API 契约

### 5.1 统一前缀与规范

- 前缀：`/api/hr/performance`
- 响应格式：`{"code":0,"message":"...","data":...}`
- 分页参数：`pageNum`、`pageSize`、`sortField`、`sortOrder`
- 权限码格式：`hr:performance:{resource}:{action}`

### 5.2 管理员接口

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| POST | `/api/hr/performance/templates` | `hr:perf:template:create` | 创建考核模板 |
| PUT | `/api/hr/performance/templates/{id}` | `hr:perf:template:update` | 修改模板 |
| GET | `/api/hr/performance/templates` | `hr:perf:template:list` | 查询模板列表 |
| GET | `/api/hr/performance/templates/{id}` | `hr:perf:template:detail` | 模板详情 |
| PUT | `/api/hr/performance/templates/{id}/toggle` | `hr:perf:template:update` | 启用/停用模板 |
| POST | `/api/hr/performance/cycles` | `hr:perf:cycle:create` | 创建考核周期 |
| PUT | `/api/hr/performance/cycles/{id}` | `hr:perf:cycle:update` | 修改周期 |
| POST | `/api/hr/performance/cycles/{id}/advance` | `hr:perf:cycle:manage` | 推进到下一阶段 |
| GET | `/api/hr/performance/cycles` | `hr:perf:cycle:list` | 周期列表 |
| GET | `/api/hr/performance/cycles/{id}` | `hr:perf:cycle:detail` | 周期详情 |
| GET | `/api/hr/performance/results` | `hr:perf:result:list` | 结果列表（管理端） |
| PUT | `/api/hr/performance/results/{id}/adjust` | `hr:perf:result:adjust` | HR调整结果 |
| GET | `/api/hr/performance/results/statistics` | `hr:perf:result:stats` | 绩效统计报表 |

### 5.3 员工接口

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/api/hr/performance/my-cycles` | `hr:perf:cycle:view` | 我的考核周期 |
| GET | `/api/hr/performance/my-goals` | `hr:perf:goal:view` | 当前周期我的目标 |
| POST | `/api/hr/performance/my-goals` | `hr:perf:goal:create` | 提交/保存目标 |
| PUT | `/api/hr/performance/my-goals/{id}` | `hr:perf:goal:update` | 修改目标（审批前） |
| POST | `/api/hr/performance/my-goals/{id}/submit` | `hr:perf:goal:submit` | 正式提交目标 |
| GET | `/api/hr/performance/my-evaluations` | `hr:perf:eval:view` | 我的评估任务 |
| POST | `/api/hr/performance/my-evaluations` | `hr:perf:eval:submit` | 提交评估打分 |
| GET | `/api/hr/performance/my-result` | `hr:perf:result:view` | 我的绩效结果 |
| POST | `/api/hr/performance/my-result/confirm` | `hr:perf:result:confirm` | 确认结果 |
| POST | `/api/hr/performance/my-result/appeal` | `hr:perf:result:appeal` | 申诉 |

### 5.4 上级接口

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| GET | `/api/hr/performance/subordinate-goals` | `hr:perf:goal:approve` | 查看下属目标 |
| POST | `/api/hr/performance/subordinate-goals/{id}/approve` | `hr:perf:goal:approve` | 审批通过 |
| POST | `/api/hr/performance/subordinate-goals/{id}/reject` | `hr:perf:goal:approve` | 审批驳回 |
| GET | `/api/hr/performance/subordinate-results` | `hr:perf:result:view` | 查看下属结果 |

---

## 6. DTO / VO 设计

### 6.1 `PerfTemplateCreateDTO`

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| `templateCode` | String | 必填, 2-64字符 | 编码 |
| `templateName` | String | 必填, 2-200字符 | 名称 |
| `templateType` | String | 必填, 枚举值 | kpi/okr/360/comprehensive |
| `dimensions` | List<DimensionConfig> | 必填, 非空, weight之和=100 | 维度配置 |
| `gradeConfig` | List<GradeConfig> | 必填, 非空, 覆盖0-100 | 等级配置 |
| `scoringType` | String | 必填 | 评分制 |
| `evaluatorTypes` | List<String> | 必填 | 评估人类型 |

**DimensionConfig 结构：**
```json
{
  "code": "work",
  "name": "工作业绩",
  "weight": 60,
  "maxScore": 100,
  "description": "..."
}
```

**GradeConfig 结构：**
```json
{
  "grade": "A",
  "label": "优秀",
  "minScore": 90,
  "maxScore": 100
}
```

### 6.2 `PerfCycleCreateDTO`

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| `cycleName` | String | 必填 | 名称 |
| `cycleYear` | Integer | 必填, >2000 | 年度 |
| `cycleQuarter` | Integer | 1-4 或 null | 季度 |
| `templateId` | Long | 必填 | 模板ID |
| `goalStartDate` / `goalEndDate` | String(Date) | 必填, 起≤止 | 目标期 |
| `evalStartDate` / `evalEndDate` | String(Date) | 必填, 起≤止 | 评估期 |
| `resultStartDate` / `resultEndDate` | String(Date) | 必填, 起≤止 | 结果确认期 |
| `coverDeptIds` | List<Long> | 可选 | 覆盖部门 |
| `coverEmpIds` | List<Long> | 可选 | 覆盖人员 |

### 6.3 `PerfGoalCreateDTO`

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| `cycleId` | Long | 必填 | 周期ID |
| `goalType` | String | 必填 | 目标类型 |
| `dimensionCode` | String | 必填 | 维度编码 |
| `content` | String | 必填, ≤1000 | 目标内容 |
| `weight` | BigDecimal | 必填, 0-100 | 权重 |
| `measureStd` | String | 可选, ≤500 | 衡量标准 |
| `targetValue` | String | 可选, ≤200 | 目标值 |
| `deadline` | String(Date) | 可选 | 截止日期 |

### 6.4 `PerfEvalSubmitDTO`

| 字段 | 类型 | 校验 | 说明 |
|------|------|------|------|
| `cycleId` | Long | 必填 | 周期ID |
| `goalId` | Long | 必填 | 目标ID |
| `evaluatedEmpId` | Long | 必填 | 被评估人 |
| `score` | BigDecimal | 必填, 0-100 | 分数 |
| `comment` | String | 可选, ≤1000 | 评语 |

### 6.5 `PerfResultVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 结果ID |
| `cycleName` | String | 周期名称 |
| `empId` / `empName` | Long/String | 员工 |
| `deptName` | String | 部门 |
| `totalScore` | BigDecimal | 综合得分 |
| `dimensionScores` | Map | 各维度得分 |
| `grade` / `gradeLabel` | String/String | 等级 |
| `summary` | String | 评语 |
| `status` / `statusName` | String/String | 状态 |
| `confirmTime` | String | 确认时间 |
| `canAppeal` | Boolean | 是否可申诉 |
| `canConfirm` | Boolean | 是否可确认 |

---

## 7. 新旧映射关系

本模块在旧系统中无完全对等实现，属于新增业务能力。以下列出旧系统中可能弱关联的部分及对应关系：

| 旧系统 | 新系统 | 关系 |
|--------|--------|------|
| `oa_emp_archive` (员工档案) | `hr_employee_ext` + `hr_perf_result` | 旧档案可能有年度考核结论文本字段，新系统拆分为结构化结果表 |
| `sys_employee.emp_code` 等 | `hr_perf_result.emp_id` | 关联基础员工信息 |
| `oa_salary_structure` (薪资) | `hr_perf_result` (事件) | 未来通过 `PerformanceResultPublishedEvent` 联动 |
| 无考核模板 | `hr_perf_template` | 全新能力 |
| 无考核周期 | `hr_perf_cycle` | 全新能力 |
| 无目标管理 | `hr_perf_goal` | 全新能力 |
| 无360评估 | `hr_perf_eval` | 全新能力 |

**结论**：绩效管理子模块基本属于**纯新增功能**，不存在旧表映射替换问题。但是仍需遵循以下约束——

- 所有新表使用 `hr_` 前缀，与现有 `hr_*` 表风格一致。
- Entity 放在 `cn.oa.hr.entity` 包下，与现有 `HrLeaveApply` 等同级。
- API 前缀 `/api/hr/performance/*`，与现有 `/api/hr/leaves/*` 同级。

---

## 8. 任务波次 (W10-W11)

> 范围编号 W10-W11 表示在整体重构计划 Phase 2（W20-W25 oa-hr 扩展波次）中的定位。

### Wave 10: 契约与基线

#### T1: 数据模型与 API 契约

| 字段 | 内容 |
|------|------|
| **目标** | 定义绩效管理五张表、DTO/VO、权限码、API 路径 |
| **路径** | `code/backend/sql/`、`docs/superpowers/specs/2026-06-02-hr-performance-task-split.md` |
| **输入** | 本拆分文档第 3-6 章；`oa-hr` 模块现有包结构；`001_schema.sql` 风格 |
| **输出** | SQL DDL 草案、API 契约表、权限码清单、DTO/VO 字段清单 |
| **禁止修改** | 不实现 Java/前端代码；不修改正式 SQL baseline |
| **验收** | 文档列出所有表结构、接口、字段、索引、权限码 |

#### T2: 旧系统影响分析

| 字段 | 内容 |
|------|------|
| **目标** | 确认绩效管理在旧系统中的缺失/关联点，明确纯新增边界 |
| **路径** | `code/backend/oa-model`、`code/backend/oa-service`、`code/backend/oa-web` 下可能的档案/考核相关残留 |
| **输入** | 旧 `oa_emp_archive`、`oa_salary_structure` |
| **输出** | 影响分析清单：是否为纯新增、是否有同名表/类冲突 |
| **禁止修改** | 不改旧代码 |
| **验收** | 清单明确说明"纯新增"或列出冲突 |

---

### Wave 10: 后端核心

#### T3: Entity + Mapper + Enum

| 字段 | 内容 |
|------|------|
| **目标** | 在 `oa-hr` 模块建立绩效管理五张表的 Entity、DTO/VO 雏形、Mapper、枚举 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/{entity,dto,vo,enums,mapper}` |
| **输入** | T1 结果；`oa-hr` 模块现有依赖和包规范 |
| **输出** | 5个 Entity、DTO/VO、Mapper 接口、枚举 |
| **禁止修改** | 不实现 Service/Controller；不改前端；不改旧 `oa_*` |
| **验收** | `cd code/backend && mvn -pl oa-hr -am test` (编译+单元测试通过) |

#### T4: Service 层

| 字段 | 内容 |
|------|------|
| **目标** | 实现模板管理、周期管理、目标填报、评估打分、结果汇总的业务 Service |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/service/` |
| **输入** | T3 产出；模板/周期/目标/评估/结果全链路业务规则 |
| **输出** | Service 接口与实现、DTO、单元测试 |
| **禁止修改** | 不实现 Controller；不直接操作 `wf_*` 表；不修改前端 |
| **验收** | `cd code/backend && mvn -pl oa-hr -am test` |

**必须实现的服务能力**：

| 能力 | 说明 |
|------|------|
| `create/update/query Template` | CRUD + 启用停用；校验 dimensions weight 合计=100 |
| `create/update/advance Cycle` | 周期创建与阶段推进；校验各阶段日期不重叠 |
| `save/submit/approve Goal` | 员工保存/提交目标；上级审批通过/驳回；校验目标填报期 |
| `submit Evaluation` | 按模板维度+评估人打分；校验评估期；UR 保证同一评估人只能打一次 |
| `calculate Result` | 按权重汇总所有评估分数；按等级配置定级；生成结果记录 |
| `confirm/appeal Result` | 员工确认或申诉；HR调整得分/等级 |
| `statistics` | 部门/全公司等级分布统计 |

**并发与幂等要求**：

1. 同一(周期, 目标, 评估人, 类型)只能有一条评估记录 — 数据库唯一约束兜底。
2. 结果计算必须幂等：重复调用只更新同一 `hr_perf_result` 记录。
3. 周期阶段切换检查：需确保前一阶段关键动作完成后再推进。

**测试覆盖场景**：

| 场景 | 断言 |
|------|------|
| 模板维度权重之和 != 100 | 抛业务异常 |
| 正常创建周期 | 状态 PENDING |
| 推进到 GOAL 阶段 | current_phase=GOAL |
| 目标填报期外提交目标 | 抛业务异常 |
| 评估期外提交打分 | 抛业务异常 |
| 正常评估打分 | eval 记录生成 |
| 重复打分 | 抛业务异常(唯一约束) |
| 结果计算 | total_score=维度加权平均分*评估人加权 |
| 等级定级 | 按 grade_config 匹配正确 grade |
| 员工确认结果 | status=CONFIRMED |
| 员工申诉 | status=APPEALED |
| HR调整得分 | status=ADJUSTED, hr_adjust_score 有值 |

#### T5: REST Controller + 测试

| 字段 | 内容 |
|------|------|
| **目标** | 暴露 T4 所有能力的 REST API，对齐 API 契约 |
| **路径** | `code/backend/oa-hr/src/main/java/cn/oa/hr/controller/` |
| **输入** | T4 Service；API 契约表 (第 5 章) |
| **输出** | Controller、权限注解、OpenAPI 注解、Controller 测试 |
| **禁止修改** | 旧 Controller；前端；mobile；正式 SQL baseline |
| **验收** | `cd code/backend && mvn -pl oa-hr,oa-web -am test` |

---

### Wave 11: 前端与验证

#### T6: Web 管理端页面

| 字段 | 内容 |
|------|------|
| **目标** | Web 管理端：模板配置页、周期管理页、结果统计页 |
| **路径** | `code/frontend/src/views/hr/performance/` |
| **输入** | T5 API；Element Plus 组件 |
| **输出** | 模板列表/编辑、周期列表/创建、结果统计/调整页面 |
| **禁止修改** | 不重构全局布局；不做 monorepo 改造 |
| **验收** | `cd code/frontend && pnpm typecheck && pnpm build` |

#### T7: Web 员工端页面 + Mobile 页面

| 字段 | 内容 |
|------|------|
| **目标** | Web 员工端：目标填报、评估打分、结果查看；Mobile：同功能精简版 |
| **路径** | `code/frontend/src/views/hr/performance/my/` + `code/mobile/src/pages/hr/performance/` |
| **输入** | T5 API；uni-app 组件规范 |
| **输出** | 目标填报表单、评估打分表单、结果查看页面 |
| **禁止修改** | 不改全局路由；Mobile 不做管理配置页 |
| **验收** | Web: `pnpm typecheck && pnpm build`；Mobile: `pnpm build:h5` |

#### T8: 端到端集成与回归

| 字段 | 内容 |
|------|------|
| **目标** | 验证完整绩效管理链路：模板 -> 周期 -> 目标填报 -> 审批 -> 评估 -> 结果 -> 确认 |
| **路径** | `code/backend/oa-hr/src/test/java/` 集成测试；或手工验证 |
| **输入** | 全链路 API |
| **输出** | 集成测试类或验证脚本、结果报告 |
| **禁止修改** | 不扩大测试范围到请假/异动等无关模块 |
| **验收** | 测试通过，无阻断问题 |

---

## 9. 推荐执行顺序

```
Wave 10
├── T1 + T2 (并行)     # 契约与影响分析
├── T3                  # Entity + Mapper + Enum
├── T4                  # Service
├── T5                  # Controller + 测试

Wave 11
├── T6 与 T7 (并行)     # Web管理端 + Web员工端 + Mobile
└── T8                  # 集成验证
```

T1/T2 完成前不得开始代码实现（Entity/Mapper）。

---

## 10. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| HR后端编译 | `cd code/backend && mvn -pl oa-hr -am compile` |
| HR后端测试 | `cd code/backend && mvn -pl oa-hr -am test` |
| HR + Web 入口 | `cd code/backend && mvn -pl oa-hr,oa-web -am test` |
| Web 前端 | `cd code/frontend && pnpm typecheck && pnpm build` |
| Mobile | `cd code/mobile && pnpm build:h5` |

---

## 11. 约束与红线

### 11.1 通用约束

| 约束 | 规则 |
|------|------|
| 表前缀 | 新表必须用 `hr_perf_` 前缀 |
| API前缀 | `/api/hr/performance/*` |
| 包路径 | `cn.oa.hr.{entity,dto,vo,enums,mapper,service,controller}` |
| 枚举 | 字符串枚举，与请假试点风格一致 |
| 权限码 | `hr:perf:{resource}:{action}` |
| JSON 字段 | `dimensions`、`gradeConfig`、`dimensionScores` 必须注明结构示例 |
| 软删除 | 统一 `del_flag` |

### 11.2 禁止行为

1. **不修改旧 `oa_*` 相关代码/表/实体** — 本模块为纯新增。
2. **不扩展 `oa-workflow` core** — 绩效模块目前不依赖审批流；如需审批（目标审批、申诉审批），后续迭代再接入。
3. **不做薪资/档案/培训/晋升联动** — 只发布领域事件，不消费下游。
4. **不在 T5 之前开始前端编码**。
5. **不修改前端全局路由/布局/样式系统**。

---

## 12. 跨模块协作

### 12.1 发布的事件

绩效管理作为 `oa-hr` 内的子模块，仅在结果终定时发布领域事件，供其他模块按需订阅：

```java
// 绩效结果已终定事件
public record PerformanceResultPublishedEvent(
    Long cycleId,
    Long empId,
    BigDecimal totalScore,
    String grade,
    LocalDateTime publishedAt
) {}
```

消费方（未来实现）：
- `oa-finance`：可按等级调整薪资系数。
- `oa-hr`（异动）：晋升/调薪决策参考。
- `oa-knowledge`：根据等级推荐培训课程。

### 12.2 可选的接入点

如果后续决定将"目标审批"和"申诉审批"也接入工作流，可复用 `oa-workflow` 引擎：

| 审批场景 | 流程编码 | 说明 |
|----------|----------|------|
| 目标上级审批 | `perf_goal_approval` | 员工提交目标 -> 直属上级审批 -> 通过/驳回 |
| 绩效申诉 | `perf_appeal` | 员工申诉 -> HR复核 -> 调整/驳回 |

本阶段（Wave 10-11）**不实现**工作流接入，采用简单的直接回调模式：上级直接在 Service 中审批目标。后续迭代如需工作流，按 HR 请假试点的 T6 模式接入即可。

---

## 13. Claude Code 可执行提示词

### 13.1 T1: 数据模型与 API 契约

```text
请执行绩效管理子模块 T1：数据模型与 API 契约。

必须先阅读：
- docs/superpowers/specs/2026-06-02-hr-performance-task-split.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md (第3.4.2节 人事管理)
- code/backend/sql/baseline/001_schema.sql (现有 hr_* 表风格参考)

范围：
- 只允许输出文档草案，不实现任何 Java/Vue 代码。
- 不修改正式 001_schema.sql，输出到单独的 SQL 草案文件。

输出：
1. code/backend/sql/hr_performance_contract.sql
   - 包含 hr_perf_template、hr_perf_cycle、hr_perf_goal、hr_perf_eval、hr_perf_result 五张表 DDL
   - 索引需对齐第3章的查询场景
2. 完整 API 契约表（路径、方法、权限码、请求/响应 DTO 字段）
3. 枚举值清单（所有字符串枚举）
4. 索引 EXPLAIN 验收说明
5. T3 需要的 DTO/VO/Entity 字段清单

完成后汇报改动文件和下一步建议。
```

### 13.2 T2: 旧系统影响分析

```text
请执行绩效管理子模块 T2：旧系统影响分析。

目标：确认绩效管理是否为纯新增能力，是否存在旧表/旧代码冲突。

检查范围：
1. code/backend/oa-model/src/main/java/cn/oa/entity/ 下是否有 perf/performance/考核/绩效 相关类
2. code/backend/oa-mapper/src/main/java/cn/oa/mapper/ 下是否有相关 Mapper
3. code/backend/oa-service/src/main/java/cn/oa/service/ 下是否有相关 Service
4. code/backend/oa-web/src/main/java/cn/oa/controller/ 下是否有相关 Controller
5. code/frontend/src/api/ 和 code/frontend/src/views/ 下是否有绩效页面/API
6. code/mobile/src/ 下是否有绩效页面/API
7. 数据库现有 schema 中是否有 perf/performance 相关表

输出：
- 旧系统关联清单（如有）
- 是否"纯新增"的明确结论
- 如有同名/近似类，给出冲突说明和合并建议

禁止：不改任何代码文件。
```

### 13.3 T3: Entity + Mapper + Enum

```text
请执行绩效管理子模块 T3：Entity + Mapper + Enum。

严格遵循 docs/superpowers/specs/2026-06-02-hr-performance-task-split.md 第3章表结构、第4章枚举、第8章T3任务定义。

前置条件：
- T1 已完成 SQL DDL 草案确认
- T2 已确认"纯新增"，无旧代码冲突

允许修改：
- code/backend/oa-hr/src/main/java/cn/oa/hr/entity/Perf*.java
- code/backend/oa-hr/src/main/java/cn/oa/hr/dto/Perf*DTO.java
- code/backend/oa-hr/src/main/java/cn/oa/hr/vo/Perf*VO.java
- code/backend/oa-hr/src/main/java/cn/oa/hr/enums/Perf*.java
- code/backend/oa-hr/src/main/java/cn/oa/hr/mapper/Perf*Mapper.java
- code/backend/oa-hr/src/test/java/cn/oa/hr/ 相关测试

禁止修改：
- 不实现 Service/Controller
- 不改 frontend/mobile
- 不改旧 oa-service/oa-model
- 不改正式 SQL baseline

完成后运行：
cd code/backend
mvn -pl oa-hr -am test

最终汇报：
- 新增/修改文件清单
- 是否发现已有重复模型
- 验收命令结果
- T4 需要注意的问题
```

### 13.4 T4: Service 层

```text
请执行绩效管理子模块 T4：Service 层。

严格遵循 docs/superpowers/specs/2026-06-02-hr-performance-task-split.md 第8章 T4 任务定义。

前置条件：
- T3 已完成 Entity/DTO/VO/Enum/Mapper

允许修改：
- code/backend/oa-hr/src/main/java/cn/oa/hr/service/Perf*.java
- code/backend/oa-hr/src/main/java/cn/oa/hr/service/impl/Perf*.java
- code/backend/oa-hr/src/test/java/cn/oa/hr/service/ 相关测试

必须实现的服务能力：
1. PerfTemplateService - 模板CRUD，校验 dimensions weight 合计=100
2. PerfCycleService - 周期CRUD，阶段推进(advance)，日期校验
3. PerfGoalService - 目标保存/提交/审批/驳回，阶段校验
4. PerfEvalService - 评估打分，唯一约束保障重复提交防护
5. PerfResultService - 结果计算(calculate)、确认(confirm)、申诉(appeal)、HR调整(adjust)
6. PerfStatisticsService - 统计报表

并发与幂等要求：
- 同一(周期,目标,评估人,类型)只能一条记录 -> 数据库唯一约束
- 结果计算幂等 -> 同一(周期,员工)只更新一条 result
- 阶段切换原子性 -> 校验当前阶段后再更新

测试要求：
- 模板维度权重校验(通过/失败)
- 周期日期校验(重叠/正常)
- 阶段切换(草稿->目标期->评估期->结果期->归档)
- 目标提交(阶段校验)
- 评估打分(重复提交防护)
- 结果计算与定级(权重公式正确性)
- 结果申诉与调整(状态流转)

完成后运行：
cd code/backend
mvn -pl oa-hr -am test

最终汇报：
- 新增/修改文件清单
- 核心业务方法清单
- 测试覆盖场景数
- 验收命令结果
- T5 Controller 需要注意的问题
```

### 13.5 T5: REST Controller

```text
请执行绩效管理子模块 T5：REST Controller。

严格遵循 docs/superpowers/specs/2026-06-02-hr-performance-task-split.md 第5章 API 契约、第8章 T5 任务定义。

前置条件：
- T4 Service 层已实现并通过测试

允许修改：
- code/backend/oa-hr/src/main/java/cn/oa/hr/controller/Perf*.java
- code/backend/oa-hr/src/test/java/cn/oa/hr/controller/ 相关测试
- code/backend/oa-web/pom.xml（如 oa-web 尚未依赖 oa-hr 或测试装配失败时）

必须实现的 API：
管理员接口：
- POST/PUT/GET /api/hr/performance/templates
- POST/PUT/GET /api/hr/performance/cycles
- GET /api/hr/performance/results
- PUT /api/hr/performance/results/{id}/adjust
- GET /api/hr/performance/results/statistics

员工接口：
- GET /api/hr/performance/my-cycles
- GET/POST/PUT /api/hr/performance/my-goals
- POST /api/hr/performance/my-goals/{id}/submit
- GET/POST /api/hr/performance/my-evaluations
- GET/POST /api/hr/performance/my-result

上级接口：
- GET /api/hr/performance/subordinate-goals
- POST /api/hr/performance/subordinate-goals/{id}/approve
- POST /api/hr/performance/subordinate-goals/{id}/reject

要求：
- 统一返回 R<T>
- DTO 参数 @Valid 校验
- 从 UserContext 获取 empId
- 权限注解 @RequirePermission
- Controller 不写业务逻辑

完成后运行：
cd code/backend
mvn -pl oa-hr,oa-web -am test

最终汇报：
- 新增/修改文件
- API 路径清单
- 权限注解清单
- Controller 测试覆盖
- 验收命令结果
- T6 前端需要注意的问题
```

### 13.6 T6-T7 前端 (聚合提示词)

```text
请执行绩效管理子模块 T6/T7：Web 前端与 Mobile 页面。

严格按 docs/superpowers/specs/2026-06-02-hr-performance-task-split.md 第5章 API 契约调用后端接口。

Web 管理端 (code/frontend/src/views/hr/performance/)：
- 模板配置：列表 + 编辑（维度/等级 JSON 编辑器）
- 周期管理：列表 + 创建 + 阶段推进
- 结果统计：列表 + 等级分布图表 + HR调整弹窗

Web 员工端 (code/frontend/src/views/hr/performance/my/)：
- 我的目标：当前周期目标列表 + 填报表单 + 提交
- 我的评估：评估任务列表 + 打分表单
- 我的结果：结果查看 + 确认/申诉按钮

Mobile (code/mobile/src/pages/hr/performance/)：
- 我的目标填报、我的评估、我的结果（精简版）

验收：
cd code/frontend && pnpm typecheck && pnpm build
cd code/mobile && pnpm build:h5
```

---

## 14. 回滚策略

| 回滚场景 | 操作 |
|----------|------|
| T3 Entity 设计缺陷 | 删除 `oa-hr` 内新增 Perf* 类，下一波次重新设计 |
| T4 Service 逻辑错误 | 回退到 T3 状态，保留 Entity，重写 Service |
| T5 Controller 路径冲突 | 保留 Service，修改 Controller 路径和测试 |
| T6/T7 前端实现失败 | 前端代码可独立删除，不影响后端 |
| 整体模块取消 | 删除 `oa-hr` 内所有 `Perf*` 类及相关测试；删除前端 `hr/performance` 目录 |

---

## 15. 附录

### 附录 A：模板维度配置 JSON 示例

```json
[
  {
    "code": "work",
    "name": "工作业绩",
    "weight": 60,
    "maxScore": 100,
    "description": "基于目标完成度和关键成果"
  },
  {
    "code": "attitude",
    "name": "工作态度",
    "weight": 20,
    "maxScore": 100,
    "description": "责任心、团队协作、主动性"
  },
  {
    "code": "ability",
    "name": "工作能力",
    "weight": 20,
    "maxScore": 100,
    "description": "专业技能、学习能力、创新能力"
  }
]
```

### 附录 B：等级配置 JSON 示例

```json
[
  {"grade": "A", "label": "优秀", "minScore": 90, "maxScore": 100},
  {"grade": "B+", "label": "良好+", "minScore": 80, "maxScore": 89.99},
  {"grade": "B", "label": "良好", "minScore": 70, "maxScore": 79.99},
  {"grade": "C", "label": "合格", "minScore": 60, "maxScore": 69.99},
  {"grade": "D", "label": "待改进", "minScore": 0, "maxScore": 59.99}
]
```

### 附录 C：结果维度得分 JSON 示例

```json
{
  "work": 92.5,
  "attitude": 85.0,
  "ability": 88.0
}
```

### 附录 D：领域事件定义

```java
package cn.oa.hr.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绩效结果已终定事件 — 供其他模块订阅
 */
public record PerformanceResultPublishedEvent(
    Long resultId,
    Long cycleId,
    Long empId,
    BigDecimal totalScore,
    String grade,
    String gradeLabel,
    LocalDateTime publishedAt
) {}
```

### 附录 E：结果计算公式

```
单维度得分 = SUM(评估人打分_i * 评估人权重_i) / SUM(评估人权重_i)
综合得分 = SUM(单维度得分_j * 维度权重_j)
等级 = 查找 grade_config 中 minScore <= 综合得分 <= maxScore 的 grade
```

---

## 变更日志

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-06-03 | v1.0 | 初始创建，基于 HR 请假试点模板适配绩效管理子模块 |
