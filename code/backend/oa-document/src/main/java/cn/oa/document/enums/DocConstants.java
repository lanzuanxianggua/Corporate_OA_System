package cn.oa.document.enums;

/**
 * 文档模块常量.
 *
 * <p>集中管理状态枚举值, 避免字符串散落各处.
 */
public final class DocConstants {

    private DocConstants() {
        // 工具类禁止实例化
    }

    // ==================== 发文状态 ====================
    /** 草稿 */
    public static final String DISPATCH_DRAFT = "DRAFT";
    /** 待审批 */
    public static final String DISPATCH_PENDING = "PENDING";
    /** 已审批 */
    public static final String DISPATCH_APPROVED = "APPROVED";
    /** 已发布 */
    public static final String DISPATCH_PUBLISHED = "PUBLISHED";
    /** 已归档 */
    public static final String DISPATCH_ARCHIVED = "ARCHIVED";

    // ==================== 收文状态 ====================
    /** 待处理 */
    public static final String RECEIVE_PENDING = "PENDING";
    /** 处理中 */
    public static final String RECEIVE_PROCESSING = "PROCESSING";
    /** 已完成 */
    public static final String RECEIVE_COMPLETED = "COMPLETED";
    /** 已归档 */
    public static final String RECEIVE_ARCHIVED = "ARCHIVED";

    // ==================== 签报状态 ====================
    /** 草稿 */
    public static final String REPORT_DRAFT = "DRAFT";
    /** 待审批 */
    public static final String REPORT_PENDING = "PENDING";
    /** 已通过 */
    public static final String REPORT_APPROVED = "APPROVED";
    /** 已驳回 */
    public static final String REPORT_REJECTED = "REJECTED";
    /** 已归档 */
    public static final String REPORT_ARCHIVED = "ARCHIVED";

    // ==================== 签报审批记录状态 ====================
    /** 待审批 */
    public static final String ITEM_PENDING = "PENDING";
    /** 已通过 */
    public static final String ITEM_APPROVED = "APPROVED";
    /** 已驳回 */
    public static final String ITEM_REJECTED = "REJECTED";

    // ==================== 档案状态 ====================
    /** 正常 */
    public static final String ARCHIVE_ACTIVE = "ACTIVE";
    /** 冻结 */
    public static final String ARCHIVE_FROZEN = "FROZEN";
    /** 销毁 */
    public static final String ARCHIVE_DESTROYED = "DESTROYED";

    // ==================== 紧急程度 ====================
    /** 普通 */
    public static final String URGENCY_NORMAL = "NORMAL";
    /** 紧急 */
    public static final String URGENCY_URGENT = "URGENT";
    /** 特急 */
    public static final String URGENCY_VERY_URGENT = "VERY_URGENT";

    // ==================== 密级 ====================
    /** 公开 */
    public static final String SECURITY_PUBLIC = "PUBLIC";
    /** 普通 */
    public static final String SECURITY_CONFIDENTIAL = "CONFIDENTIAL";
    /** 秘密 */
    public static final String SECURITY_SECRET = "SECRET";
    /** 绝密 */
    public static final String SECURITY_TOP_SECRET = "TOP_SECRET";

    // ==================== 签报类型 ====================
    /** 一般签报 */
    public static final String REPORT_TYPE_GENERAL = "GENERAL";
    /** 紧急签报 */
    public static final String REPORT_TYPE_URGENT = "URGENT";
    /** 特殊签报 */
    public static final String REPORT_TYPE_SPECIAL = "SPECIAL";

    // ==================== 档案类型 ====================
    /** 发文档案 */
    public static final String ARCHIVE_TYPE_DISPATCH = "DISPATCH";
    /** 收文档案 */
    public static final String ARCHIVE_TYPE_RECEIVE = "RECEIVE";
    /** 签报档案 */
    public static final String ARCHIVE_TYPE_SIGN_REPORT = "SIGN_REPORT";
}
