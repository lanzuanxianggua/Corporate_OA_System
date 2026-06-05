package cn.oa.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知接收人.
 *
 * <p>对应表 msg_notification_recipient (V100 已建).
 */
@Data
@TableName("msg_notification_recipient")
@Schema(description = "通知接收人")
public class MsgNotificationRecipient {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "通知 id")
    @TableField("notification_id")
    private Long notificationId;

    @Schema(description = "接收人 id")
    @TableField("recipient_id")
    private Long recipientId;

    @Schema(description = "是否已读: Y/N")
    @TableField("is_read")
    private String isRead;

    @Schema(description = "读取时间")
    @TableField("read_time")
    private LocalDateTime readTime;
}
