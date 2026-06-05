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
 * 借款单.
 *
 * <p>对应表 fin_loans, 与 wf_instance 通过 wf_instance_id 关联.
 * 状态: DRAFT/PENDING/APPROVED/REJECTED/SETTLED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_loans")
@Schema(description = "借款单")
public class FinLoan extends BaseEntity {

    @Schema(description = "借款单号")
    @TableField("apply_no")
    private String applyNo;

    @Schema(description = "借款人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "所属部门 ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "借款类型: TRAVEL/BUSINESS/OTHER")
    @TableField("loan_type")
    private String loanType;

    @Schema(description = "借款金额")
    @TableField("amount")
    private BigDecimal amount;

    @Schema(description = "借款用途")
    @TableField("purpose")
    private String purpose;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/SETTLED")
    @TableField("status")
    private String status;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;

    @Schema(description = "已还款金额")
    @TableField("repaid_amount")
    private BigDecimal repaidAmount;

    @Schema(description = "还款期限")
    @TableField("deadline_date")
    private LocalDate deadlineDate;
}
