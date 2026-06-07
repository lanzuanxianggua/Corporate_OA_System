package cn.oa.meeting.service;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.dto.MtBookingCreateDTO;
import cn.oa.meeting.dto.MtBookingQueryDTO;
import cn.oa.meeting.entity.MtBooking;
import cn.oa.meeting.event.MtBusinessSubmittedEvent;
import cn.oa.meeting.mapper.MtBookingMapper;
import cn.oa.meeting.vo.MtBookingVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会议室预约 Service.
 *
 * <p>业务主路径: 申请人创建 PENDING → 启动工作流 → 审批通过 → 完成.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtBookingService {

    private final MtBookingMapper mapper;
    private final ObjectMapper objectMapper;
    private final WfInstanceService wfInstanceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建预约 (PENDING + 启动工作流).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(MtBookingCreateDTO dto, Long empId) {
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().equals(dto.getStartTime())) {
            throw new BizException(RCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (dto.getStartTime().toNanoOfDay() < LocalTime.of(7, 0).toNanoOfDay()
                || dto.getEndTime().toNanoOfDay() > LocalTime.of(22, 0).toNanoOfDay()) {
            throw new BizException(RCode.BAD_REQUEST, "预约时段需在 07:00-22:00 之间");
        }

        List<MtBooking> conflicts = mapper.findByRoomAndDate(dto.getRoomId(), dto.getBookDate());
        for (MtBooking existing : conflicts) {
            if (isTimeOverlap(dto.getStartTime(), dto.getEndTime(),
                    existing.getStartTime(), existing.getEndTime())) {
                throw new BizException(RCode.BAD_REQUEST, "该时间段会议室已被预约");
            }
        }

        MtBooking booking = new MtBooking();
        booking.setRoomId(dto.getRoomId());
        booking.setEmpId(empId);
        booking.setBookDate(dto.getBookDate());
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setMeetingTitle(dto.getMeetingTitle());
        booking.setMeetingDesc(dto.getMeetingDesc());
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            try {
                booking.setParticipantIds(objectMapper.writeValueAsString(dto.getParticipantIds()));
            } catch (JsonProcessingException e) {
                throw new BizException(RCode.BAD_REQUEST, "参会人数据格式错误");
            }
        }
        booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
        mapper.insert(booking);

        // 启动工作流
        String businessKey = MtConstants.BIZ_KEY_PREFIX_BOOKING + booking.getId();
        Long wfInstanceId = wfInstanceService.start(MtConstants.WF_DEF_BOOKING, businessKey, empId);
        booking.setWfInstanceId(wfInstanceId);
        mapper.updateById(booking);

        // 事件
        eventPublisher.publishEvent(new MtBusinessSubmittedEvent(
                MtConstants.BIZ_KEY_PREFIX_BOOKING, booking.getId(),
                buildBookNo(booking.getId()), empId, wfInstanceId));

        log.info("会议室预约已创建并提交: bookingId={}, roomId={}, date={}, time={}-{}",
                booking.getId(), dto.getRoomId(), dto.getBookDate(), dto.getStartTime(), dto.getEndTime());
        return booking.getId();
    }

    /**
     * 取消预约.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long empId) {
        MtBooking booking = mapper.selectById(id);
        if (booking == null) {
            throw new BizException(RCode.NOT_FOUND, "预约不存在: " + id);
        }
        if (!Objects.equals(booking.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能取消自己的预约");
        }
        String status = booking.getStatus();
        if (!MtConstants.BOOKING_STATUS_PENDING.equals(status)
                && !MtConstants.BOOKING_STATUS_APPROVED.equals(status)) {
            throw new BizException(RCode.BAD_REQUEST, "仅 PENDING/APPROVED 状态可取消, 当前状态: " + status);
        }
        booking.setStatus(MtConstants.BOOKING_STATUS_CANCELLED);
        mapper.updateById(booking);
        log.info("会议室预约已取消: bookingId={}, empId={}", id, empId);
    }

    /**
     * 查询预约详情.
     */
    public MtBookingVO getById(Long id) {
        Map<String, Object> detail = mapper.selectDetailById(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "预约不存在: " + id);
        }
        return toVO(detail);
    }

    /**
     * 分页查询预约列表.
     */
    public PageResult<MtBookingVO> listPage(MtBookingQueryDTO query, Long empId) {
        Page<MtBooking> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MtBooking> wrapper = new LambdaQueryWrapper<MtBooking>()
                .eq(query.getRoomId() != null, MtBooking::getRoomId, query.getRoomId())
                .eq(query.getBookDate() != null, MtBooking::getBookDate, query.getBookDate())
                .eq(query.getStatus() != null, MtBooking::getStatus, query.getStatus())
                .eq(MtBooking::getEmpId, empId)
                .orderByDesc(MtBooking::getCreateTime);

        Page<MtBooking> result = mapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVOEntity).toList(),
                result.getTotal(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private MtBookingVO toVOEntity(MtBooking booking) {
        MtBookingVO vo = new MtBookingVO();
        vo.setId(booking.getId());
        vo.setRoomId(booking.getRoomId());
        vo.setEmpId(booking.getEmpId());
        vo.setBookDate(booking.getBookDate());
        vo.setStartTime(booking.getStartTime());
        vo.setEndTime(booking.getEndTime());
        vo.setMeetingTitle(booking.getMeetingTitle());
        vo.setMeetingDesc(booking.getMeetingDesc());
        vo.setParticipantIds(booking.getParticipantIds());
        vo.setStatus(booking.getStatus());
        vo.setCreateTime(booking.getCreateTime());
        return vo;
    }

    private MtBookingVO toVO(Map<String, Object> map) {
        MtBookingVO vo = new MtBookingVO();
        vo.setId(toLong(map.get("id")));
        vo.setRoomId(toLong(map.get("room_id")));
        vo.setEmpId(toLong(map.get("book_emp_id")));
        vo.setRoomName(toString(map.get("room_name")));
        vo.setBookEmpName(toString(map.get("book_emp_name")));
        vo.setBookDate(toLocalDate(map.get("book_date")));
        vo.setStartTime(toLocalTime(map.get("start_time")));
        vo.setEndTime(toLocalTime(map.get("end_time")));
        vo.setMeetingTitle(toString(map.get("meeting_title")));
        vo.setMeetingDesc(toString(map.get("meeting_desc")));
        vo.setParticipantIds(toString(map.get("participant_ids")));
        vo.setStatus(toString(map.get("status")));
        vo.setCreateTime(toLocalDateTime(map.get("create_time")));
        return vo;
    }

    private Long toLong(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    private String toString(Object v) {
        return v != null ? v.toString() : null;
    }

    private java.time.LocalDate toLocalDate(Object v) {
        return v instanceof java.sql.Date ? ((java.sql.Date) v).toLocalDate()
                : v instanceof java.time.LocalDate ? (java.time.LocalDate) v : null;
    }

    private java.time.LocalTime toLocalTime(Object v) {
        return v instanceof java.sql.Time ? ((java.sql.Time) v).toLocalTime()
                : v instanceof java.time.LocalTime ? (java.time.LocalTime) v : null;
    }

    private java.time.LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.LocalDateTime ldt) return ldt;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try { return java.time.LocalDateTime.parse(v.toString()); } catch (Exception e) { return null; }
    }

    private String buildBookNo(Long id) {
        return "MT" + System.currentTimeMillis() + "_" + id;
    }
}
