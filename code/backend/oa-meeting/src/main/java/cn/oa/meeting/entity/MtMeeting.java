package cn.oa.meeting.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议.
 *
 * <p>对应表 mt_meetings.
 * 状态: SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mt_meetings")
@Schema(description = "会议")
public class MtMeeting extends BaseEntity {

    @Schema(description = "关联预约 ID")
    @TableField("booking_id")
    private Long bookingId;

    @Schema(description = "会议主题")
    @TableField("subject")
    private String subject;

    @Schema(description = "会议内容")
    @TableField("content")
    private String content;

    @Schema(description = "预计开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "预计结束时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    @Schema(description = "会议室(冗余)")
    @TableField("location")
    private String location;

    @Schema(description = "组织者 emp_id")
    @TableField("organizer_id")
    private Long organizerId;

    @Schema(description = "状态: SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED")
    @TableField("status")
    private String status;
}
