package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产操作DTO（领用/归还/维修/报废）
 *
 * @author oa-admin
 */
@Data
@Schema(description = "资产操作DTO")
public class AdmAssetOperateDTO {

    @NotNull(message = "资产ID不能为空")
    @Schema(description = "资产ID")
    private Long assetId;

    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型(ALLOCATE-领用 RETURN-归还 MAINTAIN-维修 SCRAP-报废)")
    private String operation;

    @Schema(description = "目标用户ID（领用时必填）")
    private Long toUserId;

    @Schema(description = "备注")
    private String remark;
}
