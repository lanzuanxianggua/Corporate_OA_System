package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会议室查询 DTO.
 */
@Data
@Schema(description = "会议室查询参数")
public class MtRoomQueryDTO {

    @Schema(description = "状态: ACTIVE/INACTIVE/MAINTENANCE")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
