package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("oa_attendance_group")
public class OaAttendanceGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String groupName;

    @JsonAlias("clockInTime")
    private LocalTime workStart;

    @JsonAlias("clockOutTime")
    private LocalTime workEnd;

    private Integer lateThreshold = 15;

    private Character status = '0';

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
