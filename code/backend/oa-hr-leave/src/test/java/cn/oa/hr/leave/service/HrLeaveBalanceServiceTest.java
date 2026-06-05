package cn.oa.hr.leave.service;

import cn.oa.hr.leave.entity.HrLeaveBalance;
import cn.oa.hr.leave.mapper.HrLeaveBalanceMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrLeaveBalanceServiceTest {

    @Mock
    private HrLeaveBalanceMapper balanceMapper;

    @Captor
    private ArgumentCaptor<HrLeaveBalance> balanceCaptor;

    private HrLeaveBalanceService service;

    private static final Long EMP_ID = 42L;
    private static final Long BALANCE_ID = 10L;
    private static final String LEAVE_TYPE = "ANNUAL";
    private static final int YEAR = 2026;

    @BeforeEach
    void setUp() {
        service = new HrLeaveBalanceService(balanceMapper);
    }

    // ==================== getBalance() ====================

    @Test
    void shouldReturnBalanceWhenFound() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when
        HrLeaveBalance result = service.getBalance(EMP_ID, LEAVE_TYPE, YEAR);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmpId()).isEqualTo(EMP_ID);
        assertThat(result.getLeaveType()).isEqualTo(LEAVE_TYPE);
        assertThat(result.getYear()).isEqualTo(YEAR);
    }

    @Test
    void shouldReturnNullWhenBalanceNotFound() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(null);

        // when
        HrLeaveBalance result = service.getBalance(EMP_ID, LEAVE_TYPE, YEAR);

        // then
        assertThat(result).isNull();
    }

    // ==================== initBalance() ====================

    @Test
    void shouldInitBalanceSuccessfully() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(null);

        // when
        service.initBalance(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(10));

        // then
        verify(balanceMapper).insert(balanceCaptor.capture());
        HrLeaveBalance captured = balanceCaptor.getValue();
        assertThat(captured.getEmpId()).isEqualTo(EMP_ID);
        assertThat(captured.getLeaveType()).isEqualTo(LEAVE_TYPE);
        assertThat(captured.getYear()).isEqualTo(YEAR);
        assertThat(captured.getTotalDays()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(captured.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captured.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captured.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(captured.getStatus()).isEqualTo("ACTIVE");
        assertThat(captured.getCreateBy()).isEqualTo(String.valueOf(EMP_ID));
    }

    @Test
    void shouldThrowWhenBalanceAlreadyExists() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(aActiveBalance());

        // when / then
        assertThatThrownBy(() -> service.initBalance(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(10)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额记录已存在");

        verify(balanceMapper, never()).insert(any(HrLeaveBalance.class));
    }

    // ==================== adjustBalance() ====================

    @Test
    void shouldAdjustBalanceSuccessfully() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setTotalDays(BigDecimal.valueOf(10));
        balance.setRemainingDays(BigDecimal.valueOf(7));
        when(balanceMapper.selectById(BALANCE_ID)).thenReturn(balance);

        // when
        service.adjustBalance(BALANCE_ID, BigDecimal.valueOf(3), "增加额度");

        // then
        assertThat(balance.getTotalDays()).isEqualByComparingTo(BigDecimal.valueOf(13));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(10));
        verify(balanceMapper).updateById(balance);
    }

    @Test
    void shouldAdjustBalanceNegativeDays() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setTotalDays(BigDecimal.valueOf(10));
        balance.setRemainingDays(BigDecimal.valueOf(7));
        when(balanceMapper.selectById(BALANCE_ID)).thenReturn(balance);

        // when
        service.adjustBalance(BALANCE_ID, BigDecimal.valueOf(-2), "扣减额度");

        // then
        assertThat(balance.getTotalDays()).isEqualByComparingTo(BigDecimal.valueOf(8));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(5));
        verify(balanceMapper).updateById(balance);
    }

    @Test
    void shouldThrowWhenAdjustBalanceNotFound() {
        // given
        when(balanceMapper.selectById(999L)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> service.adjustBalance(999L, BigDecimal.ONE, "n/a"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额记录不存在");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    @Test
    void shouldThrowWhenAdjustedTotalNegative() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setTotalDays(BigDecimal.valueOf(5));
        when(balanceMapper.selectById(BALANCE_ID)).thenReturn(balance);

        // when / then
        assertThatThrownBy(() -> service.adjustBalance(BALANCE_ID, BigDecimal.valueOf(-10), "超扣"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("调整后总额度不能为负数");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    @Test
    void shouldThrowWhenAdjustedRemainingNegative() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setTotalDays(BigDecimal.valueOf(10));
        balance.setRemainingDays(BigDecimal.valueOf(2));
        when(balanceMapper.selectById(BALANCE_ID)).thenReturn(balance);

        // when / then
        assertThatThrownBy(() -> service.adjustBalance(BALANCE_ID, BigDecimal.valueOf(-5), "超扣"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("调整后剩余天数不能为负数");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    // ==================== freezeOnSubmit() ====================

    @Test
    void shouldFreezeOnSubmitSuccessfully() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setRemainingDays(BigDecimal.valueOf(10));
        balance.setFrozenDays(BigDecimal.valueOf(2));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when
        service.freezeOnSubmit(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3));

        // then
        assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(7));
        verify(balanceMapper).updateById(balance);
    }

    @Test
    void shouldThrowWhenFreezeBalanceNotFound() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> service.freezeOnSubmit(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额记录不存在");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    @Test
    void shouldThrowWhenFreezeInsufficientBalance() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setRemainingDays(BigDecimal.valueOf(2));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when / then
        assertThatThrownBy(() -> service.freezeOnSubmit(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(5)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额不足");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    // ==================== deductOnApprove() ====================

    @Test
    void shouldDeductOnApproveSuccessfully() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setFrozenDays(BigDecimal.valueOf(5));
        balance.setUsedDays(BigDecimal.valueOf(3));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when
        service.deductOnApprove(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3));

        // then
        assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(6));
        verify(balanceMapper).updateById(balance);
    }

    @Test
    void shouldThrowWhenDeductBalanceNotFound() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> service.deductOnApprove(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("余额记录不存在");

        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    @Test
    void shouldResetFrozenToZeroWhenNegativeOnDeduct() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setFrozenDays(BigDecimal.valueOf(2));
        balance.setUsedDays(BigDecimal.valueOf(3));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when (deduct more than frozen)
        service.deductOnApprove(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(5));

        // then
        assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(8));
        verify(balanceMapper).updateById(balance);
    }

    // ==================== unfreezeOnReject() ====================

    @Test
    void shouldUnfreezeOnRejectSuccessfully() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setFrozenDays(BigDecimal.valueOf(5));
        balance.setRemainingDays(BigDecimal.valueOf(5));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when
        service.unfreezeOnReject(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3));

        // then
        assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(8));
        verify(balanceMapper).updateById(balance);
    }

    @Test
    void shouldNotThrowWhenUnfreezeBalanceNotFound() {
        // given
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(null);

        // when (should not throw)
        service.unfreezeOnReject(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3));

        // then
        verify(balanceMapper, never()).updateById(any(HrLeaveBalance.class));
    }

    @Test
    void shouldResetFrozenToZeroWhenNegativeOnUnfreeze() {
        // given
        HrLeaveBalance balance = aActiveBalance();
        balance.setFrozenDays(BigDecimal.valueOf(1));
        balance.setRemainingDays(BigDecimal.valueOf(9));
        when(balanceMapper.findByEmpAndTypeAndYear(EMP_ID, LEAVE_TYPE, YEAR)).thenReturn(balance);

        // when (unfreeze more than frozen)
        service.unfreezeOnReject(EMP_ID, LEAVE_TYPE, YEAR, BigDecimal.valueOf(3));

        // then
        assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(12));
        verify(balanceMapper).updateById(balance);
    }

    // ==================== helpers ====================

    private HrLeaveBalance aActiveBalance() {
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setId(BALANCE_ID);
        balance.setEmpId(EMP_ID);
        balance.setLeaveType(LEAVE_TYPE);
        balance.setYear(YEAR);
        balance.setTotalDays(BigDecimal.valueOf(10));
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setFrozenDays(BigDecimal.ZERO);
        balance.setRemainingDays(BigDecimal.valueOf(10));
        balance.setStatus("ACTIVE");
        return balance;
    }
}
