package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_meeting")
public class OaMeeting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;

    private Long roomId;

    private Long organizerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String description;

    /** JSON array of emp IDs */
    private String participants;

    /** 0-scheduled 1-in-progress 2-completed 3-canceled */
    private String status = "0";

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

    /** 会议室名称（非数据库字段） */
    @TableField(exist = false)
    private String roomName;

    /** 组织者姓名（非数据库字段） */
    @TableField(exist = false)
    private String organizerName;
}
