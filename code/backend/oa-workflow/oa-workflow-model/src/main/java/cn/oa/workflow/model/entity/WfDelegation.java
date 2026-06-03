package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批委托表
 */
@Data
@TableName("wf_delegation")
public class WfDelegation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 委托人ID */
    private Long delegatorId;

    /** 被委托人ID */
    private Long delegateId;

    /** 流程分类（NULL=全部） */
    private String processCategory;

    /** 业务类型（NULL=全部） */
    private String businessType;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 状态: ACTIVE/CANCELED/EXPIRED */
    private String status;

    /** 是否仍提醒委托人 */
    private Integer notifyDelegator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}