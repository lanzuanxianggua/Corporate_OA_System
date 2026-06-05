package cn.oa.workflow.event;

import lombok.Getter;

/**
 * 流程实例完成事件.
 *
 * <p>工作流引擎在流程实例达到终端状态(APPROVED/REJECTED)时发布此事件.
 * 业务模块通过 {@code @EventListener} 监听并执行后续动作(如更新业务单据状态、扣减余额等).
 *
 * <p>此为 Spring 普通 POJO 事件(4.2+ 支持), 无需继承 ApplicationEvent.
 */
@Getter
public class WfInstanceCompletedEvent {

    private final Long instanceId;
    private final String status;
    private final String businessKey;

    public WfInstanceCompletedEvent(Long instanceId, String status, String businessKey) {
        this.instanceId = instanceId;
        this.status = status;
        this.businessKey = businessKey;
    }
}
