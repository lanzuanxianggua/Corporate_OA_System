package cn.oa.hr.leave.service;

import cn.oa.hr.leave.callback.HrLeaveWfCallback;
import cn.oa.hr.leave.dto.HrLeaveCreateDTO;
import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import cn.oa.workflow.mapper.WfInstanceMapper;
import cn.oa.workflow.service.WfInstanceService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrLeaveApprovalFlowTest {

    private static final Long EMP_ID = 42L;
    private static final Long LEAVE_ID = 100L;
    private static final Long WF_INSTANCE_ID = 200L;

    @Mock
    private HrLeaveMapper leaveMapper;
    @Mock
    private HrLeaveBalanceService balanceService;
    @Mock
    private WfInstanceService wfInstanceService;
    @Mock
    private WfInstanceMapper instanceMapper;

    @Captor
    private ArgumentCaptor<HrLeave> leaveCaptor;

    private HrLeaveService leaveService;
    private HrLeaveWfCallback callback;

    @BeforeEach
    void setUp() {
        leaveService = new HrLeaveService(leaveMapper, balanceService, wfInstanceService);
        callback = new HrLeaveWfCallback(leaveMapper, balanceService, instanceMapper);
    }

    @Test
    void shouldSubmitThenApproveAndDeductBalance() {
        HrLeaveCreateDTO dto = leaveDto();
        when(leaveMapper.insert(any(HrLeave.class))).thenAnswer(inv -> {
            HrLeave leave = inv.getArgument(0);
            leave.setId(LEAVE_ID);
            return 1;
        });
        when(wfInstanceService.start("hr_leave", "LEAVE_" + LEAVE_ID, EMP_ID))
                .thenReturn(WF_INSTANCE_ID);

        Long leaveId = leaveService.submit(EMP_ID, dto);

        verify(leaveMapper).insert(leaveCaptor.capture());
        HrLeave leave = leaveCaptor.getValue();
        assertThat(leaveId).isEqualTo(LEAVE_ID);
        assertThat(leave.getStatus()).isEqualTo("PENDING");
        assertThat(leave.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
        verify(balanceService).freezeOnSubmit(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));

        when(instanceMapper.selectById(WF_INSTANCE_ID))
                .thenReturn(instance("APPROVED"));
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        callback.handleEvent(new WfInstanceCompletedEvent(
                WF_INSTANCE_ID, "APPROVED", "LEAVE_" + LEAVE_ID));

        assertThat(leave.getStatus()).isEqualTo("APPROVED");
        verify(leaveMapper, times(2)).updateById(leave);
        verify(balanceService).deductOnApprove(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
    }

    @Test
    void shouldSubmitThenRejectAndUnfreezeBalance() {
        HrLeaveCreateDTO dto = leaveDto();
        when(leaveMapper.insert(any(HrLeave.class))).thenAnswer(inv -> {
            HrLeave leave = inv.getArgument(0);
            leave.setId(LEAVE_ID);
            return 1;
        });
        when(wfInstanceService.start("hr_leave", "LEAVE_" + LEAVE_ID, EMP_ID))
                .thenReturn(WF_INSTANCE_ID);

        leaveService.submit(EMP_ID, dto);

        verify(leaveMapper).insert(leaveCaptor.capture());
        HrLeave leave = leaveCaptor.getValue();
        when(instanceMapper.selectById(WF_INSTANCE_ID))
                .thenReturn(instance("REJECTED"));
        when(leaveMapper.selectById(LEAVE_ID)).thenReturn(leave);

        callback.handleEvent(new WfInstanceCompletedEvent(
                WF_INSTANCE_ID, "REJECTED", "LEAVE_" + LEAVE_ID));

        assertThat(leave.getStatus()).isEqualTo("REJECTED");
        verify(leaveMapper, times(2)).updateById(leave);
        verify(balanceService).unfreezeOnReject(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
    }

    private HrLeaveCreateDTO leaveDto() {
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartDate(LocalDate.of(2026, 6, 10));
        dto.setEndDate(LocalDate.of(2026, 6, 12));
        dto.setReason("年假");
        return dto;
    }

    private WfInstance instance(String status) {
        WfInstance instance = new WfInstance();
        instance.setId(WF_INSTANCE_ID);
        instance.setStatus(status);
        instance.setBusinessKey("LEAVE_" + LEAVE_ID);
        return instance;
    }
}
