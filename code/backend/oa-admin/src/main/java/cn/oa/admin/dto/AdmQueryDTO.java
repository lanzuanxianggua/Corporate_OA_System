package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 综合行政查询DTO
 *
 * @author oa-admin
 */
@Data
@Schema(description = "综合行政查询DTO")
public class AdmQueryDTO {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "关键字（名称模糊搜索）")
    private String keyword;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序方式(asc/desc)")
    private String sortOrder;
}
