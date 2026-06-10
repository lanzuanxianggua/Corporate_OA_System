package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.exception.BusinessException;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.oa.entity.WfDelegation;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
import cn.oa.entity.WfCcRecord;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.SysEmpRole;
import cn.oa.entity.SysRole;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaBusinessTrip;
import cn.oa.entity.OaOvertime;
import cn.oa.entity.OaPurchase;
import cn.oa.entity.OaTodo;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.WfProcessDefinitionMapper;
import cn.oa.mapper.WfProcessInstanceMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.mapper.WfCcRecordMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.mapper.SysEmpRoleMapper;
import cn.oa.mapper.SysRoleMapper;
import cn.oa.service.DelegationService;
import cn.oa.service.DeptService;
import cn.oa.service.TodoService;
import cn.oa.service.WorkflowService;
import cn.oa.service.NotificationService;
import cn.oa.service.workflow.WorkflowGraph;
import cn.oa.service.workflow.WorkflowRuntimeEngine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowServiceImpl extends ServiceImpl<WfProcessDefinitionMapper, WfProcessDefinition> implements WorkflowService {

    @Autowired
    private WfProcessInstanceMapper instanceMapper;

    @Autowired
    private WfTaskMapper taskMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private SysEmpRoleMapper empRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private TodoService todoService;

    @Lazy
    @Autowired
    private WorkflowCallbackDispatcher callbackDispatcher;

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private WfCcRecordMapper ccRecordMapper;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DeptService deptService;

    /** V1010: per-business mappers used by previewPath() to build the condition context. */
    @Autowired
    private cn.oa.mapper.OaBusinessTripMapper oaBusinessTripMapper;
    @Autowired
    private cn.oa.mapper.OaOvertimeMapper oaOvertimeMapper;
    @Autowired
    private cn.oa.mapper.OaPurchaseMapper oaPurchaseMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WorkflowRuntimeEngine workflowRuntimeEngine;

    @Override
    @Transactional
    public WfProcessInstance startProcess(String businessType, Long businessId, Long initiatorId) {
        return startProcess(businessType, businessId, initiatorId, null);
    }

    @Override
    @Transactional
    public WfProcessInstance startProcess(String businessType, Long businessId, Long initiatorId, Map<String, Object> conditionContext) {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessDefinition::getProcessType, businessType)
                .eq(WfProcessDefinition::getStatus, "0")
                .orderByDesc(WfProcessDefinition::getVersion)
                .last("LIMIT 1");
        WfProcessDefinition definition = this.getOne(wrapper);
        if (definition == null) {
            log.warn("No workflow definition found for businessType={}, auto-approving businessId={}", businessType, businessId);
            WfProcessInstance instance = new WfProcessInstance();
            instance.setProcessId(null);
            instance.setBusinessType(businessType);
            instance.setBusinessId(businessId);
            instance.setInitiatorId(initiatorId);
            instance.setCurrentNode(0);
            instance.setStatus("1"); // approved
            instance.setStartTime(LocalDateTime.now());
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.insert(instance);
            // Auto-approve the business entity since no workflow is configured
            try {
                callbackDispatcher.onApproved(businessType, businessId);
            } catch (Exception e) {
                log.error("Auto-approve callback failed for businessType={}, businessId={}: {}", businessType, businessId, e.getMessage(), e);
            }
            return instance;
        }

        JSONArray nodes;
        try {
            nodes = materializeExecutableNodes(definition, null, conditionContext);
        } catch (Exception e) {
            log.error("Failed to parse nodeConfig for processType={}, auto-approving businessId={}: {}", businessType, businessId, e.getMessage());
            WfProcessInstance instance = new WfProcessInstance();
            instance.setProcessId(definition.getId());
            instance.setBusinessType(businessType);
            instance.setBusinessId(businessId);
            instance.setInitiatorId(initiatorId);
            instance.setCurrentNode(0);
            instance.setStatus("1"); // approved
            instance.setStartTime(LocalDateTime.now());
            instance.setEndTime(LocalDateTime.now());
            instance.setProcessVersion(definition.getVersion());
            instanceMapper.insert(instance);
            try {
                callbackDispatcher.onApproved(businessType, businessId);
            } catch (Exception ex) {
                log.error("Auto-approve callback failed for businessType={}, businessId={}: {}", businessType, businessId, ex.getMessage(), ex);
            }
            return instance;
        }

        if (nodes == null || nodes.isEmpty()) {
            log.warn("Workflow definition has empty nodeConfig for businessType={}, auto-approving businessId={}", businessType, businessId);
            WfProcessInstance instance = new WfProcessInstance();
            instance.setProcessId(definition.getId());
            instance.setBusinessType(businessType);
            instance.setBusinessId(businessId);
            instance.setInitiatorId(initiatorId);
            instance.setCurrentNode(0);
            instance.setStatus("1"); // approved
            instance.setStartTime(LocalDateTime.now());
            instance.setEndTime(LocalDateTime.now());
            instance.setProcessVersion(definition.getVersion());
            instanceMapper.insert(instance);
            try {
                callbackDispatcher.onApproved(businessType, businessId);
            } catch (Exception e) {
                log.error("Auto-approve callback failed for businessType={}, businessId={}: {}", businessType, businessId, e.getMessage(), e);
            }
            return instance;
        }

        // Filter nodes by conditions
        List<JSONObject> applicableNodes = filterApplicableNodes(nodes, conditionContext);
        if (applicableNodes.isEmpty()) {
            log.warn("No applicable approval nodes for businessType={}, auto-approving businessId={}", businessType, businessId);
            WfProcessInstance instance = new WfProcessInstance();
            instance.setProcessId(definition.getId());
            instance.setBusinessType(businessType);
            instance.setBusinessId(businessId);
            instance.setInitiatorId(initiatorId);
            instance.setCurrentNode(0);
            instance.setStatus("1"); // approved
            instance.setStartTime(LocalDateTime.now());
            instance.setEndTime(LocalDateTime.now());
            instance.setProcessVersion(definition.getVersion());
            instanceMapper.insert(instance);
            try {
                callbackDispatcher.onApproved(businessType, businessId);
            } catch (Exception e) {
                log.error("Auto-approve callback failed for businessType={}, businessId={}: {}", businessType, businessId, e.getMessage(), e);
            }
            return instance;
        }

        WfProcessInstance instance = new WfProcessInstance();
        instance.setProcessId(definition.getId());
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setInitiatorId(initiatorId);
        instance.setCurrentNode(0);
        instance.setStatus("0");
        instance.setStartTime(LocalDateTime.now());
        instance.setProcessVersion(definition.getVersion());
        instance.setSnapshotNodeConfig(definition.getNodeConfig());
        if (conditionContext != null && !conditionContext.isEmpty()) {
            instance.setConditionContext(JSONUtil.toJsonStr(conditionContext));
        }
        instanceMapper.insert(instance);

        try {
            createTaskForNode(instance, applicableNodes.get(0), 0);
        } catch (Exception e) {
            log.error("Failed to create task for first node of businessType={}, businessId={}: {}", businessType, businessId, e.getMessage(), e);
            // Roll back the instance to auto-approved since task creation failed
            instance.setStatus("1");
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.updateById(instance);
            try {
                callbackDispatcher.onApproved(businessType, businessId);
            } catch (Exception ex) {
                log.error("Auto-approve callback failed for businessType={}, businessId={}: {}", businessType, businessId, ex.getMessage(), ex);
            }
            return instance;
        }
        log.info("Process started: businessType={}, businessId={}, initiatorId={}", businessType, businessId, initiatorId);

        return instance;
    }

    @Override
    public WfTask getCurrentTask(Long instanceId) {
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getStatus, "0")
                .orderByDesc(WfTask::getCreateTime)
                .last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public void handleTask(Long taskId, Long handlerId, Integer status, String remark) {
        if (status == 2 && (remark == null || remark.trim().isEmpty())) {
            throw new BusinessException("驳回时必须填写原因");
        }

        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // Authorization check: handler must be the task assignee OR authorized via delegation
        if (!task.getAssigneeId().equals(handlerId)) {
            boolean authorized = false;
            // Check if handler is the delegator of the task assignee (task was delegated to assignee)
            Long delegateId = delegationService.resolveDelegate(handlerId);
            if (delegateId != null && delegateId.equals(task.getAssigneeId())) {
                authorized = true;
            }
            // Check if handler is the delegate of the task assignee (reverse delegation)
            if (!authorized) {
                WfDelegation reverseDelegation = delegationService.findActiveDelegationForDelegate(handlerId);
                if (reverseDelegation != null && reverseDelegation.getDelegatorId().equals(task.getAssigneeId())) {
                    authorized = true;
                }
            }
            if (!authorized) {
                // Check admin role
                boolean isAdmin = isAdminUser(handlerId);
                if (!isAdmin) {
                    log.warn("handleTask: user {} not authorized for task {} assigned to {}, rejecting", handlerId, taskId, task.getAssigneeId());
                    throw new BusinessException("无权处理此任务");
                }
                log.info("handleTask: admin override - user {} handling task {} assigned to {}", handlerId, taskId, task.getAssigneeId());
            } else {
                log.info("handleTask: delegation approval - user {} acting on task {} assigned to {}", handlerId, taskId, task.getAssigneeId());
            }
        }

        // === Acquire distributed lock to prevent TOCTOU race on countersign/orsign ===
        String lockKey = "lock:workflow:task:" + (task.getParentTaskId() != null ? task.getParentTaskId() : taskId);
        String lockValue = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            if (!"0".equals(task.getStatus())) {
                throw new BusinessException("任务已处理");
            }

            task.setStatus(String.valueOf(status));
            task.setCompleteTime(LocalDateTime.now());
            task.setOpinion(remark);
            taskMapper.updateById(task);

            WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
            if (instance == null) {
                throw new BusinessException("流程实例不存在");
            }

        // Fetch approver name for audit record
        SysEmployee approver = employeeMapper.selectById(handlerId);
        String approverName = approver != null ? approver.getEmpName() : null;

        // Insert audit record with taskId, nodeName, assigneeName
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(instance.getBusinessId());
        record.setBusinessType(instance.getBusinessType());
        record.setApproverId(handlerId);
        // delegation scenario: record original task assignee as delegatorId
        if (!handlerId.equals(task.getAssigneeId())) {
            record.setDelegatorId(task.getAssigneeId());
        }
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        record.setTaskId(taskId);
        record.setNodeName(task.getNodeName());
        record.setAssigneeName(approverName);
        approvalRecordMapper.insert(record);

        // Handle multi-person approval (countersign/orsign)
        boolean isMultiTask = task.getParentTaskId() != null || isMultiTaskType(task.getTaskType());
        if (isMultiTask && status == 1) {
            String multiType = task.getTaskType();
            if ("countersign".equals(multiType)) {
                // Countersign: check if all siblings approved
                Long parentId = task.getParentTaskId() != null ? task.getParentTaskId() : taskId;
                LambdaQueryWrapper<WfTask> siblingWrapper = new LambdaQueryWrapper<>();
                siblingWrapper.eq(WfTask::getParentTaskId, parentId)
                        .eq(WfTask::getStatus, "0");
                Long pendingCount = taskMapper.selectCount(siblingWrapper);

                if (pendingCount > 0) {
                    // Still waiting for other approvers, don't advance
                    return;
                }

                // All approved — cancel parent task if this was a child
                if (task.getParentTaskId() != null) {
                    WfTask parentTask = taskMapper.selectById(task.getParentTaskId());
                    if (parentTask != null && "0".equals(parentTask.getStatus())) {
                        parentTask.setStatus("1");
                        parentTask.setCompleteTime(LocalDateTime.now());
                        taskMapper.updateById(parentTask);
                    }
                }
                // Fall through to advance to next node
            } else if ("orsign".equals(multiType)) {
                // Orsign: first approval wins — cancel all other pending siblings
                Long parentId = task.getParentTaskId() != null ? task.getParentTaskId() : taskId;
                LambdaQueryWrapper<WfTask> siblingWrapper = new LambdaQueryWrapper<>();
                siblingWrapper.eq(WfTask::getParentTaskId, parentId)
                        .eq(WfTask::getStatus, "0");
                List<WfTask> pendingSiblings = taskMapper.selectList(siblingWrapper);
                for (WfTask sibling : pendingSiblings) {
                    sibling.setStatus("4"); // canceled
                    sibling.setCompleteTime(LocalDateTime.now());
                    sibling.setOpinion("或签已由其他审批人处理");
                    taskMapper.updateById(sibling);
                }

                // Cancel parent task
                if (task.getParentTaskId() != null) {
                    WfTask parentTask = taskMapper.selectById(task.getParentTaskId());
                    if (parentTask != null && "0".equals(parentTask.getStatus())) {
                        parentTask.setStatus("1");
                        parentTask.setCompleteTime(LocalDateTime.now());
                        taskMapper.updateById(parentTask);
                    }
                }
                // Fall through to advance to next node
            }
        }

        if (status == 2) {
            // rejected
            handleRejection(task, instance, isMultiTask);
        } else if (status == 1) {
            // approved, check if more applicable nodes
            Map<String, Object> ctx = null;
            if (instance.getConditionContext() != null && !instance.getConditionContext().isEmpty()) {
                ctx = JSONUtil.parseObj(instance.getConditionContext()).toBean(Map.class);
            }
            WfProcessDefinition definition = this.getById(instance.getProcessId());
            JSONArray nodes = materializeExecutableNodes(definition, instance, ctx);
            advanceToNextNode(task, instance, nodes, ctx, remark);
        }
        } finally {
            String currentVal = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentVal)) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    public IPage<WfTask> myPendingTasks(Long assigneeId, int pageNum, int pageSize) {
        Page<WfTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        if (!isAdminUser(assigneeId)) {
            wrapper.eq(WfTask::getAssigneeId, assigneeId);
        }
        wrapper.eq(WfTask::getStatus, "0")
               .orderByDesc(WfTask::getCreateTime);
        IPage<WfTask> result = taskMapper.selectPage(page, wrapper);
        fillWorkflowTaskDisplayFields(result.getRecords());
        return result;
    }

    @Override
    public IPage<WfTask> myHandledTasks(Long assigneeId, int pageNum, int pageSize) {
        Page<WfTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();

        if (!isAdminUser(assigneeId)) {
            List<OaApprovalRecord> handledRecords = approvalRecordMapper.selectList(new LambdaQueryWrapper<OaApprovalRecord>()
                    .eq(OaApprovalRecord::getApproverId, assigneeId)
                    .isNotNull(OaApprovalRecord::getTaskId));
            List<Long> handledTaskIds = Optional.ofNullable(handledRecords)
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .map(OaApprovalRecord::getTaskId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            wrapper.and(w -> {
                w.eq(WfTask::getAssigneeId, assigneeId);
                if (!handledTaskIds.isEmpty()) {
                    w.or().in(WfTask::getId, handledTaskIds);
                }
            });
        }

        wrapper.ne(WfTask::getStatus, "0")
                .orderByDesc(WfTask::getCompleteTime);
        IPage<WfTask> result = taskMapper.selectPage(page, wrapper);
        fillWorkflowTaskDisplayFields(result.getRecords());
        return result;
    }

    private void fillWorkflowTaskDisplayFields(List<WfTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> missingInstanceIds = tasks.stream()
                .filter(task -> task.getInstance() == null)
                .map(WfTask::getInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, WfProcessInstance> instanceMap = missingInstanceIds.isEmpty()
                ? Map.of()
                : Optional.ofNullable(instanceMapper.selectBatchIds(missingInstanceIds)).orElseGet(Collections::emptyList).stream()
                        .collect(Collectors.toMap(WfProcessInstance::getId, instance -> instance, (a, b) -> a));

        Set<Long> initiatorIds = new LinkedHashSet<>();
        Set<Long> processIds = new LinkedHashSet<>();
        for (WfTask task : tasks) {
            WfProcessInstance instance = task.getInstance();
            if (instance == null && task.getInstanceId() != null) {
                instance = instanceMap.get(task.getInstanceId());
                task.setInstance(instance);
            }
            if (instance != null) {
                if (instance.getInitiatorId() != null) {
                    initiatorIds.add(instance.getInitiatorId());
                }
                if (instance.getProcessId() != null) {
                    processIds.add(instance.getProcessId());
                }
            }
        }

        Map<Long, SysEmployee> initiatorMap = initiatorIds.isEmpty()
                ? Map.of()
                : Optional.ofNullable(employeeMapper.selectBatchIds(initiatorIds)).orElseGet(Collections::emptyList).stream()
                        .collect(Collectors.toMap(SysEmployee::getId, emp -> emp, (a, b) -> a));
        Map<Long, WfProcessDefinition> definitionMap = processIds.isEmpty()
                ? Map.of()
                : Optional.ofNullable(baseMapper.selectBatchIds(processIds)).orElseGet(Collections::emptyList).stream()
                        .collect(Collectors.toMap(WfProcessDefinition::getId, definition -> definition, (a, b) -> a));

        for (WfTask task : tasks) {
            WfProcessInstance instance = task.getInstance();

            String businessType = instance != null ? instance.getBusinessType() : task.getBusinessType();
            task.setBusinessType(businessType);
            task.setTaskName(firstNonBlank(task.getNodeName(), task.getTaskType()));
            task.setMultiType(isMultiTaskType(task.getTaskType()) ? task.getTaskType() : null);
            task.setRemark(task.getOpinion());
            task.setUpdateTime(firstNonNull(task.getCompleteTime(), task.getCreateTime()));

            if (instance == null) {
                task.setProcessName(firstNonBlank(task.getProcessName(), workflowBusinessName(businessType)));
                task.setApplicant(firstNonBlank(task.getApplicant(), "-"));
                continue;
            }

            if (instance.getInitiatorId() != null) {
                SysEmployee initiator = initiatorMap.get(instance.getInitiatorId());
                if (initiator != null) {
                    instance.setInitiatorName(initiator.getEmpName());
                    task.setApplicant(initiator.getEmpName());
                }
            }

            if (instance.getProcessId() != null) {
                WfProcessDefinition definition = definitionMap.get(instance.getProcessId());
                if (definition != null) {
                    task.setProcessName(definition.getProcessName());
                }
            }

            task.setProcessName(firstNonBlank(task.getProcessName(), workflowBusinessName(businessType)));
            task.setApplicant(firstNonBlank(task.getApplicant(), instance.getInitiatorName(), "-"));
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private String workflowBusinessName(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return "-";
        }
        return switch (businessType) {
            case "leave" -> "请假审批";
            case "trip" -> "出差审批";
            case "outing" -> "外出审批";
            case "purchase" -> "采购审批";
            case "expense" -> "经费审批";
            case "overtime" -> "加班审批";
            case "loan" -> "借支审批";
            case "contract" -> "合同审批";
            default -> businessType;
        };
    }

    @Override
    public WfProcessInstance getByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<WfProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessInstance::getBusinessType, businessType)
                .eq(WfProcessInstance::getBusinessId, businessId)
                .orderByDesc(WfProcessInstance::getCreateTime)
                .last("LIMIT 1");
        return instanceMapper.selectOne(wrapper);
    }

    @Override
    public List<WfProcessDefinition> listDefinitions() {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(WfProcessDefinition::getProcessType)
                .orderByDesc(WfProcessDefinition::getVersion);
        return this.list(wrapper);
    }

    @Override
    @Transactional
    public int repairMissingPendingTasks() {
        LambdaQueryWrapper<WfProcessInstance> instanceWrapper = new LambdaQueryWrapper<>();
        instanceWrapper.eq(WfProcessInstance::getStatus, "0")
                .orderByAsc(WfProcessInstance::getCreateTime);
        List<WfProcessInstance> instances = instanceMapper.selectList(instanceWrapper);
        if (instances == null || instances.isEmpty()) {
            return 0;
        }

        int repaired = 0;
        for (WfProcessInstance instance : instances) {
            if (instance == null || hasPendingTask(instance.getId())) {
                continue;
            }

            WfProcessDefinition definition = findActiveDefinition(instance.getBusinessType());
            if (definition == null || definition.getNodeConfig() == null || definition.getNodeConfig().isBlank()) {
                log.warn("repairMissingPendingTasks: no active v2 definition for businessType={}, businessId={}",
                        instance.getBusinessType(), instance.getBusinessId());
                continue;
            }

            Map<String, Object> context = repairConditionContext(instance);
            List<JSONObject> nodes;
            try {
                JSONArray materializedNodes = materializeExecutableNodes(definition, null, context);
                nodes = filterApplicableNodes(materializedNodes, context);
            } catch (Exception e) {
                log.warn("repairMissingPendingTasks: cannot materialize v2 definition for businessType={}, businessId={}: {}",
                        instance.getBusinessType(), instance.getBusinessId(), e.getMessage());
                continue;
            }
            if (nodes == null || nodes.isEmpty()) {
                log.warn("repairMissingPendingTasks: no executable approval node for businessType={}, businessId={}",
                        instance.getBusinessType(), instance.getBusinessId());
                continue;
            }

            JSONObject node = selectRuntimeNode(nodes, instance.getCurrentNode());
            if (node == null) {
                continue;
            }
            int currentNode = node.getInt("runtimeIndex", Math.max(0, Optional.ofNullable(instance.getCurrentNode()).orElse(0)));

            WfProcessInstance update = new WfProcessInstance();
            update.setId(instance.getId());
            update.setProcessId(definition.getId());
            update.setProcessVersion(definition.getVersion());
            update.setSnapshotNodeConfig(definition.getNodeConfig());
            update.setConditionContext(JSONUtil.toJsonStr(context));
            update.setCurrentNode(currentNode);
            instanceMapper.updateById(update);

            instance.setProcessId(definition.getId());
            instance.setProcessVersion(definition.getVersion());
            instance.setSnapshotNodeConfig(definition.getNodeConfig());
            instance.setConditionContext(JSONUtil.toJsonStr(context));
            instance.setCurrentNode(currentNode);
            createTaskForNode(instance, node, currentNode);
            repaired++;
        }

        if (repaired > 0) {
            log.info("repairMissingPendingTasks: rebuilt {} missing workflow pending task set(s)", repaired);
        }
        return repaired;
    }

    private boolean hasPendingTask(Long instanceId) {
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getStatus, "0"));
        return count != null && count > 0;
    }

    private WfProcessDefinition findActiveDefinition(String businessType) {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessDefinition::getProcessType, businessType)
                .eq(WfProcessDefinition::getStatus, "0")
                .orderByDesc(WfProcessDefinition::getVersion)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    private Map<String, Object> repairConditionContext(WfProcessInstance instance) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (instance.getConditionContext() != null && !instance.getConditionContext().isBlank()) {
            try {
                JSONObject parsed = JSONUtil.parseObj(instance.getConditionContext());
                for (String key : parsed.keySet()) {
                    context.put(key, parsed.get(key));
                }
            } catch (Exception e) {
                log.warn("repairMissingPendingTasks: invalid conditionContext on instance {}, rebuilding from business data",
                        instance.getId());
            }
        }

        Map<String, Object> rebuilt = buildPreviewContext(
                instance.getBusinessType(), instance.getBusinessId(), instance.getInitiatorId());
        rebuilt.forEach(context::putIfAbsent);
        return context;
    }

    private JSONObject selectRuntimeNode(List<JSONObject> nodes, Integer currentNode) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        int target = Math.max(0, Optional.ofNullable(currentNode).orElse(0));
        JSONObject fallback = nodes.get(nodes.size() - 1);
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.get(i);
            int runtimeIndex = node.getInt("runtimeIndex", i);
            if (runtimeIndex == target) {
                return node;
            }
            if (runtimeIndex > target) {
                return node;
            }
        }
        return fallback;
    }

    @Override
    @Transactional
    public void saveDefinition(WfProcessDefinition definition) {
        WorkflowGraph graph = validateDefinition(definition.getNodeConfig());
        if (!graph.valid) {
            throw new BusinessException("流程定义校验失败: " + graph.errors.stream()
                    .map(error -> error.message)
                    .collect(Collectors.joining("; ")));
        }
        if (definition.getProcessType() == null || definition.getProcessType().isBlank()) {
            throw new BusinessException("流程分类不能为空");
        }
        if (definition.getProcessKey() == null || definition.getProcessKey().isBlank()) {
            definition.setProcessKey(definition.getProcessType() + "_process");
        }

        if (definition.getId() != null) {
            WfProcessDefinition existing = this.getById(definition.getId());
            if (existing != null) {
                existing.setStatus("1");
                this.updateById(existing);
                definition.setId(null);
                definition.setVersion(existing.getVersion() + 1);
                definition.setStatus("0");
                definition.setCreateTime(LocalDateTime.now());
                deactivateActiveDefinitions(definition.getProcessType(), existing.getId());
                this.save(definition);
                return;
            }
        }
        definition.setVersion(nextDefinitionVersion(definition.getProcessType()));
        definition.setStatus("0");
        definition.setCreateTime(LocalDateTime.now());
        deactivateActiveDefinitions(definition.getProcessType(), null);
        this.save(definition);
    }

    private int nextDefinitionVersion(String processType) {
        WfProcessDefinition latest = this.getOne(new LambdaQueryWrapper<WfProcessDefinition>()
                .eq(WfProcessDefinition::getProcessType, processType)
                .orderByDesc(WfProcessDefinition::getVersion)
                .last("LIMIT 1"));
        return Optional.ofNullable(latest)
                .map(WfProcessDefinition::getVersion)
                .map(version -> version + 1)
                .orElse(1);
    }

    private void deactivateActiveDefinitions(String processType, Long excludeId) {
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<WfProcessDefinition>()
                .eq(WfProcessDefinition::getProcessType, processType)
                .eq(WfProcessDefinition::getStatus, "0");
        if (excludeId != null) {
            wrapper.ne(WfProcessDefinition::getId, excludeId);
        }
        List<WfProcessDefinition> activeDefinitions = this.list(wrapper);
        if (activeDefinitions == null || activeDefinitions.isEmpty()) {
            return;
        }
        for (WfProcessDefinition activeDefinition : activeDefinitions) {
            activeDefinition.setStatus("1");
            this.updateById(activeDefinition);
        }
    }

    @Override
    @Transactional
    public void activateDefinition(Long definitionId) {
        WfProcessDefinition definition = this.getById(definitionId);
        if (definition == null) {
            throw new BusinessException("流程定义不存在");
        }
        if ("0".equals(definition.getStatus())) {
            definition.setStatus("1");
            this.updateById(definition);
            return;
        }

        WorkflowGraph graph = validateDefinition(definition.getNodeConfig());
        if (!graph.valid) {
            throw new BusinessException("流程定义校验失败: " + graph.errors.stream()
                    .map(error -> error.message)
                    .collect(Collectors.joining("; ")));
        }
        deactivateActiveDefinitions(definition.getProcessType(), definition.getId());
        definition.setStatus("0");
        this.updateById(definition);
    }

    private void createTaskForNode(WfProcessInstance instance, JSONObject node, int runtimeIndex) {
        String nodeName = node.getStr("nodeName");
        String nodeType = node.getStr("nodeType");

        // Handle subprocess node
        if ("subprocess".equals(nodeType)) {
            String subProcessKey = node.getStr("subProcessKey");
            if (subProcessKey != null) {
                createSubprocess(instance, subProcessKey, runtimeIndex);
                return;
            }
        }

        String assigneeType = node.getStr("assigneeType");
        String assigneeValue = resolveAssigneeValue(node);
        String multiType = node.getStr("multiType");
        JSONArray multiAssigneeIds = node.getJSONArray("multiAssigneeIds");

        // Resolve primary assignee
        Long primaryAssigneeId = resolveAssignee(assigneeType, assigneeValue, instance.getInitiatorId());

        // Check delegation
        Long delegateId = delegationService.resolveDelegate(primaryAssigneeId);
        if (delegateId != null) {
            primaryAssigneeId = delegateId;
        }

        // Collect all assignees for countersign/orsign
        List<Long> allAssigneeIds = new ArrayList<>();
        allAssigneeIds.add(primaryAssigneeId);
        if (("countersign".equals(multiType) || "orsign".equals(multiType)) && multiAssigneeIds != null && !multiAssigneeIds.isEmpty()) {
            for (int i = 0; i < multiAssigneeIds.size(); i++) {
                Long extraId = multiAssigneeIds.getLong(i);
                if (extraId != null && !allAssigneeIds.contains(extraId)) {
                    // Check delegation for extra assignees too
                    Long extraDelegate = delegationService.resolveDelegate(extraId);
                    Long resolvedId = extraDelegate != null ? extraDelegate : extraId;
                    if (!allAssigneeIds.contains(resolvedId)) {
                        allAssigneeIds.add(resolvedId);
                    }
                }
            }
        }

        // Create task(s) - single task for normal flow, multiple for countersign/orsign
        Long parentTaskId = null;
        if (("countersign".equals(multiType) || "orsign".equals(multiType)) && allAssigneeIds.size() > 1) {
            // Create a logical parent task (placeholder) for grouping
            WfTask parentTask = new WfTask();
            parentTask.setInstanceId(instance.getId());
            parentTask.setNodeId((long) runtimeIndex);
            parentTask.setNodeName(nodeName);
            parentTask.setAssigneeId(allAssigneeIds.get(0));
            parentTask.setStatus("0");
            parentTask.setActionSource("assignee");
            parentTask.setTaskType(multiType);
            parentTask.setCreateTime(LocalDateTime.now());
            Integer timeoutHours = node.getInt("timeoutHours", 0);
            if (timeoutHours != null && timeoutHours > 0) {
                parentTask.setDueTime(LocalDateTime.now().plusHours(timeoutHours));
            }
            parentTask.setRemindCount(0);
            taskMapper.insert(parentTask);
            parentTaskId = parentTask.getId();

            // Create child tasks for all assignees (including primary)
            for (Long assigneeId : allAssigneeIds) {
                WfTask childTask = new WfTask();
                childTask.setInstanceId(instance.getId());
                childTask.setNodeId((long) runtimeIndex);
                childTask.setNodeName(nodeName);
                childTask.setAssigneeId(assigneeId);
                childTask.setStatus("0");
                childTask.setActionSource("assignee");
                childTask.setTaskType(multiType);
                childTask.setParentTaskId(parentTaskId);
                childTask.setCreateTime(LocalDateTime.now());
                if (timeoutHours != null && timeoutHours > 0) {
                    childTask.setDueTime(LocalDateTime.now().plusHours(timeoutHours));
                }
                childTask.setRemindCount(0);
                taskMapper.insert(childTask);

                // CC, todo, notification for each assignee
                createCcAndTodoForTask(node, instance, childTask, assigneeId);
            }

            // [P1.5] Mark parent task as inactive to prevent parent/child dual activation
            // The parent task exists only as a logical grouping placeholder; children do the real work.
            WfTask parentTaskUpdate = new WfTask();
            parentTaskUpdate.setId(parentTaskId);
            parentTaskUpdate.setAssigneeId(null);
            parentTaskUpdate.setStatus("1");
            parentTaskUpdate.setCompleteTime(LocalDateTime.now());
            taskMapper.updateById(parentTaskUpdate);
        } else {
            // Single approver
            WfTask task = new WfTask();
            task.setInstanceId(instance.getId());
            task.setNodeId((long) runtimeIndex);
            task.setNodeName(nodeName);
            task.setAssigneeId(primaryAssigneeId);
            task.setStatus("0");
            task.setActionSource("assignee");
            task.setCreateTime(LocalDateTime.now());

            Integer timeoutHours = node.getInt("timeoutHours", 0);
            if (timeoutHours != null && timeoutHours > 0) {
                task.setDueTime(LocalDateTime.now().plusHours(timeoutHours));
            }
            task.setRemindCount(0);
            taskMapper.insert(task);

            createCcAndTodoForTask(node, instance, task, primaryAssigneeId);
        }
    }

    private boolean isMultiTaskType(String taskType) {
        return "countersign".equals(taskType) || "orsign".equals(taskType);
    }

    /**
     * Convert graph JSON into the executable approval-node list
     * consumed by the runtime engine.
     */
    private JSONArray materializeExecutableNodes(WfProcessDefinition definition,
                                                 WfProcessInstance instance,
                                                 Map<String, Object> conditionContext) {
        String nodeConfig = null;
        if (instance != null && instance.getSnapshotNodeConfig() != null && !instance.getSnapshotNodeConfig().isBlank()) {
            nodeConfig = instance.getSnapshotNodeConfig();
        } else if (definition != null) {
            nodeConfig = definition.getNodeConfig();
        }
        if (nodeConfig == null || nodeConfig.isBlank()) {
            return new JSONArray();
        }

        WorkflowGraph graph = parseNodeConfig(nodeConfig);
        if (!graph.isGraph() || !graph.valid) {
            throw new BusinessException("流程定义必须使用 graph schema 配置: " + graph.errors);
        }
        return workflowRuntimeEngine.materializeGraphToRuntimePath(graph, conditionContext);
    }

    private int runtimeIndex(WfTask task) {
        return task.getNodeId() == null ? -1 : task.getNodeId().intValue();
    }

    private void createCcAndTodoForTask(JSONObject node, WfProcessInstance instance, WfTask task, Long assigneeId) {
        // CC list
        JSONArray ccList = node.getJSONArray("ccList");
        if (ccList != null && !ccList.isEmpty()) {
            for (int i = 0; i < ccList.size(); i++) {
                Long ccEmpId = ccList.getLong(i);
                WfCcRecord ccRecord = new WfCcRecord();
                ccRecord.setInstanceId(instance.getId());
                ccRecord.setTaskId(task.getId());
                ccRecord.setCcEmpId(ccEmpId);
                ccRecord.setStatus("0");
                ccRecord.setCreateTime(LocalDateTime.now());
                ccRecordMapper.insert(ccRecord);
                todoService.addTodo(ccEmpId, "抄送: " + instance.getBusinessType(), "cc", instance.getId(), instance.getBusinessType());
            }
        }

        // Create todo for the assignee
        String title = "待审批: " + instance.getBusinessType();
        todoService.addTodo(assigneeId, title, "approval", instance.getId(), instance.getBusinessType());

        // Send WebSocket notification to the assignee
        notificationService.notifyTask(assigneeId, instance.getBusinessType(),
                instance.getBusinessId(), "你有一个新的审批任务: " + instance.getBusinessType());
    }

    private Long resolveAssignee(String assigneeType, String assigneeValue, Long initiatorId) {
        switch (assigneeType) {
            case "specific":
                return Long.valueOf(assigneeValue);
            case "role":
                return resolveByRole(assigneeValue, initiatorId);
            case "role_global":
                return resolveByRoleGlobal(assigneeValue);
            case "dept_manager":
                return resolveDeptManager(initiatorId);
            case "role_chain":
                // V1010: amount-based level routing.
                //   assigneeValue is a JSON array string of role keys in ascending level order,
                //   e.g. "[\"DEPT_MANAGER\", \"DIRECTOR\", \"GM\"]".
                //   We pick the highest role that still has a candidate.
                return resolveByRoleChain(assigneeValue);
            case "initiator_level_match":
                // V1010: high-level initiator optimization.
                //   assigneeValue is a JSON array string of role keys in DESCENDING level order,
                //   e.g. "[\"GM\", \"DIRECTOR\", \"DEPT_MANAGER\"]".
                //   We pick the first role that is strictly higher than the initiator's own
                //   level, so a GM's leave request goes only to ADMIN and skips lower tiers.
                return resolveByInitiatorLevelMatch(assigneeValue, initiatorId);
            default:
                try { return Long.valueOf(assigneeValue); }
                catch (NumberFormatException e) { return resolveByRole(assigneeValue, initiatorId); }
        }
    }

    private String resolveAssigneeValue(JSONObject node) {
        if (node == null) return null;
        String assigneeType = node.getStr("assigneeType");
        String assigneeValue = node.getStr("assigneeValue");
        if (assigneeValue == null || assigneeValue.isBlank()) {
            assigneeValue = node.getStr("roleKey");
        }
        if (("dept_manager".equals(assigneeType) || "direct_manager".equals(assigneeType))
                && (assigneeValue == null || assigneeValue.isBlank())) {
            assigneeValue = "dept_manager";
        }
        return assigneeValue;
    }

    /**
     * V1010: pick the LAST role key in a chain (chain implies ascending level,
     * so the last entry is the most senior role the workflow can use). Falls
     * back to the previous entry if no employee holds the most senior role.
     */
    private Long resolveByRoleChain(String assigneeValueJson) {
        List<String> chain = parseJsonStringList(assigneeValueJson);
        if (chain.isEmpty()) {
            throw new BusinessException("role_chain 配置为空");
        }
        // Walk from most senior to least, return first with a candidate.
        for (int i = chain.size() - 1; i >= 0; i--) {
            Long empId = tryResolveByRoleGlobal(chain.get(i));
            if (empId != null) return empId;
        }
        throw new BusinessException("未找到角色链中任何角色的审批人: " + chain);
    }

    /**
     * V1010: pick the first role in a DESCENDING-level array whose level is
     * strictly greater than the initiator's own level. Lets e.g. a GM request
     * skip directly to ADMIN without bothering DIRECTOR/DEPT_MANAGER.
     */
    private Long resolveByInitiatorLevelMatch(String assigneeValueJson, Long initiatorId) {
        List<String> chain = parseJsonStringList(assigneeValueJson);
        if (chain.isEmpty()) {
            throw new BusinessException("initiator_level_match 配置为空");
        }
        int initiatorLevel = computeInitiatorLevel(initiatorId);
        for (String roleKey : chain) {
            int candidateLevel = cn.oa.common.constant.RoleLevel.of(roleKey);
            if (candidateLevel > initiatorLevel) {
                Long empId = tryResolveByRoleGlobal(roleKey);
                if (empId != null) return empId;
            }
        }
        // No strictly-higher role found: fall back to the first entry that has a candidate
        // (better than throwing — the workflow can still complete via the next routing rule).
        for (String roleKey : chain) {
            Long empId = tryResolveByRoleGlobal(roleKey);
            if (empId != null) return empId;
        }
        throw new BusinessException("未找到 initiator_level_match 任何角色的审批人: " + chain);
    }

    /** Like {@link #resolveByRoleGlobal} but returns null instead of throwing,
     *  so chain resolvers can fall through to the next role key. */
    private Long tryResolveByRoleGlobal(String roleKey) {
        try {
            return resolveByRoleGlobal(roleKey);
        } catch (BusinessException e) {
            return null;
        }
    }

    /** Compute the initiator's highest role level (delegates to RoleLevel.maxLevel). */
    private int computeInitiatorLevel(Long initiatorId) {
        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, initiatorId));
        if (empRoles.isEmpty()) return cn.oa.common.constant.RoleLevel.USER;
        List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return cn.oa.common.constant.RoleLevel.maxLevel(
                roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList()));
    }

    /** Parse a JSON array of strings (e.g. {@code "[\"DEPT_MANAGER\", \"GM\"]"}) into a List.
     *  Returns an empty list on null / empty / parse errors. */
    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) out.add(arr.getStr(i));
            return out;
        } catch (Exception e) {
            log.warn("parseJsonStringList: failed to parse '{}', returning empty", json);
            return Collections.emptyList();
        }
    }

    /**
     * Resolve the department manager for the initiator's department.
     * Strategy:
     * 1. Look at the initiator's department and use the dept leader field if it matches a DEPT_MANAGER role.
     * 2. Walk up the department tree to find a department with a leader who has the DEPT_MANAGER role.
     * 3. Fallback to the role-based resolution (any employee with DEPT_MANAGER role in same dept).
     */
    private Long resolveDeptManager(Long initiatorId) {
        SysEmployee initiator = employeeMapper.selectById(initiatorId);
        if (initiator == null || initiator.getDeptId() == null) {
            log.warn("resolveDeptManager: initiator {} has no dept, falling back to role lookup", initiatorId);
            return resolveByRole("DEPT_MANAGER", initiatorId);
        }

        // Find the DEPT_MANAGER role ID
        SysRole deptManagerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "DEPT_MANAGER").last("LIMIT 1"));

        // Walk up the department tree looking for a department with a leader
        Long currentDeptId = initiator.getDeptId();
        int maxDepth = 10; // prevent infinite loop
        while (currentDeptId != null && maxDepth-- > 0) {
            cn.oa.entity.SysDept dept = deptService.getById(currentDeptId);
            if (dept == null) break;

            String leader = dept.getLeader();
            if (leader != null && !leader.trim().isEmpty()) {
                // The leader field might be an empId (numeric) or a name
                try {
                    Long leaderEmpId = Long.valueOf(leader.trim());
                    // Verify this leader has DEPT_MANAGER role
                    if (deptManagerRole != null) {
                        Long count = empRoleMapper.selectCount(
                                new LambdaQueryWrapper<SysEmpRole>()
                                        .eq(SysEmpRole::getEmpId, leaderEmpId)
                                        .eq(SysEmpRole::getRoleId, deptManagerRole.getId()));
                        if (count > 0) {
                            log.debug("resolveDeptManager: found dept leader empId={} for deptId={}", leaderEmpId, currentDeptId);
                            return leaderEmpId;
                        }
                    }
                    // Leader exists but may not have explicit role; still return if dept is set up this way
                    SysEmployee leaderEmp = employeeMapper.selectById(leaderEmpId);
                    if (leaderEmp != null) {
                        log.debug("resolveDeptManager: using dept leader empId={} (no role check) for deptId={}", leaderEmpId, currentDeptId);
                        return leaderEmpId;
                    }
                } catch (NumberFormatException ignored) {
                    // leader is a name string, not an empId; try to find by name
                    SysEmployee leaderByName = employeeMapper.selectOne(
                            new LambdaQueryWrapper<SysEmployee>()
                                    .eq(SysEmployee::getEmpName, leader.trim())
                                    .eq(SysEmployee::getDelFlag, "0")
                                    .last("LIMIT 1"));
                    if (leaderByName != null) {
                        log.debug("resolveDeptManager: found dept leader by name empId={} for deptId={}", leaderByName.getId(), currentDeptId);
                        return leaderByName.getId();
                    }
                }
            }

            // Move to parent department
            if (dept.getParentId() == null || dept.getParentId() == 0L) break;
            currentDeptId = dept.getParentId();
        }

        // Fallback to role-based resolution
        log.debug("resolveDeptManager: falling back to role-based lookup for initiatorId={}", initiatorId);
        return resolveByRole("DEPT_MANAGER", initiatorId);
    }

    private boolean isAdminUser(Long empId) {
        if (empId == null) {
            return false;
        }
        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, empId));
        if (empRoles == null || empRoles.isEmpty()) return false;
        List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return false;
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles == null || roles.isEmpty()) return false;
        return roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleKey()));
    }

    private Long resolveByRole(String roleKey, Long initiatorId) {
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey).last("LIMIT 1"));
        if (role == null) throw new BusinessException("角色不存在: " + roleKey);

        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getRoleId, role.getId()));
        if (empRoles.isEmpty()) throw new BusinessException("未找到角色为 " + roleKey + " 的审批人");

        Set<Long> candidateIds = empRoles.stream().map(SysEmpRole::getEmpId).collect(Collectors.toSet());

        // Prefer same department
        SysEmployee initiator = employeeMapper.selectById(initiatorId);
        if (initiator != null && initiator.getDeptId() != null) {
            SysEmployee sameDept = employeeMapper.selectOne(
                    new LambdaQueryWrapper<SysEmployee>()
                            .eq(SysEmployee::getDeptId, initiator.getDeptId())
                            .in(SysEmployee::getId, candidateIds)
                            .last("LIMIT 1"));
            if (sameDept != null) return sameDept.getId();
        }

        // Fallback: any employee with this role
        SysEmployee fallback = employeeMapper.selectOne(
                new LambdaQueryWrapper<SysEmployee>()
                        .in(SysEmployee::getId, candidateIds)
                        .last("LIMIT 1"));
        if (fallback != null) return fallback.getId();

        throw new BusinessException("未找到角色为 " + roleKey + " 的审批人");
    }

    private Long resolveByRoleGlobal(String roleKey) {
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, roleKey).last("LIMIT 1"));
        if (role == null) throw new BusinessException("角色不存在: " + roleKey);

        SysEmpRole empRole = empRoleMapper.selectOne(
                new LambdaQueryWrapper<SysEmpRole>()
                        .eq(SysEmpRole::getRoleId, role.getId())
                        .last("LIMIT 1"));
        if (empRole == null) throw new BusinessException("未找到角色为 " + roleKey + " 的审批人");
        return empRole.getEmpId();
    }

    private List<JSONObject> filterApplicableNodes(JSONArray nodes, Map<String, Object> context) {
        return workflowRuntimeEngine.filterApplicableNodes(nodes, context);
    }

    private boolean evaluateConditions(JSONArray conditions, Map<String, Object> context) {
        return workflowRuntimeEngine.evaluateConditions(conditions, context);
    }

    @FunctionalInterface
    private interface NumBiPredicate {
        boolean test(double a, double b);
    }

    private boolean numCheck(Object rawVal, Object thresholdObj, NumBiPredicate pred) {
        try {
            double actual = (rawVal instanceof Number) ? ((Number) rawVal).doubleValue()
                    : Double.parseDouble(String.valueOf(rawVal));
            double threshold = (thresholdObj instanceof Number) ? ((Number) thresholdObj).doubleValue()
                    : Double.parseDouble(String.valueOf(thresholdObj));
            return pred.test(actual, threshold);
        } catch (NumberFormatException e) {
            log.warn("numCheck: failed to parse numeric values, rawVal={}, thresholdObj={}", rawVal, thresholdObj);
            return false;
        }
    }

    private void completeTodo(Long instanceId) {
        try {
            LambdaQueryWrapper<OaTodo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OaTodo::getBusinessId, instanceId)
                   .eq(OaTodo::getStatus, "0");
            OaTodo todo = todoService.getOne(wrapper);
            if (todo != null) {
                todoService.doneTodo(todo.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to complete todo for instance {}: {}", instanceId, e.getMessage());
        }
    }

    /**
     * Handle task rejection: cancel sibling tasks, mark instance as rejected, fire callbacks.
     */
    private void handleRejection(WfTask task, WfProcessInstance instance, boolean isMultiTask) {
        if (isMultiTask) {
            Long parentId = task.getParentTaskId() != null ? task.getParentTaskId() : task.getId();
            LambdaQueryWrapper<WfTask> siblingWrapper = new LambdaQueryWrapper<>();
            siblingWrapper.eq(WfTask::getParentTaskId, parentId)
                    .eq(WfTask::getStatus, "0");
            List<WfTask> pendingSiblings = taskMapper.selectList(siblingWrapper);
            for (WfTask sibling : pendingSiblings) {
                sibling.setStatus("4");
                sibling.setCompleteTime(LocalDateTime.now());
                taskMapper.updateById(sibling);
            }
            if (task.getParentTaskId() != null) {
                WfTask parentTask = taskMapper.selectById(task.getParentTaskId());
                if (parentTask != null && "0".equals(parentTask.getStatus())) {
                    parentTask.setStatus("2");
                    parentTask.setCompleteTime(LocalDateTime.now());
                    taskMapper.updateById(parentTask);
                }
            }
        }

        instance.setStatus("2");
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        callbackDispatcher.onRejected(instance.getBusinessType(), instance.getBusinessId());
        completeTodo(instance.getId());
        notificationService.notifyApproval(instance.getInitiatorId(), instance.getBusinessType(),
                instance.getBusinessId(), "rejected", task.getOpinion());
    }

    /**
     * Advance workflow to the next applicable node after approval.
     * If no more nodes, complete the process and fire onApproved callback.
     */
    private void advanceToNextNode(WfTask task, WfProcessInstance instance,
                                   JSONArray nodes, Map<String, Object> ctx, String remark) {
        List<JSONObject> applicableNodes = filterApplicableNodes(nodes, ctx);

        int currentApplicableIndex = -1;
        for (int i = 0; i < applicableNodes.size(); i++) {
            if (applicableNodes.get(i).getInt("runtimeIndex", -1) == runtimeIndex(task)) {
                currentApplicableIndex = i;
                break;
            }
        }

        if (currentApplicableIndex >= 0 && currentApplicableIndex + 1 < applicableNodes.size()) {
            JSONObject nextNode = applicableNodes.get(currentApplicableIndex + 1);
            int nextIdx = nextNode.getInt("runtimeIndex", currentApplicableIndex + 1);
            instance.setCurrentNode(nextIdx);
            instanceMapper.updateById(instance);
            createTaskForNode(instance, nextNode, nextIdx);

            Long nextAssigneeId = resolveAssignee(nextNode.getStr("assigneeType"),
                    resolveAssigneeValue(nextNode), instance.getInitiatorId());
            notificationService.notifyTask(nextAssigneeId, instance.getBusinessType(),
                    instance.getBusinessId(), "你有一个新的审批任务");
        } else {
            // No more nodes — process complete
            instance.setStatus("1");
            instance.setEndTime(LocalDateTime.now());
            instanceMapper.updateById(instance);
            callbackDispatcher.onApproved(instance.getBusinessType(), instance.getBusinessId());
            completeTodo(instance.getId());
            notificationService.notifyApproval(instance.getInitiatorId(), instance.getBusinessType(),
                    instance.getBusinessId(), "approved", remark);

            if (instance.getParentInstanceId() != null) {
                resumeParentProcess(instance.getParentInstanceId());
            }
        }
    }

    @Override
    public WfTask findPendingTask(String businessType, Long businessId, Long assigneeId) {
        WfProcessInstance instance = getByBusiness(businessType, businessId);
        if (instance == null) {
            log.warn("findPendingTask: no instance found for businessType={}, businessId={}", businessType, businessId);
            return null;
        }

        if (isAdminUser(assigneeId)) {
            WfTask adminTask = findAnyPendingTask(instance.getId());
            if (adminTask != null) {
                log.debug("findPendingTask: admin match taskId={} for businessType={}, businessId={}",
                        adminTask.getId(), businessType, businessId);
                return adminTask;
            }
        }

        // 1. Direct match: task assigned to this user
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instance.getId())
               .eq(WfTask::getAssigneeId, assigneeId)
               .eq(WfTask::getStatus, "0")
               .last("LIMIT 1");
        WfTask task = taskMapper.selectOne(wrapper);
        if (task != null) {
            log.debug("findPendingTask: direct match taskId={} for assigneeId={}", task.getId(), assigneeId);
            return task;
        }

        // 2. Delegation check: task was delegated, assigneeId on task is the delegate,
        //    but current user is the delegator (original assignee). Allow the delegator to see it.
        Long delegateId = delegationService.resolveDelegate(assigneeId);
        if (delegateId != null) {
            LambdaQueryWrapper<WfTask> delegateWrapper = new LambdaQueryWrapper<>();
            delegateWrapper.eq(WfTask::getInstanceId, instance.getId())
                    .eq(WfTask::getAssigneeId, delegateId)
                    .eq(WfTask::getStatus, "0")
                    .last("LIMIT 1");
            WfTask delegatedTask = taskMapper.selectOne(delegateWrapper);
            if (delegatedTask != null) {
                log.debug("findPendingTask: delegation match taskId={} delegator={} delegate={}",
                        delegatedTask.getId(), assigneeId, delegateId);
                return delegatedTask;
            }
        }

        // 3. Reverse delegation check: current user may be a delegate whose delegator
        //    was the original assignee. Find if there is an active delegation where
        //    this user is the delegateToId.
        WfDelegation reverseDelegation = findActiveDelegationForDelegate(assigneeId);
        if (reverseDelegation != null) {
            LambdaQueryWrapper<WfTask> reverseWrapper = new LambdaQueryWrapper<>();
            reverseWrapper.eq(WfTask::getInstanceId, instance.getId())
                    .eq(WfTask::getAssigneeId, reverseDelegation.getDelegatorId())
                    .eq(WfTask::getStatus, "0")
                    .last("LIMIT 1");
            WfTask reverseTask = taskMapper.selectOne(reverseWrapper);
            if (reverseTask != null) {
                log.debug("findPendingTask: reverse delegation match taskId={} delegate={} delegator={}",
                        reverseTask.getId(), assigneeId, reverseDelegation.getDelegatorId());
                return reverseTask;
            }
        }

        // 4. Multi-person task fallback: check if user is a child task assignee
        //    (e.g., in countersign/orsign scenarios where parentTaskId is set)
        LambdaQueryWrapper<WfTask> multiWrapper = new LambdaQueryWrapper<>();
        multiWrapper.eq(WfTask::getInstanceId, instance.getId())
                .eq(WfTask::getAssigneeId, assigneeId)
                .eq(WfTask::getStatus, "0")
                .isNotNull(WfTask::getParentTaskId)
                .last("LIMIT 1");
        WfTask multiTask = taskMapper.selectOne(multiWrapper);
        if (multiTask != null) {
            log.debug("findPendingTask: multi-person match taskId={} for assigneeId={}", multiTask.getId(), assigneeId);
            return multiTask;
        }

        // 5. No matching task found for this user
        log.debug("findPendingTask: no pending task found for businessType={}, businessId={}, assigneeId={}", businessType, businessId, assigneeId);
        return null;
    }

    private WfTask findAnyPendingTask(Long instanceId) {
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getStatus, "0")
                .orderByAsc(WfTask::getCreateTime)
                .last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    /**
     * Find an active delegation record where the given empId is the delegateToId.
     * This handles the case where a user is acting as delegate for someone else.
     */
    private WfDelegation findActiveDelegationForDelegate(Long delegateToId) {
        return delegationService.findActiveDelegationForDelegate(delegateToId);
    }

    @Override
    @Transactional
    public void withdrawProcess(String businessType, Long businessId, Long initiatorId) {
        WfProcessInstance instance = getByBusiness(businessType, businessId);
        if (instance == null) {
            throw new BusinessException("流程实例不存在");
        }
        if (!"0".equals(instance.getStatus())) {
            throw new BusinessException("流程已结束，无法撤回");
        }
        if (!instance.getInitiatorId().equals(initiatorId)) {
            throw new BusinessException("只有申请人才能撤回");
        }

        // Cancel all pending tasks
        LambdaQueryWrapper<WfTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(WfTask::getInstanceId, instance.getId())
                .eq(WfTask::getStatus, "0");
        List<WfTask> pendingTasks = taskMapper.selectList(taskWrapper);
        for (WfTask t : pendingTasks) {
            t.setStatus("4");
            t.setActionSource("initiator");
            t.setCompleteTime(LocalDateTime.now());
            taskMapper.updateById(t);
        }

        // Set instance status to canceled
        instance.setStatus("3");
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // Insert approval record
        SysEmployee withdrawer = employeeMapper.selectById(initiatorId);
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(instance.getBusinessId());
        record.setBusinessType(instance.getBusinessType());
        record.setApproverId(initiatorId);
        record.setApproveStatus(4);
        record.setRemark("申请人撤回");
        record.setApproveTime(LocalDateTime.now());
        record.setAssigneeName(withdrawer != null ? withdrawer.getEmpName() : null);
        approvalRecordMapper.insert(record);

        callbackDispatcher.onWithdrawn(instance.getBusinessType(), instance.getBusinessId());
        completeTodo(instance.getId());
    }

    @Override
    @Transactional
    public void transferTask(Long taskId, Long fromAssigneeId, Long toAssigneeId, String reason) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!"0".equals(task.getStatus())) {
            throw new BusinessException("任务已处理，无法转办");
        }
        if (!task.getAssigneeId().equals(fromAssigneeId)) {
            throw new BusinessException("无权转办此任务");
        }

        // Update old task
        task.setStatus("3");
        task.setCompleteTime(LocalDateTime.now());
        task.setTransferFromId(fromAssigneeId);
        task.setTransferReason(reason);
        taskMapper.updateById(task);

        // Create new task
        WfTask newTask = new WfTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeId(task.getNodeId());
        newTask.setNodeName(task.getNodeName());
        newTask.setAssigneeId(toAssigneeId);
        newTask.setStatus("0");
        newTask.setActionSource("transfer");
        newTask.setCreateTime(LocalDateTime.now());
        taskMapper.insert(newTask);

        // Complete old assignee's todo, create new assignee's todo
        WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance != null) {
            completeTodo(instance.getId());
            String title = "待审批(转办): " + instance.getBusinessType();
            todoService.addTodo(toAssigneeId, title, "approval", instance.getId(), instance.getBusinessType());
        }
    }

    @Override
    @Transactional
    public void urgeTask(Long instanceId, Long initiatorId) {
        WfProcessInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException("流程实例不存在");
        }
        if (!instance.getInitiatorId().equals(initiatorId)) {
            throw new BusinessException("只有申请人才能催办");
        }

        // Find current pending task
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instanceId)
                .eq(WfTask::getStatus, "0")
                .last("LIMIT 1");
        WfTask task = taskMapper.selectOne(wrapper);
        if (task == null) {
            throw new BusinessException("没有待处理的任务");
        }

        int count = task.getRemindCount() != null ? task.getRemindCount() : 0;
        task.setRemindCount(count + 1);
        taskMapper.updateById(task);

        todoService.addTodo(task.getAssigneeId(), "催办提醒: 审批任务待处理", "approval", instanceId, instance.getBusinessType());
    }

    @Override
    public List<WfTask> getApprovalHistory(String businessType, Long businessId) {
        WfProcessInstance instance = getByBusiness(businessType, businessId);
        if (instance == null) return Collections.emptyList();
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instance.getId())
               .orderByAsc(WfTask::getNodeId);
        List<WfTask> tasks = taskMapper.selectList(wrapper);
        fillWorkflowTaskDisplayFields(tasks);
        // Fill assignee names
        for (WfTask task : tasks) {
            if (task.getAssigneeId() != null) {
                SysEmployee emp = employeeMapper.selectById(task.getAssigneeId());
                if (emp != null) task.setAssigneeName(emp.getEmpName());
            }
        }
        return tasks;
    }

    @Override
    public List<OaApprovalRecord> getApprovalChain(String businessType, Long businessId) {
        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaApprovalRecord::getBusinessType, businessType)
                .eq(OaApprovalRecord::getApplyId, businessId)
                .orderByAsc(OaApprovalRecord::getApproveTime);
        List<OaApprovalRecord> records = approvalRecordMapper.selectList(wrapper);
        // Fill assigneeName if missing
        for (OaApprovalRecord record : records) {
            if (record.getAssigneeName() == null && record.getApproverId() != null) {
                SysEmployee emp = employeeMapper.selectById(record.getApproverId());
                if (emp != null) {
                    record.setAssigneeName(emp.getEmpName());
                }
            }
        }
        return records;
    }

    @Override
    @Transactional
    public void returnTask(Long taskId, Long handlerId, String returnTarget, String remark) {
        if (remark == null || remark.trim().isEmpty()) {
            throw new BusinessException("退回时必须填写原因");
        }

        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!task.getAssigneeId().equals(handlerId)) {
            throw new BusinessException("无权处理此任务");
        }
        if (!"0".equals(task.getStatus())) {
            throw new BusinessException("任务已处理");
        }

        WfProcessInstance instance = instanceMapper.selectById(task.getInstanceId());
        if (instance == null) {
            throw new BusinessException("流程实例不存在");
        }

        // Cancel all pending tasks for this instance
        LambdaQueryWrapper<WfTask> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(WfTask::getInstanceId, instance.getId())
                .eq(WfTask::getStatus, "0");
        List<WfTask> pendingTasks = taskMapper.selectList(pendingWrapper);
        for (WfTask t : pendingTasks) {
            t.setStatus("5"); // returned
            t.setCompleteTime(LocalDateTime.now());
            t.setOpinion("流程退回");
            taskMapper.updateById(t);
        }

        // Insert audit record for the return action
        SysEmployee handler = employeeMapper.selectById(handlerId);
        OaApprovalRecord returnRecord = new OaApprovalRecord();
        returnRecord.setApplyId(instance.getBusinessId());
        returnRecord.setBusinessType(instance.getBusinessType());
        returnRecord.setApproverId(handlerId);
        returnRecord.setApproveStatus(5); // returned
        returnRecord.setRemark("退回: " + remark);
        returnRecord.setApproveTime(LocalDateTime.now());
        returnRecord.setTaskId(taskId);
        returnRecord.setNodeName(task.getNodeName());
        returnRecord.setAssigneeName(handler != null ? handler.getEmpName() : null);
        approvalRecordMapper.insert(returnRecord);

        Map<String, Object> ctx = null;
        if (instance.getConditionContext() != null && !instance.getConditionContext().isEmpty()) {
            ctx = JSONUtil.parseObj(instance.getConditionContext()).toBean(Map.class);
        }
        WfProcessDefinition definition = this.getById(instance.getProcessId());
        JSONArray nodes = materializeExecutableNodes(definition, instance, ctx);
        List<JSONObject> applicableNodes = filterApplicableNodes(nodes, ctx);

        // Determine target node
        int targetNodeIndex;
        if ("initiator".equals(returnTarget)) {
            // Return to initiator: set instance status to "returned" so initiator can re-submit
            instance.setCurrentNode(-1);
            instance.setStatus("5"); // returned
            instance.setEndTime(null);
            instanceMapper.updateById(instance);

            completeTodo(instance.getId());
            notificationService.notifyApproval(instance.getInitiatorId(), instance.getBusinessType(),
                    instance.getBusinessId(), "returned", remark);
            return;
        } else {
            // Return to specific node index
            try {
                targetNodeIndex = Integer.parseInt(returnTarget);
            } catch (NumberFormatException e) {
                throw new BusinessException("无效的退回目标: " + returnTarget);
            }
        }

        // Find the target node config
        JSONObject targetNode = null;
        for (JSONObject node : applicableNodes) {
            if (node.getInt("runtimeIndex", -1) == targetNodeIndex) {
                targetNode = node;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("退回目标节点不存在");
        }

        // Create new task at the target node
        instance.setCurrentNode(targetNodeIndex);
        instanceMapper.updateById(instance);
        createTaskForNode(instance, targetNode, targetNodeIndex);

        // Notify the target node assignee
        Long targetAssigneeId = resolveAssignee(targetNode.getStr("assigneeType"),
                resolveAssigneeValue(targetNode), instance.getInitiatorId());
        notificationService.notifyTask(targetAssigneeId, instance.getBusinessType(),
                instance.getBusinessId(), "退回的审批任务需要重新处理");

        completeTodo(instance.getId());
    }

    @Override
    public Long resolveAssigneeForEscalation(String assigneeType, String assigneeValue, Long currentAssigneeId) {
        // Treat the current assignee as the "initiator" for role-based resolution.
        // The escalation scheduler will discard the result if it equals the current
        // assignee, so this is safe for role_global / role / dept_manager / specific.
        return resolveAssignee(assigneeType, assigneeValue, currentAssigneeId);
    }

    @Override
    public WorkflowGraph validateDefinition(String nodeConfig) {
        return parseNodeConfig(nodeConfig);
    }

    @Override
    public List<JSONObject> previewPath(String businessType, Long businessId, Long initiatorId) {
        // Look up the latest active definition for this processType
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessDefinition::getProcessType, businessType)
                .eq(WfProcessDefinition::getStatus, "0")
                .orderByDesc(WfProcessDefinition::getVersion)
                .last("LIMIT 1");
        WfProcessDefinition definition = this.getOne(wrapper);
        if (definition == null) {
            log.warn("previewPath: no definition for businessType={}", businessType);
            return java.util.Collections.emptyList();
        }

        // Build the condition context the same way startProcess would
        Map<String, Object> ctx = buildPreviewContext(businessType, businessId, initiatorId);
        WorkflowGraph graph = parseNodeConfig(definition.getNodeConfig());
        if (!graph.isGraph() || !graph.valid) {
            return java.util.Collections.emptyList();
        }
        JSONArray path = workflowRuntimeEngine.materializeGraphToRuntimePath(graph, ctx);
        return path.toList(JSONObject.class);
    }

    /**
     * Build the condition context used for preview. Reuses the same mappers
     * the live startProcess path uses (BaseApprovalServiceImpl is in oa-service,
     * so we can call its buildConditionContext method via a per-type switch).
     */
    private Map<String, Object> buildPreviewContext(String businessType, Long businessId, Long initiatorId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("businessType", businessType);
        ctx.put("initiatorId", initiatorId);
        if (initiatorId != null) {
            ctx.put("initiatorLevel", computeInitiatorLevel(initiatorId));
        }
        try {
            switch (businessType) {
                case "trip": {
                    OaBusinessTrip trip = oaBusinessTripMapper.selectById(businessId);
                    if (trip != null && trip.getStartTime() != null && trip.getEndTime() != null) {
                        long days = java.time.temporal.ChronoUnit.DAYS.between(
                                trip.getStartTime().toLocalDate(), trip.getEndTime().toLocalDate()) + 1;
                        ctx.put("days", days);
                    }
                    break;
                }
                case "overtime": {
                    OaOvertime ot = oaOvertimeMapper.selectById(businessId);
                    if (ot != null && ot.getHours() != null) {
                        ctx.put("hours", ot.getHours().doubleValue());
                    }
                    break;
                }
                case "purchase": {
                    OaPurchase p = oaPurchaseMapper.selectById(businessId);
                    if (p != null && p.getAmount() != null) {
                        ctx.put("amount", p.getAmount().doubleValue());
                        ctx.put("quantity", p.getQuantity());
                    }
                    break;
                }
                default:
                    // No extractor for this business type
                    break;
            }
        } catch (Exception e) {
            log.warn("buildPreviewContext: failed to populate context for businessType={}, businessId={}: {}",
                    businessType, businessId, e.getMessage());
        }
        return ctx;
    }

    private void createSubprocess(WfProcessInstance parentInstance, String subProcessKey, int runtimeIndex) {
        // Find the subprocess definition        // Find the subprocess definition
        LambdaQueryWrapper<WfProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfProcessDefinition::getProcessType, subProcessKey)
                .eq(WfProcessDefinition::getStatus, "0")
                .orderByDesc(WfProcessDefinition::getVersion)
                .last("LIMIT 1");
        WfProcessDefinition subDef = this.getOne(wrapper);
        if (subDef == null) {
            throw new BusinessException("子流程定义不存在: " + subProcessKey);
        }

        JSONArray subNodes = materializeExecutableNodes(subDef, null, null);
        if (subNodes == null || subNodes.isEmpty()) {
            throw new BusinessException("子流程节点配置为空: " + subProcessKey);
        }

        List<JSONObject> applicableSubNodes = filterApplicableNodes(subNodes, null);

        WfProcessInstance subInstance = new WfProcessInstance();
        subInstance.setProcessId(subDef.getId());
        subInstance.setBusinessType(subProcessKey);
        subInstance.setBusinessId(parentInstance.getBusinessId());
        subInstance.setInitiatorId(parentInstance.getInitiatorId());
        subInstance.setCurrentNode(0);
        subInstance.setStatus("0");
        subInstance.setStartTime(LocalDateTime.now());
        subInstance.setProcessVersion(subDef.getVersion());
        subInstance.setParentInstanceId(parentInstance.getId());
        subInstance.setSnapshotNodeConfig(subDef.getNodeConfig());
        instanceMapper.insert(subInstance);

        createTaskForNode(subInstance, applicableSubNodes.get(0), 0);
    }

    private void resumeParentProcess(Long parentInstanceId) {
        WfProcessInstance parent = instanceMapper.selectById(parentInstanceId);
        if (parent == null || !"0".equals(parent.getStatus())) return;

        WfProcessDefinition definition = this.getById(parent.getProcessId());
        if (definition == null) return;

        Map<String, Object> ctx = null;
        if (parent.getConditionContext() != null && !parent.getConditionContext().isEmpty()) {
            ctx = JSONUtil.parseObj(parent.getConditionContext()).toBean(Map.class);
        }
        JSONArray allNodes = materializeExecutableNodes(definition, parent, ctx);
        List<JSONObject> applicableNodes = filterApplicableNodes(allNodes, ctx);

        // Find the subprocess node and advance to the next node
        int subprocessNodeIndex = parent.getCurrentNode();
        for (int i = 0; i < applicableNodes.size(); i++) {
            if (applicableNodes.get(i).getInt("runtimeIndex", -1) == subprocessNodeIndex) {
                if (i + 1 < applicableNodes.size()) {
                    JSONObject nextNode = applicableNodes.get(i + 1);
                    int nextIdx = nextNode.getInt("runtimeIndex", i + 1);
                    parent.setCurrentNode(nextIdx);
                    instanceMapper.updateById(parent);
                    createTaskForNode(parent, nextNode, nextIdx);
                } else {
                    parent.setStatus("1");
                    parent.setEndTime(LocalDateTime.now());
                    instanceMapper.updateById(parent);
                    callbackDispatcher.onApproved(parent.getBusinessType(), parent.getBusinessId());
                    completeTodo(parent.getId());
                }
                break;
            }
        }
    }

    public WorkflowGraph parseNodeConfig(String nodeConfig) {
        return workflowRuntimeEngine.parseNodeConfig(nodeConfig);
    }

    public String findNextNode(WorkflowGraph graph, String currentNodeId, Map<String, Object> ctx) {
        return workflowRuntimeEngine.findNextNode(graph, currentNodeId, ctx);
    }
}
