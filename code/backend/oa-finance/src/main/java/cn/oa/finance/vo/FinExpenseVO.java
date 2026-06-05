package cn.oa.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销单 VO.
 */
@Data
@Schema(description = "报销单详情")
public class FinExpenseVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "报销单号")
    private String applyNo;

    @Schema(description = "报销人 emp_id")
    private Long empId;

    @Schema(description = "报销人姓名")
    private String empName;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "报销类型")
    private String expenseType;

    @Schema(description = "报销总金额")
    private BigDecimal totalAmount;

    @Schema(description = "报销事由")
    private String reason;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/PAID")
    private String status;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "冲抵借款金额")
    private BigDecimal loanOffsetAmount;

    @Schema(description = "支付时间")
    private LocalDateTime paidTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
