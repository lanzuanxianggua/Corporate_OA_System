package cn.oa.finance.dto;

import lombok.Data;

/**
 * 费用报销查询DTO
 *
 * @author oa-finance
 */
@Data
public class FinExpenseQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long empId;

    private String category;

    private String status;

    private String startDate;

    private String endDate;
}
