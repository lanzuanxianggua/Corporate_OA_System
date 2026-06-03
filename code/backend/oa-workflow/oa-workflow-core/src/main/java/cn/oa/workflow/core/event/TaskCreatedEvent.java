package cn.oa.workflow.core.event;

import cn.oa.workflow.model.entity.WfTask;

import java.time.LocalDateTime;

/**
 * 任务创建事件
 */
public class TaskCreatedEvent {

    private final Long taskId;
    private final Long instanceId;
    private final Long nodeId;
    private final String nodeName;
    private final Long assigneeId;
    private final Long originalAssigneeId;
    private final String taskType;
    private final Long parentTaskId;
    private final LocalDateTime dueTime;
    private final LocalDateTime occurredTime;

    public TaskCreatedEvent(WfTask task, String nodeName) {
        this.taskId = task.getId();
        this.instanceId = task.getInstanceId();
        this.nodeId = task.getNodeId();
        this.nodeName = nodeName;
        this.assigneeId = task.getAssigneeId();
        this.originalAssigneeId = task.getOriginalAssigneeId();
        this.taskType = task.getTaskType();
        this.parentTaskId = task.getParentTaskId();
        this.dueTime = task.getDueTime();
        this.occurredTime = LocalDateTime.now();
    }

    // Getters
    public Long getTaskId() { return taskId; }
    public Long getInstanceId() { return instanceId; }
    public Long getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getOriginalAssigneeId() { return originalAssigneeId; }
    public String getTaskType() { return taskType; }
    public Long getParentTaskId() { return parentTaskId; }
    public LocalDateTime getDueTime() { return dueTime; }
    public LocalDateTime getOccurredTime() { return occurredTime; }

    /**
     * 是否为委托任务
     */
    public boolean isDelegated() {
        return originalAssigneeId != null && !originalAssigneeId.equals(assigneeId);
    }

    /**
     * 是否为会签任务
     */
    public boolean isCountersign() {
        return parentTaskId != null;
    }
}
