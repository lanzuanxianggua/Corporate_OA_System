package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产表
 * 对应表: adm_asset
 *
 * @author oa-admin
 */
@Data
@TableName("adm_asset")
public class AdmAsset {

    /**
     * 资产ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资产编码
     */
    private String assetCode;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 序列号
     */
    private String sn;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 型号
     */
    private String model;

    /**
     * 资产分类
     */
    private String category;

    /**
     * 状态(IDLE-闲置 IN_USE-使用中 MAINTAINING-维修中 SCRAPPED-已报废)
     */
    private String status;

    /**
     * 当前使用人ID
     */
    private Long currentUserId;

    /**
     * 购买日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    /**
     * 资产价格
     */
    private BigDecimal price;

    /**
     * 删除标志(0存在 1删除)
     */
    @TableLogic
    private String delFlag;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ============ 非数据库字段 ============

    /**
     * 当前使用人姓名（关联查询）
     */
    @TableField(exist = false)
    private String currentUserName;

    /**
     * 状态名称（关联查询）
     */
    @TableField(exist = false)
    private String statusName;
}
