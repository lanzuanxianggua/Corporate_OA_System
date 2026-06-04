# 费控与报销(oa-finance)模块重构与任务拆分设计方案

> 日期: 2026-06-03  
> 模块范围: 费控与报销 (oa-finance) — 预算控制、费用报销、员工借款、合同管理、付款追踪  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> 试点与参考方案: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`  

---

## 1. 模块说明与目标

费控与报销（`oa-finance`）是企业OA系统的核心财务管理模块。通过对预算额度、日常报销、员工借款、合同签署及合同付款的数字化管理，实现企业支出的合规、透明与高效。本模块的核心目标是构建一个端到端的“预算-借款-报销-付款-合同”闭环财务管控系统。

### 1.1 预算控制模式：硬控制与软警示

预算管理（`fin_budget`）是费控的基石。系统提供两种精细化的预算控制模式，作用于员工提交“费用报销”、“出差申请”、“合同付款申请”等涉财审批时：

*   **硬控制（Hard Control）**：强管控策略。当报销/申请金额 + 已执行金额 + 已占用（审批中）金额超过该部门、该类别在指定周期的预算总额时，系统在提报阶段直接拦截，禁止提交，并给出明确错误提示。
*   **软警示（Soft Control）**：温和管控策略。当超出预算总额时，系统允许员工提报，但在提报页面弹出黄色超支警示框，并在流程流转到审批节点时，向审批人和财务主管发送专门的“预算超支预警”站内消息/邮件抄送，方便人工干预。
*   **无控制（None Control）**：仅做记录与统计，不进行任何限制与警示。

### 1.2 预算占用、确认与释放的原子操作及并发处理

在涉财审批的整个生命周期中，预算的流动必须满足原子性（ACID），防止在并发提报时发生预算“超卖”或不一致。系统采用以下机制实现安全周密的流水线：

1.  **提交审批（预算占用/冻结）**：
    *   **业务动作**：员工提交报销单或借款申请。
    *   **原子操作**：在单笔事务中，先通过 Redis 分布式锁锁定 `dept_id + category + period`。校验当前预算是否足够（仅硬控制下校验）。若校验通过，更新 `fin_budget` 对应记录：`occupied_amount = occupied_amount + current_apply_amount`。
    *   **并发防护**：使用 MySQL 乐观锁更新，基于 `version` 字段。更新 SQL 如下：
        ```sql
        UPDATE fin_budget 
        SET occupied_amount = occupied_amount + ?, version = version + 1, update_time = NOW()
        WHERE id = ? AND version = ? AND (control_strategy != 'hard' OR amount >= occupied_amount + executed_amount + ?);
        ```
2.  **审批通过（预算确认/转实际）**：
    *   **业务动作**：审批流最终通过，财务打款确认。
    *   **原子操作**：在事务中释放占用，累加执行：
        ```sql
        UPDATE fin_budget 
        SET occupied_amount = occupied_amount - ?, executed_amount = executed_amount + ?, version = version + 1, update_time = NOW()
        WHERE id = ? AND version = ? AND occupied_amount >= ?;
        ```
3.  **审批驳回/撤销/删除（预算释放）**：
    *   **业务动作**：审批中途被驳回，或员工撤回申请，或删除草稿。
    *   **原子操作**：在事务中回滚占用金额：
        ```sql
        UPDATE fin_budget 
        SET occupied_amount = occupied_amount - ?, version = version + 1, update_time = NOW()
        WHERE id = ? AND version = ? AND occupied_amount >= ?;
        ```

### 1.3 借款与报销的核销联动

企业日常运营中存在“先借款、后报销、多退少补”的典型场景。系统建立借款（`fin_loan`）与报销（`fin_expense`）的自动化联动核销逻辑：

*   **借款提报**：员工提交借款单（`loan_amount`），状态变为 `repaying` (还款中)，`repaid_amount` 初始化为 `0`。
*   **报销提报核销**：
    *   员工提报报销时，系统自动查询其名下所有处于 `repaying` 状态的借款。
    *   员工可选择某一笔或多笔借款进行冲抵（核销）。
    *   报销单中引入以下核心字段：
        *   `total_amount`：本次报销明细总计额。
        *   `offset_amount`：冲抵借款的金额（`MIN(未还借款金额, 本次报销总额)`）。
        *   `pay_amount`：最终实际付款金额（`total_amount - offset_amount`，若冲抵后为 0，则无需实际付款打款）。
*   **审批通过后的核销滚算**：
    *   当报销审批通过时，系统自动生成一条核销还款流水，写入 `fin_loan_repayment`：`repay_amount = offset_amount`，`repay_type = 'offset'` (报销冲抵)，`expense_id = current_expense_id`。
    *   更新借款单主表：`repaid_amount = repaid_amount + offset_amount`。若 `repaid_amount == loan_amount`，自动将该借款状态置为 `paid_off` (已结清)。
    *   以上操作必须包裹在同一个 Spring 声明式事务中，实现完全一致性。

### 1.4 合同签署、付款计划追踪与工作流联动

合同管理（`fin_contract`）及付款管理（`fin_payment`）联动，提供企业合同生命周期追踪：

*   **合同签署审批**：起草合同后，触发 `contract_approval` 审批流程。审批通过后，合同状态由 `draft` 变更为 `active` (生效)。
*   **付款计划生成**：生效合同可以预先设立多笔付款计划。
*   **付款提报与核销**：
    *   根据合同付款节点（首款、阶段款、尾款），发起付款申请，触发 `payment_approval` 工作流，并锁定合同对应的未付预算。
    *   审批通过后，将对应的付款申请核算进合同总额：`UPDATE fin_contract SET paid_amount = paid_amount + ? WHERE id = ?`。
    *   同时生成 `fin_payment` 付款记录，状态更新为 `completed`。

---

## 2. 边界定义

### 2.1 本模块包含范围

| 区域 | 内容 |
|------|------|
| **数据库** | 包含 `fin_budget`, `fin_expense`, `fin_expense_detail`, `fin_loan`, `fin_loan_repayment`, `fin_contract`, `fin_payment` 7张表的重构与优化。 |
| **预算原子层** | 实现高并发下预算占用、确认、释放的乐观锁与悲观锁组合策略，包含超期预算自动冻结策略。 |
| **报销与发票防重** | 报销单及明细提报；提供电子发票去重校验（在 `fin_expense_detail` 中校验联合唯一性 `invoice_no` 且未删除的记录）。 |
| **借贷核销联动** | 自动查找名下未结借款，报销单中冲抵逻辑，审批通过后自动产生 `fin_loan_repayment` 记录并滚算结清。 |
| **合同与付款闭环** | 合同录入与审批、付款计划设置、付款申请提交、已付金额自动滚算，状态同步机制。 |
| **工作流回调** | 提供报销、借款、合同、付款四个核心工作流流程定义回调适配（状态驱动核心）。 |
| **待办消息中台** | 预算超支、付款到期、借款催还、审批分配与结果的站内信与WebSocket实时推送。 |

### 2.2 本模块排除范围

| 不包含项 | 排除原因 |
|----------|----------|
| **物理网银自动接口打款** | 涉及银企直连通道与极其复杂的安全数字证书认证，采用线下打款并在系统手动确认/回填水单。 |
| **发票真伪OCR物理验证** | 属于第三方SaaS或国税API对接服务，重构不直接接入物理扫描OCR接口，发票去重基于逻辑号码匹配。 |
| **企业财务总账凭证系统** | 排除生成用友、金蝶、SAP等专业的财务分录和总账凭证，只提供符合审计要求的业务单据流水与汇总导出。 |
| **第三方报销商旅对接** | 排除携程商旅、同程等商旅平台自动同步发票、订票数据，数据由员工自行填报上传。 |

---

## 3. 核心数据模型

以下是对 `001_schema.sql` 现有 `fin_*` 财务相关数据表进行的升级规范化 DDL 设计，增强了索引匹配、唯一约束以及并发控制字段。

```sql
-- ============================================================================
-- 费控报销与合同管理模块 DDL 基线 (升级版)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 预算额度表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_budget`;
CREATE TABLE `fin_budget` (
  `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `dept_id`           BIGINT          DEFAULT NULL            COMMENT '部门ID(NULL为项目预算)',
  `project_id`        BIGINT          DEFAULT NULL            COMMENT '项目ID(NULL为部门预算)',
  `expense_category`  VARCHAR(64)     NOT NULL                COMMENT '费用类别(travel=差旅费 office=办公费 entertainment=招待费 transport=交通费 other=其他)',
  `year`              INT             NOT NULL                COMMENT '年度(如 2026)',
  `month`             INT             DEFAULT NULL            COMMENT '月份(1-12, NULL表示年度总预算)',
  `amount`            DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '预算总额(可分配额度)',
  `occupied_amount`   DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '占用金额(审批中的总冻结额)',
  `executed_amount`   DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '已执行金额(已打款报销额)',
  `control_strategy`  VARCHAR(32)     NOT NULL DEFAULT 'soft' COMMENT '预算控制策略(soft=超支警告并通知 hard=强行拦截 none=仅统计不控制)',
  `status`            CHAR(1)         NOT NULL DEFAULT '1'    COMMENT '状态(0待启用 1已启用 2已冻结 3已完成/归档)',
  `version`           INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `del_flag`          CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`         VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  -- 索引设计：支持高频的预算余额匹配与滚算
  KEY `idx_dept_category_year` (`dept_id`, `expense_category`, `year`, `month`),
  KEY `idx_project_category_year` (`project_id`, `expense_category`, `year`, `month`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预算额度表';

-- ---------------------------------------------------------------------------
-- 3.2 费用报销主表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_expense`;
CREATE TABLE `fin_expense` (
  `id`                        BIGINT          NOT NULL AUTO_INCREMENT COMMENT '报销单ID',
  `emp_id`                    BIGINT          NOT NULL                COMMENT '提报员工ID',
  `dept_id`                   BIGINT          NOT NULL                COMMENT '提报部门ID',
  `title`                     VARCHAR(200)    NOT NULL                COMMENT '报销标题',
  `total_amount`              DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '报销总金额(明细总和)',
  `offset_amount`             DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '冲抵借款总金额',
  `pay_amount`                DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '实付金额(总金额-冲抵金额)',
  `related_business_trip_id`  BIGINT          DEFAULT NULL            COMMENT '关联出差单ID',
  `related_loan_id`           BIGINT          DEFAULT NULL            COMMENT '关联借款单ID',
  `description`               TEXT            DEFAULT NULL            COMMENT '报销整体说明',
  `status`                    VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '状态(draft=草稿 pending=审批中 approved=已通过 rejected=已驳回 withdrawn=已撤回 paid=已付款打款)',
  `process_instance_id`       BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `paid_time`                 DATETIME        DEFAULT NULL            COMMENT '出账付款确认时间',
  `del_flag`                  CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`                 VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`                 VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_status` (`emp_id`, `status`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_related_trip` (`related_business_trip_id`),
  KEY `idx_related_loan` (`related_loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='费用报销主表';

-- ---------------------------------------------------------------------------
-- 3.3 费用报销明细表 (含发波去重物理索引)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_expense_detail`;
CREATE TABLE `fin_expense_detail` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `expense_id`      BIGINT          NOT NULL                COMMENT '所属报销单ID',
  `expense_date`    DATE            NOT NULL                COMMENT '费用发生日期',
  `expense_type`    VARCHAR(64)     NOT NULL                COMMENT '费用子类型(travel=差旅费 accommodation=住宿费 meal=餐饮费 transport=市内交通 office=办公用品 other=其他)',
  `amount`          DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '明细报销金额',
  `description`     VARCHAR(500)    DEFAULT NULL            COMMENT '单项费用说明',
  `invoice_no`      VARCHAR(64)     DEFAULT NULL            COMMENT '发票号码',
  `invoice_amount`  DECIMAL(14,2)   DEFAULT NULL            COMMENT '发票票面金额',
  `invoice_url`     VARCHAR(512)    DEFAULT NULL            COMMENT '发票电子版PDF或图片链接',
  `budget_id`       BIGINT          DEFAULT NULL            COMMENT '关联预算分配ID',
  `del_flag`        CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_expense_id` (`expense_id`),
  KEY `idx_budget_id` (`budget_id`),
  -- 索引匹配：查询相同发票号。发票号码加上逻辑删除，在应用层校验，此处建立高效过滤索引。
  KEY `idx_invoice_no_lookup` (`invoice_no`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='费用报销明细表';

-- ---------------------------------------------------------------------------
-- 3.4 借款表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_loan`;
CREATE TABLE `fin_loan` (
  `id`                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '借款ID',
  `emp_id`              BIGINT          NOT NULL                COMMENT '借款员工ID',
  `dept_id`             BIGINT          NOT NULL                COMMENT '所属部门ID',
  `loan_amount`         DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '借款总金额',
  `repaid_amount`       DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '已偿还/已核销金额',
  `purpose`             VARCHAR(500)    NOT NULL                COMMENT '借款核心用途',
  `repayment_method`    VARCHAR(32)     NOT NULL DEFAULT 'lump_sum' COMMENT '还款方案(lump_sum=一次性还清 installment=按期分批 offset=后续报销自动冲抵)',
  `expected_repay_date` DATE            NOT NULL                COMMENT '承诺预计还款日期',
  `status`              VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '状态(draft=草稿 pending=审批中 approved=已通过 rejected=已驳回 withdrawn=已撤回 repaying=还款中/未结清 paid_off=已结清)',
  `process_instance_id` BIGINT          DEFAULT NULL            COMMENT '流程实例ID',
  `del_flag`            CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`           VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_status` (`emp_id`, `status`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_expected_repay` (`expected_repay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工借款管理表';

-- ---------------------------------------------------------------------------
-- 3.5 还款与核销流水表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_loan_repayment`;
CREATE TABLE `fin_loan_repayment` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `loan_id`       BIGINT          NOT NULL                COMMENT '关联借款ID',
  `expense_id`    BIGINT          DEFAULT NULL            COMMENT '关联冲抵报销单ID(手动还款则为NULL)',
  `repay_amount`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '单笔还款/核销金额',
  `repay_date`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '还款确认时间',
  `repay_type`    VARCHAR(32)     NOT NULL DEFAULT 'manual' COMMENT '还款类型(manual=财务线下收款 offset=报销单自动冲抵)',
  `remark`        VARCHAR(500)    DEFAULT NULL            COMMENT '备注说明',
  `del_flag`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_loan_id` (`loan_id`),
  KEY `idx_expense_id` (`expense_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='还款与核销流水表';

-- ---------------------------------------------------------------------------
-- 3.6 合同信息表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_contract`;
CREATE TABLE `fin_contract` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `contract_no`     VARCHAR(64)     NOT NULL                COMMENT '合同统一编号',
  `contract_name`   VARCHAR(200)    NOT NULL                COMMENT '合同名称',
  `party_a_id`      BIGINT          NOT NULL                COMMENT '甲方主体ID(关联sys_dept)',
  `party_a_name`    VARCHAR(200)    NOT NULL                COMMENT '甲方主体名称',
  `party_b`         VARCHAR(200)    NOT NULL                COMMENT '乙方主体全称',
  `party_b_contact` VARCHAR(64)     DEFAULT NULL            COMMENT '乙方联系人姓名',
  `party_b_phone`   VARCHAR(20)     DEFAULT NULL            COMMENT '乙方联系人电话',
  `amount`          DECIMAL(16,2)   NOT NULL DEFAULT 0.00   COMMENT '合同签约金额',
  `paid_amount`     DECIMAL(16,2)   NOT NULL DEFAULT 0.00   COMMENT '累计已付出账金额',
  `start_date`      DATE            NOT NULL                COMMENT '合同有效期生效开始日',
  `end_date`        DATE            NOT NULL                COMMENT '合同有效期截止日',
  `sign_date`       DATE            NOT NULL                COMMENT '签署合同日期',
  `contract_type`   VARCHAR(32)     NOT NULL DEFAULT 'purchase' COMMENT '合同类型(purchase=采购服务 sales=销售合同 service=技术服务 labor=劳务外包 lease=租赁合同 other=其他)',
  `category`        VARCHAR(64)     DEFAULT NULL            COMMENT '合同归类分类',
  `status`          VARCHAR(32)     NOT NULL DEFAULT 'draft' COMMENT '状态(draft=草稿 pending=审批中 active=生效中 expiring=即将过期 expired=已过期 terminated=已终止)',
  `manager_id`      BIGINT          NOT NULL                COMMENT '我方合同第一负责人ID(关联sys_employee)',
  `attachment`      VARCHAR(512)    DEFAULT NULL            COMMENT '合同盖章扫描件文件URL',
  `remark`          TEXT            DEFAULT NULL            COMMENT '合同备注文档说明',
  `del_flag`        CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_manager_status` (`manager_id`, `status`),
  KEY `idx_party_a_id` (`party_a_id`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同信息表';

-- ---------------------------------------------------------------------------
-- 3.7 合同付款与支出记录表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `fin_payment`;
CREATE TABLE `fin_payment` (
  `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '付款记录ID',
  `contract_id`   BIGINT          NOT NULL                COMMENT '关联合同ID',
  `expense_id`    BIGINT          DEFAULT NULL            COMMENT '关联费账提报单ID(若通过日常报销单核销付款)',
  `payee`         VARCHAR(200)    NOT NULL                COMMENT '收款账户名(乙方或第三方指定收款方)',
  `payee_account` VARCHAR(64)     NOT NULL                COMMENT '收款银行账号',
  `payee_bank`    VARCHAR(100)    NOT NULL                COMMENT '开户网点银行全称',
  `amount`        DECIMAL(14,2)   NOT NULL DEFAULT 0.00   COMMENT '本次请款/付款金额',
  `pay_date`      DATETIME        DEFAULT NULL            COMMENT '实际出账银行划款日期',
  `payment_type`  VARCHAR(32)     NOT NULL DEFAULT 'bank_transfer' COMMENT '支付工具(cash=线下现金 bank_transfer=网银转账 online=线上快捷 alipay=支付宝 wechat=微信)',
  `remark`        VARCHAR(500)    DEFAULT NULL            COMMENT '付款备注(如：付第二期进度款)',
  `status`        VARCHAR(32)     NOT NULL DEFAULT 'pending' COMMENT '付款状态(pending=出账申请中 approved=审批通过待划账 completed=支付成功已出账 failed=划款失败)',
  `operator_id`   BIGINT          NOT NULL                COMMENT '经办人ID(sys_employee)',
  `approver_id`   BIGINT          DEFAULT NULL            COMMENT '财务复核出纳人ID',
  `process_instance_id` BIGINT    DEFAULT NULL            COMMENT '审批流流程实例ID',
  `del_flag`      CHAR(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 1删除)',
  `create_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     VARCHAR(64)     DEFAULT NULL            COMMENT '更新人',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_expense_id` (`expense_id`),
  KEY `idx_status` (`status`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同付款与支出记录表';
```

---

## 4. API 契约与操作码

接口前缀统一采用 `/api/finance`。响应报文严格遵循：`{"code": 0, "message": "...", "data": ...}`。

### 4.1 核心 REST API 清单

| 模块区域 | HTTP 方法 | 请求路径 | 核心输入数据 (DTO) | 核心输出结构 (VO) | 操作权限标识码 | 职责说明 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **预算配置** | `POST` | `/api/finance/budgets` | `BudgetConfigDTO` | `R<BudgetVO>` | `fin:budget:config` | 新增或导入某部门某分类预算额度 |
| **预算更新** | `PUT` | `/api/finance/budgets/{id}`| `BudgetUpdateDTO` | `R<Boolean>` | `fin:budget:config` | 调整、冻结或废止当前单条预算额度 |
| **预算查询** | `GET` | `/api/finance/budgets` | `BudgetQueryDTO` | `R<Page<BudgetVO>>` | `fin:budget:list` | 分页拉取及多维度检索部门/项目预算余额 |
| **报销申请** | `POST` | `/api/finance/expenses` | `ExpenseApplyDTO` | `R<ExpenseVO>` | `fin:expense:apply` | 提交日常费用报销，包含发票上传与借款冲抵勾选 |
| **报销撤回** | `POST` | `/api/finance/expenses/{id}/withdraw`| - | `R<Boolean>` | `fin:expense:apply` | 在审批流程未结束时主动撤回，释放临时占用预算 |
| **报销检索** | `GET` | `/api/finance/expenses` | `ExpenseQueryDTO` | `R<Page<ExpenseVO>>`| `fin:expense:query` | 员工查询自我报销记录；或财务及审计拉取全量报销 |
| **发票重合度**| `POST` | `/api/finance/expenses/invoice-check`| `InvoiceVerifyDTO` | `R<InvoiceCheckVO>` | `fin:expense:apply` | 发票去重物理检测API，在填报时提供弹窗防重预警 |
| **借支提报** | `POST` | `/api/finance/loans` | `LoanApplyDTO` | `R<LoanVO>` | `fin:loan:apply` | 员工临时借款提报（如出差前款项），绑定还款方案 |
| **还款核销** | `POST` | `/api/finance/loans/{id}/repay`| `LoanRepayDTO` | `R<Boolean>` | `fin:loan:repay` | 手动财务现金结清或报销剩余部分还清核销确认 |
| **借贷检索** | `GET` | `/api/finance/loans` | `LoanQueryDTO` | `R<Page<LoanVO>>` | `fin:loan:query` | 获取名下所有未还清和已还清借贷流水单据 |
| **合同归档** | `POST` | `/api/finance/contracts` | `ContractCreateDTO` | `R<ContractVO>` | `fin:contract:create`| 合同基础属性建档并提交签署流程审批 |
| **合同明细** | `GET` | `/api/finance/contracts/{id}`| - | `R<ContractDetailVO>`| `fin:contract:query` | 抓取单份合同、付款计划与实际已付款流转比例 |
| **付款提报** | `POST` | `/api/finance/payments` | `PaymentApplyDTO` | `R<PaymentVO>` | `fin:payment:apply` | 面向已生效合同发起付款/请款流程申请，联动工作流 |
| **实际付款** | `POST` | `/api/finance/payments/{id}/pay` | `PaymentExecuteDTO` | `R<Boolean>` | `fin:payment:pay` | 出纳点击确认付款：录入网银回单号并更新合同已付金额 |

### 4.2 核心 DTO 结构与核心字段

#### 4.2.1 `ExpenseApplyDTO` (报销单提报)
```json
{
  "title": "2026年6月研发二部团建与交通费报销",
  "relatedBusinessTripId": null,
  "relatedLoanId": 12,
  "description": "二季度团队技术研讨以及部分员工加班打车费报销",
  "details": [
    {
      "expenseDate": "2026-05-28",
      "expenseType": "meal",
      "amount": 1200.00,
      "description": "5.28 团队研发交流晚宴",
      "invoiceNo": "011002200311_55489622",
      "invoiceAmount": 1200.00,
      "invoiceUrl": "https://oss.oa.company/invoices/2026/06/inv001.pdf",
      "budgetId": 105
    },
    {
      "expenseDate": "2026-05-29",
      "expenseType": "transport",
      "amount": 120.50,
      "description": "5.29 深夜加班打车回家",
      "invoiceNo": "011002200422_66589233",
      "invoiceAmount": 120.50,
      "invoiceUrl": "https://oss.oa.company/invoices/2026/06/inv002.jpg",
      "budgetId": 106
    }
  ]
}
```

#### 4.2.2 `LoanApplyDTO` (借款提报)
```json
{
  "loanAmount": 5000.00,
  "purpose": "赴北京大兴机场线客户现场网络割接，预计食宿及紧急耗材采购垫付",
  "repaymentMethod": "offset",
  "expectedRepayDate": "2026-06-30"
}
```

#### 4.2.3 `ContractCreateDTO` (合同录入)
```json
{
  "contractNo": "CON-RD-2026-008",
  "contractName": "2026办公自动化服务器维保及云计算采购协议",
  "partyAId": 2,
  "partyAName": "北京智核科技有限公司研发中心",
  "partyB": "阿里云计算有限公司",
  "partyBContact": "李经理",
  "partyBPhone": "13800002222",
  "amount": 48000.00,
  "startDate": "2026-07-01",
  "endDate": "2027-06-30",
  "signDate": "2026-06-03",
  "contractType": "service",
  "attachment": "https://oss.oa.company/contracts/2026/con_008_signed.pdf",
  "remark": "包含全年云服务器安全防护与无缝升级支持"
}
```

---

## 5. 任务波次拆分 (Wave 1 至 Wave 5)

模块开发严格按契约进行，在前面的波次未完成验收前，严禁越级或提前进入后续的构建。

### Wave 1: 契约、基线与旧入口分析

#### T1 数据库模型重构与 API 契约落地

*   **目标**：在 `sql/baseline/` 下优化和完善升级 `fin_*` 7张表 DDL。确认并规范财务领域所有 DTO、VO 字段与 Java 数据映射，梳理完整的权限码和路径。
*   **涉及路径**：`code/backend/sql/baseline/001_schema.sql`，`docs/superpowers/specs/2026-06-02-fin-budget-expense-task-split.md`
*   **输入**：重构大纲中财务子章节、以及本设计文档第3章 DDL 草案。
*   **输出**：可在空环境直接执行并完成约束级初始化的 `fin_` 数据表 DDL 部分、以及完备的 API Java 接口骨架。
*   **禁止修改**：禁止直接编写任何具体的 Service 或者是 Controller 的业务层逻辑代码。
*   **验收标准**：通过本地数据库工具执行升级 DDL 语句无报错，表字段属性、自增主键、索引性能要求与备注说明完全对齐。

#### T2 旧系统财务逻辑分析与旧入口移除清单

*   **目标**：对现有旧系统包含的旧报销（`oa_expense`）、旧借款（`oa_loan`）、旧合同（`oa_contract`）实体、XML Mapper、业务逻辑进行底层排查，评估升级影响。
*   **涉及路径**：`code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、`code/frontend/src/api/` 等。
*   **输出**：输出精确到行和类的旧代码保留/下线映射矩阵、路由兼容机制。
*   **禁止修改**：在本波次中，严禁删除任何线上现存的旧逻辑，保持代码完整运行。
*   **验收标准**：输出的影响分析报告完整覆盖后端所有依赖调用链、前端及移动端旧 API 地址。

---

### Wave 2: 后端核心业务层 (T3/T4/T5)

#### T3 Finance 实体、Mapper 与多租户/软删除约束

*   **目标**：在 `oa-finance` 模块或相关子目录下（新建或升级），完全建立预算、报销、明细、借贷、还款核销、合同、付款的 Entity 实体层和 MyBatis-Plus Mapper 接口。
*   **涉及路径**：`code/backend/oa-finance/src/main/java/cn/oa/finance/entity/`，`cn/oa/finance/mapper/`
*   **输出**：7张表完全对齐的 Java 实体类（含 Lombok, MyBatisPlus 注解），以及带有逻辑删除 `@TableLogic` 机制的 MP Mapper 接口。
*   **禁止修改**：不实现任何 Service、Controller 等控制和流程流转业务逻辑。
*   **验收标准**：在 `oa-finance` 下运行 `mvn clean compile` 及编写基础 Mapper 单元测试，连接数据库读写基础记录通过。

#### T4 费控报销核心业务逻辑 Service 实现 (核心重点)

*   **目标**：开发报销单据提交及超支校验、预算原子冻结（通过、驳回、撤销时的释放扣减）、借贷核销冲抵联动流水的核心 Service 实现。
*   **涉及路径**：`code/backend/oa-finance/src/main/java/cn/oa/finance/service/impl/`
*   **输出**：`BudgetService`、`ExpenseService`、`LoanService` 核心逻辑实现，包含事务驱动，高并发加锁控制代码，发票号码唯一性查重校验逻辑。
*   **禁止修改**：禁止为了调试而在 Service 层直接编写依赖或调用流程引擎具体的内部表数据的脏代码。必须遵循微内核与领域事件/通用 API 的解耦要求。
*   **并发要求**：涉及预算占用和核销的更新必须使用乐观锁 + 分布式排它锁（采用 Redis/Redisson）。
*   **验收标准**：编写针对性的 Service 单元测试类，重点断言：并发报销下的预算冻结金额没有产生坏账；相同发票多次提交时系统能成功发现并抛出 `BusinessError`；借支金额冲抵逻辑结果核算正确。

#### T5 费控报销 REST API 开发与校验层拦截

*   **目标**：根据 REST 规范，编写对外的 Controller 实体，使用 `@Valid` 强制入参属性完整性校验，并利用 SecurityContext 获取真实的员工账户。
*   **涉及路径**：`code/backend/oa-finance/src/main/java/cn/oa/finance/controller/`
*   **输出**：暴露 14 个核心 API，绑定完整的操作权限标识码。
*   **禁止修改**：禁止将任何重度业务逻辑沉淀在 Controller 层；Controller 仅起参数验证、会话注入和异常封装作用。
*   **验收标准**：`mvn -pl oa-finance,oa-web -am test`。所有 Controller 单元测试覆盖率达到 85% 以上，接口入参校验拦截正常。

---

### Wave 3: 工作流与通知事件联动

#### T6 费控报销涉财审批工作流回调处理器 (BPMN 业务映射)

*   **目标**：对接 `oa-workflow` 引擎回调（`WorkflowCallbackDispatcher`），实现报销审批通过（付款完成）、报销驳回/撤销、借款生效、合同签署、付款成功等业务回调状态机转变。
*   **涉及路径**：`code/backend/oa-finance/src/main/java/cn/oa/finance/callback/`
*   **输出**：报销回调 Handler、借支回调 Handler、合同回调 Handler、付款回调 Handler 注册。
*   **业务状态变更规则**：
    *   报销通过 (`onApproved`)：状态置为 `approved`（进入付款排队），预算中占用金额正式扣减并转为 `executed_amount`。如果勾选了冲抵借贷，自动执行 `fin_loan` 冲抵事务更新。
    *   报销驳回 (`onRejected`)：状态置为 `rejected`，释放对应的 `occupied_amount` 预算。
*   **禁止修改**：严禁在 `oa-workflow-core` 中直接 `import cn.oa.finance.*`，流程回调必须通过通用的业务类型名称字符串（`expense`、`loan`、`contract`、`payment`）及通用 ID 进行驱动。
*   **验收标准**：通过工作流回调 Mock 模拟测试类验证，向回调入口发送审批通过/驳回指令，对应财务模块数据变化完全正确。

#### T7 预算超额与付款到期的待办消息联动

*   **目标**：当财务动作触达时，向消息中台（`oa-message`）投递领域事件。例如，报销超支预警、付款到期提醒、借贷超期催还。
*   **涉及路径**：`code/backend/oa-finance/src/main/java/cn/oa/finance/event/`
*   **输出**：`BudgetExceededEvent`、`PaymentDueEvent`、`LoanOverdueEvent` 事件定义与发布，消息订阅监听器编写。
*   **禁止修改**：不在财务模块写任何具体的邮件发送（SMTP）、短信发送等涉及第三方基础设施适配的底层细节。
*   **验收标准**：模拟触发事件，`oa-message` 成功解析事件，生成了格式规范的站内信以及 WebSocket 推送内容。

---

### Wave 4: 管理端与移动端业务功能

#### T8 费控管理端 Web 界面迁移与升级 (Vue 3)

*   **目标**：编写 Web 管理端的界面，实现：1) 部门预算导入与超支警示设定；2) 员工多明细报销提报及发票附件预览；3) 借贷申请与还款管理；4) 合同签署状态与付款进度仪表盘。
*   **涉及路径**：`code/frontend/src/views/finance/`，`code/frontend/src/api/finance.ts`
*   **输出**：完全适配 Element Plus 框架及 TypeScript 实体的费控管理前端。
*   **禁止修改**：不破坏 `vue-pure-admin` 现有的权限控制、布局引擎及主题渲染内核。
*   **验收标准**：`cd code/frontend && pnpm typecheck && pnpm build` 编译完美通过。

#### T9 uni-app 移动端费控报销组件开发 (H5/WeChat)

*   **目标**：开发移动端费控提报与审批组件。允许员工随时随地手机拍照上传发票、提交报销、申请借款。
*   **涉及路径**：`code/mobile/src/pages/finance/`，`code/mobile/src/api/finance.ts`
*   **输出**：支持多项费用追加、拍照预览附件、借贷待还列表展示的移动端组件。
*   **禁止修改**：避免在移动端提供复杂的后台预算配置和复杂的合同图表统计（移动端只保留核心的报销提报、借支提报和财务审批详情）。
*   **验收标准**：`cd code/mobile && pnpm build:h5` 及微信小程序打包通过。

---

### Wave 5: 端到端回归测试、清理与下线准备

#### T10 费控与合同管理完整闭环端到端回归测试 (E2E)

*   **目标**：全链路回归。流程包括：录入部门年度预算 -> 员工借款审批 -> 发起报销 -> 选择报销单进行合同/借款关联 -> 冲抵借款核销 -> 报销审批驳回（或通过） -> 预算扣减回退（或实付结清） -> 生成付款流水 -> 消息通知。
*   **涉及路径**：`code/backend/src/test/`，本地或 CI/CD 测试管道。
*   **输出**：覆盖全生命周期 100+ 场景的 E2E 自动化测试脚本或详细手工黑盒测试操作日志。
*   **验收标准**：在后端、前端、移动端全部服务启动时，测试流在没有任何人工后台数据库脏干预的前提下，能自闭环流转，所有的金额滚算精确到 `0.01` 无任何差异。

#### T11 旧入口清理与业务一键割接方案

*   **目标**：将原有的 `oa_expense`、`oa_loan`、`oa_contract` 等旧表与相关死代码、过时视图文件和菜单项彻底移除。
*   **涉及路径**：`code/backend/` 各旧子模块、`code/frontend/` 路由与 API 模块。
*   **输出**：旧代码彻底移除 Git Commit、旧表删除迁移脚本、单向备份割接方案。
*   **禁止修改**：如果在 T10 的端到端验收中存在一处阻断性 Bug，绝对禁止执行此步。
*   **验收标准**：旧表、旧 API 彻底下线，系统无法再访问任何带有 `oa_expense` 或 `oa_loan` 的资源，控制台和日志中无任何因移除旧接口导致的编译与加载报错。

---

## 6. Claude Code 可执行提示词

在实际使用 AI 研发助手执行该重构计划时，可将以下标准提示词按步骤输入：

### 6.1 T3-Entity/Mapper 生成提示词
```text
请执行费控报销(oa-finance)模块的 T3 任务：Entity 与 Mapper 层构建。

必须先阅读：
- docs/superpowers/specs/2026-06-02-fin-budget-expense-task-split.md (重点阅读第3章的 DDL 规范设计)
- code/backend/sql/baseline/001_schema.sql (参考其中 fin_* 的基本约束)

开发要求：
1. 在 oa-finance 模块下（如果不存在，请按照大纲规范在 code/backend/ 下查找或定位到合适的 Maven 模块），按照 cn.oa.finance 包路径分别建立：
   - entity: Budget.java, Expense.java, ExpenseDetail.java, Loan.java, LoanRepayment.java, Contract.java, Payment.java
   - mapper: BudgetMapper.java, ExpenseMapper.java, ExpenseDetailMapper.java, LoanMapper.java, LoanRepaymentMapper.java, ContractMapper.java, PaymentMapper.java
2. 实体层必须整合 MyBatis-Plus 规范：
   - 字段delFlag映射软删除 @TableLogic(value = "0", delval = "1")
   - id 使用 @TableId(type = IdType.AUTO)
   - 对 fin_budget，版本字段 version 需配置乐观锁注解 @Version
3. 使用 Lombok 的 @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor 简化代码，不要生成冗长的 getter/setter。
4. 所有的枚举字段（如费用类别、合同类型、控制策略、各种单据状态）在 entity 中一律采用 String 保存，保持良好的数据库兼容性。并在 cn.oa.finance.enums 包下建立对应的规范字符串常量类或标准枚举。

禁止修改：不编写 Service 和 Controller；不改前端和移动端；不修改数据库 baseline。

完成后，在 code/backend 目录下执行：
mvn clean compile -pl oa-finance -am
确保没有任何编译报错！
```

### 6.2 T4-Service 业务层逻辑生成提示词
```text
请执行费控报销(oa-finance)模块的 T4 任务：核心 Service 层研发。

必须先阅读：
- docs/superpowers/specs/2026-06-02-fin-budget-expense-task-split.md (重点阅读第1章中的预算控制、借贷联动以及付款计划逻辑)

必须研发的核心业务能力与容错设计：
1. BudgetService:
   - 占用预算(occupyBudget)：扣除前需判定是否为硬控制。若硬控制且预算不足，抛出 BusinessError 拒绝。更新时使用乐观锁 version 防并发冲突。
   - 确认预算(confirmBudget)：审批通过回调，由临时占用划入已执行。
   - 释放预算(releaseBudget)：审批失败/撤销回调，临时占用扣减回滚。
2. ExpenseService:
   - 提报报销：支持多明细同时保存。必须实现【发票防重校验】：在 fin_expense_detail 检索是否存在 del_flag = '0' 且 invoice_no 相同的其他发票，若有则抛出 BusinessError。
   - 如果用户选择了冲抵借贷(relatedLoanId)，需校验该借款是否属于该员工且处于未结清(repaying)状态，并将 offset_amount 及 pay_amount 精确计算存入。
3. LoanService:
   - 提报借款。
   - 还款/核销：提供 repayLoan 方法，当报销审批通过触发核销时，向 fin_loan_repayment 插入 offset 冲抵流水，并将 repaid_amount 滚算加入，若足额还清自动更新状态为 paid_off。
4. ContractService & PaymentService:
   - 实现合同建档、付款请款申请与确认付款(pay)。确认付款时自动更新 fin_contract 累计已付金额。
5. 所有方法涉及主子表修改、多表状态流转的必须包裹在 Spring @Transactional(rollbackFor = Exception.class) 中。

测试覆盖：
在 E:\JavaProject\Corporate_OA_System\code\backend\oa-finance\src\test\java\cn\oa\finance\service\ 下编写集成的 JUnit 单元测试，重点测试发票号码冲突、并发预算冻结、借款自动冲抵等场景。

完成后，在 code/backend 目录下执行：
mvn clean test -pl oa-finance -am
确保编译和所有单元测试断言 100% 成功通过！
```

### 6.3 T5-Controller 控制层开发提示词
```text
请执行费控报销(oa-finance)模块的 T5 任务：REST API Controller 开发。

开发要求：
1. 在 cn.oa.finance.controller 包下，编写各模块 Controller，对齐 API 契约设计（第 4 章）：
   - BudgetController, ExpenseController, LoanController, ContractController, PaymentController
2. 所有 REST 路径以 /api/finance 开头。
3. 对提报、修改等写入方法，必须使用 @Validated 并配合 JSR303 验证注解进行入参格式校验。
4. 统一响应格式：使用 R.ok(data) 或 R.failed(msg) 的形式返回。
5. 从当前的 UserContext 中获取提报的员工 emp_id，禁止从客户端入参传入，确保数据隔离性。
6. 所有的核心写与读权限，必须打上标准的权限标识注解，如：
   - @RequirePermission("fin:expense:apply")
   - @RequirePermission("fin:budget:config")

禁止：不写复杂的业务处理，直接委派给 T4 阶段开发好的 Service 实现。

完成后，在 code/backend 目录下运行以下命令进行构建和测试：
mvn clean test -pl oa-finance,oa-web -am
确保 Controller 启动与参数校验端点功能无误！
```

### 6.4 T8-T9 前端与移动端聚合开发提示词
```text
请执行费控报销(oa-finance)模块的 T8/T9 任务：前端管理端与移动端研发。

开发指南：
1. 查阅 API 契约（第 4 章）及定义的 DTO 属性。
2. Web 管理端（Vue 3 + Element Plus）：
   - 在 code/frontend/src/api/finance.ts 定义完全 typed 且带有 Bearer Auth 传递的 axios 调用器。
   - 在 code/frontend/src/views/finance/ 编写：
     * BudgetList.vue: 预算列表、导入分配弹窗、乐观锁冲突捕获、超支警示开关。
     * ExpenseApply.vue: 报销申请单（支持多行明细添加，带 invoiceNo 填写，带 OSS 上传发票按钮，借款冲抵选项列表）。
     * LoanList.vue: 个人借款明细与还款进度，财务确认收款弹窗。
     * ContractDashboard.vue: 合同概览、已付/签约金额比例图、付款请款列表。
3. 移动端 (uni-app + Vue 3)：
   - 在 code/mobile/src/api/finance.ts 增加移动端对应的 API 入口服务。
   - 在 code/mobile/src/pages/finance/ 建立：
     * apply.vue: 便捷多项费用报销，支持微信小程序摄像头拍照上传发票。
     * loans.vue: 借还款便民通道。
     * approval-detail.vue: 移动端涉财合同、付款、报销审批详情页（突出预算超限的黄色警示）。

验收标准：
- 执行 cd code/frontend && pnpm typecheck && pnpm build 确保前端构建无误。
- 执行 cd code/mobile && pnpm build:h5 确保移动端编译正常。
```

---

## 7. 模块演进与升级回滚策略

为确保系统重构中的高可用度，必须预备回退防御举措：

### 7.1 失败与性能退级场景预案

| 异常事件 | 原因判定 | 应急处理与退级方案 |
| :--- | :--- | :--- |
| **高并发下发生大量乐观锁冲突** | 多员工同一分钟提报，导致 `fin_budget` 被抢占更新。 | 触发自动重试机制：在 `BudgetService` 的更新方法上增加 `@Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3)`，利用 Hutool 延迟重试。若重试依然失败，则在前端友好提示：“当前预算服务繁忙，系统正在重试排队中，请2秒后刷新查看”。 |
| **电子发票去重造成严重的慢查询** | `fin_expense_detail` 数据量突破百万，在逻辑匹配时没有命中索引。 | 1. 强制在数据库层优化建立 `idx_invoice_no_lookup` 联合索引；<br>2. 引入 Redis 缓存哈希去重：每次发票审批通过后，将 `invoice_no` 作为 String 写入 Redis `finance:invoice:set`（永久保存），在提报的第一时间利用 Redis 的 `SISMEMBER` 进行 O(1) 速度的一级瞬时过滤，若可能存在冲突再下钻到数据库中通过锁进行校验。 |
| **回调联动造成严重的分布式事务中断** | 工作流最终通过，但由于打款失败或数据库网络中断，报销已变成已付款，但是借款未核销。 | 本地事务控制，回调全部被包裹在 `@Transactional`。对于工作流回调中发生的逻辑中断，向工作流引擎抛出 Exception，迫使 `oa-workflow` 状态回滚并进入“审批异常”待人工核算状态；同时在 `oa-finance` 后台产生日志报警。 |

### 7.2 回滚执行指令
*   **后端回退**：若重构导致严重的线上异常，利用 Git 执行：
    ```bash
    git checkout master -- code/backend/oa-finance/
    git checkout master -- code/backend/sql/baseline/
    ```
    重置所有财务相关类，回滚数据库 schema 升级部分。
*   **前端与移动端回退**：
    ```bash
    git checkout master -- code/frontend/src/views/finance/
    git checkout master -- code/mobile/src/pages/finance/
    ```

---

## 8. 变更日志

| 日期 | 版本 | 变更人 | 变更说明 |
| :--- | :--- | :--- | :--- |
| 2026-06-03 | v1.0 | Claude Code | 初始创建。基于微内核及务实DDD重构思想，完整编写了费控与报销（oa-finance）预算、借支、报销核销联动、合同付款全闭环设计方案。 |
