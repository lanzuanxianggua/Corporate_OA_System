package cn.oa.task.enums;

/**
 * 任务模块状态常量.
 */
public final class TaskConstants {

    private TaskConstants() {}

    // ========== 项目状态 ==========
    /** 进行中 */
    public static final String PROJECT_ACTIVE = "ACTIVE";
    /** 已冻结 */
    public static final String PROJECT_FROZEN = "FROZEN";
    /** 已完成 */
    public static final String PROJECT_COMPLETED = "COMPLETED";
    /** 已归档 */
    public static final String PROJECT_ARCHIVED = "ARCHIVED";

    // ========== 任务状态 ==========
    /** 待办 */
    public static final String ITEM_TODO = "TODO";
    /** 进行中 */
    public static final String ITEM_IN_PROGRESS = "IN_PROGRESS";
    /** 已完成 */
    public static final String ITEM_DONE = "DONE";
    /** 已关闭 */
    public static final String ITEM_CLOSED = "CLOSED";

    // ========== 任务优先级 ==========
    /** 高 */
    public static final String PRIORITY_HIGH = "HIGH";
    /** 普通 */
    public static final String PRIORITY_NORMAL = "NORMAL";
    /** 低 */
    public static final String PRIORITY_LOW = "LOW";
}
