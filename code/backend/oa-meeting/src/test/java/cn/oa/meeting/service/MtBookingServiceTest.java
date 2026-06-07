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
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MtBookingService 单元测试.
 *
 * <p>覆盖 create (含时间冲突 + 时段校验 + workflow 集成) / cancel / getById / listPage 5 大场景.
 */
@ExtendWith(MockitoExtension.class)
class MtBookingServiceTest {

    @Mock private MtBookingMapper mapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<MtBooking> bookingCaptor;
    @Captor private ArgumentCaptor<MtBusinessSubmittedEvent> eventCaptor;

    private MtBookingService service;
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Long EMP_ID = 1L;
    private static final Long OTHER_EMP_ID = 2L;
    private static final Long ROOM_ID = 100L;
    private static final Long BOOKING_ID = 5000L;
    private static final Long WF_INSTANCE_ID = 7777L;

    @BeforeEach
    void setUp() {
        service = new MtBookingService(mapper, objectMapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 创建预约")
    class Create {

        @Test
        @DisplayName("正常时段 + 无冲突 — 创建成功, 启动 workflow, 序列化参会人, 发布事件")
        void create_success() {
            // given
            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(9, 0));
            dto.setEndTime(LocalTime.of(10, 0));
            dto.setMeetingTitle("项目周会");
            dto.setMeetingDesc("本周进度");
            dto.setParticipantIds(List.of(10L, 20L));

            when(mapper.findByRoomAndDate(eq(ROOM_ID), eq(LocalDate.of(2026, 6, 10))))
                    .thenReturn(Collections.emptyList());

            org.mockito.Mockito.doAnswer(inv -> {
                MtBooking b = inv.getArgument(0);
                b.setId(BOOKING_ID);
                return 1;
            }).when(mapper).insert(any(MtBooking.class));
            when(wfInstanceService.start(eq(MtConstants.WF_DEF_BOOKING),
                    eq(MtConstants.BIZ_KEY_PREFIX_BOOKING + BOOKING_ID), eq(EMP_ID)))
                    .thenReturn(WF_INSTANCE_ID);

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(BOOKING_ID);
            verify(mapper, times(1)).insert(bookingCaptor.capture());
            MtBooking inserted = bookingCaptor.getValue();
            assertThat(inserted.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(inserted.getEmpId()).isEqualTo(EMP_ID);
            assertThat(inserted.getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_PENDING);
            assertThat(inserted.getMeetingTitle()).isEqualTo("项目周会");
            // participantIds JSON 序列化
            assertThat(inserted.getParticipantIds()).contains("10").contains("20");

            // 启动 workflow
            verify(wfInstanceService).start(MtConstants.WF_DEF_BOOKING,
                    MtConstants.BIZ_KEY_PREFIX_BOOKING + BOOKING_ID, EMP_ID);

            // 更新 wfInstanceId
            verify(mapper, times(1)).updateById(bookingCaptor.capture());
            MtBooking updated = bookingCaptor.getAllValues().get(1);
            assertThat(updated.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);

            // 发布事件
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            MtBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo(MtConstants.BIZ_KEY_PREFIX_BOOKING);
            assertThat(ev.getBusinessId()).isEqualTo(BOOKING_ID);
            assertThat(ev.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
        }

        @Test
        @DisplayName("结束时间 <= 开始时间 — 抛 BizException")
        void create_endTimeBeforeStart() {
            // given
            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(10, 0));
            dto.setEndTime(LocalTime.of(9, 0));
            dto.setMeetingTitle("x");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("结束时间必须晚于开始时间");

            verify(mapper, never()).insert(any(MtBooking.class));
        }

        @Test
        @DisplayName("开始时间 < 07:00 — 抛 BizException (时段越界)")
        void create_beforeOpeningHours() {
            // given
            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(6, 30));
            dto.setEndTime(LocalTime.of(8, 0));
            dto.setMeetingTitle("x");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("07:00-22:00");
        }

        @Test
        @DisplayName("结束时间 > 22:00 — 抛 BizException (时段越界)")
        void create_afterClosingHours() {
            // given
            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(20, 0));
            dto.setEndTime(LocalTime.of(23, 0));
            dto.setMeetingTitle("x");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("07:00-22:00");
        }

        @Test
        @DisplayName("时间段与已存在 PENDING 冲突 — 抛 BizException")
        void create_timeConflict() {
            // given
            MtBooking existing = new MtBooking();
            existing.setId(1L);
            existing.setStartTime(LocalTime.of(9, 30));
            existing.setEndTime(LocalTime.of(10, 30));
            when(mapper.findByRoomAndDate(eq(ROOM_ID), eq(LocalDate.of(2026, 6, 10))))
                    .thenReturn(List.of(existing));

            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(10, 0));
            dto.setEndTime(LocalTime.of(11, 0));
            dto.setMeetingTitle("冲突");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("该时间段会议室已被预约");

            verify(mapper, never()).insert(any(MtBooking.class));
        }

