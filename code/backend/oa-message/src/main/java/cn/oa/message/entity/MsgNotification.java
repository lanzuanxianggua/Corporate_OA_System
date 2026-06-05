package cn.oa.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知.
 *
 * <p>对应表 msg_notification (V100 已建).
 * 不继承 BaseEntity, 因 V100 DDL 字段结构与 BaseEntity 不一致.
 */
@Data
@TableName("msg_notification")
@Schema(description = "消息通知")
public class MsgNotification {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "通知标题")
    @TableField("title")
    private String title;

    @Schema(description = "通知内容")
    @TableField("content")
    private String content;

    @Schema(description = "通知类型: SYSTEM/WORKFLOW/ANNOUNCE/TODO")
    @TableField("category")
    private String type;

    @Schema(description = "发送者 id (0=系统)")
    @TableField("sender_id")
    private Long senderId;

    @Schema(description = "接收者 id")
    @TableField("recipient_id")
    private Long recipientId;

    @Schema(description = "状态: UNREAD/READ/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "读取时间")
    @TableField("read_time")
    private LocalDateTime readTime;

    @Schema(description = "删除标记")
    @TableField("del_flag")
    private String delFlag;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;
}
