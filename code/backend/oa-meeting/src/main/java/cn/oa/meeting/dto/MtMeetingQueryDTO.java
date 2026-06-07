package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会议记录查询 DTO.
 */
@Data
@Schema(description = "会议记录查询参数")
public class MtMeetingQueryDTO {

    @Schema(description = "关联预约 ID")
    private Long bookingId;

    @Schema(description = "状态: SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED")
    private String status;

    @Schema(description = "组织者 emp_id")
    private Long organizerId;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
