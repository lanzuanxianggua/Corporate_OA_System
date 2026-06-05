package cn.oa.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建 DTO.
 */
@Data
@Schema(description = "预算创建请求")
public class FinBudgetCreateDTO {

    @NotBlank(message = "预算名称不能为空")
    @Schema(description = "预算名称", example = "2026年部门运营预算")
    private String budgetName;

    @NotNull(message = "预算年度不能为空")
    @Schema(description = "预算年度", example = "2026")
    private Integer budgetYear;

    @NotNull(message = "总预算金额不能为空")
    @DecimalMin(value = "0.01", message = "总预算金额必须大于 0")
    @Schema(description = "总预算金额", example = "500000.00")
    private BigDecimal totalAmount;
}
