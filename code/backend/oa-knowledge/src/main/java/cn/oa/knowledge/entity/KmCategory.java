package cn.oa.knowledge.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识分类.
 *
 * <p>树形结构, parent_id=0 表示根分类.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_categories")
@Schema(description = "知识分类")
public class KmCategory extends BaseEntity {

    @Schema(description = "分类名称")
    @TableField("category_name")
    private String categoryName;

    @Schema(description = "父分类ID, 0=根分类")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @Schema(description = "分类描述")
    @TableField("description")
    private String description;
}
