package cn.oa.workflow.core.engine;

import cn.hutool.json.JSONUtil;
import cn.oa.workflow.core.engine.InstanceStateMachine;
import cn.oa.workflow.core.event.WorkflowEventPublisher;
import cn.oa.workflow.core.handler.SignHandler;
import cn.oa.workflow.core.parser.ExpressionParser;
import cn.oa.workflow.core.resolver.AssigneeResolver;
import cn.oa.workflow.core.resolver.AssigneeResolverChain;
import cn.oa.workflow.mapper.*;
import cn.oa.workflow.model.constant.WorkflowConstants;
import cn.oa.workflow.model.dto.HandleTaskDTO;
import cn.oa.workflow.model.dto.StartProcessDTO;
import cn.oa.workflow.model.dto.TransferTaskDTO;
import cn.oa.workflow.model.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工作流引擎实现类
 * 重构后的标准实现，依赖状态机和事件发布器
 */
@Slf4j
@Service
public class WorkflowEngineImpl implements IWorkflowEngine {

    private final WfDefinitionMapper definitionMapper;
    private final WfNodeMapper nodeMapper;
    private final WfTransitionMapper transitionMapper;
    private final WfAssigneeRuleMapper assigneeRuleMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfRecordMapper recordMapper;
    private final WfDelegationMapper delegationMapper;
    private final StringRedisTemplate redisTemplate;

    private final InstanceStateMachine stateMachine;
    private final WorkflowEventPublisher eventPublisher;
    private final SignHandler signHandler;
    private final ExpressionParser expressionParser;
    private final AssigneeResolverChain resolverChain;

    public WorkflowEngineImpl(
            WfDefinitionMapper definitionMapper,
            WfNodeMapper nodeMapper,
            WfTransitionMapper transitionMapper,
            WfAssigneeRuleMapper assigneeRuleMapper,
            WfInstanceMapper instanceMapper,
            WfTaskMapper taskMapper,
            WfRecordMapper recordMapper,
            WfDelegationMapper delegationMapper,
            StringRedisTemplate redisTemplate,
            InstanceStateMachine stateMachine,
            WorkflowEventPublisher eventPublisher,
            SignHandler signHandler,
            ExpressionParser expressionParser,
            AssigneeResolverChain resolverChain) {
        this.definitionMapper = definitionMapper;
        this.nodeMapper = nodeMapper;
        this.transitionMapper = transitionMapper;
        this.assigneeRuleMapper = assigneeRuleMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.delegationMapper = delegationMapper;
        this.redisTemplate = redisTemplate;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.signHandler = signHandler;
        this.expressionParser = expressionParser;
        this.resolverChain = resolverChain;
    }

    // ==================== 流程启动 ====================

    @Override
    @Transactional
    public Long startWorkflow(StartProcessDTO dto) {
        // 从上下文获取当前用户作为发起人（实际应从 SecurityContext 获取）
        Long starterId = 1L; // TODO: 从安全上下文获取
        WfInstance instance = startProcess(dto.getBusinessType(), dto.getBusinessId(), starterId,
                dto.getConditionContext(), dto.getFormData());
        return instance.getId();
    }

