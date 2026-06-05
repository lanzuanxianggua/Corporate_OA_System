package cn.oa.hr.employee.service;

import cn.oa.hr.employee.dto.HrEmployeeProfileCreateDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileUpdateDTO;
import cn.oa.hr.employee.entity.HrEmployeeProfile;
import cn.oa.hr.employee.mapper.HrEmployeeProfileMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * HrEmployeeProfileService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeProfileServiceTest {

    @Mock
    private HrEmployeeProfileMapper mapper;

    @Captor
    private ArgumentCaptor<HrEmployeeProfile> profileCaptor;

    private HrEmployeeProfileService service;

    @BeforeEach
    void setUp() {
        service = new HrEmployeeProfileService(mapper);
    }

    @Nested
    @DisplayName("create() 新增员工档案")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回新档案ID")
        void create_success() {
            // given
            HrEmployeeProfileCreateDTO dto = new HrEmployeeProfileCreateDTO();
            dto.setEmpId(1L);
            dto.setWorkNo("HR20260001");
            dto.setHireDate(LocalDate.of(2026, 6, 1));
            dto.setContractType("REGULAR");
            dto.setContractEndDate(LocalDate.of(2029, 6, 1));
            dto.setEmergencyContact("张三");
            dto.setEmergencyPhone("13800138000");
            dto.setBankName("中国工商银行");
            dto.setBankAccount("6222021234567890123");

            when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(mapper.insert(any(HrEmployeeProfile.class))).thenAnswer(invocation -> {
                HrEmployeeProfile p = invocation.getArgument(0);
                p.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
            verify(mapper, times(1)).insert(profileCaptor.capture());

            HrEmployeeProfile saved = profileCaptor.getValue();
            assertThat(saved.getEmpId()).isEqualTo(1L);
            assertThat(saved.getWorkNo()).isEqualTo("HR20260001");
            assertThat(saved.getHireDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(saved.getContractType()).isEqualTo("REGULAR");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("empId已存在档案时抛出 BizException")
        void create_duplicateEmpId_throwsException() {
            // given
            HrEmployeeProfileCreateDTO dto = new HrEmployeeProfileCreateDTO();
            dto.setEmpId(1L);

            when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // when & then
            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(BizException.class)
                    .hasMessage("该员工已存在档案")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.BAD_REQUEST.getCode()));

            verify(mapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
            verify(mapper, never()).insert(any(HrEmployeeProfile.class));
        }
    }

    @Nested
    @DisplayName("update() 修改员工档案")
    class Update {

        @Test
        @DisplayName("更新成功 — 保留empId不变")
        void update_success() {
            // given
            HrEmployeeProfile exist = new HrEmployeeProfile();
            exist.setId(1L);
            exist.setEmpId(10L);
            exist.setWorkNo("HR001");

            HrEmployeeProfileUpdateDTO dto = new HrEmployeeProfileUpdateDTO();
            dto.setWorkNo("HR002");
            dto.setContractType("CONTRACT");
            dto.setStatus("LEAVE");

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.update(1L, dto);

            // then
            verify(mapper, times(1)).selectById(1L);
            verify(mapper, times(1)).updateById(profileCaptor.capture());

            HrEmployeeProfile patch = profileCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getEmpId()).isEqualTo(10L);   // 保留原值
            assertThat(patch.getWorkNo()).isEqualTo("HR002");
            assertThat(patch.getContractType()).isEqualTo("CONTRACT");
            assertThat(patch.getStatus()).isEqualTo("LEAVE");
        }

        @Test
        @DisplayName("档案不存在时抛出 BizException")
        void update_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.update(999L, new HrEmployeeProfileUpdateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessage("员工档案不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).updateById(any(HrEmployeeProfile.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除员工档案")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // given
            HrEmployeeProfile exist = new HrEmployeeProfile();
            exist.setId(1L);

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.delete(1L);

            // then
            verify(mapper, times(1)).selectById(1L);
            verify(mapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("档案不存在时抛出 BizException")
        void delete_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("员工档案不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).deleteById(any(Long.class));
        }
    }

    @Nested
    @DisplayName("list() 员工档案列表")
    class ListMethod {

        @Test
        @DisplayName("返回列表结果")
        void list_returnsResults() {
            // given
            Map<String, Object> row1 = Map.of("id", 1L, "emp_name", "张三");
            Map<String, Object> row2 = Map.of("id", 2L, "emp_name", "李四");
            when(mapper.findAllWithJoins(100)).thenReturn(List.of(row1, row2));

            // when
            List<Map<String, Object>> result = service.list(100);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("emp_name")).isEqualTo("张三");
            verify(mapper, times(1)).findAllWithJoins(100);
        }

        @Test
        @DisplayName("limit超过100时截断为100")
        void list_capsLimitAt100() {
            // given
            when(mapper.findAllWithJoins(100)).thenReturn(List.of());

            // when
            service.list(200);

            // then
            verify(mapper, times(1)).findAllWithJoins(100);
        }

        @Test
        @DisplayName("没有数据时返回空列表")
        void list_returnsEmpty() {
            // given
            when(mapper.findAllWithJoins(100)).thenReturn(List.of());

            // when
            List<Map<String, Object>> result = service.list(100);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDetail() 员工档案详情")
    class GetDetail {

        @Test
        @DisplayName("查询详情成功")
        void getDetail_success() {
            // given
            Map<String, Object> detail = Map.of("id", 1L, "emp_name", "张三");
            when(mapper.findDetail(1L)).thenReturn(detail);

            // when
            Map<String, Object> result = service.getDetail(1L);

            // then
            assertThat(result).isEqualTo(detail);
            verify(mapper, times(1)).findDetail(1L);
        }

        @Test
        @DisplayName("档案不存在时抛出 BizException")
        void getDetail_notFound_throwsException() {
            // given
            when(mapper.findDetail(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getDetail(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("员工档案不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("getByEmpId() 按员工ID查询档案")
    class GetByEmpId {

        @Test
        @DisplayName("查询成功返回档案")
        void getByEmpId_success() {
            // given
            HrEmployeeProfile profile = new HrEmployeeProfile();
            profile.setId(1L);
            profile.setEmpId(10L);
            profile.setWorkNo("HR001");

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);

            // when
            HrEmployeeProfile result = service.getByEmpId(10L);

            // then
            assertThat(result).isSameAs(profile);
            assertThat(result.getEmpId()).isEqualTo(10L);
            verify(mapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("empId不存在时抛出 BizException")
        void getByEmpId_notFound_throwsException() {
            // given
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getByEmpId(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("该员工不存在档案")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }
}
