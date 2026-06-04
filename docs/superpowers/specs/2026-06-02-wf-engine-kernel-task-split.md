# 工作流微内核引擎 (oa-workflow) 重构实施与任务拆分

> 日期: 2026-06-02  
> 模块: `oa-workflow` (工作流微内核引擎)  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  

---

## 1. 模块说明与目标

工作流微内核引擎（`oa-workflow`）是企业 OA 系统的核心调度中枢。本引擎采用**"微内核 + 策略插件 + 异步事件驱动"**的务实 DDD 架构，旨在构建一个轻量级、高性能、不依赖外部大型 BPMN 引擎（如 Flowable / Camunda）的数据驱动状态机。

### 1.1 核心目标与技术特性

1. **流程定义设计器结构（低耦合 JSON-Graph）**  
   定义器采用有向图（Directed Graph）逻辑，支持通过定义节点（Nodes）和流转规则（Transitions）来表达复杂的业务流程，并通过 JSON 快照版本控制，提供极其灵活的流程定义管理。
2. **状态机调度驱动**  
   使用状态机（StateMachine）驱动流程实例（Instance）在 `DRAFT -> RUNNING -> PASSED/REJECTED/REVOKED/SUSPENDED/ABORTED` 之间的转换。状态机的每一次扭转都会触发相应的任务（Task）生命周期。
3. **策略链解析审批人（AssigneeResolverChain）**  
   采用策略链模式解耦审批人获取规则，支持固定人（FIXED_USER）、岗位（POST）、部门负责人（DEPT_LEADER）、汇报线（REPORT_LINE）等策略。
4. **会签/或签/比例审批/投票汇总算法设计**  
   内置多模式审批决策器。会签要求全员通过；或签满足一人即可；比例审批（Proportional）按通过比例判断；投票（Vote）按多数票判断。
5. **版本控制与快照运行机制**  
   在启动流程实例时，系统会将当前的流程定义、节点、跳转规则和审批规则全量转化为 JSON 存储在 `wf_instance.def_snapshot` 中。后续的所有审批、跳转动作全部基于快照运行，保证了流程定义的“热修改”不会破坏正在运行的实例。
6. **前/后加签挂起与恢复设计**  
   支持审批人员在当前节点进行前加签（PRE_ADD_SIGN）或后加签（POST_ADD_SIGN）。前加签时，原任务进入 `SUSPENDED` 挂起状态，优先执行加签子任务，加签完成自动恢复原任务；后加签时，加签任务在当前审批人通过后触发。
7. **转办与催办实现**  
   转办（Transfer）实现将当前 PENDING 状态的任务委派给其他员工，记录流转日志。催办（Urge）通过发送 WebSocket/IM 消息，限制催办频率，防止消息轰炸。
8. **与业务服务的回调解耦分发机制（WorkflowCallbackDispatcher）**  
   引擎内核不直接依赖任何具体的业务包（如 `oa-hr`、`oa-finance`）。通过应用层领域事件与 `WorkflowCallbackDispatcher`，当状态发生终转（Approved / Rejected / Revoked）时，分发器通过 `BusinessType` 映射自动路由到相应的业务回调处理器（`WorkflowCallbackHandler`）中，确保强一致的业务状态流转。

---

## 2. 边界定义

### 2.1 本内核包含 (In-Scope)

1. **工作流全套状态机计算与转换**：流程及任务状态的变更校验、分支路径合并与排他跳转判定。
2. **审批人计算与策略链执行**：根据流程规则动态计算审批人，完成委托（Delegation）重定向。
3. **标准八张表的数据结构维护与 DDL 基线**。
4. **统一的任务生命周期管理 API**：提供供前端、移动端和业务逻辑调用的标准契约。
5. **通用流程图展现数据结构**：返回前端渲染流程图所需的 Node/Transition 关系模型。

### 2.2 本内核排除 (Out-of-Scope)

1. **复杂的动态表单生成器设计**：内核仅负责存储 DTO 中的 `formData` 字段用于条件表达式判定，不实现表单 UI 渲染器及拖拽设计器，表单完全由前端业务代码自主声明。
2. **与 BPMN 2.0 规范的转换**：排除繁琐的 XML schema 与 BPMN2.0 协议解析。引擎采用自研的轻量级 JSON 状态图。
3. **业务单据字段存储**：内核不保存任何人事请假天数、报销金额等具体业务单据字段，只通过 `business_type` 和 `business_id` 关联业务主键。

---

## 3. 引擎核心实现细节与算法

