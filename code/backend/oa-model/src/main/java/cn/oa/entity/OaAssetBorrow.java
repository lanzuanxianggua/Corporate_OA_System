package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_asset_borrow")
public class OaAssetBorrow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private Long borrowerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedReturn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualReturn;

    /** 0-borrowed 1-returned */
    private String status = "0";

    private String remark;

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

    /** 资产名称（非数据库字段） */
    @TableField(exist = false)
    private String assetName;

    /** 借用人姓名（非数据库字段） */
    @TableField(exist = false)
    private String borrower;
}
