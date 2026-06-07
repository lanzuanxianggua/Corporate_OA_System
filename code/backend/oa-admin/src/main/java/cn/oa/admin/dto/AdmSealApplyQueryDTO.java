package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 印章申请查询 DTO.
 */
@Data
@Schema(description = "印章申请查询参数")
public class AdmSealApplyQueryDTO {

    @Schema(description = "印章 ID")
    private Long sealId;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/USED/ARCHIVED")
    private String status;

    @Schema(description = "开始日期(过滤期望用印日期)")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