    @Override
    @Transactional
    public WfInstance startProcess(String businessType, Long businessId, Long starterId,
                                   Map<String, Object> conditionContext, Map<String, Object> formData) {
        log.info("Starting process: businessType={}, businessId={}, starterId={}",
                businessType, businessId, starterId);

        // 1. 查找最新已发布流程定义
        WfDefinition definition = findPublishedDefinition(businessType);
        if (definition == null) {
            log.info("No published definition found for {}, auto-approving", businessType);
            return autoApprove(businessType, businessId, starterId);
        }

        // 2. 加载节点
        List<WfNode> nodes = nodeMapper.selectByDefId(definition.getId());
        if (nodes.isEmpty()) {
            log.info("No nodes found for definition {}, auto-approving", definition.getId());
            return autoApprove(businessType, businessId, starterId);
        }

        // 3. 创建实例
        WfInstance instance = createInstance(definition, businessType, businessId, starterId,
                conditionContext, formData);

        // 发布实例启动事件
        eventPublisher.publishInstanceStarted(instance);

        // 4. 找到第一个审批节点并创建任务
        List<WfNode> applicableNodes = filterApplicableNodes(nodes, conditionContext);
        WfNode firstNode = findFirstApprovalNode(applicableNodes);

        if (firstNode != null) {
            createTasksForNode(instance, firstNode, starterId, formData);
        } else {
            // 没有审批节点，直接通过
            completeInstance(instance, WorkflowConstants.INSTANCE_STATUS_PASSED);
        }

        return instance;
    }

    // ==================== 任务处理 ====================

    @Override
    @Transactional
    public void approveTask(Long taskId, HandleTaskDTO dto) {
        handleTask(taskId, dto, WorkflowConstants.TASK_STATUS_APPROVED);
    }

    @Override
    @Transactional
    public void rejectTask(Long taskId, HandleTaskDTO dto) {
        handleTask(taskId, dto, WorkflowConstants.TASK_STATUS_REJECTED);
    }

    private void handleTask(Long taskId, HandleTaskDTO dto, String result) {
        Long handlerId = 1L; // TODO: 从安全上下文获取

        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (!WorkflowConstants.TASK_STATUS_PENDING.equals(task.getStatus())) {
            throw new RuntimeException("任务已处理，当前状态: " + task.getStatus());
        }

        // 获取分布式锁（会签场景）
        String lockKey = "lock:workflow:task:" + (task.getParentTaskId() != null ? task.getParentTaskId() : taskId);
        String lockValue = UUID.randomUUID().toString();

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            // 授权检查
            if (!task.getAssigneeId().equals(handlerId)) {
                if (!isAuthorizedDelegate(handlerId, task.getAssigneeId())) {
                    throw new RuntimeException("无权处理此任务");
                }
            }

            WfInstance instance = instanceMapper.selectById(task.getInstanceId());
            if (!stateMachine.canApprove(instance)) {
                throw new RuntimeException("流程当前状态不允许审批");
            }

            // 更新任务状态
            task.setStatus(result);
            task.setEndTime(LocalDateTime.now());
            task.setOpinion(dto.getOpinion());
            task.setSignature(dto.getSignature());
            taskMapper.updateById(task);

            // 发布任务完成事件
            eventPublisher.publishTaskCompleted(task);

            // 创建审批记录
            createRecord(instance.getId(), taskId, handlerId, result, task.getNodeId(), null, dto.getOpinion());

            if (WorkflowConstants.TASK_STATUS_REJECTED.equals(result)) {
                handleRejection(task, instance);
            } else {
                handleApproval(task, instance);
            }

        } finally {
            String currentVal = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentVal)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    private void handleApproval(WfTask task, WfInstance instance) {
        // 检查会签
        if (task.getParentTaskId() != null) {
            AggregationResult agg = signHandler.evaluateSign(task.getParentTaskId(), instance.getId());
            if (agg.isWaiting()) {
                return;
            }
            if (agg.isRejected()) {
                completeInstance(instance, WorkflowConstants.INSTANCE_STATUS_REJECTED);
                return;
            }
        }

        // 流转到下一节点
        advanceToNextNode(task, instance);
    }

    private void handleRejection(WfTask task, WfInstance instance) {
        // 取消所有待处理任务
        taskMapper.cancelPendingByInstanceId(instance.getId());

        // 更新实例状态为驳回
        completeInstance(instance, WorkflowConstants.INSTANCE_STATUS_REJECTED);
    }

    // ==================== 转办 ====================

