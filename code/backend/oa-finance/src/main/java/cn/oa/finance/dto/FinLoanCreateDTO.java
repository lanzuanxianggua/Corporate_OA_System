package cn.oa.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 借款创建DTO
 *
 * @author oa-finance
 */
@Data
public class FinLoanCreateDTO {

    @NotNull(message = "借款金额不能为空")
    private BigDecimal loanAmount;

    @NotBlank(message = "借款原因不能为空")
    @Size(max = 500, message = "借款原因最长500字")
    private String loanReason;

    @Size(max = 500, message = "还款计划最长500字")
    private String repaymentPlan;
}