        @Test
        @DisplayName("参会人列表为空 — participantIds 不写入")
        void create_noParticipants() {
            // given
            MtBookingCreateDTO dto = new MtBookingCreateDTO();
            dto.setRoomId(ROOM_ID);
            dto.setBookDate(LocalDate.of(2026, 6, 10));
            dto.setStartTime(LocalTime.of(14, 0));
            dto.setEndTime(LocalTime.of(15, 0));
            dto.setMeetingTitle("无参会人");
            dto.setParticipantIds(null);

            when(mapper.findByRoomAndDate(any(), any())).thenReturn(Collections.emptyList());
            org.mockito.Mockito.doAnswer(inv -> {
                MtBooking b = inv.getArgument(0);
                b.setId(BOOKING_ID);
                return 1;
            }).when(mapper).insert(any(MtBooking.class));
            when(wfInstanceService.start(any(), any(), any())).thenReturn(WF_INSTANCE_ID);

            // when
            service.create(dto, EMP_ID);

            // then
            verify(mapper).insert(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getParticipantIds()).isNull();
        }
    }

    @Nested
    @DisplayName("cancel() 取消预约")
    class Cancel {

        @Test
        @DisplayName("PENDING + 自己 — 取消成功")
        void cancel_pending_success() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setEmpId(EMP_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            service.cancel(BOOKING_ID, EMP_ID);

            // then
            verify(mapper).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_CANCELLED);
        }

        @Test
        @DisplayName("APPROVED + 自己 — 取消成功 (允许 APPROVED 取消)")
        void cancel_approved_success() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setEmpId(EMP_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_APPROVED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when
            service.cancel(BOOKING_ID, EMP_ID);

            // then
            verify(mapper).updateById(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_CANCELLED);
        }

        @Test
        @DisplayName("非自己 — 抛 BizException(FORBIDDEN)")
        void cancel_notOwner() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setEmpId(OTHER_EMP_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when & then
            assertThatThrownBy(() -> service.cancel(BOOKING_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能取消自己的预约")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("COMPLETED 状态 — 抛 BizException(BAD_REQUEST)")
        void cancel_completed_throws() {
            // given
            MtBooking booking = new MtBooking();
            booking.setId(BOOKING_ID);
            booking.setEmpId(EMP_ID);
            booking.setStatus(MtConstants.BOOKING_STATUS_COMPLETED);
            when(mapper.selectById(BOOKING_ID)).thenReturn(booking);

            // when & then
            assertThatThrownBy(() -> service.cancel(BOOKING_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 PENDING/APPROVED 状态可取消");
        }

        @Test
        @DisplayName("预约不存在 — 抛 BizException(NOT_FOUND)")
        void cancel_notFound() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.cancel(999L, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预约不存在");
        }
    }

    @Nested
    @DisplayName("getById() / listPage() 查询")
    class Query {

        @Test
        @DisplayName("getById — 返回 join detail VO")
        void getById_withDetail() {
            // given
            Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("id", BOOKING_ID);
            detail.put("room_id", ROOM_ID);
            detail.put("book_emp_id", EMP_ID);
            detail.put("room_name", "第一会议室");
            detail.put("book_emp_name", "张三");
            detail.put("book_date", java.sql.Date.valueOf(LocalDate.of(2026, 6, 10)));
            detail.put("start_time", java.sql.Time.valueOf(LocalTime.of(9, 0)));
            detail.put("end_time", java.sql.Time.valueOf(LocalTime.of(10, 0)));
            detail.put("meeting_title", "项目周会");
            detail.put("meeting_desc", "进度同步");
            detail.put("participant_ids", "[10,20]");
            detail.put("status", MtConstants.BOOKING_STATUS_APPROVED);
            detail.put("create_time", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            when(mapper.selectDetailById(BOOKING_ID)).thenReturn(detail);

            // when
            MtBookingVO vo = service.getById(BOOKING_ID);

            // then
            assertThat(vo.getId()).isEqualTo(BOOKING_ID);
            assertThat(vo.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(vo.getEmpId()).isEqualTo(EMP_ID);
            assertThat(vo.getRoomName()).isEqualTo("第一会议室");
            assertThat(vo.getBookEmpName()).isEqualTo("张三");
            assertThat(vo.getMeetingTitle()).isEqualTo("项目周会");
            assertThat(vo.getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_APPROVED);
        }

        @Test
        @DisplayName("getById — 不存在抛 NOT_FOUND")
        void getById_notFound() {
            // given
            when(mapper.selectDetailById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("预约不存在");
        }

        @Test
        @DisplayName("listPage — empId 强过滤 + 多条件")
        void listPage() {
            // given
            MtBooking b = new MtBooking();
            b.setId(BOOKING_ID);
            b.setRoomId(ROOM_ID);
            b.setEmpId(EMP_ID);
            b.setStatus(MtConstants.BOOKING_STATUS_PENDING);

            Page<MtBooking> page = new Page<>(1, 10);
            page.setRecords(List.of(b));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            MtBookingQueryDTO query = new MtBookingQueryDTO();
            query.setRoomId(ROOM_ID);
            query.setBookDate(LocalDate.of(2026, 6, 10));
            query.setStatus(MtConstants.BOOKING_STATUS_PENDING);
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<MtBookingVO> result = service.listPage(query, EMP_ID);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getStatus()).isEqualTo(MtConstants.BOOKING_STATUS_PENDING);
            assertThat(result.getList().get(0).getEmpId()).isEqualTo(EMP_ID);
        }
    }

}
