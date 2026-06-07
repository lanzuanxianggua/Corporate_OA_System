package cn.oa.finance.service.impl;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinBudgetMapper;
import cn.oa.finance.service.FinBudgetService;
import cn.oa.finance.vo.FinBudgetVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FinBudgetService 补充单测 — 覆盖 listPage / freezeOnExpense / unfreezeOnReject / deductOnApprove / getAvailableBalance.
 *
 * <p>既有 {@link cn.oa.finance.service.FinBudgetServiceTest} 已覆盖 create / update / delete / getById.
 * 本测试类不重复, 聚焦于"分页 + 预算冻结/解冻/扣减" 4 个核心业务行为 + 余额查询.
 */
@ExtendWith(MockitoExtension.class)
class FinBudgetServiceImplTest {

    @Mock
    private FinBudgetMapper mapper;

    @Captor
    private ArgumentCaptor<FinBudget> budgetCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<FinBudget>> wrapperCaptor;

    private FinBudgetService service;

    @BeforeEach
    void setUp() {
        service = new FinBudgetService(mapper);
    }

    @Nested
    @DisplayName("listPage() 分页查询预算")
    class ListPage {

        @Test
        @DisplayName("按 deptId 过滤分页 — 返回 PageResult<FinBudgetVO>")
        void listPage_withDeptId() {
            // given
            FinBudget b1 = new FinBudget();
            b1.setId(1L);
            b1.setEmpId(10L);
            b1.setDeptId(100L);
            b1.setBudgetYear(2026);
            b1.setBudgetName("2026 部门预算");
            b1.setTotalAmount(BigDecimal.valueOf(500_000));
            b1.setUsedAmount(BigDecimal.ZERO);
            b1.setFrozenAmount(BigDecimal.ZERO);
            b1.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            FinBudget b2 = new FinBudget();
            b2.setId(2L);
            b2.setEmpId(11L);
            b2.setDeptId(100L);
            b2.setBudgetYear(2025);
            b2.setBudgetName("2025 部门预算");
            b2.setTotalAmount(BigDecimal.valueOf(400_000));
            b2.setStatus(FinConstants.BUDGET_STATUS_CLOSED);

            Page<FinBudget> page = new Page<>(1, 10);
            page.setRecords(List.of(b1, b2));
            page.setTotal(2);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // when
            PageResult<FinBudgetVO> result = service.listPage(1, 10, 100L);

            // then
            assertThat(result.getTotal()).isEqualTo(2L);
            assertThat(result.getList()).hasSize(2);
            assertThat(result.getList().get(0).getBudgetName()).isEqualTo("2026 部门预算");
            assertThat(result.getList().get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
            assertThat(result.getList().get(1).getStatus()).isEqualTo(FinConstants.BUDGET_STATUS_CLOSED);

            verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("deptId=null 时不过滤部门 — 返回所有预算")
        void listPage_noDeptId() {
            // given
            Page<FinBudget> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            // when
            PageResult<FinBudgetVO> result = service.listPage(1, 10, null);

            // then
            assertThat(result.getList()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
            verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("freezeOnExpense() 报销提交冻结预算")
    class FreezeOnExpense {

        @Test
        @DisplayName("成功冻结 — available >= amount, 调 atomicFreeze")
        void freeze_success() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setDeptId(100L);
            budget.setBudgetYear(2026);
            budget.setTotalAmount(BigDecimal.valueOf(100_000));
            budget.setUsedAmount(BigDecimal.valueOf(20_000));
            budget.setFrozenAmount(BigDecimal.valueOf(10_000));
            budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(budget);

            // when
            service.freezeOnExpense(100L, 2026, BigDecimal.valueOf(30_000));

            // then
            verify(mapper).selectActiveForUpdate(100L, 2026);
            verify(mapper).atomicFreeze(eq(1L), eq(BigDecimal.valueOf(30_000)));
        }

        @Test
        @DisplayName("预算不足时抛出 BizException (BAD_REQUEST)")
        void freeze_insufficientBudget_throws() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setTotalAmount(BigDecimal.valueOf(100_000));
            budget.setUsedAmount(BigDecimal.valueOf(80_000));
            budget.setFrozenAmount(BigDecimal.valueOf(15_000)); // available = 5000
            budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(budget);

            // when & then
            assertThatThrownBy(() -> service.freezeOnExpense(100L, 2026, BigDecimal.valueOf(20_000)))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预算不足")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.BAD_REQUEST.getCode()));

            verify(mapper, never()).atomicFreeze(any(), any());
        }

        @Test
        @DisplayName("未找到 ACTIVE 预算时跳过冻结 (log warn, 不抛异常)")
        void freeze_noActiveBudget_skipped() {
            // given
            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(null);

            // when
            service.freezeOnExpense(100L, 2026, BigDecimal.valueOf(10_000));

            // then
            verify(mapper, never()).atomicFreeze(any(), any());
        }

        @Test
        @DisplayName("amount <= 0 时静默跳过")
        void freeze_zeroAmount_skipped() {
            // when
            service.freezeOnExpense(100L, 2026, BigDecimal.ZERO);

            // then
            verify(mapper, never()).selectActiveForUpdate(any(), any(Integer.class));
            verify(mapper, never()).atomicFreeze(any(), any());
        }
    }

    @Nested
    @DisplayName("unfreezeOnReject() 解冻预算 (驳回/撤回)")
    class UnfreezeOnReject {

        @Test
        @DisplayName("成功解冻 — 调 atomicUnfreeze")
        void unfreeze_success() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setTotalAmount(BigDecimal.valueOf(100_000));
            budget.setFrozenAmount(BigDecimal.valueOf(30_000));
            budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(budget);

            // when
            service.unfreezeOnReject(100L, 2026, BigDecimal.valueOf(15_000));

            // then
            verify(mapper).atomicUnfreeze(1L, BigDecimal.valueOf(15_000));
        }

        @Test
        @DisplayName("未找到 ACTIVE 预算时跳过解冻 (log warn)")
        void unfreeze_noBudget_skipped() {
            // given
            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(null);

            // when
            service.unfreezeOnReject(100L, 2026, BigDecimal.valueOf(10_000));

            // then
            verify(mapper, never()).atomicUnfreeze(any(), any());
        }
    }

    @Nested
    @DisplayName("deductOnApprove() 审批通过 (frozen 转 used)")
    class DeductOnApprove {

        @Test
        @DisplayName("成功扣减 — 调 atomicDeduct")
        void deduct_success() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setTotalAmount(BigDecimal.valueOf(100_000));
            budget.setFrozenAmount(BigDecimal.valueOf(30_000));
            budget.setUsedAmount(BigDecimal.valueOf(20_000));
            budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(budget);

            // when
            service.deductOnApprove(100L, 2026, BigDecimal.valueOf(20_000));

            // then
            verify(mapper).atomicDeduct(1L, BigDecimal.valueOf(20_000));
        }

        @Test
        @DisplayName("未找到 ACTIVE 预算时跳过扣减 (log warn)")
        void deduct_noBudget_skipped() {
            // given
            when(mapper.selectActiveForUpdate(100L, 2026)).thenReturn(null);

            // when
            service.deductOnApprove(100L, 2026, BigDecimal.valueOf(10_000));

            // then
            verify(mapper, never()).atomicDeduct(any(), any());
        }
    }

    @Nested
    @DisplayName("getAvailableBalance() 查询可用余额")
    class GetAvailableBalance {

        @Test
        @DisplayName("deptId/year 均有 — 返回 total - used - frozen")
        void getAvailableBalance_withBudget() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setTotalAmount(BigDecimal.valueOf(100_000));
            budget.setUsedAmount(BigDecimal.valueOf(30_000));
            budget.setFrozenAmount(BigDecimal.valueOf(20_000));
            budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(budget);

            // when
            BigDecimal balance = service.getAvailableBalance(100L, 2026);

            // then
            assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(50_000));
            verify(mapper).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无预算时返回 ZERO")
        void getAvailableBalance_noBudget() {
            // given
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // when
            BigDecimal balance = service.getAvailableBalance(999L, 2026);

            // then
            assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("deptId 或 year 为 null 时返回 ZERO, 不查 mapper")
        void getAvailableBalance_nullParam() {
            // when
            assertThat(service.getAvailableBalance(null, 2026)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(service.getAvailableBalance(100L, null)).isEqualByComparingTo(BigDecimal.ZERO);

            // then
            verify(mapper, never()).selectOne(any(LambdaQueryWrapper.class));
        }
    }
}
