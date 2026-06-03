package cn.oa.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_notification")
public class MsgNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private String title;

    private String content;

    private String type;

    private String channel;

    private String status;

    private String sourceType;

    private Long sourceId;

    private String params;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}