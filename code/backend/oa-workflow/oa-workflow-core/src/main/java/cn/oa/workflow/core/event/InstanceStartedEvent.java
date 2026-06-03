package cn.oa.workflow.core.event;

import cn.oa.workflow.model.entity.WfInstance;

import java.time.LocalDateTime;

/**
 * 流程实例启动事件
 */
public class InstanceStartedEvent {

    private final Long instanceId;
    private final String businessType;
    private final Long businessId;
    private final Long starterId;
    private final Long definitionId;
    private final Integer definitionVersion;
    private final LocalDateTime occurredTime;

    public InstanceStartedEvent(WfInstance instance) {
        this.instanceId = instance.getId();
        this.businessType = instance.getBusinessType();
        this.businessId = instance.getBusinessId();
        this.starterId = instance.getStarterId();
        this.definitionId = instance.getDefId();
        this.definitionVersion = instance.getDefVersion();
        this.occurredTime = LocalDateTime.now();
    }

    // Getters
    public Long getInstanceId() { return instanceId; }
    public String getBusinessType() { return businessType; }
    public Long getBusinessId() { return businessId; }
    public Long getStarterId() { return starterId; }
    public Long getDefinitionId() { return definitionId; }
    public Integer getDefinitionVersion() { return definitionVersion; }
    public LocalDateTime getOccurredTime() { return occurredTime; }
}
