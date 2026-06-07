package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 资产领用/归还记录.
 *
 * <p>对应表 adm_asset_loans.
 * 状态: DRAFT/PENDING/APPROVED/REJECTED/RETURNED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_asset_loans")
@Schema(description = "资产领用/归还记录")
public class AdmAssetLoan extends BaseEntity {

    @Schema(description = "领用单号")
    @TableField("loan_no")
    private String loanNo;

    @Schema(description = "资产 ID")
    @TableField("asset_id")
    private Long assetId;

    @Schema(description = "领用人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "领用类型: BORROW/RETURN/SCRAP")
    @TableField("loan_type")
    private String loanType;

    @Schema(description = "领用日期")
    @TableField("loan_date")
    private LocalDate loanDate;

    @Schema(description = "预计归还日期")
    @TableField("expect_return_date")
    private LocalDate expectReturnDate;

    @Schema(description = "实际归还日期")
    @TableField("actual_return_date")
    private LocalDate actualReturnDate;

    @Schema(description = "用途说明")
    @TableField("purpose")
    private String purpose;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/RETURNED")
    @TableField("status")
    private String status;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;
}
