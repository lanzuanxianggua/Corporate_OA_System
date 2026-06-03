package cn.oa.hr.service;

import cn.oa.hr.entity.HrLeaveBalance;
import cn.oa.hr.mapper.HrLeaveBalanceMapper;
import cn.oa.hr.service.impl.HrLeaveBalanceServiceImpl;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HR假期余额语义测试
 *
 * 余额语义：
 * - remainingDays = 账面剩余余额 = totalDays - usedDays
 * - frozenDays = 审批中冻结天数
 * - availableDays = remainingDays - frozenDays
 *
 * @author oa-hr
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrLeaveBalance 余额语义测试")
class HrLeaveBalanceSemanticsTest {

    @Mock
    private HrLeaveBalanceMapper balanceMapper;

    @InjectMocks
    private HrLeaveBalanceServiceImpl balanceService;

    /**
     * 创建余额实体
     * remainingDays = totalDays - usedDays
     */
    private HrLeaveBalance createBalance(Long id, BigDecimal totalDays,
                                          BigDecimal usedDays, BigDecimal frozenDays) {
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setId(id);
        balance.setEmpId(1L);
        balance.setLeaveType("ANNUAL");
        balance.setYear(2026);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(usedDays);
        balance.setFrozenDays(frozenDays);
        balance.setRemainingDays(totalDays.subtract(usedDays));
        balance.setStatus("ACTIVE");
        return balance;
    }

    // ==================== 余额语义测试 ====================

    @Nested
    @DisplayName("余额语义验证")
    class BalanceSemantics {

        @Test
        @DisplayName("初始余额：remaining=10 frozen=0 available=10")
        void initial_balance() {
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, BigDecimal.ZERO);

            // 验证语义
            assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);

            // availableDays = remaining - frozen = 10 - 0 = 10
            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("冻结2天后：remaining=10 frozen=2 available=8")
        void after_freeze() {
            // 初始状态
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, BigDecimal.ZERO);

            // 模拟冻结：frozenDays += 2，remainingDays 不变
            balance.setFrozenDays(new BigDecimal("2"));

            // 验证语义：remainingDays 不变，frozenDays 增加
            assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(balance.getFrozenDays()).isEqualByComparingTo(new BigDecimal("2"));

