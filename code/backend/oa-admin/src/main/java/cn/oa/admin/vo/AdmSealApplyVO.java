package cn.oa.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 印章申请 VO.
 */
@Data
@Schema(description = "印章申请视图")
public class AdmSealApplyVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "申请单号")
    private String applyNo;

    @Schema(description = "印章 ID")
    private Long sealId;

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "申请人 emp_id")
    private Long empId;

    @Schema(description = "申请人姓名")
    private String empName;

    @Schema(description = "申请用途")
    private String purpose;

    @Schema(description = "用印文件名称")
    private String docName;

    @Schema(description = "文件份数")
    private Integer docCount;

    @Schema(description = "期望用印日期")
    private LocalDate expectDate;

    @Schema(description = "实际用印日期")
    private LocalDate useDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
