package cn.oa.service.impl;

import cn.hutool.json.JSONObject;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
import cn.oa.mapper.WfProcessInstanceMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.service.NotificationService;
import cn.oa.service.TodoService;
import cn.oa.service.WorkflowService;
import cn.oa.service.workflow.WorkflowGraph;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TaskReminderScheduler {

    /** V1010: cap the number of auto-escalations per task to prevent infinite loops. */
    private static final int MAX_ESCALATIONS = 3;

    @Autowired
    private WfTaskMapper taskMapper;

    @Autowired
    private WfProcessInstanceMapper instanceMapper;

    @Lazy
    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TodoService todoService;

    @Scheduled(initialDelay = 60000, fixedRate = 300000) // start after schema initialization, then every 5 minutes
    public void checkOverdueTasks() {
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getStatus, "0")
                .isNotNull(WfTask::getDueTime)
                .le(WfTask::getDueTime, LocalDateTime.now());
        List<WfTask> overdueTasks = taskMapper.selectList(wrapper);

        for (WfTask task : overdueTasks) {
            try {
                handleOverdueTask(task);
            } catch (Exception e) {
                log.error("处理超时任务失败: taskId={}", task.getId(), e);
            }
        }
    }

    private void handleOverdueTask(WfTask task) {
        String timeoutAction = resolveTimeoutAction(task);
        log.info("任务超时: taskId={}, nodeId={}, action={}", task.getId(), task.getNodeId(), timeoutAction);

        int count = task.getRemindCount() != null ? task.getRemindCount() : 0;
        task.setRemindCount(count + 1);

        switch (timeoutAction) {
            case "auto_approve":
                taskMapper.updateById(task);
                workflowService.handleTask(task.getId(), task.getAssigneeId(), 1, "系统自动通过（超时）");
                notificationService.notifyApproval(task.getAssigneeId(), "系统", task.getId(),
                        "auto_approved", "审批任务因超时已自动通过");
                break;
            case "auto_reject":
                taskMapper.updateById(task);
                workflowService.handleTask(task.getId(), task.getAssigneeId(), 2, "系统自动驳回（超时）");
                break;
            case "escalate":
                taskMapper.updateById(task);
                escalateTask(task);
                break;
            default: // notify_only
                taskMapper.updateById(task);
                todoService.addTodo(task.getAssigneeId(), "催办提醒: 审批任务超时", "approval", task.getInstanceId(), "");
                notificationService.notifyTask(task.getAssigneeId(), "", task.getId(),
                        "您有一个审批任务已超时，请尽快处理");
                break;
        }
    }

    private String resolveTimeoutAction(WfTask task) {
        WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) return "notify_only";

        WfProcessDefinition definition = workflowService.getById(instance.getProcessId());
        if (definition == null || definition.getNodeConfig() == null) return "notify_only";

        String nodeConfig = definition.getNodeConfig();
        String targetNodeId = task.getNodeId() == null ? null : String.valueOf(task.getNodeId());
        int runtimeIndex = task.getNodeId() == null ? -1 : task.getNodeId().intValue();

        WorkflowGraph graph = workflowService.parseNodeConfig(nodeConfig);
        if (graph.isGraph() && graph.valid) {
            JSONObject graphNode = findGraphNodeForRuntimeTask(graph, runtimeIndex);
            if (graphNode != null) return graphNode.getStr("timeoutAction", "notify_only");
            String id = targetNodeId;
            if (id != null && graph.nodes.containsKey(id)) return graph.nodes.get(id).getStr("timeoutAction", "notify_only");
            return "notify_only";
        }
        return "notify_only";
    }

    /**
     * V1010: actually transfer the task to an escalation target rather than
     * just notify. Workflow:
     *   1. Mark current task as status=3 (transferred) and increment escalationCount.
     *   2. Resolve the new assignee from the node's {@code escalateTo} config.
     *   3. Insert a new WfTask row with the same context but the new assignee and status=0.
     *   4. Notify the old and new approvers.
     *
     * <p>If {@code escalationCount} already hit {@link #MAX_ESCALATIONS}, stop escalating
     * and fall back to a notification + ADMIN CC.
     */
    private void escalateTask(WfTask task) {
        int currentEscalations = Optional.ofNullable(task.getEscalationCount()).orElse(0);
        if (currentEscalations >= MAX_ESCALATIONS) {
            log.warn("任务 {} 已升级 {} 次，停止升级，改为通知 ADMIN", task.getId(), currentEscalations);
            notificationService.notifyTask(task.getAssigneeId(), "system", task.getInstanceId(),
                    "审批任务已多次升级，请尽快处理或联系管理员");
            return;
        }

        WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) return;
        WfProcessDefinition definition = workflowService.getById(instance.getProcessId());
        if (definition == null) return;

        JSONObject currentNode = findNodeForTask(definition, task);
        JSONObject escalateTo = currentNode == null ? null : currentNode.getJSONObject("escalateTo");
        if (escalateTo == null) {
            // Default: escalate to ADMIN
            escalateTo = new JSONObject();
            escalateTo.set("type", "role_global");
            escalateTo.set("value", "ADMIN");
            log.info("任务 {} 节点未配置 escalateTo，默认升级到 ADMIN", task.getId());
        }

        Long newAssigneeId;
        try {
            newAssigneeId = workflowService.resolveAssigneeForEscalation(
                    escalateTo.getStr("type"), escalateTo.getStr("value"), task.getAssigneeId());
        } catch (Exception e) {
            log.error("升级任务 {} 解析 escalateTo 失败: {}", task.getId(), e.getMessage());
            return;
        }
        if (newAssigneeId == null || newAssigneeId.equals(task.getAssigneeId())) {
            log.warn("升级任务 {} 未找到更高级别审批人，跳过升级", task.getId());
            return;
        }

        // Mark old task as transferred
        task.setStatus("3"); // transferred
        task.setEscalationCount(currentEscalations + 1);
        task.setCompleteTime(LocalDateTime.now());
        task.setOpinion("系统自动升级（超时）");
        taskMapper.updateById(task);

        // Create new task for escalation target
        WfTask newTask = new WfTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeName(task.getNodeName());
        newTask.setAssigneeId(newAssigneeId);
        newTask.setStatus("0"); // pending
        newTask.setTaskType("TODO");
        newTask.setOpinion(null);
        newTask.setDueTime(null);
        newTask.setEscalationCount(0);
        newTask.setCreateTime(LocalDateTime.now());
        taskMapper.insert(newTask);

        // Notifications
        notificationService.notifyTask(newAssigneeId, instance.getBusinessType(),
                instance.getBusinessId(), "审批任务因超时已升级到您，请尽快处理");
        notificationService.notifyTask(task.getAssigneeId(), instance.getBusinessType(),
                instance.getBusinessId(), "您负责的审批任务因超时已自动升级");

        log.info("任务 {} 已升级: 原审批人={} → 新审批人={}, 升级次数={}",
                task.getId(), task.getAssigneeId(), newAssigneeId, currentEscalations + 1);
    }

    /** Locate the graph node config object for a given task. */
    private JSONObject findNodeForTask(WfProcessDefinition definition, WfTask task) {
        String nodeConfig = definition.getNodeConfig();
        if (nodeConfig == null) return null;

        WorkflowGraph graph = workflowService.parseNodeConfig(nodeConfig);
        if (graph.isGraph() && graph.valid) {
            String nodeId = task.getNodeId() == null ? null : String.valueOf(task.getNodeId());
            JSONObject graphNode = findGraphNodeForRuntimeTask(graph, task.getNodeId() == null ? -1 : task.getNodeId().intValue());
            if (graphNode != null) return graphNode;
            return nodeId == null ? null : graph.nodes.get(nodeId);
        }
        return null;
    }

    private JSONObject findGraphNodeForRuntimeTask(WorkflowGraph graph, int runtimeNodeIndex) {
        if (graph == null || runtimeNodeIndex < 0) return null;
        int index = 0;
        for (JSONObject node : graph.nodes.values()) {
            if (!"approval".equals(node.getStr("nodeType"))) continue;
            if (index == runtimeNodeIndex) return node;
            index++;
        }
        return null;
    }
}
