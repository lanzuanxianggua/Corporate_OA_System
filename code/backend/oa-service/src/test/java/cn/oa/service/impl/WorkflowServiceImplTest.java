package cn.oa.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("workflow test")
class WorkflowServiceImplTest {

    @Mock private WfProcessDefinitionMapper definitionMapper;
    @Mock private WfProcessInstanceMapper instanceMapper;
    @Mock private WfTaskMapper taskMapper;
    @Mock private SysEmployeeMapper employeeMapper;
    @Mock private SysEmpRoleMapper empRoleMapper;
    @Mock private SysRoleMapper roleMapper;
    @Mock private TodoService todoService;
    @Mock private WorkflowCallbackDispatcher callbackDispatcher;
    @Mock private OaApprovalRecordMapper approvalRecordMapper;
    @Mock private WfCcRecordMapper ccRecordMapper;
    @Mock private DelegationService delegationService;
    @Mock private NotificationService notificationService;
    @Mock private DeptService deptService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @Captor private ArgumentCaptor<WfProcessInstance> instanceCaptor;
    @Captor private ArgumentCaptor<WfTask> taskCaptor;

    private final String businessType = "leave";
    private final Long businessId = 100L;
    private final Long initiatorId = 1L;
    private final Long approverId = 2L;

    private WfProcessDefinition createDefinition(String nodeConfigJson) {
        WfProcessDefinition def = new WfProcessDefinition();
        def.setId(10L);
        def.setProcessType(businessType);
        def.setProcessKey("leave_process");
        def.setNodeConfig(nodeConfigJson);
        def.setStatus("0");
        def.setVersion(1);
        return def;
    }

    private WfProcessInstance createRunningInstance() {
        WfProcessInstance inst = new WfProcessInstance();
        inst.setId(200L);
        inst.setProcessId(10L);
        inst.setBusinessType(businessType);
        inst.setBusinessId(businessId);
        inst.setInitiatorId(initiatorId);
        inst.setCurrentNode(0);
        inst.setStatus("0");
        return inst;
    }

    private WfTask createPendingTask(Long instanceId, Long assigneeId, int runtimeIndex) {
        WfTask task = new WfTask();
        task.setId(300L);
        task.setInstanceId(instanceId);
        task.setNodeId((long) runtimeIndex);
        task.setNodeName("缁忕悊瀹℃壒");
        task.setAssigneeId(assigneeId);
        task.setStatus("0");
        task.setCreateTime(LocalDateTime.now());
        return task;
    }

    private String v2SingleApproverConfig() {
        return "{\"schemaVersion\":2,\"nodes\":["
                + "{\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},"
                + "{\"nodeId\":\"n1\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"specific\",\"assigneeValue\":\"2\"},"
                + "{\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}"
                + "],\"edges\":[{\"source\":\"start\",\"target\":\"n1\"},{\"source\":\"n1\",\"target\":\"end\"}]}";
    }

    private String v2ConditionalApproverConfig() {
        return "{\"schemaVersion\":2,\"nodes\":["
                + "{\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},"
                + "{\"nodeId\":\"n1\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"specific\",\"assigneeValue\":\"2\","
                + "\"conditions\":[{\"field\":\"amount\",\"operator\":\">\",\"value\":1000}]},"
                + "{\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}"
                + "],\"edges\":[{\"source\":\"start\",\"target\":\"n1\"},{\"source\":\"n1\",\"target\":\"end\"}]}";
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        // No delegation by default
        lenient().when(delegationService.resolveDelegate(anyLong())).thenReturn(null);
        // Set baseMapper for CrudRepository parent class via reflection (@InjectMocks doesn't handle it)
        Field baseMapperField = com.baomidou.mybatisplus.extension.repository.CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(workflowService, definitionMapper);
    }

    // ==================== startProcess ====================

