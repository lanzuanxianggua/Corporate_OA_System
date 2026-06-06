package cn.oa.hr.training.service;

import cn.oa.hr.training.entity.HrTrainCourse;
import cn.oa.hr.training.mapper.HrTrainCourseMapper;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HrTrainCourseService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class HrTrainCourseServiceTest {

    @Mock
    private HrTrainCourseMapper mapper;

    @Captor
    private ArgumentCaptor<HrTrainCourse> courseCaptor;

    private HrTrainCourseService service;

    @BeforeEach
    void setUp() {
        service = new HrTrainCourseService(mapper);
    }

    @Nested
    @DisplayName("create() 创建课程")
    class Create {

        @Test
        @DisplayName("创建成功")
        void create_success() {
            // given
            HrTrainCourse course = new HrTrainCourse();
            course.setCourseName("Java进阶培训");
            course.setCourseType("TECHNICAL");
            course.setCredit(BigDecimal.valueOf(2.5));
            course.setTotalHours(40);
            course.setDescription("面向3年以上Java开发人员");
            course.setStatus("ACTIVE");

            when(mapper.insert(course)).thenAnswer(invocation -> {
                course.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(course);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(course);
        }
    }

    @Nested
    @DisplayName("update() 更新课程")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            HrTrainCourse course = new HrTrainCourse();
            course.setId(1L);
            course.setCourseName("Spring Boot实战");

            // when
            service.update(course);

            // then
            verify(mapper).updateById(course);
        }
    }

    @Nested
    @DisplayName("delete() 删除课程")
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

    @Nested
    @DisplayName("getById() 查询课程")
    class GetById {

        @Test
        @DisplayName("查询成功")
        void getById_success() {
            // given
            HrTrainCourse course = new HrTrainCourse();
            course.setId(1L);
            course.setCourseName("新员工入职培训");
            course.setStatus("ACTIVE");

            when(mapper.selectById(1L)).thenReturn(course);

            // when
            HrTrainCourse result = service.getById(1L);

            // then
            assertThat(result).isSameAs(course);
            assertThat(result.getCourseName()).isEqualTo("新员工入职培训");
        }

        @Test
        @DisplayName("不存在时返回 null")
        void getById_notFound_returnsNull() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            HrTrainCourse result = service.getById(999L);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("listPage() 分页查询")
    class ListPage {

        @Test
        @DisplayName("按状态分页查询")
        void listPage_filterByStatus() {
            // given
            Page<HrTrainCourse> pageResult = new Page<>(1, 10);
            HrTrainCourse course = new HrTrainCourse();
            course.setId(1L);
            course.setCourseName("项目管理培训");
            course.setStatus("ACTIVE");
            pageResult.setRecords(List.of(course));
            pageResult.setTotal(1);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

            // when
            Page<HrTrainCourse> result = service.listPage("ACTIVE", 1, 10);

            // then
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getCourseName()).isEqualTo("项目管理培训");
            verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("status 为 null 时查询全部")
        void listPage_all() {
            // given
            Page<HrTrainCourse> pageResult = new Page<>(1, 10);
            pageResult.setRecords(List.of());
            pageResult.setTotal(0);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

            // when
            Page<HrTrainCourse> result = service.listPage(null, 1, 10);

            // then
            assertThat(result.getRecords()).isEmpty();
        }
    }
}
