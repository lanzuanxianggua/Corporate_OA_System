package cn.oa.meeting;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_booking")
public class MtBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long bookerId;

    private String participants;

    private String status = "0";

    private LocalDateTime checkinDeadline;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}