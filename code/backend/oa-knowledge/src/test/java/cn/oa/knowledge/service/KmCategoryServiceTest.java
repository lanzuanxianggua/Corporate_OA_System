package cn.oa.knowledge.service;

import cn.oa.knowledge.dto.KmCategoryCreateDTO;
import cn.oa.knowledge.entity.KmCategory;
import cn.oa.knowledge.mapper.KmCategoryMapper;
import cn.oa.knowledge.mapper.KmEntryMapper;
import cn.oa.knowledge.vo.KmCategoryVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * KmCategoryService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class KmCategoryServiceTest {

    @Mock
    private KmCategoryMapper mapper;

    @Mock
    private KmEntryMapper entryMapper;

    @Captor
    private ArgumentCaptor<KmCategory> categoryCaptor;

    private KmCategoryService service;

    @BeforeEach
    void setUp() {
        service = new KmCategoryService(mapper, entryMapper);
    }

    @Nested
    @DisplayName("create() 创建分类")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回分类ID")
        void create_success() {
            // given
            KmCategoryCreateDTO dto = new KmCategoryCreateDTO();
            dto.setCategoryName("技术文档");
            dto.setParentId(0L);
            dto.setSortOrder(1);
            dto.setDescription("技术相关文档分类");

            when(mapper.insert(any(KmCategory.class))).thenAnswer(invocation -> {
                KmCategory c = invocation.getArgument(0);
                c.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(categoryCaptor.capture());

            KmCategory saved = categoryCaptor.getValue();
            assertThat(saved.getCategoryName()).isEqualTo("技术文档");
            assertThat(saved.getParentId()).isZero();
            assertThat(saved.getSortOrder()).isEqualTo(1);
            assertThat(saved.getDescription()).isEqualTo("技术相关文档分类");
        }

        @Test
        @DisplayName("创建子分类")
        void create_childCategory() {
            // given
            KmCategoryCreateDTO dto = new KmCategoryCreateDTO();
            dto.setCategoryName("Java基础");
            dto.setParentId(100L);
            dto.setSortOrder(2);

            when(mapper.insert(any(KmCategory.class))).thenAnswer(invocation -> {
                KmCategory c = invocation.getArgument(0);
                c.setId(101L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(101L);
            verify(mapper).insert(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().getParentId()).isEqualTo(100L);
            assertThat(categoryCaptor.getValue().getCategoryName()).isEqualTo("Java基础");
        }
    }

    @Nested
    @DisplayName("listTree() 树形结构")
    class ListTree {

        @Test
        @DisplayName("返回顶级分类及其子分类树")
        void listTree_withChildren() {
            // given
            KmCategory root1 = new KmCategory();
            root1.setId(1L);
            root1.setCategoryName("技术文档");
            root1.setParentId(0L);

            KmCategory root2 = new KmCategory();
            root2.setId(2L);
            root2.setCategoryName("管理规范");
            root2.setParentId(0L);

            KmCategory child1 = new KmCategory();
            child1.setId(3L);
            child1.setCategoryName("Java");
            child1.setParentId(1L);

            KmCategory child2 = new KmCategory();
            child2.setId(4L);
            child2.setCategoryName("前端");
            child2.setParentId(1L);

            KmCategory subChild = new KmCategory();
            subChild.setId(5L);
            subChild.setCategoryName("Spring Boot");
            subChild.setParentId(3L);

            when(mapper.findTree()).thenReturn(List.of(root1, root2, child1, child2, subChild));

            // when
            List<KmCategoryVO> tree = service.listTree();

            // then
            assertThat(tree).hasSize(2);

            KmCategoryVO techRoot = tree.get(0);
            assertThat(techRoot.getCategoryName()).isEqualTo("技术文档");

            // 顶级根节点应有 2 个子分类
            assertThat(techRoot.getChildren()).hasSize(2);
            assertThat(techRoot.getChildren().get(0).getCategoryName()).isEqualTo("Java");
            assertThat(techRoot.getChildren().get(1).getCategoryName()).isEqualTo("前端");

            // Java 子分类下应有 Spring Boot
            assertThat(techRoot.getChildren().get(0).getChildren()).hasSize(1);
            assertThat(techRoot.getChildren().get(0).getChildren().get(0).getCategoryName()).isEqualTo("Spring Boot");

            KmCategoryVO mgmtRoot = tree.get(1);
            assertThat(mgmtRoot.getCategoryName()).isEqualTo("管理规范");
            assertThat(mgmtRoot.getChildren()).isEmpty();

            verify(mapper, times(6)).findTree();
        }

        @Test
        @DisplayName("没有分类时返回空列表")
        void listTree_empty() {
            // given
            when(mapper.findTree()).thenReturn(List.of());

            // when
            List<KmCategoryVO> tree = service.listTree();

            // then
            assertThat(tree).isEmpty();
            verify(mapper).findTree();
        }
    }

    @Nested
    @DisplayName("delete() 删除分类")
    class Delete {

        @Test
        @DisplayName("删除子分类时抛出 BizException")
        void delete_withChildren_throwsException() {
            // given
            when(mapper.selectCount(any())).thenReturn(2L);

            // when & then
            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("请先删除子分类");

            verify(mapper, never()).deleteById(any());
        }
    }
}
