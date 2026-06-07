package cn.oa.admin.callback;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.entity.AdmSealApply;
import cn.oa.admin.mapper.AdmSealApplyMapper;
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
 * AdmSealApplyWfCallback 单元测试.
 *
 * <p>覆盖 handleEvent 分发 / onApproved 状态机 / onRejected 状态机 / 幂等跳过.
 */
@ExtendWith(MockitoExtension.class)
class AdmSealApplyWfCallbackTest {

    @Mock private AdmSealApplyMapper mapper;

    @Captor private ArgumentCaptor<AdmSealApply> applyCaptor;

    private AdmSealApplyWfCallback callback;

    private static final Long APPLY_ID = 1000L;
    private static final Long INSTANCE_ID = 9999L;

    @BeforeEach
    void setUp() {
        callback = new AdmSealApplyWfCallback(mapper);
    }

    @Nested
    @DisplayName("handleEvent() 事件分发")
    class HandleEvent {

        @Test
        @DisplayName("SEAL_ 前缀 + APPROVED — 调 onApproved")
        void approvedEvent_routesToOnApproved() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", AdmConstants.BIZ_KEY_PREFIX_SEAL + APPLY_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(applyCaptor.capture());
            assertThat(applyCaptor.getValue().getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
        }

        @Test
        @DisplayName("SEAL_ 前缀 + REJECTED — 调 onRejected")
        void rejectedEvent_routesToOnRejected() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "REJECTED", AdmConstants.BIZ_KEY_PREFIX_SEAL + APPLY_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(applyCaptor.capture());
            assertThat(applyCaptor.getValue().getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_REJECTED);
        }

        @Test
        @DisplayName("非 SEAL_ 前缀 — 跳过, 不查 mapper")
        void nonSealPrefix_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", "FINANCE_8888");

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).selectById(any());
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("非终态状态 (PENDING/RUNNING) — 跳过")
        void nonTerminalStatus_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "RUNNING", AdmConstants.BIZ_KEY_PREFIX_SEAL + APPLY_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("businessKey 数字解析失败 — 静默跳过")
        void invalidBusinessKey_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", AdmConstants.BIZ_KEY_PREFIX_SEAL + "NOT_A_NUMBER");

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).selectById(any());
        }

        @Test
        @DisplayName("businessKey 为 null — 跳过")
        void nullBusinessKey_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(INSTANCE_ID, "APPROVED", null);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).selectById(any());
        }
    }

    @Nested
    @DisplayName("onApproved() 状态机")
    class OnApproved {

        @Test
        @DisplayName("PENDING → APPROVED — 状态更新")
        void onApproved_pendingToApproved() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onApproved(APPLY_ID);

            // then
            verify(mapper).updateById(applyCaptor.capture());
            assertThat(applyCaptor.getValue().getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
        }

        @Test
        @DisplayName("已是 APPROVED — 幂等跳过, 不 update")
        void onApproved_alreadyApproved_idempotent() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onApproved(APPLY_ID);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("已是 USED — 幂等跳过")
        void onApproved_alreadyUsed_idempotent() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_USED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onApproved(APPLY_ID);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("非 PENDING 状态 (DRAFT) — 跳过")
        void onApproved_wrongStatus_skipped() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onApproved(APPLY_ID);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("申请不存在 — 跳过, 不抛异常")
        void onApproved_notFound_skipped() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            callback.onApproved(999L);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }
    }

    @Nested
    @DisplayName("onRejected() 状态机")
    class OnRejected {

        @Test
        @DisplayName("PENDING → REJECTED — 状态更新")
        void onRejected_pendingToRejected() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onRejected(APPLY_ID);

            // then
            verify(mapper).updateById(applyCaptor.capture());
            assertThat(applyCaptor.getValue().getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_REJECTED);
        }

        @Test
        @DisplayName("已是 REJECTED — 幂等跳过")
        void onRejected_alreadyRejected_idempotent() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_REJECTED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onRejected(APPLY_ID);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("非 PENDING 状态 — 跳过")
        void onRejected_wrongStatus_skipped() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_ARCHIVED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            callback.onRejected(APPLY_ID);

            // then
            verify(mapper, never()).updateById(any(AdmSealApply.class));
        }
    }

}
