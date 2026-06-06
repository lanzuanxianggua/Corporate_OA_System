package cn.oa.knowledge.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识条目.
 *
 * <p>状态流转: DRAFT -> PUBLISHED -> ARCHIVED.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_entries")
@Schema(description = "知识条目")
public class KmEntry extends BaseEntity {

    @Schema(description = "标题")
    @TableField("title")
    private String title;

    @Schema(description = "正文/Markdown")
    @TableField("content")
    private String content;

    @Schema(description = "摘要")
    @TableField("summary")
    private String summary;

    @Schema(description = "分类ID")
    @TableField("category_id")
    private Long categoryId;

    @Schema(description = "标签, 逗号分隔")
    @TableField("tags")
    private String tags;

    @Schema(description = "状态: DRAFT/PUBLISHED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "浏览量")
    @TableField("view_count")
    private Integer viewCount;

    @Schema(description = "创建人emp_id")
    @TableField("create_emp_id")
    private Long createEmpId;
}
