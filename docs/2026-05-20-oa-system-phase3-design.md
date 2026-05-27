# 企业OA系统 - 第三阶段设计文档：业务管理模块

## 1. 阶段概述

第三阶段将OA系统与组织核心业务深度融合，打通办公与业务数据通道。包含人事管理、财务管理、资产管理、合同管理四个模块。这些模块依赖第一阶段的组织架构权限，部分流程与第二阶段的审批引擎对接。

### 1.1 依赖关系

```
第一阶段                第二阶段                第三阶段
┌────────────┐      ┌────────────┐      ┌──────────────────┐
│ 组织架构    │◄─────│ 流程审批引擎 │◄─────│ 人事管理         │
│ 用户/角色   │◄─────│            │◄─────│ 财务管理         │
│ 数据字典    │◄─────│            │◄─────│ 资产管理         │
│ 系统配置    │◄─────│            │◄─────│ 合同管理         │
└────────────┘      └────────────┘      └──────────────────┘
```

### 1.2 新增微服务

| 服务 | 职责 | 端口 |
|------|------|------|
| oa-hr | 人事管理服务 | 9204 |
| oa-finance | 财务管理服务 | 9205 |
| oa-asset | 资产管理服务 | 9206 |
| oa-contract | 合同管理服务 | 9207 |

---

## 2. 人事管理

人事管理覆盖员工"入职-在职-离职"全生命周期，是企业最核心的业务模块之一。

### 2.1 功能清单

#### 2.1.1 人事档案管理

**员工信息：**
| 信息分类 | 字段 |
|---------|------|
| 基本信息 | 工号、姓名、性别、出生日期、身份证号、民族、籍贯、政治面貌、婚姻状况 |
| 联系信息 | 手机号、邮箱、紧急联系人、紧急联系人电话、现住址 |
| 岗位信息 | 部门、岗位、职级、入职日期、转正日期、合同到期日、直接上级 |
| 教育经历 | 学历、学位、毕业院校、专业、毕业时间（支持多条） |
| 工作经历 | 原单位、职位、起止时间（支持多条） |
| 证件信息 | 证件类型、证件号、有效期（支持多条：资格证、驾照等） |

**档案操作：**
- 新增员工档案（可手动创建或入职审批自动创建）
- 编辑员工信息（变更记录留痕）
- 离职处理（冻结账号、待办转交、资产归还检查）
- 档案导出（Excel/PDF花名册）
- 高级搜索（按部门/岗位/入职时间/学历等多维度筛选）

#### 2.1.2 考勤管理

**考勤规则：**
- 考勤组管理：支持多套考勤规则（如：研发弹性打卡、行政固定时间）
- 打卡规则：上班时间、下班时间、迟到阈值、早退阈值
- 弹性打卡：允许前后N分钟的弹性时间
- 打卡方式：PC端Web打卡 + 移动端GPS打卡（预留）
- 免打卡日：法定节假日、公司自定义休息日
- 排班管理：支持固定排班和倒班制

**考勤记录：**
- 每日打卡记录（上班打卡/下班打卡，含时间、IP/GPS位置）
- 考勤异常：迟到、早退、缺卡、旷工自动识别
- 月度考勤汇总：每人每月出勤天数、迟到次数、早退次数、加班时长、请假天数
- 考勤异常申诉：员工可提交异常说明，主管审批

**考勤统计：**
- 部门考勤统计：各部门月度出勤率、异常率
- 个人考勤明细：每人每日打卡详情
- 导出：月度考勤报表Excel导出

#### 2.1.3 请假管理

**假期类型：**
| 假期类型 | 说明 | 额度管理 |
|---------|------|---------|
| 年假 | 按工龄自动计算额度 | 年度额度，按入职日期重置 |
| 事假 | 无薪假 | 不限额，扣薪 |
| 病假 | 需上传医院证明 | 按月/年限额 |
| 婚假 | 法定3天 | 一次性 |
| 产假/陪产假 | 法定天数 | 一次性 |
| 丧假 | 直系亲属3天 | 一次性 |
| 调休 | 加班换取 | 按加班时长累计 |

**请假流程：**
1. 员工发起请假申请 → 选择假期类型、起止时间、请假事由
2. 系统自动校验：假期余额是否足够、日期是否与已批请假重叠
3. 关联审批流程（第二阶段的流程引擎）
4. 审批通过 → 自动扣减假期余额 → 更新考勤记录

**假期余额管理：**
- 年假自动计算：根据工龄和社会工龄，按法定标准自动发放
- 调休自动累计：加班审批通过后自动增加调休余额
- 余额查询：员工可查看自己各类假期余额和使用明细

#### 2.1.4 加班管理

- 员工申请加班：日期、时段（工作日加班/周末加班/节假日加班）、加班事由
- 审批通过后自动计入加班时长
- 加班补偿方式选择：调休/加班费
- 加班统计：月度/年度加班时长统计

#### 2.1.5 出差管理

- 出差申请：目的地、起止日期、出差事由、预估费用
- 关联审批流程
- 出差期间自动设置考勤免打卡
- 出差报告：归来后填写出差报告
- 差旅报销关联：出差审批单可直接关联差旅报销

#### 2.1.6 薪酬管理（基础版）

**薪酬结构：**
```
应发工资 = 基本工资 + 岗位工资 + 绩效工资 + 各类补贴
实发工资 = 应发工资 - 五险一金个人部分 - 个税 - 其他扣款
```

