package cn.oa.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识条目查询 DTO
 */
@Data
@Schema(description = "知识条目查询参数")
public class KmEntryQueryDTO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "状态(DRAFT/PUBLISHED/ARCHIVED)")
    private String status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "安全级别")
    private String securityLevel;

    @Schema(description = "页码")
    private int pageNum = 1;

    @Schema(description = "每页条数")
    private int pageSize = 10;
}