### 3.1 会签/或签决策器（evaluateSign）

在每个任务完成后，调度引擎需要判断当前节点的审批任务集（Tasks）是否已满足跳转到下一节点的条件。以下为 `evaluateSign` 算法伪代码：

```java
public class SignEvaluator {
    
    public enum ApprovalMode {
        SEQUENTIAL,   // 依次审批
        COUNTERSIGN,  // 会签（全票通过）
        ORSIGN,        // 或签（一票通过，其余任务自动取消）
        PROPORTIONAL, // 比例审批（通过率 >= 阈值）
        VOTE          // 投票（多数票通过）
    }

    public EvaluationResult evaluateNodeStatus(List<WfTask> nodeTasks, WfNode node) {
        ApprovalMode mode = ApprovalMode.valueOf(node.getApprovalMode());
        if (nodeTasks.isEmpty()) {
            return EvaluationResult.pending();
        }

        long totalCount = nodeTasks.size();
        long approvedCount = nodeTasks.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
        long rejectedCount = nodeTasks.stream().filter(t -> "REJECTED".equals(t.getStatus())).count();
        long pendingCount = nodeTasks.stream().filter(t -> "PENDING".equals(t.getStatus()) || "SUSPENDED".equals(t.getStatus())).count();

        switch (mode) {
            case COUNTERSIGN:
                if (rejectedCount > 0) {
                    return EvaluationResult.reject(); // 只要有一个拒绝，立刻拒绝整个节点
                }
                if (approvedCount == totalCount) {
                    return EvaluationResult.pass();   // 全员通过
                }
                return EvaluationResult.pending();    // 仍有人未审批

            case ORSIGN:
                if (approvedCount > 0) {
                    return EvaluationResult.pass();   // 只要一个通过，立刻通过
                }
                if (rejectedCount == totalCount) {
                    return EvaluationResult.reject(); // 所有人拒绝才拒绝
                }
                return EvaluationResult.pending();

            case PROPORTIONAL:
                double threshold = node.getPassRatio().doubleValue(); // 例如 80.00%
                double currentApprovedRatio = ((double) approvedCount / totalCount) * 100;
                double maxPossibleApprovedRatio = ((double) (approvedCount + pendingCount) / totalCount) * 100;

                if (currentApprovedRatio >= threshold) {
                    return EvaluationResult.pass();
                }
                if (maxPossibleApprovedRatio < threshold) {
                    return EvaluationResult.reject(); // 即使剩余的人全过，也达不到比例，提早拒绝
                }
                return EvaluationResult.pending();

            case VOTE:
                double majorityThreshold = 50.0;
                double appRatio = ((double) approvedCount / totalCount) * 100;
                double rejRatio = ((double) rejectedCount / totalCount) * 100;

                if (appRatio > majorityThreshold) {
                    return EvaluationResult.pass();   // 过半数同意
                }
                if (rejRatio >= majorityThreshold) {
                    return EvaluationResult.reject(); // 过半数（或等于半数）拒绝
                }
                return EvaluationResult.pending();

            case SEQUENTIAL:
            default:
                // 依次审批模式下，如果有人拒绝则拒绝；若最后一个审批完则通过
                if (rejectedCount > 0) {
                    return EvaluationResult.reject();
                }
                if (pendingCount == 0) {
                    return EvaluationResult.pass();
                }
                return EvaluationResult.pending();
        }
    }
}
```

### 3.2 审批人策略解析（ResolverChain）

审批人解析流程包含两阶段：策略解析 与 委托重定向。

```
[开始解析节点审批人]
       │
       ▼
[读取节点绑定的所有规则(wf_assignee_rule)]
       │
       ▼
[按 sort_order 逐一执行策略解析器]
  ├── FIXED_USER ──► 提取固定用户ID列表
  ├── POST ────────► 查询部门/全局岗位关联的员工
  ├── DEPT_LEADER ─► 查询申请人所在部门的主管负责人
  └── REPORT_LINE ─► 查询向上层汇报线的直接领导
       │
       ▼
[汇总审批人列表并去重 (Set<Long>)]
       │
       ▼
[是否为空(assigneeList.isEmpty)?]
  ├── 是 ──► [执行审批人为空兜底策略]
  │            ├── AUTO_PASS   ──► 标记节点跳过
  │            ├── AUTO_REJECT ──► 驳回流程
  │            └── ASSIGN_ADMIN ─► 分配给超级管理员
  └── 否 ──► 进入委托判定
       │
       ▼
[遍历审批人列表，检测生效委托(wf_delegation)]
  ├── 命中委托 ──► [委托循环判定：检测是否出现 A->B->A 环形委托]
  │                  ├── 检测到环 ──► 抛出委托循环异常，熔断
  │                  └── 未检测到 ──► 替换当前审批人为被委托人(DelegateId)
  └── 未命中 ────► 保持不变
       │
       ▼
[生成任务列表 (wf_task)]
```

