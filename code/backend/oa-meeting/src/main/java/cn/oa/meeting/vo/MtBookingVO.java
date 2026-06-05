package cn.oa.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 会议室预约 VO.
 */
@Data
@Schema(description = "会议室预约详情")
public class MtBookingVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会议室 ID")
    private Long roomId;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "预约人 emp_id")
    private Long empId;

    @Schema(description = "预约人姓名")
    private String bookEmpName;

    @Schema(description = "预约日期")
    private LocalDate bookDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "会议主题")
    private String meetingTitle;

    @Schema(description = "会议描述")
    private String meetingDesc;

    @Schema(description = "参会人 ID 列表(JSON)")
    private String participantIds;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
