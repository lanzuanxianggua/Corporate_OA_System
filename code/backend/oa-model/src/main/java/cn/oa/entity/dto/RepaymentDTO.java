package cn.oa.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 借支还款DTO
 */
@Data
public class RepaymentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 借支ID */
    @NotNull(message = "loanId不能为空")
    private Long loanId;

    /** 还款金额 */
    @NotNull(message = "还款金额不能为空")
    private java.math.BigDecimal amount;

    /** 备注 */
    private String remark;
}