**功能：**
- 薪资项目配置：自定义薪资组成项（基本工资、岗位津贴、交通补贴、餐补等）
- 薪资标准设置：按岗位/职级设置薪资标准
- 月度薪资核算：根据考勤、请假、加班等数据自动计算
- 薪资审批：HR核算 → 部门确认 → 财务审批 → 发放
- 薪资条：员工查看个人薪资明细（可配置是否允许查看）
- 薪资历史：查看历史薪资记录

#### 2.1.7 培训管理

- 培训计划：年度/季度培训计划制定
- 培训课程管理：课程信息、讲师、时间、地点、名额
- 培训报名：员工在线报名（名额控制）
- 培训签到：二维码签到
- 培训评估：课后评价、考试（预留）
- 培训记录：个人培训档案

#### 2.1.8 绩效管理（基础版）

- 考核方案配置：KPI指标设置、权重分配、评分规则
- 考核流程：自评 → 上级评 → 隔级评 → HR汇总
- 考核周期：月度/季度/年度
- 考核结果：分数、等级（S/A/B/C/D）、绩效系数
- 考核结果关联薪酬

### 2.2 数据库设计（oa_hr库）

```sql
-- 员工档案主表
CREATE TABLE hr_employee (
    employee_id    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       DEFAULT NULL COMMENT '关联系统用户ID',
    emp_no         VARCHAR(20)  NOT NULL COMMENT '工号',
    name           VARCHAR(30)  NOT NULL COMMENT '姓名',
    gender         CHAR(1)      DEFAULT '0' COMMENT '性别',
    birthday       DATE         DEFAULT NULL COMMENT '出生日期',
    id_card        VARCHAR(18)  DEFAULT '' COMMENT '身份证号',
    nation         VARCHAR(20)  DEFAULT '' COMMENT '民族',
    native_place   VARCHAR(50)  DEFAULT '' COMMENT '籍贯',
    political_status VARCHAR(10) DEFAULT '' COMMENT '政治面貌',
    marital_status CHAR(1)      DEFAULT '0' COMMENT '婚姻状况',
    phone          VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    email          VARCHAR(50)  DEFAULT '' COMMENT '邮箱',
    emergency_contact VARCHAR(20) DEFAULT '' COMMENT '紧急联系人',
    emergency_phone VARCHAR(20)  DEFAULT '' COMMENT '紧急联系人电话',
    address        VARCHAR(200) DEFAULT '' COMMENT '现住址',
    dept_id        BIGINT       DEFAULT NULL COMMENT '部门ID',
    dept_name      VARCHAR(50)  DEFAULT '' COMMENT '部门名称',
    post_id        BIGINT       DEFAULT NULL COMMENT '岗位ID',
    post_name      VARCHAR(50)  DEFAULT '' COMMENT '岗位名称',
    rank           VARCHAR(20)  DEFAULT '' COMMENT '职级',
    entry_date     DATE         DEFAULT NULL COMMENT '入职日期',
    regular_date   DATE         DEFAULT NULL COMMENT '转正日期',
    contract_end   DATE         DEFAULT NULL COMMENT '合同到期日',
    superior_id    BIGINT       DEFAULT NULL COMMENT '直接上级ID',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0在职 1试用期 2离职)',
    leave_date     DATE         DEFAULT NULL COMMENT '离职日期',
    leave_reason   VARCHAR(200) DEFAULT '' COMMENT '离职原因',
    avatar         VARCHAR(200) DEFAULT '' COMMENT '照片',
    education      VARCHAR(10)  DEFAULT '' COMMENT '最高学历',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (employee_id),
    UNIQUE KEY uk_emp_no (emp_no)
) ENGINE=InnoDB COMMENT='员工档案表';

-- 教育经历
CREATE TABLE hr_education (
    education_id   BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    school         VARCHAR(100) NOT NULL COMMENT '学校',
    major          VARCHAR(50)  DEFAULT '' COMMENT '专业',
    degree         VARCHAR(10)  DEFAULT '' COMMENT '学历(专科/本科/硕士/博士)',
    start_date     DATE         DEFAULT NULL,
    end_date       DATE         DEFAULT NULL,
    PRIMARY KEY (education_id),
    KEY idx_employee (employee_id)
) ENGINE=InnoDB COMMENT='教育经历表';

-- 工作经历
CREATE TABLE hr_work_experience (
    experience_id  BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    company        VARCHAR(100) NOT NULL COMMENT '公司名称',
    position       VARCHAR(50)  DEFAULT '' COMMENT '职位',
    start_date     DATE         DEFAULT NULL,
    end_date       DATE         DEFAULT NULL,
    description    VARCHAR(500) DEFAULT '' COMMENT '工作描述',
    PRIMARY KEY (experience_id),
    KEY idx_employee (employee_id)
) ENGINE=InnoDB COMMENT='工作经历表';

-- 考勤组
CREATE TABLE hr_attendance_group (
    group_id       BIGINT       NOT NULL AUTO_INCREMENT,
    group_name     VARCHAR(50)  NOT NULL COMMENT '考勤组名称',
    work_start     TIME         NOT NULL COMMENT '上班时间',
    work_end       TIME         NOT NULL COMMENT '下班时间',
    late_threshold INT          DEFAULT 0 COMMENT '迟到阈值(分钟)',
    early_threshold INT         DEFAULT 0 COMMENT '早退阈值(分钟)',
    elastic_minutes INT         DEFAULT 0 COMMENT '弹性时间(分钟)',
    work_days      VARCHAR(20)  DEFAULT '1,2,3,4,5' COMMENT '工作日(1-7逗号分隔)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (group_id)
) ENGINE=InnoDB COMMENT='考勤组表';

-- 考勤组-员工关联
CREATE TABLE hr_attendance_group_member (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    group_id       BIGINT       NOT NULL,
    employee_id    BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee (employee_id)
) ENGINE=InnoDB COMMENT='考勤组成员表';

-- 打卡记录
CREATE TABLE hr_attendance_record (
    record_id      BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    emp_no         VARCHAR(20)  DEFAULT '',
    emp_name       VARCHAR(30)  DEFAULT '',
    attendance_date DATE        NOT NULL COMMENT '考勤日期',
    clock_in       DATETIME     DEFAULT NULL COMMENT '上班打卡时间',
    clock_out      DATETIME     DEFAULT NULL COMMENT '下班打卡时间',
    clock_in_ip    VARCHAR(128) DEFAULT '' COMMENT '上班打卡IP',
    clock_out_ip   VARCHAR(128) DEFAULT '' COMMENT '下班打卡IP',
    clock_in_location VARCHAR(200) DEFAULT '' COMMENT '上班GPS位置',
    clock_out_location VARCHAR(200) DEFAULT '' COMMENT '下班GPS位置',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1迟到 2早退 3迟到+早退 4缺卡 5旷工 6请假 7出差 8休息)',
    late_minutes   INT          DEFAULT 0 COMMENT '迟到分钟数',
    early_minutes  INT          DEFAULT 0 COMMENT '早退分钟数',
    work_hours     DECIMAL(4,1) DEFAULT 0 COMMENT '工作时长(小时)',
    overtime_hours DECIMAL(4,1) DEFAULT 0 COMMENT '加班时长(小时)',
    group_id       BIGINT       DEFAULT NULL COMMENT '考勤组ID',
    remark         VARCHAR(200) DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_emp_date (employee_id, attendance_date),
    KEY idx_date (attendance_date)
) ENGINE=InnoDB COMMENT='打卡记录表';

-- 月度考勤汇总
CREATE TABLE hr_attendance_monthly (
    monthly_id     BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    emp_no         VARCHAR(20)  DEFAULT '',
    emp_name       VARCHAR(30)  DEFAULT '',
    dept_name      VARCHAR(50)  DEFAULT '',
    year_month     VARCHAR(7)   NOT NULL COMMENT '年月(2026-05)',
    should_days    INT          DEFAULT 0 COMMENT '应出勤天数',
    actual_days    INT          DEFAULT 0 COMMENT '实际出勤天数',
    late_count     INT          DEFAULT 0 COMMENT '迟到次数',
    early_count    INT          DEFAULT 0 COMMENT '早退次数',
    absent_count   INT          DEFAULT 0 COMMENT '旷工天数',
    leave_days     DECIMAL(4,1) DEFAULT 0 COMMENT '请假天数',
    overtime_hours DECIMAL(6,1) DEFAULT 0 COMMENT '加班时长',
    business_days  DECIMAL(4,1) DEFAULT 0 COMMENT '出差天数',
    create_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (monthly_id),
    UNIQUE KEY uk_emp_month (employee_id, year_month)
) ENGINE=InnoDB COMMENT='月度考勤汇总表';

-- 假期类型
CREATE TABLE hr_leave_type (
    type_id        BIGINT       NOT NULL AUTO_INCREMENT,
    type_name      VARCHAR(30)  NOT NULL COMMENT '类型名称',
    type_code      VARCHAR(30)  NOT NULL COMMENT '类型编码',
    quota_rule     CHAR(1)      DEFAULT '0' COMMENT '额度规则(0无限制 1年度额度 2按月额度 3一次性)',
    default_quota  DECIMAL(4,1) DEFAULT 0 COMMENT '默认额度(天)',
    is_paid        CHAR(1)      DEFAULT '1' COMMENT '带薪(Y/N)',
    need_proof     CHAR(1)      DEFAULT 'N' COMMENT '需要证明(Y/N)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (type_id),
    UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB COMMENT='假期类型表';

-- 假期余额
CREATE TABLE hr_leave_balance (
    balance_id     BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    type_id        BIGINT       NOT NULL,
    year           INT          NOT NULL COMMENT '年度',
    total_quota    DECIMAL(6,1) DEFAULT 0 COMMENT '总额度(天)',
    used_quota     DECIMAL(6,1) DEFAULT 0 COMMENT '已用(天)',
    remaining      DECIMAL(6,1) DEFAULT 0 COMMENT '剩余(天)',
    expire_date    DATE         DEFAULT NULL COMMENT '过期日期',
    PRIMARY KEY (balance_id),
    UNIQUE KEY uk_emp_type_year (employee_id, type_id, year)
) ENGINE=InnoDB COMMENT='假期余额表';

-- 请假申请
CREATE TABLE hr_leave_apply (
    apply_id       BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    emp_no         VARCHAR(20)  DEFAULT '',
    emp_name       VARCHAR(30)  DEFAULT '',
    dept_name      VARCHAR(50)  DEFAULT '',
    type_id        BIGINT       NOT NULL COMMENT '假期类型ID',
    type_name      VARCHAR(30)  DEFAULT '',
    start_time     DATETIME     NOT NULL COMMENT '开始时间',
    end_time       DATETIME     NOT NULL COMMENT '结束时间',
    duration       DECIMAL(4,1) NOT NULL COMMENT '时长(天)',
    reason         VARCHAR(500) NOT NULL COMMENT '请假事由',
    attachment     VARCHAR(500) DEFAULT '' COMMENT '证明附件',
    process_instance_id BIGINT  DEFAULT NULL COMMENT '流程实例ID',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0待审批 1审批中 2已通过 3已拒绝 4已撤销)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (apply_id),
    KEY idx_employee (employee_id),
    KEY idx_status_time (status, create_time)
) ENGINE=InnoDB COMMENT='请假申请表';

-- 薪资项目
CREATE TABLE hr_salary_item (
    item_id        BIGINT       NOT NULL AUTO_INCREMENT,
    item_name      VARCHAR(50)  NOT NULL COMMENT '项目名称',
    item_code      VARCHAR(30)  NOT NULL COMMENT '项目编码',
    item_type      CHAR(1)      NOT NULL COMMENT '类型(0收入 1扣款)',
    calc_type      CHAR(1)      DEFAULT '0' COMMENT '计算方式(0固定 1公式)',
    calc_formula   VARCHAR(200) DEFAULT '' COMMENT '计算公式',
    is_taxable     CHAR(1)      DEFAULT 'Y' COMMENT '计税(Y/N)',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (item_id)
) ENGINE=InnoDB COMMENT='薪资项目表';

-- 薪资记录
CREATE TABLE hr_salary_record (
    record_id      BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    emp_no         VARCHAR(20)  DEFAULT '',
    emp_name       VARCHAR(30)  DEFAULT '',
    dept_name      VARCHAR(50)  DEFAULT '',
    year_month     VARCHAR(7)   NOT NULL COMMENT '薪资月份',
    items_json     TEXT         NOT NULL COMMENT '薪资明细JSON',
    gross_salary   DECIMAL(12,2) DEFAULT 0 COMMENT '应发工资',
    deductions     DECIMAL(12,2) DEFAULT 0 COMMENT '扣除合计',
    net_salary     DECIMAL(12,2) DEFAULT 0 COMMENT '实发工资',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0草稿 1已审批 2已发放)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_emp_month (employee_id, year_month)
) ENGINE=InnoDB COMMENT='薪资记录表';

-- 培训课程
CREATE TABLE hr_training_course (
    course_id      BIGINT       NOT NULL AUTO_INCREMENT,
    course_name    VARCHAR(100) NOT NULL COMMENT '课程名称',
    lecturer       VARCHAR(30)  DEFAULT '' COMMENT '讲师',
    training_date  DATE         DEFAULT NULL COMMENT '培训日期',
    start_time     TIME         DEFAULT NULL,
    end_time       TIME         DEFAULT NULL,
    location       VARCHAR(200) DEFAULT '' COMMENT '地点',
    capacity       INT          DEFAULT 0 COMMENT '名额',
    enrolled       INT          DEFAULT 0 COMMENT '已报名',
    description    TEXT         DEFAULT NULL,
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0报名中 1已结束 2已取消)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (course_id)
) ENGINE=InnoDB COMMENT='培训课程表';

-- 培训报名
CREATE TABLE hr_training_enrollment (
    enrollment_id  BIGINT       NOT NULL AUTO_INCREMENT,
    course_id      BIGINT       NOT NULL,
    employee_id    BIGINT       NOT NULL,
    emp_name       VARCHAR(30)  DEFAULT '',
    enroll_time    DATETIME     DEFAULT NULL,
    attend_flag    CHAR(1)      DEFAULT '0' COMMENT '签到(0未签到 1已签到)',
    score          DECIMAL(5,1) DEFAULT NULL COMMENT '评分',
    PRIMARY KEY (enrollment_id),
    UNIQUE KEY uk_course_emp (course_id, employee_id)
) ENGINE=InnoDB COMMENT='培训报名表';
```

