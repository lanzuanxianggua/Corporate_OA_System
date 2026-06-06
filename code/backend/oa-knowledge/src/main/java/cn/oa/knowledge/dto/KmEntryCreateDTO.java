package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data @Schema(description = "创建知识条目请求")
public class KmEntryCreateDTO {

    @NotBlank @Schema(description = "标题") private String title;

    @Schema(description = "正文内容") private String content;

    @Schema(description = "摘要") private String summary;

    @Schema(description = "分类ID") private Long categoryId;

    @Schema(description = "标签(逗号分隔)") private String tags;
}