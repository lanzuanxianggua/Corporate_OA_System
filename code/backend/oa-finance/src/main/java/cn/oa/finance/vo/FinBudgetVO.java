package cn.oa.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算 VO.
 */
@Data
@Schema(description = "预算详情")
public class FinBudgetVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "预算责任人 emp_id")
    private Long empId;

    @Schema(description = "预算责任人姓名")
    private String empName;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "预算名称")
    private String budgetName;

    @Schema(description = "预算年度")
    private Integer budgetYear;

    @Schema(description = "总预算金额")
    private BigDecimal totalAmount;

    @Schema(description = "已使用金额")
    private BigDecimal usedAmount;

    @Schema(description = "审批中冻结金额")
    private BigDecimal frozenAmount;

    @Schema(description = "状态: ACTIVE/FROZEN/CLOSED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
