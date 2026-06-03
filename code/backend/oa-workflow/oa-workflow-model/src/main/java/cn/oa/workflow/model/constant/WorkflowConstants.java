package cn.oa.workflow.model.constant;

/**
 * 工作流常量
 */
public final class WorkflowConstants {

    private WorkflowConstants() {}

    // ===== 流程定义状态 =====
    public static final String DEF_STATUS_DRAFT = "DRAFT";
    public static final String DEF_STATUS_PUBLISHED = "PUBLISHED";
    public static final String DEF_STATUS_DISABLED = "DISABLED";

    // ===== 流程实例状态 =====
    public static final String INSTANCE_STATUS_DRAFT = "DRAFT";
    public static final String INSTANCE_STATUS_RUNNING = "RUNNING";
    public static final String INSTANCE_STATUS_SUSPENDED = "SUSPENDED";
    public static final String INSTANCE_STATUS_ABORTED = "ABORTED";
    public static final String INSTANCE_STATUS_PASSED = "PASSED";
    public static final String INSTANCE_STATUS_REJECTED = "REJECTED";
    public static final String INSTANCE_STATUS_REVOKED = "REVOKED";

    // ===== 任务状态 =====
    public static final String TASK_STATUS_PENDING = "PENDING";
    public static final String TASK_STATUS_APPROVED = "APPROVED";
    public static final String TASK_STATUS_REJECTED = "REJECTED";
    public static final String TASK_STATUS_TRANSFERRED = "TRANSFERRED";
    public static final String TASK_STATUS_CANCELED = "CANCELED";

    // ===== 任务类型 =====
    public static final String TASK_TYPE_TODO = "TODO";
    public static final String TASK_TYPE_COUNTERSIGN = "COUNTERSIGN";
    public static final String TASK_TYPE_ADD_SIGN_FRONT = "ADD_SIGN_FRONT";
    public static final String TASK_TYPE_ADD_SIGN_BEHIND = "ADD_SIGN_BEHIND";

    // ===== 节点类型 =====
    public static final String NODE_TYPE_START = "START";
    public static final String NODE_TYPE_END = "END";
    public static final String NODE_TYPE_APPROVAL = "APPROVAL";
    public static final String NODE_TYPE_CONDITION = "CONDITION";
    public static final String NODE_TYPE_PARALLEL = "PARALLEL";
    public static final String NODE_TYPE_CC = "CC";
    public static final String NODE_TYPE_SUBPROCESS = "SUBPROCESS";

    // ===== 审批模式 =====
    public static final String APPROVAL_MODE_SEQUENTIAL = "SEQUENTIAL";
    public static final String APPROVAL_MODE_COUNTERSIGN = "COUNTERSIGN";
    public static final String APPROVAL_MODE_ORSIGN = "ORSIGN";
    public static final String APPROVAL_MODE_PROPORTIONAL = "PROPORTIONAL";
    public static final String APPROVAL_MODE_VOTE = "VOTE";

    // ===== 审批人规则类型 =====
    public static final String RULE_TYPE_FIXED_USER = "FIXED_USER";
    public static final String RULE_TYPE_POST = "POST";
    public static final String RULE_TYPE_DEPT_LEADER = "DEPT_LEADER";
    public static final String RULE_TYPE_REPORT_LINE = "REPORT_LINE";
    public static final String RULE_TYPE_FORM_SELECT = "FORM_SELECT";
    public static final String RULE_TYPE_API = "API";

    // ===== 操作类型 =====
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_TRANSFER = "TRANSFER";
    public static final String ACTION_ADD_SIGN = "ADD_SIGN";
    public static final String ACTION_WITHDRAW = "WITHDRAW";
    public static final String ACTION_URGE = "URGE";
    public static final String ACTION_SUSPEND = "SUSPEND";
    public static final String ACTION_RESUME = "RESUME";
    public static final String ACTION_ABORT = "ABORT";

    // ===== 驳回策略 =====
    public static final String RETURN_DIRECT = "DIRECT_RETURN";
    public static final String RETURN_SEQUENTIAL = "SEQUENTIAL_RETURN";

    // ===== 超时动作 =====
    public static final String TIMEOUT_AUTO_APPROVE = "AUTO_APPROVE";
    public static final String TIMEOUT_AUTO_REJECT = "AUTO_REJECT";
    public static final String TIMEOUT_ESCALATE = "ESCALATE";
    public static final String TIMEOUT_NOTIFY = "NOTIFY";

    // ===== 审批人为空策略 =====
    public static final String EMPTY_AUTO_SKIP = "AUTO_SKIP";
    public static final String EMPTY_ADMIN_ASSIGN = "ADMIN_ASSIGN";
    public static final String EMPTY_ERROR = "ERROR";
}