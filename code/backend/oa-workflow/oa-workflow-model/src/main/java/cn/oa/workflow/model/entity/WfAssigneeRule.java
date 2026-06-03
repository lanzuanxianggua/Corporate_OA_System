package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批人解析规则表
 */
@Data
@TableName("wf_assignee_rule")
public class WfAssigneeRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节点ID */
    private Long nodeId;

    /** 规则类型: FIXED_USER/POST/DEPT_LEADER/REPORT_LINE/FORM_SELECT/API */
    private String ruleType;

    /** 规则值（岗位code/层级数/API地址等） */
    private String ruleValue;

    /** 排序 */
    private Integer sortOrder;

    /** 失败策略: SKIP/NEXT_RULE/ERROR */
    private String failStrategy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}