package cn.oa.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收文查询 DTO.
 */
@Data
@Schema(description = "收文查询参数")
public class DocReceiveQueryDTO {

    @Schema(description = "状态: PENDING/COMPLETED/ARCHIVED")
    private String status;

    @Schema(description = "关键词(标题/来文单位)")
    private String keyword;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
