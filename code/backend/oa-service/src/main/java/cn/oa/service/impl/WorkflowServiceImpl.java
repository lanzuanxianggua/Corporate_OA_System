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
        wrapper.eq(WfTask::getAssigneeId, assigneeId)
                .eq(WfTask::getStatus, "0")
                .orderByDesc(WfTask::getCreateTime);
        IPage<WfTask> result = taskMapper.selectPage(page, wrapper);

        // fill instance info
        for (WfTask task : result.getRecords()) {
            WfProcessInstance inst = instanceMapper.selectById(task.getInstanceId());
            task.setInstance(inst);
            task.setBusinessType(inst != null ? inst.getBusinessType() : null);
        }
        return result;
    }

    @Override
    public IPage<WfTask> myHandledTasks(Long assigneeId, int pageNum, int pageSize) {
        Page<WfTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getAssigneeId, assigneeId)
                .ne(WfTask::getStatus, "0")
                .orderByDesc(WfTask::getCompleteTime);
        IPage<WfTask> result = taskMapper.selectPage(page, wrapper);

        for (WfTask task : result.getRecords()) {
            WfProcessInstance inst = instanceMapper.selectById(task.getInstanceId());
            task.setInstance(inst);
            task.setBusinessType(inst != null ? inst.getBusinessType() : null);
        }
        return result;
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
    public void saveDefinition(WfProcessDefinition definition) {
        if (definition.getId() != null) {
            WfProcessDefinition existing = this.getById(definition.getId());
            if (existing != null) {
                existing.setStatus("1");
                this.updateById(existing);
                definition.setId(null);
                definition.setVersion(existing.getVersion() + 1);
                definition.setStatus("0");
                definition.setCreateTime(LocalDateTime.now());
                this.save(definition);
                return;
            }
        }
        definition.setVersion(1);
        definition.setStatus("0");
        definition.setCreateTime(LocalDateTime.now());
        this.save(definition);
    }

    private void createTaskForNode(WfProcessInstance instance, JSONObject node, int nodeIndex) {
        String nodeName = node.getStr("nodeName");
        String nodeType = node.getStr("nodeType");

        // Handle subprocess node
        if ("subprocess".equals(nodeType)) {
            String subProcessKey = node.getStr("subProcessKey");
            if (subProcessKey != null) {
                createSubprocess(instance, subProcessKey, nodeIndex);
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
            parentTask.setNodeId((long) nodeIndex);
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
                childTask.setNodeId((long) nodeIndex);
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
            task.setNodeId((long) nodeIndex);
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
     * Convert either legacy flat node JSON or schemaVersion=2 graph JSON into the
     * flat approval-node list consumed by the runtime engine.
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
        if (graph.isGraph() && graph.valid) {
            return materializeGraphToFlatPath(graph, conditionContext);
        }
        return JSONUtil.parseArray(nodeConfig);
    }

    private int getLegacyNodeIndex(WfTask task) {
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
        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, empId));
        if (empRoles.isEmpty()) return false;
        List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
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
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            JSONArray conditions = node.getJSONArray("conditions");
            if (conditions == null || conditions.isEmpty() || context == null || context.isEmpty()) {
                result.add(node);
            } else if (evaluateConditions(conditions, context)) {
                result.add(node);
            }
        }
        return result;
    }

    private boolean evaluateConditions(JSONArray conditions, Map<String, Object> context) {
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject cond = conditions.getJSONObject(i);
            String field = cond.getStr("field");
            String operator = cond.getStr("operator");
            Object rawVal = context.get(field);
            if (rawVal == null) return false;

            Object thresholdObj = cond.get("value");
            String type = cond.getStr("type", "number");

            switch (operator) {
                // Numeric operators
                case "<=": if (!numCheck(rawVal, thresholdObj, (a, t) -> a <= t)) return false; break;
                case "<":  if (!numCheck(rawVal, thresholdObj, (a, t) -> a < t)) return false; break;
                case ">=": if (!numCheck(rawVal, thresholdObj, (a, t) -> a >= t)) return false; break;
                case ">":  if (!numCheck(rawVal, thresholdObj, (a, t) -> a > t)) return false; break;
                case "==": if (!numCheck(rawVal, thresholdObj, (a, t) -> Math.abs(a - t) < 1e-9)) return false; break;
                case "!=": if (!numCheck(rawVal, thresholdObj, (a, t) -> Math.abs(a - t) >= 1e-9)) return false; break;
                // String operators
                case "equals": if (!String.valueOf(rawVal).equals(String.valueOf(thresholdObj))) return false; break;
                case "not_equals": if (String.valueOf(rawVal).equals(String.valueOf(thresholdObj))) return false; break;
                case "contains": if (!String.valueOf(rawVal).contains(String.valueOf(thresholdObj))) return false; break;
                case "starts_with": if (!String.valueOf(rawVal).startsWith(String.valueOf(thresholdObj))) return false; break;
                // Enum operator
                case "in": {
                    String val = String.valueOf(rawVal);
                    JSONArray arr = cond.getJSONArray("values");
                    if (arr == null || !arr.toList(String.class).contains(val)) return false;
                    break;
                }
                default: return false;
            }
        }
        return true;
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
            if (applicableNodes.get(i).getInt("nodeIndex", -1) == getLegacyNodeIndex(task)) {
                currentApplicableIndex = i;
                break;
            }
        }

        if (currentApplicableIndex >= 0 && currentApplicableIndex + 1 < applicableNodes.size()) {
            JSONObject nextNode = applicableNodes.get(currentApplicableIndex + 1);
            int nextIdx = nextNode.getInt("nodeIndex", currentApplicableIndex + 1);
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
            if (node.getInt("nodeIndex", -1) == targetNodeIndex) {
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
            // Flat: return as-is
            try {
                return JSONUtil.parseArray(definition.getNodeConfig()).toList(JSONObject.class);
            } catch (Exception e) {
                return java.util.Collections.emptyList();
            }
        }
        JSONArray flat = materializeGraphToFlatPath(graph, ctx);
        return flat.toList(JSONObject.class);
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

    // --- Graph-based flow support ---

    /**
     * Detect if node config is graph format (has "nodes" and "edges") vs legacy flat array.
     */
    private boolean isGraphFormat(String nodeConfig) {
        if (nodeConfig == null) return false;
        String trimmed = nodeConfig.trim();
        return trimmed.startsWith("{\"nodes\"");
    }

    /**
     * Parse graph-format config into a map of nodeId -> node object.
     */
    private Map<String, JSONObject> buildNodeMap(JSONObject graphConfig) {
        JSONArray nodesArr = graphConfig.getJSONArray("nodes");
        Map<String, JSONObject> map = new LinkedHashMap<>();
        for (int i = 0; i < nodesArr.size(); i++) {
            JSONObject node = nodesArr.getJSONObject(i);
            map.put(node.getStr("nodeId"), node);
        }
        return map;
    }

    /**
     * Get outgoing edges from a node.
     */
    private List<JSONObject> getOutgoingEdges(JSONObject graphConfig, String nodeId) {
        JSONArray edges = graphConfig.getJSONArray("edges");
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            JSONObject edge = edges.getJSONObject(i);
            if (nodeId.equals(edge.getStr("sourceId"))) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * Evaluate edge condition against the process condition context.
     */
    private boolean evaluateEdgeCondition(JSONObject edge, Map<String, Object> ctx) {
        JSONObject condition = edge.getJSONObject("condition");
        if (condition == null) return true; // no condition = always match
        if (ctx == null || ctx.isEmpty()) return false;

        String field = condition.getStr("field");
        String operator = condition.getStr("operator");
        Object thresholdObj = condition.get("value");
        Object rawVal = ctx.get(field);
        if (rawVal == null || thresholdObj == null) return false;

        double actual = ((Number) rawVal).doubleValue();
        double threshold = ((Number) thresholdObj).doubleValue();
        switch (operator) {
            case "<=": return actual <= threshold;
            case "<":  return actual < threshold;
            case ">=": return actual >= threshold;
            case ">":  return actual > threshold;
            case "==": return actual == threshold;
            case "!=": return actual != threshold;
            default: return false;
        }
    }

    private void createSubprocess(WfProcessInstance parentInstance, String subProcessKey, int nodeIndex) {
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
            if (applicableNodes.get(i).getInt("nodeIndex", -1) == subprocessNodeIndex) {
                if (i + 1 < applicableNodes.size()) {
                    JSONObject nextNode = applicableNodes.get(i + 1);
                    int nextIdx = nextNode.getInt("nodeIndex", i + 1);
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

    // ====================================================================
    // V1010: Graph-format workflow definitions (schemaVersion=2).
    //
    // WorkflowGraph is a parsed view of a graph-format nodeConfig that exposes:
    //   - nodeId -> node object lookup
    //   - outgoing edges per nodeId
    //   - entry (start) and exit (end) nodes
    //
    // The graph path is opt-in: a definition without schemaVersion=2 is still
    // treated as a flat JSONArray (legacy). Use parseNodeConfig() as the single
    // entry point for callers that need graph traversal.
    // ====================================================================

    /** Parsed graph view of a graph-format nodeConfig. */
    public static class WorkflowGraph {
        public final int schemaVersion;
        public final Map<String, JSONObject> nodes;       // nodeId -> node
        public final Map<String, List<JSONObject>> outgoing; // nodeId -> edges where source=nodeId
        public final List<JSONObject> edges;
        public final List<ValidationError> errors;
        public final boolean valid;

        WorkflowGraph(int schemaVersion,
                      Map<String, JSONObject> nodes,
                      Map<String, List<JSONObject>> outgoing,
                      List<JSONObject> edges,
                      List<ValidationError> errors) {
            this.schemaVersion = schemaVersion;
            this.nodes = nodes;
            this.outgoing = outgoing;
            this.edges = edges;
            this.errors = errors;
            this.valid = errors.isEmpty();
        }

        public boolean isGraph() { return schemaVersion == 2; }
    }

    /** Structured validation error for graph definitions. */
    public static class ValidationError {
        public final String type;       // duplicate_node_id | unknown_edge_endpoint | no_start | no_end | cycle
        public final String nodeId;     // may be null
        public final String message;

        public ValidationError(String type, String nodeId, String message) {
            this.type = type;
            this.nodeId = nodeId;
            this.message = message;
        }
    }

    /**
     * Walk the graph from the start node following routing rules + edges,
     * collecting every approval node the workflow will visit given the
     * current condition context. Returns a JSONArray of node objects with
     * {@code nodeIndex} rewritten in visit order (0, 1, 2, ...).
     *
     * <p>Stops at the first end node or when the next nodeId is null / cyclic.
     * Gateway nodes are visited but not included in the output (they are
     * routing logic, not approval work).
     */
    private JSONArray materializeGraphToFlatPath(WorkflowGraph graph, Map<String, Object> ctx) {
        JSONArray out = new JSONArray();
        if (graph == null || !graph.valid) return out;

        String startId = graph.nodes.values().stream()
                .filter(n -> "start".equals(n.getStr("nodeType")))
                .map(n -> n.getStr("nodeId"))
                .findFirst().orElse(null);
        if (startId == null) return out;

        java.util.Set<String> visited = new java.util.HashSet<>();
        String currentId = startId;
        int outIndex = 0;
        int safetyLimit = 64; // path length cap

        while (currentId != null && safetyLimit-- > 0) {
            if (visited.contains(currentId)) break;
            visited.add(currentId);
            JSONObject current = graph.nodes.get(currentId);
            if (current == null) break;
            String type = current.getStr("nodeType");

            if ("approval".equals(type)) {
                JSONObject flat = new JSONObject();
                flat.set("nodeIndex", outIndex++);
                flat.set("nodeId", currentId);
                flat.set("nodeName", current.getStr("nodeName", current.getStr("name")));
                flat.set("nodeType", "approval");
                flat.set("assigneeType", current.getStr("assigneeType"));
                flat.set("assigneeValue", current.getStr("assigneeValue"));
                flat.set("multiType", current.getStr("multiType"));
                flat.set("multiAssigneeIds", current.getJSONArray("multiAssigneeIds"));
                flat.set("conditions", current.getJSONArray("conditions"));
                flat.set("ccList", current.getJSONArray("ccList"));
                flat.set("timeoutHours", current.getInt("timeoutHours", 0));
                flat.set("timeoutAction", current.getStr("timeoutAction", "notify_only"));
                out.add(flat);
            } else if ("end".equals(type)) {
                break;
            }

            currentId = findNextNode(graph, currentId, ctx);
        }
        return out;
    }

    /**
     * Parse a nodeConfig string into a WorkflowGraph. The string may be either
     * legacy flat JSONArray (returns a graph with schemaVersion=1 and empty
     * edges/nodes maps) or graph format (schemaVersion=2). Validation is
     * performed during parsing; check {@link WorkflowGraph#valid} before use.
     */
    public WorkflowGraph parseNodeConfig(String nodeConfig) {
        if (nodeConfig == null || nodeConfig.isBlank()) {
            return new WorkflowGraph(1, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(),
                    java.util.Collections.singletonList(new ValidationError("empty", null, "nodeConfig 为空")));
        }

        String trimmed = nodeConfig.trim();
        // Legacy flat array
        if (!trimmed.startsWith("{")) {
            // Validate parseability
            try {
                JSONUtil.parseArray(trimmed);
            } catch (Exception e) {
                return new WorkflowGraph(1, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(),
                        java.util.Collections.singletonList(new ValidationError("parse_error", null, "扁平格式 JSON 解析失败: " + e.getMessage())));
            }
            return new WorkflowGraph(1, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>());
        }

        // Graph format
        JSONObject config;
        try {
            config = JSONUtil.parseObj(trimmed);
        } catch (Exception e) {
            return new WorkflowGraph(2, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(),
                    java.util.Collections.singletonList(new ValidationError("parse_error", null, "图格式 JSON 解析失败: " + e.getMessage())));
        }
        int schemaVersion = config.getInt("schemaVersion", 2);
        JSONArray nodesArr = config.getJSONArray("nodes");
        JSONArray edgesArr = config.getJSONArray("edges");

        Map<String, JSONObject> nodes = new LinkedHashMap<>();
        Map<String, List<JSONObject>> outgoing = new LinkedHashMap<>();
        List<JSONObject> edges = new ArrayList<>();
        List<ValidationError> errors = new ArrayList<>();

        if (nodesArr == null) {
            errors.add(new ValidationError("no_nodes", null, "图格式缺少 nodes 数组"));
        } else {
            for (int i = 0; i < nodesArr.size(); i++) {
                JSONObject n = nodesArr.getJSONObject(i);
                String id = n.getStr("nodeId");
                if (id == null || id.isBlank()) {
                    errors.add(new ValidationError("missing_node_id", null, "第 " + i + " 个节点缺少 nodeId"));
                    continue;
                }
                if (nodes.containsKey(id)) {
                    errors.add(new ValidationError("duplicate_node_id", id, "节点 ID 重复: " + id));
                }
                nodes.put(id, n);
                outgoing.computeIfAbsent(id, k -> new ArrayList<>());
            }
        }

        if (edgesArr != null) {
            for (int i = 0; i < edgesArr.size(); i++) {
                JSONObject e = edgesArr.getJSONObject(i);
                String source = e.getStr("source");
                String target = e.getStr("target");
                if (source == null || !nodes.containsKey(source)) {
                    errors.add(new ValidationError("unknown_edge_endpoint", source, "边的 source 不存在: " + source));
                }
                if (target == null || !nodes.containsKey(target)) {
                    errors.add(new ValidationError("unknown_edge_endpoint", target, "边的 target 不存在: " + target));
                }
                if (source != null && target != null) {
                    outgoing.computeIfAbsent(source, k -> new ArrayList<>()).add(e);
                    edges.add(e);
                }
            }
        }

        // At least one start and one end
        boolean hasStart = nodes.values().stream().anyMatch(n -> "start".equals(n.getStr("nodeType")));
        boolean hasEnd = nodes.values().stream().anyMatch(n -> "end".equals(n.getStr("nodeType")));
        if (!hasStart) errors.add(new ValidationError("no_start", null, "图缺少开始节点 (nodeType=start)"));
        if (!hasEnd) errors.add(new ValidationError("no_end", null, "图缺少结束节点 (nodeType=end)"));

        // Cycle detection via DFS from start
        if (hasStart) {
            String startId = nodes.values().stream()
                    .filter(n -> "start".equals(n.getStr("nodeType")))
                    .findFirst().get().getStr("nodeId");
            java.util.Set<String> visiting = new java.util.HashSet<>();
            java.util.Set<String> visited = new java.util.HashSet<>();
            if (hasCycle(startId, outgoing, visiting, visited)) {
                errors.add(new ValidationError("cycle", startId, "图存在环，可能导致死循环"));
            }
        }

        // Per-approval-node: assigneeType + assigneeValue required
        for (JSONObject n : nodes.values()) {
            if ("approval".equals(n.getStr("nodeType"))) {
                String t = n.getStr("assigneeType");
                String v = n.getStr("assigneeValue");
                if (t == null || t.isBlank()) {
                    errors.add(new ValidationError("missing_assignee_type", n.getStr("nodeId"),
                            "审批节点缺少 assigneeType"));
                }
                if (v == null || v.isBlank()) {
                    errors.add(new ValidationError("missing_assignee_value", n.getStr("nodeId"),
                            "审批节点缺少 assigneeValue"));
                }
            }
        }

        return new WorkflowGraph(schemaVersion, nodes, outgoing, edges, errors);
    }

    /** DFS cycle detection. */
    private boolean hasCycle(String startId,
                             Map<String, List<JSONObject>> outgoing,
                             java.util.Set<String> visiting,
                             java.util.Set<String> visited) {
        if (visited.contains(startId)) return false;
        if (visiting.contains(startId)) return true;
        visiting.add(startId);
        List<JSONObject> edges = outgoing.getOrDefault(startId, java.util.Collections.emptyList());
        for (JSONObject e : edges) {
            String target = e.getStr("target");
            if (target != null && hasCycle(target, outgoing, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(startId);
        visited.add(startId);
        return false;
    }

    /**
     * Resolve the next nodeId for a given current nodeId using routing rules
     * and gateway branches. Returns null if the current node has no outgoing
     * edge (i.e. process terminates). Routing precedence:
     *   1. node-level routingRules (skipTo / jumpTo) — first match wins
     *   2. gateway branches (if current node is a gateway)
     *   3. first outgoing edge whose condition evaluates true (no condition = always)
     *   4. first outgoing edge unconditional fallback
     */
    public String findNextNode(WorkflowGraph graph, String currentNodeId, Map<String, Object> ctx) {
        if (graph == null || !graph.valid || currentNodeId == null) return null;
        JSONObject current = graph.nodes.get(currentNodeId);
        if (current == null) return null;

        // 1. Node-level routing rules
        JSONArray rules = current.getJSONArray("routingRules");
        if (rules != null && ctx != null) {
            for (int i = 0; i < rules.size(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                if (evaluateRoutingRule(rule, ctx)) {
                    String skipTo = rule.getStr("skipTo");
                    if (skipTo != null && graph.nodes.containsKey(skipTo)) return skipTo;
                    String jumpTo = rule.getStr("jumpTo");
                    if (jumpTo != null && graph.nodes.containsKey(jumpTo)) return jumpTo;
                }
            }
        }

        // 2. Gateway branches
        if ("gateway".equals(current.getStr("nodeType"))) {
            return pickGatewayBranch(current, graph, ctx);
        }

        // 3 & 4. Outgoing edges
        List<JSONObject> edges = graph.outgoing.getOrDefault(currentNodeId, java.util.Collections.emptyList());
        for (JSONObject e : edges) {
            if (evaluateEdgeCondition(e, ctx)) {
                return e.getStr("target");
            }
        }
        // 4. Unconditional fallback
        if (!edges.isEmpty()) {
            return edges.get(0).getStr("target");
        }
        return null;
    }

    /**
     * Evaluate a routing rule against the condition context. The rule format is:
     *   { "when": "context.amount > 1000", "skipTo": "n_finance", "jumpTo": "n_gm" }
     * Reuses the same operator set as evaluateConditions().
     */
    private boolean evaluateRoutingRule(JSONObject rule, Map<String, Object> ctx) {
        String when = rule.getStr("when");
        if (when == null || when.isBlank()) return false;
        return evaluateExpression(when, ctx);
    }

    /** Parse and evaluate a boolean expression. Supports && / || plus a small
     *  comparison operator set: ==, !=, <, <=, >, >=. Anything else returns false. */
    private boolean evaluateExpression(String expr, Map<String, Object> ctx) {
        if (ctx == null || ctx.isEmpty()) return false;
        if (expr == null || expr.isBlank()) return false;

        List<String> orParts = splitExpression(expr, "||");
        if (orParts.size() > 1) {
            for (String part : orParts) {
                if (evaluateExpression(part, ctx)) return true;
            }
            return false;
        }

        List<String> andParts = splitExpression(expr, "&&");
        if (andParts.size() > 1) {
            for (String part : andParts) {
                if (!evaluateExpression(part, ctx)) return false;
            }
            return true;
        }

        String[] operators = {"<=", ">=", "==", "!=", "<", ">"};
        for (String op : operators) {
            int idx = expr.indexOf(op);
            if (idx <= 0) continue;
            String lhs = expr.substring(0, idx).trim();
            String rhs = expr.substring(idx + op.length()).trim();
            Object lhsVal = resolveContextPath(lhs, ctx);
            Object rhsVal = tryParseLiteral(rhs);
            if (lhsVal == null || rhsVal == null) return false;
            return compareNumeric(lhsVal, rhsVal, op);
        }
        return false;
    }

    private List<String> splitExpression(String expr, String delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i <= expr.length() - delimiter.length(); i++) {
            char ch = expr.charAt(i);
            if ((ch == '"' || ch == '\'') && (i == 0 || expr.charAt(i - 1) != '\\')) {
                if (!inQuote) {
                    inQuote = true;
                    quote = ch;
                } else if (quote == ch) {
                    inQuote = false;
                    quote = 0;
                }
            }
            if (!inQuote && expr.startsWith(delimiter, i)) {
                parts.add(expr.substring(start, i).trim());
                i += delimiter.length() - 1;
                start = i + 1;
            }
        }
        if (start == 0) return java.util.Collections.singletonList(expr.trim());
        parts.add(expr.substring(start).trim());
        return parts;
    }

    /** Resolve a flat field ("amount") or legacy path ("context.amount") in the ctx map. */
    private Object resolveContextPath(String path, Map<String, Object> ctx) {
        if (path == null || ctx == null) return null;
        if (ctx.containsKey(path)) return ctx.get(path);
        if (path.startsWith("context.")) {
            String flatKey = path.substring("context.".length());
            if (ctx.containsKey(flatKey)) return ctx.get(flatKey);
        }
        String[] parts = path.split("\\.");
        Object cur = ctx;
        for (String p : parts) {
            if (cur instanceof Map) {
                cur = ((Map<?, ?>) cur).get(p);
            } else {
                return null;
            }
        }
        return cur;
    }

    /** Try parsing a literal: number, boolean, or quoted string. */
    private Object tryParseLiteral(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        if (s.equals("true")) return Boolean.TRUE;
        if (s.equals("false")) return Boolean.FALSE;
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private boolean compareNumeric(Object lhs, Object rhs, String op) {
        double l = toDouble(lhs);
        double r = toDouble(rhs);
        switch (op) {
            case "==": return l == r;
            case "!=": return l != r;
            case "<":  return l <  r;
            case "<=": return l <= r;
            case ">":  return l >  r;
            case ">=": return l >= r;
            default:   return false;
        }
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (NumberFormatException e) { return Double.NaN; }
    }

    /** Pick the next node from a gateway's branches based on condition evaluation. */
    private String pickGatewayBranch(JSONObject gateway, WorkflowGraph graph, Map<String, Object> ctx) {
        JSONArray branches = gateway.getJSONArray("branches");
        if (branches != null) {
            for (int i = 0; i < branches.size(); i++) {
                JSONObject b = branches.getJSONObject(i);
                if (evaluateRoutingRule(b, ctx)) {
                    String to = b.getStr("to");
                    if (to != null && graph.nodes.containsKey(to)) return to;
                }
            }
        }
        // Fallback: first outgoing edge
        String gid = gateway.getStr("nodeId");
        List<JSONObject> edges = graph.outgoing.getOrDefault(gid, java.util.Collections.emptyList());
        if (!edges.isEmpty()) return edges.get(0).getStr("target");
        return null;
    }
}
