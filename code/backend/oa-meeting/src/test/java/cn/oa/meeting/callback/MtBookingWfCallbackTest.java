package cn.oa.meeting.callback;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.entity.MtBooking;
import cn.oa.meeting.mapper.MtBookingMapper;
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
 * MtBookingWfCallback 单元测试.
 *
 * <p>覆盖 handleEvent 分发 / onApproved 状态机 / onRejected 状态机 / 幂等跳过 / 错误处理.
 */
@ExtendWith(MockitoExtension.class)
class MtBookingWfCallbackTest {

    @Mock private MtBookingMapper mapper;

    @Captor private ArgumentCaptor<MtBooking> bookingCaptor;

    private MtBookingWfCallback callback;

    private static final Long BOOKING_ID = 5000L;
    private static final Long INSTANCE_ID = 7777L;

    @BeforeEach
    void setUp() {
        callback = new MtBookingWfCallback(mapper);
    }

    @Nested
    @DisplayName("handleEvent() 事件分发")
    class HandleEvent {

        @Test
        @DisplayName("BOOKING_ + APPROVED — 调 onApproved")
        void approved_routesToOnApproved() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", MtConstants.BIZ_KEY_PREFIX_BOOKING + BOOKING_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_APPROVED);
        }

        @Test
        @DisplayName("BOOKING_ + REJECTED — 调 onRejected")
        void rejected_routesToOnRejected() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "REJECTED", MtConstants.BIZ_KEY_PREFIX_BOOKING + BOOKING_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, times(1)).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_REJECTED);
        }

        @Test
        @DisplayName("非 BOOKING_ 前缀 — 跳过")
        void nonBookingPrefix_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", "EXP_9999");

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).selectById(any());
        }

        @Test
        @DisplayName("非终态 — 跳过")
        void nonTerminalStatus_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "RUNNING", MtConstants.BIZ_KEY_PREFIX_BOOKING + BOOKING_ID);

            // when
            callback.handleEvent(event);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }

        @Test
        @DisplayName("businessKey 数字解析失败 — 静默跳过")
        void invalidBusinessKey_skipped() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(
                    INSTANCE_ID, "APPROVED", MtConstants.BIZ_KEY_PREFIX_BOOKING + "NOT_NUM");

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
        void pendingToApproved() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onApproved(BOOKING_ID);

            // then
            verify(mapper).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_APPROVED);
        }

        @Test
        @DisplayName("已是 APPROVED — 幂等跳过")
        void alreadyApproved_idempotent() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_APPROVED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onApproved(BOOKING_ID);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }

        @Test
        @DisplayName("已是 COMPLETED — 幂等跳过")
        void alreadyCompleted_idempotent() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_COMPLETED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onApproved(BOOKING_ID);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }

        @Test
        @DisplayName("CANCELLED 状态 — 跳过")
        void cancelledStatus_skipped() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_CANCELLED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onApproved(BOOKING_ID);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }

        @Test
        @DisplayName("预约不存在 — 跳过")
        void bookingNotFound_skipped() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            callback.onApproved(999L);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }
    }

    @Nested
    @DisplayName("onRejected() 状态机")
    class OnRejected {

        @Test
        @DisplayName("PENDING → REJECTED")
        void pendingToRejected() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onRejected(BOOKING_ID);

            // then
            verify(mapper).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_REJECTED);
        }

        @Test
        @DisplayName("已是 REJECTED — 幂等跳过")
        void alreadyRejected_idempotent() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_REJECTED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onRejected(BOOKING_ID);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }

        @Test
        @DisplayName("APPROVED 状态 — 跳过 (非 PENDING 终态后不再修改)")
        void approvedStatus_skipped() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_APPROVED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            callback.onRejected(BOOKING_ID);

            // then
            verify(mapper, never()).updateById(any(MtBooking.class));
        }
    }

}
