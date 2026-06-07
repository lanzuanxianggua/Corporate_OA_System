package cn.oa.finance.service.impl;

import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanQueryDTO;
import cn.oa.finance.entity.FinLoan;
import cn.oa.finance.entity.FinLoanRepayment;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.event.FinBusinessSubmittedEvent;
import cn.oa.finance.mapper.FinLoanMapper;
import cn.oa.finance.mapper.FinLoanRepaymentMapper;
import cn.oa.finance.service.FinLoanService;
import cn.oa.finance.vo.FinLoanVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FinLoanService 单测 — 覆盖 create/repay/approve/reject/listPage 4 个核心场景.
 */
@ExtendWith(MockitoExtension.class)
class FinLoanServiceImplTest {

    @Mock private FinLoanMapper mapper;
    @Mock private FinLoanRepaymentMapper repaymentMapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<FinLoan> loanCaptor;
    @Captor private ArgumentCaptor<FinLoanRepayment> repaymentCaptor;
    @Captor private ArgumentCaptor<FinBusinessSubmittedEvent> eventCaptor;

    private FinLoanService service;

    private static final Long EMP_ID = 1L;
    private static final Long LOAN_ID = 5000L;
    private static final Long WF_INSTANCE_ID = 8888L;

    @BeforeEach
    void setUp() {
        service = new FinLoanService(mapper, repaymentMapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 提交借款")
    class Create {

        @Test
        @DisplayName("提交成功 — 启动 workflow + 发布事件, 状态 PENDING")
        void create_success() {
            // given
            FinLoanCreateDTO dto = new FinLoanCreateDTO();
            dto.setLoanType(FinConstants.LOAN_TYPE_TRAVEL);
            dto.setAmount(BigDecimal.valueOf(5000));
            dto.setPurpose("出差备用金");
            dto.setDeadlineDate(LocalDate.of(2026, 7, 1));

            doAnswer(inv -> {
                FinLoan l = inv.getArgument(0);
                l.setId(LOAN_ID);
                return 1;
            }).when(mapper).insert(any(FinLoan.class));

            when(wfInstanceService.start("finance_loan", "LOAN_" + LOAN_ID, EMP_ID))
                    .thenReturn(WF_INSTANCE_ID);

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(LOAN_ID);

            // 1) insert 借款单 (DRAFT)
            verify(mapper, times(1)).insert(any(FinLoan.class));

            // 2) 启动 workflow
            verify(wfInstanceService).start("finance_loan", "LOAN_" + LOAN_ID, EMP_ID);

            // 3) updateById 把 status 改为 PENDING + 回写 wfInstanceId
            verify(mapper, times(1)).updateById(loanCaptor.capture());
            FinLoan updated = loanCaptor.getValue();
            assertThat(updated.getId()).isEqualTo(LOAN_ID);
            assertThat(updated.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
            assertThat(updated.getStatus()).isEqualTo(FinConstants.LOAN_STATUS_PENDING);
            assertThat(updated.getRepaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);

            // 4) 发布事件
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            FinBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo("LOAN_");
            assertThat(ev.getBusinessId()).isEqualTo(LOAN_ID);
            assertThat(ev.getSubmitterId()).isEqualTo(EMP_ID);
            assertThat(ev.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
        }
    }

    @Nested
    @DisplayName("repay() 还款")
    class Repay {

        @Test
        @DisplayName("部分还款 — 创建还款记录, repaid += amount, 状态保持 APPROVED")
        void repay_partial() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setAmount(BigDecimal.valueOf(10_000));
            loan.setRepaidAmount(BigDecimal.valueOf(3_000));
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.repay(LOAN_ID, BigDecimal.valueOf(2_000), null);

            // then
            verify(repaymentMapper).insert(repaymentCaptor.capture());
            FinLoanRepayment repayment = repaymentCaptor.getValue();
            assertThat(repayment.getLoanId()).isEqualTo(LOAN_ID);
            assertThat(repayment.getRepayAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000));
            assertThat(repayment.getRepayType()).isEqualTo(FinConstants.REPAY_TYPE_CASH);
            assertThat(repayment.getExpenseId()).isNull();

            verify(mapper).updateById(loanCaptor.capture());
            FinLoan updated = loanCaptor.getValue();
            assertThat(updated.getRepaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(5_000));
            assertThat(updated.getStatus()).isEqualTo(FinConstants.LOAN_STATUS_APPROVED); // 未还清, 状态不变
        }

        @Test
        @DisplayName("还清 (repaid == amount) — 状态置为 SETTLED")
        void repay_fullSettled() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setAmount(BigDecimal.valueOf(10_000));
            loan.setRepaidAmount(BigDecimal.valueOf(7_000));
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.repay(LOAN_ID, BigDecimal.valueOf(3_000), null);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            FinLoan updated = loanCaptor.getValue();
            assertThat(updated.getRepaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
            assertThat(updated.getStatus()).isEqualTo(FinConstants.LOAN_STATUS_SETTLED);
        }

        @Test
        @DisplayName("报销冲抵还款 — repay_type=EXPENSE_OFFSET, expenseId 写入")
        void repay_expenseOffset() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setAmount(BigDecimal.valueOf(10_000));
            loan.setRepaidAmount(BigDecimal.ZERO);
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.repay(LOAN_ID, BigDecimal.valueOf(5_000), 123L);

            // then
            verify(repaymentMapper).insert(repaymentCaptor.capture());
            FinLoanRepayment repayment = repaymentCaptor.getValue();
            assertThat(repayment.getRepayType()).isEqualTo(FinConstants.REPAY_TYPE_EXPENSE_OFFSET);
            assertThat(repayment.getExpenseId()).isEqualTo(123L);
        }

        @Test
        @DisplayName("PENDING 状态 — 抛 BizException (不允许还款)")
        void repay_pending_throws() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(FinConstants.LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.repay(LOAN_ID, BigDecimal.valueOf(1000), null))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅已审批的借款可还款");
        }

        @Test
        @DisplayName("还款超额 — 抛 BizException")
        void repay_exceed_throws() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setAmount(BigDecimal.valueOf(10_000));
            loan.setRepaidAmount(BigDecimal.valueOf(8_000));
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.repay(LOAN_ID, BigDecimal.valueOf(5_000), null))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("还款金额超出借款总额");
        }
    }

    @Nested
    @DisplayName("approve() / reject() 业务层审批 / 驳回")
    class ApproveReject {

        @Test
        @DisplayName("approve — PENDING → APPROVED")
        void approve_success() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(FinConstants.LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.approve(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(FinConstants.LOAN_STATUS_APPROVED);
        }

        @Test
        @DisplayName("reject — PENDING → REJECTED")
        void reject_success() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(FinConstants.LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.reject(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(FinConstants.LOAN_STATUS_REJECTED);
        }

        @Test
        @DisplayName("reject 非 PENDING 状态 — 抛 BizException")
        void reject_invalidStatus() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.reject(LOAN_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅待审批状态可驳回");
        }
    }

    @Nested
    @DisplayName("getById() / listPage() 查询")
    class Query {

        @Test
        @DisplayName("getById — 返回头+还款记录 Map")
        void getById_withRepayments() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setApplyNo("LOAN" + System.currentTimeMillis());
            loan.setEmpId(EMP_ID);
            loan.setAmount(BigDecimal.valueOf(10_000));
            loan.setRepaidAmount(BigDecimal.valueOf(2_000));
            loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            FinLoanRepayment r = new FinLoanRepayment();
            r.setLoanId(LOAN_ID);
            r.setRepayAmount(BigDecimal.valueOf(2_000));
            r.setRepayType(FinConstants.REPAY_TYPE_CASH);
            when(repaymentMapper.findByLoanId(LOAN_ID)).thenReturn(List.of(r));

            // when
            Map<String, Object> result = service.getById(LOAN_ID);

            // then
            assertThat(result).containsKeys("id", "applyNo", "amount", "repaidAmount", "status", "repayments");
            assertThat(result.get("id")).isEqualTo(LOAN_ID);
            assertThat((BigDecimal) result.get("amount")).isEqualByComparingTo(BigDecimal.valueOf(10_000));
            assertThat((List<?>) result.get("repayments")).hasSize(1);
        }

        @Test
        @DisplayName("getById 借款单不存在 — 抛 NOT_FOUND")
        void getById_notFound() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("借款单不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("listPage — 按 empId 过滤, status 可选, 返回 PageResult<FinLoanVO>")
        void listPage() {
            // given
            FinLoan loan = new FinLoan();
            loan.setId(LOAN_ID);
            loan.setApplyNo("LOAN123");
            loan.setEmpId(EMP_ID);
            loan.setAmount(BigDecimal.valueOf(5000));
            loan.setStatus(FinConstants.LOAN_STATUS_PENDING);
            Page<FinLoan> page = new Page<>(1, 10);
            page.setRecords(List.of(loan));
            page.setTotal(1);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            FinLoanQueryDTO query = new FinLoanQueryDTO();
            query.setStatus(FinConstants.LOAN_STATUS_PENDING);
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<FinLoanVO> result = service.listPage(query, EMP_ID);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getStatus()).isEqualTo(FinConstants.LOAN_STATUS_PENDING);
        }
    }
}
