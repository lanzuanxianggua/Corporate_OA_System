package cn.oa.meeting.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 会议室预约.
 *
 * <p>对应表 mt_bookings.
 * 状态: PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mt_bookings")
@Schema(description = "会议室预约")
public class MtBooking extends BaseEntity {

    @Schema(description = "会议室 ID")
    @TableField("room_id")
    private Long roomId;

    @Schema(description = "预约人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "预约日期")
    @TableField("book_date")
    private LocalDate bookDate;

    @Schema(description = "开始时间")
    @TableField("start_time")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    @TableField("end_time")
    private LocalTime endTime;

    @Schema(description = "会议主题")
    @TableField("meeting_title")
    private String meetingTitle;

    @Schema(description = "会议描述")
    @TableField("meeting_desc")
    private String meetingDesc;

    @Schema(description = "参会人 ID 列表(JSON)")
    @TableField("participant_ids")
    private String participantIds;

    @Schema(description = "状态: PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED")
    @TableField("status")
    private String status;
}
