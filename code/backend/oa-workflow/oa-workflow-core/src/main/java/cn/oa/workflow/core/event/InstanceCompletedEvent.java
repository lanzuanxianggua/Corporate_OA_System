package cn.oa.workflow.core.event;

import cn.oa.workflow.model.entity.WfInstance;

import java.time.LocalDateTime;

/**
 * 流程实例完成事件
 */
public class InstanceCompletedEvent {

    private final Long instanceId;
    private final String businessType;
    private final Long businessId;
    private final Long starterId;
    private final String status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime occurredTime;

    public InstanceCompletedEvent(WfInstance instance) {
        this.instanceId = instance.getId();
        this.businessType = instance.getBusinessType();
        this.businessId = instance.getBusinessId();
        this.starterId = instance.getStarterId();
        this.status = instance.getStatus();
        this.startTime = instance.getStartTime();
        this.endTime = instance.getEndTime();
        this.occurredTime = LocalDateTime.now();
    }

    // Getters
    public Long getInstanceId() { return instanceId; }
    public String getBusinessType() { return businessType; }
    public Long getBusinessId() { return businessId; }
    public Long getStarterId() { return starterId; }
    public String getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getOccurredTime() { return occurredTime; }

    /**
     * 是否为通过
     */
    public boolean isPassed() {
        return "PASSED".equals(status);
    }

    /**
     * 是否为拒绝
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    /**
     * 是否为撤回
     */
    public boolean isRevoked() {
        return "REVOKED".equals(status);
    }

    /**
     * 是否为终止
     */
    public boolean isAborted() {
        return "ABORTED".equals(status);
    }

    /**
     * 计算流程总耗时（毫秒）
     */
    public long getDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
}
