package cn.oa.workflow.engine;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.entity.*;
import cn.oa.workflow.mapper.*;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流引擎核心.
 *
 * <p>提供两个主要操作:
 * <ul>
 *   <li>startProcess - 启动流程: 创建实例 + 创建第一个任务 (从 START 节点往后找第一个 APPROVAL 节点)</li>
 *   <li>approve - 审批任务: 更新任务状态 + 创建下一节点任务 + 写历史</li>
 * </ul>
 */
@Component
public class WfEngine {

    private static final Logger log = LoggerFactory.getLogger(WfEngine.class);

    private static final String STATUS_RUNNING   = "RUNNING";
    private static final String STATUS_APPROVED  = "APPROVED";
    private static final String STATUS_REJECTED  = "REJECTED";

    private static final String NODE_START    = "START";
    private static final String NODE_APPROVAL = "APPROVAL";
    private static final String NODE_END      = "END";

    private static final String TASK_PENDING   = "PENDING";
    private static final String TASK_APPROVED  = "APPROVED";
    private static final String TASK_REJECTED  = "REJECTED";

    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT  = "REJECT";
    private static final String ACTION_START   = "START";
    private static final String ACTION_END     = "END";

    private final WfDefinitionMapper definitionMapper;
    private final WfNodeMapper nodeMapper;
    private final WfTransitionMapper transitionMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfRecordMapper recordMapper;
    private final WfAssigneeResolver assigneeResolver;
    private final ApplicationEventPublisher eventPublisher;

    public WfEngine(WfDefinitionMapper definitionMapper,
                    WfNodeMapper nodeMapper,
                    WfTransitionMapper transitionMapper,
                    WfInstanceMapper instanceMapper,
                    WfTaskMapper taskMapper,
                    WfRecordMapper recordMapper,
                    WfAssigneeResolver assigneeResolver,
                    ApplicationEventPublisher eventPublisher) {
        this.definitionMapper = definitionMapper;
        this.nodeMapper = nodeMapper;
        this.transitionMapper = transitionMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.assigneeResolver = assigneeResolver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 启动流程.
     */
    @Transactional
    public Long startProcess(String defKey, String businessKey, Long initiator) {
        WfDefinition def = definitionMapper.findActiveByKey(defKey);
        if (def == null) {
            throw new BizException(RCode.NOT_FOUND, "流程定义不存在: " + defKey);
        }
        if (instanceMapper.findRunningByBusinessKey(businessKey) != null) {
            throw new BizException(RCode.BAD_REQUEST, "该业务单据已存在进行中的流程: " + businessKey);
        }

        // 1. 找 START 节点的下一节点 (第一个 APPROVAL 节点)
        List<WfNode> nodes = nodeMapper.findByDefId(def.getId());
        WfNode startNode = nodes.stream()
                .filter(n -> NODE_START.equals(n.getNodeType()))
                .findFirst()
                .orElseThrow(() -> new BizException(RCode.BAD_REQUEST, "流程定义无 START 节点"));

        List<WfTransition> starts = transitionMapper.findByFromNodeAndAction(startNode.getId(), ACTION_APPROVE);
        if (starts.isEmpty()) {
            throw new BizException(RCode.BAD_REQUEST, "START 节点无 APPROVE 流转");
        }
        WfNode firstApproval = nodeMapper.selectById(starts.get(0).getToNodeId());
        if (firstApproval == null) {
            throw new BizException(RCode.NOT_FOUND, "下一节点不存在");
        }

        // 2. 创建 instance
        WfInstance instance = new WfInstance();
        instance.setDefId(def.getId());
        instance.setDefKey(defKey);
        instance.setBusinessKey(businessKey);
        instance.setInitiatorId(initiator);
        instance.setStatus(STATUS_RUNNING);
        instance.setCurrentNodeId(firstApproval.getId());
        instance.setStartTime(LocalDateTime.now());
        instance.setDelFlag("0");
        instanceMapper.insert(instance);

        // 3. 创建第一个 task
        Long assignee = assigneeResolver.resolve(def.getId(), firstApproval.getNodeKey(), initiator);
        WfTask task = new WfTask();
        task.setInstanceId(instance.getId());
        task.setNodeId(firstApproval.getId());
        task.setAssigneeId(assignee);
        task.setStatus(TASK_PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setDelFlag("0");
        taskMapper.insert(task);

        // 4. 写历史
        saveRecord(instance.getId(), null, initiator, ACTION_START, "流程启动");

        log.info("Workflow started: defKey={}, instanceId={}, businessKey={}, firstAssignee={}",
                defKey, instance.getId(), businessKey, assignee);
        return instance.getId();
    }

    /**
     * 审批任务.
     */
    @Transactional
    public void approve(Long taskId, Long actionEmpId, String action, String comment) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(RCode.NOT_FOUND, "任务不存在: " + taskId);
        }
        if (!TASK_PENDING.equals(task.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "任务已处理: status=" + task.getStatus());
        }
        WfInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null || !STATUS_RUNNING.equals(instance.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "流程不在运行中");
        }