### 3.3 环形委托检测算法实现 (Java 伪代码)

```java
public class DelegationResolver {

    public Long resolveDelegate(Long originalAssigneeId, String category, LocalDate date, Map<Long, Boolean> visited) {
        if (visited.containsKey(originalAssigneeId)) {
            throw new WorkflowException("检测到环形委托链路！断开节点员工ID: " + originalAssigneeId);
        }
        
        visited.put(originalAssigneeId, true);
        
        // 查找是否有当前生效的委托配置
        WfDelegation delegation = delegationMapper.selectActiveDelegation(originalAssigneeId, category, date);
        if (delegation == null) {
            return originalAssigneeId; // 无委托，原样返回
        }
        
        // 递归解析多级委托
        return resolveDelegate(delegation.getDelegateId(), category, date, visited);
    }
}
```

---

## 4. 核心数据模型建表 DDL (MySQL 8.0)

以下为工作流核心的 8 张表建表语句。所有主键均采用 `BIGINT AUTO_INCREMENT`，支持与外部生成的 Snowflake ID 契约兼容，字符集统一为 `utf8mb4`。

```sql
-- 1. 流程定义表
CREATE TABLE `wf_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '定义ID',
  `code` VARCHAR(64) NOT NULL COMMENT '流程编码(如: leave, trip, expense)',
  `name` VARCHAR(128) NOT NULL COMMENT '流程名称',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `category` VARCHAR(64) NOT NULL COMMENT '分类(hr/finance/admin/common)',
  `form_def_id` BIGINT DEFAULT NULL COMMENT '关联表单定义ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT-草稿/PUBLISHED-已发布/DISABLED-已禁用)',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0-正常, 1-删除)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_def_code_version` (`code`, `version`),
  KEY `idx_def_category` (`category`),
  KEY `idx_def_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';

-- 2. 流程节点表
CREATE TABLE `wf_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `def_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `node_code` VARCHAR(64) NOT NULL COMMENT '节点编码(如: start, end, node_1)',
  `node_name` VARCHAR(128) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型(START/END/APPROVAL/SUBPROCESS/CONDITION/GATEWAY)',
  `approval_mode` VARCHAR(32) DEFAULT NULL COMMENT '审批模式(SEQUENTIAL/COUNTERSIGN/ORSIGN/PROPORTIONAL/VOTE)',
  `pass_ratio` DECIMAL(5,2) DEFAULT NULL COMMENT '通过比例(PROPORTIONAL模式使用, 百分制, 如 80.00)',
  `timeout_hours` INT DEFAULT NULL COMMENT '超时限制时间(小时)',
  `timeout_action` VARCHAR(32) DEFAULT NULL COMMENT '超时动作(AUTO_PASS/AUTO_REJECT/NOTIFY)',
  `field_permission` JSON DEFAULT NULL COMMENT '字段权限矩阵JSON {"field": "readonly/hidden/editable"}',
  `empty_assignee_strategy` VARCHAR(32) NOT NULL DEFAULT 'AUTO_PASS' COMMENT '审批人为空策略(AUTO_PASS/AUTO_REJECT/ASSIGN_ADMIN/ERROR)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_def_code` (`def_id`, `node_code`),
  KEY `idx_node_def` (`def_id`),
  CONSTRAINT `fk_node_def_id` FOREIGN KEY (`def_id`) REFERENCES `wf_definition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点表';

-- 3. 流转条件表
CREATE TABLE `wf_transition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流转ID',
  `def_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `from_node_id` BIGINT NOT NULL COMMENT '源节点ID',
  `to_node_id` BIGINT NOT NULL COMMENT '目标节点ID',
  `expression` VARCHAR(500) DEFAULT NULL COMMENT '流转条件表达式(SpEL)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '跳转判定优先级',
  PRIMARY KEY (`id`),
  KEY `idx_trans_def` (`def_id`),
  KEY `idx_trans_from` (`from_node_id`),
  CONSTRAINT `fk_trans_def_id` FOREIGN KEY (`def_id`) REFERENCES `wf_definition` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trans_from_node` FOREIGN KEY (`from_node_id`) REFERENCES `wf_node` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_trans_to_node` FOREIGN KEY (`to_node_id`) REFERENCES `wf_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流转条件表';

