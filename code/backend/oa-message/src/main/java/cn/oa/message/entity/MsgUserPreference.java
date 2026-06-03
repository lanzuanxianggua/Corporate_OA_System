package cn.oa.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息用户偏好设置
 */
@Data
@TableName("msg_user_preference")
public class MsgUserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long empId;

    /** 消息类型 */
    private String msgType;

    /** 接收渠道 JSON数组 ["SITE","EMAIL"] */
    private String channels;

    /** 是否启用 0-禁用 1-启用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
