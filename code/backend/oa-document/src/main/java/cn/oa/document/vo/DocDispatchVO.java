package cn.oa.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发文 VO.
 */
@Data
@Schema(description = "发文详情")
public class DocDispatchVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "文号")
    private String docNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "主题词")
    private String subjectWord;

    @Schema(description = "发送部门")
    private String sendToDept;

    @Schema(description = "抄送部门")
    private String copyToDept;

    @Schema(description = "紧急程度")
    private String urgency;

    @Schema(description = "密级")
    private String securityLevel;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/PUBLISHED/ARCHIVED")
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
