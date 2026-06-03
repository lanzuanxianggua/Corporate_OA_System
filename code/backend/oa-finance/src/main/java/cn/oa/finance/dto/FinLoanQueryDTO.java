package cn.oa.finance.dto;

import lombok.Data;

/**
 * 借款查询DTO
 *
 * @author oa-finance
 */
@Data
public class FinLoanQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long empId;

    private Long deptId;

    private String status;

    private String startDate;

    private String endDate;

    private String sortField;

    private String sortOrder;
}
