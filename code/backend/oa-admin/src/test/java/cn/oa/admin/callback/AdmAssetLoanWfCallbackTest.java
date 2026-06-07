package cn.oa.admin.callback;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.entity.AdmAssetLoan;
import cn.oa.admin.mapper.AdmAssetLoanMapper;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.service.AdmAssetLoanService;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdmAssetLoanWfCallback 单元测试.
 *
 * <p>覆盖 handleEvent 分发 / onApproved 状态机 (BORROW→IN_USE, SCRAP→SCRAPPED) / onRejected 状态机.
 */
@ExtendWith(MockitoExtension.class)
class AdmAssetLoanWfCallbackTest {

    @Mock private AdmAssetLoanMapper mapper;
    @Mock private AdmAssetMapper assetMapper;

    @Captor private ArgumentCaptor<AdmAssetLoan> loanCaptor;
    @Captor private ArgumentCaptor<AdmAsset> assetCaptor;

    private AdmAssetLoanWfCallback callback;

    private static final Long LOAN_ID = 2000L;
    private static final Long ASSET_ID = 50L;
    private static final Long INSTANCE_ID = 8888L;

    @BeforeEach
    void setUp() {
        callback = new AdmAssetLoanWfCallback(mapper, assetMapper);
    }

    @Nested
    @DisplayName("handleEvent() 事件分发")
    class HandleEvent {

        @Test
        @DisplayName("ASSET_ + APPROVED — 调 onApproved")
        void approved_routesToOnApproved() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setAssetId(ASSET_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_IDLE);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", AdmConstants.BIZ_KEY_PREFIX_ASSET + LOAN_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
            verify(assetMapper, times(1)).updateById(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_STATUS_IN_USE);
        }

        @Test
        @DisplayName("ASSET_ + REJECTED — 调 onRejected, 不联动资产")
        void rejected_routesToOnRejected() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "REJECTED", AdmConstants.BIZ_KEY_PREFIX_ASSET + LOAN_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_REJECTED);
            verify(assetMapper, never()).updateById(any(AdmAsset.class));
        }

        @Test
        @DisplayName("非 ASSET_ 前缀 — 跳过")
        void nonAssetPrefix_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", "EXP_9999");

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).selectById(any());
        }

        @Test
        @DisplayName("非终态状态 — 跳过")
        void nonTerminalStatus_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "RUNNING", AdmConstants.BIZ_KEY_PREFIX_ASSET + LOAN_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).updateById(any(AdmAssetLoan.class));
        }
    }

    @Nested
    @DisplayName("onApproved() 状态机 + 资产联动")
    class OnApproved {

        @Test
        @DisplayName("BORROW 领用 — 资产变 IN_USE")
        void borrowAssetTurnsInUse() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setAssetId(ASSET_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_IDLE);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            // when
            callback.onApproved(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_APPROVED);

            verify(assetMapper).updateById(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_STATUS_IN_USE);
        }

        @Test
        @DisplayName("SCRAP 报废 — 资产变 SCRAPPED")
        void scrapAssetTurnsScrapped() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setAssetId(ASSET_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_SCRAP);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_REPAIR);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            // when
            callback.onApproved(LOAN_ID);

            // then
            verify(assetMapper).updateById(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_STATUS_SCRAPPED);
        }

        @Test
        @DisplayName("已是 APPROVED — 幂等跳过, 资产不联动")
        void alreadyApproved_idempotent() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onApproved(LOAN_ID);

            // then
            verify(mapper, never()).updateById(any(AdmAssetLoan.class));
            verify(assetMapper, never()).updateById(any(AdmAsset.class));
        }

        @Test
        @DisplayName("非 PENDING 状态 — 跳过, 不联动")
        void wrongStatus_skipped() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onApproved(LOAN_ID);

            // then
            verify(mapper, never()).updateById(any(AdmAssetLoan.class));
            verify(assetMapper, never()).updateById(any(AdmAsset.class));
        }

        @Test
        @DisplayName("领用单不存在 — 跳过, 资产不联动")
        void loanNotFound_skipped() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            callback.onApproved(999L);

            // then
            verify(assetMapper, never()).updateById(any(AdmAsset.class));
        }

        @Test
        @DisplayName("资产 ID 为 null — 状态仍更新, 不查资产")
        void nullAssetId_loanUpdated() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            loan.setAssetId(null);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onApproved(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
            verify(assetMapper, never()).selectById(any());
        }
    }

    @Nested
    @DisplayName("onRejected() 状态机")
    class OnRejected {

        @Test
        @DisplayName("PENDING → REJECTED")
        void pendingToRejected() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onRejected(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_REJECTED);
        }

        @Test
        @DisplayName("已是 REJECTED — 幂等跳过")
        void alreadyRejected_idempotent() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_REJECTED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onRejected(LOAN_ID);

            // then
            verify(mapper, never()).updateById(any(AdmAssetLoan.class));
        }

        @Test
        @DisplayName("非 PENDING 状态 (RETURNED) — 跳过")
        void returnedStatus_skipped() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_RETURNED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            callback.onRejected(LOAN_ID);

            // then
            verify(mapper, never()).updateById(any(AdmAssetLoan.class));
        }
    }

}
