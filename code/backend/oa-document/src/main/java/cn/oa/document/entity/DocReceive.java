package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 收文.
 *
 * <p>对应表 doc_receives.
 * 状态: PENDING / COMPLETED / ARCHIVED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_receives")
@Schema(description = "收文")
public class DocReceive extends BaseEntity {

    @Schema(description = "来文单位")
    @TableField("source_dept")
    private String sourceDept;

    @Schema(description = "公文标题")
    @TableField("doc_title")
    private String docTitle;

    @Schema(description = "来文日期")
    @TableField("doc_date")
    private LocalDate docDate;

    @Schema(description = "收文日期")
    @TableField("receive_date")
    private LocalDate receiveDate;

    @Schema(description = "紧急程度: URGENT/EMERGENCY/NORMAL")
    @TableField("urgent_level")
    private String urgentLevel;

    @Schema(description = "内容摘要")
    @TableField("content")
    private String content;

    @Schema(description = "拟办意见")
    @TableField("process_opinion")
    private String processOpinion;

    @Schema(description = "状态: PENDING/COMPLETED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "承办部门 dept_id")
    @TableField("process_dept_id")
    private Long processDeptId;
}
