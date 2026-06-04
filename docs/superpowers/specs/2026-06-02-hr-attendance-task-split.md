# HR 考勤管理子模块重构与任务拆分设计方案

> 日期: 2026-06-02
> 子模块范围: 考勤管理 (Attendance) — 考勤组规则、班制管理、打卡定位与校验、考勤异常扫描、补卡申诉审批、考勤统计报表
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`
> 试点参考: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 子模块说明与目标

考勤管理（`attendance`）是企业日常行政管理与人力资源的核心模块，直接关系到员工的考勤纪律评价和工薪（Payroll）计算。本次重构的考勤子模块将废弃旧有简单的 `oa_attendance` 系列表，完全重构为包含考勤排班规则、异常判定算法和考勤结果统计的一体化闭环体系。

### 1.1 考勤班制支持
考勤系统需要具备高度的灵活性，以支持企业内不同岗位的多样化出勤要求：
*   **固定班制（Fixed Shift）**：适用于规律作息的行政/后勤人员。可指定工作日（如周一到周五），每天具有固定的上下班时间点（如 09:00 签到，18:00 签退），包含固定的迟到和早退宽限时长。
*   **弹性班制（Flexible Shift）**：适用于研发、设计等弹性工时人员。仅规定每日须出勤的总时长（如 8 小时），或允许在特定时间窗口内弹性打卡（例如：上午 08:30-10:00 之间打卡上班，下班时间顺延 9 小时，包含 1 小时休息）。
*   **排班制（Scheduled Shift）**：适用于客服、工厂车间或轮值人员。考勤组成员的班次每日可能不同，通过在考勤组规则中配置“轮班周期规律”（如上二休二，Day 1/2 对应班次A，Day 3/4 休息）或上传排班日历映射关系来动态判断当天的签到/签退要求。

### 1.2 考勤异常自动生成机制
考勤系统并非仅由员工打卡时做即时更新，而是建立在**打卡流水 + 判定引擎**的机制之上：
*   **实时更新**：员工打卡后，系统实时在 `hr_attendance` 表中写入或更新上班/下班打卡的时间与IP/GPS/BSSID等，并基于当前考勤组规则做出初次状态评估（正常、迟到、早退）。
*   **每日凌晨扫描任务**：每日凌晨 02:00，考勤异常判定引擎自动运行。它会扫描前一日的所有应出勤员工：
    *   若发现前一日有应出勤要求却无任何打卡，自动标记为“缺勤”，并往 `hr_attendance_exception` 表中生成一条 `absent` 类型的异常记录。
    *   若仅有上班打卡而无下班打卡，自动生成一条 `no_clock_out` 类型的异常记录。
    *   若仅有下班打卡而无上班打卡，自动生成一条 `no_clock_in` 类型的异常记录。
    *   若发生迟到或早退，且超过了宽限阈值，自动生成 `late` 或 `early_leave` 类型的异常记录。
*   **自动提醒**：生成异常后，判定引擎自动联动消息中台，向异常员工推送补卡/申诉待办任务。

### 1.3 跨模块联动机制
考勤子模块必须与其他 HR 业务及财务模块实现深度的数据交换和状态共享：
*   **请假申请联动**：员工请假申请审批通过后，工作流回调调用 `AttendanceService.markLeaveAttendance`，在指定日期段内自动将 `hr_attendance.work_status` 标记为 `leave`。对于已生成的考勤异常记录，将其状态置为 `ignored`（原因：请假消核）。支持半天请假逻辑（上午请假，下午仍需打卡）。
*   **出差申请联动**：员工出差申请审批通过后，自动调用 `AttendanceService.markTripAttendance`，将指定日期段内的出勤状态标记为 `business_trip`，免除当日的打卡考核，自动冲抵已生成的缺勤或漏打卡异常。
*   **外出申请联动**：外出通常指单日内的数小时临时离岗。判定引擎在扫描时，如果发现缺勤或早退，会自动查询员工当天是否有审批通过的外出记录，若外出时间能覆盖缺勤段，则自动纠正考勤结果为“正常外出”。
*   **加班申请联动**：考勤系统只承认员工有审批单的加班时长。在计算考勤月报表中的加班工时（`overtime_hours`）时，会将实际下班打卡超出标准下班时间的部分，与审批通过的加班申请单做取交集计算，避免 speculative 的“打卡蹭加班”现象。
*   **工薪核算联动**：每月 1 号生成上月归档的 `hr_attendance_stats`（月报表），发布 `AttendanceMonthStatsPublishedEvent`。薪资模块（`oa-finance`）消费该事件，拉取迟到次数、请假天数、缺勤天数等指标，代入薪资核算公式自动扣减当月绩效或基本工资。

---

## 2. 边界定义

为了保证系统实现的合理复杂度，避免 speculative 抽象，对考勤子模块做出如下明确边界划分：

### 2.1 包含范围
*   **GPS定位范围打卡校验**：用户通过移动端发起打卡时，将当前设备获取的 GPS 经纬度传入后端。后端读取当前所属考勤组白名单配置，使用**半正矢公式（Haversine Formula）**计算打卡点与预设办公点之间的球面物理距离，在指定半径（如 300 米）内方允许打卡，否则抛出越界异常。
*   **企业WiFi打卡校验**：支持连接指定企业无线AP（基于物理 MAC 地址，即 BSSID，格式如 `aa:bb:cc:dd:ee:ff`）打卡。后端直接比对传入的 BSSID 是否在考勤组 WiFi 白名单中。
*   **考勤异常补卡申诉工作流**：针对每一条考勤异常，员工可发起申诉/补卡。系统将启动 `attendance_appeal` 工作流，由直属上级审批。审批通过后，考勤判定引擎自动重算该日考勤，将异常状态改为已修复，并回填正常打卡记录。
*   **日报与月报统计报表**：全自动汇算与手动重算。

### 2.2 排除范围
*   **考勤机物理硬件直连**：排除指纹仪、人脸识别闸机硬件通过 TCP/串口等直接回传数据。本模块打卡流水仅支持移动端（H5/微信小程序）定位打卡、Web端打卡。若后续引入考勤机硬件，需另写数据接收中间件写入 `hr_attendance`。
*   **蓝牙打卡（Beacon）**：不实现基于蓝牙信标的定位打卡。
*   **人脸防作弊/防假定位物理校验**：本模块重点在于业务流程重构，对于 GPS 模拟器防作弊、打卡拍照人脸对比等底层硬防御逻辑，本次重构不包含，采用标准的客户端经纬度签名校验。

---

## 3. 核心数据模型

以下是考勤管理模块的 5 张核心数据表 DDL，基于企业级 MySQL 8.0 规范，包含主键、逻辑删除、索引和详尽备注。

### 3.1 DDL 建表语句

```sql
-- ============================================================================
-- 考勤管理子模块 DDL
-- Database: oa_system | Charset: utf8mb4 | Collation: utf8mb4_general_ci
-- MySQL 8.0+ | Engine: InnoDB
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1.1 考勤组排班规则表 (hr_attendance_group)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance_group`;
CREATE TABLE `hr_attendance_group` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考勤组ID',
  `group_name`          VARCHAR(100) NOT NULL                COMMENT '考勤组名称',
  `group_type`          VARCHAR(32)  NOT NULL                COMMENT '考勤组类型(fixed=固定班制, flexible=弹性班制, schedule=排班制)',
  `rules`               JSON         NOT NULL                COMMENT '考勤规则配置JSON(包含打卡GPS范围、WiFi白名单BSSID、打卡提醒、加班计算规则等)',
  `fixed_work_days`     VARCHAR(64)  DEFAULT NULL            COMMENT '固定工作日(字符串逗号分隔, 如: "1,2,3,4,5" 代表周一到周五)',
  `fixed_shift_id`      BIGINT       DEFAULT NULL            COMMENT '固定班次ID(非固定班制或自定义班次为NULL)',
  `status`              CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤组排班规则表';

-- ---------------------------------------------------------------------------
-- 3.1.2 考勤组与员工关联表 (hr_attendance_group_emp)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance_group_emp`;
CREATE TABLE `hr_attendance_group_emp` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `group_id`            BIGINT       NOT NULL                COMMENT '考勤组ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `effect_start_date`   DATE         NOT NULL                COMMENT '生效开始日期',
  `effect_end_date`     DATE         DEFAULT NULL            COMMENT '生效结束日期(NULL代表永久生效)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_emp` (`group_id`, `emp_id`),
  KEY `idx_emp_effect` (`emp_id`, `effect_start_date`, `effect_end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤组与员工关联表';

-- ---------------------------------------------------------------------------
-- 3.1.3 考勤打卡记录表 (hr_attendance)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance`;
CREATE TABLE `hr_attendance` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考勤记录ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `work_date`           DATE         NOT NULL                COMMENT '考勤日期',
  `group_id`            BIGINT       DEFAULT NULL            COMMENT '使用考勤组ID',
  `clock_in_time`       DATETIME     DEFAULT NULL            COMMENT '上班打卡时间',
  `clock_out_time`      DATETIME     DEFAULT NULL            COMMENT '下班打卡时间',
  `clock_in_status`     VARCHAR(32)  NOT NULL DEFAULT 'none' COMMENT '上班打卡状态(normal=正常, late=迟到, absent=缺勤, none=未打卡)',
  `clock_out_status`    VARCHAR(32)  NOT NULL DEFAULT 'none' COMMENT '下班打卡状态(normal=正常, early_leave=早退, absent=缺勤, none=未打卡)',
  `work_status`         VARCHAR(32)  NOT NULL DEFAULT 'absent' COMMENT '出勤状态(present=正常出勤, absent=缺勤, leave=请假, business_trip=出差, outing=外出)',
  `clock_in_ip`         VARCHAR(64)  DEFAULT NULL            COMMENT '上班打卡IP',
  `clock_in_location`   VARCHAR(256) DEFAULT NULL            COMMENT '上班打卡地址/WiFi名称',
  `clock_in_gps`        VARCHAR(64)  DEFAULT NULL            COMMENT '上班打卡GPS坐标(经度,纬度)',
  `clock_in_bssid`      VARCHAR(64)  DEFAULT NULL            COMMENT '上班打卡WiFi BSSID',
  `clock_out_ip`        VARCHAR(64)  DEFAULT NULL            COMMENT '下班打卡IP',
  `clock_out_location`  VARCHAR(256) DEFAULT NULL            COMMENT '下班打卡地址/WiFi名称',
  `clock_out_gps`       VARCHAR(64)  DEFAULT NULL            COMMENT '下班打卡GPS坐标(经度,纬度)',
  `clock_out_bssid`     VARCHAR(64)  DEFAULT NULL            COMMENT '下班打卡WiFi BSSID',
  `remark`              VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date` (`emp_id`, `work_date`),
  KEY `idx_work_date` (`work_date`),
  KEY `idx_emp_status` (`emp_id`, `work_status`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤打卡记录表';

-- ---------------------------------------------------------------------------
-- 3.1.4 考勤异常记录与申诉表 (hr_attendance_exception)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance_exception`;
CREATE TABLE `hr_attendance_exception` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '异常ID',
  `emp_id`              BIGINT       NOT NULL                COMMENT '员工ID',
  `work_date`           DATE         NOT NULL                COMMENT '考勤日期',
  `exception_type`      VARCHAR(32)  NOT NULL                COMMENT '异常类型(late=迟到, early_leave=早退, no_clock_in=漏签到, no_clock_out=漏签退, absent=缺勤)',
  `shift_start`         DATETIME     NOT NULL                COMMENT '班次应签到时间',
  `shift_end`           DATETIME     NOT NULL                COMMENT '班次应签退时间',
  `actual_clock_in`     DATETIME     DEFAULT NULL            COMMENT '实际打卡签到时间',
  `actual_clock_out`    DATETIME     DEFAULT NULL            COMMENT '实际打卡签退时间',
  `exception_desc`      VARCHAR(256) DEFAULT NULL            COMMENT '异常情况说明',
  `status`              VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态(pending=待处理, appealed=申诉中, approved=申诉通过/已修复, rejected=申诉驳回, ignored=已忽略/请假消核)',
  `appeal_reason`       VARCHAR(500) DEFAULT NULL            COMMENT '申诉理由',
  `appeal_time`         DATETIME     DEFAULT NULL            COMMENT '申诉时间',
  `appeal_attachments`  JSON         DEFAULT NULL            COMMENT '申诉附件URL列表JSON',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '申诉工作流实例ID',
  `handler_id`          BIGINT       DEFAULT NULL            COMMENT '处理人ID',
  `handle_opinion`      VARCHAR(500) DEFAULT NULL            COMMENT '处理意见',
  `handle_time`         DATETIME     DEFAULT NULL            COMMENT '处理时间',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_date` (`emp_id`, `work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_process_instance` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤异常记录与申诉表';

-- ---------------------------------------------------------------------------
-- 3.1.5 考勤统计报表 (hr_attendance_stats)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `hr_attendance_stats`;
CREATE TABLE `hr_attendance_stats` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '员工ID',
  `dept_id`             BIGINT          NOT NULL                COMMENT '部门ID',
  `report_type`         VARCHAR(16)     NOT NULL                COMMENT '统计报表类型(daily=日报, monthly=月报)',
  `stat_period`         VARCHAR(32)     NOT NULL                COMMENT '统计时间段(日报: "yyyy-MM-dd", 月报: "yyyy-MM")',
  `work_days`           DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '应出勤天数',
  `actual_work_days`    DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '实际出勤天数',
  `late_count`          INT             NOT NULL DEFAULT 0      COMMENT '迟到次数',
  `late_minutes`        INT             NOT NULL DEFAULT 0      COMMENT '迟到总分钟数',
  `early_leave_count`   INT             NOT NULL DEFAULT 0      COMMENT '早退次数',
  `early_leave_minutes` INT             NOT NULL DEFAULT 0      COMMENT '早退总分钟数',
  `absent_days`         DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '缺勤天数',
  `leave_days`          DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '请假天数',
  `business_trip_days`  DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '出差天数',
  `outing_days`         DECIMAL(4,1)    NOT NULL DEFAULT 0.0    COMMENT '外出天数',
  `overtime_hours`      DECIMAL(6,2)    NOT NULL DEFAULT 0.00   COMMENT '加班工时/小时',
  `exception_count`     INT             NOT NULL DEFAULT 0      COMMENT '异常未处理次数',
  `details`             JSON            DEFAULT NULL            COMMENT '多维度明细配置(如请假类型明细统计)',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_type_period` (`emp_id`, `report_type`, `stat_period`),
  KEY `idx_dept_period` (`dept_id`, `report_type`, `stat_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考勤统计报表';
```

### 3.2 索引设计与查询场景

| 查询场景 | 依赖索引 | EXPLAIN 验收要求 |
| :--- | :--- | :--- |
| 获取指定员工当天的打卡记录 | `uk_emp_date` | type=const, 毫秒级返回 |
| 获取某天全公司的所有打卡状态 | `idx_work_date` | type=ref |
| 查询员工处于生效期内的考勤组 | `idx_emp_effect` | type=range, 过滤生效时间段 |
| 月度/每日统计报表生成 | `idx_dept_period` | type=ref, 合并部门过滤场景 |
| 获取指定流程关联的申诉记录 | `idx_process_instance` | type=ref |

---

## 4. 状态枚举与数据约定

### 4.1 考勤组班制类型 `AttendanceGroupType`

| Code | 描述 |
| :--- | :--- |
| `FIXED` | 固定班制（指定星期和班次） |
| `FLEXIBLE` | 弹性班制（满足总工时限制） |
| `SCHEDULE` | 排班制（动态循环规律排班） |

### 4.2 考勤打卡状态 `AttendanceClockStatus`

| Code | 描述 |
| :--- | :--- |
| `NORMAL` | 正常（在标准时间或宽限期内打卡） |
| `LATE` | 迟到 |
| `EARLY_LEAVE` | 早退 |
| `ABSENT` | 缺勤（应打卡未打卡且无有效补卡） |
| `NONE` | 未打卡 |

### 4.3 每日出勤总体状态 `AttendanceWorkStatus`

| Code | 描述 |
| :--- | :--- |
| `PRESENT` | 正常出勤（正常打卡或补卡通过） |
| `ABSENT` | 缺勤 |
| `LEAVE` | 请假（被审批过的假期冲抵） |
| `BUSINESS_TRIP` | 出差 |
| `OUTING` | 外出（数小时的公务离岗） |

### 4.4 异常申诉审批状态 `AttendanceExceptionStatus`

| Code | 描述 |
| :--- | :--- |
| `PENDING` | 待处理（刚被引擎扫描出来的异常，或待申诉） |
| `APPEALED` | 申诉中（员工已提起补卡，工作流进行中） |
| `APPROVED` | 申诉通过（审批通过，考勤数据已重算修复） |
| `REJECTED` | 申诉驳回 |
| `IGNORED` | 忽略（由请假、出差等后置流程自动核销的异常） |

### 4.5 考勤组配置 rules JSON 字段约束结构示例如下：

```json
{
  "gps_locations": [
    {
      "name": "总部大楼",
      "latitude": 39.9087,
      "longitude": 116.3975,
      "radius": 300
    }
  ],
  "wifi_white_list": [
    {
      "name": "Office-VIP-5G",
      "bssid": "8c:a6:df:12:34:ab"
    }
  ],
  "allow_out_of_range": false,
  "late_allowance_minutes": 15,
  "early_leave_allowance_minutes": 15,
  "shifts": [
    {
      "shift_id": 1,
      "shift_name": "标准白班",
      "work_in_time": "09:00:00",
      "work_out_time": "18:00:00"
    }
  ],
  "schedule_rules": {
    "cycle_type": "week",
    "cycle_days": 7,
    "cycle_shifts": [1, 1, 1, 1, 1, null, null]
  }
}
```

---

## 5. API 契约

### 5.1 统一前缀与规范
*   前缀：`/api/hr/attendance`
*   统一响应：`{"code": 0, "message": "操作成功", "data": ...}`
*   鉴权头部：`Authorization: Bearer <JWT_TOKEN>`

### 5.2 员工端接口

| 方法 | 路径 | 权限码 | 说明 |
| :--- | :--- | :--- | :--- |
| POST | `/api/hr/attendance/clock` | `hr:attendance:clock` | 考勤打卡（传定位、WiFi和打卡类别） |
| GET | `/api/hr/attendance/my-today` | `hr:attendance:clock` | 获取员工今日打卡详情与限制规则 |
| GET | `/api/hr/attendance/my-history` | `hr:attendance:clock` | 查询我当前日期的打卡记录历史 |
| GET | `/api/hr/attendance/my-exceptions` | `hr:attendance:clock` | 获取我的考勤异常流水（供申请申诉补卡） |
| POST | `/api/hr/attendance/my-exceptions/{id}/appeal` | `hr:attendance:clock` | 提交考勤异常申诉/补卡申请 |
| GET | `/api/hr/attendance/my-stats` | `hr:attendance:clock` | 查看我个人的考勤月报、日报详情 |

### 5.3 管理员接口

| 方法 | 路径 | 权限码 | 说明 |
| :--- | :--- | :--- | :--- |
| GET | `/api/hr/attendance/groups` | `hr:attendance:group:query` | 考勤组条件分页列表查询 |
| POST | `/api/hr/attendance/groups` | `hr:attendance:group:manage` | 创建考勤组排班规则 |
| PUT | `/api/hr/attendance/groups/{id}` | `hr:attendance:group:manage` | 修改考勤组排班规则 |
| DELETE | `/api/hr/attendance/groups/{id}` | `hr:attendance:group:manage` | 删除考勤组规则 |
| POST | `/api/hr/attendance/groups/{id}/employees` | `hr:attendance:group:manage` | 批量关联/移出考勤组成员 |
| GET | `/api/hr/attendance/records` | `hr:attendance:record:query` | 查询全员考勤记录（支持按部门/工号/状态过滤） |
| GET | `/api/hr/attendance/exceptions` | `hr:attendance:exception:query` | 查看考勤异常记录 |
| POST | `/api/hr/attendance/exceptions/{id}/handle` | `hr:attendance:exception:handle` | 人工直接审批考勤异常/申诉结果 |
| GET | `/api/hr/attendance/stats` | `hr:attendance:stats:query` | 全员考勤日报与月报统计查询 |
| POST | `/api/hr/attendance/stats/recompute` | `hr:attendance:stats:query` | 手动发起某员工或部门的历史考勤重算 |

---

## 6. DTO / VO 设计

### 6.1 `AttendanceClockDTO`

| 字段 | 类型 | 校验 | 说明 |
| :--- | :--- | :--- | :--- |
| `clockType` | String | 必填, "in" 或 "out" | 打卡类别 |
| `longitude` | BigDecimal | 可选 | 当前打卡 GPS 经度 |
| `latitude` | BigDecimal | 可选 | 当前打卡 GPS 纬度 |
| `bssid` | String | 可选 | 当前打卡 WiFi BSSID (无线AP MAC地址) |
| `locationName` | String | 可选 | 定位到的打卡详细地址/AP热点名称 |
| `ip` | String | 必填 | 客户端网络 IP |

### 6.2 `AttendanceGroupCreateDTO`

| 字段 | 类型 | 校验 | 说明 |
| :--- | :--- | :--- | :--- |
| `groupName` | String | 必填, 2-100字符 | 考勤组名称 |
| `groupType` | String | 必填, fixed/flexible/schedule | 考勤组班制类型 |
| `rules` | String (JSON) | 必填 | 包含位置、WiFi、考勤时间规则详情的 JSON 串 |
| `fixedWorkDays` | String | 可选, 逗号分隔如 "1,2,3,4,5" | 固定班制的工作日范围 |
| `fixedShiftId` | Long | 可选 | 固定班次的 ID |

### 6.3 `AttendanceExceptionAppealDTO`

| 字段 | 类型 | 校验 | 说明 |
| :--- | :--- | :--- | :--- |
| `appealReason` | String | 必填, 5-500字符 | 申诉补充理由/备注说明 |
| `appealAttachments` | List\<String\> | 可选 | 申诉凭据图片或文件地址列表 |

### 6.4 `AttendanceRecordVO`

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 记录 ID |
| `empId` | Long | 员工 ID |
| `empName` | String | 员工姓名 |
| `workDate` | String | 工作日期 (yyyy-MM-dd) |
| `clockInTime` | String | 上班打卡时间 (yyyy-MM-dd HH:mm:ss) |
| `clockOutTime` | String | 下班打卡时间 (yyyy-MM-dd HH:mm:ss) |
| `clockInStatus` | String | 上班状态名称 |
| `clockOutStatus` | String | 下班状态名称 |
| `workStatus` | String | 当日出勤总体状态码 |
| `workStatusLabel` | String | 出勤状态显示(正常/请假/旷工) |

---

## 7. 新旧映射关系

本次重构对老旧设计存在全替换机制，必须注意新旧表的对齐：

| 旧数据表 | 新数据表 | 迁移策略 / 转换逻辑 |
| :--- | :--- | :--- |
| `oa_attendance` | `hr_attendance` | 废弃旧打卡表，新打卡记录表拆分为 `clock_in_status` 与 `clock_out_status` 双边机制，增加 WiFi BSSID 与 GPS 冗余。 |
| `oa_attendance_group` | `hr_attendance_group` | 废弃旧考勤组，由新 `hr_attendance_group` 提供完善的 rules JSON 存储，支持固定/弹性/排班三种班制形态。 |
| `oa_attendance_group_emp` | `hr_attendance_group_emp` | 废弃，迁移并加入 `effect_start_date` 与 `effect_end_date` 生效周期段控制。 |
| 无单独异常表 | `hr_attendance_exception` | 纯新增表。用于记录因为未打卡或打卡超标产生的需申诉工单，支持状态闭环。 |
| 无考勤统计表 | `hr_attendance_stats` | 纯新增表。避免以前通过 SQL 大量聚合函数实时计算报表的性能缺陷，现在均由异常判定引擎定期归算写入。 |

---

## 8. 任务波次拆分

考勤管理子模块的重构按照如下波次（Wave 1 至 Wave 5）逐步开展：

### Wave 1: 契约与基线

#### T1: 数据库与 API 契约设计
*   **目标**：定义考勤 5 张新表的 DDL 语句、各 API 路径、DTO/VO 的 Java 属性定义。
*   **路径**：`code/backend/sql/`、`docs/superpowers/specs/2026-06-02-hr-attendance-task-split.md`
*   **输入**：本重构任务书第 3-6 节规范，旧 `oa_attendance` 系列表模型。
*   **输出**：完成本设计文档，生成基线 `hr_attendance_contract.sql`。
*   **禁止修改**：不编写任何 Java 业务实现逻辑。
*   **验收**：在本地数据库可成功执行该 SQL 并通过 DDL 约束规范。

#### T2: 旧考勤功能受损分析
*   **目标**：排查系统中引用了老 `oa_attendance` 的类、Service 和前端视图，形成下线清单。
*   **路径**：`code/backend/oa-mapper/`、`code/backend/oa-service/`、`code/frontend/`
*   **输出**：在本项目文档中登记受影响的类，制定下线兼容计划。
*   **禁止修改**：不删除任何老代码，不变更旧接口。

---

### Wave 2: 后端核心实现

#### T3: 考勤 Entity / Mapper / Enum 开发
*   **目标**：使用 MyBatis-Plus 生成或手写新考勤五张表的 Entity 和 Mapper，配置逻辑删除与审计注解。
*   **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/{entity,mapper,enums}/`
*   **输出**：`HrAttendance.java` 等 5 个 Entity 和对应的 Mapper、对应的考勤状态枚举类。
*   **禁止修改**：不实现 Service 层的任何业务规则。
*   **验收**：`cd code/backend && mvn -pl oa-hr -am compile` 编译无错，完成基础 CRUD 单测。

#### T4: 考勤引擎业务 Service 开发
*   **目标**：实现考勤核心服务类。必须包含：
    1. 打卡定位 GPS 范围的球面距离计算。
    2. 打卡 AP WiFi MAC 校验。
    3. 获取员工生效期内所属考勤组策略。
    4. 单日考勤打卡状态写入逻辑。
*   **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/service/`
*   **输入**：T3 的 Mapper 与 Entity 基线。
*   **输出**：`HrAttendanceService`、`HrAttendanceGroupService` 接口与实现类，配套单元测试。
*   **禁止修改**：不开发 Controller 层，不接入定时扫描任务。
*   **验收**：`cd code/backend && mvn -pl oa-hr -am test` 通过打卡定位计算的单元测试。

#### T5: REST API 暴露与权限绑定
*   **目标**：开发考勤的 Controller 类，对齐 API 契约绑定的权限码注解。
*   **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/controller/`
*   **输出**：`HrAttendanceController` 等控制器类，Knife4j 注解补齐。
*   **禁止修改**：不改动老 `AttendanceController`。
*   **验收**：`cd code/backend && mvn -pl oa-hr,oa-web -am test`。

---

### Wave 3: 自动化判定与联动回调

#### T6: 考勤联动（请假/出差/外出/加班）接入
*   **目标**：在 `WorkflowCallbackDispatcher` 中绑定请假、出差等回调，事件发布时，自动重算和修改对应日期的打卡状态。
*   **路径**：`code/backend/oa-workflow/`、`code/backend/oa-hr/`
*   **输出**：考勤重算联动事件发布器，联动消核服务方法。
*   **验收**：单元测试模拟“员工提交请假单通过 -> 自动更新该日期考勤记录状态为 leave”。

#### T7: 考勤异常自动生成定时任务 (Cron)
*   **目标**：编写 Spring Task 定时扫描类，每天凌晨自动生成未打卡员工的异常流水，写入 `hr_attendance_exception`。
*   **路径**：`code/backend/oa-hr/src/main/java/cn/oa/hr/task/`
*   **输出**：`AttendanceScanTask.java`
*   **验收**：调用单测手动触发扫描任务，断言未打卡员工在 exception 表中产生对应数据。

---

### Wave 4: 前端视图与接口迁移

#### T8: Web 管理端（考勤组配置与报表页面）
*   **目标**：重构后台管理端的考勤页面。包含考勤组排班新增与编辑表单（支持 GPS 地图标点、WiFi 输入）、考勤日报与月报展示、补卡申诉审核。
*   **路径**：`code/frontend/src/views/hr/attendance/`、`code/frontend/src/api/`
*   **验收**：`pnpm typecheck && pnpm build` 顺利编译完成。

#### T9: 移动端打卡及补卡申诉页面
*   **目标**：在 uni-app 移动端重构打卡页面。调用 `uni.getLocation` 获取 GPS，通过请求发送给新 API，实现打卡，并提供申诉提交界面。
*   **路径**：`code/mobile/src/pages/hr/attendance/`
*   **验收**：`pnpm build:h5` 编译打包无警告。

---

### Wave 5: 验证与下线切换

#### T10: E2E 考勤流程全链路闭环测试
*   **目标**：从“打卡越界被拦” -> “正常打卡” -> “未打卡凌晨扫描出异常” -> “发起申诉工作流” -> “审批通过自动消核重新计算考勤” -> “生成月度报表”的完整闭环流程测试。
*   **输出**：集成测试报告及演示视频截图。

#### T11: 旧考勤老代码下线与表结构废弃
*   **目标**：物理下线 `oa_attendance` 等旧表，安全地从 001_schema.sql 基线中剔除，移去后端旧的 controller 和 mapper 路由。
*   **验收**：重新启动整个应用无老接口加载，所有 CI 测试流水线全部通过。

---

## 9. 推荐执行顺序

```
Wave 1 (T1 + T2) -> Wave 2 (T3 -> T4 -> T5) -> Wave 3 (T6 + T7) -> Wave 4 (T8 + T9) -> Wave 5 (T10 -> T11)
```

---

## 10. 最小验收矩阵

| 模块 / 目标 | 验证命令 | 期望结果 |
| :--- | :--- | :--- |
| 后端 HR 模块 | `cd code/backend && mvn -pl oa-hr -am test` | 所有关于考勤定位范围、班制比对、请假消核的单测全部 Green |
| 后端 API 编译 | `cd code/backend && mvn -pl oa-hr,oa-web -am clean package -Dmaven.test.skip=true` | 后端顺利构建出最终 Jar 包，无依赖缺失 |
| Web 管理端 | `cd code/frontend && pnpm typecheck && pnpm build` | Web 管理端无 TypeScript 报错且打包通过 |
| 移动端 H5 | `cd code/mobile && pnpm build:h5` | 移动端编译打包通过 |

---

## 11. 约束与红线

### 11.1 通用红线
1.  **禁止在打卡校验逻辑中写死固定的 IP、BSSID 或经纬度**。必须完全由 `hr_attendance_group` 的 `rules` JSON 反序列化获取。
2.  **打卡范围距离计算必须使用半正矢公式（Haversine）**，绝对禁止使用欧氏距离（平直笛卡尔积）计算，避免经纬度偏差造成考勤范围漂移。
3.  **请假、出差、外出的标记不能直接物理删除原有打卡记录**，只能逻辑修改 `hr_attendance` 中的 `work_status` 状态字段并添加 `remark` 标记。
4.  **考勤统计的日报月报必须建立基于并发乐观锁的幂等保存**，防范定时任务因重复触发或并发调用导致的报表行冲突。

---

## 12. 跨模块协作

### 12.1 考勤模块发布的事件

考勤月度统计报表归档发布：
```java
package cn.oa.hr.event;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * 考勤月度统计数据发布事件，用于联动财务薪资扣减
 */
public record AttendanceMonthStatsPublishedEvent(
    Long empId,
    YearMonth month,
    BigDecimal shouldWorkDays,
    BigDecimal actualWorkDays,
    Integer lateCount,
    Integer earlyLeaveCount,
    BigDecimal absentDays,
    BigDecimal leaveDays,
    BigDecimal overtimeHours
) {}
```

---

## 13. Claude Code 可执行提示词

### 13.1 T1 契约与基线设计提示词

```text
请执行 HR 考勤管理子模块的 T1 任务：数据库与 API 契约设计。

必须阅读的文件：
- CLAUDE.md (项目架构规范)
- docs/superpowers/specs/2026-06-02-hr-attendance-task-split.md (考勤详细要求)
- code/backend/sql/baseline/001_schema.sql (原有表的风格参考)

工作范围：
1. 编写独立的 SQL 契约草案文件 `code/backend/sql/hr_attendance_contract.sql`。
2. 包含 hr_attendance_group、hr_attendance_group_emp、hr_attendance、hr_attendance_exception、hr_attendance_stats 五张表的标准建表 DDL。
3. 声明所有的主外键关系、逻辑删除 del_flag、索引设置、自增主键及注释。
4. 罗列完整的 API 契约表和对应操作所需的权限码。

警告：只编写 SQL 草案与文档，不编写任何 Java 业务代码与前端页面！完成之后报告更新的文件路径。
```

### 13.2 T3 实体与 Mapper 开发提示词

```text
请执行 HR 考勤管理子模块的 T3 任务：Entity、Mapper 和 Enum 开发。

前置条件：
- T1 数据库 SQL 契约草案已设计确认。

工作范围：
1. 依据 SQL 契约，在 `oa-hr` 模块的 `cn.oa.hr.entity` 下建立：
   - HrAttendance.java
   - HrAttendanceGroup.java
   - HrAttendanceGroupEmp.java
   - HrAttendanceException.java
   - HrAttendanceStats.java
2. 在 `cn.oa.hr.mapper` 下编写对应的 MyBatis-Plus Mapper 接口。
3. 在 `cn.oa.hr.enums` 下建立：
   - AttendanceGroupType
   - AttendanceClockStatus
   - AttendanceWorkStatus
   - AttendanceExceptionStatus
4. 配置通用审计字段自动填充注解。

限制：只在 `oa-hr` 模块下操作，不可实现 Service 和 Controller 逻辑，不改动前端。
验证：运行 `cd code/backend && mvn -pl oa-hr -am compile` 确保编译通过。
```

### 13.3 T4 考勤引擎业务 Service 开发提示词

```text
请执行 HR 考勤管理子模块的 T4 任务：考勤核心 Service 编写。

前置要求：
- T3 实体与 Mapper 编译通过。

工作范围：
1. 编写 `HrAttendanceService` 接口与其实现类。
2. 重点实现打卡核算逻辑：
   - `clock(AttendanceClockDTO dto, Long empId)`：根据考勤组校验 IP/BSSID 或 GPS（Haversine公式计算球面物理距离是否在半径内），判定本次打卡是 late、early_leave 还是 normal，写入 hr_attendance。
   - `getEffectiveGroup(Long empId, LocalDate date)`：获取员工当日生效考勤组。
3. 单元测试覆盖：
   - 打卡 GPS 距离正常与越界校验（测试 Haversine 公式计算的准确性）。
   - WiFi BSSID 匹配与拦截校验。
   - 弹性班制和固定班制下不同的迟到判定断言。

验证方式：运行 `cd code/backend && mvn -pl oa-hr -am test` 通过所有考勤判定测试。
```

### 13.4 T6-T7 考勤联动与扫描定时任务提示词

```text
请执行 HR 考勤管理子模块的 T6 和 T7 任务：考勤联动回调与异常扫描定时器开发。

工作范围：
1. 实现 `WorkflowCallbackDispatcher` 的回调适配：当请假或出差审批通过时，调用 `HrAttendanceService` 更新指定日期的出勤状态并忽略/关闭已产生的考勤异常单。
2. 编写 `AttendanceScanTask.java` 定时调度类：
   - 每天凌晨 02:00 对前一天应打卡却没有记录或打卡异常的员工自动往 `hr_attendance_exception` 生成异常数据。
   - 自动联动消息中台发送补卡消息。
3. 编写对此定时器和联动状态转换的单元/集成测试。

验证方式：运行 `cd code/backend && mvn -pl oa-workflow/oa-workflow-core,oa-hr,oa-web -am test` 确保流程回调和扫描机制测试全部通过。
```
