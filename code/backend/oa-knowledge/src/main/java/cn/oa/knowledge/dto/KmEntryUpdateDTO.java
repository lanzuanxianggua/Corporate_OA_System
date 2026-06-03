package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新知识条目 DTO
 */
@Data
@Schema(description = "更新知识条目")
public class KmEntryUpdateDTO {

    @NotNull(message = "ID不能为空")
    @Schema(description = "条目ID")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200字符")
    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "安全级别")
    private String securityLevel;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签列表")
    private List<String> tags;
}
