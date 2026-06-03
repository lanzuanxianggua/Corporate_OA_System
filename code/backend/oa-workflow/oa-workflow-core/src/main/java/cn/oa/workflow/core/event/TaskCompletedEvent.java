package cn.oa.workflow.core.event;

import cn.oa.workflow.model.entity.WfTask;

import java.time.LocalDateTime;

/**
 * 任务完成事件
 */
public class TaskCompletedEvent {

    private final Long taskId;
    private final Long instanceId;
    private final Long nodeId;
    private final Long assigneeId;
    private final String result;
    private final String opinion;
    private final String signature;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime occurredTime;

    public TaskCompletedEvent(WfTask task) {
        this.taskId = task.getId();
        this.instanceId = task.getInstanceId();
        this.nodeId = task.getNodeId();
        this.assigneeId = task.getAssigneeId();
        this.result = task.getStatus();
        this.opinion = task.getOpinion();
        this.signature = task.getSignature();
        this.startTime = task.getStartTime();
        this.endTime = task.getEndTime();
        this.occurredTime = LocalDateTime.now();
    }

    // Getters
    public Long getTaskId() { return taskId; }
    public Long getInstanceId() { return instanceId; }
    public Long getNodeId() { return nodeId; }
    public Long getAssigneeId() { return assigneeId; }
    public String getResult() { return result; }
    public String getOpinion() { return opinion; }
    public String getSignature() { return signature; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getOccurredTime() { return occurredTime; }

    /**
     * 是否为同意
     */
    public boolean isApproved() {
        return "APPROVED".equals(result);
    }

    /**
     * 是否为拒绝
     */
    public boolean isRejected() {
        return "REJECTED".equals(result);
    }

    /**
     * 计算处理耗时（毫秒）
     */
    public long getDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
}
