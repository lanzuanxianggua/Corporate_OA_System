package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建版本 DTO
 */
@Data
@Schema(description = "创建知识版本")
public class KmVersionCreateDTO {

    @NotNull(message = "条目ID不能为空")
    @Schema(description = "条目ID")
    private Long entryId;

    @Schema(description = "版本说明")
    private String comment;
}
