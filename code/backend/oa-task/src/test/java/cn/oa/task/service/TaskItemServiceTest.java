package cn.oa.task.service;

import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.mapper.TaskItemMapper;
import cn.oa.task.vo.TaskItemVO;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskItemService 单测 — 覆盖 create/update/assign/changeStatus/updateProgress/delete/get/listPage.
 */
@ExtendWith(MockitoExtension.class)
class TaskItemServiceTest {

    @Mock private TaskItemMapper mapper;
    @Captor private ArgumentCaptor<TaskItem> itemCaptor;

    private TaskItemService service;

    private static final Long EMP_ID = 7L;
    private static final Long TASK_ID = 7000L;
    private static final Long PROJECT_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new TaskItemService(mapper);
    }

    @Nested
    @DisplayName("create() 创建任务")
    class Create {
        @Test
        @DisplayName("创建成功 — 默认 TODO + 进度 0 + 优先级 NORMAL")
        void create_success() {
            TaskItemCreateDTO dto = new TaskItemCreateDTO();
            dto.setProjectId(PROJECT_ID);
            dto.setTaskName("写单元测试");
            dto.setDescription("覆盖 Service");
            dto.setAssigneeId(EMP_ID);

            when(mapper.insert(any(TaskItem.class))).thenAnswer(inv -> {
                TaskItem t = inv.getArgument(0);
                t.setId(TASK_ID);
                return 1;
            });

            Long id = service.create(dto, EMP_ID);

            assertThat(id).isEqualTo(TASK_ID);
            verify(mapper).insert(itemCaptor.capture());
            TaskItem saved = itemCaptor.getValue();
            assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(saved.getTaskName()).isEqualTo("写单元测试");
            assertThat(saved.getStatus()).isEqualTo("TODO");
            assertThat(saved.getProgress()).isEqualTo(0);
            assertThat(saved.getPriority()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("create 支持自定义 priority + 父任务")
        void create_withParent() {
            TaskItemCreateDTO dto = new TaskItemCreateDTO();
            dto.setProjectId(PROJECT_ID);
            dto.setTaskName("子任务");
            dto.setParentTaskId(666L);
            dto.setPriority("HIGH");
            dto.setPlanStartDate(LocalDate.of(2026, 6, 1));
            dto.setPlanEndDate(LocalDate.of(2026, 6, 5));

            when(mapper.insert(any(TaskItem.class))).thenAnswer(inv -> {
                TaskItem t = inv.getArgument(0);
                t.setId(TASK_ID);
                return 1;
            });

            service.create(dto, EMP_ID);

            verify(mapper).insert(itemCaptor.capture());
            TaskItem saved = itemCaptor.getValue();
            assertThat(saved.getParentTaskId()).isEqualTo(666L);
            assertThat(saved.getPriority()).isEqualTo("HIGH");
            assertThat(saved.getPlanStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(saved.getPlanEndDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        }
    }

    @Nested
    @DisplayName("update()/assign() 编辑/分派")
    class UpdateAssign {
        @Test
        @DisplayName("update 成功")
        void update_success() {
            TaskItem item = newItem("TODO");
            when(mapper.selectById(TASK_ID)).thenReturn(item);

            TaskItemCreateDTO dto = new TaskItemCreateDTO();
            dto.setTaskName("新名称");
            dto.setAssigneeId(99L);
            dto.setPriority("LOW");
            dto.setPlanEndDate(LocalDate.of(2026, 6, 10));

            service.update(TASK_ID, dto);

            verify(mapper).updateById(itemCaptor.capture());
            assertThat(itemCaptor.getValue().getTaskName()).isEqualTo("新名称");
            assertThat(itemCaptor.getValue().getAssigneeId()).isEqualTo(99L);
            assertThat(itemCaptor.getValue().getPriority()).isEqualTo("LOW");
            assertThat(itemCaptor.getValue().getPlanEndDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        }

        @Test
        @DisplayName("update 不存在抛 NOT_FOUND")
        void update_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.update(404L, new TaskItemCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("任务不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("assign 成功 — 重设负责人")
        void assign_success() {
            TaskItem item = newItem("TODO");
            when(mapper.selectById(TASK_ID)).thenReturn(item);

            service.assign(TASK_ID, 888L);

            verify(mapper).updateById(itemCaptor.capture());
            assertThat(itemCaptor.getValue().getAssigneeId()).isEqualTo(888L);
        }
    }

    @Nested
    @DisplayName("changeStatus() 任务状态机")
    class ChangeStatus {
        @Test
        @DisplayName("TODO -> IN_PROGRESS 自动写 actual_start")
        void changeStatus_toInProgress() {
            TaskItem item = newItem("TODO");
            when(mapper.selectById(TASK_ID)).thenReturn(item);

            service.changeStatus(TASK_ID, "IN_PROGRESS");

            verify(mapper).updateById(itemCaptor.capture());
            TaskItem updated = itemCaptor.getValue();
            assertThat(updated.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(updated.getActualStart()).isNotNull();
        }

        @Test
        @DisplayName("IN_PROGRESS -> DONE 自动写 actual_end + progress=100")
        void changeStatus_toDone() {
            TaskItem item = newItem("IN_PROGRESS");
            item.setActualStart(java.time.LocalDateTime.now().minusDays(1));
            when(mapper.selectById(TASK_ID)).thenReturn(item);

            service.changeStatus(TASK_ID, "DONE");

            verify(mapper).updateById(itemCaptor.capture());
            TaskItem updated = itemCaptor.getValue();
            assertThat(updated.getStatus()).isEqualTo("DONE");
            assertThat(updated.getActualEnd()).isNotNull();
            assertThat(updated.getProgress()).isEqualTo(100);
        }

        @Test
        @DisplayName("非法状态抛异常")
        void changeStatus_invalid() {
            assertThatThrownBy(() -> service.changeStatus(TASK_ID, "INVALID"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("非法状态");
            verify(mapper, never()).selectById(any(java.io.Serializable.class));
        }

        @Test
        @DisplayName("任务不存在抛 NOT_FOUND")
        void changeStatus_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.changeStatus(404L, "TODO"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("任务不存在");
        }
    }

    @Nested
    @DisplayName("updateProgress() 进度更新")
    class UpdateProgress {
        @Test
        @DisplayName("updateProgress 50 成功")
        void updateProgress_valid() {
            TaskItem item = newItem("IN_PROGRESS");
            when(mapper.selectById(TASK_ID)).thenReturn(item);

            service.updateProgress(TASK_ID, 50);

            verify(mapper).updateById(itemCaptor.capture());
            assertThat(itemCaptor.getValue().getProgress()).isEqualTo(50);
        }

        @Test
        @DisplayName("updateProgress -1 / 101 / null 抛 BizException")
        void updateProgress_invalidRange() {
            assertThatThrownBy(() -> service.updateProgress(TASK_ID, -1))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("进度必须在 0-100");
            assertThatThrownBy(() -> service.updateProgress(TASK_ID, 101))
                    .isInstanceOf(BizException.class);
            assertThatThrownBy(() -> service.updateProgress(TASK_ID, null))
                    .isInstanceOf(BizException.class);
            verify(mapper, never()).selectById(any(java.io.Serializable.class));
        }
    }

    @Nested
    @DisplayName("getById()/listPage() 查询")
    class Query {
        @Test
        @DisplayName("getById 返回 VO + 子任务数")
        void getById_success() {
            TaskItem item = newItem("TODO");
            item.setProgress(30);
            item.setSortOrder(1);
            when(mapper.selectById(TASK_ID)).thenReturn(item);
            when(mapper.findByParentId(TASK_ID)).thenReturn(List.of(new TaskItem(), new TaskItem()));

            TaskItemVO vo = service.getById(TASK_ID);

            assertThat(vo.getId()).isEqualTo(TASK_ID);
            assertThat(vo.getStatus()).isEqualTo("TODO");
            assertThat(vo.getProgress()).isEqualTo(30);
            assertThat(vo.getSortOrder()).isEqualTo(1);
            assertThat(vo.getSubTaskCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("getById 不存在抛 NOT_FOUND")
        void getById_notFound() {
            when(mapper.selectById(404L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(404L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("任务不存在");
        }

        @Test
        @DisplayName("listPage 返回分页 VO")
        @SuppressWarnings("unchecked")
        void listPage_success() {
            TaskItem item = newItem("TODO");
            Page<TaskItem> page = new Page<>(1, 10);
            page.setRecords(List.of(item));
            page.setTotal(1L);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            // toVO 会调 findByParentId
            when(mapper.findByParentId(any(Long.class))).thenReturn(Collections.emptyList());

            TaskItemQueryDTO q = new TaskItemQueryDTO();
            q.setPageNum(1);
            q.setPageSize(10);
            q.setProjectId(PROJECT_ID);
            q.setStatus("TODO");

            PageResult<TaskItemVO> result = service.listPage(q);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getStatus()).isEqualTo("TODO");
            assertThat(result.getList().get(0).getSubTaskCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("delete() 软删除")
    class Delete {
        @Test
        @DisplayName("delete 直接成功 (不校验存在性)")
        void delete_success() {
            service.delete(TASK_ID);
            verify(mapper).deleteById(TASK_ID);
        }
    }

    private TaskItem newItem(String status) {
        TaskItem t = new TaskItem();
        t.setId(TASK_ID);
        t.setProjectId(PROJECT_ID);
        t.setTaskName("任务");
        t.setStatus(status);
        t.setAssigneeId(EMP_ID);
        t.setPriority("NORMAL");
        t.setProgress(0);
        return t;
    }
}
