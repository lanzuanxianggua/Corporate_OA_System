package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.entity.DocDispatch;
import cn.oa.document.event.DocBusinessSubmittedEvent;
import cn.oa.document.mapper.DocDispatchMapper;
import cn.oa.document.vo.DocDispatchVO;
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
 * DocDispatchService 单测 — 覆盖 create/submit/approve/reject/publish/archive/update/delete/get/listPage.
 */
@ExtendWith(MockitoExtension.class)
class DocDispatchServiceTest {

    @Mock private DocDispatchMapper mapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<DocDispatch> dispatchCaptor;
    @Captor private ArgumentCaptor<DocBusinessSubmittedEvent> eventCaptor;

    private DocDispatchService service;

    private static final Long EMP_ID = 1L;
    private static final Long DEPT_ID = 100L;
    private static final Long DISPATCH_ID = 1000L;
    private static final Long WF_INSTANCE_ID = 9001L;

    @BeforeEach
    void setUp() {
        service = new DocDispatchService(mapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 创建发文")
    class Create {
        @Test
        @DisplayName("创建成功 — 状态 DRAFT, 自动生成 docNo")
        void create_success() {
            DocDispatchCreateDTO dto = new DocDispatchCreateDTO();
            dto.setTitle("2026年工作总结的通知");
            dto.setSubjectWord("年度总结");
            dto.setSendToDept("各部门");
            dto.setContent("正文内容");

            when(mapper.insert(any(DocDispatch.class))).thenAnswer(inv -> {
                DocDispatch d = inv.getArgument(0);
                d.setId(DISPATCH_ID);
                return 1;
            });

            Long id = service.create(dto, EMP_ID, DEPT_ID);

            assertThat(id).isEqualTo(DISPATCH_ID);
            verify(mapper).insert(dispatchCaptor.capture());
            DocDispatch saved = dispatchCaptor.getValue();
            assertThat(saved.getTitle()).isEqualTo("2026年工作总结的通知");
            assertThat(saved.getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_DRAFT);
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getDeptId()).isEqualTo(DEPT_ID);
            assertThat(saved.getDocNo()).startsWith("DOC");
            assertThat(saved.getUrgency()).isEqualTo(DocConstants.URGENCY_NORMAL);
            assertThat(saved.getSecurityLevel()).isEqualTo(DocConstants.SECURITY_PUBLIC);
        }

        @Test
        @DisplayName("创建支持自定义 urgency/security/attachments")
        void create_customFields() {
            DocDispatchCreateDTO dto = new DocDispatchCreateDTO();
            dto.setTitle("紧急通知");
            dto.setUrgency(DocConstants.URGENCY_URGENT);
            dto.setSecurityLevel(DocConstants.SECURITY_SECRET);
            dto.setAttachmentIds("[1,2,3]");

            when(mapper.insert(any(DocDispatch.class))).thenAnswer(inv -> {
                DocDispatch d = inv.getArgument(0);
                d.setId(DISPATCH_ID);
                return 1;
            });

            service.create(dto, EMP_ID, DEPT_ID);

            verify(mapper).insert(dispatchCaptor.capture());
            DocDispatch saved = dispatchCaptor.getValue();
            assertThat(saved.getUrgency()).isEqualTo(DocConstants.URGENCY_URGENT);
            assertThat(saved.getSecurityLevel()).isEqualTo(DocConstants.SECURITY_SECRET);
            assertThat(saved.getAttachmentIds()).isEqualTo("[1,2,3]");
        }
    }

    @Nested
    @DisplayName("submit() 提交发文")
    class Submit {
        @Test
        @DisplayName("DRAFT -> PENDING + 启动工作流 + 发布事件")
        void submit_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);
            when(wfInstanceService.start(eq(DocConstants.WF_DEF_DISPATCH),
                    eq(DocConstants.BIZ_KEY_PREFIX_DISPATCH + DISPATCH_ID), eq(EMP_ID)))
                    .thenReturn(WF_INSTANCE_ID);

            Long wfId = service.submit(DISPATCH_ID, EMP_ID);

            assertThat(wfId).isEqualTo(WF_INSTANCE_ID);
            // 应该 update 两次: 第一次置 PENDING, 第二次回写 wfInstanceId
            verify(mapper, times(2)).updateById(dispatchCaptor.capture());
            List<DocDispatch> updates = dispatchCaptor.getAllValues();
            assertThat(updates.get(0).getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_PENDING);
            assertThat(updates.get(1).getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            DocBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo(DocConstants.BIZ_KEY_PREFIX_DISPATCH);
            assertThat(ev.getBusinessId()).isEqualTo(DISPATCH_ID);
            assertThat(ev.getSubmitterId()).isEqualTo(EMP_ID);
            assertThat(ev.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
        }

        @Test
        @DisplayName("非 DRAFT 状态抛 BizException")
        void submit_invalidStatus() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_PENDING);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            assertThatThrownBy(() -> service.submit(DISPATCH_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅草稿状态可提交");

            verify(wfInstanceService, never()).start(any(), any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("非创建人提交 — 抛 FORBIDDEN")
        void submit_notOwner() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            assertThatThrownBy(() -> service.submit(DISPATCH_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能提交自己的发文");
        }

        @Test
        @DisplayName("发文不存在 — 抛 NOT_FOUND")
        void submit_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.submit(404L, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("approve()/reject()/publish()/archive() 状态机")
    class StateMachine {
        @Test
        @DisplayName("approve: PENDING -> APPROVED")
        void approve_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_PENDING);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.approve(DISPATCH_ID);

            verify(mapper).updateById(dispatchCaptor.capture());
            assertThat(dispatchCaptor.getValue().getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_APPROVED);
        }

        @Test
        @DisplayName("approve: 已是终态时幂等跳过")
        void approve_idempotent() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_APPROVED);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.approve(DISPATCH_ID);

            verify(mapper, never()).updateById(any(DocDispatch.class));
        }

        @Test
        @DisplayName("reject: PENDING -> REJECTED")
        void reject_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_PENDING);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.reject(DISPATCH_ID);

            verify(mapper).updateById(dispatchCaptor.capture());
            assertThat(dispatchCaptor.getValue().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("reject 非 PENDING 状态抛异常")
        void reject_invalidStatus() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            assertThatThrownBy(() -> service.reject(DISPATCH_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅待审批状态可驳回");
        }

        @Test
        @DisplayName("publish: APPROVED -> PUBLISHED")
        void publish_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_APPROVED);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.publish(DISPATCH_ID);

            verify(mapper).updateById(dispatchCaptor.capture());
            assertThat(dispatchCaptor.getValue().getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_PUBLISHED);
        }

        @Test
        @DisplayName("archive: PUBLISHED -> ARCHIVED")
        void archive_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_PUBLISHED);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.archive(DISPATCH_ID);

            verify(mapper).updateById(dispatchCaptor.capture());
            assertThat(dispatchCaptor.getValue().getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_ARCHIVED);
        }
    }

    @Nested
    @DisplayName("update()/delete() 草稿编辑")
    class UpdateDelete {
        @Test
        @DisplayName("update 成功 — 仅 DRAFT 可改")
        void update_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            DocDispatchCreateDTO dto = new DocDispatchCreateDTO();
            dto.setTitle("新标题");
            dto.setUrgency(DocConstants.URGENCY_URGENT);

            service.update(DISPATCH_ID, dto, EMP_ID);

            verify(mapper).updateById(dispatchCaptor.capture());
            assertThat(dispatchCaptor.getValue().getTitle()).isEqualTo("新标题");
            assertThat(dispatchCaptor.getValue().getUrgency()).isEqualTo(DocConstants.URGENCY_URGENT);
        }

        @Test
        @DisplayName("update 非 DRAFT 状态抛异常")
        void update_nonDraft() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_PENDING);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);
            assertThatThrownBy(() -> service.update(DISPATCH_ID, new DocDispatchCreateDTO(), EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅草稿状态可更新");
        }

        @Test
        @DisplayName("delete 成功 — 软删除")
        void delete_success() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);

            service.delete(DISPATCH_ID, EMP_ID);

            verify(mapper).deleteById(DISPATCH_ID);
        }

        @Test
        @DisplayName("delete 非创建人抛 FORBIDDEN")
        void delete_notOwner() {
            DocDispatch dispatch = newDispatch(DocConstants.DISPATCH_STATUS_DRAFT);
            when(mapper.selectById(DISPATCH_ID)).thenReturn(dispatch);
            assertThatThrownBy(() -> service.delete(DISPATCH_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能删除自己的发文");
        }
    }

    @Nested
    @DisplayName("getById()/listPage() 查询")
    class Query {
        @Test
        @DisplayName("getById 返回 VO")
        void getById_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", DISPATCH_ID);
            row.put("doc_no", "DOC202606001");
            row.put("title", "标题");
            row.put("status", DocConstants.DISPATCH_STATUS_APPROVED);
            row.put("emp_id", EMP_ID);
            row.put("emp_name", "张三");
            row.put("dept_id", DEPT_ID);
            when(mapper.findDetail(DISPATCH_ID)).thenReturn(row);

            DocDispatchVO vo = service.getById(DISPATCH_ID);
            assertThat(vo.getId()).isEqualTo(DISPATCH_ID);
            assertThat(vo.getDocNo()).isEqualTo("DOC202606001");
            assertThat(vo.getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_APPROVED);
            assertThat(vo.getEmpName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("getById 不存在抛 NOT_FOUND")
        void getById_notFound() {
            when(mapper.findDetail(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(404L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("发文不存在");
        }

        @Test
        @DisplayName("listPage 返回分页 VO")
        @SuppressWarnings("unchecked")
        void listPage_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", DISPATCH_ID);
            row.put("title", "Test");
            row.put("status", DocConstants.DISPATCH_STATUS_PENDING);
            Page<Map<String, Object>> page = new Page<>(1, 10);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            when(mapper.findPageWithJoins(any(Page.class), any(), any(), any())).thenReturn(page);

            DocDispatchQueryDTO q = new DocDispatchQueryDTO();
            q.setPageNum(1);
            q.setPageSize(10);

            PageResult<DocDispatchVO> result = service.listPage(q, DEPT_ID);
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getStatus()).isEqualTo(DocConstants.DISPATCH_STATUS_PENDING);
        }
    }

    private DocDispatch newDispatch(String status) {
        DocDispatch d = new DocDispatch();
        d.setId(DISPATCH_ID);
        d.setEmpId(EMP_ID);
        d.setDeptId(DEPT_ID);
        d.setTitle("测试");
        d.setStatus(status);
        return d;
    }
}
