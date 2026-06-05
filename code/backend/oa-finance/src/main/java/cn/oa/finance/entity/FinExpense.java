package cn.oa.finance.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销单.
 *
 * <p>对应表 fin_expenses, 与 wf_instance 通过 wf_instance_id 关联.
 * 状态: DRAFT/PENDING/APPROVED/REJECTED/PAID
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_expenses")
@Schema(description = "报销单")
public class FinExpense extends BaseEntity {

    @Schema(description = "报销单号")
    @TableField("apply_no")
    private String applyNo;

    @Schema(description = "报销人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "所属部门 ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "报销类型: TRAVEL/MEAL/OFFICE/OTHER")
    @TableField("expense_type")
    private String expenseType;

    @Schema(description = "报销总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @Schema(description = "报销事由")
    @TableField("reason")
    private String reason;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/PAID")
    @TableField("status")
    private String status;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;

    @Schema(description = "冲抵借款金额")
    @TableField("loan_offset_amount")
    private BigDecimal loanOffsetAmount;

    @Schema(description = "支付时间")
    @TableField("paid_time")
    private LocalDateTime paidTime;
}
