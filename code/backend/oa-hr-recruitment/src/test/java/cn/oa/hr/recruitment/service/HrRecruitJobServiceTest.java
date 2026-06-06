package cn.oa.hr.recruitment.service;

import cn.oa.hr.recruitment.entity.HrRecruitJob;
import cn.oa.hr.recruitment.mapper.HrRecruitJobMapper;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * HrRecruitJobService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class HrRecruitJobServiceTest {

    @Mock
    private HrRecruitJobMapper mapper;

    @Captor
    private ArgumentCaptor<HrRecruitJob> jobCaptor;

    private HrRecruitJobService service;

    @BeforeEach
    void setUp() {
        service = new HrRecruitJobService(mapper);
    }

    @Nested
    @DisplayName("create() 创建岗位")
    class Create {

        @Test
        @DisplayName("创建成功")
        void create_success() {
            // given
            HrRecruitJob job = new HrRecruitJob();
            job.setJobTitle("Java后端开发");
            job.setDeptId(10L);
            job.setHeadcount(3);
            job.setRequirement("3年以上Java开发经验");
            job.setResponsibility("负责后端模块开发");
            job.setSalaryMin(BigDecimal.valueOf(15000));
            job.setSalaryMax(BigDecimal.valueOf(25000));
            job.setStatus("PUBLISHED");

            when(mapper.insert(job)).thenAnswer(invocation -> {
                job.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(job);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(job);
        }
    }

    @Nested
    @DisplayName("update() 更新岗位")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            HrRecruitJob job = new HrRecruitJob();
            job.setId(1L);
            job.setJobTitle("高级Java开发");
            job.setHeadcount(2);

            // when
            service.update(job);

            // then
            verify(mapper).updateById(job);
        }
    }

    @Nested
    @DisplayName("getById() 查询岗位")
    class GetById {

        @Test
        @DisplayName("查询成功")
        void getById_success() {
            // given
            HrRecruitJob job = new HrRecruitJob();
            job.setId(1L);
            job.setJobTitle("产品经理");
            job.setStatus("PUBLISHED");

            when(mapper.selectById(1L)).thenReturn(job);

            // when
            HrRecruitJob result = service.getById(1L);

            // then
            assertThat(result).isSameAs(job);
            assertThat(result.getJobTitle()).isEqualTo("产品经理");
        }

        @Test
        @DisplayName("不存在时返回 null")
        void getById_notFound_returnsNull() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when
            HrRecruitJob result = service.getById(999L);

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
            Page<HrRecruitJob> pageResult = new Page<>(1, 10);
            HrRecruitJob job = new HrRecruitJob();
            job.setId(1L);
            job.setJobTitle("前端开发");
            job.setStatus("PUBLISHED");
            pageResult.setRecords(List.of(job));
            pageResult.setTotal(1);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

            // when
            Page<HrRecruitJob> result = service.listPage("PUBLISHED", 1, 10);

            // then
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getJobTitle()).isEqualTo("前端开发");
            verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("status 为 null 时查询全部")
        void listPage_all() {
            // given
            Page<HrRecruitJob> pageResult = new Page<>(1, 10);
            pageResult.setRecords(List.of());
            pageResult.setTotal(0);

            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);

            // when
            Page<HrRecruitJob> result = service.listPage(null, 1, 10);

            // then
            assertThat(result.getRecords()).isEmpty();
        }
    }
}
