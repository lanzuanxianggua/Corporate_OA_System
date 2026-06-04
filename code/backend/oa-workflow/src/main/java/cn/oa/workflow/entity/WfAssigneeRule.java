package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审批人规则.
 */
@Schema(description = "审批人规则")
@TableName("wf_assignee_rules")
public class WfAssigneeRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defId;
    private String ruleType;
    private String ruleTarget;
    private Integer priority;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDefId() { return defId; }
    public void setDefId(Long defId) { this.defId = defId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getRuleTarget() { return ruleTarget; }
    public void setRuleTarget(String ruleTarget) { this.ruleTarget = ruleTarget; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
