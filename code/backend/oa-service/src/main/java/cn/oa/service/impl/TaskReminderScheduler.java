package cn.oa.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
import cn.oa.mapper.WfProcessInstanceMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.service.NotificationService;
import cn.oa.service.TodoService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class TaskReminderScheduler {

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

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkOverdueTasks() {
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getStatus, "0")
                .isNotNull(WfTask::getDeadline)
                .le(WfTask::getDeadline, LocalDateTime.now());
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
        log.info("任务超时: taskId={}, nodeIndex={}, action={}", task.getId(), task.getNodeIndex(), timeoutAction);

        int count = task.getRemindCount() != null ? task.getRemindCount() : 0;
        task.setRemindCount(count + 1);
        task.setLastRemindTime(LocalDateTime.now());

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

        JSONArray nodes = JSONUtil.parseArray(definition.getNodeConfig());
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (node.getInt("nodeIndex", -1) == task.getNodeIndex()) {
                return node.getStr("timeoutAction", "notify_only");
            }
        }
        return "notify_only";
    }

    private void escalateTask(WfTask task) {
        // Transfer to dept manager as escalation
        notificationService.notifyTask(task.getAssigneeId(), "", task.getId(),
                "审批任务因超时已上报处理");
    }
}
