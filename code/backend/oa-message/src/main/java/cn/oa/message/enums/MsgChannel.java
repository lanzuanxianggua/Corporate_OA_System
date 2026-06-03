package cn.oa.message.enums;

/**
 * 消息渠道枚举
 */
public enum MsgChannel {

    SITE("站点消息"),
    EMAIL("邮件"),
    SMS("短信"),
    WECHAT("微信");

    private final String displayName;

    MsgChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
