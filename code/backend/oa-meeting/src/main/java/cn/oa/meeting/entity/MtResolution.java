package cn.oa.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议纪要/决议实体
 *
 * @author oa-meeting
 */
@Data
@TableName("mt_resolution")
public class MtResolution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预订ID */
    private Long bookingId;

    /** 决议内容 */
    private String content;

    /** 负责人ID */
    private Long assigneeId;

    /** 截止日期 */
    private LocalDate dueDate;

    /** 状态(0=待办 1=进行中 2=已完成 3=已逾期) */
    private Integer status;

    /** 关联任务ID */
    private Long taskId;

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
