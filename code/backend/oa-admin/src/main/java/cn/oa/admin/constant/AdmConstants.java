package cn.oa.admin.constant;

/**
 * 行政模块常量 (统一状态 / 业务前缀 / 工作流 defKey).
 *
 * <p>oa-admin 唯一 AdmConstants, 与 oa-finance / oa-document 保持命名风格一致.
 */
public final class AdmConstants {

    private AdmConstants() {}

    // ==================== 资产状态 ====================
    public static final String ASSET_STATUS_IDLE = "IDLE";
    public static final String ASSET_STATUS_IN_USE = "IN_USE";
    public static final String ASSET_STATUS_REPAIR = "REPAIR";
    public static final String ASSET_STATUS_SCRAPPED = "SCRAPPED";

    // ==================== 资产分类 ====================
    public static final String ASSET_CATEGORY_IT = "IT";
    public static final String ASSET_CATEGORY_OFFICE = "OFFICE";
    public static final String ASSET_CATEGORY_VEHICLE = "VEHICLE";
    public static final String ASSET_CATEGORY_OTHER = "OTHER";

    // ==================== 印章状态 ====================
    public static final String SEAL_STATUS_ACTIVE = "ACTIVE";
    public static final String SEAL_STATUS_INACTIVE = "INACTIVE";
    public static final String SEAL_STATUS_LOST = "LOST";

    // ==================== 印章类型 ====================
    public static final String SEAL_TYPE_OFFICIAL = "OFFICIAL";
    public static final String SEAL_TYPE_CONTRACT = "CONTRACT";
    public static final String SEAL_TYPE_FINANCE = "FINANCE";
    public static final String SEAL_TYPE_PERSONAL = "PERSONAL";

    // ==================== 印章申请状态 ====================
    public static final String SEAL_APPLY_STATUS_DRAFT = "DRAFT";
    public static final String SEAL_APPLY_STATUS_PENDING = "PENDING";
    public static final String SEAL_APPLY_STATUS_APPROVED = "APPROVED";
    public static final String SEAL_APPLY_STATUS_REJECTED = "REJECTED";
    public static final String SEAL_APPLY_STATUS_USED = "USED";
    public static final String SEAL_APPLY_STATUS_ARCHIVED = "ARCHIVED";

    // ==================== 资产领用状态 ====================
    public static final String ASSET_LOAN_STATUS_DRAFT = "DRAFT";
    public static final String ASSET_LOAN_STATUS_PENDING = "PENDING";
    public static final String ASSET_LOAN_STATUS_APPROVED = "APPROVED";
    public static final String ASSET_LOAN_STATUS_REJECTED = "REJECTED";
    public static final String ASSET_LOAN_STATUS_RETURNED = "RETURNED";

    // ==================== 工作流 defKey ====================
    public static final String WF_DEF_SEAL_APPLY = "admin_seal_apply";
    public static final String WF_DEF_ASSET_LOAN = "admin_asset_loan";

    // ==================== 业务 Key 前缀 ====================
    public static final String BIZ_KEY_PREFIX_SEAL = "SEAL_";
    public static final String BIZ_KEY_PREFIX_ASSET = "ASSET_";
}