-- 4. 审批人规则表
CREATE TABLE `wf_assignee_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `node_id` BIGINT NOT NULL COMMENT '节点ID',
  `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型(FIXED_USER/POST/DEPT_LEADER/REPORT_LINE/FORM_SELECT/API)',
  `rule_value` VARCHAR(500) NOT NULL COMMENT '规则匹配数据值',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '匹配优先级',
  PRIMARY KEY (`id`),
  KEY `idx_rule_node` (`node_id`),
  CONSTRAINT `fk_rule_node_id` FOREIGN KEY (`node_id`) REFERENCES `wf_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批人规则表';

-- 5. 流程实例表
CREATE TABLE `wf_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '实例ID',
  `def_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `def_version` INT NOT NULL COMMENT '流程启动时绑定版本号',
  `def_snapshot` JSON NOT NULL COMMENT '定义快照(保存节点、流转及审批人规则，防篡改和热变动影响)',
  `business_type` VARCHAR(64) NOT NULL COMMENT '业务单据类型(leave/trip/expense等)',
  `business_id` BIGINT NOT NULL COMMENT '关联业务主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '流程标题',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT '实例状态(RUNNING/PASSED/REJECTED/REVOKED/SUSPENDED/ABORTED)',
  `current_node_ids` VARCHAR(500) DEFAULT NULL COMMENT '当前所处节点ID集合(支持多节点并行，JSON String Array)',
  `return_source_node_id` BIGINT DEFAULT NULL COMMENT '驳回发起节点ID',
  `return_strategy` VARCHAR(32) NOT NULL DEFAULT 'DIRECT_RETURN' COMMENT '重新提交流转策略(DIRECT_RETURN-直回驳回节点, SEQUENTIAL_RETURN-重新走完整流程)',
  `start_time` DATETIME NOT NULL COMMENT '流程启动时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '流程结束时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0-正常, 1-删除)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_inst_def` (`def_id`),
  KEY `idx_inst_business` (`business_type`, `business_id`),
  KEY `idx_inst_applicant` (`applicant_id`),
  KEY `idx_inst_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';

-- 6. 审批任务表
CREATE TABLE `wf_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `node_id` BIGINT NOT NULL COMMENT '当前节点ID',
  `node_name` VARCHAR(128) NOT NULL COMMENT '冗余节点名称',
  `assignee_id` BIGINT NOT NULL COMMENT '审批责任人ID',
  `task_type` VARCHAR(32) NOT NULL DEFAULT 'TODO' COMMENT '任务类型(TODO/COUNTERSIGN/PRE_ADD_SIGN/POST_ADD_SIGN)',
  `parent_task_id` BIGINT DEFAULT NULL COMMENT '父任务ID(加签挂起与分支任务关联场景使用)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态(PENDING/APPROVED/REJECTED/TRANSFERRED/SUSPENDED/CANCELED)',
  `opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `signature` VARCHAR(512) DEFAULT NULL COMMENT '电子签名图片地址或哈希摘要值',
  `due_time` DATETIME DEFAULT NULL COMMENT '到期时限',
  `remind_count` INT NOT NULL DEFAULT 0 COMMENT '催办发送次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_instance` (`instance_id`),
  KEY `idx_task_assignee` (`assignee_id`, `status`),
  KEY `idx_task_status` (`status`),
  CONSTRAINT `fk_task_instance_id` FOREIGN KEY (`instance_id`) REFERENCES `wf_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批任务表';

-- 7. 流程审计记录表
CREATE TABLE `wf_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '流转日志记录ID',
  `instance_id` BIGINT NOT NULL COMMENT '流程实例ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '关联任务ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) NOT NULL COMMENT '操作人姓名',
  `action` VARCHAR(32) NOT NULL COMMENT '操作动作(SUBMIT/APPROVE/REJECT/TRANSFER/ADD_SIGN/WITHDRAW/RETURN/SUSPEND/RESUME)',
  `from_node_id` BIGINT DEFAULT NULL COMMENT '源节点ID',
  `to_node_id` BIGINT DEFAULT NULL COMMENT '目标节点ID',
  `opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `field_snapshot_before` JSON DEFAULT NULL COMMENT '字段修改前快照',
  `field_snapshot_after` JSON DEFAULT NULL COMMENT '字段修改后快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_record_instance` (`instance_id`),
  KEY `idx_record_operator` (`operator_id`),
  KEY `idx_record_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程审计流转记录表';

-- 8. 审批委托表
CREATE TABLE `wf_delegation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '委托ID',
  `delegator_id` BIGINT NOT NULL COMMENT '委托授权人ID',
  `delegate_id` BIGINT NOT NULL COMMENT '代理人ID',
  `process_category` VARCHAR(64) NOT NULL DEFAULT '*' COMMENT '委托流程分类(*代表全局, 亦可细化为具体分类名称)',
  `start_date` DATE NOT NULL COMMENT '生效开始日期',
  `end_date` DATE NOT NULL COMMENT '失效结束日期',
  `notify_delegator` TINYINT NOT NULL DEFAULT 1 COMMENT '代理审批时是否同时通知授权人(0-不通知, 1-通知)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '规则启用状态(ACTIVE/CANCELLED)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_deleg_delegator` (`delegator_id`, `status`),
  KEY `idx_deleg_delegate` (`delegate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批委托表';
