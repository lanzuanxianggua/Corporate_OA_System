package cn.oa.hr.leave.callback;

import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.hr.leave.service.HrLeaveBalanceService;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import cn.oa.workflow.mapper.WfInstanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrLeaveWfCallbackTest {

    @Mock
    private HrLeaveMapper leaveMapper;
    @Mock
    private HrLeaveBalanceService balanceService;
    @Mock
    private WfInstanceMapper instanceMapper;

    @Captor
    private ArgumentCaptor<HrLeave> leaveCaptor;

    private HrLeaveWfCallback callback;

    private static final Long WF_INSTANCE_ID = 200L;
    private static final Long LEAVE_ID = 100L;
    private static final Long EMP_ID = 42L;

    @BeforeEach
    void setUp() {
        callback = new HrLeaveWfCallback(leaveMapper, balanceService, instanceMapper);
    }

    // ==================== onWorkflowFinished() ====================

    @Test
    void shouldThrowWhenInstanceNotFound() {
        // given
        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> callback.onWorkflowFinished(WF_INSTANCE_ID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("流程实例不存在");

        verify(leaveMapper, never()).selectById(anyLong());
        verify(balanceService, never()).deductOnApprove(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldSkipWhenBusinessKeyNotLeave() {
        // given
        WfInstance instance = anInstance("APPROVED", "MEETING_999");
        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);

        // when
        callback.onWorkflowFinished(WF_INSTANCE_ID);

        // then
        verify(leaveMapper, never()).selectById(anyLong());
        verify(leaveMapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).deductOnApprove(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldSkipWhenLeaveNotFound() {
        // given
        WfInstance instance = anInstance("APPROVED", "LEAVE_" + LEAVE_ID);
        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(null);

        // when
        callback.onWorkflowFinished(WF_INSTANCE_ID);

        // then
        verify(leaveMapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).deductOnApprove(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldApproveAndDeductBalance() {
        // given
        WfInstance instance = anInstance("APPROVED", "LEAVE_" + LEAVE_ID);
        HrLeave leave = aPendingLeave();

        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when
        callback.onWorkflowFinished(WF_INSTANCE_ID);

        // then
        assertThat(leave.getStatus()).isEqualTo("APPROVED");
        verify(leaveMapper).updateById(leave);
        verify(balanceService).deductOnApprove(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldRejectAndUnfreezeBalance() {
        // given
        WfInstance instance = anInstance("REJECTED", "LEAVE_" + LEAVE_ID);
        HrLeave leave = aPendingLeave();

        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when
        callback.onWorkflowFinished(WF_INSTANCE_ID);

        // then
        assertThat(leave.getStatus()).isEqualTo("REJECTED");
        verify(leaveMapper).updateById(leave);
        verify(balanceService).unfreezeOnReject(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
        verify(balanceService, never()).deductOnApprove(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldHandleUnknownStatusGracefully() {
        // given
        WfInstance instance = anInstance("UNKNOWN", "LEAVE_" + LEAVE_ID);
        HrLeave leave = aPendingLeave();

        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when
        callback.onWorkflowFinished(WF_INSTANCE_ID);

        // then (status is neither APPROVED nor REJECTED — no action on leave or balance)
        verify(leaveMapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).deductOnApprove(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldHandleEventViaEventListener() {
        // given - 与 shouldApproveAndDeductBalance 相同的数据，但通过事件触发
        WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(WF_INSTANCE_ID, "APPROVED", "LEAVE_" + LEAVE_ID);
        WfInstance instance = anInstance("APPROVED", "LEAVE_" + LEAVE_ID);
        HrLeave leave = aPendingLeave();

        when(instanceMapper.selectById(WF_INSTANCE_ID)).thenReturn(instance);
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when - 通过 @EventListener 桥接方法触发
        callback.handleEvent(event);

        // then - 与 shouldApproveAndDeductBalance 相同的验证
        assertThat(leave.getStatus()).isEqualTo("APPROVED");
        verify(leaveMapper).updateById(leave);
        verify(balanceService).deductOnApprove(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    // ==================== helpers ====================

    private WfInstance anInstance(String status, String businessKey) {
        WfInstance instance = new WfInstance();
        instance.setId(WF_INSTANCE_ID);
        instance.setBusinessKey(businessKey);
        instance.setStatus(status);
        return instance;
    }

    private HrLeave aPendingLeave() {
        HrLeave leave = new HrLeave();
        leave.setId(LEAVE_ID);
        leave.setEmpId(EMP_ID);
        leave.setLeaveType("ANNUAL");
        leave.setStartDate(LocalDate.of(2026, 6, 10));
        leave.setEndDate(LocalDate.of(2026, 6, 12));
        leave.setTotalDays(BigDecimal.valueOf(3));
        leave.setStatus("PENDING");
        return leave;
    }
}
