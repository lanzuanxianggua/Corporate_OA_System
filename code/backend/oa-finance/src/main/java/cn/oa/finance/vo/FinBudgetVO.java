package cn.oa.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算VO
 *
 * @author oa-finance
 */
@Data
public class FinBudgetVO {

    private Long id;

    private Long deptId;

    private String deptName;

    private Long projectId;

    private String projectName;

    private String expenseCategory;

    private String expenseCategoryName;

    private Integer year;

    private Integer month;

    private BigDecimal amount;

    private BigDecimal occupiedAmount;

    private BigDecimal executedAmount;

    private BigDecimal availableAmount;

    private String controlStrategy;

    private String status;

    private String statusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
