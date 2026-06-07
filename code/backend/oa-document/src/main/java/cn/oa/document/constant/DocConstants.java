package cn.oa.document.constant;

/**
 * 公文常量 (统一状态 / 业务前缀 / 工作流 defKey).
 *
 * <p>oa-document 唯一 DocConstants, 替代早期的 enums.DocConstants 副本.
 */
public final class DocConstants {

    private DocConstants() {}

    // ==================== 发文状态 ====================
    public static final String DISPATCH_STATUS_DRAFT = "DRAFT";
    public static final String DISPATCH_STATUS_PENDING = "PENDING";
    public static final String DISPATCH_STATUS_APPROVED = "APPROVED";
    public static final String DISPATCH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String DISPATCH_STATUS_ARCHIVED = "ARCHIVED";

    // ==================== 收文状态 ====================
    public static final String RECEIVE_STATUS_PENDING = "PENDING";
    public static final String RECEIVE_STATUS_PROCESSING = "PROCESSING";
    public static final String RECEIVE_STATUS_COMPLETED = "COMPLETED";
    public static final String RECEIVE_STATUS_ARCHIVED = "ARCHIVED";

    // ==================== 签报状态 ====================
    public static final String SIGN_REPORT_STATUS_DRAFT = "DRAFT";
    public static final String SIGN_REPORT_STATUS_PENDING = "PENDING";
    public static final String SIGN_REPORT_STATUS_APPROVED = "APPROVED";
    public static final String SIGN_REPORT_STATUS_REJECTED = "REJECTED";
    public static final String SIGN_REPORT_STATUS_ARCHIVED = "ARCHIVED";

    // ==================== 签报审批记录状态 ====================
    public static final String ITEM_STATUS_PENDING = "PENDING";
    public static final String ITEM_STATUS_APPROVED = "APPROVED";
    public static final String ITEM_STATUS_REJECTED = "REJECTED";

    // ==================== 档案状态 ====================
    public static final String ARCHIVE_STATUS_ACTIVE = "ACTIVE";
    public static final String ARCHIVE_STATUS_FROZEN = "FROZEN";
    public static final String ARCHIVE_STATUS_DESTROYED = "DESTROYED";

    // ==================== 紧急程度 ====================
    public static final String URGENCY_NORMAL = "NORMAL";
    public static final String URGENCY_URGENT = "URGENT";
    public static final String URGENCY_VERY_URGENT = "VERY_URGENT";

    // ==================== 密级 ====================
    public static final String SECURITY_PUBLIC = "PUBLIC";
    public static final String SECURITY_CONFIDENTIAL = "CONFIDENTIAL";
    public static final String SECURITY_SECRET = "SECRET";
    public static final String SECURITY_TOP_SECRET = "TOP_SECRET";

    // ==================== 签报类型 ====================
    public static final String REPORT_TYPE_GENERAL = "GENERAL";
    public static final String REPORT_TYPE_URGENT = "URGENT";
    public static final String REPORT_TYPE_SPECIAL = "SPECIAL";

    // ==================== 档案类型 ====================
    public static final String ARCHIVE_TYPE_DISPATCH = "DISPATCH";
    public static final String ARCHIVE_TYPE_RECEIVE = "RECEIVE";
    public static final String ARCHIVE_TYPE_SIGN_REPORT = "SIGN_REPORT";

    // ==================== 工作流 defKey ====================
    public static final String WF_DEF_DISPATCH = "document_dispatch";
    public static final String WF_DEF_SIGN_REPORT = "document_sign_report";

    // ==================== 业务 Key 前缀 (oa-workflow businessKey) ====================
    public static final String BIZ_KEY_PREFIX_DISPATCH = "DISPATCH_";
    public static final String BIZ_KEY_PREFIX_SIGN_REPORT = "SIGN_REPORT_";

    // ==================== 通知 type code (oa-message) ====================
    public static final String NOTIFY_DISPATCH_APPROVE = "DISPATCH_APPROVE";
    public static final String NOTIFY_DISPATCH_REJECT = "DISPATCH_REJECT";
    public static final String NOTIFY_SIGN_REPORT_APPROVE = "SIGN_REPORT_APPROVE";
    public static final String NOTIFY_SIGN_REPORT_REJECT = "SIGN_REPORT_REJECT";
}
