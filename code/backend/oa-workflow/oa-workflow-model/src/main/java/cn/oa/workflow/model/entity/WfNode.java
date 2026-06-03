package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点表
 */
@Data
@TableName("wf_node")
public class WfNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义ID */
    private Long defId;

    /** 节点编码 */
    private String nodeCode;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型: APPROVAL/CONDITION/PARALLEL/CC/SUBPROCESS/START/END */
    private String nodeType;

    /** 前置节点ID列表(JSON) */
    private String prevNodeIds;

    /** 后置节点ID列表(JSON) */
    private String nextNodeIds;

    /** 审批模式: COUNTERSIGN/ORSIGN/SEQUENTIAL/PROPORTIONAL/VOTE */
    private String approvalMode;

    /** 通过比例（比例模式） */
    private Double passRatio;

    /** 一票否决 */
    private Integer vetoEnabled;

    /** 超时时长（小时） */
    private Integer timeoutHours;

    /** 超时动作: AUTO_APPROVE/AUTO_REJECT/ESCALATE/NOTIFY */
    private String timeoutAction;

    /** 字段权限矩阵(JSON): {"field1":"R","field2":"E","field3":"H","field4":"M"} */
    private String fieldPermission;

    /** 审批人为空策略: AUTO_SKIP/ADMIN_ASSIGN/ERROR */
    private String emptyAssigneeStrategy;

    /** 任务创建策略: ON_ENTER/AFTER_PREV_COMPLETE */
    private String taskCreateStrategy;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}