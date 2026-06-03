package cn.oa.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算更新DTO
 *
 * @author oa-finance
 */
@Data
public class FinBudgetUpdateDTO {

    @NotNull(message = "预算ID不能为空")
    private Long id;

    private Long deptId;

    private Long projectId;

    private String expenseCategory;

    private Integer year;

    private Integer month;

    private BigDecimal amount;

    private String controlStrategy;

    private String status;
}
