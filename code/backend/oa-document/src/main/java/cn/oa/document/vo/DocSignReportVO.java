package cn.oa.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签报 VO.
 */
@Data
@Schema(description = "签报详情")
public class DocSignReportVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "签报编号")
    private String reportNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "签报类型")
    private String reportType;

    @Schema(description = "签报内容")
    private String content;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "申请人 emp_id")
    private Long empId;

    @Schema(description = "申请人姓名")
    private String empName;

    @Schema(description = "所属部门 dept_id")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
