package cn.oa.workflow.core.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 工作流事件发布器
 * 使用 Spring 事件机制发布工作流相关事件
 */
@Slf4j
@Component
public class WorkflowEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public WorkflowEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布流程实例启动事件
     */
    public void publishInstanceStarted(cn.oa.workflow.model.entity.WfInstance instance) {
        log.info("Publishing InstanceStartedEvent: instanceId={}, businessType={}, businessId={}",
                instance.getId(), instance.getBusinessType(), instance.getBusinessId());
        eventPublisher.publishEvent(new InstanceStartedEvent(instance));
    }

    /**
     * 发布任务创建事件
     */
    public void publishTaskCreated(cn.oa.workflow.model.entity.WfTask task, String nodeName) {
        log.info("Publishing TaskCreatedEvent: taskId={}, instanceId={}, assigneeId={}",
                task.getId(), task.getInstanceId(), task.getAssigneeId());
        eventPublisher.publishEvent(new TaskCreatedEvent(task, nodeName));
    }

    /**
     * 发布任务完成事件
     */
    public void publishTaskCompleted(cn.oa.workflow.model.entity.WfTask task) {
        log.info("Publishing TaskCompletedEvent: taskId={}, instanceId={}, result={}",
                task.getId(), task.getInstanceId(), task.getStatus());
        eventPublisher.publishEvent(new TaskCompletedEvent(task));
    }

    /**
     * 发布流程实例完成事件
     */
    public void publishInstanceCompleted(cn.oa.workflow.model.entity.WfInstance instance) {
        log.info("Publishing InstanceCompletedEvent: instanceId={}, status={}",
                instance.getId(), instance.getStatus());
        eventPublisher.publishEvent(new InstanceCompletedEvent(instance));
    }
}
