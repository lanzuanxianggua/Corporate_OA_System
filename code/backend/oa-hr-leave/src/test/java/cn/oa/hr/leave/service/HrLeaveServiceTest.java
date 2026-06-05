package cn.oa.hr.leave.service;

import cn.oa.hr.leave.dto.HrLeaveCreateDTO;
import cn.oa.hr.leave.dto.HrLeaveQueryDTO;
import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.hr.leave.vo.HrLeaveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrLeaveServiceTest {

    @Mock
    private HrLeaveMapper mapper;
    @Mock
    private HrLeaveBalanceService balanceService;
    @Mock
    private WfInstanceService wfInstanceService;

    @Captor
    private ArgumentCaptor<HrLeave> leaveCaptor;
    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<HrLeave>> wrapperCaptor;

    private HrLeaveService service;

    private static final Long EMP_ID = 42L;
    private static final Long LEAVE_ID = 100L;
    private static final Long WF_INSTANCE_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new HrLeaveService(mapper, balanceService, wfInstanceService);
    }

    // ==================== submit() ====================

    @Test
    void shouldSubmitSuccessfullyWhenBalanceSufficient() {
        // given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartDate(LocalDate.of(2026, 6, 10));
        dto.setEndDate(LocalDate.of(2026, 6, 12));
        dto.setReason("年假");

        // mapper.insert 设置 id
        when(mapper.insert(any(HrLeave.class))).thenAnswer(inv -> {
            HrLeave leave = inv.getArgument(0);
            leave.setId(LEAVE_ID);
            return 1;
        });
        when(wfInstanceService.start(eq("hr_leave"), eq("LEAVE_" + LEAVE_ID), eq(EMP_ID)))
                .thenReturn(WF_INSTANCE_ID);

        // when
        Long result = service.submit(EMP_ID, dto);

        // then
        assertThat(result).isEqualTo(LEAVE_ID);

        // 验证余额冻结被调用
        verify(balanceService).freezeOnSubmit(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));

        // 验证 leave 插入
        verify(mapper).insert(leaveCaptor.capture());
        HrLeave captured = leaveCaptor.getValue();
        assertThat(captured.getEmpId()).isEqualTo(EMP_ID);
        assertThat(captured.getLeaveType()).isEqualTo("ANNUAL");
        assertThat(captured.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(captured.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(captured.getTotalDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(captured.getReason()).isEqualTo("年假");
        assertThat(captured.getStatus()).isEqualTo("PENDING");
        assertThat(captured.getCreateBy()).isEqualTo(String.valueOf(EMP_ID));

        // 验证 wf_instance_id 回写
        verify(mapper).updateById(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
    }

    @Test
    void shouldThrowWhenEndDateBeforeStartDate() {
        // given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartDate(LocalDate.of(2026, 6, 12));
        dto.setEndDate(LocalDate.of(2026, 6, 10));

        // when / then
        assertThatThrownBy(() -> service.submit(EMP_ID, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("结束日期不能早于开始日期");

        verify(balanceService, never()).freezeOnSubmit(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        verify(mapper, never()).insert(any(HrLeave.class));
    }

    @Test
    void shouldPropagateBizExceptionWhenBalanceInsufficient() {
        // given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartDate(LocalDate.of(2026, 6, 10));
        dto.setEndDate(LocalDate.of(2026, 6, 10));
        dto.setReason("年假");

        doThrow(new BizException(RCode.BAD_REQUEST, "余额不足"))
                .when(balanceService).freezeOnSubmit(EMP_ID, "ANNUAL", 2026, BigDecimal.ONE);

        // when / then
        assertThatThrownBy(() -> service.submit(EMP_ID, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额不足");

        verify(mapper, never()).insert(any(HrLeave.class));
        verify(wfInstanceService, never()).start(anyString(), anyString(), anyLong());
    }

    // ==================== revoke() ====================

    @Test
    void shouldRevokeSuccessfullyWhenPending() {
        // given
        HrLeave leave = aPendingLeave();
        when(mapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when
        service.revoke(LEAVE_ID, EMP_ID);

        // then
        assertThat(leave.getStatus()).isEqualTo("CANCELLED");
        verify(mapper).updateById(leave);
        verify(balanceService).unfreezeOnReject(EMP_ID, "ANNUAL", 2026, BigDecimal.valueOf(3));
    }

    @Test
    void shouldThrowWhenLeaveNotFound() {
        // given
        when(mapper.selectById(999L)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> service.revoke(999L, EMP_ID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请假单不存在");

        verify(mapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldThrowWhenNotTheOwner() {
        // given
        HrLeave leave = aPendingLeave();
        when(mapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when / then
        assertThatThrownBy(() -> service.revoke(LEAVE_ID, 99L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能撤回自己的请假申请");

        verify(mapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    void shouldThrowWhenStatusNotPending() {
        // given
        HrLeave leave = aPendingLeave();
        leave.setStatus("APPROVED");
        when(mapper.selectById(LEAVE_ID)).thenReturn(leave);

        // when / then
        assertThatThrownBy(() -> service.revoke(LEAVE_ID, EMP_ID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("仅待审批状态可撤回");

        verify(mapper, never()).updateById(any(HrLeave.class));
        verify(balanceService, never()).unfreezeOnReject(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
    }

    // ==================== listByEmpId() ====================

    @Test
    void shouldListByEmpId() {
        // given
        Map<String, Object> row1 = Map.of("id", 1L, "leave_type", "ANNUAL");
        Map<String, Object> row2 = Map.of("id", 2L, "leave_type", "SICK");
        when(mapper.findByEmpId(EMP_ID, 10)).thenReturn(List.of(row1, row2));

        // when
        List<Map<String, Object>> result = service.listByEmpId(EMP_ID, 10);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("id", 1L);
        verify(mapper).findByEmpId(EMP_ID, 10);
    }

    // ==================== getDetail() ====================

    @Test
    void shouldGetDetail() {
        // given
        Map<String, Object> detail = Map.of("id", LEAVE_ID, "leave_type", "ANNUAL", "status", "PENDING");
        when(mapper.findDetail(LEAVE_ID)).thenReturn(detail);

        // when
        Map<String, Object> result = service.getDetail(LEAVE_ID);

        // then
        assertThat(result).containsEntry("id", LEAVE_ID);
        verify(mapper).findDetail(LEAVE_ID);
    }

    // ==================== listPage() ====================

    @SuppressWarnings("unchecked")
    @Test
    void shouldListPageWithDefaultQuery() {
        // given
        HrLeaveQueryDTO query = new HrLeaveQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        HrLeave leave1 = new HrLeave();
        leave1.setId(1L);
        leave1.setEmpId(EMP_ID);
        leave1.setLeaveType("ANNUAL");
        leave1.setStatus("PENDING");

        Page<HrLeave> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(leave1));

        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // when
        PageResult<HrLeaveVO> result = service.listPage(EMP_ID, query);

        // then
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        HrLeaveVO vo = result.getList().get(0);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getLeaveType()).isEqualTo("ANNUAL");
        assertThat(vo.getStatus()).isEqualTo("PENDING");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldListPageWithStatusFilter() {
        // given
        HrLeaveQueryDTO query = new HrLeaveQueryDTO();
        query.setStatus("APPROVED");
        query.setPageNum(1);
        query.setPageSize(20);

        Page<HrLeave> mockPage = new Page<>(1, 20, 0);
        mockPage.setRecords(List.of());

        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // when
        PageResult<HrLeaveVO> result = service.listPage(EMP_ID, query);

        // then
        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();

        verify(mapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue()).isNotNull();
    }

    // ==================== listMyBalances() ====================

    @Test
    void shouldListMyBalances() {
        // given
        Map<String, Object> balance1 = Map.of("leave_type", "ANNUAL", "remaining_days", BigDecimal.TEN);
        Map<String, Object> balance2 = Map.of("leave_type", "SICK", "remaining_days", BigDecimal.valueOf(5));
        when(mapper.findBalancesByEmpId(EMP_ID)).thenReturn(List.of(balance1, balance2));

        // when
        List<Map<String, Object>> result = service.listMyBalances(EMP_ID);

        // then
        assertThat(result).hasSize(2);
        verify(mapper).findBalancesByEmpId(EMP_ID);
    }

    // ==================== helpers ====================

    private HrLeave aPendingLeave() {
        HrLeave leave = new HrLeave();
        leave.setId(LEAVE_ID);
        leave.setEmpId(EMP_ID);
        leave.setLeaveType("ANNUAL");
        leave.setStartDate(LocalDate.of(2026, 6, 10));
        leave.setEndDate(LocalDate.of(2026, 6, 12));
        leave.setTotalDays(BigDecimal.valueOf(3));
        leave.setReason("年假");
        leave.setStatus("PENDING");
        return leave;
    }
}
