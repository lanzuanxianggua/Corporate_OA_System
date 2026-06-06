package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data @Schema(description = "创建分类请求")
public class KmCategoryCreateDTO {

    @NotBlank @Schema(description = "分类名称") private String categoryName;

    @Schema(description = "父分类ID") private Long parentId = 0L;

    @Schema(description = "排序") private Integer sortOrder;

    @Schema(description = "描述") private String description;
}