---

## 3. 财务管理

财务管理聚焦费用管控，与审批引擎深度结合，实现财务流程规范化。

### 3.1 功能清单

#### 3.1.1 费用预算管理

**预算编制：**
- 按年度/部门/科目编制预算
- 预算科目树：一级科目（办公费、差旅费、招待费、培训费等）→ 二级科目
- 预算审批流程：部门提交 → 财务审核 → 总经理审批
- 支持预算调整（需审批）

**预算执行控制：**
- 报销/付款申请时自动校验预算余额
- 预算超支预警（达80%提醒、达100%拦截）
- 预算执行率实时统计
- 部门预算看板

#### 3.1.2 费用报销

**报销流程：**
```
员工填报 → 直属主管审批 → 部门负责人审批（超金额阈值）→ 财务审核 → 出纳付款
```

**报销单：**
| 字段 | 说明 |
|------|------|
| 报销类型 | 差旅费、办公费、交通费、招待费、培训费、其他 |
| 费用明细 | 日期、费用科目、金额、发票张数、备注 |
| 发票信息 | 发票号码、代码、金额、发票图片（OCR自动识别） |
| 关联出差 | 可关联出差审批单 |
| 预算校验 | 自动检查部门预算余额 |
| 合计金额 | 明细自动汇总 |

