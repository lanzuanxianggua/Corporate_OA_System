package cn.oa.finance.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 还款记录.
 *
 * <p>对应表 fin_loan_repayments, 关联借款单.
 * 还款类型: CASH/DEDUCT/EXPENSE_OFFSET
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_loan_repayments")
@Schema(description = "还款记录")
public class FinLoanRepayment extends BaseEntity {

    @Schema(description = "关联借款 ID")
    @TableField("loan_id")
    private Long loanId;

    @Schema(description = "还款金额")
    @TableField("repay_amount")
    private BigDecimal repayAmount;

    @Schema(description = "还款类型: CASH/DEDUCT/EXPENSE_OFFSET")
    @TableField("repay_type")
    private String repayType;

    @Schema(description = "关联报销单 ID（冲抵时）")
    @TableField("expense_id")
    private Long expenseId;

    @Schema(description = "还款日期")
    @TableField("repay_date")
    private LocalDate repayDate;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
