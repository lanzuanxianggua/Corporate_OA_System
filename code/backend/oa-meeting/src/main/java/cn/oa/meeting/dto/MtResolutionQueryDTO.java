package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会议决议查询 DTO.
 */
@Data
@Schema(description = "会议决议查询参数")
public class MtResolutionQueryDTO {

    @Schema(description = "会议 ID")
    private Long meetingId;

    @Schema(description = "责任人 emp_id")
    private Long assigneeId;

    @Schema(description = "状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
