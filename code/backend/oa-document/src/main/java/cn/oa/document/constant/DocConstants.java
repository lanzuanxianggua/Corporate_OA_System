package cn.oa.document.constant;

/**
 * 公文常量.
 */
public final class DocConstants {

    private DocConstants() {}

    /** 发文状态. */
    public static final String DISPATCH_STATUS_DRAFT = "DRAFT";
    public static final String DISPATCH_STATUS_PENDING = "PENDING";
    public static final String DISPATCH_STATUS_APPROVED = "APPROVED";
    public static final String DISPATCH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String DISPATCH_STATUS_ARCHIVED = "ARCHIVED";

    /** 收文状态. */
    public static final String RECEIVE_STATUS_PENDING = "PENDING";
    public static final String RECEIVE_STATUS_COMPLETED = "COMPLETED";
    public static final String RECEIVE_STATUS_ARCHIVED = "ARCHIVED";

    /** 签报状态. */
    public static final String SIGN_REPORT_STATUS_DRAFT = "DRAFT";
    public static final String SIGN_REPORT_STATUS_PENDING = "PENDING";
    public static final String SIGN_REPORT_STATUS_APPROVED = "APPROVED";
    public static final String SIGN_REPORT_STATUS_REJECTED = "REJECTED";

    /** 工作流 defKey. */
    public static final String WF_DEF_DISPATCH = "document_dispatch";
    public static final String WF_DEF_SIGN_REPORT = "document_sign_report";

    /** 业务 Key 前缀. */
    public static final String BIZ_KEY_PREFIX_DISPATCH = "DISPATCH_";
    public static final String BIZ_KEY_PREFIX_SIGN_REPORT = "SIGN_REPORT_";
}
