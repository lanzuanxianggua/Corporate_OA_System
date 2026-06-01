package cn.oa.common.constant;

/**
 * 业务审批状态常量，统一管理所有业务单据的审批状态码与中文标签。
 *
 * <p>提供 getLabel(int, boolean) 方法将状态码转为展示文本。
 * isLeave=true 时 REJECTED 返回 "已拒绝"（请假用），其余返回 "已驳回"。</p>
 */
public class BusinessStatus {

    private BusinessStatus() {
        // utility class
    }

    /** 待审批 */
    public static final int PENDING = 0;
    /** 已通过 */
    public static final int APPROVED = 1;
    /** 已驳回 / 已拒绝（请假） */
    public static final int REJECTED = 2;
    /** 已撤回 */
    public static final int WITHDRAWN = 3;
    /** 已取消 */
    public static final int CANCELED = 4;
    /** 已退回 */
    public static final int RETURNED = 5;

    private static final String[] LABELS = {
            "待审批", "已通过", "已驳回", "已撤回", "已取消", "已退回"
    };

    private static final String[] LABELS_LEAVE = {
            "待审批", "已通过", "已拒绝", "已撤回", "已取消", "已退回"
    };

    /**
     * 根据状态码获取中文标签。
     *
     * @param status  状态码（0-5），越界返回 "未知"
     * @param isLeave 是否请假类型；true 时 REJECTED(2) 返回 "已拒绝"
     * @return 中文状态文本
     */
    public static String getLabel(int status, boolean isLeave) {
        if (status < 0 || status >= LABELS.length) {
            return "未知";
        }
        return (isLeave ? LABELS_LEAVE : LABELS)[status];
    }

    /**
     * {@link #getLabel(int, boolean)} 的 Integer 重载，自动处理 null。
     *
     * @param status  状态码 Integer，为 null 时返回 "未知"
     * @param isLeave 是否请假类型
     * @return 中文状态文本
     */
    public static String getLabel(Integer status, boolean isLeave) {
        if (status == null) {
            return "未知";
        }
        return getLabel((int) status, isLeave);
    }
}
