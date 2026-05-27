package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_asset")
public class OaAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String assetCode;

    private String assetName;

    private String category;

    private String specification;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    /** 0-idle 1-in-use 2-repair 3-scrapped */
    private String status = "0";

    private Long currentUserId;

    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;
}
