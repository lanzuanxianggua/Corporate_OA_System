package cn.oa.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知VO
 */
@Data
public class MsgNotificationVO {

    private Long id;
    private Long empId;
    private String msgType;
    private String msgTypeName;
    private String title;
    private String content;
    private String channel;
    private String channelName;
    private Integer isRead;
    private LocalDateTime readTime;
    private Long bizId;
    private String bizType;
    private LocalDateTime createdAt;
}
