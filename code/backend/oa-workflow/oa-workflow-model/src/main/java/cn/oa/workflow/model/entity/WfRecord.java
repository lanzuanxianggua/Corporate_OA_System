package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流转记录表（审计日志） - 任何动作都留痕
 */
@Data
@TableName("wf_record")
public class WfRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程实例ID */
    private Long instanceId;

    /** 关联任务ID */
    private Long taskId;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作: SUBMIT/APPROVE/REJECT/TRANSFER/ADD_SIGN/WITHDRAW/URGE/SUSPEND/RESUME/ABORT */
    private String action;

    /** 来源节点ID */
    private Long fromNodeId;

    /** 目标节点ID */
    private Long toNodeId;

    /** 操作意见 */
    private String opinion;

    /** 操作前表单字段快照(JSON) */
    private String fieldSnapshotBefore;

    /** 操作后表单字段快照(JSON) */
    private String fieldSnapshotAfter;

    /** 处理耗时（毫秒） */
    private Long durationMs;

    /** 操作IP */
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}