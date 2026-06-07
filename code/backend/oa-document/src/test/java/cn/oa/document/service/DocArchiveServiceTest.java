package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.entity.DocArchive;
import cn.oa.document.mapper.DocArchiveMapper;
import cn.oa.document.vo.DocArchiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.exception.BizException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DocArchiveService 单测 — 档案模块只读 + 供回调 create.
 */
@ExtendWith(MockitoExtension.class)
class DocArchiveServiceTest {

    @Mock private DocArchiveMapper mapper;

    @Captor private ArgumentCaptor<DocArchive> archiveCaptor;

    private DocArchiveService service;

    private static final Long ARCHIVE_ID = 8000L;

    @BeforeEach
    void setUp() {
        service = new DocArchiveService(mapper);
    }

    @Nested
    @DisplayName("getById() 查询档案")
    class GetById {
        @Test
        @DisplayName("查询成功 — 返回 VO 全字段")
        void getById_success() {
            DocArchive a = new DocArchive();
            a.setId(ARCHIVE_ID);
            a.setArchiveNo("ARC202606001");
            a.setArchiveType(DocConstants.ARCHIVE_TYPE_DISPATCH);
            a.setSourceId(1000L);
            a.setArchiveDate(LocalDate.of(2026, 6, 1));
            a.setTitle("发文归档");
            a.setStatus(DocConstants.ARCHIVE_STATUS_ACTIVE);
            when(mapper.selectById(ARCHIVE_ID)).thenReturn(a);

            DocArchiveVO vo = service.getById(ARCHIVE_ID);

            assertThat(vo.getId()).isEqualTo(ARCHIVE_ID);
            assertThat(vo.getArchiveNo()).isEqualTo("ARC202606001");
            assertThat(vo.getArchiveType()).isEqualTo(DocConstants.ARCHIVE_TYPE_DISPATCH);
            assertThat(vo.getSourceId()).isEqualTo(1000L);
            assertThat(vo.getArchiveDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(vo.getTitle()).isEqualTo("发文归档");
            assertThat(vo.getStatus()).isEqualTo(DocConstants.ARCHIVE_STATUS_ACTIVE);
        }

        @Test
        @DisplayName("查询不存在抛 BizException")
        void getById_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(404L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("档案不存在");
        }
    }

    @Nested
    @DisplayName("listPage() 分页查询")
    class ListPage {
        @Test
        @DisplayName("分页返回 VO 列表")
        @SuppressWarnings("unchecked")
        void listPage_success() {
            DocArchive a = new DocArchive();
            a.setId(ARCHIVE_ID);
            a.setArchiveNo("ARC1");
            a.setArchiveType(DocConstants.ARCHIVE_TYPE_RECEIVE);
            a.setStatus(DocConstants.ARCHIVE_STATUS_ACTIVE);

            Page<DocArchive> page = new Page<>(1, 10);
            page.setRecords(List.of(a));
            page.setTotal(1L);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            PageResult<DocArchiveVO> result = service.listPage(1, 10);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getArchiveType()).isEqualTo(DocConstants.ARCHIVE_TYPE_RECEIVE);
        }
    }

    @Nested
    @DisplayName("create() 新增档案")
    class Create {
        @Test
        @DisplayName("create 成功 — 默认状态 ACTIVE")
        void create_success() {
            DocArchive archive = new DocArchive();
            archive.setArchiveNo("ARC202606010");
            archive.setArchiveType(DocConstants.ARCHIVE_TYPE_SIGN_REPORT);
            archive.setSourceId(2000L);
            archive.setTitle("签报归档");

            when(mapper.insert(any(DocArchive.class))).thenAnswer(inv -> {
                DocArchive a = inv.getArgument(0);
                a.setId(ARCHIVE_ID);
                return 1;
            });

            Long id = service.create(archive);

            assertThat(id).isEqualTo(ARCHIVE_ID);
            verify(mapper).insert(archiveCaptor.capture());
            DocArchive saved = archiveCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(DocConstants.ARCHIVE_STATUS_ACTIVE);
            assertThat(saved.getArchiveType()).isEqualTo(DocConstants.ARCHIVE_TYPE_SIGN_REPORT);
            assertThat(saved.getSourceId()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("create 已指定 status 时保留")
        void create_preserveStatus() {
            DocArchive archive = new DocArchive();
            archive.setArchiveNo("ARC2");
            archive.setStatus(DocConstants.ARCHIVE_STATUS_FROZEN);

            when(mapper.insert(any(DocArchive.class))).thenAnswer(inv -> {
                DocArchive a = inv.getArgument(0);
                a.setId(ARCHIVE_ID);
                return 1;
            });

            service.create(archive);
            verify(mapper).insert(archiveCaptor.capture());
            assertThat(archiveCaptor.getValue().getStatus()).isEqualTo(DocConstants.ARCHIVE_STATUS_FROZEN);
        }
    }
}
