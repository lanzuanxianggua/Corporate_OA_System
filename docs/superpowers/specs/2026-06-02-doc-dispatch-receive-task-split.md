# 公文管理(发文与收文)重构设计与任务拆分

> 日期: 2026-06-03  
> 子模块范围: 公文管理(Doc/Document) — 发文管理、收文管理、文号管理、套红模板、正文修订留痕、PDF/OFD生成与预览  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> 试点/参考文档: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md` 和 `docs/superpowers/specs/2026-06-02-hr-performance-task-split.md`  

---

## 1. 模块说明与目标

### 1.1 模块定位与核心价值

在大型企事业单位中，公文管理是 OA 系统的核心中枢，承载着信息上情下达、政策落实与对外交往的重要职能。国家对公文有着极具权威性的排版与格式要求（如《GB/T 9704-2012 党政机关公文格式》国家标准）。

本模块进行全新重构，旨在淘汰以往“附件式公文”的落后模式，全面推行**在线化、结构化、规范化的电子公文闭环管理**。完成后的公文管理模块将具备以下四大核心技术与业务特征：

1. **公文套红模板（红头套红）**：系统支持对套红模板（发文红头、红色双线、发文机关标识、落款等公文版头版尾要素）进行可视化配置和动态渲染。利用 HTML 弹性容器，在线将正文（支持 HTML 或 RichText 格式）自动套入指定红头文件，形成专业公文页面。
2. **文号自动编制与无断号回收**：支持企业级的多分类发文字号自动生成（如：`校办发〔2026〕12号`、`党委发〔2026〕3号`）。建立分布式行锁保障多端高并发取号排他性，并通过独创的“废弃回收池”机制，实现作废公文文号的自动释放与优先复用，杜绝出现断号跳号。
3. **正文修改痕迹追溯（修订对比）**：公文编辑过程中，起草人与会签、签发领导所有的修改都会被记录。系统采用基于 Longest Common Subsequence（LCS）和 Myers Diff 的字符/段落级差分算法，精确计算修改节点并在前端直观呈现。不仅支持保留修订历史，还能在审批流程任意节点一键对比两个版本正文，标红删除线为去、标绿下划线为加，使改动历历在目。
4. **国家版式 OFD/PDF 渲染及在线预览**：在签发结案阶段，系统后端服务将 HTML 套红页面通过排版引擎（OpenHTMLtoPDF/wkhtmltopdf）转换为国家标准的 A4 PDF 文件。随后，采用 `ofdrw` 库（OFD Reader & Writer）转换为国家标准版式文件 OFD，供在 Web 页面与移动端无缝预览及下载，兼备内容防篡改、打印保真及国家规范要求。

### 1.2 与其他核心组件的联动

* **工作流级联（Callback Dispatcher）**：
  * **发文流程**：
    * 提交起草：触发流程启动，公文状态由 `DRAFT` 变为 `SUBMITTED`（流转中）。
    * 办公室核稿/核实阶段：调用公文服务触发“正式锁定取号”操作，将发文字号固化到公文数据中。
    * 领导签发：流程归口通过，回调 `WorkflowCallbackDispatcher` 的 `onApproved`，将公文状态更新为 `SIGNED`（已签发），正式生成并封存最终 PDF/OFD 版式公文。
    * 审批驳回作废或撤销：回调 `onWithdrawn` / `onRejected`，改变状态，并激活文号释放与回收逻辑。
  * **收文流程**：
    * 登记与流转：收文登记完成启动承办流转工作流，状态更新为 `RUNNING`。
    * 承办批办：工作流推进，通知承办部门负责人和经办人。
    * 归档/结案：工作流结束后，回调将收文状态更新为 `FINISHED`（已办结）。
* **消息中台与待办中心（Todo & Notice）**：
  * 任务分配时自动将审批、承办、传阅信息推送到员工待办列表中，并通过实时 WebSocket 弹窗推送。
  * 公文流转、签发成功、或者分发传阅后，相关人通过消息中心接收到带有公文详情链接的通知。

---

## 2. 边界定义

为了保障重构目标的按时、高质量完成，确保聚焦在电子化公文核心底座上，本模块限定明确的范围边界。

### 2.1 包含范围

| 区域 | 本模块包含的具体内容 | 业务与技术价值 |
|------|----------------------|----------------|
| **公文套红** | 模板可视化创建与编辑；HTML 骨架动态变量替换渲染，满足国标 A4 排版格式。 | 摆脱 Word 线下排版烦恼，统一发文视觉。 |
| **文号管理** | 支持并发锁定的文号序号自动递增；文号规则设置；废弃/作废发文的文号**自动释放及回收优先复用**。 | 杜绝由于审批中途驳回或手工撤销导致的跳号、断号，确保公文连续性。 |
| **正文修订** | 支持每次更新正文自动记录历史版本，生成修订明细；基于 `Diff` 差分引擎实现文本级的高亮比对渲染。 | 真实还原会签过程中领导和办公室对正文一字一语的打磨历史。 |
| **版式生成** | 后端利用排版引擎生成带红头、页脚、正文的标准 PDF ；支持 OFD 标准版式生成；前端实现在线预览。 | 采用国家信创推荐的 OFD 与标准 PDF ，达到防篡改与高保真打印。 |
| **流程级联** | 发文（起草-核稿-签发-套红归档）、收文（登记-批办-承办-传阅归档）的工作流回调状态接轨。 | 消除业务状态与工作流审批状态的不一致，保障事务一体化。 |
| **传阅分发** | 登记后或签发后的公文可以一键分发或抄送至各部门与指定员工，关联待办和传阅已读记录。 | 替代传统的纸质传阅单，员工在线勾选传阅，随时写下阅办意见。 |

### 2.2 不包含范围

| 不包含的内容 | 排除原因与解决/过渡方案 |
|--------------|-------------------------|
| **公文跨单位物理交换** | 属于高级政务系统跨网段数据交换规范，需要专门的政务前置机和安全隔离，当前系统暂不需要。 |
| **物理硬件 Key 签发与CA物理红章** | 避开物理电子签章厂商、CA 服务商高昂的闭源 SDK 授权。系统本阶段采用高精度 PNG/SVG 的机构电子印章进行水印覆盖渲染以作视觉展现。 |
| **多人高并发在线协同编辑 Office** | 避开集成庞大且不稳定的 OnlyOffice/WebOffice 服务。系统采用结构化富文本编辑器，通过阶段性的版本控制和 Diff 留痕技术实现业务层协同。 |
| **扫描件/纸质收文 OCR 智能识别** | 属于人工智能增值，本模块通过手工录入和上传 PDF 附件归档作为基础，后续如有需求可单独设计插件。 |

---

## 3. 核心数据模型

公文管理模块对原有设计进行系统性重构，新增了**公文套红模板表**和**文号回收池表**，同时大幅增强和优化了现有的**发文表**、**收文表**、**文号表**、以及**正文修订表**，增加审计、冗余和联合索引，以应对高并发、严格一致性及复杂的流转场景。

### 3.1 核心数据表总览 (6张表)

```
                       ┌───────────────────────┐
                       │     doc_template      │ (套红模板)
                       └───────────┬───────────┘
                                   │ 1
                                   │ N
