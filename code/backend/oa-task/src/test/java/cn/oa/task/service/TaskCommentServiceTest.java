package cn.oa.task.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskCommentCreateDTO;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.mapper.TaskCommentMapper;
import cn.oa.task.vo.TaskCommentVO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskCommentService 单测 — 覆盖 create/delete/listByItem.
 */
@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock private TaskCommentMapper mapper;
    @Captor private ArgumentCaptor<TaskComment> commentCaptor;

    private TaskCommentService service;

    private static final Long EMP_ID = 5L;
    private static final Long ITEM_ID = 7000L;
    private static final Long COMMENT_ID = 50001L;

    @BeforeEach
    void setUp() {
        service = new TaskCommentService(mapper);
    }

    @Nested
    @DisplayName("create() 添加评论")
    class Create {
        @Test
        @DisplayName("create 成功 — 顶级评论")
        void create_topLevel() {
            TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
            dto.setItemId(ITEM_ID);
            dto.setContent("评论内容");

            when(mapper.insert(any(TaskComment.class))).thenAnswer(inv -> {
                TaskComment c = inv.getArgument(0);
                c.setId(COMMENT_ID);
                return 1;
            });

            Long id = service.create(dto, EMP_ID);

            assertThat(id).isEqualTo(COMMENT_ID);
            verify(mapper).insert(commentCaptor.capture());
            TaskComment saved = commentCaptor.getValue();
            assertThat(saved.getItemId()).isEqualTo(ITEM_ID);
            assertThat(saved.getContent()).isEqualTo("评论内容");
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getParentCommentId()).isNull();
        }

        @Test
        @DisplayName("create 成功 — 回复评论 (parentCommentId 不为空)")
        void create_reply() {
            TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
            dto.setItemId(ITEM_ID);
            dto.setContent("回复");
            dto.setParentCommentId(999L);

            when(mapper.insert(any(TaskComment.class))).thenAnswer(inv -> {
                TaskComment c = inv.getArgument(0);
                c.setId(COMMENT_ID);
                return 1;
            });

            service.create(dto, EMP_ID);
            verify(mapper).insert(commentCaptor.capture());
            assertThat(commentCaptor.getValue().getParentCommentId()).isEqualTo(999L);
        }

        @Test
        @DisplayName("itemId 为空抛 BizException")
        void create_nullItemId() {
            TaskCommentCreateDTO dto = new TaskCommentCreateDTO();
            dto.setItemId(null);
            dto.setContent("X");
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("itemId 不能为空");
            verify(mapper, never()).insert(any(TaskComment.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除评论")
    class Delete {
        @Test
        @DisplayName("delete 成功 — 自己的评论")
        void delete_success() {
            TaskComment c = new TaskComment();
            c.setId(COMMENT_ID);
            c.setEmpId(EMP_ID);
            when(mapper.selectById(COMMENT_ID)).thenReturn(c);

            service.delete(COMMENT_ID, EMP_ID);

            verify(mapper).deleteById(COMMENT_ID);
        }

        @Test
        @DisplayName("非创建人 — 抛 FORBIDDEN")
        void delete_notOwner() {
            TaskComment c = new TaskComment();
            c.setId(COMMENT_ID);
            c.setEmpId(EMP_ID);
            when(mapper.selectById(COMMENT_ID)).thenReturn(c);

            assertThatThrownBy(() -> service.delete(COMMENT_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能删除自己的评论")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.FORBIDDEN.getCode()));
            verify(mapper, never()).deleteById(any(java.io.Serializable.class));
        }

        @Test
        @DisplayName("不存在抛 NOT_FOUND")
        void delete_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.delete(404L, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("评论不存在");
        }
    }

    @Nested
    @DisplayName("listByItem() 列表查询")
    class ListByItem {
        @Test
        @DisplayName("listByItem 按时间升序返回评论 VO")
        void listByItem_success() {
            TaskComment c1 = new TaskComment();
            c1.setId(1L);
            c1.setItemId(ITEM_ID);
            c1.setContent("评论 A");
            c1.setEmpId(EMP_ID);
            TaskComment c2 = new TaskComment();
            c2.setId(2L);
            c2.setItemId(ITEM_ID);
            c2.setContent("评论 B");
            c2.setEmpId(EMP_ID);
            c2.setParentCommentId(1L);

            when(mapper.findByItemIdOrderByTime(ITEM_ID)).thenReturn(List.of(c1, c2));

            List<TaskCommentVO> list = service.listByItem(ITEM_ID);

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getContent()).isEqualTo("评论 A");
            assertThat(list.get(1).getParentCommentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("listByItem 空列表也返回空 list")
        void listByItem_empty() {
            when(mapper.findByItemIdOrderByTime(ITEM_ID)).thenReturn(List.of());
            List<TaskCommentVO> list = service.listByItem(ITEM_ID);
            assertThat(list).isEmpty();
        }
    }
}
