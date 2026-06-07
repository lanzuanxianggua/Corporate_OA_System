package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveQueryDTO;
import cn.oa.document.entity.DocReceive;
import cn.oa.document.mapper.DocReceiveMapper;
import cn.oa.document.vo.DocReceiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DocReceiveService 单测 — 收文为行政登记动作（无工作流）, 覆盖 create/update/process/complete/archive/get/listPage.
 */
@ExtendWith(MockitoExtension.class)
class DocReceiveServiceTest {

    @Mock private DocReceiveMapper mapper;

    @Captor private ArgumentCaptor<DocReceive> receiveCaptor;

    private DocReceiveService service;

    private static final Long DEPT_ID = 100L;
    private static final Long RECEIVE_ID = 5000L;

    @BeforeEach
    void setUp() {
        service = new DocReceiveService(mapper);
    }

    @Nested
    @DisplayName("create() 登记收文")
    class Create {
        @Test
        @DisplayName("登记成功 — 自动生成 receiveNo + 默认 NORMAL")
        void create_success() {
            DocReceiveCreateDTO dto = new DocReceiveCreateDTO();
            dto.setSourceDept("市人力资源和社会保障局");
            dto.setDocTitle("关于调整社保缴费基数的通知");
            dto.setDocDate(LocalDate.of(2026, 6, 1));
            dto.setReceiveDate(LocalDate.of(2026, 6, 5));
            dto.setContent("文摘要");

            when(mapper.insert(any(DocReceive.class))).thenAnswer(inv -> {
                DocReceive r = inv.getArgument(0);
                r.setId(RECEIVE_ID);
                return 1;
            });

            Long id = service.create(dto, DEPT_ID);

            assertThat(id).isEqualTo(RECEIVE_ID);
            verify(mapper).insert(receiveCaptor.capture());
            DocReceive saved = receiveCaptor.getValue();
            assertThat(saved.getReceiveNo()).startsWith("RCV");
            assertThat(saved.getSourceDept()).isEqualTo("市人力资源和社会保障局");
            assertThat(saved.getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_PENDING);
            assertThat(saved.getProcessDeptId()).isEqualTo(DEPT_ID);
            assertThat(saved.getUrgentLevel()).isEqualTo(DocConstants.URGENCY_NORMAL);
        }

        @Test
        @DisplayName("create 支持自定义 urgentLevel")
        void create_urgent() {
            DocReceiveCreateDTO dto = new DocReceiveCreateDTO();
            dto.setSourceDept("S");
            dto.setDocTitle("T");
            dto.setDocDate(LocalDate.now());
            dto.setReceiveDate(LocalDate.now());
            dto.setUrgentLevel(DocConstants.URGENCY_URGENT);

            when(mapper.insert(any(DocReceive.class))).thenAnswer(inv -> {
                DocReceive r = inv.getArgument(0);
                r.setId(RECEIVE_ID);
                return 1;
            });

            service.create(dto, DEPT_ID);
            verify(mapper).insert(receiveCaptor.capture());
            assertThat(receiveCaptor.getValue().getUrgentLevel()).isEqualTo(DocConstants.URGENCY_URGENT);
        }
    }

