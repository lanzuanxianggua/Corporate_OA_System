package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产创建DTO
 *
 * @author oa-admin
 */
@Data
@Schema(description = "资产创建DTO")
public class AdmAssetCreateDTO {

    @NotBlank(message = "资产编码不能为空")
    @Schema(description = "资产编码")
    private String assetCode;

    @NotBlank(message = "资产名称不能为空")
    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "序列号")
    private String sn;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @NotBlank(message = "资产分类不能为空")
    @Schema(description = "资产分类")
    private String category;

    @Schema(description = "当前使用人ID")
    private Long currentUserId;

    @NotNull(message = "购买日期不能为空")
    @Schema(description = "购买日期")
    private LocalDate purchaseDate;

    @NotNull(message = "资产价格不能为空")
    @Schema(description = "资产价格")
    private BigDecimal price;
}
