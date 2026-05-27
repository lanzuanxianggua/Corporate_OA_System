package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("oa_attendance_group")
public class OaAttendanceGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String groupName;

    private LocalTime workStart;

    private LocalTime workEnd;

    private Integer lateThreshold = 15;

    private Character status = '0';

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
