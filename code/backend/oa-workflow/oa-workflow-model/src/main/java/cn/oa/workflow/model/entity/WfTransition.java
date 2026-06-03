package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流转条件表
 */
@Data
@TableName("wf_transition")
public class WfTransition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义ID */
    private Long defId;

    /** 来源节点ID */
    private Long fromNodeId;

    /** 目标节点ID */
    private Long toNodeId;

    /** 条件名称 */
    private String conditionName;

    /** 条件表达式 e.g. amount>5000 && type=="travel" */
    private String expression;

    /** 优先级 */
    private Integer priority;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}