package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 办公用品表
 * 对应表: adm_supply
 *
 * @author oa-admin
 */
@Data
@TableName("adm_supply")
public class AdmSupply {

    /**
     * 用品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用品名称
     */
    private String supplyName;

    /**
     * 规格型号
     */
    private String specification;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 用品分类
     */
    private String category;

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
}
