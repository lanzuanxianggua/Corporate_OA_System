package cn.oa.finance.service;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinBudgetMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FinBudgetService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class FinBudgetServiceTest {

    @Mock
    private FinBudgetMapper mapper;

    @Captor
    private ArgumentCaptor<FinBudget> budgetCaptor;

    private FinBudgetService service;

    private static final Long EMP_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new FinBudgetService(mapper);
    }

    @Nested
    @DisplayName("create() 创建预算")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回预算ID, 默认状态 ACTIVE")
        void create_success() {
            // given
            FinBudgetCreateDTO dto = new FinBudgetCreateDTO();
            dto.setBudgetName("2026年部门运营预算");
            dto.setBudgetYear(2026);
            dto.setTotalAmount(BigDecimal.valueOf(500000));

            when(mapper.insert(any(FinBudget.class))).thenAnswer(invocation -> {
                FinBudget b = invocation.getArgument(0);
                b.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(budgetCaptor.capture());

            FinBudget saved = budgetCaptor.getValue();
            assertThat(saved.getBudgetName()).isEqualTo("2026年部门运营预算");
            assertThat(saved.getBudgetYear()).isEqualTo(2026);
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
            assertThat(saved.getUsedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getStatus()).isEqualTo(FinConstants.BUDGET_STATUS_ACTIVE);
        }
    }

    @Nested
    @DisplayName("update() 更新预算")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            FinBudget exist = new FinBudget();
            exist.setId(1L);
            exist.setBudgetName("旧预算");
            exist.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

            FinBudgetCreateDTO dto = new FinBudgetCreateDTO();
            dto.setBudgetName("新预算");
            dto.setBudgetYear(2027);
            dto.setTotalAmount(BigDecimal.valueOf(600000));

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.update(1L, dto);

            // then
            verify(mapper).selectById(1L);
            verify(mapper).updateById(budgetCaptor.capture());

            FinBudget patch = budgetCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getBudgetName()).isEqualTo("新预算");
            assertThat(patch.getBudgetYear()).isEqualTo(2027);
            assertThat(patch.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(600000));
        }

        @Test
        @DisplayName("预算不存在时抛出 BizException")
        void update_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.update(999L, new FinBudgetCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预算不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).updateById(any(FinBudget.class));
        }

        @Test
        @DisplayName("非 ACTIVE 状态预算抛出 BizException")
        void update_notActive_throwsException() {
            // given
            FinBudget exist = new FinBudget();
            exist.setId(1L);
            exist.setStatus(FinConstants.BUDGET_STATUS_FROZEN);

            when(mapper.selectById(1L)).thenReturn(exist);

            // when & then
            assertThatThrownBy(() -> service.update(1L, new FinBudgetCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 ACTIVE 状态的预算可更新");

            verify(mapper, never()).updateById(any(FinBudget.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除预算")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // given
            FinBudget exist = new FinBudget();
            exist.setId(1L);

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.delete(1L);

            // then
            verify(mapper).selectById(1L);
            verify(mapper).deleteById(1L);
        }

        @Test
        @DisplayName("预算不存在时抛出 BizException")
        void delete_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预算不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getById() 查询预算")
    class GetById {

        @Test
        @DisplayName("查询成功")
        void getById_success() {
            // given
            FinBudget budget = new FinBudget();
            budget.setId(1L);
            budget.setBudgetName("2026预算");
            budget.setBudgetYear(2026);
            budget.setTotalAmount(BigDecimal.valueOf(500000));

            when(mapper.selectById(1L)).thenReturn(budget);

            // when
            FinBudget result = service.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getBudgetName()).isEqualTo("2026预算");
            assertThat(result.getBudgetYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("预算不存在时抛出 BizException")
        void getById_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预算不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }
}
