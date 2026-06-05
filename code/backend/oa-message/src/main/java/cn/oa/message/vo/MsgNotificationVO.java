package cn.oa.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "消息通知")
public class MsgNotificationVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long senderId;
    private String senderName;
    private String status;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}