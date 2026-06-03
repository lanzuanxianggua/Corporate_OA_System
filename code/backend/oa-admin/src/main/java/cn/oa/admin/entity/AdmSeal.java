package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章表
 * 对应表: adm_seal
 *
 * @author oa-admin
 */
@Data
@TableName("adm_seal")
public class AdmSeal {

    /**
     * 印章ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 印章名称
     */
    private String sealName;

    /**
     * 印章类型(ORG/PERSONAL/SPECIAL)
     */
    private String sealType;

    /**
     * 保管人ID
     */
    private Long keeperId;

    /**
     * 状态(NORMAL-正常 DISABLED-停用 LOST-遗失)
     */
    private String status;

    /**
     * 印章描述
     */
    private String description;

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
     * 保管人姓名（关联查询）
     */
    @TableField(exist = false)
    private String keeperName;
}
