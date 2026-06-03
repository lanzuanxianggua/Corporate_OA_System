package cn.oa.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 借款还款DTO
 *
 * @author oa-finance
 */
@Data
public class FinLoanRepayDTO {

    @NotNull(message = "还款金额不能为空")
    private BigDecimal amount;

    private String remark;
}