**费用标准控制（内置规则引擎）：**
- 差旅标准：按城市等级（一线/二线/三线）+ 员工职级 → 住宿/交通/餐饮标准
- 自动校验：超出标准自动标红，需额外说明理由
- 灵活配置：标准规则可在系统配置中调整

**发票管理：**
- 发票图片上传
- 发票OCR识别（预留，可手动录入）
- 发票验真（对接税务API，预留）
- 发票去重：同一发票号不可重复报销

#### 3.1.3 付款申请

- 供应商付款申请：关联采购订单/合同
- 付款计划：一次性/分期
- 付款审批流程
- 付款记录跟踪

#### 3.1.4 借款管理

- 借款申请：金额、用途、预计还款日期
- 借款审批流程
- 借款台账：未还款借款一览
- 还款冲销：报销时可选择冲抵未还借款
- 逾期提醒：超过预计还款日期自动提醒

### 3.2 数据库设计（oa_finance库）

```sql
-- 预算科目
CREATE TABLE fin_budget_subject (
    subject_id     BIGINT       NOT NULL AUTO_INCREMENT,
    subject_name   VARCHAR(50)  NOT NULL,
    subject_code   VARCHAR(30)  NOT NULL,
    parent_id      BIGINT       DEFAULT 0,
    order_num      INT          DEFAULT 0,
    status         CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (subject_id)
) ENGINE=InnoDB COMMENT='预算科目表';

-- 预算方案
CREATE TABLE fin_budget (
    budget_id      BIGINT       NOT NULL AUTO_INCREMENT,
    budget_name    VARCHAR(100) NOT NULL COMMENT '预算名称',
    fiscal_year    INT          NOT NULL COMMENT '年度',
    dept_id        BIGINT       DEFAULT NULL COMMENT '部门ID(空=全公司)',
    dept_name      VARCHAR(50)  DEFAULT '',
    total_amount   DECIMAL(14,2) DEFAULT 0 COMMENT '总金额',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0草稿 1已审批 2执行中 3已关闭)',
    process_instance_id BIGINT  DEFAULT NULL,
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (budget_id)
) ENGINE=InnoDB COMMENT='预算方案表';

-- 预算明细
CREATE TABLE fin_budget_detail (
    detail_id      BIGINT       NOT NULL AUTO_INCREMENT,
    budget_id      BIGINT       NOT NULL,
    subject_id     BIGINT       NOT NULL,
    dept_id        BIGINT       DEFAULT NULL,
    year_month     VARCHAR(7)   DEFAULT '' COMMENT '月份(空=全年)',
    budget_amount  DECIMAL(14,2) NOT NULL COMMENT '预算金额',
    used_amount    DECIMAL(14,2) DEFAULT 0 COMMENT '已用金额',
    frozen_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '冻结金额(审批中)',
    PRIMARY KEY (detail_id),
    KEY idx_budget (budget_id)
) ENGINE=InnoDB COMMENT='预算明细表';

-- 费用标准
CREATE TABLE fin_expense_standard (
    standard_id    BIGINT       NOT NULL AUTO_INCREMENT,
    expense_type   VARCHAR(30)  NOT NULL COMMENT '费用类型(travel_hotel/travel_transport/meal)',
    city_level     CHAR(1)      DEFAULT '0' COMMENT '城市等级(0一线 1二线 2三线)',
    rank           VARCHAR(20)  DEFAULT '' COMMENT '适用职级(空=全部)',
    standard_amount DECIMAL(10,2) NOT NULL COMMENT '标准金额',
    unit           VARCHAR(10)  DEFAULT '次' COMMENT '单位(次/天)',
    description    VARCHAR(200) DEFAULT '',
    status         CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (standard_id)
) ENGINE=InnoDB COMMENT='费用标准表';

-- 报销单
CREATE TABLE fin_reimbursement (
    reimburse_id   BIGINT       NOT NULL AUTO_INCREMENT,
    reimburse_no   VARCHAR(30)  NOT NULL COMMENT '报销单号',
    applicant_id   BIGINT       NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(30)  NOT NULL,
    dept_id        BIGINT       DEFAULT NULL,
    dept_name      VARCHAR(50)  DEFAULT '',
    reimburse_type VARCHAR(30)  NOT NULL COMMENT '报销类型',
    total_amount   DECIMAL(12,2) NOT NULL COMMENT '报销总额',
    invoice_count  INT          DEFAULT 0 COMMENT '发票张数',
    travel_id      BIGINT       DEFAULT NULL COMMENT '关联出差ID',
    description    VARCHAR(500) DEFAULT '',
    process_instance_id BIGINT  DEFAULT NULL,
    payment_status CHAR(1)      DEFAULT '0' COMMENT '付款状态(0未付 1已付)',
    payment_time   DATETIME     DEFAULT NULL,
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0草稿 1审批中 2已通过 3已拒绝 4已撤回 5已付款)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (reimburse_id),
    UNIQUE KEY uk_no (reimburse_no)
) ENGINE=InnoDB COMMENT='报销单表';

-- 报销明细
CREATE TABLE fin_reimburse_detail (
    detail_id      BIGINT       NOT NULL AUTO_INCREMENT,
    reimburse_id   BIGINT       NOT NULL,
    expense_date   DATE         NOT NULL COMMENT '费用日期',
    subject_id     BIGINT       DEFAULT NULL COMMENT '费用科目ID',
    subject_name   VARCHAR(50)  DEFAULT '',
    amount         DECIMAL(12,2) NOT NULL COMMENT '金额',
    invoice_no     VARCHAR(50)  DEFAULT '' COMMENT '发票号',
    invoice_code   VARCHAR(50)  DEFAULT '' COMMENT '发票代码',
    invoice_amount DECIMAL(12,2) DEFAULT 0 COMMENT '发票金额',
    invoice_url    VARCHAR(500) DEFAULT '' COMMENT '发票图片',
    remark         VARCHAR(200) DEFAULT '',
    is_over_standard CHAR(1)    DEFAULT 'N' COMMENT '超标(Y/N)',
    over_reason    VARCHAR(200) DEFAULT '' COMMENT '超标原因',
    PRIMARY KEY (detail_id),
    KEY idx_reimburse (reimburse_id)
) ENGINE=InnoDB COMMENT='报销明细表';

-- 借款单
CREATE TABLE fin_loan (
    loan_id        BIGINT       NOT NULL AUTO_INCREMENT,
    loan_no        VARCHAR(30)  NOT NULL,
    applicant_id   BIGINT       NOT NULL,
    applicant_name VARCHAR(30)  NOT NULL,
    dept_name      VARCHAR(50)  DEFAULT '',
    loan_amount    DECIMAL(12,2) NOT NULL COMMENT '借款金额',
    purpose        VARCHAR(500) NOT NULL COMMENT '借款用途',
    expected_date  DATE         NOT NULL COMMENT '预计还款日期',
    repaid_amount  DECIMAL(12,2) DEFAULT 0 COMMENT '已还金额',
    process_instance_id BIGINT  DEFAULT NULL,
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0草稿 1审批中 2已通过 3部分还款 4已还清 5已拒绝)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (loan_id),
    UNIQUE KEY uk_no (loan_no)
) ENGINE=InnoDB COMMENT='借款单表';
```

