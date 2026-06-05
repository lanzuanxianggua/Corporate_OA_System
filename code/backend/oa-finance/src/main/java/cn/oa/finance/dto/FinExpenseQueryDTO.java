package cn.oa.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报销单查询 DTO.
 */
@Data
@Schema(description = "报销单查询参数")
public class FinExpenseQueryDTO {

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/PAID")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
