package cn.oa.workflow.core.engine;

import cn.oa.workflow.model.constant.WorkflowConstants;
import cn.oa.workflow.model.entity.WfInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 流程实例状态机
 * 定义并控制流程实例的状态转换规则
 */
@Slf4j
@Component
public class InstanceStateMachine {

    /**
     * 状态转换规则表
     * Key: 当前状态, Value: 允许转换的目标状态集合
     */
    private static final Map<String, Set<String>> TRANSITION_RULES = new LinkedHashMap<>();

    static {
        // DRAFT -> RUNNING, ABORTED
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_DRAFT,
                Set.of(WorkflowConstants.INSTANCE_STATUS_RUNNING,
                       WorkflowConstants.INSTANCE_STATUS_ABORTED));

        // RUNNING -> SUSPENDED, ABORTED, PASSED, REJECTED, REVOKED
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_RUNNING,
                Set.of(WorkflowConstants.INSTANCE_STATUS_SUSPENDED,
                       WorkflowConstants.INSTANCE_STATUS_ABORTED,
                       WorkflowConstants.INSTANCE_STATUS_PASSED,
                       WorkflowConstants.INSTANCE_STATUS_REJECTED,
                       WorkflowConstants.INSTANCE_STATUS_REVOKED));

        // SUSPENDED -> RUNNING, ABORTED
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_SUSPENDED,
                Set.of(WorkflowConstants.INSTANCE_STATUS_RUNNING,
                       WorkflowConstants.INSTANCE_STATUS_ABORTED));

        // 终态不能转换: ABORTED, PASSED, REJECTED, REVOKED
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_ABORTED, Collections.emptySet());
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_PASSED, Collections.emptySet());
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_REJECTED, Collections.emptySet());
        TRANSITION_RULES.put(WorkflowConstants.INSTANCE_STATUS_REVOKED, Collections.emptySet());
    }

    /**
     * 检查状态转换是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 是否允许转换
     */
    public boolean canTransition(String from, String to) {
        if (from == null || to == null) {
            log.warn("State transition check failed: from={}, to={}", from, to);
            return false;
        }

        // 相同状态不需要转换
        if (from.equals(to)) {
            return true;
        }

        Set<String> allowedTargets = TRANSITION_RULES.get(from);
        if (allowedTargets == null) {
            log.warn("Unknown source state: {}", from);
            return false;
        }

        boolean allowed = allowedTargets.contains(to);
        if (!allowed) {
            log.warn("Invalid state transition: {} -> {} is not allowed", from, to);
        }
        return allowed;
    }

    /**
     * 执行状态转换
     *
     * @param instance     流程实例
     * @param targetStatus 目标状态
     * @throws IllegalStateException 如果状态转换非法
     */
    public void transit(WfInstance instance, String targetStatus) {
        String currentStatus = instance.getStatus();

        if (!canTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                    String.format("非法状态转换: %s -> %s (实例ID: %d)",
                            currentStatus, targetStatus, instance.getId()));
        }

        log.info("Instance state transition: instanceId={}, {} -> {}",
                instance.getId(), currentStatus, targetStatus);
        instance.setStatus(targetStatus);
    }

    /**
     * 获取当前状态允许的所有目标状态
     *
     * @param currentStatus 当前状态
     * @return 允许转换的目标状态集合
     */
    public Set<String> getAllowedTransitions(String currentStatus) {
        return TRANSITION_RULES.getOrDefault(currentStatus, Collections.emptySet());
    }

    /**
     * 判断是否为终态
     *
     * @param status 状态
     * @return 是否为终态
     */
    public boolean isFinalState(String status) {
        Set<String> allowed = TRANSITION_RULES.get(status);
        return allowed != null && allowed.isEmpty();
    }

    /**
     * 判断是否可以执行审批操作
     *
     * @param instance 流程实例
     * @return 是否可以审批
     */
    public boolean canApprove(WfInstance instance) {
        return WorkflowConstants.INSTANCE_STATUS_RUNNING.equals(instance.getStatus());
    }

    /**
     * 判断是否可以挂起
     *
     * @param instance 流程实例
     * @return 是否可以挂起
     */
    public boolean canSuspend(WfInstance instance) {
        return canTransition(instance.getStatus(), WorkflowConstants.INSTANCE_STATUS_SUSPENDED);
    }

    /**
     * 判断是否可以恢复
     *
     * @param instance 流程实例
     * @return 是否可以恢复
     */
    public boolean canResume(WfInstance instance) {
        return WorkflowConstants.INSTANCE_STATUS_SUSPENDED.equals(instance.getStatus());
    }

    /**
     * 判断是否可以终止
     *
     * @param instance 流程实例
     * @return 是否可以终止
     */
    public boolean canAbort(WfInstance instance) {
        return canTransition(instance.getStatus(), WorkflowConstants.INSTANCE_STATUS_ABORTED);
    }

    /**
     * 判断是否可以撤回
     *
     * @param instance 流程实例
     * @return 是否可以撤回
     */
    public boolean canWithdraw(WfInstance instance) {
        return canTransition(instance.getStatus(), WorkflowConstants.INSTANCE_STATUS_REVOKED);
    }
}
