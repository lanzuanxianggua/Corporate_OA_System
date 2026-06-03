package cn.oa.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借款还款记录VO
 *
 * @author oa-finance
 */
@Data
public class FinLoanRepaymentVO {

    private Long id;

    private Long loanId;

    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repayTime;

    private String remark;
}
