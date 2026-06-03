package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 出入库操作DTO
 *
 * @author oa-admin
 */
@Data
@Schema(description = "出入库操作DTO")
public class AdmStockOperationDTO {

    @NotNull(message = "用品ID不能为空")
    @Schema(description = "用品ID")
    private Long supplyId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @Schema(description = "操作数量")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;
}
