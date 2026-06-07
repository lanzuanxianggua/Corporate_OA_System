package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产修改 DTO.
 */
@Data
@Schema(description = "资产修改请求")
public class AdmAssetUpdateDTO {

    @NotBlank(message = "资产名称不能为空")
    @Schema(description = "资产名称")
    private String assetName;

    @NotBlank(message = "资产分类不能为空")
    @Schema(description = "分类: IT/OFFICE/VEHICLE/OTHER")
    private String category;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "购买日期")
    private LocalDate purchaseDate;

    @Schema(description = "购买价格")
    private BigDecimal purchasePrice;

    @NotNull(message = "所属部门不能为空")
    @Schema(description = "所属部门ID")
    private Long deptId;

    @NotNull(message = "保管人不能为空")
    @Schema(description = "保管人ID")
    private Long custodian;

    @Schema(description = "存放位置")
    private String location;
}
