package cn.oa.meeting.callback;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.entity.MtBooking;
import cn.oa.meeting.mapper.MtBookingMapper;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会议室预约流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey 前缀 {@code BOOKING_}.
 * 终态 APPROVED → 状态置 APPROVED; REJECTED → 状态置 REJECTED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MtBookingWfCallback {

    static final String BIZ_PREFIX = MtConstants.BIZ_KEY_PREFIX_BOOKING;

    private final MtBookingMapper mapper;

    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[MtBookingCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[MtBookingCallback] 非会议室预约流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long bookingId = parseId(businessKey);
            if (bookingId == null) {
                log.warn("[MtBookingCallback] 解析 bookingId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (MtConstants.BOOKING_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(bookingId);
            } else if (MtConstants.BOOKING_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(bookingId);
            } else {
                log.info("[MtBookingCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            log.error("[MtBookingCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[MtBookingCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long bookingId) {
        MtBooking booking = mapper.selectById(bookingId);
        if (booking == null) {
            log.warn("[MtBookingCallback] 预约不存在: id={}", bookingId);
            return;
        }
        if (MtConstants.BOOKING_STATUS_APPROVED.equals(booking.getStatus())
                || MtConstants.BOOKING_STATUS_COMPLETED.equals(booking.getStatus())) {
            log.info("[MtBookingCallback] 预约已是终态, 幂等跳过: id={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }
        if (!MtConstants.BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            log.warn("[MtBookingCallback] 预约非 PENDING 状态, 跳过: id={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }
        booking.setStatus(MtConstants.BOOKING_STATUS_APPROVED);
        mapper.updateById(booking);
        log.info("[MtBookingCallback] 会议室预约已审批通过: id={}", bookingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long bookingId) {
        MtBooking booking = mapper.selectById(bookingId);
        if (booking == null) {
            log.warn("[MtBookingCallback] 预约不存在: id={}", bookingId);
            return;
        }
        if (MtConstants.BOOKING_STATUS_REJECTED.equals(booking.getStatus())) {
            log.info("[MtBookingCallback] 预约已是 REJECTED 终态, 幂等跳过: id={}", bookingId);
            return;
        }
        if (!MtConstants.BOOKING_STATUS_PENDING.equals(booking.getStatus())) {
            log.warn("[MtBookingCallback] 预约非 PENDING 状态, 跳过: id={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }
        booking.setStatus(MtConstants.BOOKING_STATUS_REJECTED);
        mapper.updateById(booking);
        log.info("[MtBookingCallback] 会议室预约已驳回: id={}", bookingId);
    }

    private static Long parseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
