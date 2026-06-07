package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产领用查询 DTO.
 */
@Data
@Schema(description = "资产领用查询参数")
public class AdmAssetLoanQueryDTO {

    @Schema(description = "资产 ID")
    private Long assetId;

    @Schema(description = "领用类型: BORROW/RETURN/SCRAP")
    private String loanType;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/RETURNED")
    private String status;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
