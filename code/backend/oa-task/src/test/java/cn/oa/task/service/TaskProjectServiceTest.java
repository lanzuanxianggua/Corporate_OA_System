package cn.oa.task.service;

import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.entity.TaskProject;
import cn.oa.task.mapper.TaskProjectMapper;
import cn.oa.task.vo.TaskProjectVO;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskProjectService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class TaskProjectServiceTest {

    @Mock
    private TaskProjectMapper mapper;

    @Captor
    private ArgumentCaptor<TaskProject> projectCaptor;

    private TaskProjectService service;

    private static final Long EMP_ID = 42L;

    @BeforeEach
    void setUp() {
        service = new TaskProjectService(mapper);
    }

    @Nested
    @DisplayName("create() 创建项目")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回项目ID, 默认状态 ACTIVE")
        void create_success() {
            // given
            TaskProjectCreateDTO dto = new TaskProjectCreateDTO();
            dto.setProjectName("OA系统v2开发");
            dto.setDescription("企业OA系统第二版开发项目");
            dto.setStartDate(LocalDate.of(2026, 1, 1));
            dto.setEndDate(LocalDate.of(2026, 12, 31));
            dto.setDeptId(10L);

            when(mapper.insert(any(TaskProject.class))).thenAnswer(invocation -> {
                TaskProject p = invocation.getArgument(0);
                p.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(projectCaptor.capture());

            TaskProject saved = projectCaptor.getValue();
            assertThat(saved.getProjectName()).isEqualTo("OA系统v2开发");
            assertThat(saved.getDescription()).isEqualTo("企业OA系统第二版开发项目");
            assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getOwnerEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("创建项目不含描述和日期")
        void create_minimal() {
            // given
            TaskProjectCreateDTO dto = new TaskProjectCreateDTO();
            dto.setProjectName("简单任务");

            when(mapper.insert(any(TaskProject.class))).thenAnswer(invocation -> {
                TaskProject p = invocation.getArgument(0);
                p.setId(101L);
                return 1;
            });

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(101L);
            verify(mapper).insert(projectCaptor.capture());
            assertThat(projectCaptor.getValue().getProjectName()).isEqualTo("简单任务");
            assertThat(projectCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("getById() 查询项目")
    class GetById {

        @Test
        @DisplayName("查询成功 — 返回 VO")
        void getById_success() {
            // given
            TaskProject project = new TaskProject();
            project.setId(1L);
            project.setProjectName("OA系统v2");
            project.setProjectCode("P2026001");
            project.setDescription("开发项目");
            project.setStatus("ACTIVE");
            project.setStartDate(LocalDate.of(2026, 1, 1));
            project.setEndDate(LocalDate.of(2026, 12, 31));
            project.setDeptId(10L);
            project.setOwnerEmpId(EMP_ID);

            when(mapper.selectById(1L)).thenReturn(project);

            // when
            TaskProjectVO vo = service.getById(1L);

            // then
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getProjectName()).isEqualTo("OA系统v2");
            assertThat(vo.getProjectCode()).isEqualTo("P2026001");
            assertThat(vo.getDescription()).isEqualTo("开发项目");
            assertThat(vo.getStatus()).isEqualTo("ACTIVE");
            assertThat(vo.getDeptId()).isEqualTo(10L);
            assertThat(vo.getOwnerEmpId()).isEqualTo(EMP_ID);
        }

        @Test
        @DisplayName("项目不存在时抛出 BizException")
        void getById_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("项目不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("delete() 删除项目")
    class Delete {

        @Test
        @DisplayName("删除成功 — 直接调用删除不抛异常")
        void delete_success() {
            // when
            service.delete(1L);

            // then
            verify(mapper).deleteById(1L);
        }
    }
}
