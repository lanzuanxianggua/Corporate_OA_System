package cn.oa.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收文 VO.
 */
@Data
@Schema(description = "收文详情")
public class DocReceiveVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "收文号")
    private String receiveNo;

    @Schema(description = "来文单位")
    private String sourceDept;

    @Schema(description = "公文标题")
    private String docTitle;

    @Schema(description = "来文日期")
    private LocalDate docDate;

    @Schema(description = "收文日期")
    private LocalDate receiveDate;

    @Schema(description = "紧急程度")
    private String urgentLevel;

    @Schema(description = "内容摘要")
    private String content;

    @Schema(description = "拟办意见")
    private String processOpinion;

    @Schema(description = "状态: PENDING/COMPLETED/ARCHIVED")
    private String status;

    @Schema(description = "承办部门 dept_id")
    private Long processDeptId;

    @Schema(description = "承办部门名称")
    private String processDeptName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