```

### 4.1 表关联与外键约束机制

* **级联物理删除 (ON DELETE CASCADE)**：在进行重置或者流程配置被管理员物理清理时，`wf_node`、`wf_transition`、`wf_assignee_rule` 均绑定了针对 `wf_definition` 的级联删除约束，保证数据库冗余配置自动清理。
* **隔离性解耦**：流程流转过程中的实例(`wf_instance`)和任务(`wf_task`)不直接在数据库物理层面与 `wf_definition` 形成强关联，原因是在开启“快照机制”后，引擎即便移除了模板定义，也要求运行中的流程可以顺利归档，所以实例直接使用 JSON 序列化存储定义快照。

---

## 5. API 契约设计

### 5.1 契约端点设计表 (基准路径 `/api/wf`)

| 方法 | 契约路径 | 功能描述 | 权限控制 |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/wf/definitions` | 新建流程定义模板 | `workflow:definition:create` |
| **PUT** | `/api/wf/definitions/{id}` | 更新流程定义模板 (只能修改草稿) | `workflow:definition:update` |
| **POST** | `/api/wf/definitions/{id}/actions/publish` | 发布流程定义 (升级版本并锁定模板) | `workflow:definition:publish` |
| **POST** | `/api/wf/process/actions/start` | 启动流程实例 | 统一登录拦截 (`UserContext`) |
| **POST** | `/api/wf/process/instances/{id}/actions/withdraw` | 申请人自行撤回实例 (必须处于 RUNNING 状态) | 校验申请人本人所有权 |
| **POST** | `/api/wf/process/instances/{id}/actions/suspend` | 暂停流程实例 (挂起流程) | `workflow:process:suspend` |
| **POST** | `/api/wf/process/instances/{id}/actions/resume` | 恢复流程实例 (解除挂起) | `workflow:process:resume` |
| **POST** | `/api/wf/tasks/{id}/actions/approve` | 同意并提交当前节点任务 | 校验任务审批人归属 |
| **POST** | `/api/wf/tasks/{id}/actions/reject` | 驳回并指定重填规则 | 校验任务审批人归属 |
| **POST** | `/api/wf/tasks/{id}/actions/transfer` | 转办任务至其他审批人 | 校验任务审批人归属 |
| **POST** | `/api/wf/tasks/{id}/actions/add-sign` | 前/后加签新审批人 | 校验任务审批人归属 |
| **POST** | `/api/wf/tasks/{id}/actions/urge` | 催办未审批节点 (限流每2小时最多1次) | 校验申请人所有权 |

### 5.2 核心 API DTO/VO 定义

#### 5.2.1 流程启动参数 DTO (`WfProcessStartDTO`)
```java
@Data
public class WfProcessStartDTO {
    @NotBlank(message = "流程编码不能为空")
    private String processCode;
    
    @NotBlank(message = "业务类型不能为空")
    private String businessType;
    
    @NotNull(message = "业务主键ID不能为空")
    private Long businessId;
    
    @NotBlank(message = "流程标题不能为空")
    private String title;
    
    private Map<String, Object> variables = new HashMap<>(); // 供条件表达式分支跳转计算使用的表单参数
}
```

#### 5.2.2 任务审批 DTO (`WfTaskApproveDTO`)
```java
@Data
public class WfTaskApproveDTO {
    @NotBlank(message = "审批意见不能为空")
    @Size(max = 500, message = "审批意见最长不能超过500个字符")
    private String opinion;
    
    private String signature; // 电子签名Base64或上传URL
    
    private Map<String, Object> variables = new HashMap<>(); // 修改审批判定变量
}
```

