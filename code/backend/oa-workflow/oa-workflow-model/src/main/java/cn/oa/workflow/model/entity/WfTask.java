package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务表 - 记录每个审批任务
 */
@Data
@TableName("wf_task")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程实例ID */
    private Long instanceId;

    /** 节点ID */
    private Long nodeId;

    /** 处理人ID */
    private Long assigneeId;

    /** 原始处理人（委托场景） */
    private Long originalAssigneeId;

    /** 任务类型: TODO/COUNTERSIGN/ADD_SIGN_FRONT/ADD_SIGN_BEHIND */
    private String taskType;

    /** 状态: PENDING/APPROVED/REJECTED/TRANSFERRED/CANCELED */
    private String status;

    /** 父任务ID（加签/会签分组） */
    private Long parentTaskId;

    /** 审批意见 */
    private String opinion;

    /** 手写签批图片URL */
    private String signature;

    /** 接收时间 */
    private LocalDateTime startTime;

    /** 截止时间 */
    private LocalDateTime dueTime;

    /** 处理时间 */
    private LocalDateTime endTime;

    /** 催办次数 */
    private Integer remindCount;

    /** 最后催办时间 */
    private LocalDateTime lastRemindTime;

    /** 是否已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // === 非数据库字段（用于前端展示） ===

    /** 业务标题 */
    @TableField(exist = false)
    private String businessTitle;

    /** 业务类型 */
    @TableField(exist = false)
    private String businessType;

    /** 流程实例 */
    @TableField(exist = false)
    private WfInstance instance;

    /** 审批人姓名 */
    @TableField(exist = false)
    private String assigneeName;

    /** 节点名称 */
    @TableField(exist = false)
    private String nodeName;
}