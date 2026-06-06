package cn.oa.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data @Schema(description = "知识分类")
public class KmCategoryVO {

    private Long id; private String categoryName; private Long parentId;

    private Integer sortOrder; private String description;

    private List<KmCategoryVO> children;
}