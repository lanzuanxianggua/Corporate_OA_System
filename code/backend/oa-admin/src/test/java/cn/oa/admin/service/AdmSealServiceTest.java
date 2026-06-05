package cn.oa.admin.service;

import cn.oa.admin.dto.AdmSealCreateDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.mapper.AdmSealMapper;
import cn.oa.admin.vo.AdmSealVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdmSealService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class AdmSealServiceTest {

    @Mock
    private AdmSealMapper sealMapper;

    @Mock
    private SysEmpMapper empMapper;

    @Mock
    private SysDeptMapper deptMapper;

    @Captor
    private ArgumentCaptor<AdmSeal> sealCaptor;

    private AdmSealService service;

    @BeforeEach
    void setUp() {
        service = new AdmSealService(sealMapper, empMapper, deptMapper);
    }

    @Nested
    @DisplayName("create() 新增印章")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回新印章ID, 默认状态 ACTIVE")
        void create_success() {
            // given
            AdmSealCreateDTO dto = new AdmSealCreateDTO();
            dto.setSealName("公章");
            dto.setSealType("OFFICIAL");
            dto.setCustodian(1L);
            dto.setDeptId(10L);
            dto.setLocation("A栋3楼档案室");

            when(sealMapper.insert(any(AdmSeal.class))).thenAnswer(invocation -> {
                AdmSeal s = invocation.getArgument(0);
                s.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(100L);
            verify(sealMapper, times(1)).insert(sealCaptor.capture());

            AdmSeal saved = sealCaptor.getValue();
            assertThat(saved.getSealName()).isEqualTo("公章");
            assertThat(saved.getSealType()).isEqualTo("OFFICIAL");
            assertThat(saved.getCustodian()).isEqualTo(1L);
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getLocation()).isEqualTo("A栋3楼档案室");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("update() 修改印章")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            AdmSeal exist = new AdmSeal();
            exist.setId(1L);
            exist.setSealName("旧公章");

            AdmSealCreateDTO dto = new AdmSealCreateDTO();
            dto.setSealName("新公章");
            dto.setSealType("CONTRACT");
            dto.setCustodian(2L);
            dto.setDeptId(20L);
            dto.setLocation("B栋2楼");

            when(sealMapper.selectById(1L)).thenReturn(exist);

            // when
            service.update(1L, dto);

            // then
            verify(sealMapper, times(1)).selectById(1L);
            verify(sealMapper, times(1)).updateById(sealCaptor.capture());

            AdmSeal patch = sealCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getSealName()).isEqualTo("新公章");
            assertThat(patch.getSealType()).isEqualTo("CONTRACT");
            assertThat(patch.getCustodian()).isEqualTo(2L);
            assertThat(patch.getDeptId()).isEqualTo(20L);
            assertThat(patch.getLocation()).isEqualTo("B栋2楼");
        }

        @Test
        @DisplayName("印章不存在时抛出 BizException")
        void update_notFound_throwsException() {
            // given
            when(sealMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.update(999L, new AdmSealCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessage("印章不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(sealMapper, never()).updateById(any(AdmSeal.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除印章")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // given
            AdmSeal exist = new AdmSeal();
            exist.setId(1L);

            when(sealMapper.selectById(1L)).thenReturn(exist);

            // when
            service.delete(1L);

            // then
            verify(sealMapper, times(1)).selectById(1L);
            verify(sealMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("印章不存在时抛出 BizException")
        void delete_notFound_throwsException() {
            // given
            when(sealMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("印章不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(sealMapper, never()).deleteById(any(Long.class));
        }
    }

    @Nested
    @DisplayName("getById() 印章详情")
    class GetById {

        @Test
        @DisplayName("查询详情成功 — 关联保管人姓名和部门名称")
        void getById_success() {
            // given
            AdmSeal seal = new AdmSeal();
            seal.setId(1L);
            seal.setSealName("公章");
            seal.setCustodian(10L);
            seal.setDeptId(20L);

            SysEmp emp = new SysEmp();
            emp.setId(10L);
            emp.setRealName("张三");

            SysDept dept = new SysDept();
            dept.setId(20L);
            dept.setDeptName("行政部");

            when(sealMapper.selectById(1L)).thenReturn(seal);
            when(empMapper.selectById(10L)).thenReturn(emp);
            when(deptMapper.selectById(20L)).thenReturn(dept);

            // when
            AdmSealVO vo = service.getById(1L);

            // then
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getSealName()).isEqualTo("公章");
            assertThat(vo.getCustodianName()).isEqualTo("张三");
            assertThat(vo.getDeptName()).isEqualTo("行政部");
        }

        @Test
        @DisplayName("印章不存在时抛出 BizException")
        void getById_notFound_throwsException() {
            // given
            when(sealMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("印章不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("保管人或部门为空时, 关联名称为null")
        void getById_nullAssociations() {
            // given
            AdmSeal seal = new AdmSeal();
            seal.setId(2L);
            seal.setSealName("合同章");
            seal.setCustodian(null);
            seal.setDeptId(null);

            when(sealMapper.selectById(2L)).thenReturn(seal);

            // when
            AdmSealVO vo = service.getById(2L);

            // then
            assertThat(vo.getCustodianName()).isNull();
            assertThat(vo.getDeptName()).isNull();
            verify(empMapper, never()).selectById(any());
            verify(deptMapper, never()).selectById(any());
        }
    }
}