---

## 4. 资产管理

实现资产全流程数字化管理，覆盖购置、入库、领用、变更、盘点、报废。

### 4.1 功能清单

| 功能 | 说明 |
|------|------|
| 资产分类 | 多级分类树（电子设备、办公家具、交通工具等） |
| 资产台账 | 资产编号、名称、分类、规格、购入日期、原值、净值、使用人、存放位置、状态 |
| 资产入库 | 新购资产录入，关联采购审批单 |
| 资产领用 | 员工申请领用 → 审批 → 确认领用 |
| 资产归还 | 员工归还资产 → 管理员确认 |
| 资产变更 | 使用人变更、位置变更、状态变更，变更记录留痕 |
| 资产维修 | 维修申请、维修记录、维修费用 |
| 资产盘点 | 创建盘点任务 → 扫码/手动盘点 → 差异处理 |
| 资产报废 | 报废申请 → 审批 → 资产状态更新 |
| 资产统计 | 资产总览、分类统计、部门分布、折旧报表 |

### 4.2 数据库设计（oa_asset库）

```sql
-- 资产分类
CREATE TABLE asset_category (
    category_id    BIGINT       NOT NULL AUTO_INCREMENT,
    category_name  VARCHAR(50)  NOT NULL,
    parent_id      BIGINT       DEFAULT 0,
    order_num      INT          DEFAULT 0,
    status         CHAR(1)      DEFAULT '0',
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (category_id)
) ENGINE=InnoDB COMMENT='资产分类表';

-- 资产台账
CREATE TABLE asset_info (
    asset_id       BIGINT       NOT NULL AUTO_INCREMENT,
    asset_no       VARCHAR(30)  NOT NULL COMMENT '资产编号(自动生成)',
    asset_name     VARCHAR(100) NOT NULL COMMENT '资产名称',
    category_id    BIGINT       DEFAULT NULL COMMENT '分类ID',
    category_name  VARCHAR(50)  DEFAULT '',
    spec           VARCHAR(200) DEFAULT '' COMMENT '规格型号',
    brand          VARCHAR(50)  DEFAULT '' COMMENT '品牌',
    unit           VARCHAR(10)  DEFAULT '个' COMMENT '单位',
    purchase_date  DATE         DEFAULT NULL COMMENT '购入日期',
    original_value DECIMAL(12,2) DEFAULT 0 COMMENT '原值',
    net_value      DECIMAL(12,2) DEFAULT 0 COMMENT '净值',
    depreciation_years INT      DEFAULT 0 COMMENT '折旧年限',
    current_user_id BIGINT     DEFAULT NULL COMMENT '使用人ID',
    current_user_name VARCHAR(30) DEFAULT '' COMMENT '使用人',
    current_dept_id BIGINT     DEFAULT NULL COMMENT '使用部门',
    current_dept_name VARCHAR(50) DEFAULT '',
    location       VARCHAR(200) DEFAULT '' COMMENT '存放位置',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0闲置 1在用 2维修中 3已报废)',
    source_type    CHAR(1)      DEFAULT '0' COMMENT '来源(0采购 1捐赠 2调拨)',
    supplier       VARCHAR(100) DEFAULT '' COMMENT '供应商',
    warranty_date  DATE         DEFAULT NULL COMMENT '保修到期日',
    remark         VARCHAR(500) DEFAULT '',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (asset_id),
    UNIQUE KEY uk_asset_no (asset_no)
) ENGINE=InnoDB COMMENT='资产台账表';

-- 资产领用/归还记录
CREATE TABLE asset_operation (
    operation_id   BIGINT       NOT NULL AUTO_INCREMENT,
    asset_id       BIGINT       NOT NULL,
    asset_no       VARCHAR(30)  DEFAULT '',
    asset_name     VARCHAR(100) DEFAULT '',
    operation_type CHAR(1)      NOT NULL COMMENT '操作类型(0领用 1归还 2调拨 3维修 4报废)',
    operator_id    BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(30)  DEFAULT '',
    from_dept_id   BIGINT       DEFAULT NULL COMMENT '原部门',
    to_dept_id     BIGINT       DEFAULT NULL COMMENT '新部门',
    remark         VARCHAR(500) DEFAULT '',
    process_instance_id BIGINT  DEFAULT NULL,
    operate_time   DATETIME     DEFAULT NULL,
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (operation_id),
    KEY idx_asset (asset_id)
) ENGINE=InnoDB COMMENT='资产操作记录表';

-- 盘点任务
CREATE TABLE asset_inventory (
    inventory_id   BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_name VARCHAR(100) NOT NULL COMMENT '盘点名称',
    inventory_date DATE         NOT NULL COMMENT '盘点日期',
    scope_type     CHAR(1)      DEFAULT '0' COMMENT '范围(0全部 1按部门 2按分类)',
    scope_value    VARCHAR(200) DEFAULT '' COMMENT '范围值',
    total_count    INT          DEFAULT 0 COMMENT '资产总数',
    checked_count  INT          DEFAULT 0 COMMENT '已盘数量',
    diff_count     INT          DEFAULT 0 COMMENT '差异数量',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0进行中 1已完成)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (inventory_id)
) ENGINE=InnoDB COMMENT='盘点任务表';

-- 盘点明细
CREATE TABLE asset_inventory_detail (
    detail_id      BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_id   BIGINT       NOT NULL,
    asset_id       BIGINT       NOT NULL,
    asset_no       VARCHAR(30)  DEFAULT '',
    asset_name     VARCHAR(100) DEFAULT '',
    book_user      VARCHAR(30)  DEFAULT '' COMMENT '账面使用人',
    book_location  VARCHAR(200) DEFAULT '' COMMENT '账面位置',
    actual_user    VARCHAR(30)  DEFAULT '' COMMENT '实际使用人',
    actual_location VARCHAR(200) DEFAULT '' COMMENT '实际位置',
    result         CHAR(1)      DEFAULT '0' COMMENT '结果(0一致 1不一致 2盘亏 3盘盈)',
    remark         VARCHAR(200) DEFAULT '',
    check_time     DATETIME     DEFAULT NULL,
    checker        VARCHAR(30)  DEFAULT '',
    PRIMARY KEY (detail_id),
    KEY idx_inventory (inventory_id)
) ENGINE=InnoDB COMMENT='盘点明细表';
```

