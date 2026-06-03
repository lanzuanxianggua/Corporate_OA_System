package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 办公用品库存表
 * 对应表: adm_supply_stock
 *
 * @author oa-admin
 */
@Data
@TableName("adm_supply_stock")
public class AdmSupplyStock {

    /**
     * 库存ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用品ID
     */
    private Long supplyId;

    /**
     * 总库存数量
     */
    private Integer totalQty;

    /**
     * 可用数量
     */
    private Integer availableQty;

    /**
     * 锁定数量
     */
    private Integer lockedQty;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

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
     * 用品名称（关联查询）
     */
    @TableField(exist = false)
    private String supplyName;

    /**
     * 用品规格（关联查询）
     */
    @TableField(exist = false)
    private String specification;

    /**
     * 计量单位（关联查询）
     */
    @TableField(exist = false)
    private String unit;

    /**
     * 用品分类（关联查询）
     */
    @TableField(exist = false)
    private String category;
}
