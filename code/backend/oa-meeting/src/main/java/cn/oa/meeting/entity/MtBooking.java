package cn.oa.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室预订实体
 *
 * @author oa-meeting
 */
@Data
@TableName("mt_booking")
public class MtBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 会议标题 */
    private String title;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 参与人列表(JSON) */
    private String participants;

    /** 状态(0=待审批 1=已通过 2=已拒绝 3=已取消) */
    private Integer status;

    /** 预订人ID */
    private Long bookEmpId;

    /** 工作流实例ID */
    private Long processInstanceId;

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
