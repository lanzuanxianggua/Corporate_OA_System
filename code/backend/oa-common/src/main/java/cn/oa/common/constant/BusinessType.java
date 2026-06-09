package cn.oa.common.constant;

/**
 * BusinessType constants used as {@code businessType} keys throughout the
 * workflow engine. Each value must have a corresponding
 * {@code XXXService.updateStatus} implementation in the dispatcher
 * (cn.oa.service.impl.WorkflowCallbackDispatcher).
 *
 * <p>The legacy approval modules still compile and submit workflow instances
 * through these keys. Callback wiring may be narrower than this constant set,
 * but removing constants breaks those service contracts.
 */
public class BusinessType {
    public static final String LEAVE = "leave";
    public static final String TRIP = "trip";
    public static final String OUTING = "outing";
    public static final String EXPENSE = "expense";
    public static final String OVERTIME = "overtime";
    public static final String PURCHASE = "purchase";
    public static final String LOAN = "loan";
}