    @Override
    @Transactional
    public void transferTask(Long taskId, TransferTaskDTO dto) {
        Long fromAssigneeId = 1L; // TODO: 从安全上下文获取

        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if (!task.getAssigneeId().equals(fromAssigneeId)) {
            throw new RuntimeException("无权转办");
        }

        task.setStatus(WorkflowConstants.TASK_STATUS_TRANSFERRED);
        task.setEndTime(LocalDateTime.now());
        task.setOpinion("转办给用户" + dto.getToAssigneeId() +
                (dto.getReason() != null ? ": " + dto.getReason() : ""));
        taskMapper.updateById(task);

        // 创建新任务
        WfNode node = nodeMapper.selectById(task.getNodeId());
        WfTask newTask = buildTask(task.getInstanceId(), task.getNodeId(),
                dto.getToAssigneeId(), task.getAssigneeId(),
                WorkflowConstants.TASK_TYPE_TODO, null, node);
        taskMapper.insert(newTask);

        // 发布任务创建事件
        eventPublisher.publishTaskCreated(newTask, node.getNodeName());

        // 创建转办记录
        createRecord(task.getInstanceId(), taskId, fromAssigneeId,
                WorkflowConstants.ACTION_TRANSFER, task.getNodeId(), task.getNodeId(), dto.getReason());
    }

    // ==================== 撤回 ====================

