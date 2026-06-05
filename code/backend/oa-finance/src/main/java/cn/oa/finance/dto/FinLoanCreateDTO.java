package cn.oa.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 借款单创建 DTO.
 */
@Data
@Schema(description = "借款单创建请求")
public class FinLoanCreateDTO {

    @NotBlank(message = "借款类型不能为空")
    @Schema(description = "借款类型: TRAVEL/BUSINESS/OTHER", example = "TRAVEL")
    private String loanType;

    @NotNull(message = "借款金额不能为空")
    @Schema(description = "借款金额", example = "5000.00")
    private BigDecimal amount;

    @NotBlank(message = "借款用途不能为空")
    @Schema(description = "借款用途", example = "出差备用金")
    private String purpose;

    @Schema(description = "还款期限", example = "2026-07-01")
    private LocalDate deadlineDate;
}
