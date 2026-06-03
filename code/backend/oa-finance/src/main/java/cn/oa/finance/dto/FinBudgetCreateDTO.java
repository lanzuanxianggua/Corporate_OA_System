package cn.oa.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建DTO
 *
 * @author oa-finance
 */
@Data
public class FinBudgetCreateDTO {

    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    private Long projectId;

    @NotBlank(message = "费用类别不能为空")
    private String expenseCategory;

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "预算金额不能为空")
    private BigDecimal amount;

    private String controlStrategy;
}
