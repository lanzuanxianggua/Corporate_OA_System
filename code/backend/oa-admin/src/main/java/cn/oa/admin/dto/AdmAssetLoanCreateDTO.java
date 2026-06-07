package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 资产领用创建 DTO.
 */
@Data
@Schema(description = "资产领用创建请求")
public class AdmAssetLoanCreateDTO {

    @NotNull(message = "资产不能为空")
    @Schema(description = "资产 ID", example = "1")
    private Long assetId;

    @NotBlank(message = "领用类型不能为空")
    @Schema(description = "领用类型: BORROW/RETURN/SCRAP", example = "BORROW")
    private String loanType;

    @Schema(description = "预计归还日期", example = "2026-07-01")
    private LocalDate expectReturnDate;

    @NotBlank(message = "用途说明不能为空")
    @Schema(description = "用途说明", example = "外出办公使用")
    private String purpose;

    @Schema(description = "备注")
    private String remark;
}
