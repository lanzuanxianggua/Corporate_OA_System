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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
            nodes = JSONUtil.parseArray(definition.getNodeConfig());
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

        if (!"0".equals(task.getStatus())) {
            throw new BusinessException("任务已处理");
        }

        task.setStatus(String.valueOf(status));
        task.setActionTime(LocalDateTime.now());
        task.setRemark(remark);
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
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        record.setTaskId(taskId);
        record.setNodeName(task.getNodeName());
        record.setAssigneeName(approverName);
        approvalRecordMapper.insert(record);

        // Handle multi-person approval (countersign/orsign)
        boolean isMultiTask = task.getParentTaskId() != null || task.getMultiType() != null;
        if (isMultiTask && status == 1) {
            String multiType = task.getMultiType();
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
                        parentTask.setActionTime(LocalDateTime.now());
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
                    sibling.setActionTime(LocalDateTime.now());
                    sibling.setRemark("或签已由其他审批人处理");
                    taskMapper.updateById(sibling);
                }

                // Cancel parent task
                if (task.getParentTaskId() != null) {
                    WfTask parentTask = taskMapper.selectById(task.getParentTaskId());
                    if (parentTask != null && "0".equals(parentTask.getStatus())) {
                        parentTask.setStatus("1");
                        parentTask.setActionTime(LocalDateTime.now());
                        taskMapper.updateById(parentTask);
                    }
                }
                // Fall through to advance to next node
            }
        }

        WfProcessDefinition definition = this.getById(instance.getProcessId());
        JSONArray nodes = JSONUtil.parseArray(definition.getNodeConfig());

        if (status == 2) {
            // rejected
            handleRejection(task, instance, isMultiTask);
        } else if (status == 1) {
            // approved, check if more applicable nodes
            Map<String, Object> ctx = null;
            if (instance.getConditionContext() != null && !instance.getConditionContext().isEmpty()) {
                ctx = JSONUtil.parseObj(instance.getConditionContext()).toBean(Map.class);
            }
            advanceToNextNode(task, instance, nodes, ctx, remark);
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
                .orderByDesc(WfTask::getActionTime);
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
        String assigneeValue = node.getStr("assigneeValue");
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
            parentTask.setProcessId(instance.getProcessId());
            parentTask.setNodeIndex(nodeIndex);
            parentTask.setNodeName(nodeName);
            parentTask.setAssigneeId(allAssigneeIds.get(0));
            parentTask.setStatus("0");
            parentTask.setActionSource("assignee");
            parentTask.setMultiType(multiType);
            parentTask.setCreateTime(LocalDateTime.now());
            Integer timeoutHours = node.getInt("timeoutHours", 0);
            if (timeoutHours != null && timeoutHours > 0) {
                parentTask.setDeadline(LocalDateTime.now().plusHours(timeoutHours));
            }
            parentTask.setRemindCount(0);
            taskMapper.insert(parentTask);
            parentTaskId = parentTask.getId();

            // Create child tasks for all assignees (including primary)
            for (Long assigneeId : allAssigneeIds) {
                WfTask childTask = new WfTask();
                childTask.setInstanceId(instance.getId());
                childTask.setProcessId(instance.getProcessId());
                childTask.setNodeIndex(nodeIndex);
                childTask.setNodeName(nodeName);
                childTask.setAssigneeId(assigneeId);
                childTask.setStatus("0");
                childTask.setActionSource("assignee");
                childTask.setMultiType(multiType);
                childTask.setParentTaskId(parentTaskId);
                childTask.setCreateTime(LocalDateTime.now());
                if (timeoutHours != null && timeoutHours > 0) {
                    childTask.setDeadline(LocalDateTime.now().plusHours(timeoutHours));
                }
                childTask.setRemindCount(0);
                taskMapper.insert(childTask);

                // CC, todo, notification for each assignee
                createCcAndTodoForTask(node, instance, childTask, assigneeId);
            }
        } else {
            // Single approver
            WfTask task = new WfTask();
            task.setInstanceId(instance.getId());
            task.setProcessId(instance.getProcessId());
            task.setNodeIndex(nodeIndex);
            task.setNodeName(nodeName);
            task.setAssigneeId(primaryAssigneeId);
            task.setStatus("0");
            task.setActionSource("assignee");
            task.setCreateTime(LocalDateTime.now());

            Integer timeoutHours = node.getInt("timeoutHours", 0);
            if (timeoutHours != null && timeoutHours > 0) {
                task.setDeadline(LocalDateTime.now().plusHours(timeoutHours));
            }
            task.setRemindCount(0);
            taskMapper.insert(task);

            createCcAndTodoForTask(node, instance, task, primaryAssigneeId);
        }
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
            default:
                try { return Long.valueOf(assigneeValue); }
                catch (NumberFormatException e) { return resolveByRole(assigneeValue, initiatorId); }
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
                case "==": if (!numCheck(rawVal, thresholdObj, (a, t) -> a == t)) return false; break;
                case "!=": if (!numCheck(rawVal, thresholdObj, (a, t) -> a != t)) return false; break;
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
                sibling.setActionTime(LocalDateTime.now());
                taskMapper.updateById(sibling);
            }
            if (task.getParentTaskId() != null) {
                WfTask parentTask = taskMapper.selectById(task.getParentTaskId());
                if (parentTask != null && "0".equals(parentTask.getStatus())) {
                    parentTask.setStatus("2");
                    parentTask.setActionTime(LocalDateTime.now());
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
                instance.getBusinessId(), "rejected", task.getRemark());
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
            if (applicableNodes.get(i).getInt("nodeIndex", -1) == task.getNodeIndex()) {
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
                    nextNode.getStr("assigneeValue"), instance.getInitiatorId());
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
            t.setActionTime(LocalDateTime.now());
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
        task.setActionTime(LocalDateTime.now());
        task.setTransferFromId(fromAssigneeId);
        task.setTransferReason(reason);
        taskMapper.updateById(task);

        // Create new task
        WfTask newTask = new WfTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setProcessId(task.getProcessId());
        newTask.setNodeIndex(task.getNodeIndex());
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
        task.setLastRemindTime(LocalDateTime.now());
        taskMapper.updateById(task);

        todoService.addTodo(task.getAssigneeId(), "催办提醒: 审批任务待处理", "approval", instanceId, instance.getBusinessType());
    }

    @Override
    public List<WfTask> getApprovalHistory(String businessType, Long businessId) {
        WfProcessInstance instance = getByBusiness(businessType, businessId);
        if (instance == null) return Collections.emptyList();
        LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTask::getInstanceId, instance.getId())
               .orderByAsc(WfTask::getNodeIndex);
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
            t.setActionTime(LocalDateTime.now());
            t.setRemark("流程退回");
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

        WfProcessDefinition definition = this.getById(instance.getProcessId());
        JSONArray nodes = JSONUtil.parseArray(definition.getNodeConfig());
        Map<String, Object> ctx = null;
        if (instance.getConditionContext() != null && !instance.getConditionContext().isEmpty()) {
            ctx = JSONUtil.parseObj(instance.getConditionContext()).toBean(Map.class);
        }
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
                targetNode.getStr("assigneeValue"), instance.getInitiatorId());
        notificationService.notifyTask(targetAssigneeId, instance.getBusinessType(),
                instance.getBusinessId(), "退回的审批任务需要重新处理");

        completeTodo(instance.getId());
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

        JSONArray subNodes = JSONUtil.parseArray(subDef.getNodeConfig());
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

        JSONArray allNodes = JSONUtil.parseArray(definition.getNodeConfig());
        Map<String, Object> ctx = null;
        if (parent.getConditionContext() != null && !parent.getConditionContext().isEmpty()) {
            ctx = JSONUtil.parseObj(parent.getConditionContext()).toBean(Map.class);
        }
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
}
