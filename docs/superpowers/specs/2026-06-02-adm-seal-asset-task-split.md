# 综合行政 (oa-admin) 重构实施与任务拆分

> 日期: 2026-06-02  
> 模块范围: 印章管理 / 办公用品 / 固定资产  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 第 3.4.3 节
> 参考模板: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 模块说明与目标

综合行政 (`oa-admin`) 是企业 OA 系统中负责"看得见、摸得着"实物资产与管控印章的核心模块，覆盖三类高频内部管理场景：

1. **印章管理 (Seal)**
   - 维护电子印章与物理印章的台账 (主键 `adm_seal`)。
   - 用印申请 -> 审批 -> 用印登记 -> 监控审计的完整闭环 (`adm_seal_usage`)。
   - 印章状态机：`ENABLED`（启用）、`DISABLED`（停用）、`LOST`（挂失）、`SCRAPPED`（报废）。
   - 物理印章与电子印章区分保管人 (keeperId) 与存放位置 (location)。

2. **办公用品 (Supply)**
   - 物品分类树 (`adm_supply_category`) 与物品台账 (`adm_supply`)。
   - 库存独立行 (`adm_supply_stock`)，使用 `version` 乐观锁防止超发。
   - 领用申请 (`adm_supply_request`) -> 审批 -> 出库 (`adm_supply_request_item`)。
   - 入库与出库均可走申请单完成原子扣减。

3. **固定资产 (Asset)**
   - 资产编号 (code) + SN/序列号 双唯一约束。
   - 全生命周期：`IDLE` (闲置) -> `IN_USE` (在用) -> `BORROWED` (借用) -> `MAINTAIN` (维修) -> `SCRAPPED` (报废)。
   - 领用、归还、调拨、盘点、折旧、报废六大操作，全部留痕于 `adm_asset_log`。
   - 借用单独建表 `adm_asset_borrow`（与领用不同，借用可跨部门、可设置归还日期）。

### 1.1 不在本期范围

| 不包含 | 原因 | 归属 |
|--------|------|------|
| 证照管理 | 强依赖员工档案（持证人、到期预警） | hr_employee 模块 |
| 名片印刷 | 频次极低，可后续合并 | 后续迭代 |
| 车辆管理 | 当前 OA 用户量小、需求零散 | 后续迭代 |
| 会议室预订 | 已有 `oa_meeting` 历史实现 | mt 模块 |
| 印章电子签章 (CA) 对接 | 涉及外部 UKey/数字证书 | 后续迭代 |
| Elasticsearch 搜索 | 库存/资产体量不足以支撑 ES | 平台统一评估 |

### 1.2 完成后应具备

1. `adm_seal` / `adm_seal_usage` / `adm_supply_category` / `adm_supply` / `adm_supply_stock` / `adm_supply_request` / `adm_supply_request_item` / `adm_asset_category` / `adm_asset` / `adm_asset_log` / `adm_asset_borrow` 共 11 张核心表结构与 seed。
2. `oa-admin` 模块内的印章、用品、资产业务 Service、Mapper、Controller。
3. 工作流引擎能启动印章用印、资产领用、用品领用三类审批。
4. 库存扣减使用 `version` 乐观锁，**不可出现负库存**。
5. Web 管理端能完成印章登记、用印审批、用品分类维护、库存出入库、资产台账、领用归还、盘点报废。
6. 后端单测、前端构建通过；库存并发测试通过。

---

## 2. 边界定义

### 2.1 本期包含

| 区域 | 内容 |
|------|------|
| 数据库 | 11 张核心表 (`adm_*`)，含分类、库存乐观锁、资产日志、借用 |
| 后端 | `oa-admin` 模块：Seal / Supply / Asset 三大子域的 Service/Controller |
| 工作流 | 用印申请 (seal_apply)、用品领用 (supply_apply)、资产领用 (asset_apply) 三种流程定义 |
| 通知 | 用印审批结果、资产领用到期提醒、库存低于阈值预警 |
| Web | 印章管理、用印申请、用品分类/库存、领用申请、资产台账、领用/归还/调拨/盘点/折旧/报废页面 |
| 测试 | Service 单测、库存并发测试、Controller 集成测试、关键前端构建 |

### 2.2 本期不包含

| 不包含 | 原因 |
|--------|------|
| 证照/合同/会议室 | 边界已划分至 hr/mt/fin 模块 |
| 车辆管理 | 需求未明确 |
| 第三方电子签章对接 | 涉及外部依赖 |
| 资产折旧自动计算 | 业务侧仍需手工折旧确认，本期提供公式字段与触发入口 |
| 名片印刷 | 频次低 |

### 2.3 与其他模块的接口

| 接口 | 方向 | 用途 |
|------|------|------|
| `sys_employee` | oa-admin 读 | 申请人/保管人/使用人 |
| `sys_dept` | oa-admin 读 | 部门数据权限、归属部门 |
| `oa-workflow` | oa-admin 调 | 启动用印/领用流程、回调业务状态 |
| `oa-message` | oa-admin 调 | 审批结果、库存预警、资产到期通知 |
| `oa-attendance` (后续) | oa-admin 调 | 资产领用人不影响考勤（无接口） |

---

## 3. 数据模型 DDL

> 文件: `code/backend/sql/adm_admin_contract.sql`  
> 数据库: `oa_system` / 字符集 `utf8mb4`  
> 逻辑删除: `del_flag` (0=未删, 1=已删)

### 3.1 印章与用印

#### `adm_seal` 印章主表

