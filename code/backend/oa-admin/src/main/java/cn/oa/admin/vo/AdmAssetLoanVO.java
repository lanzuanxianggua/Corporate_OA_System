package cn.oa.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产领用 VO.
 */
@Data
@Schema(description = "资产领用视图")
public class AdmAssetLoanVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "领用单号")
    private String loanNo;

    @Schema(description = "资产 ID")
    private Long assetId;

    @Schema(description = "资产编号")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "领用人 emp_id")
    private Long empId;

    @Schema(description = "领用人姓名")
    private String empName;

    @Schema(description = "领用类型: BORROW/RETURN/SCRAP")
    private String loanType;

    @Schema(description = "领用日期")
    private LocalDate loanDate;

    @Schema(description = "预计归还日期")
    private LocalDate expectReturnDate;

    @Schema(description = "实际归还日期")
    private LocalDate actualReturnDate;

    @Schema(description = "用途说明")
    private String purpose;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
