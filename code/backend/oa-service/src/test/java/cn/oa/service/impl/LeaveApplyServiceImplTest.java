package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaLeaveApply;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.WfTask;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveApplyServiceImpl 请假服务测试")
class LeaveApplyServiceImplTest {

    @Mock
    private OaLeaveApplyMapper leaveApplyMapper;

    @Mock
    private SysEmployeeMapper employeeMapper;

    @Mock
    private OaApprovalRecordMapper approvalRecordMapper;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private DelegationService delegationService;

    @Mock
    private cn.oa.mapper.WfTaskMapper wfTaskMapper;

    @Mock
    private cn.oa.mapper.WfProcessInstanceMapper wfProcessInstanceMapper;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private LeaveApplyServiceImpl leaveApplyService;

    @Captor
    private ArgumentCaptor<OaLeaveApply> leaveCaptor;

    @BeforeEach
    void setUp() throws Exception {
        // Set baseMapper for CrudRepository parent class via reflection
        Field baseMapperField = com.baomidou.mybatisplus.extension.repository.CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(leaveApplyService, leaveApplyMapper);
    }

    private OaLeaveApply createLeaveApply(LocalDateTime start, LocalDateTime end, String leaveType, String period) {
        OaLeaveApply apply = new OaLeaveApply();
        apply.setEmpId(1L);
        apply.setLeaveType(leaveType);
        apply.setStartTime(start);
        apply.setEndTime(end);
        apply.setReason("休假");
        apply.setLeavePeriod(period);
        return apply;
    }

    private OaLeaveApply createPersistedLeave(Long id, Integer status) {
        OaLeaveApply apply = new OaLeaveApply();
        apply.setId(id);
        apply.setEmpId(1L);
        apply.setLeaveType("annual");
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setReason("休假");
        apply.setDays(BigDecimal.ONE);
        apply.setStatus(status);
        return apply;
    }

    // ==================== submit ====================

