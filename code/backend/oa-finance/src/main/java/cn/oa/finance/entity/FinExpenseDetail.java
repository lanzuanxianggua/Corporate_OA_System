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
 * 报销明细.
 *
 * <p>对应表 fin_expense_details, 多行关联一笔报销单.
 * 费用类型: TRANSPORT/ACCOMMODATION/MEAL/OTHER
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_expense_details")
@Schema(description = "报销明细")
public class FinExpenseDetail extends BaseEntity {

    @Schema(description = "关联报销单 ID")
    @TableField("expense_id")
    private Long expenseId;

    @Schema(description = "费用日期")
    @TableField("fee_date")
    private LocalDate feeDate;

    @Schema(description = "费用类型: TRANSPORT/ACCOMMODATION/MEAL/OTHER")
    @TableField("fee_type")
    private String feeType;

    @Schema(description = "金额")
    @TableField("amount")
    private BigDecimal amount;

    @Schema(description = "发票号")
    @TableField("invoice_no")
    private String invoiceNo;

    @Schema(description = "发票金额")
    @TableField("invoice_amount")
    private BigDecimal invoiceAmount;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