    @Test
    @DisplayName("workflow test")
    void startProcess_Success() {
        String nodeConfig = v2SingleApproverConfig();
        WfProcessDefinition def = createDefinition(nodeConfig);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(def);
        when(instanceMapper.insert(any(WfProcessInstance.class))).thenReturn(1);
        when(taskMapper.insert(any(WfTask.class))).thenReturn(1);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getInitiatorId()).isEqualTo(initiatorId);
        assertThat(result.getBusinessType()).isEqualTo(businessType);
        verify(instanceMapper).insert(any(WfProcessInstance.class));
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeId()).isEqualTo(approverId);
    }

    @Test
    @DisplayName("workflow test")
    void startProcess_NoDefinition_AutoApproved() {
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("1");
        assertThat(result.getProcessId()).isNull();
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void startProcess_NodeParseError_AutoApproved() {
        WfProcessDefinition def = createDefinition("{invalid json}");
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(def);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId);

        assertThat(result.getStatus()).isEqualTo("1");
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void startProcess_EmptyNodes_AutoApproved() {
        WfProcessDefinition def = createDefinition("[]");
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(def);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId);

        assertThat(result.getStatus()).isEqualTo("1");
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void startProcess_WithConditionContext() {
        String nodeConfig = v2ConditionalApproverConfig();
        WfProcessDefinition def = createDefinition(nodeConfig);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(def);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", 2000);

        when(instanceMapper.insert(any(WfProcessInstance.class))).thenReturn(1);
        when(taskMapper.insert(any(WfTask.class))).thenReturn(1);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId, ctx);

        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getConditionContext()).contains("\"amount\":2000");
    }

    @Test
    @DisplayName("workflow test")
    void startProcess_NoApplicableNodes_AutoApproved() {
        String nodeConfig = v2ConditionalApproverConfig();
        WfProcessDefinition def = createDefinition(nodeConfig);
        when(definitionMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(def);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", 500);

        WfProcessInstance result = workflowService.startProcess(businessType, businessId, initiatorId, ctx);

        assertThat(result.getStatus()).isEqualTo("1");
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    // ==================== handleTask ====================

    @Test
    @DisplayName("workflow test")
    void handleTask_Approve_SingleApprover() {
        WfProcessDefinition def = createDefinition(v2SingleApproverConfig());
        WfProcessInstance inst = createRunningInstance();
        WfTask task = createPendingTask(inst.getId(), approverId, 0);

        when(taskMapper.selectById(task.getId())).thenReturn(task);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));

        workflowService.handleTask(task.getId(), approverId, 1, "鍚屾剰");

        verify(taskMapper).updateById(argThat((WfTask t) -> "1".equals(t.getStatus())));
        verify(instanceMapper).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getStatus()).isEqualTo("1");
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Reject_SingleApprover() {
        WfProcessDefinition def = createDefinition(v2SingleApproverConfig());
        WfProcessInstance inst = createRunningInstance();
        WfTask task = createPendingTask(inst.getId(), approverId, 0);

        when(taskMapper.selectById(task.getId())).thenReturn(task);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));

        workflowService.handleTask(task.getId(), approverId, 2, "reject");

        verify(taskMapper).updateById(argThat((WfTask t) -> "2".equals(t.getStatus())));
        verify(instanceMapper).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getStatus()).isEqualTo("2");
        verify(callbackDispatcher).onRejected(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Reject_NoRemark_Throws() {
        assertThatThrownBy(() -> workflowService.handleTask(1L, approverId, 2, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_TaskNotFound_Throws() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> workflowService.handleTask(999L, approverId, 1, "鍚屾剰"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Unauthorized_Throws() {
        Long otherUserId = 99L;
        WfTask task = createPendingTask(200L, approverId, 0);
        task.setInstanceId(200L);

        when(taskMapper.selectById(task.getId())).thenReturn(task);
        // No delegation, no admin role (resolveDelegate defaults to null from setUp)
        when(delegationService.findActiveDelegationForDelegate(otherUserId)).thenReturn(null);
        when(empRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> workflowService.handleTask(task.getId(), otherUserId, 1, "鍚屾剰"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_AlreadyHandled_Throws() {
        WfTask task = createPendingTask(200L, approverId, 0);
        task.setStatus("1");
        when(taskMapper.selectById(task.getId())).thenReturn(task);

        assertThatThrownBy(() -> workflowService.handleTask(task.getId(), approverId, 1, "鍚屾剰"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Countersign_StillPending() {
        WfProcessInstance inst = createRunningInstance();
        WfTask childTask = createPendingTask(inst.getId(), approverId, 0);
        Long parentTaskId = 301L;
        childTask.setId(302L);
        childTask.setParentTaskId(parentTaskId);
        childTask.setTaskType("countersign");

        WfTask parent = createPendingTask(inst.getId(), approverId, 0);
        parent.setId(parentTaskId);
        parent.setTaskType("countersign");
        parent.setStatus("1");

        when(taskMapper.selectById(childTask.getId())).thenReturn(childTask);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));
        // Still has pending sibling
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        workflowService.handleTask(childTask.getId(), approverId, 1, "鍚屾剰");

        // Task approved but doesn't advance - still waiting for sibling
        verify(callbackDispatcher, never()).onApproved(anyString(), anyLong());
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Countersign_AllApproved() {
        WfProcessInstance inst = createRunningInstance();
        WfTask childTask = createPendingTask(inst.getId(), approverId, 0);
        Long parentTaskId = 301L;
        childTask.setId(302L);
        childTask.setParentTaskId(parentTaskId);
        childTask.setTaskType("countersign");

        WfTask parent = createPendingTask(inst.getId(), approverId, 0);
        parent.setId(parentTaskId);
        parent.setTaskType("countersign");
        parent.setStatus("1");

        WfProcessDefinition def = createDefinition(v2SingleApproverConfig());

        when(taskMapper.selectById(childTask.getId())).thenReturn(childTask);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(taskMapper.selectById(parentTaskId)).thenReturn(parent);
        when(definitionMapper.selectById(anyLong())).thenReturn(def);

        workflowService.handleTask(childTask.getId(), approverId, 1, "鍚屾剰");

        // Parent task marked as complete
        verify(taskMapper).updateById(argThat((WfTask t) -> t.getId() != null && "1".equals(t.getStatus())));
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void handleTask_Orsign_FirstApproval() {
        WfProcessInstance inst = createRunningInstance();
        WfTask childTask = createPendingTask(inst.getId(), approverId, 0);
        Long parentTaskId = 301L;
        childTask.setId(302L);
        childTask.setParentTaskId(parentTaskId);
        childTask.setTaskType("orsign");

        WfTask parent = createPendingTask(inst.getId(), approverId, 0);
        parent.setId(parentTaskId);
        parent.setTaskType("orsign");
        parent.setStatus("1");

        WfTask sibling = createPendingTask(inst.getId(), 3L, 0);
        sibling.setId(303L);
        sibling.setParentTaskId(parentTaskId);
        sibling.setTaskType("orsign");
        sibling.setStatus("0");

        WfProcessDefinition def = createDefinition(v2SingleApproverConfig());

        when(taskMapper.selectById(childTask.getId())).thenReturn(childTask);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(sibling));
        when(taskMapper.selectById(parentTaskId)).thenReturn(parent);
        when(definitionMapper.selectById(anyLong())).thenReturn(def);

        workflowService.handleTask(childTask.getId(), approverId, 1, "鍚屾剰");

        // Sibling should be canceled
        verify(taskMapper).updateById(argThat((WfTask t) -> 303L == t.getId() && "4".equals(t.getStatus())));
        // Process should advance
        verify(callbackDispatcher).onApproved(businessType, businessId);
    }

    // ==================== findPendingTask ====================

    @Test
    @DisplayName("workflow test")
    void findPendingTask_DirectMatch() {
        WfProcessInstance inst = createRunningInstance();
        inst.setId(200L);
        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);

        WfTask task = createPendingTask(200L, approverId, 0);
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        WfTask result = workflowService.findPendingTask(businessType, businessId, approverId);

        assertThat(result).isNotNull();
        assertThat(result.getAssigneeId()).isEqualTo(approverId);
    }

    @Test
    @DisplayName("workflow test")
    void findPendingTask_AdminReturnsAnyPendingTask() {
        Long adminId = 1L;
        WfProcessInstance inst = createRunningInstance();
        inst.setId(200L);
        WfTask task = createPendingTask(200L, approverId, 0);

        SysEmpRole empRole = new SysEmpRole();
        empRole.setEmpId(adminId);
        empRole.setRoleId(10L);
        SysRole adminRole = new SysRole();
        adminRole.setId(10L);
        adminRole.setRoleKey("ADMIN");

        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);
        when(empRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(empRole));
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(adminRole));
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        WfTask result = workflowService.findPendingTask(businessType, businessId, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(task.getId());
        assertThat(result.getAssigneeId()).isEqualTo(approverId);
    }

    @Test
    @DisplayName("workflow test")
    void findPendingTask_NoInstance_ReturnsNull() {
        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        WfTask result = workflowService.findPendingTask(businessType, businessId, approverId);

        assertThat(result).isNull();
    }

    // ==================== myPendingTasks ====================

    @Test
    @DisplayName("workflow test")
    void myPendingTasks_ReturnsPage() {
        WfTask task = createPendingTask(200L, approverId, 0);
        WfProcessInstance inst = createRunningInstance();
        inst.setId(200L);
        task.setInstance(inst);
        Page<WfTask> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(task));
        page.setTotal(1);

        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(instanceMapper.selectById(200L)).thenReturn(inst);

        IPage<WfTask> result = workflowService.myPendingTasks(approverId, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getBusinessType()).isEqualTo(businessType);
    }

    @Test
    @DisplayName("workflow test")
    void myPendingTasks_AdminReturnsAllPendingTasks() {
        Long adminId = 1L;
        WfTask task = createPendingTask(200L, approverId, 0);
        WfProcessInstance inst = createRunningInstance();
        inst.setId(200L);
        Page<WfTask> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(task));
        page.setTotal(1);

        SysEmpRole empRole = new SysEmpRole();
        empRole.setEmpId(adminId);
        empRole.setRoleId(10L);
        SysRole adminRole = new SysRole();
        adminRole.setId(10L);
        adminRole.setRoleKey("ADMIN");

        when(empRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(empRole));
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(adminRole));
        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(instanceMapper.selectById(200L)).thenReturn(inst);

        IPage<WfTask> result = workflowService.myPendingTasks(adminId, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getAssigneeId()).isEqualTo(approverId);
    }

    // ==================== myHandledTasks ====================

    @Test
    @DisplayName("workflow test")
    void myHandledTasks_ReturnsPage() {
        WfTask task = createPendingTask(200L, approverId, 0);
        task.setStatus("1");
        task.setCompleteTime(LocalDateTime.now());
        WfProcessInstance inst = createRunningInstance();
        inst.setId(200L);
        task.setInstance(inst);
        Page<WfTask> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(task));
        page.setTotal(1);

        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(instanceMapper.selectById(200L)).thenReturn(inst);

        IPage<WfTask> result = workflowService.myHandledTasks(approverId, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("1");
    }

    // ==================== withdrawProcess ====================

    @Test
    @DisplayName("workflow test")
    void withdrawProcess_Success() {
        WfProcessInstance inst = createRunningInstance();
        WfTask pendingTask = createPendingTask(inst.getId(), approverId, 0);

        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(pendingTask));
        when(employeeMapper.selectById(initiatorId)).thenReturn(createEmployee(initiatorId, "initiator"));

        workflowService.withdrawProcess(businessType, businessId, initiatorId);

        assertThat(inst.getStatus()).isEqualTo("3");
        verify(taskMapper).updateById(argThat((WfTask t) -> "4".equals(t.getStatus())));
        verify(instanceMapper).updateById(inst);
        verify(callbackDispatcher).onWithdrawn(businessType, businessId);
    }

    @Test
    @DisplayName("workflow test")
    void withdrawProcess_NotInitiator_Throws() {
        WfProcessInstance inst = createRunningInstance();
        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);

        assertThatThrownBy(() -> workflowService.withdrawProcess(businessType, businessId, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    // ==================== transferTask ====================

    @Test
    @DisplayName("workflow test")
    void transferTask_Success() {
        Long toAssigneeId = 3L;
        WfTask task = createPendingTask(200L, approverId, 0);
        WfProcessInstance inst = createRunningInstance();

        when(taskMapper.selectById(task.getId())).thenReturn(task);
        when(instanceMapper.selectById(200L)).thenReturn(inst);

        workflowService.transferTask(task.getId(), approverId, toAssigneeId, "宸ヤ綔璋冩暣");

        verify(taskMapper).updateById(argThat((WfTask t) -> "3".equals(t.getStatus())));
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeId()).isEqualTo(toAssigneeId);
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo("0");
        verify(todoService).addTodo(eq(toAssigneeId), anyString(), eq("approval"), anyLong(), anyString());
    }

    @Test
    @DisplayName("workflow test")
    void transferTask_Unauthorized_Throws() {
        WfTask task = createPendingTask(200L, approverId, 0);
        when(taskMapper.selectById(task.getId())).thenReturn(task);

        assertThatThrownBy(() -> workflowService.transferTask(task.getId(), 99L, 3L, "璋冩暣"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    // ==================== returnTask ====================

    @Test
    @DisplayName("workflow test")
    void returnTask_ToInitiator() {
        WfProcessInstance inst = createRunningInstance();
        WfTask task = createPendingTask(inst.getId(), approverId, 0);
        WfProcessDefinition def = createDefinition(v2SingleApproverConfig());

        when(taskMapper.selectById(task.getId())).thenReturn(task);
        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(definitionMapper.selectById(anyLong())).thenReturn(def);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));

        workflowService.returnTask(task.getId(), approverId, "initiator", "璇蜂慨鏀瑰悗閲嶆柊鎻愪氦");

        verify(instanceMapper).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getStatus()).isEqualTo("5");
        assertThat(instanceCaptor.getValue().getCurrentNode()).isEqualTo(-1);
    }

    @Test
    @DisplayName("workflow test")
    void returnTask_NoRemark_Throws() {
        assertThatThrownBy(() -> workflowService.returnTask(1L, approverId, "initiator", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    // ==================== getByBusiness / getApprovalHistory ====================

    @Test
    @DisplayName("workflow test")
    void getByBusiness_ReturnsInstance() {
        WfProcessInstance inst = createRunningInstance();
        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);

        WfProcessInstance result = workflowService.getByBusiness(businessType, businessId);

        assertThat(result).isNotNull();
        assertThat(result.getBusinessId()).isEqualTo(businessId);
    }

    @Test
    @DisplayName("workflow test")
    void getApprovalHistory_ReturnsTasks() {
        WfProcessInstance inst = createRunningInstance();
        WfTask task = createPendingTask(inst.getId(), approverId, 0);
        task.setStatus("1");

        when(instanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inst);
        when(taskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(task));
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));

        List<WfTask> history = workflowService.getApprovalHistory(businessType, businessId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAssigneeName()).isEqualTo("寮犱笁");
    }

    // ==================== saveDefinition ====================

    @Test
    @DisplayName("workflow test")
    void saveDefinition_CreateNew() {
        WfProcessDefinition def = new WfProcessDefinition();
        def.setProcessName("娴嬭瘯娴佺▼");
        def.setProcessType(businessType);
        def.setNodeConfig("[]");

        workflowService.saveDefinition(def);

        assertThat(def.getVersion()).isEqualTo(1);
        assertThat(def.getStatus()).isEqualTo("0");
        verify(definitionMapper).insert(def);
    }

    @Test
    @DisplayName("workflow test")
    void saveDefinition_UpdateVersion() {
        WfProcessDefinition existing = createDefinition("[]");
        existing.setVersion(2);

        WfProcessDefinition newDef = new WfProcessDefinition();
        newDef.setId(10L);
        newDef.setProcessName("娴嬭瘯娴佺▼");
        newDef.setProcessType(businessType);
        newDef.setNodeConfig("[]");

        when(definitionMapper.selectById(10L)).thenReturn(existing);

        workflowService.saveDefinition(newDef);

        // existing should be deactivated
        assertThat(existing.getStatus()).isEqualTo("1");
        // new def should be version 3
        assertThat(newDef.getVersion()).isEqualTo(3);
        verify(definitionMapper).updateById(existing);
        verify(definitionMapper).insert(newDef);
    }

    // ==================== urgeTask ====================

    @Test
    @DisplayName("workflow test")
    void urgeTask_Success() {
        WfProcessInstance inst = createRunningInstance();
        WfTask task = createPendingTask(inst.getId(), approverId, 0);

        when(instanceMapper.selectById(inst.getId())).thenReturn(inst);
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        workflowService.urgeTask(inst.getId(), initiatorId);

        assertThat(task.getRemindCount()).isEqualTo(1);
        verify(taskMapper).updateById(task);
        verify(todoService).addTodo(eq(approverId), anyString(), anyString(), anyLong(), anyString());
    }

    // ==================== listDefinitions ====================

    @Test
    @DisplayName("workflow test")
    void listDefinitions_ReturnsList() {
        WfProcessDefinition def = createDefinition("[]");
        when(definitionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(def));

        List<WfProcessDefinition> result = workflowService.listDefinitions();

        assertThat(result).hasSize(1);
    }

    // ==================== getCurrentTask ====================

    @Test
    @DisplayName("workflow test")
    void getCurrentTask_ReturnsPendingTask() {
        WfTask task = createPendingTask(200L, approverId, 0);
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        WfTask result = workflowService.getCurrentTask(200L);

        assertThat(result).isNotNull();
        assertThat(result.getAssigneeId()).isEqualTo(approverId);
    }

    // ==================== getApprovalChain ====================

    @Test
    @DisplayName("workflow test")
    void getApprovalChain_ReturnsRecords() {
        OaApprovalRecord record = new OaApprovalRecord();
        record.setId(1L);
        record.setApplyId(businessId);
        record.setBusinessType(businessType);
        record.setApproverId(approverId);
        record.setApproveStatus(1);

        when(approvalRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(record));
        when(employeeMapper.selectById(approverId)).thenReturn(createEmployee(approverId, "寮犱笁"));

        List<OaApprovalRecord> chain = workflowService.getApprovalChain(businessType, businessId);

        assertThat(chain).hasSize(1);
        assertThat(chain.get(0).getAssigneeName()).isEqualTo("寮犱笁");
    }

    // ==================== V1010: graph-format parse + validation ====================

    @Test
    @DisplayName("workflow test")
    void parseNodeConfig_ArrayDefinition_Rejected() {
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(
                "[{\"nodeName\":\"Node\",\"nodeType\":\"approval\",\"assigneeType\":\"role\",\"assigneeValue\":\"DEPT_MANAGER\"}]");

        assertThat(graph.schemaVersion).isEqualTo(0);
        assertThat(graph.isGraph()).isFalse();
        assertThat(graph.valid).isFalse();
        assertThat(graph.errors).isNotEmpty();
    }

    @Test
    @DisplayName("workflow test")
    void parseNodeConfig_GraphValid() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"n1\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"GM\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [{\"source\":\"start\",\"target\":\"n1\"},{\"source\":\"n1\",\"target\":\"end\"}]\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);

        assertThat(graph.isGraph()).isTrue();
        assertThat(graph.valid).isTrue();
        assertThat(graph.nodes).containsKeys("start", "n1", "end");
        assertThat(graph.outgoing.get("start")).hasSize(1);
    }

    @Test
    @DisplayName("workflow test")
    void parseNodeConfig_MissingStartEnd() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"n1\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"specific\",\"assigneeValue\":\"2\"}\n" +
                "  ],\n" +
                "  \"edges\": []\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);

        assertThat(graph.valid).isFalse();
        assertThat(graph.errors).extracting(e -> e.type).contains("no_start", "no_end");
    }

    @Test
    @DisplayName("workflow test")
    void parseNodeConfig_CycleDetected() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"a\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"specific\",\"assigneeValue\":\"2\"},\n" +
                "    {\"nodeId\":\"b\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"specific\",\"assigneeValue\":\"3\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"source\":\"start\",\"target\":\"a\"},\n" +
                "    {\"source\":\"a\",\"target\":\"b\"},\n" +
                "    {\"source\":\"b\",\"target\":\"a\"},\n" +
                "    {\"source\":\"b\",\"target\":\"end\"}\n" +
                "  ]\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);

        assertThat(graph.valid).isFalse();
        assertThat(graph.errors).extracting(e -> e.type).contains("cycle");
    }

    @Test
    @DisplayName("workflow test")
    void parseNodeConfig_UnknownEdgeEndpoint() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [{\"source\":\"start\",\"target\":\"ghost\"}]\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);

        assertThat(graph.valid).isFalse();
        assertThat(graph.errors).extracting(e -> e.type).contains("unknown_edge_endpoint");
    }

    @Test
    @DisplayName("workflow test")
    void findNextNode_RoutingRuleByAmount() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"n_dept\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"DEPT_MANAGER\",\n" +
                "     \"routingRules\":[{\"when\":\"context.amount > 10000\",\"skipTo\":\"n_director\"}]},\n" +
                "    {\"nodeId\":\"n_director\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"DIRECTOR\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"source\":\"start\",\"target\":\"n_dept\"},\n" +
                "    {\"source\":\"n_dept\",\"target\":\"end\"},\n" +
                "    {\"source\":\"n_director\",\"target\":\"end\"}\n" +
                "  ]\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);
        assertThat(graph.valid).isTrue();

        Map<String, Object> ctxSmall = new HashMap<>();
        ctxSmall.put("amount", 500.0);
        assertThat(workflowService.findNextNode(graph, "n_dept", ctxSmall)).isEqualTo("end");

        Map<String, Object> ctxBig = new HashMap<>();
        ctxBig.put("amount", 50000.0);
        assertThat(workflowService.findNextNode(graph, "n_dept", ctxBig)).isEqualTo("n_director");
    }

    @Test
    @DisplayName("workflow test")
    void findNextNode_RoutingRuleByAmountRange() {
        String cfg = "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"gw_amount\",\"nodeType\":\"gateway\",\"gatewayType\":\"exclusive\",\"nodeName\":\"Node\",\n" +
                "     \"branches\":[\n" +
                "       {\"when\":\"amount <= 5000\",\"to\":\"end\"},\n" +
                "       {\"when\":\"amount > 5000 && amount <= 50000\",\"to\":\"n_director\"},\n" +
                "       {\"when\":\"amount > 50000\",\"to\":\"n_gm\"}\n" +
                "     ]},\n" +
                "    {\"nodeId\":\"n_director\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"DIRECTOR\"},\n" +
                "    {\"nodeId\":\"n_gm\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"GM\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"source\":\"start\",\"target\":\"gw_amount\"},\n" +
                "    {\"source\":\"gw_amount\",\"target\":\"end\"},\n" +
                "    {\"source\":\"n_director\",\"target\":\"end\"},\n" +
                "    {\"source\":\"n_gm\",\"target\":\"end\"}\n" +
                "  ]\n" +
                "}";
        WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(cfg);
        assertThat(graph.valid).isTrue();

        Map<String, Object> ctxMid = new HashMap<>();
        ctxMid.put("amount", 10000.0);
        assertThat(workflowService.findNextNode(graph, "gw_amount", ctxMid)).isEqualTo("n_director");

        Map<String, Object> ctxHigh = new HashMap<>();
        ctxHigh.put("amount", 60000.0);
        assertThat(workflowService.findNextNode(graph, "gw_amount", ctxHigh)).isEqualTo("n_gm");
    }

    @Test
    @DisplayName("workflow test")
    void materializeGraphToRuntimePath_AmountTieredApprovalChain() {
        String cfg = amountTieredWorkflowConfig();

        assertThat(materializeApprovalNodeIds(cfg, Map.of("amount", 3000)))
                .containsExactly("n_manager");

        assertThat(materializeApprovalNodeIds(cfg, Map.of("amount", 10000)))
                .containsExactly("n_manager", "n_director");

        assertThat(materializeApprovalNodeIds(cfg, Map.of("amount", 60000)))
                .containsExactly("n_manager", "n_director", "n_gm");
    }

    @Test
    @DisplayName("workflow test")
    void resolveAssignee_RoleChain() {
        // Empty chain 鈫?throws
        assertThatThrownBy(() ->
                invokeResolveAssignee("role_chain", "[]", 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("");
    }

    private Long invokeResolveAssignee(String type, String value, Long empId) {
        try {
            java.lang.reflect.Method m = WorkflowServiceImpl.class.getDeclaredMethod(
                    "resolveAssignee", String.class, String.class, Long.class);
            m.setAccessible(true);
            return (Long) m.invoke(workflowService, type, value, empId);
        } catch (Exception e) {
            if (e.getCause() instanceof BusinessException) throw (BusinessException) e.getCause();
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            throw new RuntimeException(e);
        }
    }

    private List<String> materializeApprovalNodeIds(String nodeConfig, Map<String, Object> ctx) {
        try {
            WorkflowServiceImpl.WorkflowGraph graph = workflowService.parseNodeConfig(nodeConfig);
            assertThat(graph.valid).isTrue();
            java.lang.reflect.Method m = WorkflowServiceImpl.class.getDeclaredMethod(
                    "materializeGraphToRuntimePath", WorkflowServiceImpl.WorkflowGraph.class, Map.class);
            m.setAccessible(true);
            JSONArray nodes = (JSONArray) m.invoke(workflowService, graph, ctx);
            List<String> nodeIds = new ArrayList<>();
            for (Object obj : nodes) {
                nodeIds.add(((JSONObject) obj).getStr("nodeId"));
            }
            return nodeIds;
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            throw new RuntimeException(e);
        }
    }

    private String amountTieredWorkflowConfig() {
        return "{\n" +
                "  \"schemaVersion\": 2,\n" +
                "  \"nodes\": [\n" +
                "    {\"nodeId\":\"start\",\"nodeType\":\"start\",\"nodeName\":\"Node\"},\n" +
                "    {\"nodeId\":\"n_manager\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"dept_manager\",\"assigneeValue\":\"dept_manager\"},\n" +
                "    {\"nodeId\":\"gw_amount\",\"nodeType\":\"gateway\",\"gatewayType\":\"exclusive\",\"nodeName\":\"Node\",\n" +
                "     \"branches\":[\n" +
                "       {\"when\":\"amount > 5000\",\"to\":\"n_director\"},\n" +
                "       {\"when\":\"amount <= 5000\",\"to\":\"end\"}\n" +
                "     ]},\n" +
                "    {\"nodeId\":\"n_director\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"DIRECTOR\",\n" +
                "     \"routingRules\":[{\"when\":\"amount > 50000\",\"skipTo\":\"n_gm\"}]},\n" +
                "    {\"nodeId\":\"n_gm\",\"nodeType\":\"approval\",\"nodeName\":\"Node\",\"assigneeType\":\"role_global\",\"assigneeValue\":\"GM\"},\n" +
                "    {\"nodeId\":\"end\",\"nodeType\":\"end\",\"nodeName\":\"Node\"}\n" +
                "  ],\n" +
                "  \"edges\": [\n" +
                "    {\"source\":\"start\",\"target\":\"n_manager\"},\n" +
                "    {\"source\":\"n_manager\",\"target\":\"gw_amount\"},\n" +
                "    {\"source\":\"gw_amount\",\"target\":\"end\"},\n" +
                "    {\"source\":\"n_director\",\"target\":\"end\"},\n" +
                "    {\"source\":\"n_gm\",\"target\":\"end\"}\n" +
                "  ]\n" +
                "}";
    }

    // ==================== helpers ====================

    private SysEmployee createEmployee(Long id, String name) {
        SysEmployee emp = new SysEmployee();
        emp.setId(id);
        emp.setEmpName(name);
        emp.setDeptId(10L);
        return emp;
    }
}
