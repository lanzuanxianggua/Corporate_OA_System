package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 流程流转.
 */
@Schema(description = "流程流转")
@TableName("wf_transitions")
public class WfTransition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defId;
    private Long fromNodeId;
    private Long toNodeId;
    private String conditionExpr;
    private String action;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDefId() { return defId; }
    public void setDefId(Long defId) { this.defId = defId; }
    public Long getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(Long fromNodeId) { this.fromNodeId = fromNodeId; }
    public Long getToNodeId() { return toNodeId; }
    public void setToNodeId(Long toNodeId) { this.toNodeId = toNodeId; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