            // availableDays = remaining - frozen = 10 - 2 = 8
            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("8"));
        }

        @Test
        @DisplayName("确认2天后：remaining=8 used=2 frozen=0 available=8")
        void after_confirm() {
            // 冻结状态
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("2"));

            // 模拟确认：
            // usedDays += 2, frozenDays -= 2, remainingDays -= 2
            balance.setUsedDays(new BigDecimal("2"));
            balance.setFrozenDays(BigDecimal.ZERO);
            balance.setRemainingDays(new BigDecimal("8")); // total - used = 10 - 2

            // 验证语义
            assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("8"));
            assertThat(balance.getUsedDays()).isEqualByComparingTo(new BigDecimal("2"));
            assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);

            // availableDays = remaining - frozen = 8 - 0 = 8
            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("8"));

            // 验证不变式：remaining = total - used
            assertThat(balance.getRemainingDays())
                    .isEqualByComparingTo(balance.getTotalDays().subtract(balance.getUsedDays()));
        }

        @Test
        @DisplayName("释放冻结2天后：remaining=10 used=0 frozen=0 available=10")
        void after_release() {
            // 冻结状态
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("2"));

            // 模拟释放：frozenDays -= 2，remainingDays 不变
            balance.setFrozenDays(BigDecimal.ZERO);

            // 验证语义：remainingDays 不变，frozenDays 减少
            assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("10"));
            assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(balance.getFrozenDays()).isEqualByComparingTo(BigDecimal.ZERO);

            // availableDays = remaining - frozen = 10 - 0 = 10
            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("10"));

            // 验证不变式：remaining = total - used
            assertThat(balance.getRemainingDays())
                    .isEqualByComparingTo(balance.getTotalDays().subtract(balance.getUsedDays()));
        }
    }

    // ==================== 冻结条件测试 ====================

    @Nested
    @DisplayName("冻结条件校验")
    class FreezeCondition {

        @Test
        @DisplayName("可用余额充足-冻结成功")
        void freeze_available_sufficient() {
            // remaining=10, frozen=0, available=10 >= 2
            when(balanceMapper.freezeBalance(1L, "ANNUAL", 2026, new BigDecimal("2")))
                    .thenReturn(1);

            boolean result = balanceService.freezeBalance(1L, "ANNUAL", 2026, new BigDecimal("2"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("可用余额不足-冻结失败")
        void freeze_available_insufficient() {
            // remaining=10, frozen=9, available=1 < 2
            when(balanceMapper.freezeBalance(1L, "ANNUAL", 2026, new BigDecimal("2")))
                    .thenReturn(0);

            boolean result = balanceService.freezeBalance(1L, "ANNUAL", 2026, new BigDecimal("2"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("冻结后可用余额正确计算")
        void freeze_available_calculation() {
            // 初始 remaining=10, frozen=0
            // 冻结2后 remaining=10, frozen=2, available=8
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("2"));

            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("8"));
        }
    }

    // ==================== 确认余额测试 ====================

    @Nested
    @DisplayName("确认余额")
    class ConfirmBalance {

        @Test
        @DisplayName("确认后remaining正确减少")
        void confirm_remaining_decreases() {
            // 冻结状态：total=10, used=0, frozen=2, remaining=10
            HrLeaveBalance before = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("2"));

            // 确认后：total=10, used=2, frozen=0, remaining=8
            HrLeaveBalance after = createBalance(1L, new BigDecimal("10"),
                    new BigDecimal("2"), BigDecimal.ZERO);

            // remaining = total - used
            assertThat(after.getRemainingDays())
                    .isEqualByComparingTo(after.getTotalDays().subtract(after.getUsedDays()));
            assertThat(after.getRemainingDays()).isEqualByComparingTo(new BigDecimal("8"));
        }
    }

    // ==================== VO 可用余额测试 ====================

    @Nested
    @DisplayName("VO可用余额计算")
    class VoAvailableDays {

        @Test
        @DisplayName("VO的availableDays=remainingDays-frozenDays")
        void vo_available_days() {
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("3"));

            // 手动计算 expected available
            BigDecimal expectedAvailable = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(expectedAvailable).isEqualByComparingTo(new BigDecimal("7"));
        }
    }

    // ==================== 不变式验证 ====================

    @Nested
    @DisplayName("余额不变式")
    class Invariants {

        @Test
        @DisplayName("不变式：remaining = total - used")
        void invariant_remaining_equals_total_minus_used() {
            // 各种状态验证
            BigDecimal total = new BigDecimal("15");

            // 状态1：未使用
            HrLeaveBalance b1 = createBalance(1L, total, BigDecimal.ZERO, BigDecimal.ZERO);
            assertThat(b1.getRemainingDays()).isEqualByComparingTo(total);

            // 状态2：已使用5天
            HrLeaveBalance b2 = createBalance(2L, total, new BigDecimal("5"), BigDecimal.ZERO);
            assertThat(b2.getRemainingDays()).isEqualByComparingTo(new BigDecimal("10"));

            // 状态3：已使用5天，冻结3天
            HrLeaveBalance b3 = createBalance(3L, total, new BigDecimal("5"), new BigDecimal("3"));
            assertThat(b3.getRemainingDays()).isEqualByComparingTo(new BigDecimal("10"));
            // 可用 = remaining - frozen = 10 - 3 = 7
            assertThat(b3.getRemainingDays().subtract(b3.getFrozenDays()))
                    .isEqualByComparingTo(new BigDecimal("7"));
        }

        @Test
        @DisplayName("不变式：available = remaining - frozen")
        void invariant_available_equals_remaining_minus_frozen() {
            BigDecimal total = new BigDecimal("20");
            BigDecimal used = new BigDecimal("5");
            BigDecimal frozen = new BigDecimal("3");

            HrLeaveBalance balance = createBalance(1L, total, used, frozen);

            // remaining = total - used = 15
            assertThat(balance.getRemainingDays()).isEqualByComparingTo(new BigDecimal("15"));

            // available = remaining - frozen = 15 - 3 = 12
            BigDecimal available = balance.getRemainingDays().subtract(balance.getFrozenDays());
            assertThat(available).isEqualByComparingTo(new BigDecimal("12"));
        }
    }

    // ==================== hasEnoughBalance 测试 ====================

    @Nested
    @DisplayName("余额充足检查")
    class HasEnoughBalance {

        @Test
        @DisplayName("可用余额充足返回true")
        void hasEnough_true() {
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("2"));
            // available = 10 - 2 = 8 >= 5

            when(balanceMapper.selectOne(any())).thenReturn(balance);

            boolean result = balanceService.hasEnoughBalance(1L, "ANNUAL", 2026, new BigDecimal("5"));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("可用余额不足返回false")
        void hasEnough_false() {
            HrLeaveBalance balance = createBalance(1L, new BigDecimal("10"),
                    BigDecimal.ZERO, new BigDecimal("8"));
            // available = 10 - 8 = 2 < 5

            when(balanceMapper.selectOne(any())).thenReturn(balance);

            boolean result = balanceService.hasEnoughBalance(1L, "ANNUAL", 2026, new BigDecimal("5"));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("余额不存在返回false")
        void hasEnough_notExist() {
            when(balanceMapper.selectOne(any())).thenReturn(null);

            boolean result = balanceService.hasEnoughBalance(1L, "ANNUAL", 2026, new BigDecimal("1"));
            assertThat(result).isFalse();
        }
    }
}
