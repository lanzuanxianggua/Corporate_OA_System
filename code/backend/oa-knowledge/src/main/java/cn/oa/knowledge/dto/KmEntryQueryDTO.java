package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data @Schema(description = "知识条目查询")
public class KmEntryQueryDTO {

    @Schema(description = "分类ID") private Long categoryId;

    @Schema(description = "状态") private String status;

    @Schema(description = "页码") private Integer pageNum = 1;

    @Schema(description = "每页大小") private Integer pageSize = 10;
}