package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 流程节点.
 */
@Schema(description = "流程节点")
@TableName("wf_nodes")
public class WfNode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defId;
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private Long assigneeRuleId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDefId() { return defId; }
    public void setDefId(Long defId) { this.defId = defId; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Long getAssigneeRuleId() { return assigneeRuleId; }
    public void setAssigneeRuleId(Long assigneeRuleId) { this.assigneeRuleId = assigneeRuleId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
