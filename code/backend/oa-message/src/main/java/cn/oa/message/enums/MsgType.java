package cn.oa.message.enums;

/**
 * 消息类型枚举
 */
public enum MsgType {

    TODO_ASSIGN("待办分配"),
    TODO_URGE("待办催办"),
    APPROVAL_PASS("审批通过"),
    APPROVAL_REJECT("审批驳回"),
    NOTICE_PUBLISH("通知发布"),
    MEETING_REMIND("会议提醒"),
    SYSTEM_ALERT("系统预警");

    private final String displayName;

    MsgType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