        // 1. 更新任务状态
        task.setStatus(ACTION_REJECT.equals(action) ? TASK_REJECTED : TASK_APPROVED);
        task.setAction(action);
        task.setActionTime(LocalDateTime.now());
        task.setActionEmpId(actionEmpId);
        task.setComment(comment);
        taskMapper.updateById(task);

        // 2. 写历史
        saveRecord(instance.getId(), task.getNodeId(), actionEmpId, action, comment);

        if (ACTION_REJECT.equals(action)) {
            // 拒绝: 流程结束
            instance.setStatus(STATUS_REJECTED);
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.updateById(instance);
            saveRecord(instance.getId(), task.getNodeId(), actionEmpId, ACTION_END, "流程被拒绝");
            eventPublisher.publishEvent(new WfInstanceCompletedEvent(
                    instance.getId(), STATUS_REJECTED, instance.getBusinessKey()));
            log.info("Workflow rejected: instanceId={}, taskId={}", instance.getId(), taskId);
            return;
        }

        // 3. 找下一节点
        List<WfTransition> transitions = transitionMapper.findByFromNodeAndAction(task.getNodeId(), action);
        if (transitions.isEmpty()) {
            throw new BizException(RCode.BAD_REQUEST, "当前节点无 APPROVE 流转");
        }
        WfNode nextNode = nodeMapper.selectById(transitions.get(0).getToNodeId());
        if (nextNode == null) {
            throw new BizException(RCode.NOT_FOUND, "下一节点不存在");
        }

        if (NODE_END.equals(nextNode.getNodeType())) {
            // 流程结束
            instance.setStatus(STATUS_APPROVED);
            instance.setEndTime(LocalDateTime.now());
            instance.setCurrentNodeId(nextNode.getId());
            instanceMapper.updateById(instance);
            saveRecord(instance.getId(), nextNode.getId(), actionEmpId, ACTION_END, "流程通过");
            eventPublisher.publishEvent(new WfInstanceCompletedEvent(
                    instance.getId(), STATUS_APPROVED, instance.getBusinessKey()));
            log.info("Workflow approved: instanceId={}", instance.getId());
            return;
        }

        // 4. 创建下一节点 task
        Long nextAssignee = assigneeResolver.resolve(instance.getDefId(), nextNode.getNodeKey(), instance.getInitiatorId());
        WfTask nextTask = new WfTask();
        nextTask.setInstanceId(instance.getId());
        nextTask.setNodeId(nextNode.getId());
        nextTask.setAssigneeId(nextAssignee);
        nextTask.setStatus(TASK_PENDING);
        nextTask.setCreateTime(LocalDateTime.now());
        nextTask.setDelFlag("0");
        taskMapper.insert(nextTask);

        instance.setCurrentNodeId(nextNode.getId());
        instanceMapper.updateById(instance);
        log.info("Workflow advanced: instanceId={}, fromNode={}, toNode={}, nextAssignee={}",
                instance.getId(), task.getNodeId(), nextNode.getId(), nextAssignee);
    }

    private void saveRecord(Long instanceId, Long nodeId, Long empId, String action, String comment) {
        WfRecord record = new WfRecord();
        record.setInstanceId(instanceId);
        record.setNodeId(nodeId);
        record.setEmpId(empId);
        record.setAction(action);
        record.setComment(comment);
        record.setActionTime(LocalDateTime.now());
        recordMapper.insert(record);
    }
}
