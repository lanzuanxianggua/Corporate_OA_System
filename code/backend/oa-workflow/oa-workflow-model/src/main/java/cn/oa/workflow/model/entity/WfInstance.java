package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例表 - 记录每个运行时流程
 */
@Data
@TableName("wf_instance")
public class WfInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义ID */
    private Long defId;

    /** 启动时的流程版本 */
    private Integer defVersion;

    /** 流程定义快照(JSON): 节点+条件+审批规则 */
    private String defSnapshot;

    /** 业务类型 */
    private String businessType;

    /** 业务表单ID */
    private Long businessId;

    /** 发起人ID */
    private Long starterId;

    /** 提交时的表单数据快照(JSON) */
    private String formDataSnapshot;

    /** 条件上下文(JSON)，用于条件路由计算 */
    private String conditionContext;

    /** 状态: DRAFT/RUNNING/SUSPENDED/ABORTED/PASSED/REJECTED/REVOKED */
    private String status;

    /** 当前活跃节点ID列表(JSON)，支持并行 */
    private String currentNodeIds;

    /** 驳回源节点ID（重新提交时使用） */
    private Long returnSourceNodeId;

    /** 重新提交策略: DIRECT_RETURN/SEQUENTIAL_RETURN */
    private String returnStrategy;

    /** 父实例ID（子流程） */
    private Long parentInstanceId;

    /** 启动时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 挂起时间 */
    private LocalDateTime suspendTime;

    /** 挂起原因 */
    private String suspendReason;

    /** 终止原因 */
    private String abortReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}