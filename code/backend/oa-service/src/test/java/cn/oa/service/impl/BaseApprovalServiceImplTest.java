package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.DelegationService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseApprovalServiceImplTest {

    // ====== Test entity with proper getters for SFunction references ======

    static class TestEntity {
        private Long id;
        private Long empId;
        private Integer status;
        private String remark;
        private String empName;
        private LocalDateTime createTime;

        Long getId() { return id; }
        void setId(Long id) { this.id = id; }
        Long getEmpId() { return empId; }
        void setEmpId(Long empId) { this.empId = empId; }
        Integer getStatus() { return status; }
        void setStatus(Integer status) { this.status = status; }
        String getRemark() { return remark; }
        void setRemark(String remark) { this.remark = remark; }
        String getEmpName() { return empName; }
        void setEmpName(String empName) { this.empName = empName; }
        LocalDateTime getCreateTime() { return createTime; }
        void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    static class TestServiceImpl extends BaseApprovalServiceImpl<BaseMapper<TestEntity>, TestEntity> {
        public TestServiceImpl() {
            this.empIdGetter = TestEntity::getEmpId;
            this.statusGetter = TestEntity::getStatus;
            this.createTimeGetter = TestEntity::getCreateTime;
            this.idGetter = TestEntity::getId;
        }

        @Override
        protected String getBusinessType() { return "test"; }

        @Override
        protected void setStatus(TestEntity entity, Integer status) { entity.setStatus(status); }

        @Override
        protected void setEmpName(TestEntity entity, String name) { entity.setEmpName(name); }

        @Override
        protected void setRemark(TestEntity entity, String remark) { entity.setRemark(remark); }
    }

    @InjectMocks
    private TestServiceImpl testService;

    @Mock
    private BaseMapper<TestEntity> baseMapper;

    @Mock
    private SysEmployeeMapper employeeMapper;

    @Mock
    private OaApprovalRecordMapper approvalRecordMapper;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private DelegationService delegationService;

    @Mock
    private WfTaskMapper wfTaskMapper;

    @Mock
    private WfProcessInstanceMapper wfProcessInstanceMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(testService, "baseMapper", baseMapper);
    }

    // ========== doSubmit ==========

    @Test
    void doSubmit_success() {
        TestEntity entity = new TestEntity();
        entity.setEmpId(100L);

        when(workflowService.startProcess(anyString(), anyLong(), anyLong(), anyMap())).thenReturn(null);

        testService.doSubmit(entity);

        assertEquals(0, entity.getStatus().intValue());
        verify(baseMapper).insert(entity);
        verify(workflowService).startProcess(eq("test"), any(), eq(100L), anyMap());
    }

    // ========== doApprove ==========

    @Test
    void doApprove_authorized() {
        WfTask task = new WfTask();
        task.setId(1L);
        task.setAssigneeId(200L);
        task.setStatus("0");

        when(workflowService.findPendingTask("test", 1L, 200L)).thenReturn(task);

        testService.doApprove(1L, 200L, 1, "同意");

        verify(workflowService).handleTask(1L, 200L, 1, "同意");
    }

    @Test
    void doApprove_taskFound_delegatesToHandleTask() {
        WfTask task = new WfTask();
        task.setId(1L);
        task.setAssigneeId(200L); // assigned to 200L, but approver is 300L
        task.setStatus("0");

        // findPendingTask returns a task (delegation resolution happens inside findPendingTask)
        when(workflowService.findPendingTask("test", 1L, 300L)).thenReturn(task);

        // doApprove passes approverId through to handleTask — authorization is handleTask's concern
        testService.doApprove(1L, 300L, 1, "同意");

        verify(workflowService).handleTask(1L, 300L, 1, "同意");
    }

    @Test
    void doApprove_delegation_success() {
        Long delegatorId = 100L;
        Long delegateId = 200L;

        WfTask task = new WfTask();
        task.setId(1L);
        task.setAssigneeId(200L); // assigned to delegate
        task.setStatus("0");

        when(workflowService.findPendingTask("test", 1L, delegatorId)).thenReturn(task);
        // delegator has delegated to 200L
        when(delegationService.resolveDelegate(delegatorId)).thenReturn(200L);

        testService.doApprove(1L, delegatorId, 1, "委派审批");

        // doApprove passes the original approverId (delegatorId=100L) to handleTask
        // Delegation resolution happens inside findPendingTask/handleTask
        verify(workflowService).handleTask(1L, 100L, 1, "委派审批");
    }

    @Test
    void doApprove_noTask_throws() {
        when(workflowService.findPendingTask("test", 1L, 200L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> testService.doApprove(1L, 200L, 1, "同意"));
        assertTrue(ex.getMessage().contains("未找到"));
    }

    @Test
    void doApprove_reverseDelegation_success() {
        Long delegateId = 300L; // user acting as delegate
        Long delegatorId = 200L; // original task assignee

        WfTask task = new WfTask();
        task.setId(1L);
        task.setAssigneeId(200L); // original assignee
        task.setStatus("0");

        WfDelegation delegation = new WfDelegation();
        delegation.setDelegatorId(200L);
        delegation.setDelegateToId(300L);

        when(workflowService.findPendingTask("test", 1L, delegateId)).thenReturn(task);
        when(delegationService.resolveDelegate(delegateId)).thenReturn(null);
        when(delegationService.findActiveDelegationForDelegate(delegateId)).thenReturn(delegation);

        testService.doApprove(1L, delegateId, 1, "反向委派审批");

        // doApprove passes the original approverId (delegateId=300L) to handleTask
        // Reverse delegation authorization happens inside handleTask
        verify(workflowService).handleTask(1L, 300L, 1, "反向委派审批");
    }

    // ========== doUpdateStatus ==========

    @Test
    void doUpdateStatus_success() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setEmpId(100L);
        entity.setStatus(0);

        when(baseMapper.selectById(1L)).thenReturn(entity);

        testService.doUpdateStatus(1L, 1);

        assertEquals(1, entity.getStatus().intValue());
        verify(baseMapper).updateById(entity);
    }

    @Test
    void doUpdateStatus_nullId_noOp() {
        testService.doUpdateStatus(null, 1);
        verify(baseMapper, never()).updateById(any(TestEntity.class));
    }

    // ========== doPageList ==========

    @Test
    void doPageList_withFilters() {
        List<TestEntity> records = new ArrayList<>();
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setEmpId(100L);
        records.add(entity);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TestEntity> pageResult =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        pageResult.setRecords(records);

        when(baseMapper.selectPage(any(), any())).thenReturn(pageResult);

        IPage<TestEntity> result = testService.doPageList(1, 10, 100L, 0);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void doPageList_noFilters() {
        when(baseMapper.selectPage(any(), any())).thenReturn(
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        IPage<TestEntity> result = testService.doPageList(1, 10, null, null);

        assertNotNull(result);
    }

    // ========== fillEmpNames ==========

    @Test
    void fillEmpNames_success() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        entity1.setEmpId(100L);

        TestEntity entity2 = new TestEntity();
        entity2.setId(2L);
        entity2.setEmpId(200L);

        List<TestEntity> records = Arrays.asList(entity1, entity2);

        SysEmployee emp1 = new SysEmployee();
        emp1.setId(100L);
        emp1.setEmpName("张三");
        SysEmployee emp2 = new SysEmployee();
        emp2.setId(200L);
        emp2.setEmpName("李四");

        when(employeeMapper.selectBatchIds(any())).thenReturn(Arrays.asList(emp1, emp2));

        testService.fillEmpNames(records);

        assertEquals("张三", entity1.getEmpName());
        assertEquals("李四", entity2.getEmpName());
    }

    // ========== fillRemarks ==========

    @Test
    void fillRemarks_success() {
        TestEntity entity1 = new TestEntity();
        entity1.setId(1L);
        entity1.setEmpId(100L);

        TestEntity entity2 = new TestEntity();
        entity2.setId(2L);
        entity2.setEmpId(200L);

        List<TestEntity> records = Arrays.asList(entity1, entity2);

        OaApprovalRecord record1 = new OaApprovalRecord();
        record1.setApplyId(1L);
        record1.setRemark("已批准");

        OaApprovalRecord record2 = new OaApprovalRecord();
        record2.setApplyId(2L);
        record2.setRemark("已驳回");

        when(approvalRecordMapper.selectList(any())).thenReturn(Arrays.asList(record1, record2));

        testService.fillRemarks(records);

        assertEquals("已批准", entity1.getRemark());
        assertEquals("已驳回", entity2.getRemark());
    }
}
