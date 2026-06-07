package cn.oa.meeting.service;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.dto.MtMeetingCreateDTO;
import cn.oa.meeting.dto.MtMeetingQueryDTO;
import cn.oa.meeting.entity.MtMeeting;
import cn.oa.meeting.mapper.MtMeetingMapper;
import cn.oa.meeting.vo.MtMeetingVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysEmpMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MtMeetingService 单元测试.
 *
 * <p>覆盖 create / start / complete / cancel / delete / getById / listPage 7 大场景.
 */
@ExtendWith(MockitoExtension.class)
class MtMeetingServiceTest {

    @Mock private MtMeetingMapper mapper;
    @Mock private SysEmpMapper empMapper;

    @Captor private ArgumentCaptor<MtMeeting> meetingCaptor;

    private MtMeetingService service;

    private static final Long EMP_ID = 1L;
    private static final Long MEETING_ID = 3000L;
    private static final Long BOOKING_ID = 5000L;

    @BeforeEach
    void setUp() {
        service = new MtMeetingService(mapper, empMapper);
    }

    @Nested
    @DisplayName("create() 创建会议")
    class Create {

        @Test
        @DisplayName("创建成功 — 状态 SCHEDULED, organizerId 为 empId")
        void create_success() {
            // given
            org.mockito.Mockito.doAnswer(inv -> {
                MtMeeting m = inv.getArgument(0);
                m.setId(MEETING_ID);
                return 1;
            }).when(mapper).insert(any(MtMeeting.class));

            MtMeetingCreateDTO dto = new MtMeetingCreateDTO();
            dto.setBookingId(BOOKING_ID);
            dto.setMeetingTitle("产品评审会");
            dto.setSummary("评审 V2 需求");
            dto.setStartTime(LocalDateTime.of(2026, 6, 10, 9, 0));
            dto.setEndTime(LocalDateTime.of(2026, 6, 10, 11, 0));
            dto.setLocation("第一会议室");

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(MEETING_ID);
            verify(mapper, times(1)).insert(meetingCaptor.capture());
            MtMeeting saved = meetingCaptor.getValue();
            assertThat(saved.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(saved.getMeetingTitle()).isEqualTo("产品评审会");
            assertThat(saved.getOrganizerId()).isEqualTo(EMP_ID);
            assertThat(saved.getStatus()).isEqualTo(MtConstants.MEETING_STATUS_SCHEDULED);
            assertThat(saved.getLocation()).isEqualTo("第一会议室");
        }
    }

    @Nested
    @DisplayName("start() / complete() / cancel() / delete() 状态机")
    class StateMachine {

        @Test
        @DisplayName("start — SCHEDULED → IN_PROGRESS")
        void start_success() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_SCHEDULED);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            service.start(MEETING_ID);

            // then
            verify(mapper).updateById(meetingCaptor.capture());
            assertThat(meetingCaptor.getValue().getStatus()).isEqualTo(MtConstants.MEETING_STATUS_IN_PROGRESS);
        }

        @Test
        @DisplayName("start — 非 SCHEDULED 状态抛 BizException")
        void start_invalidStatus() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when & then
            assertThatThrownBy(() -> service.start(MEETING_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 SCHEDULED 状态可开始");
        }

        @Test
        @DisplayName("complete — IN_PROGRESS → COMPLETED + 更新 summary")
        void complete_withSummary() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            service.complete(MEETING_ID, "本次会议完成 3 个议题");

            // then
            verify(mapper).updateById(meetingCaptor.capture());
            assertThat(meetingCaptor.getValue().getStatus()).isEqualTo(MtConstants.MEETING_STATUS_COMPLETED);
            assertThat(meetingCaptor.getValue().getSummary()).isEqualTo("本次会议完成 3 个议题");
        }

        @Test
        @DisplayName("complete — 已是 COMPLETED 幂等跳过")
        void complete_alreadyCompleted_idempotent() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_COMPLETED);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            service.complete(MEETING_ID, "x");

            // then
            verify(mapper, never()).updateById(any(MtMeeting.class));
        }

        @Test
        @DisplayName("complete — COMPLETED 状态再调抛 BizException (cancel 分支检查)")
        void cancel_completed_throws() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_COMPLETED);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when & then
            assertThatThrownBy(() -> service.cancel(MEETING_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("已完成的会议不可取消");
        }

        @Test
        @DisplayName("cancel — SCHEDULED → CANCELLED")
        void cancel_success() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_SCHEDULED);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            service.cancel(MEETING_ID);

            // then
            verify(mapper).updateById(meetingCaptor.capture());
            assertThat(meetingCaptor.getValue().getStatus()).isEqualTo(MtConstants.MEETING_STATUS_CANCELLED);
        }

        @Test
        @DisplayName("delete — IN_PROGRESS 抛 BizException")
        void delete_inProgress_throws() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when & then
            assertThatThrownBy(() -> service.delete(MEETING_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("进行中的会议不可删除");
        }

        @Test
        @DisplayName("delete — SCHEDULED 删除成功")
        void delete_success() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_SCHEDULED);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            service.delete(MEETING_ID);

            // then
            verify(mapper).deleteById(MEETING_ID);
        }
    }

    @Nested
    @DisplayName("getById() / listPage() 查询")
    class Query {

        @Test
        @DisplayName("getById — 关联 organizerName")
        void getById_withOrganizer() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setBookingId(BOOKING_ID);
            meeting.setMeetingTitle("产品评审会");
            meeting.setOrganizerId(EMP_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
            when(mapper.selectById(MEETING_ID)).thenReturn(meeting);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("张三");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            // when
            MtMeetingVO vo = service.getById(MEETING_ID);

            // then
            assertThat(vo.getId()).isEqualTo(MEETING_ID);
            assertThat(vo.getMeetingTitle()).isEqualTo("产品评审会");
            assertThat(vo.getOrganizerName()).isEqualTo("张三");
            assertThat(vo.getStatus()).isEqualTo(MtConstants.MEETING_STATUS_IN_PROGRESS);
        }

        @Test
        @DisplayName("getById — 会议不存在抛 NOT_FOUND")
        void getById_notFound() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("会议不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("listPage — 多条件查询")
        void listPage() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setBookingId(BOOKING_ID);
            meeting.setOrganizerId(EMP_ID);
            meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);

            Page<MtMeeting> page = new Page<>(1, 10);
            page.setRecords(List.of(meeting));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("李四");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            MtMeetingQueryDTO query = new MtMeetingQueryDTO();
            query.setBookingId(BOOKING_ID);
            query.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
            query.setOrganizerId(EMP_ID);
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<MtMeetingVO> result = service.listPage(query);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getOrganizerName()).isEqualTo("李四");
        }
    }

}