┌───────────────────┐  1  N ┌──────▼───────────┐  N  1 ┌───────────────────┐
│    doc_serial     ├──────>│   doc_dispatch   │<──────┤   doc_revision    │
│    (文号规则)     │       │     (发文表)     │       │    (正文修订表)   │
└─────────┬─────────┘       └──────────┬───────┘       └───────────────────┘
          │ 1                          │ 1
          │ N                          │ N
┌─────────▼─────────┐                  │
│doc_serial_recycle │                  │
│   (断号回收池)    │                  │
└───────────────────┘                  │
                                       │ 联动
                                       │
                            ┌──────────▼───────────┐
                            │     doc_receive      │
                            │       (收文表)       │
                            └──────────────────────┘
```

| 表名 | 数据库表名 | 核心职责 | 相比旧版之改造点 |
|------|------------|----------|------------------|
| **公文套红模板表** | `doc_template` | 定义发文红头、字体、边距、落款结构及渲染用的 HTML DOM。 | **[完全新增]** 支持灵活选择不同发文主体的红头套红模板。 |
| **公文发文表** | `doc_dispatch` | 存储发文基础元数据，保存富文本正文、状态以及最终生成的版式文件路径。 | 将附件 ID 扩展为 JSON 格式、增强状态字典控制、追加修订版本号、增加工作流关键字段。 |
| **公文收文表** | `doc_receive` | 登记来文元数据、办理状态、承办人、阅办意见，管理内部流转及分发。 | 替换陈旧的列设计，补充登记人、承办人字段，使状态控制和待办产生逻辑闭环。 |
| **公文文号管理表** | `doc_serial` | 配置各类发文字号的格式模板、年度限制、当前最新占用的最大序号。 | 增强并发字段、多租户/多分支文号标识。 |
| **文号回收与复用表**| `doc_serial_recycle`| 暂存已被审批作废、撤销释放的空闲序号，取号时优先从中抓取。 | **[完全新增]** 支撑政务与企业规范中“无跳号、防断号”的刚性设计。 |
| **公文修订留痕表** | `doc_revision` | 记录起草人、会签人、签发人在每个版本中做出的正文内容更改。 | 添加对发文/收文双模式的支持、添加 `diff_json`（保存差分快照）、操作人姓名冗余。 |

### 3.2 完整 DDL 设计 (MySQL 8.0 可执行脚本)

```sql
-- ============================================================================
-- 公文管理子系统数据库重构设计 DDL (MySQL 8.0+)
-- ============================================================================

USE `oa_system`;

-- ---------------------------------------------------------------------------
-- 3.2.1 公文套红模板表 (doc_template)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_template`;
CREATE TABLE `doc_template` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code`   VARCHAR(64)  NOT NULL                COMMENT '模板编码(唯一, 供代码识别)',
  `template_name`   VARCHAR(200) NOT NULL                COMMENT '模板名称(如 院级公文标准红头)',
  `header_text`     VARCHAR(200) NOT NULL                COMMENT '红头文字内容(如 杭州XX集团有限公司文件)',
  `header_color`    VARCHAR(16)  NOT NULL DEFAULT '#FF0000' COMMENT '红头颜色(Hex编码)',
  `footer_text`     VARCHAR(500) DEFAULT NULL            COMMENT '主题词/抄送栏上方等底部文字',
  `has_double_line` CHAR(1)      NOT NULL DEFAULT '1'    COMMENT '是否有红色双线(0无 1有)',
  `html_content`    LONGTEXT     NOT NULL                COMMENT 'HTML排版骨架(含 CSS样式与 {title}, {doc_no}, {content}, {issuer_date} 等占位符)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文套红模板表';

