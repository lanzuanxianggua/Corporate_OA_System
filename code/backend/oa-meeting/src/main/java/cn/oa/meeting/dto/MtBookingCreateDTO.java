package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 会议室预约创建 DTO.
 */
@Data
@Schema(description = "会议室预约创建请求")
public class MtBookingCreateDTO {

    @NotNull(message = "会议室不能为空")
    @Schema(description = "会议室 ID", example = "1")
    private Long roomId;

    @NotNull(message = "预约日期不能为空")
    @Schema(description = "预约日期", example = "2026-06-10")
    private LocalDate bookDate;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间", example = "09:00")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间", example = "10:00")
    private LocalTime endTime;

    @Schema(description = "会议主题", example = "项目周会")
    private String meetingTitle;

    @Schema(description = "会议描述", example = "本周项目进度同步")
    private String meetingDesc;

    @Schema(description = "参会人 emp_id 列表")
    private List<Long> participantIds;
}
