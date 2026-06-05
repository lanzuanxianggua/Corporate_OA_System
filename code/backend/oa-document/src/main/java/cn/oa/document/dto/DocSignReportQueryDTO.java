package cn.oa.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 签报查询 DTO.
 */
@Data
@Schema(description = "签报查询参数")
public class DocSignReportQueryDTO {

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
