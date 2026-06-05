package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产管理.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_asset")
@Schema(description = "资产管理")
public class AdmAsset extends BaseEntity {

    @Schema(description = "资产编号")
    private String assetCode;

    @Schema(description = "资产名称")
    private String assetName;

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

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "保管人ID")
    private Long custodian;

    @Schema(description = "状态: IDLE/IN_USE/REPAIR/SCRAPPED")
    private String status;

    @Schema(description = "存放位置")
    private String location;
}
