package cn.oa.task.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskHourCreateDTO;
import cn.oa.task.entity.TaskHour;
import cn.oa.task.mapper.TaskHourMapper;
import cn.oa.task.vo.TaskHourVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskHourService 单测 — 覆盖 create/delete/listByItem/listByEmpAndDateRange.
 */
@ExtendWith(MockitoExtension.class)
class TaskHourServiceTest {

    @Mock private TaskHourMapper mapper;
    @Captor private ArgumentCaptor<TaskHour> hourCaptor;

    private TaskHourService service;

    private static final Long EMP_ID = 5L;
    private static final Long ITEM_ID = 7000L;
    private static final Long HOUR_ID = 50000L;

    @BeforeEach
    void setUp() {
        service = new TaskHourService(mapper);
    }

    @Nested
    @DisplayName("create() 登记工时")
    class Create {
        @Test
        @DisplayName("登记成功 — 4 小时")
        void create_success() {
            TaskHourCreateDTO dto = new TaskHourCreateDTO();
            dto.setItemId(ITEM_ID);
            dto.setWorkDate(LocalDate.of(2026, 6, 1));
            dto.setHours(new BigDecimal("4.0"));
            dto.setDescription("写单元测试");

            when(mapper.insert(any(TaskHour.class))).thenAnswer(inv -> {
                TaskHour h = inv.getArgument(0);
                h.setId(HOUR_ID);
                return 1;
            });

            Long id = service.create(dto, EMP_ID);

            assertThat(id).isEqualTo(HOUR_ID);
            verify(mapper).insert(hourCaptor.capture());
            TaskHour saved = hourCaptor.getValue();
            assertThat(saved.getItemId()).isEqualTo(ITEM_ID);
            assertThat(saved.getHours()).isEqualByComparingTo(new BigDecimal("4.0"));
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getWorkDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(saved.getDescription()).isEqualTo("写单元测试");
        }

        @Test
        @DisplayName("itemId 为空抛 BizException")
        void create_nullItemId() {
            TaskHourCreateDTO dto = new TaskHourCreateDTO();
            dto.setItemId(null);
            dto.setHours(BigDecimal.ONE);
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("itemId 不能为空");
            verify(mapper, never()).insert(any(TaskHour.class));
        }

        @Test
        @DisplayName("工时 0 / 25 / null 抛 BizException")
        void create_invalidHours() {
            TaskHourCreateDTO dto1 = new TaskHourCreateDTO();
            dto1.setItemId(ITEM_ID);
            dto1.setHours(BigDecimal.ZERO);
            assertThatThrownBy(() -> service.create(dto1, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("工时必须在 (0, 24] 区间");

            TaskHourCreateDTO dto2 = new TaskHourCreateDTO();
            dto2.setItemId(ITEM_ID);
            dto2.setHours(new BigDecimal("25"));
            assertThatThrownBy(() -> service.create(dto2, EMP_ID))
                    .isInstanceOf(BizException.class);

            TaskHourCreateDTO dto3 = new TaskHourCreateDTO();
            dto3.setItemId(ITEM_ID);
            dto3.setHours(null);
            assertThatThrownBy(() -> service.create(dto3, EMP_ID))
                    .isInstanceOf(BizException.class);

            verify(mapper, never()).insert(any(TaskHour.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除工时")
    class Delete {
        @Test
        @DisplayName("delete 成功 — 创建人删除")
        void delete_success() {
            TaskHour h = new TaskHour();
            h.setId(HOUR_ID);
            h.setEmpId(EMP_ID);
            when(mapper.selectById(HOUR_ID)).thenReturn(h);

            service.delete(HOUR_ID, EMP_ID);

            verify(mapper).deleteById(HOUR_ID);
        }

        @Test
        @DisplayName("非创建人 — 抛 FORBIDDEN")
        void delete_notOwner() {
            TaskHour h = new TaskHour();
            h.setId(HOUR_ID);
            h.setEmpId(EMP_ID);
            when(mapper.selectById(HOUR_ID)).thenReturn(h);

            assertThatThrownBy(() -> service.delete(HOUR_ID, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能删除自己的工时")
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
                    .hasMessageContaining("工时不存在");
        }
    }

    @Nested
    @DisplayName("listByItem()/listByEmpAndDateRange() 查询")
    class Query {
        @Test
        @DisplayName("listByItem 返回 VO 列表")
        void listByItem_success() {
            TaskHour h = new TaskHour();
            h.setId(HOUR_ID);
            h.setItemId(ITEM_ID);
            h.setHours(new BigDecimal("3"));
            h.setEmpId(EMP_ID);
            when(mapper.findByItemId(ITEM_ID)).thenReturn(List.of(h));

            List<TaskHourVO> list = service.listByItem(ITEM_ID);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getId()).isEqualTo(HOUR_ID);
            assertThat(list.get(0).getItemId()).isEqualTo(ITEM_ID);
            assertThat(list.get(0).getHours()).isEqualByComparingTo(new BigDecimal("3"));
        }

        @Test
        @DisplayName("listByEmpAndDateRange 返回员工某段时间工时")
        void listByEmpAndDateRange_success() {
            TaskHour h1 = new TaskHour();
            h1.setId(1L);
            h1.setHours(new BigDecimal("8"));
            h1.setWorkDate(LocalDate.of(2026, 6, 1));
            h1.setEmpId(EMP_ID);
            TaskHour h2 = new TaskHour();
            h2.setId(2L);
            h2.setHours(new BigDecimal("7"));
            h2.setWorkDate(LocalDate.of(2026, 6, 2));
            h2.setEmpId(EMP_ID);

            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);
            when(mapper.findByEmpAndDateRange(EMP_ID, from, to)).thenReturn(List.of(h1, h2));

            List<TaskHourVO> list = service.listByEmpAndDateRange(EMP_ID, from, to);

            assertThat(list).hasSize(2);
            assertThat(list).extracting(TaskHourVO::getHours)
                    .containsExactly(new BigDecimal("8"), new BigDecimal("7"));
        }
    }
}