    @Override
    @Transactional
    public void withdrawInstance(Long instanceId, Long initiatorId) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!stateMachine.canWithdraw(instance)) {
            throw new RuntimeException("流程当前状态不允许撤回");
        }

        if (!instance.getStarterId().equals(initiatorId)) {
            throw new RuntimeException("只有申请人才能撤回");
        }

        // 取消所有待处理任务
        taskMapper.cancelPendingByInstanceId(instanceId);

        // 更新实例状态
        stateMachine.transit(instance, WorkflowConstants.INSTANCE_STATUS_REVOKED);
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // 发布实例完成事件
        eventPublisher.publishInstanceCompleted(instance);

        // 创建撤回记录
        createRecord(instanceId, null, initiatorId,
                WorkflowConstants.ACTION_WITHDRAW, null, null, "申请人撤回");

        log.info("Instance {} withdrawn by starter {}", instanceId, initiatorId);
    }

    // ==================== 挂起/恢复 ====================

    @Override
    @Transactional
    public void suspendInstance(Long instanceId, String reason) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!stateMachine.canSuspend(instance)) {
            throw new RuntimeException("流程当前状态不允许挂起");
        }

        stateMachine.transit(instance, WorkflowConstants.INSTANCE_STATUS_SUSPENDED);
        instance.setSuspendTime(LocalDateTime.now());
        instance.setSuspendReason(reason);
        instanceMapper.updateById(instance);

        createRecord(instanceId, null, null,
                WorkflowConstants.ACTION_SUSPEND, null, null, reason);

        log.info("Instance {} suspended: {}", instanceId, reason);
    }

    @Override
    @Transactional
    public void resumeInstance(Long instanceId) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!stateMachine.canResume(instance)) {
            throw new RuntimeException("流程当前状态不允许恢复");
        }

        stateMachine.transit(instance, WorkflowConstants.INSTANCE_STATUS_RUNNING);
        instance.setSuspendTime(null);
        instance.setSuspendReason(null);
        instanceMapper.updateById(instance);

        createRecord(instanceId, null, null,
                WorkflowConstants.ACTION_RESUME, null, null, null);

        log.info("Instance {} resumed", instanceId);
    }

    // ==================== 终止 ====================

    @Override
    @Transactional
    public void abortInstance(Long instanceId, String reason) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        if (!stateMachine.canAbort(instance)) {
            throw new RuntimeException("流程当前状态不允许终止");
        }

        // 取消所有待处理任务
        taskMapper.cancelPendingByInstanceId(instanceId);

        stateMachine.transit(instance, WorkflowConstants.INSTANCE_STATUS_ABORTED);
        instance.setEndTime(LocalDateTime.now());
        instance.setAbortReason(reason);
        instanceMapper.updateById(instance);

        // 发布实例完成事件
        eventPublisher.publishInstanceCompleted(instance);

        createRecord(instanceId, null, null,
                WorkflowConstants.ACTION_ABORT, null, null, reason);

        log.info("Instance {} aborted: {}", instanceId, reason);
    }

    // ==================== 催办 ====================

    @Override
    public void urgeTask(Long instanceId, Long initiatorId) {
        List<WfTask> pendingTasks = taskMapper.selectPendingByInstanceId(instanceId);
        for (WfTask task : pendingTasks) {
            task.setRemindCount((task.getRemindCount() != null ? task.getRemindCount() : 0) + 1);
            task.setLastRemindTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
        createRecord(instanceId, null, initiatorId,
                WorkflowConstants.ACTION_URGE, null, null, "催办提醒");

        log.info("Urged tasks for instance {}, {} tasks notified", instanceId, pendingTasks.size());
    }

    // ==================== 查询方法 ====================

    @Override
    public List<WfRecord> getRecords(Long instanceId) {
        return recordMapper.selectByInstanceId(instanceId);
    }

    @Override
    public List<WfTask> getApprovalHistory(Long instanceId) {
        return taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfTask>()
                        .eq(WfTask::getInstanceId, instanceId)
                        .orderByAsc(WfTask::getCreatedAt));
    }

    // ==================== 内部方法 ====================

    private WfDefinition findPublishedDefinition(String businessType) {
        return definitionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WfDefinition>()
                        .eq(WfDefinition::getCode, businessType)
                        .eq(WfDefinition::getStatus, WorkflowConstants.DEF_STATUS_PUBLISHED)
                        .orderByDesc(WfDefinition::getVersion)
                        .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
    }

    private WfInstance autoApprove(String businessType, Long businessId, Long starterId) {
        WfInstance instance = new WfInstance();
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setStarterId(starterId);
        instance.setStatus(WorkflowConstants.INSTANCE_STATUS_PASSED);
        instance.setStartTime(LocalDateTime.now());
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.insert(instance);

        eventPublisher.publishInstanceStarted(instance);
        eventPublisher.publishInstanceCompleted(instance);

        return instance;
    }

    private WfInstance createInstance(WfDefinition definition, String businessType, Long businessId,
                                     Long starterId, Map<String, Object> ctx, Map<String, Object> formData) {
        WfInstance instance = new WfInstance();
        instance.setDefId(definition.getId());
        instance.setDefVersion(definition.getVersion());
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setStarterId(starterId);
        instance.setStatus(WorkflowConstants.INSTANCE_STATUS_RUNNING);
        instance.setStartTime(LocalDateTime.now());

        // 快照整个流程定义
        Map<String, Object> snapshot = new LinkedHashMap<>();
        List<WfNode> nodes = nodeMapper.selectByDefId(definition.getId());
        snapshot.put("definition", definition);
        snapshot.put("nodes", nodes);
        instance.setDefSnapshot(JSONUtil.toJsonStr(snapshot));

        if (ctx != null && !ctx.isEmpty()) {
            instance.setConditionContext(JSONUtil.toJsonStr(ctx));
        }
        if (formData != null && !formData.isEmpty()) {
            instance.setFormDataSnapshot(JSONUtil.toJsonStr(formData));
        }
        instanceMapper.insert(instance);
        return instance;
    }

    private List<WfNode> filterApplicableNodes(List<WfNode> nodes, Map<String, Object> ctx) {
        if (ctx == null || ctx.isEmpty()) return nodes;
        return nodes.stream()
                .filter(n -> {
                    List<WfTransition> transitions = transitionMapper.selectByFromNodeId(n.getId());
                    if (transitions.isEmpty()) return true;
                    return transitions.stream().anyMatch(t ->
                            expressionParser.evaluate(t.getExpression(), ctx));
                })
                .collect(Collectors.toList());
    }

    private WfNode findFirstApprovalNode(List<WfNode> nodes) {
        return nodes.stream()
                .filter(n -> WorkflowConstants.NODE_TYPE_APPROVAL.equals(n.getNodeType())
                        || WorkflowConstants.NODE_TYPE_START.equals(n.getNodeType()))
                .findFirst()
                .orElse(null);
    }

    void createTasksForNode(WfInstance instance, WfNode node, Long starterId, Map<String, Object> formData) {
        String formDataJson = instance.getFormDataSnapshot();

        // 1. 解析审批人
        List<Long> assigneeIds = resolveAssignees(node, starterId, formDataJson);

        // 2. 审批人为空处理
        if (assigneeIds.isEmpty()) {
            String strategy = node.getEmptyAssigneeStrategy() != null ? node.getEmptyAssigneeStrategy()
                    : WorkflowConstants.EMPTY_AUTO_SKIP;
            if (WorkflowConstants.EMPTY_AUTO_SKIP.equals(strategy)) {
                log.info("Node {} has no assignees, auto-skipping", node.getNodeCode());
                return;
            }
            throw new RuntimeException("节点" + node.getNodeName() + "没有审批人");
        }

        // 3. 检查委托
        for (int i = 0; i < assigneeIds.size(); i++) {
            Long originalId = assigneeIds.get(i);
            Long delegateId = resolveDelegate(originalId);
            if (delegateId != null) {
                assigneeIds.set(i, delegateId);
            }
        }

        // 4. 按审批模式创建任务
        String approvalMode = node.getApprovalMode() != null ? node.getApprovalMode()
                : WorkflowConstants.APPROVAL_MODE_SEQUENTIAL;

        switch (approvalMode) {
            case WorkflowConstants.APPROVAL_MODE_SEQUENTIAL:
                createSequentialTasks(instance, node, assigneeIds);
                break;
            case WorkflowConstants.APPROVAL_MODE_COUNTERSIGN:
            case WorkflowConstants.APPROVAL_MODE_ORSIGN:
            case WorkflowConstants.APPROVAL_MODE_PROPORTIONAL:
            case WorkflowConstants.APPROVAL_MODE_VOTE:
                createMultiTasks(instance, node, assigneeIds, approvalMode);
                break;
            default:
                createSequentialTasks(instance, node, assigneeIds);
        }
    }

    private void createSequentialTasks(WfInstance instance, WfNode node, List<Long> assigneeIds) {
        WfTask task = buildTask(instance.getId(), node.getId(), assigneeIds.get(0),
                null, WorkflowConstants.TASK_TYPE_TODO, null, node);
        taskMapper.insert(task);
        updateInstanceCurrentNodes(instance, Collections.singletonList(node.getId()));

        eventPublisher.publishTaskCreated(task, node.getNodeName());
    }

    private void createMultiTasks(WfInstance instance, WfNode node, List<Long> assigneeIds, String mode) {
        WfTask parentTask = buildTask(instance.getId(), node.getId(), assigneeIds.get(0),
                null, mode, null, node);
        taskMapper.insert(parentTask);

        for (Long assigneeId : assigneeIds) {
            WfTask childTask = buildTask(instance.getId(), node.getId(), assigneeId,
                    null, WorkflowConstants.TASK_TYPE_COUNTERSIGN, parentTask.getId(), node);
            taskMapper.insert(childTask);
            eventPublisher.publishTaskCreated(childTask, node.getNodeName());
        }

        updateInstanceCurrentNodes(instance, Collections.singletonList(node.getId()));
    }

    private WfTask buildTask(Long instanceId, Long nodeId, Long assigneeId, Long originalAssigneeId,
                             String taskType, Long parentTaskId, WfNode node) {
        WfTask task = new WfTask();
        task.setInstanceId(instanceId);
        task.setNodeId(nodeId);
        task.setAssigneeId(assigneeId);
        task.setOriginalAssigneeId(originalAssigneeId);
        task.setTaskType(taskType);
        task.setStatus(WorkflowConstants.TASK_STATUS_PENDING);
        task.setParentTaskId(parentTaskId);
        task.setStartTime(LocalDateTime.now());
        task.setIsRead(0);
        task.setRemindCount(0);

        if (node != null && node.getTimeoutHours() != null && node.getTimeoutHours() > 0) {
            task.setDueTime(LocalDateTime.now().plusHours(node.getTimeoutHours()));
        }
        return task;
    }

    private void updateInstanceCurrentNodes(WfInstance instance, List<Long> nodeIds) {
        instance.setCurrentNodeIds(JSONUtil.toJsonStr(nodeIds));
        instanceMapper.updateById(instance);
    }

    private List<Long> resolveAssignees(WfNode node, Long starterId, String formDataJson) {
        List<WfAssigneeRule> rules = assigneeRuleMapper.selectByNodeId(node.getId());
        return resolverChain.resolveAll(rules, starterId, formDataJson);
    }

    private Long resolveDelegate(Long assigneeId) {
        WfDelegation delegation = delegationMapper.findActiveByDelegator(assigneeId, LocalDateTime.now());
        return delegation != null ? delegation.getDelegateId() : null;
    }

    private boolean isAuthorizedDelegate(Long handlerId, Long assigneeId) {
        WfDelegation delegation = delegationMapper.findActiveByDelegator(assigneeId, LocalDateTime.now());
        return delegation != null && delegation.getDelegateId().equals(handlerId);
    }

    private void advanceToNextNode(WfTask task, WfInstance instance) {
        List<WfNode> allNodes = loadNodesFromSnapshot(instance);
        Map<String, Object> conditionContext = instance.getConditionContext() != null ?
                JSONUtil.parseObj(instance.getConditionContext()).toBean(Map.class) : null;
        List<WfNode> applicableNodes = filterApplicableNodes(allNodes, conditionContext);

        int currentIdx = -1;
        for (int i = 0; i < applicableNodes.size(); i++) {
            if (applicableNodes.get(i).getId().equals(task.getNodeId())) {
                currentIdx = i;
                break;
            }
        }

        if (currentIdx >= 0 && currentIdx + 1 < applicableNodes.size()) {
            WfNode nextNode = applicableNodes.get(currentIdx + 1);
            createTasksForNode(instance, nextNode, instance.getStarterId(), null);
        } else {
            completeInstance(instance, WorkflowConstants.INSTANCE_STATUS_PASSED);
        }
    }

    private void completeInstance(WfInstance instance, String status) {
        stateMachine.transit(instance, status);
        instance.setEndTime(LocalDateTime.now());
        instance.setCurrentNodeIds(null);
        instanceMapper.updateById(instance);

        eventPublisher.publishInstanceCompleted(instance);
    }

    private List<WfNode> loadNodesFromSnapshot(WfInstance instance) {
        if (instance.getDefSnapshot() != null) {
            try {
                var snapshot = JSONUtil.parseObj(instance.getDefSnapshot());
                var nodesArr = snapshot.getJSONArray("nodes");
                if (nodesArr != null) {
                    return nodesArr.toList(WfNode.class);
                }
            } catch (Exception e) {
                log.warn("Failed to parse snapshot, falling back to DB", e);
            }
        }
        return nodeMapper.selectByDefId(instance.getDefId());
    }

    private void createRecord(Long instanceId, Long taskId, Long operatorId,
                               String action, Long fromNodeId, Long toNodeId, String opinion) {
        WfRecord record = new WfRecord();
        record.setInstanceId(instanceId);
        record.setTaskId(taskId);
        record.setOperatorId(operatorId);
        record.setAction(action);
        record.setFromNodeId(fromNodeId);
        record.setToNodeId(toNodeId);
        record.setOpinion(opinion);
        recordMapper.insert(record);
    }
}
