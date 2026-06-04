package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审批任务.
 */
@Schema(description = "审批任务")
@TableName("wf_tasks")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long nodeId;
    private Long assigneeId;
    private String status;
    private String action;
    private LocalDateTime actionTime;
    private Long actionEmpId;
    private String comment;
    private String attachments;
    private Long delegatedFrom;
    private LocalDateTime dueTime;
    private LocalDateTime createTime;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime actionTime) { this.actionTime = actionTime; }
    public Long getActionEmpId() { return actionEmpId; }
    public void setActionEmpId(Long actionEmpId) { this.actionEmpId = actionEmpId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public Long getDelegatedFrom() { return delegatedFrom; }
    public void setDelegatedFrom(Long delegatedFrom) { this.delegatedFrom = delegatedFrom; }
    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
