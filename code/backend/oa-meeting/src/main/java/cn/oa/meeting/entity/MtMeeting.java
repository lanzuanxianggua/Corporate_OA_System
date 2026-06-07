package cn.oa.meeting.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议记录.
 *
 * <p>对应表 mt_meetings. 字段已对齐 V972 SQL 实际列名.
 * 状态: SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED (DB: meeting_status 列)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mt_meetings")
@Schema(description = "会议记录")
public class MtMeeting extends BaseEntity {

    @Schema(description = "关联预约 ID")
    @TableField("booking_id")
    private Long bookingId;

    @Schema(description = "会议标题 (DB: meeting_title)")
    @TableField("meeting_title")
    private String meetingTitle;

    @Schema(description = "会议内容/纪要 (DB: summary)")
    @TableField("summary")
    private String summary;

    @Schema(description = "实际开始时间 (DB: actual_start)")
    @TableField("actual_start")
    private LocalDateTime startTime;

    @Schema(description = "实际结束时间 (DB: actual_end)")
    @TableField("actual_end")
    private LocalDateTime endTime;

    @Schema(description = "会议室(冗余)")
    @TableField("location")
    private String location;

    @Schema(description = "组织者 emp_id (DB: create_emp_id)")
    @TableField("create_emp_id")
    private Long organizerId;

    @Schema(description = "状态 (DB: meeting_status) SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED")
    @TableField("meeting_status")
    private String status;
}
