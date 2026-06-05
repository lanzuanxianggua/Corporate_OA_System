package cn.oa.workflow.engine;

import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.entity.*;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import cn.oa.workflow.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WfEngineTest {

    @Mock
    private WfDefinitionMapper definitionMapper;
    @Mock
    private WfNodeMapper nodeMapper;
    @Mock
    private WfTransitionMapper transitionMapper;
    @Mock
    private WfInstanceMapper instanceMapper;
    @Mock
    private WfTaskMapper taskMapper;
    @Mock
    private WfRecordMapper recordMapper;
    @Mock
    private WfAssigneeResolver assigneeResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<WfInstance> instanceCaptor;
    @Captor
    private ArgumentCaptor<WfTask> taskCaptor;
    @Captor
    private ArgumentCaptor<WfRecord> recordCaptor;
    @Captor
    private ArgumentCaptor<WfInstanceCompletedEvent> eventCaptor;

    private WfEngine engine;

    private static final Long DEF_ID = 10L;
    private static final Long START_NODE_ID = 20L;
    private static final Long APPROVAL_NODE_ID = 30L;
    private static final Long END_NODE_ID = 40L;
    private static final Long INITIATOR = 1L;
    private static final Long ASSIGNEE = 2L;

    @BeforeEach
    void setUp() {
        engine = new WfEngine(definitionMapper, nodeMapper, transitionMapper,
                instanceMapper, taskMapper, recordMapper, assigneeResolver, eventPublisher);
    }

    // -- helper: simulate MyBatis-Plus setting auto-generated ID after insert --
    private static final Answer<Integer> SET_INSTANCE_ID = inv -> {
        inv.<WfInstance>getArgument(0).setId(100L);
        return 1;
    };

    private static final Answer<Integer> SET_TASK_ID = inv -> {
        inv.<WfTask>getArgument(0).setId(200L);
        return 1;
    };

    // -- test data builders --

    private WfDefinition aDef() {
        WfDefinition def = new WfDefinition();
        def.setId(DEF_ID);
        def.setDefKey("leave");
        return def;
    }

    private WfNode aNode(Long id, String nodeType, String nodeKey) {
        WfNode n = new WfNode();
        n.setId(id);
        n.setDefId(DEF_ID);
        n.setNodeType(nodeType);
        n.setNodeKey(nodeKey);
        return n;
    }

    private WfTransition aTransition(Long fromId, Long toId) {
        WfTransition t = new WfTransition();
        t.setId(50L);
        t.setFromNodeId(fromId);
        t.setToNodeId(toId);
        t.setAction("APPROVE");
        return t;
    }

    private WfTask aTask(Long id, Long nodeId, String status) {
        WfTask t = new WfTask();
        t.setId(id);
        t.setInstanceId(100L);
        t.setNodeId(nodeId);
        t.setAssigneeId(INITIATOR);
        t.setStatus(status);
        return t;
    }

    private WfInstance anInstance(String status) {
        WfInstance inst = new WfInstance();
        inst.setId(100L);
        inst.setDefId(DEF_ID);
        inst.setDefKey("leave");
        inst.setInitiatorId(INITIATOR);
        inst.setStatus(status);
        return inst;
    }

    // ================= startProcess tests =================

    @Test
    void shouldStartProcessSuccessfully() {
        WfDefinition def = aDef();
        WfNode startNode = aNode(START_NODE_ID, "START", "start");
        WfNode approvalNode = aNode(APPROVAL_NODE_ID, "APPROVAL", "manager_approve");
        WfTransition transition = aTransition(START_NODE_ID, APPROVAL_NODE_ID);

        when(definitionMapper.findActiveByKey("leave")).thenReturn(def);
        when(instanceMapper.findRunningByBusinessKey("BIZ-001")).thenReturn(null);
        when(nodeMapper.findByDefId(DEF_ID)).thenReturn(List.of(startNode, approvalNode));
        when(transitionMapper.findByFromNodeAndAction(START_NODE_ID, "APPROVE")).thenReturn(List.of(transition));
        when(nodeMapper.selectById(APPROVAL_NODE_ID)).thenReturn(approvalNode);
        when(assigneeResolver.resolve(DEF_ID, "manager_approve", INITIATOR)).thenReturn(ASSIGNEE);

        doAnswer(SET_INSTANCE_ID).when(instanceMapper).insert(any(WfInstance.class));
        doAnswer(SET_TASK_ID).when(taskMapper).insert(any(WfTask.class));
        doReturn(1).when(recordMapper).insert(any(WfRecord.class));

        Long instanceId = engine.startProcess("leave", "BIZ-001", INITIATOR);

        assertThat(instanceId).isEqualTo(100L);

        verify(instanceMapper).insert(instanceCaptor.capture());
        WfInstance capturedInstance = instanceCaptor.getValue();
        assertThat(capturedInstance.getDefId()).isEqualTo(DEF_ID);
        assertThat(capturedInstance.getBusinessKey()).isEqualTo("BIZ-001");
        assertThat(capturedInstance.getInitiatorId()).isEqualTo(INITIATOR);
        assertThat(capturedInstance.getStatus()).isEqualTo("RUNNING");
        assertThat(capturedInstance.getCurrentNodeId()).isEqualTo(APPROVAL_NODE_ID);
        assertThat(capturedInstance.getDelFlag()).isEqualTo("0");

        verify(taskMapper).insert(taskCaptor.capture());
        WfTask capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.getInstanceId()).isEqualTo(100L);
        assertThat(capturedTask.getNodeId()).isEqualTo(APPROVAL_NODE_ID);
        assertThat(capturedTask.getAssigneeId()).isEqualTo(ASSIGNEE);
        assertThat(capturedTask.getStatus()).isEqualTo("PENDING");

        verify(recordMapper).insert(recordCaptor.capture());
        WfRecord capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.getInstanceId()).isEqualTo(100L);
        assertThat(capturedRecord.getEmpId()).isEqualTo(INITIATOR);
        assertThat(capturedRecord.getAction()).isEqualTo("START");
    }

    @Test
    void shouldThrowWhenDefinitionNotFound() {
        when(definitionMapper.findActiveByKey("unknown")).thenReturn(null);

        assertThatThrownBy(() -> engine.startProcess("unknown", "BIZ-001", INITIATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("流程定义不存在");
    }

    @Test
    void shouldThrowWhenBusinessKeyAlreadyRunning() {
        when(definitionMapper.findActiveByKey("leave")).thenReturn(aDef());
        when(instanceMapper.findRunningByBusinessKey("BIZ-001")).thenReturn(anInstance("RUNNING"));

        assertThatThrownBy(() -> engine.startProcess("leave", "BIZ-001", INITIATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在进行中的流程");
    }

    @Test
    void shouldThrowWhenNoStartNode() {
        when(definitionMapper.findActiveByKey("leave")).thenReturn(aDef());
        when(instanceMapper.findRunningByBusinessKey("BIZ-002")).thenReturn(null);
        when(nodeMapper.findByDefId(DEF_ID)).thenReturn(List.of(aNode(APPROVAL_NODE_ID, "APPROVAL", "mgr")));

        assertThatThrownBy(() -> engine.startProcess("leave", "BIZ-002", INITIATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无 START 节点");
    }

    // ================= approve tests =================

    @Test
    void shouldApproveAndCreateNextTask() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "PENDING");
        WfInstance instance = anInstance("RUNNING");
        WfTransition transition = aTransition(APPROVAL_NODE_ID, END_NODE_ID);
        WfNode nextNode = aNode(END_NODE_ID, "APPROVAL", "dept_head");
        nextNode.setNodeType("APPROVAL");

        when(taskMapper.selectById(200L)).thenReturn(task);
        when(instanceMapper.selectById(100L)).thenReturn(instance);
        when(transitionMapper.findByFromNodeAndAction(APPROVAL_NODE_ID, "APPROVE")).thenReturn(List.of(transition));
        when(nodeMapper.selectById(END_NODE_ID)).thenReturn(nextNode);
        when(assigneeResolver.resolve(DEF_ID, "dept_head", INITIATOR)).thenReturn(3L);
        doReturn(1).when(recordMapper).insert(any(WfRecord.class));

        engine.approve(200L, INITIATOR, "APPROVE", "同意");

        verify(taskMapper).updateById(task);
        assertThat(task.getAction()).isEqualTo("APPROVE");
        assertThat(task.getStatus()).isEqualTo("APPROVED");
        assertThat(task.getActionEmpId()).isEqualTo(INITIATOR);
        assertThat(task.getComment()).isEqualTo("同意");

        verify(taskMapper).insert(taskCaptor.capture());
        WfTask nextTask = taskCaptor.getValue();
        assertThat(nextTask.getAssigneeId()).isEqualTo(3L);
        assertThat(nextTask.getStatus()).isEqualTo("PENDING");

        verify(instanceMapper).updateById(instance);
        assertThat(instance.getCurrentNodeId()).isEqualTo(END_NODE_ID);

        verify(eventPublisher, never()).publishEvent(any(WfInstanceCompletedEvent.class));
    }

    @Test
    void shouldRejectAndEndInstance() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "PENDING");
        WfInstance instance = anInstance("RUNNING");

        when(taskMapper.selectById(200L)).thenReturn(task);
        when(instanceMapper.selectById(100L)).thenReturn(instance);
        doReturn(1).when(recordMapper).insert(any(WfRecord.class));

        engine.approve(200L, INITIATOR, "REJECT", "不同意");

        verify(taskMapper).updateById(task);
        assertThat(task.getStatus()).isEqualTo("REJECTED");
        assertThat(task.getAction()).isEqualTo("REJECT");

        verify(instanceMapper).updateById(instance);
        assertThat(instance.getStatus()).isEqualTo("REJECTED");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        WfInstanceCompletedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getStatus()).isEqualTo("REJECTED");
        assertThat(publishedEvent.getInstanceId()).isEqualTo(100L);
    }

    @Test
    void shouldApproveAndEndAtEndNode() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "PENDING");
        WfInstance instance = anInstance("RUNNING");
        WfTransition transition = aTransition(APPROVAL_NODE_ID, END_NODE_ID);
        WfNode endNode = aNode(END_NODE_ID, "END", "end");

        when(taskMapper.selectById(200L)).thenReturn(task);
        when(instanceMapper.selectById(100L)).thenReturn(instance);
        when(transitionMapper.findByFromNodeAndAction(APPROVAL_NODE_ID, "APPROVE")).thenReturn(List.of(transition));
        when(nodeMapper.selectById(END_NODE_ID)).thenReturn(endNode);
        doReturn(1).when(recordMapper).insert(any(WfRecord.class));

        engine.approve(200L, INITIATOR, "APPROVE", "通过");

        verify(instanceMapper).updateById(instance);
        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        verify(taskMapper, never()).insert(any(WfTask.class));

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        WfInstanceCompletedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getStatus()).isEqualTo("APPROVED");
        assertThat(publishedEvent.getInstanceId()).isEqualTo(100L);
    }

    @Test
    void shouldThrowWhenTaskNotFound() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> engine.approve(999L, INITIATOR, "APPROVE", ""))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    void shouldThrowWhenTaskAlreadyProcessed() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "APPROVED");

        when(taskMapper.selectById(200L)).thenReturn(task);

        assertThatThrownBy(() -> engine.approve(200L, INITIATOR, "APPROVE", ""))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("任务已处理");
    }

    @Test
    void shouldThrowWhenInstanceNotRunning() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "PENDING");
        WfInstance instance = anInstance("APPROVED");

        when(taskMapper.selectById(200L)).thenReturn(task);
        when(instanceMapper.selectById(100L)).thenReturn(instance);

        assertThatThrownBy(() -> engine.approve(200L, INITIATOR, "APPROVE", ""))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("流程不在运行中");
    }

    @Test
    void shouldThrowWhenNoTransitionFound() {
        WfTask task = aTask(200L, APPROVAL_NODE_ID, "PENDING");
        WfInstance instance = anInstance("RUNNING");

        when(taskMapper.selectById(200L)).thenReturn(task);
        when(instanceMapper.selectById(100L)).thenReturn(instance);
        when(transitionMapper.findByFromNodeAndAction(APPROVAL_NODE_ID, "APPROVE")).thenReturn(List.of());

        assertThatThrownBy(() -> engine.approve(200L, INITIATOR, "APPROVE", ""))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无 APPROVE 流转");
    }
}
