package cn.oa.message.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮件日志.
 *
 * <p>对应表 msg_email_logs (V974 建).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("msg_email_logs")
@Schema(description = "邮件日志")
public class MsgEmailLog extends BaseEntity {

    @Schema(description = "关联通知 id")
    @TableField("notification_id")
    private Long notificationId;

    @Schema(description = "收件邮箱")
    @TableField("recipient_email")
    private String recipientEmail;

    @Schema(description = "邮件主题")
    @TableField("subject")
    private String subject;

    @Schema(description = "邮件内容")
    @TableField("content")
    private String content;

    @Schema(description = "发送状态: PENDING/SENT/FAILED")
    @TableField("send_status")
    private String sendStatus;

    @Schema(description = "失败原因")
    @TableField("fail_reason")
    private String failReason;

    @Schema(description = "发送时间")
    @TableField("sent_time")
    private LocalDateTime sentTime;
}