---

## 5. 合同管理

构建合同全生命周期管控体系，是法务风控的核心工具。

### 5.1 功能清单

| 功能 | 说明 |
|------|------|
| 合同模板 | 标准合同模板管理，支持变量占位符自动填充 |
| 合同起草 | 基于模板或空白创建，填写合同信息 |
| 合同审批 | 合同内容审批 + 法务审核 + 领导审批 |
| 电子签章 | 集成电子签章服务（预留接口） |
| 履约跟踪 | 合同执行进度、里程碑、交付物管理 |
| 收付款计划 | 合同关联的收付款节点和金额 |
| 到期提醒 | 到期前N天自动提醒相关人员 |
| 变更管理 | 合同变更申请、审批、记录 |
| 合同归档 | 合同文件归档存储、版本管理 |
| 合同台账 | 全部合同一览，多维度查询统计 |

### 5.2 数据库设计（oa_contract库）

```sql
-- 合同模板
CREATE TABLE contract_template (
    template_id    BIGINT       NOT NULL AUTO_INCREMENT,
    template_name  VARCHAR(100) NOT NULL,
    template_type  VARCHAR(30)  DEFAULT '' COMMENT '合同类型(采购/销售/服务/劳动/保密/其他)',
    content        LONGTEXT     COMMENT '模板内容(HTML)',
    variables      TEXT         DEFAULT NULL COMMENT '变量定义JSON',
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (template_id)
) ENGINE=InnoDB COMMENT='合同模板表';

-- 合同信息
CREATE TABLE contract_info (
    contract_id    BIGINT       NOT NULL AUTO_INCREMENT,
    contract_no    VARCHAR(30)  NOT NULL COMMENT '合同编号(自动生成)',
    contract_name  VARCHAR(200) NOT NULL COMMENT '合同名称',
    contract_type  VARCHAR(30)  NOT NULL COMMENT '类型(采购/销售/服务/劳动/保密/其他)',
    template_id    BIGINT       DEFAULT NULL COMMENT '关联模板',
    our_party      VARCHAR(100) DEFAULT '' COMMENT '我方',
    counterpart    VARCHAR(100) NOT NULL COMMENT '对方',
    counterpart_contact VARCHAR(30) DEFAULT '' COMMENT '对方联系人',
    counterpart_phone VARCHAR(20) DEFAULT '' COMMENT '对方电话',
    amount         DECIMAL(14,2) DEFAULT 0 COMMENT '合同金额',
    sign_date      DATE         DEFAULT NULL COMMENT '签订日期',
    start_date     DATE         DEFAULT NULL COMMENT '生效日期',
    end_date       DATE         DEFAULT NULL COMMENT '到期日期',
    owner_id       BIGINT       NOT NULL COMMENT '负责人ID',
    owner_name     VARCHAR(30)  NOT NULL,
    dept_id        BIGINT       DEFAULT NULL,
    dept_name      VARCHAR(50)  DEFAULT '',
    content        LONGTEXT     COMMENT '合同内容(HTML)',
    attachment     VARCHAR(500) DEFAULT '' COMMENT '合同扫描件',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0草稿 1审批中 2已签订 3履行中 4已完成 5已终止)',
    process_instance_id BIGINT  DEFAULT NULL,
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT '',
    update_time    DATETIME     DEFAULT NULL,
    del_flag       CHAR(1)      DEFAULT '0',
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (contract_id),
    UNIQUE KEY uk_no (contract_no),
    KEY idx_status_end (status, end_date)
) ENGINE=InnoDB COMMENT='合同信息表';

-- 收付款计划
CREATE TABLE contract_payment_plan (
    plan_id        BIGINT       NOT NULL AUTO_INCREMENT,
    contract_id    BIGINT       NOT NULL,
    plan_name      VARCHAR(100) NOT NULL COMMENT '节点名称',
    plan_type      CHAR(1)      NOT NULL COMMENT '类型(0收款 1付款)',
    plan_amount    DECIMAL(14,2) NOT NULL,
    plan_date      DATE         NOT NULL COMMENT '计划日期',
    actual_amount  DECIMAL(14,2) DEFAULT 0 COMMENT '实际金额',
    actual_date    DATE         DEFAULT NULL COMMENT '实际日期',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0未执行 1部分执行 2已完成)',
    remark         VARCHAR(200) DEFAULT '',
    PRIMARY KEY (plan_id),
    KEY idx_contract (contract_id)
) ENGINE=InnoDB COMMENT='收付款计划表';

-- 履约里程碑
CREATE TABLE contract_milestone (
    milestone_id   BIGINT       NOT NULL AUTO_INCREMENT,
    contract_id    BIGINT       NOT NULL,
    milestone_name VARCHAR(100) NOT NULL,
    plan_date      DATE         DEFAULT NULL,
    actual_date    DATE         DEFAULT NULL,
    deliverable    VARCHAR(500) DEFAULT '' COMMENT '交付物',
    completion     INT          DEFAULT 0 COMMENT '完成度(%)',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0未开始 1进行中 2已完成)',
    remark         VARCHAR(200) DEFAULT '',
    PRIMARY KEY (milestone_id),
    KEY idx_contract (contract_id)
) ENGINE=InnoDB COMMENT='履约里程碑表';
```