#### 5.2.3 加签动作 DTO (`WfTaskAddSignDTO`)
```java
@Data
public class WfTaskAddSignDTO {
    @NotNull(message = "加签人ID不能为空")
    private Long addSignAssigneeId;
    
    @NotBlank(message = "加签类型参数错误")
    @Pattern(regexp = "^(PRE_ADD_SIGN|POST_ADD_SIGN)$", message = "加签动作必须是前加签或后加签")
    private String signType; // PRE_ADD_SIGN: 前加签, POST_ADD_SIGN: 后加签
    
    @NotBlank(message = "加签意见描述不能为空")
    private String reason;
}
```

#### 5.2.4 任务对象前端展示 VO (`WfTaskVO`)
```java
@Data
public class WfTaskVO {
    private Long taskId;
    private Long instanceId;
    private String processName;
    private String title;
    private String applicantName;
    private String nodeName;
    private String status; // PENDING / APPROVED / REJECTED
    private LocalDateTime createTime;
    private LocalDateTime dueTime;
}
```

---

## 6. 任务波次拆分 (Waves 1-5)

### Wave 1: 契约与基线

#### T1 数据库 Schema 生成与验证
* **目标**：在本地数据库完成 8 张核心 wf 表的完整创建，配置正确的联合索引和外键关系。
* **路径**：`code/backend/sql/oa_wf_schema.sql` (创建此全新 SQL 定义文件)
* **输入**：本实施设计文档的 DDL 脚本
* **输出**：在本地 MySQL 库执行并通过，EXPLAIN 执行计划显示主键及 `uk_*` 索引命中完美。
* **验收命令**：
  ```bash
  mysql -u root -p oa_system < code/backend/sql/oa_wf_schema.sql
  ```

#### T2 旧 workflow 系统分析与对接方案
* **目标**：评估旧版 `OaLeaveApply` 与现存的审批方法，确定无缝的下线步骤。
* **路径**：`code/backend/oa-workflow/` 模块结构梳理
* **输出**：形成模块级接口调整，停止直接改写 `sys_employee` 状态，统一通过新 `wf_record`。
* **验收标准**：梳理完毕后，对旧有的工作流相关类添加 `@Deprecated` 注解。

---

### Wave 2: 引擎核心数据与基础结构

#### T3 工作流核心数据模型与 MyBatis-Plus 代码层
* **目标**：在项目全新 `oa-workflow` 模块的 `oa-workflow-mapper` 与 `oa-workflow-model` 路径中，构建对应的 8 张表 Entity、Mapper 接口及 XML 文件。
* **路径**：
  * `code/backend/oa-workflow/oa-workflow-model/src/main/java/cn/oa/workflow/model/entity/`
  * `code/backend/oa-workflow/oa-workflow-mapper/src/main/java/cn/oa/workflow/mapper/`
* **输出**：8 张表的 Entity 映射与 XML 查询结构。
* **禁止修改**：禁止在这个阶段中改写任何业务领域的 Service 层。
* **验收命令**：
  ```bash
  cd code/backend && mvn clean compile -pl oa-workflow/oa-workflow-mapper -am
  ```

---

### Wave 3: 状态机调度与策略解析

#### T4 核心调度驱动器 (StateMachine) 实现
* **目标**：完成 `IWorkflowEngine` 的核心 Service 实现，编写启动流程、节点前进、分支条件表达式计算、回滚上一级节点、挂起/恢复流程的核心算法。
* **路径**：`code/backend/oa-workflow/oa-workflow-core/src/main/java/cn/oa/workflow/core/engine/`
* **输出**：`WorkflowEngineImpl.java` 以及对 MVEL / SpEL 条件解析工具类封装。
* **禁止修改**：禁止加入具体的 REST API Controller 接口及前端接入。
* **验收命令**：编写专用的 `WorkflowEngineTest` 覆盖各种有向图跳转判定：
  ```bash
  cd code/backend && mvn test -pl oa-workflow/oa-workflow-core
  ```

#### T5 审批人规则策略解析链 (AssigneeResolverChain)
* **目标**：实现 `AssigneeResolver` 接口，编写 `FixedUserResolver`、`PostResolver`、`DeptLeaderResolver`，并在总调中注入委托（Delegation）处理。
* **路径**：`code/backend/oa-workflow/oa-workflow-core/src/main/java/cn/oa/workflow/core/resolver/`
* **输出**：完整解析链与防环检测实现。
* **验收命令**：运行相关单元测试：
  ```bash
  cd code/backend && mvn test -pl oa-workflow/oa-workflow-core -Dtest=AssigneeResolverChainTest
  ```

