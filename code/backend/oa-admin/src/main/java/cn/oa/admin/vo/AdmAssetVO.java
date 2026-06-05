package cn.oa.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产 VO.
 */
@Data
@Schema(description = "资产视图对象")
public class AdmAssetVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "资产编号")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "购买日期")
    private LocalDate purchaseDate;

    @Schema(description = "购买价格")
    private BigDecimal purchasePrice;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "保管人ID")
    private Long custodian;

    @Schema(description = "保管人姓名")
    private String custodianName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