```sql
DROP TABLE IF EXISTS `adm_seal`;
CREATE TABLE `adm_seal` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seal_code`      VARCHAR(64)  NOT NULL COMMENT '印章编号 (业务编号)',
  `seal_name`      VARCHAR(128) NOT NULL COMMENT '印章名称',
  `seal_type`      VARCHAR(16)  NOT NULL COMMENT '印章类型: PHYSICAL-物理/ELECTRONIC-电子',
  `shape`          VARCHAR(16)  DEFAULT NULL COMMENT '形状: ROUND-圆形/SQUARE-方形/OVAL-椭圆/SPECIAL-特殊',
  `purpose`        VARCHAR(32)  DEFAULT NULL COMMENT '用途: COMPANY-公章/LEGAL-法人章/CONTRACT-合同章/FINANCE-财务章/PROJECT-项目章/PERSONAL-个人名章/OTHER-其他',
  `keeper_id`      BIGINT       DEFAULT NULL COMMENT '保管人 emp_id',
  `dept_id`        BIGINT       DEFAULT NULL COMMENT '保管部门 dept_id',
  `location`       VARCHAR(255) DEFAULT NULL COMMENT '存放位置/电子印章路径',
  `image_url`      VARCHAR(255) DEFAULT NULL COMMENT '印章图片URL(物理)或电子印章文件',
  `carve_date`     DATE         DEFAULT NULL COMMENT '刻制日期',
  `enable_date`    DATE         DEFAULT NULL COMMENT '启用日期',
  `scrap_date`     DATE         DEFAULT NULL COMMENT '报废日期',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED-启用/DISABLED-停用/LOST-挂失/SCRAPPED-报废',
  `remark`         VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_by`      BIGINT       DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      BIGINT       DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_seal_code` (`seal_code`),
  KEY `idx_adm_seal_keeper` (`keeper_id`),
  KEY `idx_adm_seal_dept` (`dept_id`),
  KEY `idx_adm_seal_type_status` (`seal_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='印章主表';
```

#### `adm_seal_usage` 用印记录表

```sql
DROP TABLE IF EXISTS `adm_seal_usage`;
CREATE TABLE `adm_seal_usage` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `usage_no`        VARCHAR(64)  NOT NULL COMMENT '用印单号',
  `seal_id`         BIGINT       NOT NULL COMMENT '印章ID',
  `applicant_id`    BIGINT       NOT NULL COMMENT '申请人 emp_id',
  `dept_id`         BIGINT       DEFAULT NULL COMMENT '申请部门',
  `document_name`   VARCHAR(255) NOT NULL COMMENT '用印文件名称',
  `document_type`   VARCHAR(32)  DEFAULT NULL COMMENT '文件类型: CONTRACT-合同/LETTER-函件/REPORT-报告/CERT-证书/OTHER-其他',
  `document_count`  INT          NOT NULL DEFAULT 1 COMMENT '文件份数',
  `purpose`         VARCHAR(500) NOT NULL COMMENT '用印用途',
  `usage_time`      DATETIME     DEFAULT NULL COMMENT '实际用印时间',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态: DRAFT-草稿/RUNNING-审批中/PASSED-通过/REJECTED-驳回/REVOKED-撤回/USING-已用印/CANCELLED-取消',
  `process_instance_id` BIGINT   DEFAULT NULL COMMENT '工作流实例ID',
  `current_task_id` BIGINT       DEFAULT NULL,
  `register_id`     BIGINT       DEFAULT NULL COMMENT '登记人/盖印操作人 emp_id',
  `register_time`   DATETIME     DEFAULT NULL COMMENT '登记时间',
  `attachment`      JSON         DEFAULT NULL COMMENT '用印文件附件',
  `return_required` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否需要归还原件 0-否 1-是',
  `returned_time`   DATETIME     DEFAULT NULL COMMENT '原件归还时间',
  `remark`          VARCHAR(500) DEFAULT NULL,
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`       BIGINT       DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       BIGINT       DEFAULT NULL,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_seal_usage_no` (`usage_no`),
  KEY `idx_adm_seal_usage_seal` (`seal_id`, `status`),
  KEY `idx_adm_seal_usage_applicant` (`applicant_id`, `create_time`),
  KEY `idx_adm_seal_usage_process` (`process_instance_id`),
  KEY `idx_adm_seal_usage_time` (`usage_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用印记录表';
```

### 3.2 办公用品

#### `adm_supply_category` 用品分类

```sql
DROP TABLE IF EXISTS `adm_supply_category`;
CREATE TABLE `adm_supply_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID, 0=根',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(128) NOT NULL COMMENT '分类名称',
  `sort_no`     INT          NOT NULL DEFAULT 0,
  `level`       TINYINT      NOT NULL DEFAULT 1 COMMENT '层级',
  `path`        VARCHAR(512) DEFAULT NULL COMMENT '祖先路径 1/2/3/',
  `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '启用 0-否 1-是',
  `remark`      VARCHAR(500) DEFAULT NULL,
  `del_flag`    TINYINT      NOT NULL DEFAULT 0,
  `create_by`   BIGINT       DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`   BIGINT       DEFAULT NULL,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_supply_cat_code` (`category_code`),
  KEY `idx_adm_supply_cat_parent` (`parent_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='办公用品分类';
```

#### `adm_supply` 用品主数据

```sql
DROP TABLE IF EXISTS `adm_supply`;
CREATE TABLE `adm_supply` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `supply_code`    VARCHAR(64)  NOT NULL COMMENT '物品编码',
  `supply_name`    VARCHAR(128) NOT NULL COMMENT '物品名称',
  `category_id`    BIGINT       NOT NULL COMMENT '分类ID',
  `specification`  VARCHAR(255) DEFAULT NULL COMMENT '规格',
  `unit`           VARCHAR(16)  NOT NULL DEFAULT '个' COMMENT '计量单位',
  `brand`          VARCHAR(64)  DEFAULT NULL COMMENT '品牌',
  `reference_price` DECIMAL(12,2) DEFAULT NULL COMMENT '参考单价',
  `image_url`      VARCHAR(255) DEFAULT NULL,
  `safety_stock`   INT          NOT NULL DEFAULT 0 COMMENT '安全库存(低于预警)',
  `enabled`        TINYINT      NOT NULL DEFAULT 1,
  `remark`         VARCHAR(500) DEFAULT NULL,
  `del_flag`       TINYINT      NOT NULL DEFAULT 0,
  `create_by`      BIGINT       DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      BIGINT       DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_supply_code` (`supply_code`),
  KEY `idx_adm_supply_category` (`category_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='办公用品主数据';
```

#### `adm_supply_stock` 库存表（乐观锁）

```sql
DROP TABLE IF EXISTS `adm_supply_stock`;
CREATE TABLE `adm_supply_stock` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `supply_id`     BIGINT       NOT NULL COMMENT '用品ID (单用品一行)',
  `warehouse`     VARCHAR(32)  NOT NULL DEFAULT 'DEFAULT' COMMENT '仓库/位置',
  `total_qty`     INT          NOT NULL DEFAULT 0 COMMENT '总数量(历史入库累计)',
  `available_qty` INT          NOT NULL DEFAULT 0 COMMENT '可用库存(待出库)',
  `locked_qty`    INT          NOT NULL DEFAULT 0 COMMENT '锁定中(审批中未出库)',
  `min_qty`       INT          NOT NULL DEFAULT 0 COMMENT '最低库存',
  `max_qty`       INT          NOT NULL DEFAULT 0 COMMENT '最高库存',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(并发控制)',
  `last_in_time`  DATETIME     DEFAULT NULL COMMENT '最近入库时间',
  `last_out_time` DATETIME     DEFAULT NULL COMMENT '最近出库时间',
  `del_flag`      TINYINT      NOT NULL DEFAULT 0,
  `create_by`     BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     BIGINT       DEFAULT NULL,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_supply_stock_supply_wh` (`supply_id`, `warehouse`),
  KEY `idx_adm_supply_stock_avail` (`available_qty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='办公用品库存(乐观锁防超发)';
```

#### `adm_supply_request` 用品领用申请

```sql
DROP TABLE IF EXISTS `adm_supply_request`;
CREATE TABLE `adm_supply_request` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `request_no`      VARCHAR(64)  NOT NULL COMMENT '申请单号',
  `request_type`    VARCHAR(16)  NOT NULL COMMENT '申请类型: OUT-领用(出库)/IN-入库',
  `applicant_id`    BIGINT       NOT NULL COMMENT '申请人 emp_id',
  `dept_id`         BIGINT       DEFAULT NULL COMMENT '申请部门',
  `reason`          VARCHAR(500) DEFAULT NULL COMMENT '领用/入库原因',
  `total_items`     INT          NOT NULL DEFAULT 0 COMMENT '总行项数',
  `total_qty`       INT          NOT NULL DEFAULT 0 COMMENT '总数量',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/PASSED/REJECTED/REVOKED/CLOSED-已完成',
  `process_instance_id` BIGINT   DEFAULT NULL,
  `current_task_id` BIGINT       DEFAULT NULL,
  `approved_by`     BIGINT       DEFAULT NULL COMMENT '最终审批人',
  `approved_time`   DATETIME     DEFAULT NULL,
  `warehouse`       VARCHAR(32)  NOT NULL DEFAULT 'DEFAULT' COMMENT '仓库/位置',
  `del_flag`        TINYINT      NOT NULL DEFAULT 0,
  `create_by`       BIGINT       DEFAULT NULL,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`       BIGINT       DEFAULT NULL,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_supply_req_no` (`request_no`),
  KEY `idx_adm_supply_req_applicant` (`applicant_id`, `status`),
  KEY `idx_adm_supply_req_status_time` (`status`, `create_time`),
  KEY `idx_adm_supply_req_process` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用品领用/入库申请单';
```

#### `adm_supply_request_item` 申请明细

```sql
DROP TABLE IF EXISTS `adm_supply_request_item`;
CREATE TABLE `adm_supply_request_item` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT,
  `request_id`   BIGINT   NOT NULL COMMENT '申请单ID',
  `supply_id`    BIGINT   NOT NULL COMMENT '用品ID',
  `supply_name`  VARCHAR(128) DEFAULT NULL COMMENT '冗余名称',
  `specification` VARCHAR(255) DEFAULT NULL COMMENT '冗余规格',
  `unit`         VARCHAR(16)  DEFAULT NULL COMMENT '冗余单位',
  `apply_qty`    INT      NOT NULL COMMENT '申请数量',
  `approved_qty` INT      NOT NULL DEFAULT 0 COMMENT '审批通过数量',
  `actual_qty`   INT      NOT NULL DEFAULT 0 COMMENT '实际出/入库数量',
  `remark`       VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_adm_supply_req_item_req` (`request_id`),
  KEY `idx_adm_supply_req_item_supply` (`supply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用品申请明细';
```

### 3.3 固定资产

#### `adm_asset_category` 资产分类

```sql
DROP TABLE IF EXISTS `adm_asset_category`;
CREATE TABLE `adm_asset_category` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`     BIGINT       NOT NULL DEFAULT 0,
  `category_code` VARCHAR(64)  NOT NULL,
  `category_name` VARCHAR(128) NOT NULL,
  `depreciation_method` VARCHAR(32) DEFAULT NULL COMMENT '折旧方法: STRAIGHT-直线法/DOUBLE_DECLINING-双倍余额/NO-不折旧',
  `useful_life_months` INT        DEFAULT NULL COMMENT '预计使用月数',
  `residual_rate` DECIMAL(5,4) DEFAULT 0.05 COMMENT '残值率',
  `sort_no`       INT          NOT NULL DEFAULT 0,
  `level`         TINYINT      NOT NULL DEFAULT 1,
  `path`          VARCHAR(512) DEFAULT NULL,
  `enabled`       TINYINT      NOT NULL DEFAULT 1,
  `remark`        VARCHAR(500) DEFAULT NULL,
  `del_flag`      TINYINT      NOT NULL DEFAULT 0,
  `create_by`     BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     BIGINT       DEFAULT NULL,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_asset_cat_code` (`category_code`),
  KEY `idx_adm_asset_cat_parent` (`parent_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产分类';
```

#### `adm_asset` 资产主表

```sql
DROP TABLE IF EXISTS `adm_asset`;
CREATE TABLE `adm_asset` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `asset_code`     VARCHAR(64)  NOT NULL COMMENT '资产编号(业务唯一)',
  `sn`             VARCHAR(128) DEFAULT NULL COMMENT '序列号/出厂编号',
  `asset_name`     VARCHAR(128) NOT NULL COMMENT '资产名称',
  `category_id`    BIGINT       NOT NULL COMMENT '资产分类ID',
  `brand`          VARCHAR(64)  DEFAULT NULL,
  `model`          VARCHAR(128) DEFAULT NULL COMMENT '型号',
  `specification`  VARCHAR(255) DEFAULT NULL,
  `unit`           VARCHAR(16)  DEFAULT '台',
  `purchase_price` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '购置金额',
  `current_value`  DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '当前净值',
  `depreciation_method` VARCHAR(32) DEFAULT 'STRAIGHT' COMMENT 'STRAIGHT/DOUBLE_DECLINING/NO',
  `useful_life_months` INT         DEFAULT 60,
  `residual_rate`  DECIMAL(5,4) DEFAULT 0.05,
  `purchase_date`  DATE         DEFAULT NULL,
  `start_use_date` DATE         DEFAULT NULL,
  `scrap_date`     DATE         DEFAULT NULL,
  `dept_id`        BIGINT       DEFAULT NULL COMMENT '所属部门',
  `location`       VARCHAR(255) DEFAULT NULL COMMENT '存放位置',
  `keeper_id`      BIGINT       DEFAULT NULL COMMENT '保管人',
  `current_user_id` BIGINT      DEFAULT NULL COMMENT '当前使用人',
  `status`         VARCHAR(16)  NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE-闲置/IN_USE-在用/BORROWED-借用中/MAINTAIN-维修中/SCRAPPED-已报废/LOST-盘亏',
  `supplier`       VARCHAR(255) DEFAULT NULL,
  `invoice_no`     VARCHAR(64)  DEFAULT NULL,
  `image_url`      VARCHAR(255) DEFAULT NULL,
  `remark`         VARCHAR(500) DEFAULT NULL,
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `del_flag`       TINYINT      NOT NULL DEFAULT 0,
  `create_by`      BIGINT       DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`      BIGINT       DEFAULT NULL,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_asset_code` (`asset_code`),
  UNIQUE KEY `uk_adm_asset_sn` (`sn`),
  KEY `idx_adm_asset_category` (`category_id`),
  KEY `idx_adm_asset_status` (`status`),
  KEY `idx_adm_asset_keeper` (`keeper_id`),
  KEY `idx_adm_asset_user` (`current_user_id`),
  KEY `idx_adm_asset_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定资产主表';
```

#### `adm_asset_log` 资产变动日志

```sql
DROP TABLE IF EXISTS `adm_asset_log`;
CREATE TABLE `adm_asset_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `asset_id`       BIGINT       NOT NULL,
  `operation`      VARCHAR(32)  NOT NULL COMMENT 'PURCHASE-购置/RECEIVE-领用/RETURN-归还/TRANSFER-调拨/INVENTORY-盘点/MAINTAIN-维修/SCRAP-报废/IDLE-闲置/LOSS-盘亏',
  `operator_id`    BIGINT       DEFAULT NULL COMMENT '操作人 emp_id',
  `from_user_id`   BIGINT       DEFAULT NULL COMMENT '原使用人',
  `to_user_id`     BIGINT       DEFAULT NULL COMMENT '新使用人',
  `from_dept_id`   BIGINT       DEFAULT NULL COMMENT '原部门',
  `to_dept_id`     BIGINT       DEFAULT NULL COMMENT '新部门',
  `from_location`  VARCHAR(255) DEFAULT NULL,
  `to_location`    VARCHAR(255) DEFAULT NULL,
  `from_status`    VARCHAR(16)  DEFAULT NULL,
  `to_status`      VARCHAR(16)  DEFAULT NULL,
  `change_value`   DECIMAL(14,2) DEFAULT NULL COMMENT '价值变动(折旧/报废)',
  `biz_id`         BIGINT       DEFAULT NULL COMMENT '关联业务ID(领用/借用/盘点单)',
  `biz_type`       VARCHAR(32)  DEFAULT NULL COMMENT '业务类型: BORROW/RECEIVE/INVENTORY/...',
  `operate_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark`         VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_adm_asset_log_asset` (`asset_id`, `operate_time`),
  KEY `idx_adm_asset_log_op` (`operator_id`),
  KEY `idx_adm_asset_log_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产变动日志(全生命周期留痕)';
```

#### `adm_asset_borrow` 资产借用

```sql
DROP TABLE IF EXISTS `adm_asset_borrow`;
CREATE TABLE `adm_asset_borrow` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `borrow_no`     VARCHAR(64)  NOT NULL COMMENT '借用单号',
  `asset_id`      BIGINT       NOT NULL,
  `borrower_id`   BIGINT       NOT NULL COMMENT '借用人 emp_id',
  `borrower_dept_id` BIGINT    DEFAULT NULL,
  `from_user_id`  BIGINT       DEFAULT NULL COMMENT '原使用人',
  `from_dept_id`  BIGINT       DEFAULT NULL,
  `borrow_date`   DATE         NOT NULL,
  `plan_return_date` DATE      NOT NULL COMMENT '计划归还日期',
  `actual_return_date` DATE   DEFAULT NULL,
  `purpose`       VARCHAR(500) DEFAULT NULL COMMENT '借用用途',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING-借用中/PASSED-审批通过/REJECTED-驳回/RETURNED-已归还/OVERDUE-逾期/REVOKED-撤回',
  `process_instance_id` BIGINT DEFAULT NULL,
  `current_task_id` BIGINT     DEFAULT NULL,
  `approver_id`   BIGINT       DEFAULT NULL,
  `approve_time`  DATETIME     DEFAULT NULL,
  `return_handler_id` BIGINT   DEFAULT NULL COMMENT '归还确认人',
  `return_remark` VARCHAR(500) DEFAULT NULL,
  `overdue_notified` TINYINT   NOT NULL DEFAULT 0 COMMENT '是否已发送逾期通知',
  `del_flag`      TINYINT      NOT NULL DEFAULT 0,
  `create_by`     BIGINT       DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`     BIGINT       DEFAULT NULL,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_adm_asset_borrow_no` (`borrow_no`),
  KEY `idx_adm_asset_borrow_asset` (`asset_id`),
  KEY `idx_adm_asset_borrow_borrower` (`borrower_id`, `status`),
  KEY `idx_adm_asset_borrow_overdue` (`plan_return_date`, `status`),
  KEY `idx_adm_asset_borrow_process` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产借用单';
```

### 3.4 索引与 EXPLAIN 验收

| 查询场景 | 推荐索引 | 验收 |
|----------|----------|------|
| 印章按保管人查 | `idx_adm_seal_keeper` | `EXPLAIN` 命中 |
| 用印按申请人/时间范围 | `idx_adm_seal_usage_applicant(create_time)` | 命中 |
| 用印按印章+状态 | `idx_adm_seal_usage_seal(status)` | 命中 |
| 用品分类树 | `idx_adm_supply_cat_parent` | 命中 |
| 库存低预警 | `idx_adm_supply_stock_avail` | 命中 |
| 领用申请审批列表 | `idx_adm_supply_req_status_time` | 命中 |
| 资产按状态+部门 | `idx_adm_asset_dept` + `idx_adm_asset_status` | 命中 |
| 资产日志按资产+时间 | `idx_adm_asset_log_asset(operate_time)` | 命中 |
| 借用逾期扫描 | `idx_adm_asset_borrow_overdue(plan_return_date, status)` | 命中 |

---

## 4. API 契约

> 统一前缀: `/api/admin`  
> 权限码: `adm:seal:*`、`adm:supply:*`、`adm:asset:*`

### 4.1 印章 (`/api/admin/seals`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/admin/seals` | `adm:seal:create` | 新增印章 |
| `PUT` | `/api/admin/seals/{id}` | `adm:seal:update` | 修改印章信息 |
| `GET` | `/api/admin/seals/{id}` | `adm:seal:detail` | 详情 |
| `GET` | `/api/admin/seals` | `adm:seal:list` | 分页查询 |
| `POST` | `/api/admin/seals/{id}/actions/disable` | `adm:seal:disable` | 停用印章 |
| `POST` | `/api/admin/seals/{id}/actions/enable` | `adm:seal:enable` | 启用印章 |
| `POST` | `/api/admin/seals/{id}/actions/lost` | `adm:seal:lost` | 挂失 |
| `POST` | `/api/admin/seals/{id}/actions/scrap` | `adm:seal:scrap` | 报废 |

### 4.2 用印申请 (`/api/admin/seal-usages`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/admin/seal-usages` | `adm:seal-usage:create` | 提交用印申请（启动工作流） |
| `GET` | `/api/admin/seal-usages` | `adm:seal-usage:list` | 分页查询 |
| `GET` | `/api/admin/seal-usages/{id}` | `adm:seal-usage:detail` | 详情 |
| `POST` | `/api/admin/seal-usages/{id}/actions/revoke` | `adm:seal-usage:revoke` | 申请人撤回 |
| `POST` | `/api/admin/seal-usages/{id}/actions/register` | `adm:seal-usage:register` | 审批通过后登记盖印 |
| `POST` | `/api/admin/seal-usages/{id}/actions/return` | `adm:seal-usage:return` | 归还原件 |

### 4.3 办公用品 (`/api/admin/supplies`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/admin/supply-categories` | `adm:supply-cat:list` | 分类树 |
| `POST` | `/api/admin/supply-categories` | `adm:supply-cat:create` | 新增分类 |
| `PUT` | `/api/admin/supply-categories/{id}` | `adm:supply-cat:update` | 修改分类 |
| `POST` | `/api/admin/supplies` | `adm:supply:create` | 新增用品 |
| `PUT` | `/api/admin/supplies/{id}` | `adm:supply:update` | 修改用品 |
| `GET` | `/api/admin/supplies` | `adm:supply:list` | 分页 |
| `GET` | `/api/admin/supplies/{id}` | `adm:supply:detail` | 详情 |
| `GET` | `/api/admin/supply-stocks` | `adm:supply-stock:list` | 库存查询 |
| `PUT` | `/api/admin/supply-stocks/{id}` | `adm:supply-stock:adjust` | 调整最低/最高库存 |
| `GET` | `/api/admin/supply-stocks/low-warning` | `adm:supply-stock:warning` | 低预警列表 |

### 4.4 领用/入库申请 (`/api/admin/supply-requests`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/admin/supply-requests` | `adm:supply-req:create` | 创建申请（OUT/IN） |
| `GET` | `/api/admin/supply-requests` | `adm:supply-req:list` | 分页 |
| `GET` | `/api/admin/supply-requests/{id}` | `adm:supply-req:detail` | 详情 |
| `POST` | `/api/admin/supply-requests/{id}/actions/revoke` | `adm:supply-req:revoke` | 撤回 |
| `POST` | `/api/admin/supply-requests/{id}/actions/close` | `adm:supply-req:close` | 审批通过后实际出/入库（库存原子扣减） |

### 4.5 资产 (`/api/admin/assets`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `GET` | `/api/admin/asset-categories` | `adm:asset-cat:list` | 分类树 |
| `POST` | `/api/admin/asset-categories` | `adm:asset-cat:create` | 新增分类 |
| `PUT` | `/api/admin/asset-categories/{id}` | `adm:asset-cat:update` | 修改 |
| `POST` | `/api/admin/assets` | `adm:asset:create` | 新增资产 |
| `PUT` | `/api/admin/assets/{id}` | `adm:asset:update` | 修改 |
| `GET` | `/api/admin/assets` | `adm:asset:list` | 分页（含 status/category/dept 过滤） |
| `GET` | `/api/admin/assets/{id}` | `adm:asset:detail` | 详情 |
| `POST` | `/api/admin/assets/{id}/actions/receive` | `adm:asset:receive` | 领用（写日志 + 改状态） |
| `POST` | `/api/admin/assets/{id}/actions/return` | `adm:asset:return` | 归还 |
| `POST` | `/api/admin/assets/{id}/actions/transfer` | `adm:asset:transfer` | 调拨 |
| `POST` | `/api/admin/assets/{id}/actions/maintain` | `adm:asset:maintain` | 进入维修 |
| `POST` | `/api/admin/assets/{id}/actions/maintain-finish` | `adm:asset:maintain-finish` | 维修完成 |
| `POST` | `/api/admin/assets/{id}/actions/scrap` | `adm:asset:scrap` | 报废 |
| `GET` | `/api/admin/assets/{id}/logs` | `adm:asset:log:view` | 资产变动日志 |
| `POST` | `/api/admin/assets/actions/inventory` | `adm:asset:inventory` | 批量盘点（按 dept/category 提交盘点结果） |
| `POST` | `/api/admin/assets/actions/depreciate` | `adm:asset:depreciate` | 触发折旧计算 |

### 4.6 借用 (`/api/admin/asset-borrows`)

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/admin/asset-borrows` | `adm:asset-borrow:create` | 创建借用（启动工作流） |
| `GET` | `/api/admin/asset-borrows` | `adm:asset-borrow:list` | 分页 |
| `GET` | `/api/admin/asset-borrows/{id}` | `adm:asset-borrow:detail` | 详情 |
| `POST` | `/api/admin/asset-borrows/{id}/actions/approve-return` | `adm:asset-borrow:return` | 归还确认 |
| `POST` | `/api/admin/asset-borrows/{id}/actions/revoke` | `adm:asset-borrow:revoke` | 撤回 |
| `GET` | `/api/admin/asset-borrows/overdue` | `adm:asset-borrow:overdue` | 逾期列表 |

### 4.7 工作流动作（沿用通用 API）

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/wf/tasks/{taskId}/actions/approve` | `workflow:task:approve` | 审批通过 |
| `POST` | `/api/wf/tasks/{taskId}/actions/reject` | `workflow:task:reject` | 驳回 |
| `POST` | `/api/wf/tasks/{taskId}/actions/transfer` | `workflow:task:transfer` | 转办 |

### 4.8 关键 DTO/VO 字段

#### `AdmSealCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `sealCode` | String | 必填，唯一 |
| `sealName` | String | 必填 |
| `sealType` | String | 必填，枚举 `PHYSICAL`/`ELECTRONIC` |
| `shape` | String | 可选 |
| `purpose` | String | 必填，枚举值 |
| `keeperId` | Long | 物理印章必填 |
| `deptId` | Long | 必填 |
| `location` | String | 物理印章必填 |
| `imageUrl` | String | 电子印章必填 |
| `carveDate` / `enableDate` | Date | 可选 |

#### `AdmSealUsageCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `sealId` | Long | 必填 |
| `documentName` | String | 必填 |
| `documentType` | String | 必填，枚举 |
| `documentCount` | Integer | 必填 ≥ 1 |
| `purpose` | String | 必填 |
| `returnRequired` | Boolean | 默认 false |
| `attachment` | List<FileRef> | 可选 |

#### `AdmSupplyRequestCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `requestType` | String | 必填，`OUT`/`IN` |
| `reason` | String | 可选 |
| `warehouse` | String | 默认 `DEFAULT` |
| `items` | List<AdmSupplyRequestItemDTO> | 必填，至少 1 条 |

#### `AdmSupplyRequestItemDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `supplyId` | Long | 必填 |
| `applyQty` | Integer | 必填 ≥ 1 |
| `remark` | String | 可选 |

#### `AdmAssetCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `assetCode` | String | 必填，唯一 |
| `sn` | String | 可选，唯一 |
| `assetName` | String | 必填 |
| `categoryId` | Long | 必填 |
| `purchasePrice` | BigDecimal | 必填 ≥ 0 |
| `depreciationMethod` | String | 默认 `STRAIGHT` |
| `usefulLifeMonths` | Integer | 必填 |
| `purchaseDate` | Date | 必填 |
| `startUseDate` | Date | 可选 |
| `deptId` | Long | 必填 |
| `location` | String | 必填 |
| `keeperId` | Long | 可选 |

#### `AdmAssetTransferDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `toUserId` | Long | 可选 |
| `toDeptId` | Long | 必填 |
| `toLocation` | String | 必填 |
| `reason` | String | 必填 |

#### `AdmAssetInventoryDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `scope` | String | `DEPT` / `CATEGORY` |
| `deptId` / `categoryId` | Long | 对应 scope 必填 |
| `inventoryDate` | Date | 必填 |
| `items` | List<AdmAssetInventoryItemDTO> | 必填 |

#### `AdmAssetBorrowCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `assetId` | Long | 必填 |
| `purpose` | String | 必填 |
| `borrowDate` | Date | 必填 |
| `planReturnDate` | Date | 必填 > `borrowDate` |

---

## 5. 任务波次

### Wave 1: 契约与基线

#### T1 数据库与 API 契约

| 字段 | 内容 |
|------|------|
| 目标 | 落地 11 张表 DDL、API 契约、权限码、索引验收说明 |
| 路径 | `code/backend/sql/adm_admin_contract.sql`、`docs/superpowers/specs/2026-06-02-oa-system-redesign.md` |
| 输入 | 重构文档 3.4.3 节、印章/用品/资产业务现状（如 `oa_seal`、`oa_asset`） |
| 输出 | DDL 文件、API 契约表、权限码清单、seed 草案 |
| 禁止修改 | 不实现 Service/Controller 业务逻辑 |
| 验收 | 文档列出接口、字段、索引、权限码、验收命令；DDL 在本地 MySQL 8.0 正常执行 |

#### T2 旧实现影响分析

| 字段 | 内容 |
|------|------|
| 目标 | 查清旧印章/用品/资产功能（如有）的 Entity/Mapper/Service/Controller/前端依赖 |
| 路径 | `code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、`code/frontend/src/api/`、`code/mobile/src/api/` |
| 输出 | 旧入口清单、迁移保留/替换/下线建议 |
| 禁止修改 | 不删除旧代码 |
| 验收 | 影响分析清单覆盖 Controller、Service、Mapper、菜单路由、API 文件 |

### Wave 2: 印章基础

#### T3 印章 Entity + Mapper（Wave 2 第 1 步）

| 字段 | 内容 |
|------|------|
| 目标 | 在 `oa-admin` 中建立 `AdmSeal`、`AdmSealUsage` 实体和 Mapper |
| 路径 | `code/backend/oa-admin`、`code/backend/oa-model`、`code/backend/oa-mapper` |
| 输出 | Entity、Mapper、基础查询方法、Mapper 测试 |
| 禁止修改 | 不动 workflow、message |
| 验收 | `cd code/backend && mvn -pl oa-admin -am test` 通过 |

#### T4 印章 Service（CRUD + 状态机）

| 字段 | 内容 |
|------|------|
| 目标 | 印章新增、修改、停用/启用/挂失/报废、查询 |
| 路径 | `code/backend/oa-admin/src/main/java/cn/oa/admin/seal` |
| 输出 | `SealService` 接口/实现、DTO/VO、状态机校验、单元测试 |
| 禁止修改 | 不启动工作流（用印流程在 T6） |
| 验收 | `mvn -pl oa-admin -am test` 通过；状态机非法转换测试通过 |

#### T5 印章 Controller

| 字段 | 内容 |
|------|------|
| 目标 | 暴露印章 CRUD + 状态切换 REST API |
| 路径 | `code/backend/oa-admin/src/main/java/cn/oa/admin/controller` |
| 输出 | `SealController`、OpenAPI 注解、权限码注解、Controller 测试 |
| 禁止修改 | 不复制旧实现大段逻辑 |
| 验收 | `mvn -pl oa-admin,oa-web -am test` 通过 |

### Wave 3: 用印申请与审批

#### T6 用印申请 Entity + Service

| 字段 | 内容 |
|------|------|
| 目标 | `AdmSealUsage` 业务：创建、列表、详情、撤回 |
| 路径 | `code/backend/oa-admin` |
| 输出 | Entity/Mapper/Service/单元测试 |
| 禁止修改 | 不实现审批（审批动作 T7） |
| 验收 | `mvn -pl oa-admin -am test` |

#### T7 用印流程 + 工作流回调

| 字段 | 内容 |
|------|------|
| 目标 | 提交用印申请启动 `seal_apply` 流程；审批通过/驳回/撤回回调业务状态 |
| 路径 | `code/backend/oa-workflow/oa-workflow-core`、`oa-admin` |
| 输出 | 工作流定义 seed（`wf_process_definition` 中 `processType='seal_apply'`）、回调 Handler、集成测试 |
| 禁止修改 | 不让 workflow 依赖 admin Service 实现类 |
| 验收 | `mvn -pl oa-workflow,oa-admin,oa-web -am test` 通过 |

#### T8 用印 Controller + 登记/归还

| 字段 | 内容 |
|------|------|
| 目标 | 暴露 `/api/admin/seal-usages` 全量接口、登记盖印、原件归还 |
| 路径 | `code/backend/oa-admin` |
| 输出 | Controller、OpenAPI、权限码、Controller 测试 |
| 验收 | `mvn -pl oa-admin,oa-web -am test` 通过 |

### Wave 4: 办公用品基础

#### T9 用品分类 + 用品 Entity + Mapper

| 字段 | 内容 |
|------|------|
| 目标 | `adm_supply_category`、`adm_supply` 实体/Mapper/Service/Controller |
| 路径 | `code/backend/oa-admin/supply` |
| 输出 | Entity、Mapper、Service、Controller、分类树接口、单元测试 |
| 禁止修改 | 不做库存扣减（库存独立 T10） |
| 验收 | `mvn -pl oa-admin -am test` 通过；分类树接口性能 OK（缓存可后续） |

#### T10 库存乐观锁 + 出入库原子操作

| 字段 | 内容 |
|------|------|
| 目标 | `adm_supply_stock` 实体/Mapper、Service 实现基于 `version` 的 `lockStock`、`unlockStock`、`debitStock`、`creditStock`、`adjustStock` |
| 路径 | `code/backend/oa-admin/supply` |
| 输出 | `SupplyStockService`（原子 SQL + 乐观锁）、`StockOpResult`、`StockOperationException`、并发测试（≥50 并发线程同 ID 扣减） |
| 禁止修改 | 不得在应用层先读后写（必须用 `UPDATE ... WHERE version=?`） |
| 验收 | 并发测试后 `available_qty` 永不为负；冲突时抛 `StockVersionConflictException` |

### Wave 5: 用品领用申请

#### T11 领用/入库申请 Service + 流程

| 字段 | 内容 |
|------|------|
| 目标 | `adm_supply_request` + `adm_supply_request_item`，启动 `supply_apply` 流程；通过后调用库存原子扣减/入库 |
| 路径 | `code/backend/oa-admin/supply`、`oa-workflow` |
| 输出 | Service、回调 Handler、审批通过自动出/入库逻辑、集成测试 |
| 禁止修改 | 库存扣减必须走 T10 的原子方法 |
| 验收 | 流程通过 -> 库存自动扣；流程驳回 -> 锁定回滚；`mvn -pl oa-admin,oa-workflow,oa-web -am test` |

#### T12 领用 Controller

| 字段 | 内容 |
|------|------|
| 目标 | 暴露 `/api/admin/supply-requests` 全量接口 |
| 路径 | `code/backend/oa-admin` |
| 输出 | Controller、OpenAPI、权限码、Controller 测试 |
| 验收 | `mvn -pl oa-admin,oa-web -am test` 通过 |

### Wave 6: 资产基础

#### T13 资产分类 + 资产 Entity + Mapper

| 字段 | 内容 |
|------|------|
| 目标 | `adm_asset_category`、`adm_asset` 实体/Mapper/Service（CRUD + 查询）/Controller |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | Entity、Mapper、Service、Controller、单元测试 |
| 禁止修改 | 不实现领用/调拨/盘点（独立任务 T14-T17） |
| 验收 | `mvn -pl oa-admin -am test` 通过 |

#### T14 资产领用 + 日志

| 字段 | 内容 |
|------|------|
| 目标 | 资产领用（写 `adm_asset_log` + 改 `current_user_id` + 状态 `IN_USE`） |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | `AssetReceiveService`、日志写入逻辑、Controller `actions/receive`、单元测试 |
| 验收 | 领用后 `current_user_id` / `status` / `log` 三者一致；`mvn -pl oa-admin -am test` |

### Wave 7: 资产调拨与归还

#### T15 调拨与归还

| 字段 | 内容 |
|------|------|
| 目标 | 调拨（跨部门换保管人）、归还（回到 `IDLE` 或换使用人） |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | Service、Controller `actions/transfer` / `actions/return`、单元测试 |
| 验收 | 调拨后 `dept_id` / `keeper_id` / `log` 一致；`mvn -pl oa-admin -am test` |

#### T16 资产借用

| 字段 | 内容 |
|------|------|
| 目标 | `adm_asset_borrow` Service + 流程 + 逾期扫描 |
| 路径 | `code/backend/oa-admin/asset`、`oa-workflow`、`oa-message` |
| 输出 | Entity/Mapper/Service、回调 Handler、定时任务扫描逾期（Quartz/Spring `@Scheduled`）、集成测试 |
| 验收 | 借用通过 -> 资产 `BORROWED`；归还 -> `IDLE`/`IN_USE`；逾期发送站内消息；`mvn -pl oa-admin,oa-workflow,oa-message,oa-web -am test` |

### Wave 8: 资产盘点

#### T17 盘点

| 字段 | 内容 |
|------|------|
| 目标 | 批量盘点：管理员提交范围（dept/category）+ 实物状态（盘盈/盘亏/正常），对比系统台账生成差异 |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | `InventoryService`、差异表（可用 `adm_asset_log` 的 `INVENTORY`/`LOSS` 操作记录）、Controller、单元测试 |
| 验收 | 盘点结果产生 `LOSS` 日志 + 状态 `LOST`；`mvn -pl oa-admin -am test` |

### Wave 9: 折旧与报废

#### T18 折旧计算

| 字段 | 内容 |
|------|------|
| 目标 | 实现 `STRAIGHT` 直线法、`DOUBLE_DECLINING` 双倍余额法；提供月触发入口 |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | `DepreciationService`、公式计算（按 `useful_life_months`）、`current_value` 更新、单元测试 |
| 验收 | 折旧后 `current_value >= purchase_price * residual_rate`；`mvn -pl oa-admin -am test` |

#### T19 报废

| 字段 | 内容 |
|------|------|
| 目标 | 报废：状态改 `SCRAPPED`、记 `scrap_date`、写日志 |
| 路径 | `code/backend/oa-admin/asset` |
| 输出 | Service、Controller `actions/scrap`、单元测试 |
| 验收 | 报废后资产不可再被领用/调拨；`mvn -pl oa-admin -am test` |

### Wave 10: 演示与通知

#### T20 消息通知联动

| 字段 | 内容 |
|------|------|
| 目标 | 库存低于 `safety_stock` -> 管理员站内消息；借用逾期 -> 使用人/审批人消息；资产报废/盘亏 -> 管理员消息 |
| 路径 | `code/backend/oa-admin`、`oa-message` |
| 输出 | `AdminNotifyService`、事件、集成测试 |
| 验收 | 触发场景后消息中心可见；`mvn -pl oa-admin,oa-message,oa-web -am test` |

#### T21 前端演示页

| 字段 | 内容 |
|------|------|
| 目标 | Web 端在 `/admin/seal`、`/admin/seal-usage`、`/admin/supply`、`/admin/asset` 接入新接口 |
| 路径 | `code/frontend/src/api/admin/`、`code/frontend/src/views/admin/` |
| 输出 | typed API、印章台账、用印申请/审批、用品分类/库存、领用申请、资产台账、领用/归还/调拨/盘点/折旧/报废页面 |
| 禁止修改 | 不做 monorepo 改造，不改全局布局 |
| 验收 | `cd code/frontend && pnpm typecheck && pnpm build` |

### Wave 11: 验证与下线准备

#### T22 端到端回归

| 字段 | 内容 |
|------|------|
| 目标 | 完整链路：新增印章 -> 用印申请 -> 审批 -> 登记 -> 还原件；新增用品 -> 入库 -> 领用 -> 出库；新增资产 -> 领用 -> 调拨 -> 盘点 -> 折旧 -> 报废 |
| 路径 | `code/backend/oa-admin/src/test` |
| 输出 | E2E 测试或集成测试覆盖三大闭环 |
| 验收 | 三大链路全部走通；`mvn -pl oa-admin,oa-workflow,oa-message,oa-web -am test` |

#### T23 旧入口下线清单

| 字段 | 内容 |
|------|------|
| 目标 | 标记旧印章/资产/用品接口、表的替换关系和下线时机 |
| 路径 | `docs/superpowers/specs/`、必要时旧代码注释 |
| 输出 | 下线清单、兼容策略、风险说明 |
| 禁止修改 | 未通过 E2E 前不删除旧代码 |
| 验收 | 清单包含旧路径、新路径、切换条件、回滚方式 |

---

## 6. 推荐执行顺序

```
Wave 1: T1 + T2 （并行起草）
Wave 2: T3 -> T4 -> T5                       # 印章基础 CRUD
Wave 3: T6 -> T7 -> T8                       # 用印申请 + 工作流
Wave 4: T9 -> T10                            # 用品分类 + 库存乐观锁（关键路径）
Wave 5: T11 -> T12                           # 用品领用流程
Wave 6: T13 -> T14                           # 资产基础 + 领用
Wave 7: T15 -> T16                           # 调拨归还 + 借用
Wave 8: T17                                  # 盘点
Wave 9: T18 -> T19                           # 折旧 + 报废
Wave 10: T20 -> T21                          # 消息 + 前端
Wave 11: T22 -> T23                          # E2E + 下线
```

T1/T2 完成前不得开始代码实现。T10 完成后，库存原子操作方法才允许被 T11 复用。T22 全部用例通过前，Web 端只可声明"演示版可用"，不得宣称"完整闭环"。

---

## 7. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| Admin 后端 | `cd code/backend && mvn -pl oa-admin -am test` |
| Admin + Web 入口 | `cd code/backend && mvn -pl oa-admin,oa-web -am test` |
| 工作流联动 | `cd code/backend && mvn -pl oa-workflow,oa-admin,oa-web -am test` |
| 消息联动 | `cd code/backend && mvn -pl oa-admin,oa-message,oa-web -am test` |
| 库存并发 | `cd code/backend && mvn -pl oa-admin -am test -Dtest=SupplyStockConcurrencyTest` |
| Web | `cd code/frontend && pnpm typecheck && pnpm build` |
| Mobile | 暂不要求（本期 Web 优先） |

---

## 8. 第一个可执行任务提示词

### 8.1 T1 提示词：数据库与 API 契约

```text
请执行 oa-admin 综合行政模块的 T1：数据库与 API 契约。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md（第 3.4.3 节）
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md（本文件）
- docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md（参考）

范围：
- 只允许修改文档和 SQL 草案。
- 不实现 Java/Vue 业务代码。

输出：
- code/backend/sql/adm_admin_contract.sql（11 张表 DDL 草案）
- 印章/用品/资产 三大子域的 API 契约表
- 权限码清单
- 索引和 EXPLAIN 验收说明
- 后续 T3/T4 需要的 DTO/VO 字段清单

完成后汇报改动文件和下一步建议。
```

### 8.2 T3 提示词：印章 Entity + Mapper（含用印记录）

```text
请执行 oa-admin 的 T3：印章与用印记录 Entity + Mapper。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md
- code/backend/sql/adm_admin_contract.sql

范围：
- 在 oa-admin 模块建立：
  * cn.oa.admin.seal.entity.AdmSeal / AdmSealUsage
  * cn.oa.admin.seal.mapper.AdmSealMapper / AdmSealUsageMapper
  * 基础查询方法（按 keeper、按 status、按 applicantId、分页）
- 不实现 Service 业务逻辑、不写 Controller。
- 不得新建 wf_*、msg_*、oa_seal 等旧表。

输出：
- 4 个 Java 源文件
- MyBatis-Plus BaseMapper 集成
- Mapper 测试：基础 CRUD + 索引命中（EXPLAIN）说明
- 单元测试覆盖建表后字段映射、@TableField 注解

验收：
cd code/backend && mvn -pl oa-admin -am test

完成后汇报改动文件和 T4 接口建议。
```

### 8.3 T4 提示词：库存原子操作（含乐观锁）

```text
请执行 oa-admin 的 T4：办公用品库存原子操作 Service（含乐观锁并发控制）。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md
- code/backend/sql/adm_admin_contract.sql（adm_supply_stock）
- 现有 oa-admin 已有 Entity/Mapper（如已实现）

范围：
- 在 cn.oa.admin.supply.service 实现 SupplyStockService 接口：
  * lockStock(supplyId, warehouse, qty)       锁定库存（领用申请审批中）
  * unlockStock(supplyId, warehouse, qty)     释放锁定（驳回/撤回）
  * debitStock(supplyId, warehouse, qty)      实际出库（locked -> 0）
  * creditStock(supplyId, warehouse, qty)     入库（total/available 增加）
  * adjustStock(supplyId, warehouse, deltaQty, reason) 管理员调整
- 必须基于 MyBatis-Plus 的 `update(..., where version=?)`，版本不一致抛 StockVersionConflictException。
- 单次更新 SQL 必须满足：UPDATE ... SET available_qty=available_qty+?, version=version+1 WHERE id=? AND version=? AND (available_qty + ? >= 0)（对 debit）。
- 单元测试覆盖：
  * 50 个并发线程同时 debit 同一行，最终 available_qty 永不为负
  * 冲突时抛 StockVersionConflictException
  * 锁定 -> 出库 -> 释放 的状态机转换

禁止：
- 应用层先 SELECT 再 UPDATE（必须用数据库 CAS）。
- 任何 @Transactional 包裹整个批量（必须单条 SQL 提交）。

验收：
cd code/backend && mvn -pl oa-admin -am test -Dtest=SupplyStockConcurrencyTest

完成后汇报：
- 冲突次数分布
- 性能（50 并发总耗时）
- T11 集成方法建议
```

### 8.4 T5 提示词：印章 Controller

```text
请执行 oa-admin 的 T5：印章 Controller。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md 第 4.1 节
- code/backend/sql/adm_admin_contract.sql
- 已有的 AdmSeal / AdmSealService

范围：
- 在 cn.oa.admin.controller.seal 实现：
  * SealController：CRUD + 状态切换（disable/enable/lost/scrap）
- 统一前缀 /api/admin/seals
- 权限码注解 @PreAuthorize("hasAuthority('adm:seal:create')")
- OpenAPI 注解完整
- 异常统一由 GlobalExceptionHandler 处理
- Controller 测试覆盖：路径、参数、权限、状态机非法转换（400）

验收：
cd code/backend && mvn -pl oa-admin,oa-web -am test
```

### 8.5 T11 提示词：用品领用流程 + 库存联动

```text
请执行 oa-admin 的 T11：用品领用/入库申请 Service + 工作流。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md
- 现有 SupplyStockService（T4 实现）

范围：
- 实现 SupplyRequestService：创建、详情、撤回、关闭（出/入库）
- 工作流 processType='supply_apply' seed
- 回调：审批通过 -> close() -> 调用 T4 库存方法（debitStock 或 creditStock）
- 审批驳回 -> 调用 unlockStock 回滚锁定

禁止：
- 不得在 Service 中直接 update stock，必须复用 T4 原子方法。

验收：
cd code/backend && mvn -pl oa-admin,oa-workflow,oa-web -am test
```

### 8.6 T16 提示词：资产借用 + 逾期通知

```text
请执行 oa-admin 的 T16：资产借用 + 逾期扫描。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md 第 4.6 节
- 已有 AssetBorrow / AssetLog / WorkflowCallback

范围：
- AssetBorrowService：创建借用、详情、审批、归还、撤回
- 工作流 processType='asset_borrow' seed
- 回调：审批通过 -> 资产 status=BORROWED + 写 log
- 归还确认 -> 资产 status=IDLE/IN_USE + 写 log
- 定时任务（@Scheduled 每天 02:00）：扫描 plan_return_date<today AND status=RUNNING 的借用，发送站内消息并标记 overdue_notified=1

验收：
cd code/backend && mvn -pl oa-admin,oa-workflow,oa-message,oa-web -am test
```

### 8.7 T21 提示词：Web 端演示页

```text
请执行 oa-admin 的 T21：Web 端演示页面。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-admin-task-split.md 第 4 节
- 已有的后端 API

范围：
- code/frontend/src/api/admin/ 新增：seal.ts / sealUsage.ts / supply.ts / asset.ts / assetBorrow.ts
- code/frontend/src/views/admin/ 新增：
  * seal/index.vue (列表 + 新增/编辑)
  * seal/usage.vue (用印申请/审批/登记)
  * supply/category.vue (分类树)
  * supply/index.vue (用品列表 + 库存)
  * supply/request.vue (领用申请)
  * asset/index.vue (资产台账)
  * asset/operation.vue (领用/调拨/盘点/折旧/报废)
  * asset/borrow.vue (借用)

验收：
cd code/frontend && pnpm typecheck && pnpm build
```

---

## 9. 与其他模块的依赖声明

| 依赖 | 方向 | 用途 |
|------|------|------|
| `oa-workflow` (核心) | oa-admin 调 | 启动 seal_apply / supply_apply / asset_borrow 流程 |
| `oa-message` | oa-admin 调 | 库存预警、借用逾期、资产报废通知 |
| `sys_employee` | oa-admin 读 | 保管人/申请人/使用人 |
| `sys_dept` | oa-admin 读 | 数据权限、所属部门 |
| `oa-attendance` | 无 | 综合行政不涉及考勤 |

---

## 10. 风险点

| 风险 | 影响 | 缓解 |
|------|------|------|
| 库存超发 | 财务对账不平、用户体验差 | 强制乐观锁 + 单条 SQL CAS + 50 并发测试 |
| 资产状态机非法转换 | 资产生命周期错乱 | Service 层状态机校验、Controller 单独测试 |
| 借用逾期未通知 | 法务/合规风险 | 定时任务扫描 + 多次提醒（3/7/15 天） |
| 工作流回调失败 | 业务状态不同步 | 回调重试 + 死信队列 + 对账脚本 |
| 折旧计算误差 | 财务审计问题 | 提供公式预览、审批前确认、按月分摊 |
| 11 张表跨模块 SQL 变更 | 影响文档/费控 | 文件命名按模块前缀，独立迁移脚本 |
| 旧 oa_seal / oa_asset 共存 | 数据双写风险 | 旧表 T11 之前只读、菜单/路由切换到新 API |