---

### Wave 4: 回调分发与消息联动

#### T6 领域事件发布与回调分发器 (WorkflowCallbackDispatcher)
* **目标**：建立工作流终结状态（同意、拒绝、撤销）后的回调解耦分发机制。提供注册 `WorkflowCallbackHandler` 接口，使业务模块可以异步或者声明式接收最终流转结果。
* **路径**：`code/backend/oa-workflow/oa-workflow-api/src/main/java/cn/oa/workflow/api/callback/`
* **输出**：`WorkflowCallbackDispatcher.java` 和对应的 Spring Event 配置。
* **验收标准**：通过本地 MockHandler 完成模拟调用，验证当工作流通过时能够顺利触发 MockHandler 回调。

#### T7 消息通知与待办事项 (TaskMessageListener) 联动
* **目标**：当任务产生（`PENDING`）或被催办（`URGE`）时，通过 Spring 观察者事件触发通知到 `oa-message` 模块，通过 WebSocket 或系统待办列表进行展示。
* **路径**：`code/backend/oa-workflow/oa-workflow-core/src/main/java/cn/oa/workflow/core/event/`
* **输出**：`TaskMessageListener.java`，自动向目标审批人的 Redis/WebSocket 推送一条通知。
* **验收命令**：
  ```bash
  cd code/backend && mvn test -pl oa-workflow/oa-workflow-core -Dtest=TaskMessageListenerTest
  ```

---

### Wave 5: REST API、管理视图与集成测试

#### T8 工作流 REST API 实现
* **目标**：实现 `/api/wf` 底下的所有端点（CRUD、启动、撤回、同意、驳回、转办、加签等全契约能力），接入 Spring JWT 安全验证。
* **路径**：`code/backend/oa-workflow/oa-workflow-api/src/main/java/cn/oa/workflow/api/controller/`
* **输出**：`WfDefinitionController.java`、`WfTaskController.java`、`WfInstanceController.java`。
* **验收命令**：
  ```bash
  cd code/backend && mvn clean test -pl oa-workflow/oa-workflow-api,oa-web -am
  ```

#### T9 端到端集成测试与 E2E 验证 (E2E Integration Test)
* **目标**：编写覆盖端到端场景的契约用例：发布新流程图 -> 提交申请 -> 触发策略链自动生成两级任务 -> 审批同意 -> 触发回调写入业务数据。
* **路径**：`code/backend/oa-workflow/oa-workflow-api/src/test/java/cn/oa/workflow/api/e2e/`
* **输出**：`WorkflowFullLifecycleE2ETest.java`
* **验收命令**：
  ```bash
  cd code/backend && mvn test -pl oa-workflow/oa-workflow-api -Dtest=WorkflowFullLifecycleE2ETest
  ```

---

## 7. 针对 Claude Code 的分步骤可执行提示词

### 7.1 Wave 2 - T3：模型与数据层开发提示词
```text
请开始执行 oa-workflow 模块 Wave 2：T3模型与数据层构建。

阅读准备文件：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-wf-engine-kernel-task-split.md (重点参考第4节DDL)

开发任务：
1. 在 oa-workflow-model 模块下，根据 DDL 的 8 个核心表，创建标准的领域实体类：
   WfDefinition, WfNode, WfTransition, WfAssigneeRule, WfInstance, WfTask, WfRecord, WfDelegation.
2. 实体层必须集成 Lombok 注解、使用 MyBatis-Plus 的 @TableId(type = IdType.AUTO) 以及相应的 @TableName 映射。
3. 创建对应的枚举状态包，如 TaskStatus(PENDING, APPROVED, REJECTED, TRANSFERRED, SUSPENDED, CANCELED), InstanceStatus(RUNNING, PASSED, REJECTED, REVOKED, SUSPENDED, ABORTED)
4. 在 oa-workflow-mapper 模块下建立继承 BaseMapper 的持久层接口，及对应 mapper/xml 查询结构。
5. 保证代码的绝对整洁度，字段语义必须与 DDL 一致。

编写完成后运行：
cd code/backend && mvn clean compile -pl oa-workflow/oa-workflow-mapper -am

向我汇报编译成功、创建的实体包及Mapper清单。
```

