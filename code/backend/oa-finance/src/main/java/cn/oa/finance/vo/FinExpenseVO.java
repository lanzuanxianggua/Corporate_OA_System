package cn.oa.finance.vo;

import cn.oa.finance.entity.FinExpenseDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用报销VO
 *
 * @author oa-finance
 */
@Data
public class FinExpenseVO {

    private Long id;

    private Long empId;

    private String empName;

    private String title;

    private BigDecimal totalAmount;

    private String category;

    private String categoryName;

    private String description;

    private Long relatedTripId;

    private Long relatedLoanId;

    private BigDecimal loanOffsetAmount;

    private String status;

    private String statusName;

    private Long processInstanceId;

    private List<FinExpenseDetail> details;

    /** 当前用户是否可撤回 */
    private Boolean canRevoke;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
