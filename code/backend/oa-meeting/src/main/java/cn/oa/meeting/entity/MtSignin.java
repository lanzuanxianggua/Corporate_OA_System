package cn.oa.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议签到实体
 *
 * @author oa-meeting
 */
@Data
@TableName("mt_signin")
public class MtSignin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预订ID */
    private Long bookingId;

    /** 员工ID */
    private Long empId;

    /** 签到时间 */
    private LocalDateTime signinTime;

    /** 签到类型(0=正常签到 1=补签) */
    private Integer signinType;

    /** 签到位置 */
    private String location;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