-- ---------------------------------------------------------------------------
-- 3.2.2 公文文号管理表 (doc_serial)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_serial`;
CREATE TABLE `doc_serial` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文号规则ID',
  `serial_code`     VARCHAR(64)  NOT NULL                COMMENT '文号编码(如 GOV_DOC, CORP_RED)',
  `serial_name`     VARCHAR(200) NOT NULL                COMMENT '文号名称(如 杭发, 杭政办字)',
  `current_seq`     INT          NOT NULL DEFAULT 0      COMMENT '当前已被占用的最大序号',
  `seq_length`      INT          NOT NULL DEFAULT 4      COMMENT '序号长度(不足补0, 如0015)',
  `year`            INT          NOT NULL                COMMENT '对应所属年份(如 2026)',
  `pattern`         VARCHAR(100) NOT NULL                COMMENT '文号拼装模板(如 杭发[{year}]{seq}号)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0启用 1停用)',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_serial_code_year` (`serial_code`, `year`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文文号管理表';

-- ---------------------------------------------------------------------------
-- 3.2.3 文号回收与复用表 (doc_serial_recycle)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_serial_recycle`;
CREATE TABLE `doc_serial_recycle` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '回收记录ID',
  `serial_id`       BIGINT       NOT NULL                COMMENT '关联文号规则ID',
  `recycle_seq`     INT          NOT NULL                COMMENT '回收的空闲/废弃序号',
  `full_doc_no`     VARCHAR(64)  NOT NULL                COMMENT '完整的公文文号',
  `origin_doc_id`   BIGINT       NOT NULL                COMMENT '原占用该文号的公文ID(发生废弃/撤回的发文)',
  `status`          CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '复用状态(0待分配/回收空闲 1已重新分配)',
  `target_doc_id`   BIGINT       DEFAULT NULL            COMMENT '复用此文号的新公文ID',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '释放操作人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '释放发生时间',
  `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '重新分配操作人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`id`),
  KEY `idx_serial_seq` (`serial_id`, `recycle_seq`),
  KEY `idx_status` (`status`),
  KEY `idx_full_doc_no` (`full_doc_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文文号回收与复用表';

-- ---------------------------------------------------------------------------
-- 3.2.4 公文发文表 (doc_dispatch)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_dispatch`;
CREATE TABLE `doc_dispatch` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发文ID',
  `dispatch_no`         VARCHAR(64)  DEFAULT NULL            COMMENT '正式发文字号(如 [2026]政字0012号)',
  `serial_id`           BIGINT       DEFAULT NULL            COMMENT '文号规则ID(关联doc_serial)',
  `title`               VARCHAR(500) NOT NULL                COMMENT '公文标题',
  `urgency`             CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '紧急程度(0普通 1加急 2特急)',
  `security_level`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '密级(0普通 1机密 2秘密 3绝密)',
  `template_id`         BIGINT       DEFAULT NULL            COMMENT '套红模板ID(关联doc_template)',
  `keywords`            VARCHAR(200) DEFAULT NULL            COMMENT '主题词/关键词',
  `content`             LONGTEXT     DEFAULT NULL            COMMENT '公文正文内容(在线富文本HTML/Markdown)',
  `ofd_path`            VARCHAR(512) DEFAULT NULL            COMMENT '版式OFD公文下载与预览路径',
  `pdf_path`            VARCHAR(512) DEFAULT NULL            COMMENT '版式PDF公文下载与预览路径',
  `attachment_ids`      JSON         DEFAULT NULL            COMMENT '附件ID列表(JSON 数组, 包含附件基本信息如 [{"id": 1, "name": "附件1.xlsx", "size": 12045},...])',
  `issuer_id`           BIGINT       DEFAULT NULL            COMMENT '签发人ID(sys_employee)',
  `issuer_name`         VARCHAR(64)  DEFAULT NULL            COMMENT '签发人姓名',
  `issuer_dept_id`      BIGINT       DEFAULT NULL            COMMENT '发文部门ID(sys_dept)',
  `issuer_date`         DATE         DEFAULT NULL            COMMENT '签发日期',
  `copies`              INT          NOT NULL DEFAULT 1      COMMENT '印发份数',
  `status`              VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '发文状态(DRAFT:草稿 SUBMITTED:办理中 SIGNED:已签发 ARCHIVED:已归档 CANCELED:已废弃)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '工作流实例ID',
  `current_task_id`     BIGINT       DEFAULT NULL            COMMENT '当前工作流任务ID',
  `version`             INT          NOT NULL DEFAULT 1      COMMENT '当前最新正文修订版本号',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '起草人账号',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '起草时间/创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人账号',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_no` (`dispatch_no`),
  KEY `idx_status` (`status`),
  KEY `idx_issuer_dept` (`issuer_dept_id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文发文表';

-- ---------------------------------------------------------------------------
-- 3.2.5 公文收文表 (doc_receive)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_receive`;
CREATE TABLE `doc_receive` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '收文ID',
  `receive_no`          VARCHAR(64)  NOT NULL                COMMENT '内部收文编号(登记号, 具有唯一约束)',
  `source_doc_no`       VARCHAR(64)  DEFAULT NULL            COMMENT '来文单位的文号(对方发文字号)',
  `title`               VARCHAR(500) NOT NULL                COMMENT '收文公文标题',
  `source_org`          VARCHAR(200) NOT NULL                COMMENT '来文单位/发布机构',
  `urgency`             CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '紧急程度(0普通 1加急 2特急)',
  `security_level`      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '密级(0普通 1机密 2秘密 3绝密)',
  `content`             LONGTEXT     DEFAULT NULL            COMMENT '收文摘要/备注/批注摘要',
  `attachment_ids`      JSON         DEFAULT NULL            COMMENT '附件ID及基础信息列表(JSON格式)',
  `receive_date`        DATETIME     NOT NULL                COMMENT '收文日期/登记时间',
  `receiver_id`         BIGINT       NOT NULL                COMMENT '收文登记人ID(关联sys_employee)',
  `receiver_name`       VARCHAR(64)  NOT NULL                COMMENT '收文登记人姓名',
  `handler_dept_id`     BIGINT       DEFAULT NULL            COMMENT '承办部门ID',
  `handler_id`          BIGINT       DEFAULT NULL            COMMENT '承办人ID',
  `handler_name`        VARCHAR(64)  DEFAULT NULL            COMMENT '承办人姓名',
  `filing_status`       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '归档状态(0未归档 1已归档)',
  `status`              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '办理状态(PENDING:待分办 RUNNING:办理中 FINISHED:已办结 ARCHIVED:已归档)',
  `process_instance_id` BIGINT       DEFAULT NULL            COMMENT '工作流流程实例ID',
  `current_task_id`     BIGINT       DEFAULT NULL            COMMENT '当前工作流待办任务ID',
  `del_flag`            CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人/登记人',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receive_no` (`receive_no`),
  KEY `idx_status` (`status`),
  KEY `idx_source_org` (`source_org`),
  KEY `idx_receiver` (`receiver_id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_receive_date` (`receive_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文收文表';

-- ---------------------------------------------------------------------------
-- 3.2.6 公文正文修订留痕表 (doc_revision)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `doc_revision`;
CREATE TABLE `doc_revision` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '修订记录ID',
  `doc_type`        VARCHAR(32)  NOT NULL                COMMENT '公文类型(dispatch:发文 receive:收文)',
  `doc_id`          BIGINT       NOT NULL                COMMENT '关联对应的公文ID',
  `revision_no`     INT          NOT NULL                COMMENT '修订版本号(1, 2, 3...)',
  `content_before`  LONGTEXT     DEFAULT NULL            COMMENT '修订前的完整HTML正文',
  `content_after`   LONGTEXT     NOT NULL                COMMENT '修订后的完整HTML正文',
  `diff_json`       LONGTEXT     DEFAULT NULL            COMMENT '文本比对结果快照(结构化存储Diff高亮节点)',
  `change_summary`  VARCHAR(500) DEFAULT NULL            COMMENT '修改原因与摘要',
  `operator_id`     BIGINT       NOT NULL                COMMENT '修改人/操作人ID(sys_employee)',
  `operator_name`   VARCHAR(64)  NOT NULL                COMMENT '修改人/操作人姓名',
  `del_flag`        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改产生时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc` (`doc_type`, `doc_id`),
  KEY `idx_revision_no` (`doc_id`, `revision_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公文正文修订留痕表';
```

---

## 4. API 契约

本公文管理模块的 RESTful 接口一律以 `/api/document` 作为顶层路径。由于公文涉及高度保密性和层级审批机制，本接口设计配套了细粒度的权限码和结构化数据对象。

### 4.1 接口列表及权限

#### 4.1.1 发文管理 (Dispatch API)

| 请求方法 | 接口路径 | 核心业务行为说明 | 接口权限码 |
|:---|:---|:---|:---|
| **POST** | `/api/document/dispatch` | 创建发文草稿（保存标题、拟稿科室、基础属性、富文本内容）。 | `doc:dispatch:create` |
| **PUT** | `/api/document/dispatch` | 修改发文内容（草稿修改或审批会签中途编辑，触发修订留痕）。 | `doc:dispatch:edit` |
| **GET** | `/api/document/dispatch/page` | 分页条件检索发文列表（根据状态、标题、发文时间范围筛选）。 | `doc:dispatch:query` |
| **GET** | `/api/document/dispatch/{id}` | 获取特定发文公文的详情信息。 | `doc:dispatch:query` |
| **DELETE** | `/api/document/dispatch/{id}` | 逻辑删除指定发文。只能删除 `DRAFT` 状态的文件。 | `doc:dispatch:delete` |
| **POST** | `/api/document/dispatch/{id}/submit` | 提交发文流转（拟稿提交，启动发文审批工作流）。 | `doc:dispatch:submit` |
| **POST** | `/api/document/dispatch/{id}/redhead` | 强制执行公文套红渲染，并在后台合成最终 PDF/OFD 。 | `doc:dispatch:redhead` |
| **GET** | `/api/document/dispatch/{id}/revisions`| 获取此公文的所有修订版本列表（版本、操作人、修改时间、说明）。| `doc:dispatch:history` |
| **GET** | `/api/document/dispatch/revisions/{revisionId}/diff`| 调取指定修订版本与前一版本的 Diff 渲染数据快照。 | `doc:dispatch:history` |
| **GET** | `/api/document/dispatch/{id}/preview`| 在线预览生成的 OFD/PDF 版式文件，提供安全防篡改临时连接。| `doc:dispatch:preview` |

#### 4.1.2 收文管理 (Receive API)

| 请求方法 | 接口路径 | 核心业务行为说明 | 接口权限码 |
|:---|:---|:---|:---|
| **POST** | `/api/document/receive` | 登记来文（登记人录入标题、文号、来文单位、来文日期、摘要、附件）。| `doc:receive:create` |
| **PUT** | `/api/document/receive` | 修改收文信息。 | `doc:receive:edit` |
| **GET** | `/api/document/receive/page` | 分页检索收文列表。 | `doc:receive:query` |
| **GET** | `/api/document/receive/{id}` | 获取收文公文详情。 | `doc:receive:query` |
| **DELETE** | `/api/document/receive/{id}` | 逻辑删除收文记录（仅在待分办状态下允许）。 | `doc:receive:delete` |
| **POST** | `/api/document/receive/{id}/submit` | 提交收文承办流转（开启收文工作流）。 | `doc:receive:submit` |
| **POST** | `/api/document/receive/{id}/archive` | 将此收文归档，结束当前在线流转状态。 | `doc:receive:archive` |

#### 4.1.3 文号管理 (Serial API)

| 请求方法 | 接口路径 | 核心业务行为说明 | 接口权限码 |
|:---|:---|:---|:---|
| **POST** | `/api/document/serial` | 新增文号规则（配置字号名称、格式、长度、年度等）。 | `doc:serial:manage` |
| **PUT** | `/api/document/serial` | 修改文号规则信息。 | `doc:serial:manage` |
| **GET** | `/api/document/serial/page` | 分页获取文号规则列表。 | `doc:serial:query` |
| **POST** | `/api/document/serial/allocate` | 手动/程序调用预分配文号（锁定测试）。 | `doc:serial:allocate` |
| **POST** | `/api/document/serial/release` | 手动释放/回收文号（仅用于异常回滚或作废操作）。 | `doc:serial:release` |

#### 4.1.4 套红模板 (Template API)

| 请求方法 | 接口路径 | 核心业务行为说明 | 接口权限码 |
|:---|:---|:---|:---|
| **POST** | `/api/document/template` | 创建发文套红模板（配置 HTML 结构、红头文字、双线样式等）。 | `doc:template:manage` |
| **PUT** | `/api/document/template` | 更新套红模板数据。 | `doc:template:manage` |
| **GET** | `/api/document/template/list` | 获取所有启用的公文套红模板列表（供发文起草页下拉选择）。 | `doc:template:query` |
| **GET** | `/api/document/template/{id}` | 获取套红模板详情。 | `doc:template:query` |

### 4.2 主要数据交互对象 (DTO/VO)

#### 4.2.1 发文起草与修改 DTO (`DocDispatchSaveDTO`)

```java
package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class DocDispatchSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 修改时传 ID，起草时不传

    @NotBlank(message = "公文标题不能为空")
    private String title;

    private Long serialId; // 文号规则ID (可先选择分类，核稿时正式锁号分配)

    @NotBlank(message = "紧急程度不能为空")
    private String urgency; // 0普通 1加急 2特急

    @NotBlank(message = "密级不能为空")
    private String securityLevel; // 0普通 1机密 2秘密 3绝密

    private Long templateId; // 选用的套红模板ID

    private String keywords; // 主题词/关键词

    private String content; // 核心正文（富文本 HTML）

    private String attachmentIds; // 附件ID列表JSON，格式："[{\"id\":10,\"name\":\"附件一.pdf\",\"size\":2048}]"

    private Integer copies; // 印发份数，默认 1

    private String changeSummary; // 本次修改/保存的变更摘要说明（触发修订记录时会作为修改记录）
}
```

#### 4.2.2 收文登记 DTO (`DocReceiveSaveDTO`)

```java
package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DocReceiveSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 修改传

    @NotBlank(message = "收文编号/登记号不能为空")
    private String receiveNo; // 内部登记号

    private String sourceDocNo; // 来文单位字号(对方文号)

    @NotBlank(message = "收文公文标题不能为空")
    private String title;

    @NotBlank(message = "来文单位/发布机构不能为空")
    private String sourceOrg;

    @NotBlank(message = "紧急程度不能为空")
    private String urgency; // 0普通 1加急 2特急

    @NotBlank(message = "密级不能为空")
    private String securityLevel; // 0普通 1机密 2秘密 3绝密

    private String content; // 收文摘要、批注、说明

    private String attachmentIds; // 附件JSON

    @NotNull(message = "收文日期不能为空")
    private LocalDateTime receiveDate; // 登记收文时间
}
```

#### 4.2.3 差异版本对比结果 VO (`DocRevisionDiffVO`)

```java
package cn.oa.document.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DocRevisionDiffVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 修订记录ID
    private Long docId;
    private String docType;
    private Integer revisionNo; // 版本号
    private String contentBefore; // 修改前正文
    private String contentAfter; // 修改后正文
    private String diffHtml; // 后端基于 LCS/Diff 预渲染的删除/增加对比高亮 HTML (带 <del> <ins> 标签)
    private String changeSummary; // 变更摘要
    private Long operatorId;
    private String operatorName; // 操作人姓名
    private LocalDateTime createTime; // 操作时间
}
```

---

## 5. 任务波次拆分

整个公文子系统重构设计细分为 **5 个波次 (Waves)** 进行演进，从而在不破坏系统原有编译、部署通道的前提下，稳步从老架构过渡到新电子公文系统。

```
Wave 1: 基线迁移与分析 (T1 + T2)
  │
Wave 2: 核心基础底座建设 (T3 + T4 + T5)
  │
Wave 3: 服务与流程级联 (T6 + T7 + T8)
  │
Wave 4: 端侧界面集成开发 (T9 + T10) 
  │
Wave 5: 验证回归与下线 (T11 + T12)
```

### Wave 1: 契约与基线

#### T1 数据库与 API 契约建立

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 在开发库正式创建重构的公文六张表；确保 DDL 及其初始化索引正常通过 MySQL。定义出公文微服务层级的 API 契约规范与各操作权限码清单。 |
| **关联路径** | `code/backend/sql/baseline/` , `code/backend/sql/extensions/` |
| **任务输入** | 重构文档、现有 `001_schema.sql`、国标公文 GB/T 9704-2012 相关格式元数据。 |
| **任务输出** | `code/backend/sql/extensions/doc_redesign_schema.sql`（新表的独立 DDL 文件，保障增量更新），并在主基线文件中对应更新旧表的 DDL 脚本。 |
| **禁止修改** | **禁止**在这个阶段编写任何 Service、Controller、Mapper 等 Java 代码。 |
| **验收准则** | 1. 执行脚本，成功建表且索引不冲突。<br>2. 权限码（`doc:dispatch:create`等）录入并能通过权限体系识别。 |

#### T2 旧实现影响分析与清理规划

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 详尽理清老旧公文相关实体、JPA/MyBatis 接口、旧 Controller、旧 API、旧页面的依赖链条。评估将如何平滑替代。 |
| **关联路径** | `code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、`code/frontend/src/api/` |
| **任务输入** | 旧公文代码：`LeaveApply`（参考其工作流）、旧 `DocumentCategory` 和 `Document` 相关的陈旧接口及代码结构。 |
| **任务输出** | 影响分析清单报告，明确标出需废弃哪些旧类、如何重定向旧表的现有数据至新版表。 |
| **禁止修改** | **禁止**直接物理删除或屏蔽老代码，仅做分析，防范编译链条报错。 |
| **验收准则** | 形成影响分析文档，列出需要兼容或替换的代码清单。 |

---

### Wave 2: 后端核心实现

#### T3 公文基础 Entity 与 MyBatis-Plus Mapper

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 在 `oa-model` 和 `oa-mapper`（或新创建的 `oa-document` 模块）中编写实体类（`DocDispatch`、`DocReceive`、`DocSerial`、`DocSerialRecycle`、`DocTemplate`、`DocRevision`）及配套的 Mapper 接口。 |
| **关联路径** | `code/backend/oa-model/src/main/java/cn/oa/document/entity/` , `code/backend/oa-mapper/` |
| **任务输入** | T1 创建的 `doc_redesign_schema.sql`，MyBatis-Plus 生成工具，或手写实体。 |
| **任务输出** | 六个核心实体类（加 `@TableName`、`@TableId`、逻辑删除、自动审计字段等）、配套 Mapper 接口和 XML Mapper 映射文件。 |
| **禁止修改** | 不涉及 Controller 层和具体服务层逻辑编写。 |
| **验收准则** | 单元测试能成功注入这些 Mapper ，进行基础的插入、更新及逻辑删除查询：<br>`cd code/backend && mvn -pl oa-mapper -am test` |

#### T4 文号并发分配与防断号回收核心 Service

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 编写 `DocSerialService` 与 `DocSerialRecycleService` 的核心逻辑。实现：**排他并发安全取号**（基于 `SELECT ... FOR UPDATE` 或 Redis 分布式锁进行悲观防重）、**撤回废弃断号回收**（写回收池）、**回收序号优先分配**（优先从回收池抓取）。 |
| **关联路径** | `code/backend/oa-service/`（或 `oa-document` 模块） |
| **任务输入** | `DocSerial` 实体与 `DocSerialRecycle` 实体。 |
| **任务输出** | `DocSerialService` 接口与 `DocSerialServiceImpl` 实现类。包含并发锁定测试。 |
| **禁止修改** | 暂不接入具体的工作流审批流触发器（仅暴露服务接口，保持单元测试纯净）。 |
| **验收准则** | 编写高并发压测 JUnit 单元测试，100个线程同时竞争同一个文号规则，验证：<br>1. 无重复文号。<br>2. 连续性完美（不跳号）。<br>3. 作废后重新申请，优先拿到被回收的断号。 |

#### T5 公文正文版本控制与 Diff 修订留痕 Service

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 编写公文修订历史服务。每次公文正文被更新时，自动在 `doc_revision` 表插入留痕。利用算法计算新旧正文 HTML 文本差异（可以选用 Myers 差分算法、`diff-match-path` 库或后端 Diff 自研算法），并输出高亮对比 `diff_json` 与 `diffHtml`。 |
| **关联路径** | `code/backend/oa-service/` |
| **任务输入** | `DocRevision` 实体、Diff-Match-Patch / Hutool 工具等。 |
| **任务输出** | `DocRevisionService` 及其实现类；高精准比对算法单元测试。 |
| **禁止修改** | 暂不涉及前端渲染，后端只生成标准的比对渲染数据或 JSON 结构。 |
| **验收准则** | 单元测试传入旧文本 "原告张三诉称李四欠钱" 和新文本 "原告张三诉称李四欠钱十万元"，能准确提取出新增的内容并标绿渲染输出。 |

#### T6 发文与收文 REST API 控制层开发

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 建立 `DocDispatchController`、`DocReceiveController`、`DocTemplateController`。实现对公文草稿、登记、查询、套红规则增删改查的 HTTP 终结点暴露，配备 Knife4j OpenAPI 注解和细粒度 `@PreAuthorize` 权限注解。 |
| **关联路径** | `code/backend/oa-web/src/main/java/cn/oa/controller/document/` |
| **任务输入** | Wave 2 前几步构建的核心 Service。 |
| **任务输出** | 控制层（Controller）Java 文件及配套校验层对象（DTO、VO），OpenAPI 交互契约页面。 |
| **禁止修改** | 不直接把发文核心保存与修订算法直接堆叠在 Controller，必须高度抽象下沉在 Service 中。 |
| **验收准则** | 启动后端服务，能够通过本地 `http://localhost:8080/doc.html` 访问到发文/收文/模板的全部 Swagger 文档，且 Mock 测试返回 code 为 0 的统一格式。 |

---

### Wave 3: 协同联动与外设渲染

#### T7 审批流回调状态联动（Workflow Integration）

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 编写 `DocumentWorkflowCallbackHandler` 接入工作流引擎。实现：<br>1. 提交起草：启动工作流实例。<br>2. 会签核稿节点：流程回调自动触发文号正式“锁定分配”。<br>3. 签发节点：触发 `onApproved` 回调，发文状态更新为 `SIGNED` ，收文更新为 `FINISHED` ，并发起抄送。<br>4. 驳回/废弃节点：触发 `onRejected` / `onWithdrawn` 回调，公文作废，释放文号，并更新流转状态。 |
| **关联路径** | `code/backend/oa-workflow/` , `code/backend/oa-service/` |
| **任务输入** | 工作流引擎的流程节点 Callback 设计、`WorkflowCallbackDispatcher`。 |
| **任务输出** | 工作流联动回调处理器、集成回调逻辑、状态自动同步单元测试。 |
| **禁止修改** | 不要破坏工作流引擎的底层机制，一律通过回调接口及服务层交互，低耦合设计。 |
| **验收准则** | 执行集成测试：模拟请假/审批回调方式，模拟发文流转核稿、签发，检查数据库中 `doc_dispatch` 的状态以及文号的固化和作废状态。 |

#### T8 待办中心、传阅机制与消息中台联动

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 1. 发文/收文流转到下一阶段时，自动向新经办人/承办人写入**待办任务**（`wf_task` 或 `oa_todo`）。<br>2. 签发或收文登记后，支持批量选择人员进行**公文传阅**，写入待办，阅读后更新传阅记录并可附带阅办意见。<br>3. 联动 `oa-message`，在产生待办或结案时，通过 WebSocket 向在线员工推送通知。 |
| **关联路径** | `code/backend/oa-message/` , `code/backend/oa-workflow/` |
| **任务输入** | 待办引擎 API、消息发送客户端、WebSocket 发送机制。 |
| **任务输出** | 传阅机制实现类、传阅待办接收监听器、公文专用消息模板、消息推送测试。 |
| **禁止修改** | 暂不接入外部短信/邮件服务商，专注于站内信和待办中心（`todo`）的高效闭环。 |
| **验收准则** | 签发公文时选择传阅三人，三人登录后其待办列表、WebSocket 弹窗均收到此传阅提醒，且点击阅读后状态更新为“已阅”。 |

#### T9 公文套红 PDF/OFD 后端高保真渲染与安全预览

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 编写公文套红高保真排版生成器。在签发阶段：<br>1. 提取公文的标题、正文、文号、签发时间，填充到 `doc_template` 的 HTML 骨架中。<br>2. 通过 OpenHTMLtoPDF 引擎，将拼接后的页面转化为 A4 纸张排版的 PDF。<br>3. 引入国标信创库 `ofdrw`，一键将 PDF 自动转换为 OFD 国家标准版式公文，保存至 `uploads/doc/` 目录下。<br>4. 提供安全的、有时效限制的在线预览和下载端点。 |
| **关联路径** | `code/backend/oa-service/`（或独立渲染工具包类） |
| **任务输入** | OpenHTMLtoPDF / Thymeleaf 模板渲染引擎 / ofdrw 信创转换组件。 |
| **任务输出** | `DocRenderService` 渲染服务实现类、PDF 与 OFD 离线转换拦截、标准套红排版静态 CSS 与占位符规范。 |
| **禁止修改** | 转换和渲染过程不能有严重的阻塞风险，长时间的转换过程必须考虑多线程异步渲染，避免卡死接口。 |
| **验收准则** | 传入一篇 3000 字正文和杭发红头模板，能够在本地目标目录生成完美的 `xxx.pdf` 和 `xxx.ofd`，红头鲜红明艳、红色双线标准、页码及段落错落有致。 |

---

### Wave 4: 端侧界面集成开发

#### T10 Web 管理端（发文/收文/套红/文号）模块建设

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 在 Web 管理端（vue-pure-admin）开发公文的完整管理控制台：<br>1. 发文起草与修改（带富文本编辑器、修订摘要入口、选用套红模板）。<br>2. 发文列表页、详情查看页（带双栏版本修订 Diff 对比视图，带 OFD/PDF 在线预览）。<br>3. 收文登记与流转分办配置、传阅签收弹窗、承办批办。<br>4. 后台套红模板、发文文号规则管理配置。 |
| **关联路径** | `code/frontend/src/views/oa/document/` , `code/frontend/src/api/document.ts` |
| **任务输入** | 之前的后端核心 REST 接口、前端 UI 规范与 Element Plus 库。 |
| **任务输出** | 前端 API 封装、发文/收文视图（.vue）文件、路由绑定配置、Diff 渲染组件（集成 diff 库展示高亮对比效果）、套红及 PDF/OFD 预览页面。 |
| **禁止修改** | 不要破坏现有的 axios 拦截器与 token 管理、不修改路由全局拦截逻辑。 |
| **验收准则** | `pnpm typecheck && pnpm build` 前端完全编译通过。在页面上手动登记并分办公文，UI 表单交互流程顺畅无阻。 |

#### T11 移动端（uni-app）移动流转与签收开发

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 在移动端开发公文的高频流转页面：<br>1. 移动端待办中心展示公文（发文审批/收文承办/分办任务）。<br>2. 移动端公文详情展示：展示公文主要属性、正文、传阅意见及历史记录。<br>3. 支持在手机端进行审批（同意/驳回/会签）、签收、并撰写传阅阅办意见。 |
| **关联路径** | `code/mobile/src/pages/oa/document/` , `code/mobile/src/api/document.ts` |
| **任务输入** | 移动端 `request.ts` 工具、移动 UI 布局。 |
| **任务输出** | 移动端公文处理、传阅、审批视图；API 对接层。 |
| **禁止修改** | 保持 H5 和微信小程序的双重兼容，不采用只能在一端运行的平台原生 API。 |
| **验收准则** | 在浏览器中通过 H5 运行并调试，执行：<br>`cd code/mobile && pnpm build:h5` 编译完美通过。在手机端可以接收并成功阅办公文。 |

---

### Wave 5: 验证与下线

#### T12 端到端集成与高并发文号锁定回归测试

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 组织全链路端到端回归测试：<br>1. 起草发文 -> 选用套红和文号规则 -> 提交发起工作流 -> 产生待批任务 -> 领导核稿锁号 -> 领导签发生成 PDF/OFD 并发传阅 -> 接收传阅并更新状态。<br>2. 模拟中途驳回废弃，测试文号回收，后续取号复用回收文号的全自动化过程。 |
| **关联路径** | `code/backend/src/test/`（端到端自动化/手工测试测试例） |
| **任务输入** | 各波次产生的最终产物。 |
| **任务输出** | 全功能端到端测试覆盖、BUG 修复及性能调优清单、高并发测试报告。 |
| **禁止修改** | 不属于本模块范围内的代码不得做出任何修改。 |
| **验收准则** | 整个回归流转完全实现自动化，并发文号取号测试无一漏号重号，性能吞吐率稳定。 |

#### T13 旧代码下线与无缝兼容过渡

| 字段 | 内容说明与约束 |
|:---|:---|
| **任务目标** | 在确保新公文系统完备且运行稳定后，对原有系统中关于 `Document`、`DocumentCategory` 的老旧陈规代码进行平滑移除或安全废弃。如果旧表含有存量数据，提供过渡脚本将其导入到重构的新表中。 |
| **关联路径** | 全系统迁移旧入口清理 |
| **任务输入** | Wave 1 中理出的 T2 旧实现影响分析清单。 |
| **任务输出** | 老旧接口、类及前端无用代码的安全剔除；存量数据无缝迁移 SQL 脚本。 |
| **禁止修改** | 严禁暴力删除在其他模块中依然起关键耦合作用的公共组件，必须保证系统平稳安全。 |
| **验收准则** | 1. 清理完毕后，项目整体 Maven 编译正常。<br>2. 前后端完全构建成功，老入口全部重定向至新公文页面。 |

---

## 6. 完整的 Claude Code 提示词

以下提供的三个提示词，是专为智能助手（如 Claude Code 等）设计的执行指令。它们符合本重构方案的严格架构原则，可以直接复制给助手用于编写实现代码。

### 6.1 提示词 A：针对 T3（实体与 Mapper）和 T4（并发锁定文号管理）

```text
你是一个资深的 Java 核心架构师。现在请你为企业 OA 系统重构并编写“公文管理”模块的 T3 (Entity 与 Mapper) 和 T4 (文号并发分配与防断号回收核心 Service)。

必须遵守以下核心约束：
1. 语言偏好：所有的代码注释、输出文档和思考过程必须完全使用【中文】，且在任何地方都【禁止包含任何表情符号 (emojis)】。
2. 数据库表：使用以下表结构：doc_template、doc_serial、doc_serial_recycle、doc_dispatch、doc_receive、doc_revision。
3. 框架与版本：Spring Boot 3.4.5，MyBatis-Plus 3.5.9，JDK 17。

请按照以下指导步骤进行编写：

第一步：定义实体 Entity
在 `oa-model` 模块对应的公文包下创建 6 个实体类。
- 注意：必须完美对接 DDL 中定义的字段。使用 MyBatis-Plus 的注解如 @TableName、@TableId(type = IdType.AUTO)、@TableField(fill = FieldFill.INSERT/UPDATE) 等。
- 保证包含 `delFlag` (逻辑删除，"0"存在 "1"删除)、审计字段 (createBy, createTime, updateBy, updateTime)。
- `doc_dispatch` 与 `doc_receive` 的附件字段 `attachment_ids` 需使用 JSON 字符串映射（推荐在 MyBatis-Plus 中配置 JacksonTypeHandler）。

第二步：定义 Mapper 接口与 XML 映射
在 `oa-mapper` 模块中创建对应的 6 个 Mapper 接口：
- `DocTemplateMapper`、`DocSerialMapper`、`DocSerialRecycleMapper`、`DocDispatchMapper`、`DocReceiveMapper`、`DocRevisionMapper`。
- 如果有复杂的并发操作、需要悲观锁，在 Mapper 中提供带有悲观锁的方法：例如 `SELECT * FROM doc_serial WHERE id = #{id} FOR UPDATE` 或使用 XML 自定义查询。

第三步：实现并发安全取号与断号回收 Service
编写 `DocSerialService` 与 `DocSerialRecycleService` 的接口和实现类：
- 实现方法 `String allocateDocNo(Long serialId, Long docId)`：
  1. 开启声明式事务 `@Transactional(rollbackFor = Exception.class)`。
  2. 优先检索 `doc_serial_recycle` 中状态为 "0" (回收空闲) 的该规则最小序号：
     `SELECT * FROM doc_serial_recycle WHERE serial_id = ? AND status = '0' ORDER BY recycle_seq ASC LIMIT 1 FOR UPDATE`（使用悲观锁防并发多线程抢夺相同回收号）。
  3. 如果找到可用回收记录：
     - 将其 `status` 变更为 "1" (已重新分配)，`target_doc_id` 设为传入的公文ID，并更新审计字段。
     - 根据文号模板（pattern）拼装文号，并格式化序号（不够长度seq_length则在左侧补0，如 "0012"），返回生成的文号。
  4. 如果没有可用回收记录：
     - 使用悲观锁查询文号规则 `DocSerial` 记录：`SELECT * FROM doc_serial WHERE id = ? FOR UPDATE`。
     - 获取 `current_seq`，并将其加 1：`current_seq = current_seq + 1`，写回数据库。
     - 同样根据 pattern 拼装、格式化，并返回生成的文号。
- 实现方法 `void releaseDocNo(Long serialId, String docNo, Long originDocId)`：
  1. 开启事务。
  2. 解析发文字号，提取出对应的 `seq` 整数。
  3. 在 `doc_serial_recycle` 表中插入一条新纪录，`status = '0'` (回收空闲)，把空缺序号放入回收池，等待下次分配。

第四步：编写单元测试
编写 JUnit 5 并发测试，使用线程池 (`ExecutorService` 和 `CountDownLatch`) 模拟 50 个并发线程同时调用 `allocateDocNo`，验证：
1. 绝对不产生任何重复的字号。
2. 再次释放、再次分配，能无缝优先复用被回收的序号。

在动手之前，请先说明你的总体实现设计与具体技术路线。开始你的编写。
```

### 6.2 提示词 B：针对 T5（正文修订留痕与 Diff 对比）

```text
你是一个精通差异对比与版本控制的资深开发专家。请你为 OA 公文系统实现 T5 (公文正文版本控制与 Diff 修订留痕 Service)。

必须遵守的核心约束：
1. 语言偏好：所有代码、文档及思考必须使用【中文】，且【禁止使用任何表情符号 (emojis)】。
2. 框架与版本：Spring Boot 3.4.5，MyBatis-Plus 3.5.9，JDK 17。

请按照以下指南进行开发：

第一步：设计 `DocRevisionService` 接口
需要提供以下几个核心方法：
- `void saveRevision(String docType, Long docId, String contentBefore, String contentAfter, String changeSummary, Long operatorId)`:
  1. 获取该公文当前已有的最大版本号 `revision_no`，本次版本号递增。
  2. 调用 Diff 差异对比引擎，计算出两个 LONGTEXT 文本之间的差异，生成一个高亮标注的对比 HTML（删除内容用 <del> 包裹，新增内容用 <ins> 包裹），并将其转化成一个结构化的 JSON (如：保存每一个变动操作的操作码和文本块)。
  3. 创建并插入 `DocRevision` 记录，其中 `diff_json` 存储差异 JSON 或 Diff 标记，`content_before` 与 `content_after` 保存快照，记录修改人和时间。
  4. 更新主表 `doc_dispatch` (或 `doc_receive`) 的版本号 `version = version + 1`。
- `DocRevisionDiffVO getDiff(Long revisionId)`:
  1. 调取指定修订版本，获取版本记录，填充 VO。
  2. 预渲染生成可供前端直接显示的 `diffHtml`。
- `List<DocRevision>` List 接口获取某公文的全部修改版本历史。

第二步：设计 Diff 对比引擎
你可以自研一个简单好用的 Myers Diff、最长公共子序列（LCS）字符级对比算法，或者引用成熟的第三方库（如 `diff-match-patch` 库进行包装）。
- 要求：
  1. 能够清洗 HTML 标签再比对，也可以直接对 HTML 文档树进行比对（推荐对纯文本比对后，输出带 HTML 自定义样式的差异片段）。
  2. 对比格式极其精准，任何段落、汉字的新增、修改、删除都能精确界定。

第三步：编写完善的单元测试
- 模拟起草人保存公文：旧文本为 "办公室决定于周五举行全体大会"。
- 模拟领导修改公文：新文本为 "办公室决定于本周五下午两点举行全体干部职工大会"。
- 运行 Diff 引擎并调用 saveRevision，打印并验证生成的 diff 结果，确保删除/修改/增加的信息无一遗漏且包含正确的 `<del>` / `<ins>` 样式。

请先介绍你的修订与留痕设计方案，再进行代码编写。
```

### 6.3 提示词 C：针对 T7（流程状态联动）和 T9（套红 PDF/OFD 渲染与预览）

```text
你是一个精通工作流技术与版式文件生成的后端架构师。现在请你为公文系统实现 T7 (工作流审批状态联动) 和 T9 (公文套红 PDF/OFD 高保真生成及安全预览)。

必须遵守以下核心约束：
1. 语言与风格：思考、注释、汇报和文档全部使用【中文】，【严禁使用任何表情符号 (emojis)】。
2. 框架与版本：Spring Boot 3.4.5，MyBatis-Plus 3.5.9，JDK 17。
3. 渲染选用：OpenHTMLtoPDF (或 wkhtmltopdf) 渲染 HTML 页面，引入 `ofdrw-core`/`ofdrw-converter` 相关信创组件（OFD Reader & Writer 国标版式库）来进行 OFD 生成。

请按照以下指导步骤进行编写：

第一步：实现公文套红高保真排版渲染服务
编写 `DocRenderService` 类：
- 提供方法 `File renderPdf(Long dispatchId)`：
  1. 获取发文详情及关联的套红模板 `DocTemplate`。
  2. 提取模板中的 HTML 排版骨架（其中已经定义好标准的 A4 页面 CSS，包含 A4 A级国标页边距、发文红头、发文机关标识、红色双线等样式）。
  3. 使用模板引擎 (如 Thymeleaf) 将公文标题、发文字号（dispatch_no）、主送单位、正文富文本、印发时间、发文部门等替换填充进骨架。
  4. 调用 OpenHTMLtoPDF 排版解析器，输入渲染后的标准 HTML 字符串，输出生成符合 A4 尺寸的标准高保真 PDF 临时文件。
- 提供方法 `File convertToOfd(File pdfFile, String ofdOutputPath)`：
  1. 引入 `ofdrw` 国标版式引擎，解析生成的 PDF。
  2. 将其转换为我国推荐的 OFD 版式文档，并保存到本地特定服务器存储或对象云存储中。
  3. 将生成的 PDF 与 OFD 相对路径回填写入到对应的 `doc_dispatch` 记录的 `pdf_path` 与 `ofd_path` 字段中。

第二步：实现工作流回调集成处理器 (T7)
编写 `DocumentWorkflowCallbackHandler` 并将其注入到 `WorkflowCallbackDispatcher` 中：
- `onApproved(Long processInstanceId)` (发文流转到领导签发节点并审批通过)：
  1. 查询对应的 `DocDispatch` 记录。
  2. 将公文状态 `status` 更新为 `SIGNED` (已签发)。
  3. 正式锁定并记录签发日期 `issuer_date = NOW()`。
  4. 触发异步调用 `DocRenderService`，执行公文套红高保真渲染，生成最终 PDF 与 OFD 文件，封存正文不许再次修改。
- `onNodeChange(Long processInstanceId, String nodeCode)` (发文流转到特定的“办公室核稿/核对”节点)：
  1. 如果发文还没有分配文号，在这里调用 `DocSerialService.allocateDocNo` 为该公文正式取号并固化到 `dispatch_no` 中。
- `onRejected(Long processInstanceId)` (流程被驳回至草稿/作废)：
  1. 更新公文状态 `status = 'CANCELED'` (已废弃) 或由管理员判定作废。
  2. 若已取得文号，则调用 `DocSerialService.releaseDocNo` 将取得的文号序号安全释放入回收池，状态变为回收待用。

第三步：提供安全预览与下载的 REST Endpoint
- 在 Controller 层暴露 `/api/document/dispatch/{id}/preview` 与 `download` 接口：
  1. 接收公文 ID，检查用户是否有 `doc:dispatch:preview` 权限。
  2. 生成带有短期 Token (如带时间戳的加密 signature 临时访问链接)，提供后端流媒体传输，满足前端 PDF.js / OFD 预览插件的安全访问，避免物理绝对路径暴露。

请先提供你完整的技术实现蓝图（包含套红 HTML 结构、OpenHTMLtoPDF 的 CSS 页面边距与排版要点，以及 OFD 信创转化步骤），然后开始代码开发。
```

---

## 7. 模块演进与迁移兼容策略

由于本系统原本含有简陋的 `Document` 和 `DocumentCategory` 设计（仅作为基础知识库上传附件功能），重构后要避免对其他业务模块造成连带编译破坏。本模块将采用“**双轨运行，渐进重定向**”的兼容过渡机制。

1. **库表重定向机制**：
   * 原来简陋的 `oa_document` 表作为“普通知识库文档”，重构后改名为 `km_document` (归档到知识库子系统 `oa-knowledge`)。
   * 发文与收文系统全量接手所有**公家行文/红头文件**。原有在 `Document` 表中被错误归类的公文档案，通过迁移 SQL（在 Wave 5 完成）清洗分类后分别注入 `doc_dispatch` 与 `doc_receive`。
2. **前后端路由过渡**：
   * 在动态路由表 `sys_menu` 中，原“公文管理”的旧前端组件路由指向进行重定向配置。
   * 当新系统通过 E2E 验收后，将旧菜单页面正式剔除。
3. **文号防漏号回滚应急方案**：
   * 在多线程悲观锁机制基础上，为每个年度的 `doc_serial` 配套一个定期巡检定时任务。
   * 如果出现某个文号在锁定期由于意外（如 JVM 强行关机、断电）产生僵尸占用，定时器会在深夜自动将该文号强制转回 `doc_serial_recycle` 回收空闲态，确保文号流转体系万无一失。
