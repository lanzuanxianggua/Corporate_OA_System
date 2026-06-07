package cn.oa.meeting.service;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.dto.MtResolutionCreateDTO;
import cn.oa.meeting.dto.MtResolutionQueryDTO;
import cn.oa.meeting.entity.MtMeeting;
import cn.oa.meeting.entity.MtResolution;
import cn.oa.meeting.mapper.MtMeetingMapper;
import cn.oa.meeting.mapper.MtResolutionMapper;
import cn.oa.meeting.vo.MtResolutionVO;
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

import java.time.LocalDate;
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
 * MtResolutionService 单元测试.
 *
 * <p>覆盖 create / start / complete / delete / markOverdue / getById / listPage 7 大场景.
 */
@ExtendWith(MockitoExtension.class)
class MtResolutionServiceTest {

    @Mock private MtResolutionMapper mapper;
    @Mock private MtMeetingMapper meetingMapper;
    @Mock private SysEmpMapper empMapper;

    @Captor private ArgumentCaptor<MtResolution> resolutionCaptor;

    private MtResolutionService service;

    private static final Long MEETING_ID = 3000L;
    private static final Long RESOLUTION_ID = 4000L;
    private static final Long ASSIGNEE_ID = 5L;

    @BeforeEach
    void setUp() {
        service = new MtResolutionService(mapper, meetingMapper, empMapper);
    }

    @Nested
    @DisplayName("create() 创建决议")
    class Create {

        @Test
        @DisplayName("创建成功 — 关联会议存在, 默认优先级 NORMAL")
        void create_success() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setMeetingTitle("产品评审会");
            when(meetingMapper.selectById(MEETING_ID)).thenReturn(meeting);

            org.mockito.Mockito.doAnswer(inv -> {
                MtResolution r = inv.getArgument(0);
                r.setId(RESOLUTION_ID);
                return 1;
            }).when(mapper).insert(any(MtResolution.class));

            MtResolutionCreateDTO dto = new MtResolutionCreateDTO();
            dto.setMeetingId(MEETING_ID);
            dto.setTitle("完成 v2.0 文档");
            dto.setContent("下周一前完成");
            dto.setAssigneeId(ASSIGNEE_ID);
            dto.setDeadline(LocalDate.of(2026, 6, 30));
            dto.setPriority(null);  // 默认 NORMAL

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(RESOLUTION_ID);
            verify(mapper, times(1)).insert(resolutionCaptor.capture());
            MtResolution saved = resolutionCaptor.getValue();
            assertThat(saved.getMeetingId()).isEqualTo(MEETING_ID);
            assertThat(saved.getTitle()).isEqualTo("完成 v2.0 文档");
            assertThat(saved.getAssigneeId()).isEqualTo(ASSIGNEE_ID);
            assertThat(saved.getDeadline()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(saved.getPriority()).isEqualTo(MtConstants.PRIORITY_NORMAL);
            assertThat(saved.getStatus()).isEqualTo(MtConstants.RESOLUTION_STATUS_PENDING);
        }

        @Test
        @DisplayName("关联会议不存在 — 抛 BizException(NOT_FOUND)")
        void create_meetingNotFound() {
            // given
            when(meetingMapper.selectById(999L)).thenReturn(null);
            MtResolutionCreateDTO dto = new MtResolutionCreateDTO();
            dto.setMeetingId(999L);
            dto.setTitle("x");
            dto.setAssigneeId(ASSIGNEE_ID);

            // when & then
            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("关联会议不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("自定义优先级 HIGH — 落地 HIGH")
        void create_withHighPriority() {
            // given
            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            when(meetingMapper.selectById(MEETING_ID)).thenReturn(meeting);

            org.mockito.Mockito.doAnswer(inv -> {
                MtResolution r = inv.getArgument(0);
                r.setId(RESOLUTION_ID);
                return 1;
            }).when(mapper).insert(any(MtResolution.class));

            MtResolutionCreateDTO dto = new MtResolutionCreateDTO();
            dto.setMeetingId(MEETING_ID);
            dto.setTitle("紧急");
            dto.setAssigneeId(ASSIGNEE_ID);
            dto.setPriority(MtConstants.PRIORITY_HIGH);

            // when
            service.create(dto);

            // then
            verify(mapper).insert(resolutionCaptor.capture());
            assertThat(resolutionCaptor.getValue().getPriority()).isEqualTo(MtConstants.PRIORITY_HIGH);
        }
    }

    @Nested
    @DisplayName("start() / complete() / delete() 状态机")
    class StateMachine {

        @Test
        @DisplayName("start — PENDING → IN_PROGRESS")
        void start_success() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_PENDING);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when
            service.start(RESOLUTION_ID);

            // then
            verify(mapper).updateById(resolutionCaptor.capture());
            assertThat(resolutionCaptor.getValue().getStatus()).isEqualTo(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
        }

        @Test
        @DisplayName("start — 非 PENDING 状态抛 BizException")
        void start_invalidStatus() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when & then
            assertThatThrownBy(() -> service.start(RESOLUTION_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 PENDING 状态可开始");
        }

        @Test
        @DisplayName("complete — IN_PROGRESS → COMPLETED + 设置 completeTime")
        void complete_success() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when
            service.complete(RESOLUTION_ID);

            // then
            verify(mapper).updateById(resolutionCaptor.capture());
            assertThat(resolutionCaptor.getValue().getStatus()).isEqualTo(MtConstants.RESOLUTION_STATUS_COMPLETED);
            assertThat(resolutionCaptor.getValue().getCompleteTime()).isNotNull();
        }

        @Test
        @DisplayName("complete — 已是 COMPLETED 幂等跳过")
        void complete_alreadyCompleted_idempotent() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_COMPLETED);
            r.setCompleteTime(LocalDateTime.now().minusDays(1));
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when
            service.complete(RESOLUTION_ID);

