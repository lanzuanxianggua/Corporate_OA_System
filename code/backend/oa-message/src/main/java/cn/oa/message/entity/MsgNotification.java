package cn.oa.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@TableName("msg_notification")
public class MsgNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人ID */
    private Long empId;

    /**
     * 消息类型
     * TODO_ASSIGN / TODO_URGE / APPROVAL_PASS / APPROVAL_REJECT / NOTICE_PUBLISH / MEETING_REMIND / SYSTEM_ALERT
     */
    private String msgType;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 发送渠道 SITE / EMAIL / SMS / WECHAT */
    private String channel;

    /** 是否已读 0-未读 1-已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 业务ID */
    private Long bizId;

    /** 业务类型 */
    private String bizType;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
