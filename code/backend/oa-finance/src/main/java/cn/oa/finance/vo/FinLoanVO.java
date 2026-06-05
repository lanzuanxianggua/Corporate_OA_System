package cn.oa.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借款单 VO.
 */
@Data
@Schema(description = "借款单详情")
public class FinLoanVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "借款单号")
    private String applyNo;

    @Schema(description = "借款人 emp_id")
    private Long empId;

    @Schema(description = "借款人姓名")
    private String empName;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "借款类型")
    private String loanType;

    @Schema(description = "借款金额")
    private BigDecimal amount;

    @Schema(description = "借款用途")
    private String purpose;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/SETTLED")
    private String status;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "已还款金额")
    private BigDecimal repaidAmount;

    @Schema(description = "还款期限")
    private LocalDate deadlineDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
