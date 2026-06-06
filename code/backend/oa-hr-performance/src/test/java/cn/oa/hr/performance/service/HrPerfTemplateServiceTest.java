package cn.oa.hr.performance.service;

import cn.oa.hr.performance.entity.HrPerfTemplate;
import cn.oa.hr.performance.mapper.HrPerfTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HrPerfTemplateService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class HrPerfTemplateServiceTest {

    @Mock
    private HrPerfTemplateMapper mapper;

    @Captor
    private ArgumentCaptor<HrPerfTemplate> templateCaptor;

    private HrPerfTemplateService service;

    @BeforeEach
    void setUp() {
        service = new HrPerfTemplateService(mapper);
    }

    @Nested
    @DisplayName("create() 创建模板")
    class Create {

        @Test
        @DisplayName("创建成功")
        void create_success() {
            // given
            HrPerfTemplate t = new HrPerfTemplate();
            t.setTemplateName("季度绩效考核模板");
            t.setDescription("用于季度绩效考核");
            t.setDimensions("{\"work_quality\":30,\"work_efficiency\":30,\"teamwork\":20,\"innovation\":20}");
            t.setStatus("ACTIVE");

            when(mapper.insert(t)).thenAnswer(invocation -> {
                t.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(t);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(t);
        }
    }

    @Nested
    @DisplayName("update() 更新模板")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            HrPerfTemplate t = new HrPerfTemplate();
            t.setId(1L);
            t.setTemplateName("更新后的模板");

            // when
            service.update(t);

            // then
            verify(mapper).updateById(t);
        }
    }

    @Nested
    @DisplayName("getById() 查询模板")
    class GetById {

        @Test
        @DisplayName("查询成功")
        void getById_success() {
            // given
            HrPerfTemplate t = new HrPerfTemplate();
            t.setId(1L);
            t.setTemplateName("年度考核模板");
            t.setStatus("ACTIVE");

            when(mapper.selectById(1L)).thenReturn(t);

            // when
            HrPerfTemplate result = service.getById(1L);

            // then
            assertThat(result).isSameAs(t);
            assertThat(result.getTemplateName()).isEqualTo("年度考核模板");
        }

        @Test
        @DisplayName("不存在时返回 null")
        void getById_notFound_returnsNull() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            HrPerfTemplate result = service.getById(999L);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("list() 列表查询")
    class ListMethod {

        @Test
        @DisplayName("按状态过滤")
        void list_filterByStatus() {
            // given
            HrPerfTemplate t = new HrPerfTemplate();
            t.setId(1L);
            t.setTemplateName("ACTIVE模板");
            t.setStatus("ACTIVE");

            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));

            // when
            List<HrPerfTemplate> result = service.list("ACTIVE");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
            verify(mapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("status 为 null 时查询全部")
        void list_all() {
            // given
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            // when
            List<HrPerfTemplate> result = service.list(null);

            // then
            assertThat(result).isEmpty();
            verify(mapper).selectList(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除模板")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // when
            service.delete(1L);

            // then
            verify(mapper).deleteById(1L);
        }
    }
}
