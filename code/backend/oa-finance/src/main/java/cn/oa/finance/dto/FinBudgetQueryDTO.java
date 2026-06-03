package cn.oa.finance.dto;

import lombok.Data;

/**
 * 预算查询DTO
 *
 * @author oa-finance
 */
@Data
public class FinBudgetQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long deptId;

    private Long projectId;

    private String expenseCategory;

    private Integer year;

    private Integer month;

    private String status;
}
