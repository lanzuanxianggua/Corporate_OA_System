package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocSignReportCreateDTO;
import cn.oa.document.dto.DocSignReportQueryDTO;
import cn.oa.document.entity.DocSignReport;
import cn.oa.document.event.DocBusinessSubmittedEvent;
import cn.oa.document.mapper.DocSignReportMapper;
import cn.oa.document.vo.DocSignReportVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
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

import java.util.HashMap;
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
 * DocSignReportService 单测 — 覆盖签报 create/submit/approve/reject/update/delete/get/listPage.
 */
@ExtendWith(MockitoExtension.class)
class DocSignReportServiceTest {

    @Mock private DocSignReportMapper mapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<DocSignReport> reportCaptor;
    @Captor private ArgumentCaptor<DocBusinessSubmittedEvent> eventCaptor;

    private DocSignReportService service;

    private static final Long EMP_ID = 2L;
    private static final Long DEPT_ID = 200L;
    private static final Long REPORT_ID = 2000L;
    private static final Long WF_INSTANCE_ID = 9101L;

    @BeforeEach
    void setUp() {
        service = new DocSignReportService(mapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 创建签报")
    class Create {
        @Test
        @DisplayName("创建成功 — DRAFT + 自动 reportNo + 默认 GENERAL 类型")
        void create_success() {
            DocSignReportCreateDTO dto = new DocSignReportCreateDTO();
            dto.setTitle("购置办公设备的请示");
            dto.setContent("内容");

            when(mapper.insert(any(DocSignReport.class))).thenAnswer(inv -> {
                DocSignReport r = inv.getArgument(0);
                r.setId(REPORT_ID);
                return 1;
            });

            Long id = service.create(dto, EMP_ID, DEPT_ID);

            assertThat(id).isEqualTo(REPORT_ID);
            verify(mapper).insert(reportCaptor.capture());
            DocSignReport saved = reportCaptor.getValue();
            assertThat(saved.getTitle()).isEqualTo("购置办公设备的请示");
            assertThat(saved.getStatus()).isEqualTo(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getDeptId()).isEqualTo(DEPT_ID);
            assertThat(saved.getReportNo()).startsWith("SIGN");
            assertThat(saved.getReportType()).isEqualTo(DocConstants.REPORT_TYPE_GENERAL);
        }

        @Test
        @DisplayName("create 自定义 reportType + attachmentIds")
        void create_customFields() {
            DocSignReportCreateDTO dto = new DocSignReportCreateDTO();
            dto.setTitle("紧急签报");
            dto.setReportType(DocConstants.REPORT_TYPE_URGENT);
            dto.setAttachmentIds("[7,8]");

            when(mapper.insert(any(DocSignReport.class))).thenAnswer(inv -> {
                DocSignReport r = inv.getArgument(0);
                r.setId(REPORT_ID);
                return 1;
            });

            service.create(dto, EMP_ID, DEPT_ID);

            verify(mapper).insert(reportCaptor.capture());
            assertThat(reportCaptor.getValue().getReportType()).isEqualTo(DocConstants.REPORT_TYPE_URGENT);
            assertThat(reportCaptor.getValue().getAttachmentIds()).isEqualTo("[7,8]");
        }
    }

    @Nested
    @DisplayName("submit() 提交签报")
    class Submit {
        @Test
        @DisplayName("DRAFT -> PENDING + 启动工作流 + 发布事件")
        void submit_success() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            when(wfInstanceService.start(eq(DocConstants.WF_DEF_SIGN_REPORT),
                    eq(DocConstants.BIZ_KEY_PREFIX_SIGN_REPORT + REPORT_ID), eq(EMP_ID)))
                    .thenReturn(WF_INSTANCE_ID);

            Long wfId = service.submit(REPORT_ID, EMP_ID);

            assertThat(wfId).isEqualTo(WF_INSTANCE_ID);
            verify(mapper, times(2)).updateById(reportCaptor.capture());
            List<DocSignReport> updates = reportCaptor.getAllValues();
            assertThat(updates.get(0).getStatus()).isEqualTo(DocConstants.SIGN_REPORT_STATUS_PENDING);
            assertThat(updates.get(1).getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            DocBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo(DocConstants.BIZ_KEY_PREFIX_SIGN_REPORT);
            assertThat(ev.getBusinessId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("非 DRAFT 状态抛 BizException")
        void submit_invalidStatus() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_APPROVED);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            assertThatThrownBy(() -> service.submit(REPORT_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅草稿状态可提交");
            verify(wfInstanceService, never()).start(any(), any(), any());
        }

        @Test
        @DisplayName("非创建人提交 — 抛 FORBIDDEN")
        void submit_notOwner() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            assertThatThrownBy(() -> service.submit(REPORT_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能提交自己的签报");
        }

        @Test
        @DisplayName("签报不存在 — 抛 NOT_FOUND")
        void submit_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.submit(404L, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("approve()/reject() 状态机")
    class StateMachine {
        @Test
        @DisplayName("approve: PENDING -> APPROVED")
        void approve_success() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_PENDING);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            service.approve(REPORT_ID);
            verify(mapper).updateById(reportCaptor.capture());
            assertThat(reportCaptor.getValue().getStatus()).isEqualTo(DocConstants.SIGN_REPORT_STATUS_APPROVED);
        }

        @Test
        @DisplayName("approve 已是 APPROVED 终态时幂等跳过")
        void approve_idempotent() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_APPROVED);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            service.approve(REPORT_ID);
            verify(mapper, never()).updateById(any(DocSignReport.class));
        }

        @Test
        @DisplayName("approve 非 PENDING 状态抛异常 (DRAFT)")
        void approve_invalidStatus() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            assertThatThrownBy(() -> service.approve(REPORT_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅待审批状态可审批通过");
        }

        @Test
        @DisplayName("reject: PENDING -> REJECTED")
        void reject_success() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_PENDING);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            service.reject(REPORT_ID);
            verify(mapper).updateById(reportCaptor.capture());
            assertThat(reportCaptor.getValue().getStatus()).isEqualTo(DocConstants.SIGN_REPORT_STATUS_REJECTED);
        }
    }

    @Nested
    @DisplayName("update()/delete() 草稿编辑")
    class UpdateDelete {
        @Test
        @DisplayName("update 成功 — 仅 DRAFT 可改")
        void update_success() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);

            DocSignReportCreateDTO dto = new DocSignReportCreateDTO();
            dto.setTitle("新标题");
            dto.setReportType(DocConstants.REPORT_TYPE_SPECIAL);

            service.update(REPORT_ID, dto, EMP_ID);

            verify(mapper).updateById(reportCaptor.capture());
            assertThat(reportCaptor.getValue().getTitle()).isEqualTo("新标题");
            assertThat(reportCaptor.getValue().getReportType()).isEqualTo(DocConstants.REPORT_TYPE_SPECIAL);
        }

        @Test
        @DisplayName("update 非 DRAFT 状态抛异常")
        void update_nonDraft() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_APPROVED);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            assertThatThrownBy(() -> service.update(REPORT_ID, new DocSignReportCreateDTO(), EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅草稿状态可更新");
        }

        @Test
        @DisplayName("delete 非创建人抛 FORBIDDEN")
        void delete_notOwner() {
            DocSignReport report = newReport(DocConstants.SIGN_REPORT_STATUS_DRAFT);
            when(mapper.selectById(REPORT_ID)).thenReturn(report);
            assertThatThrownBy(() -> service.delete(REPORT_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能删除自己的签报");
        }
    }

    @Nested
    @DisplayName("getById()/listPage() 查询")
    class Query {
        @Test
        @DisplayName("getById 返回 VO")
        void getById_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", REPORT_ID);
            row.put("report_no", "SIGN20260601");
            row.put("title", "请示");
            row.put("status", DocConstants.SIGN_REPORT_STATUS_PENDING);
            when(mapper.findDetail(REPORT_ID)).thenReturn(row);

            DocSignReportVO vo = service.getById(REPORT_ID);
            assertThat(vo.getId()).isEqualTo(REPORT_ID);
            assertThat(vo.getReportNo()).isEqualTo("SIGN20260601");
            assertThat(vo.getStatus()).isEqualTo(DocConstants.SIGN_REPORT_STATUS_PENDING);
        }

        @Test
        @DisplayName("getById 不存在抛 NOT_FOUND")
        void getById_notFound() {
            when(mapper.findDetail(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(404L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("签报不存在");
        }

        @Test
        @DisplayName("listPage 返回分页 VO")
        @SuppressWarnings("unchecked")
        void listPage_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", REPORT_ID);
            row.put("title", "签报 X");
            row.put("status", DocConstants.SIGN_REPORT_STATUS_PENDING);
            Page<Map<String, Object>> page = new Page<>(1, 10);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            when(mapper.findPageWithJoins(any(Page.class), any(), any(), any())).thenReturn(page);

            DocSignReportQueryDTO q = new DocSignReportQueryDTO();
            q.setPageNum(1);
            q.setPageSize(10);
            PageResult<DocSignReportVO> result = service.listPage(q, EMP_ID);
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
        }
    }

    private DocSignReport newReport(String status) {
        DocSignReport r = new DocSignReport();
        r.setId(REPORT_ID);
        r.setEmpId(EMP_ID);
        r.setDeptId(DEPT_ID);
        r.setTitle("签报");
        r.setStatus(status);
        return r;
    }
}
