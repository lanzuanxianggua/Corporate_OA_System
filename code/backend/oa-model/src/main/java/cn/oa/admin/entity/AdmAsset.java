package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("adm_asset")
public class AdmAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String assetCode;

    private String assetName;

    private String category;

    private String specification;

    private String sn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    /** 0-idle 1-in-use 2-repair 3-scrapped */
    private String status;

    private Long currentUserId;

    private Long deptId;

    private String location;

    private String qrCode;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}