    @Test
    @DisplayName("提交申请-正常提交")
    void submit_Success() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 18, 0),
                "annual", "full");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        OaLeaveApply saved = leaveCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(0);
        assertThat(saved.getDays()).isEqualByComparingTo(BigDecimal.ONE);
        verify(workflowService).startProcess(eq(BusinessType.LEAVE), isNull(), eq(1L), anyMap());
    }

    @Test
    @DisplayName("提交申请-缺少起止时间抛异常")
    void submit_NullDates_Throws() {
        OaLeaveApply apply = new OaLeaveApply();
        apply.setEmpId(1L);

        assertThatThrownBy(() -> leaveApplyService.submit(apply))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请假起止时间不能为空");
    }

    @Test
    @DisplayName("提交申请-半天假(上午)")
    void submit_HalfDayMorning() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 12, 0),
                "annual", "morning");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().getDays()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
    }

    @Test
    @DisplayName("提交申请-全天假(跨周跳过周末)")
    void submit_FullDayCrossWeek() {
        // Thursday to next Monday
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 4, 9, 0),  // Thursday
                LocalDateTime.of(2026, 6, 8, 18, 0), // next Monday
                "annual", "full");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        // Thu, Fri, Mon = 3 days (skip Sat/Sun)
        assertThat(leaveCaptor.getValue().getDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    // ==================== approve ====================

    @Test
    @DisplayName("审批通过-委托WorkflowService处理")
    void approve_ThroughWorkflow() {
        Long applyId = 100L;
        Long approverId = 2L;
        WfTask task = new WfTask();
        task.setId(300L);
        task.setNodeName("经理审批");

        when(workflowService.findPendingTask(BusinessType.LEAVE, applyId, approverId)).thenReturn(task);

        leaveApplyService.approve(applyId, approverId, 1, "同意");

        verify(workflowService).handleTask(task.getId(), approverId, 1, "同意");
    }

    @Test
    @DisplayName("审批通过-使用taskId重载方法")
    void approve_WithTaskId() {
        Long applyId = 100L;
        Long approverId = 2L;

        leaveApplyService.approve(applyId, approverId, 1, "同意", 300L);

        verify(workflowService).handleTask(300L, approverId, 1, "同意");
    }

    @Test
    @DisplayName("审批-无待办任务抛异常")
    void approve_NoPendingTask_Throws() {
        Long applyId = 100L;
        Long approverId = 2L;

        when(workflowService.findPendingTask(BusinessType.LEAVE, applyId, approverId)).thenReturn(null);

        assertThatThrownBy(() -> leaveApplyService.approve(applyId, approverId, 1, "同意"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未找到待审批的任务");
    }

    // ==================== updateStatus ====================

    @Test
    @DisplayName("更新状态-通过后调用余额扣减和考勤标记")
    void updateStatus_ToApproved_DeductAndMark() {
        Long id = 100L;
        OaLeaveApply apply = createPersistedLeave(id, 0);
        // Use numeric leave type string to avoid NumberFormatException
        apply.setLeaveType("1"); // 1 = annual leave type as numeric
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setDays(BigDecimal.ONE);

        when(leaveApplyMapper.selectById(id)).thenReturn(apply);

        leaveApplyService.updateStatus(id, 1);

        verify(leaveBalanceService).deductBalance(1L, 1, 2026, BigDecimal.ONE);
        verify(attendanceService).markLeaveAttendance(1L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
    }

    @Test
    @DisplayName("非年假(事假)通过后不应因余额不足而失败")
    void updateStatus_SickLeaveApproved_NoBalanceCheck() {
        Long id = 200L;
        OaLeaveApply apply = createPersistedLeave(id, 0);
        apply.setLeaveType("2"); // 事假，无需检查余额
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setDays(BigDecimal.ONE);

        when(leaveApplyMapper.selectById(id)).thenReturn(apply);

        // 事假通过不应抛出 BusinessException
        leaveApplyService.updateStatus(id, 1);

        verify(leaveBalanceService).deductBalance(1L, 2, 2026, BigDecimal.ONE);
        verify(attendanceService).markLeaveAttendance(1L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
    }

    @Test
    @DisplayName("更新状态-驳回后恢复余额并清除考勤标记")
    void updateStatus_ToRejected_RestoreAndRemove() {
        Long id = 100L;
        OaLeaveApply apply = createPersistedLeave(id, 1);
        apply.setLeaveType("1");
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setDays(BigDecimal.ONE);

        when(leaveApplyMapper.selectById(id)).thenReturn(apply);

        leaveApplyService.updateStatus(id, 2);

        verify(leaveBalanceService).restoreBalance(1L, 1, 2026, BigDecimal.ONE);
        verify(attendanceService).removeMarkedAttendance(1L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), 5);
    }

    @Test
    @DisplayName("更新状态-取消已通过的申请恢复余额")
    void updateStatus_CancelApproved_RestoreBalance() {
        Long id = 100L;
        OaLeaveApply apply = createPersistedLeave(id, 1);
        apply.setLeaveType("1");
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setDays(BigDecimal.ONE);

        when(leaveApplyMapper.selectById(id)).thenReturn(apply);

        leaveApplyService.updateStatus(id, 3);

        verify(leaveBalanceService).restoreBalance(1L, 1, 2026, BigDecimal.ONE);
        verify(attendanceService).removeMarkedAttendance(1L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), 5);
    }

    @Test
    @DisplayName("更新状态-nullID不执行任何操作")
    void updateStatus_NullId_NoOp() {
        leaveApplyService.updateStatus(null, 1);

        verifyNoInteractions(leaveApplyMapper, leaveBalanceService, attendanceService);
    }

    @Test
    @DisplayName("更新状态-同状态不触发侧边效应")
    void updateStatus_SameStatus_NoSideEffect() {
        Long id = 100L;
        OaLeaveApply apply = createPersistedLeave(id, 1);

        when(leaveApplyMapper.selectById(id)).thenReturn(apply);

        leaveApplyService.updateStatus(id, 1);

        // No balance or attendance changes if already same status
        verifyNoInteractions(leaveBalanceService, attendanceService);
    }

    // ==================== pageList ====================

    @Test
    @DisplayName("分页查询-按员工和状态过滤")
    void pageList_WithFilters() {
        OaLeaveApply apply = createPersistedLeave(100L, 0);
        apply.setEmpName("张三");
        apply.setRemark("备注");

        Page<OaLeaveApply> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(apply));
        page.setTotal(1);

        when(leaveApplyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // Mock fillEmpNames - selectBatchIds
        SysEmployee emp = new SysEmployee();
        emp.setId(1L);
        emp.setEmpName("张三");
        when(employeeMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(emp));

        // Mock fillRemarks
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(100L);
        record.setRemark("测试备注");
        when(approvalRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(record));

        IPage<OaLeaveApply> result = leaveApplyService.pageList(1, 10, 1L, 0);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getEmpName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("分页查询-无记录返回空")
    void pageList_Empty() {
        Page<OaLeaveApply> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(leaveApplyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<OaLeaveApply> result = leaveApplyService.pageList(1, 10, null, null);

        assertThat(result.getRecords()).isEmpty();
    }

    // ==================== calculateLeaveDays (tested through submit) ====================

    @Test
    @DisplayName("计算请假天数-全天(同一天)")
    void submit_FullDaySameDay() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 18, 0),
                "annual", "full");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("计算请假天数-半天(同一天)")
    void submit_HalfDaySameDay() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 12, 0),
                "annual", "morning");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.valueOf(0.5));
    }

    @Test
    @DisplayName("计算请假天数-跨周跳过周末")
    void submit_FullDayCrossWeekend() {
        // Thursday 9:00 to next Monday 18:00
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 4, 9, 0),   // Thursday
                LocalDateTime.of(2026, 6, 8, 18, 0),  // next Monday
                "annual", "full");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        // Thu(1) + Fri(1) + Mon(1) = 3 (Sat/Sun skipped)
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    @Test
    @DisplayName("计算请假天数-跨周最后一半天")
    void submit_CrossWeekLastHalfDay() {
        // Thursday 9:00 to next Monday 12:00 (half day at end)
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 4, 9, 0),    // Thursday
                LocalDateTime.of(2026, 6, 8, 12, 0),   // next Monday noon
                "annual", "morning");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        // Thu(1) + Fri(1) + Mon(0.5) = 2.5
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.valueOf(2.5));
    }

    @Test
    @DisplayName("计算请假天数-纯周末返回0")
    void submit_WeekendOnly_ZeroDays() {
        // Saturday to Sunday
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 6, 9, 0),   // Saturday
                LocalDateTime.of(2026, 6, 7, 18, 0),  // Sunday
                "annual", "full");

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== 边界条件 ====================

    @Test
    @DisplayName("提交申请-缺省请假类型为全天")
    void submit_DefaultPeriodIsFull() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 3, 18, 0),
                "annual", null);

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        // Mon + Tue + Wed = 3 full days
        assertThat(leaveCaptor.getValue().getDays())
                .isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    @Test
    @DisplayName("提交申请-实体字段正确设置")
    void submit_SetsEntityFields() {
        OaLeaveApply apply = createLeaveApply(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 18, 0),
                "annual", "full");
        apply.setId(null);

        leaveApplyService.submit(apply);

        verify(leaveApplyMapper).insert(leaveCaptor.capture());
        OaLeaveApply saved = leaveCaptor.getValue();
        assertThat(saved.getEmpId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(0);
        assertThat(saved.getDays()).isNotNull();
        assertThat(saved.getLeaveType()).isEqualTo("annual");
    }
}
