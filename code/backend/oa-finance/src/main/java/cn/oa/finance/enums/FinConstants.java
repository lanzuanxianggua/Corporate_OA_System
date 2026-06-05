package cn.oa.finance.enums;

/**
 * 财务模块常量.
 *
 * <p>集中定义状态枚举和类型枚举的字符串值，避免代码中散落 magic string.
 */
public final class FinConstants {

    private FinConstants() {
        // utility class
    }

    // ========== 预算状态 ==========
    public static final String BUDGET_STATUS_ACTIVE = "ACTIVE";
    public static final String BUDGET_STATUS_FROZEN = "FROZEN";
    public static final String BUDGET_STATUS_CLOSED = "CLOSED";

    // ========== 报销状态 ==========
    public static final String EXPENSE_STATUS_DRAFT = "DRAFT";
    public static final String EXPENSE_STATUS_PENDING = "PENDING";
    public static final String EXPENSE_STATUS_APPROVED = "APPROVED";
    public static final String EXPENSE_STATUS_REJECTED = "REJECTED";
    public static final String EXPENSE_STATUS_PAID = "PAID";

    // ========== 报销类型 ==========
    public static final String EXPENSE_TYPE_TRAVEL = "TRAVEL";
    public static final String EXPENSE_TYPE_MEAL = "MEAL";
    public static final String EXPENSE_TYPE_OFFICE = "OFFICE";
    public static final String EXPENSE_TYPE_OTHER = "OTHER";

    // ========== 明细费用类型 ==========
    public static final String FEE_TYPE_TRANSPORT = "TRANSPORT";
    public static final String FEE_TYPE_ACCOMMODATION = "ACCOMMODATION";
    public static final String FEE_TYPE_MEAL = "MEAL";
    public static final String FEE_TYPE_OTHER = "OTHER";

    // ========== 借款状态 ==========
    public static final String LOAN_STATUS_DRAFT = "DRAFT";
    public static final String LOAN_STATUS_PENDING = "PENDING";
    public static final String LOAN_STATUS_APPROVED = "APPROVED";
    public static final String LOAN_STATUS_REJECTED = "REJECTED";
    public static final String LOAN_STATUS_SETTLED = "SETTLED";

    // ========== 借款类型 ==========
    public static final String LOAN_TYPE_TRAVEL = "TRAVEL";
    public static final String LOAN_TYPE_BUSINESS = "BUSINESS";
    public static final String LOAN_TYPE_OTHER = "OTHER";

    // ========== 还款类型 ==========
    public static final String REPAY_TYPE_CASH = "CASH";
    public static final String REPAY_TYPE_DEDUCT = "DEDUCT";
    public static final String REPAY_TYPE_EXPENSE_OFFSET = "EXPENSE_OFFSET";
}