    @Nested
    @DisplayName("update() 编辑收文")
    class Update {
        @Test
        @DisplayName("update 成功")
        void update_success() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_PENDING);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);

            DocReceiveCreateDTO dto = new DocReceiveCreateDTO();
            dto.setSourceDept("新来源");
            dto.setDocTitle("新标题");
            dto.setDocDate(LocalDate.of(2026, 6, 2));
            dto.setReceiveDate(LocalDate.of(2026, 6, 6));

            service.update(RECEIVE_ID, dto);

            verify(mapper).updateById(receiveCaptor.capture());
            assertThat(receiveCaptor.getValue().getSourceDept()).isEqualTo("新来源");
            assertThat(receiveCaptor.getValue().getDocTitle()).isEqualTo("新标题");
        }

        @Test
        @DisplayName("update 不存在抛 NOT_FOUND")
        void update_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.update(404L, new DocReceiveCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("收文不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("process()/complete()/archive() 状态流转")
    class StateMachine {
        @Test
        @DisplayName("process: PENDING -> PROCESSING + 写拟办意见")
        void process_success() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_PENDING);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);

            service.process(RECEIVE_ID, "请人事部办理");

            verify(mapper).updateById(receiveCaptor.capture());
            DocReceive saved = receiveCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_PROCESSING);
            assertThat(saved.getProcessOpinion()).isEqualTo("请人事部办理");
        }

        @Test
        @DisplayName("process 非 PENDING 状态抛异常")
        void process_invalidStatus() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_COMPLETED);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            assertThatThrownBy(() -> service.process(RECEIVE_ID, "X"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅待处理状态可拟办");
        }

        @Test
        @DisplayName("complete: PROCESSING -> COMPLETED")
        void complete_fromProcessing() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_PROCESSING);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            service.complete(RECEIVE_ID);
            verify(mapper).updateById(receiveCaptor.capture());
            assertThat(receiveCaptor.getValue().getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_COMPLETED);
        }

        @Test
        @DisplayName("complete: PENDING -> COMPLETED 也允许")
        void complete_fromPending() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_PENDING);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            service.complete(RECEIVE_ID);
            verify(mapper).updateById(receiveCaptor.capture());
            assertThat(receiveCaptor.getValue().getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_COMPLETED);
        }

        @Test
        @DisplayName("complete 已 ARCHIVED 状态抛异常")
        void complete_alreadyArchived() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_ARCHIVED);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            assertThatThrownBy(() -> service.complete(RECEIVE_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅待处理/拟办中状态可办结");
        }

        @Test
        @DisplayName("archive: COMPLETED -> ARCHIVED")
        void archive_success() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_COMPLETED);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            service.archive(RECEIVE_ID);
            verify(mapper).updateById(receiveCaptor.capture());
            assertThat(receiveCaptor.getValue().getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_ARCHIVED);
        }

        @Test
        @DisplayName("archive 非 COMPLETED 状态抛异常")
        void archive_invalidStatus() {
            DocReceive receive = newReceive(DocConstants.RECEIVE_STATUS_PENDING);
            when(mapper.selectById(RECEIVE_ID)).thenReturn(receive);
            assertThatThrownBy(() -> service.archive(RECEIVE_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅已办结状态可归档");
            verify(mapper, never()).updateById(any(DocReceive.class));
        }
    }

    @Nested
    @DisplayName("getById()/listPage() 查询")
    class Query {
        @Test
        @DisplayName("getById 返回 VO")
        void getById_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", RECEIVE_ID);
            row.put("receive_no", "RCV202606001");
            row.put("source_dept", "上级单位");
            row.put("status", DocConstants.RECEIVE_STATUS_PROCESSING);
            row.put("process_dept_id", DEPT_ID);
            when(mapper.findDetail(RECEIVE_ID)).thenReturn(row);

            DocReceiveVO vo = service.getById(RECEIVE_ID);
            assertThat(vo.getId()).isEqualTo(RECEIVE_ID);
            assertThat(vo.getReceiveNo()).isEqualTo("RCV202606001");
            assertThat(vo.getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_PROCESSING);
            assertThat(vo.getProcessDeptId()).isEqualTo(DEPT_ID);
        }

        @Test
        @DisplayName("getById 不存在抛 NOT_FOUND")
        void getById_notFound() {
            when(mapper.findDetail(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(404L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("收文不存在");
        }

        @Test
        @DisplayName("listPage 返回分页 VO")
        @SuppressWarnings("unchecked")
        void listPage_success() {
            Map<String, Object> row = new HashMap<>();
            row.put("id", RECEIVE_ID);
            row.put("status", DocConstants.RECEIVE_STATUS_PENDING);
            row.put("doc_title", "标题");
            Page<Map<String, Object>> page = new Page<>(1, 10);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            when(mapper.findPageWithJoins(any(Page.class), any(), any(), any())).thenReturn(page);

            DocReceiveQueryDTO q = new DocReceiveQueryDTO();
            q.setPageNum(1);
            q.setPageSize(10);
            PageResult<DocReceiveVO> result = service.listPage(q, DEPT_ID);
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getStatus()).isEqualTo(DocConstants.RECEIVE_STATUS_PENDING);
        }
    }

    private DocReceive newReceive(String status) {
        DocReceive r = new DocReceive();
        r.setId(RECEIVE_ID);
        r.setStatus(status);
        r.setSourceDept("S");
        r.setDocTitle("T");
        r.setProcessDeptId(DEPT_ID);
        return r;
    }
}
