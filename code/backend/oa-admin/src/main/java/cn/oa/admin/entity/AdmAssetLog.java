package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产操作日志表
 * 对应表: adm_asset_log
 *
 * @author oa-admin
 */
@Data
@TableName("adm_asset_log")
public class AdmAssetLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 操作类型(ALLOCATE-领用 RETURN-归还 MAINTAIN-维修 SCRAP-报废)
     */
    private String operation;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 来源用户ID（领用前使用人）
     */
    private Long fromUserId;

    /**
     * 目标用户ID（领用后使用人）
     */
    private Long toUserId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
