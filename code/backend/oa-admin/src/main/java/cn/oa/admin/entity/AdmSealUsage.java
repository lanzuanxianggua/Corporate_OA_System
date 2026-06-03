package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章使用记录表
 * 对应表: adm_seal_usage
 *
 * @author oa-admin
 */
@Data
@TableName("adm_seal_usage")
public class AdmSealUsage {

    /**
     * 使用记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 印章ID
     */
    private Long sealId;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 文件/合同名称
     */
    private String documentName;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 使用日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usageDate;

    /**
     * 用途说明
     */
    private String purpose;

    /**
     * 状态(PENDING-待审批 APPROVED-已通过 REJECTED-已驳回)
     */
    private String status;

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
     * 印章名称（关联查询）
     */
    @TableField(exist = false)
    private String sealName;

    /**
     * 申请人姓名（关联查询）
     */
    @TableField(exist = false)
    private String applicantName;
}
