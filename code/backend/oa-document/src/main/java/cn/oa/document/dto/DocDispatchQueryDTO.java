package cn.oa.document.dto;

import cn.oa.document.constant.DocConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发文查询 DTO.
 */
@Data
@Schema(description = "发文查询参数")
public class DocDispatchQueryDTO {

    @Schema(description = "状态: " + DocConstants.DISPATCH_STATUS_DRAFT + "/" + DocConstants.DISPATCH_STATUS_PENDING + "/" + DocConstants.DISPATCH_STATUS_APPROVED + "/" + DocConstants.DISPATCH_STATUS_PUBLISHED + "/" + DocConstants.DISPATCH_STATUS_ARCHIVED)
    private String status;

    @Schema(description = "关键词(标题/主题词)")
    private String keyword;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
