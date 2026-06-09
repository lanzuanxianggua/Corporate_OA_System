package cn.oa.common.constant;

/**
 * BusinessType constants used as {@code businessType} keys throughout the
 * workflow engine. Each value must have a corresponding
 * {@code XXXService.updateStatus} implementation in the dispatcher
 * (cn.oa.service.impl.WorkflowCallbackDispatcher).
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