            // then
            verify(mapper, never()).updateById(any(MtResolution.class));
        }

        @Test
        @DisplayName("complete — OVERDUE 状态抛 BizException")
        void complete_overdue_throws() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_OVERDUE);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when & then
            assertThatThrownBy(() -> service.complete(RESOLUTION_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 PENDING/IN_PROGRESS 状态可完成");
        }

        @Test
        @DisplayName("delete — IN_PROGRESS 抛 BizException")
        void delete_inProgress_throws() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when & then
            assertThatThrownBy(() -> service.delete(RESOLUTION_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("进行中的决议不可删除");
        }

        @Test
        @DisplayName("delete — PENDING 删除成功")
        void delete_success() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_PENDING);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            // when
            service.delete(RESOLUTION_ID);

            // then
            verify(mapper).deleteById(RESOLUTION_ID);
        }
    }

    @Nested
    @DisplayName("markOverdue() 标记超期")
    class MarkOverdue {

        @Test
        @DisplayName("存在超期决议 — 标记成功, 返回 count")
        void markOverdue_success() {
            // given
            MtResolution overdue = new MtResolution();
            overdue.setId(RESOLUTION_ID);
            overdue.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            overdue.setDeadline(LocalDate.now().minusDays(1));
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(overdue));

            // when
            int count = service.markOverdue();

            // then
            assertThat(count).isEqualTo(1);
            verify(mapper).updateById(resolutionCaptor.capture());
            assertThat(resolutionCaptor.getValue().getStatus()).isEqualTo(MtConstants.RESOLUTION_STATUS_OVERDUE);
        }

        @Test
        @DisplayName("无超期决议 — 返回 0, 不 update")
        void markOverdue_none() {
            // given
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            // when
            int count = service.markOverdue();

            // then
            assertThat(count).isEqualTo(0);
            verify(mapper, never()).updateById(any(MtResolution.class));
        }

        @Test
        @DisplayName("多条超期 — 返回 count 正确")
        void markOverdue_multiple() {
            // given
            MtResolution r1 = new MtResolution();
            r1.setId(1L);
            r1.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            MtResolution r2 = new MtResolution();
            r2.setId(2L);
            r2.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2));

            // when
            int count = service.markOverdue();

            // then
            assertThat(count).isEqualTo(2);
            verify(mapper, times(2)).updateById(any(MtResolution.class));
        }
    }

    @Nested
    @DisplayName("getById() / listPage() 查询")
    class Query {

        @Test
        @DisplayName("getById — 关联 assigneeName + meetingTitle")
        void getById_withAssociations() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setMeetingId(MEETING_ID);
            r.setTitle("完成文档");
            r.setAssigneeId(ASSIGNEE_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
            r.setPriority(MtConstants.PRIORITY_HIGH);
            when(mapper.selectById(RESOLUTION_ID)).thenReturn(r);

            SysEmp emp = new SysEmp();
            emp.setId(ASSIGNEE_ID);
            emp.setRealName("王五");
            when(empMapper.selectById(ASSIGNEE_ID)).thenReturn(emp);

            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setMeetingTitle("产品评审会");
            when(meetingMapper.selectById(MEETING_ID)).thenReturn(meeting);

            // when
            MtResolutionVO vo = service.getById(RESOLUTION_ID);

            // then
            assertThat(vo.getId()).isEqualTo(RESOLUTION_ID);
            assertThat(vo.getTitle()).isEqualTo("完成文档");
            assertThat(vo.getAssigneeName()).isEqualTo("王五");
            assertThat(vo.getMeetingTitle()).isEqualTo("产品评审会");
            assertThat(vo.getPriority()).isEqualTo(MtConstants.PRIORITY_HIGH);
        }

        @Test
        @DisplayName("getById — 决议不存在抛 NOT_FOUND")
        void getById_notFound() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("决议不存在");
        }

        @Test
        @DisplayName("listPage — 多条件查询")
        void listPage() {
            // given
            MtResolution r = new MtResolution();
            r.setId(RESOLUTION_ID);
            r.setMeetingId(MEETING_ID);
            r.setAssigneeId(ASSIGNEE_ID);
            r.setStatus(MtConstants.RESOLUTION_STATUS_PENDING);

            Page<MtResolution> page = new Page<>(1, 10);
            page.setRecords(List.of(r));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            SysEmp emp = new SysEmp();
            emp.setId(ASSIGNEE_ID);
            emp.setRealName("王五");
            when(empMapper.selectById(ASSIGNEE_ID)).thenReturn(emp);

            MtMeeting meeting = new MtMeeting();
            meeting.setId(MEETING_ID);
            meeting.setMeetingTitle("产品评审会");
            when(meetingMapper.selectById(MEETING_ID)).thenReturn(meeting);

            MtResolutionQueryDTO query = new MtResolutionQueryDTO();
            query.setMeetingId(MEETING_ID);
            query.setAssigneeId(ASSIGNEE_ID);
            query.setStatus(MtConstants.RESOLUTION_STATUS_PENDING);
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<MtResolutionVO> result = service.listPage(query);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getAssigneeName()).isEqualTo("王五");
            assertThat(result.getList().get(0).getMeetingTitle()).isEqualTo("产品评审会");
        }
    }

}
