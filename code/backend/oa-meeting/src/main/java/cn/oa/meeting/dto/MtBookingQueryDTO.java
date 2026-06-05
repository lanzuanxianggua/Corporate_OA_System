package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 会议室预约查询 DTO.
 */
@Data
@Schema(description = "会议室预约查询参数")
public class MtBookingQueryDTO {

    @Schema(description = "会议室 ID")
    private Long roomId;

    @Schema(description = "预约日期")
    private LocalDate bookDate;

    @Schema(description = "状态: PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
