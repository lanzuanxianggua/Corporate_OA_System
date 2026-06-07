package cn.oa.meeting.constant;

/**
 * 会议模块常量 (统一状态 / 业务前缀 / 工作流 defKey).
 *
 * <p>oa-meeting 唯一 MtConstants, 与 oa-finance / oa-document 风格一致.
 */
public final class MtConstants {

    private MtConstants() {}

    // ==================== 预约状态 ====================
    public static final String BOOKING_STATUS_PENDING = "PENDING";
    public static final String BOOKING_STATUS_APPROVED = "APPROVED";
    public static final String BOOKING_STATUS_REJECTED = "REJECTED";
    public static final String BOOKING_STATUS_CANCELLED = "CANCELLED";
    public static final String BOOKING_STATUS_COMPLETED = "COMPLETED";

    // ==================== 会议室状态 ====================
    public static final String ROOM_STATUS_ACTIVE = "ACTIVE";
    public static final String ROOM_STATUS_INACTIVE = "INACTIVE";
    public static final String ROOM_STATUS_MAINTENANCE = "MAINTENANCE";

    // ==================== 会议状态 (DB meeting_status 列) ====================
    public static final String MEETING_STATUS_SCHEDULED = "SCHEDULED";
    public static final String MEETING_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String MEETING_STATUS_COMPLETED = "COMPLETED";
    public static final String MEETING_STATUS_CANCELLED = "CANCELLED";

    // ==================== 决议状态 ====================
    public static final String RESOLUTION_STATUS_PENDING = "PENDING";
    public static final String RESOLUTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String RESOLUTION_STATUS_COMPLETED = "COMPLETED";
    public static final String RESOLUTION_STATUS_OVERDUE = "OVERDUE";

    // ==================== 决议优先级 ====================
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_LOW = "LOW";

    // ==================== 工作流 defKey ====================
    public static final String WF_DEF_BOOKING = "meeting_booking";

    // ==================== 业务 Key 前缀 ====================
    public static final String BIZ_KEY_PREFIX_BOOKING = "BOOKING_";
}
