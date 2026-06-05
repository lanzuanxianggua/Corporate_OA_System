package cn.oa.admin.service;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.vo.AdmAssetVO;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdmAssetService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class AdmAssetServiceTest {

    @Mock
    private AdmAssetMapper assetMapper;

    @Mock
    private SysEmpMapper empMapper;

    @Mock
    private SysDeptMapper deptMapper;

    @Captor
    private ArgumentCaptor<AdmAsset> assetCaptor;

    private AdmAssetService service;

    @BeforeEach
    void setUp() {
        service = new AdmAssetService(assetMapper, empMapper, deptMapper);
    }

    @Nested
    @DisplayName("create() 新增资产")
    class Create {

        @Test
        @DisplayName("创建成功 — 自动生成资产编号, 默认状态 IDLE")
        void create_success() {
            // given
            AdmAssetCreateDTO dto = new AdmAssetCreateDTO();
            dto.setAssetName("ThinkPad X1 Carbon");
            dto.setCategory("IT");
            dto.setBrand("联想");
            dto.setModel("X1C Gen13");
            dto.setPurchaseDate(LocalDate.of(2026, 6, 1));
            dto.setPurchasePrice(new BigDecimal("15999.00"));
            dto.setDeptId(10L);
            dto.setCustodian(1L);
            dto.setLocation("A栋5楼-501");

            when(assetMapper.insert(any(AdmAsset.class))).thenAnswer(invocation -> {
                AdmAsset a = invocation.getArgument(0);
                a.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(100L);
            verify(assetMapper, times(1)).insert(assetCaptor.capture());

            AdmAsset saved = assetCaptor.getValue();
            assertThat(saved.getAssetName()).isEqualTo("ThinkPad X1 Carbon");
            assertThat(saved.getCategory()).isEqualTo("IT");
            assertThat(saved.getBrand()).isEqualTo("联想");
            assertThat(saved.getModel()).isEqualTo("X1C Gen13");
            assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(saved.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("15999.00"));
            assertThat(saved.getDeptId()).isEqualTo(10L);
            assertThat(saved.getCustodian()).isEqualTo(1L);
            assertThat(saved.getLocation()).isEqualTo("A栋5楼-501");
            assertThat(saved.getStatus()).isEqualTo("IDLE");
        }

        @Test
        @DisplayName("自动资产编号格式为 AST + yyyyMMdd + 4位序列")
        void create_generatesAssetCode() {
            // given
            AdmAssetCreateDTO dto = new AdmAssetCreateDTO();
            dto.setAssetName("显示器");
            dto.setCategory("IT");
            dto.setDeptId(10L);
            dto.setCustodian(1L);

            when(assetMapper.insert(any(AdmAsset.class))).thenAnswer(invocation -> {
                AdmAsset a = invocation.getArgument(0);
                a.setId(101L);
                return 1;
            });

            // when
            service.create(dto);

            // then
            verify(assetMapper).insert(assetCaptor.capture());
            AdmAsset saved = assetCaptor.getValue();
            String assetCode = saved.getAssetCode();
            assertThat(assetCode).startsWith("AST");
            assertThat(assetCode).matches("AST\\d{8}\\d{4}");
        }
    }

    @Nested
    @DisplayName("update() 修改资产")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            AdmAsset exist = new AdmAsset();
            exist.setId(1L);
            exist.setAssetCode("AST202606010001");
            exist.setAssetName("旧显示器");

            AdmAssetCreateDTO dto = new AdmAssetCreateDTO();
            dto.setAssetName("新显示器");
            dto.setCategory("IT");
            dto.setBrand("戴尔");
            dto.setModel("U2723QE");
            dto.setPurchaseDate(LocalDate.of(2026, 7, 1));
            dto.setPurchasePrice(new BigDecimal("3999.00"));
            dto.setDeptId(20L);
            dto.setCustodian(2L);
            dto.setLocation("B栋3楼");

            when(assetMapper.selectById(1L)).thenReturn(exist);

            // when
            service.update(1L, dto);

            // then
            verify(assetMapper, times(1)).selectById(1L);
            verify(assetMapper, times(1)).updateById(assetCaptor.capture());

            AdmAsset patch = assetCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getAssetName()).isEqualTo("新显示器");
            assertThat(patch.getCategory()).isEqualTo("IT");
            assertThat(patch.getBrand()).isEqualTo("戴尔");
            assertThat(patch.getModel()).isEqualTo("U2723QE");
            assertThat(patch.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(patch.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("3999.00"));
            assertThat(patch.getDeptId()).isEqualTo(20L);
            assertThat(patch.getCustodian()).isEqualTo(2L);
            assertThat(patch.getLocation()).isEqualTo("B栋3楼");
        }

        @Test
        @DisplayName("资产不存在时抛出 BizException")
        void update_notFound_throwsException() {
            // given
            when(assetMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.update(999L, new AdmAssetCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessage("资产不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(assetMapper, never()).updateById(any(AdmAsset.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除资产")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // given
            AdmAsset exist = new AdmAsset();
            exist.setId(1L);

            when(assetMapper.selectById(1L)).thenReturn(exist);

            // when
            service.delete(1L);

            // then
            verify(assetMapper, times(1)).selectById(1L);
            verify(assetMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("资产不存在时抛出 BizException")
        void delete_notFound_throwsException() {
            // given
            when(assetMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("资产不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(assetMapper, never()).deleteById(any(Long.class));
        }
    }

    @Nested
    @DisplayName("getById() 资产详情")
    class GetById {

        @Test
        @DisplayName("查询详情成功 — 关联保管人姓名和部门名称")
        void getById_success() {
            // given
            AdmAsset asset = new AdmAsset();
            asset.setId(1L);
            asset.setAssetName("ThinkPad X1 Carbon");
            asset.setAssetCode("AST202606010001");
            asset.setCustodian(10L);
            asset.setDeptId(20L);

            SysEmp emp = new SysEmp();
            emp.setId(10L);
            emp.setRealName("李四");

            SysDept dept = new SysDept();
            dept.setId(20L);
            dept.setDeptName("技术部");

            when(assetMapper.selectById(1L)).thenReturn(asset);
            when(empMapper.selectById(10L)).thenReturn(emp);
            when(deptMapper.selectById(20L)).thenReturn(dept);

            // when
            AdmAssetVO vo = service.getById(1L);

            // then
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getAssetName()).isEqualTo("ThinkPad X1 Carbon");
            assertThat(vo.getAssetCode()).isEqualTo("AST202606010001");
            assertThat(vo.getCustodianName()).isEqualTo("李四");
            assertThat(vo.getDeptName()).isEqualTo("技术部");
        }

        @Test
        @DisplayName("资产不存在时抛出 BizException")
        void getById_notFound_throwsException() {
            // given
            when(assetMapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("资产不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("保管人或部门为空时, 关联名称为null")
        void getById_nullAssociations() {
            // given
            AdmAsset asset = new AdmAsset();
            asset.setId(2L);
            asset.setAssetName("共享打印机");
            asset.setAssetCode("AST202606010002");
            asset.setCustodian(null);
            asset.setDeptId(null);

            when(assetMapper.selectById(2L)).thenReturn(asset);

            // when
            AdmAssetVO vo = service.getById(2L);

            // then
            assertThat(vo.getCustodianName()).isNull();
            assertThat(vo.getDeptName()).isNull();
            verify(empMapper, never()).selectById(any());
            verify(deptMapper, never()).selectById(any());
        }
    }
}
