package cn.oa.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借款VO
 *
 * @author oa-finance
 */
@Data
public class FinLoanVO {

    private Long id;

    private Long empId;

    private String empName;

    private BigDecimal loanAmount;

    private BigDecimal repaidAmount;

    private BigDecimal remainingAmount;

    private String loanReason;

    private String repaymentPlan;

    private String status;

    private String statusName;

    private Long processInstanceId;

    /** 当前用户是否可撤回 */
    private Boolean canRevoke;

    /** 当前用户是否可还款 */
    private Boolean canRepay;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
