package cn.oa.finance.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算.
 *
 * <p>对应表 fin_budgets, 按部门+年度管理预算总额.
 * 状态: ACTIVE/FROZEN/CLOSED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_budgets")
@Schema(description = "预算")
public class FinBudget extends BaseEntity {

    @Schema(description = "预算责任人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "所属部门 ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "预算年度")
    @TableField("budget_year")
    private Integer budgetYear;

    @Schema(description = "预算名称")
    @TableField("budget_name")
    private String budgetName;

    @Schema(description = "总预算金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @Schema(description = "已使用金额")
    @TableField("used_amount")
    private BigDecimal usedAmount;

    @Schema(description = "审批中冻结金额")
    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @Schema(description = "状态: ACTIVE/FROZEN/CLOSED")
    @TableField("status")
    private String status;
}