### 7.2 Wave 3 - T4：StateMachine 调度引擎核心提示词
```text
请开始执行 oa-workflow 模块 Wave 3：T4 核心状态机调度引擎 (StateMachine) 开发。

阅读准备文件：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-wf-engine-kernel-task-split.md (重点参考第3节算法)

开发任务：
1. 声明工作流微内核驱动接口：IWorkflowEngine.java。包含：
   - startWorkflow(WfProcessStartDTO dto)
   - handleTask(Long taskId, Long operatorId, WfTaskApproveDTO approveDto)
   - rejectTask(Long taskId, Long operatorId, WfTaskRejectDTO rejectDto)
   - revokeInstance(Long instanceId, Long operatorId)
   - suspendInstance(Long instanceId, Long operatorId)
   - resumeInstance(Long instanceId, Long operatorId)
2. 编写核心类 WorkflowEngineImpl.java 实现该接口，负责以下状态机跳转逻辑：
   - 启动流程：写入 wf_instance (生成 snapshot 保存当前节点信息)，计算 start 节点的下一个节点并进入。
   - 判定节点前进：每一次 handleTask 提交，执行 evaluateSign 算法汇总该节点任务，满足跳转时，解析下一个节点。
   - 使用有向图结构判定下一节点的 Transition 表达式 (支持简单解析或 Spring SpEL 计算变量)。
3. 在过程中若遭遇异常（如找不到下一个可流转分支），立刻回滚事务。
4. 编写针对各种节点会签、或签有向图流转逻辑的单元测试。

验证命令：
cd code/backend && mvn test -pl oa-workflow/oa-workflow-core

最终汇报状态机的测试用例执行结果，及状态转换的实现说明。
```

### 7.3 Wave 3 - T5：AssigneeResolver 审批人策略链提示词
```text
请开始执行 oa-workflow 模块 Wave 3：T5 审批人规则策略解析链 (AssigneeResolverChain) 开发。

开发任务：
1. 创建接口 AssigneeResolver.java：
   Set<Long> resolve(WfNode node, WfAssigneeRule rule, Map<String, Object> variables);
2. 编写以下解析策略实现：
   - FixedUserResolver：解析 rule_value 里面逗号分割的固定员工ID。
   - PostResolver：通过 rule_value 查出数据库中归属于该岗位编码的所有员工ID。
   - DeptLeaderResolver：提取 variables / UserContext 中申请人的部门信息，查询出对应的主管。
3. 编写 AssigneeResolverChain 逻辑，对策略解析结果进行合并，并追加：
   - 空值兜底拦截（根据 empty_assignee_strategy 分别执行跳过、驳回、分配超级管理员）。
   - 委托流转过滤：遍历解析后的集合，若命中 wf_delegation 规则，使用递归逻辑重定向给 DelegateId。
   - 追加防环形委托保护检测 (DelegationResolver 深度优先检测 visited map)。
4. 编写全面的测试用例，覆盖正常规则匹配、空规则兜底、以及故意构建 A->B->A 深度委托来验证环形熔断。

验证命令：
cd code/backend && mvn test -pl oa-workflow/oa-workflow-core -Dtest=*Resolver*

向我汇报解析策略实现逻辑及相关测试结果。
```

### 7.4 Wave 5 - T8：API 控制层集成提示词
```text
请开始执行 oa-workflow 模块 Wave 5：T8 流程内核 REST API 的整体开发。

开发任务：
1. 在 oa-workflow-api 模块下创建三个 REST 控制器：
   - WfDefinitionController：定义、修改草稿、发布定义（发布时不允许继续修改，仅可通过新建版本，确保运行快照稳定）。
   - WfInstanceController：获取我的申请列表、详情、撤销流程、挂起与恢复。
   - WfTaskController：获取待办、同意、驳回、转办、加签（支持 PRE_ADD_SIGN 前加签将原任务状态设为 SUSPENDED，优先审批新生成任务）。
2. 参数层必须配合 @Valid 注解，异常层通过项目的全局异常处理器进行捕获并转换为标准 R 响应。
3. 控制器中需要通过 AOP 拦截权限注解，如 @RequirePermission，保障管理员权限控制。
4. 编写 mock 控制器测试，验证在不登录状态下拦截未授权请求，在标准上下文登录时顺利通过并路由到服务层。

验证命令：
cd code/backend && mvn clean test -pl oa-workflow/oa-workflow-api,oa-web -am

汇报新增的所有 API 控制器路径及异常控制和测试表现。
```