---

## 6. 前端新增页面

```
views/hr/
├── employee/index.vue          # 员工档案管理
├── employee/detail.vue         # 员工详情（基本信息/教育/工作经历Tab页）
├── attendance/
│   ├── record/index.vue        # 考勤记录
│   ├── group/index.vue         # 考勤组管理
│   ├── monthly/index.vue       # 月度汇总
│   └── appeal/index.vue        # 异常申诉
├── leave/
│   ├── apply/index.vue         # 请假申请
│   ├── balance/index.vue       # 假期余额
│   └── approve/index.vue       # 请假审批
├── overtime/index.vue          # 加班管理
├── travel/index.vue            # 出差管理
├── salary/
│   ├── item/index.vue          # 薪资项目配置
│   ├── record/index.vue        # 薪资记录
│   └── slip/index.vue          # 薪资条（员工视图）
├── training/
│   ├── course/index.vue        # 课程管理
│   └── enrollment/index.vue    # 我的培训
└── performance/index.vue       # 绩效考核

views/finance/
├── budget/
│   ├── index.vue               # 预算管理
│   └── detail.vue              # 预算执行详情
├── reimbursement/
│   ├── apply/index.vue         # 报销申请
│   ├── my/index.vue            # 我的报销
│   └── approve/index.vue       # 报销审批
├── payment/index.vue           # 付款申请
├── loan/index.vue              # 借款管理
└── standard/index.vue          # 费用标准

views/asset/
├── category/index.vue          # 资产分类
├── ledger/index.vue            # 资产台账
├── apply/index.vue             # 资产领用/归还
├── inventory/index.vue         # 资产盘点
├── scrap/index.vue             # 资产报废
└── stats/index.vue             # 资产统计

views/contract/
├── template/index.vue          # 合同模板
├── list/index.vue              # 合同列表
├── detail/index.vue            # 合同详情
├── payment/index.vue           # 收付款计划
└── stats/index.vue             # 合同统计
```

---

## 7. 成功标准

- 人事档案能完整管理员工全生命周期信息
- 考勤打卡能自动识别异常（迟到/早退/旷工）并生成月度汇总
- 请假申请能自动校验假期余额，审批通过后自动扣减
- 报销申请能自动校验费用标准和预算余额
- 资产能从入库到报废全流程跟踪，盘点有差异处理
- 合同能管理全生命周期，到期自动提醒
- 所有业务流程都能对接第二阶段的审批引擎
