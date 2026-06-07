package cn.oa.admin.service;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.dto.AdmSealApplyCreateDTO;
import cn.oa.admin.dto.AdmSealApplyQueryDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.entity.AdmSealApply;
import cn.oa.admin.event.AdmBusinessSubmittedEvent;
import cn.oa.admin.mapper.AdmSealApplyMapper;
import cn.oa.admin.mapper.AdmSealMapper;
import cn.oa.admin.vo.AdmSealApplyVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.workflow.service.WfInstanceService;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdmSealApplyService 单元测试.
 *
 * <p>覆盖 create / submit / use / archive / delete / getById / listPage 7 个核心场景.
 */
@ExtendWith(MockitoExtension.class)
class AdmSealApplyServiceTest {

    @Mock private AdmSealApplyMapper mapper;
    @Mock private AdmSealMapper sealMapper;
    @Mock private SysEmpMapper empMapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<AdmSealApply> applyCaptor;
    @Captor private ArgumentCaptor<AdmBusinessSubmittedEvent> eventCaptor;

    private AdmSealApplyService service;

    private static final Long EMP_ID = 1L;
    private static final Long OTHER_EMP_ID = 2L;
    private static final Long SEAL_ID = 100L;
    private static final Long APPLY_ID = 1000L;
    private static final Long WF_INSTANCE_ID = 9999L;

    @BeforeEach
    void setUp() {
        service = new AdmSealApplyService(mapper, sealMapper, empMapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 创建印章申请")
    class Create {

        @Test
        @DisplayName("印章 ACTIVE — 创建成功, 状态 DRAFT, 自动生成 applyNo")
        void create_success() {
            // given
            AdmSeal seal = new AdmSeal();
            seal.setId(SEAL_ID);
            seal.setSealName("公司公章");
            seal.setStatus(AdmConstants.SEAL_STATUS_ACTIVE);
            when(sealMapper.selectById(SEAL_ID)).thenReturn(seal);

            org.mockito.Mockito.doAnswer(inv -> {
                AdmSealApply a = inv.getArgument(0);
                a.setId(APPLY_ID);
                return 1;
            }).when(mapper).insert(any(AdmSealApply.class));

            AdmSealApplyCreateDTO dto = new AdmSealApplyCreateDTO();
            dto.setSealId(SEAL_ID);
            dto.setPurpose("合同盖章");
            dto.setDocName("销售合同.pdf");
            dto.setDocCount(3);
            dto.setExpectDate(LocalDate.of(2026, 6, 10));

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(APPLY_ID);
            verify(mapper, times(1)).insert(applyCaptor.capture());
            AdmSealApply saved = applyCaptor.getValue();
            assertThat(saved.getSealId()).isEqualTo(SEAL_ID);
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getPurpose()).isEqualTo("合同盖章");
            assertThat(saved.getDocName()).isEqualTo("销售合同.pdf");
            assertThat(saved.getDocCount()).isEqualTo(3);
            assertThat(saved.getExpectDate()).isEqualTo(LocalDate.of(2026, 6, 10));
            assertThat(saved.getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            assertThat(saved.getApplyNo()).startsWith("SEAL");
        }

        @Test
        @DisplayName("印章不存在 — 抛 BizException(NOT_FOUND)")
        void create_sealNotFound() {
            // given
            when(sealMapper.selectById(999L)).thenReturn(null);
            AdmSealApplyCreateDTO dto = new AdmSealApplyCreateDTO();
            dto.setSealId(999L);
            dto.setPurpose("test");
            dto.setDocName("x.pdf");
            dto.setDocCount(1);

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("印章不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).insert(any(AdmSealApply.class));
        }

        @Test
        @DisplayName("印章非 ACTIVE — 抛 BizException(BAD_REQUEST)")
        void create_sealInactive() {
            // given
            AdmSeal seal = new AdmSeal();
            seal.setId(SEAL_ID);
            seal.setStatus(AdmConstants.SEAL_STATUS_INACTIVE);
            when(sealMapper.selectById(SEAL_ID)).thenReturn(seal);

            AdmSealApplyCreateDTO dto = new AdmSealApplyCreateDTO();
            dto.setSealId(SEAL_ID);
            dto.setPurpose("test");
            dto.setDocName("x.pdf");
            dto.setDocCount(1);

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("印章非 ACTIVE 状态");

            verify(mapper, never()).insert(any(AdmSealApply.class));
        }
    }

    @Nested
    @DisplayName("submit() 提交审批")
    class Submit {

        @Test
        @DisplayName("DRAFT + 自己的申请 — 提交成功, 启动 workflow, 发布事件")
        void submit_success() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setApplyNo("SEAL123");
            apply.setEmpId(EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);
            when(wfInstanceService.start(eq(AdmConstants.WF_DEF_SEAL_APPLY),
                    eq(AdmConstants.BIZ_KEY_PREFIX_SEAL + APPLY_ID), eq(EMP_ID)))
                    .thenReturn(WF_INSTANCE_ID);

            // when
            Long wfId = service.submit(APPLY_ID, EMP_ID);

            // then
            assertThat(wfId).isEqualTo(WF_INSTANCE_ID);
            verify(wfInstanceService).start(AdmConstants.WF_DEF_SEAL_APPLY,
                    AdmConstants.BIZ_KEY_PREFIX_SEAL + APPLY_ID, EMP_ID);
            verify(mapper, times(2)).updateById(applyCaptor.capture());
            AdmSealApply finalApply = applyCaptor.getAllValues().get(1);
            assertThat(finalApply.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            AdmBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo(AdmConstants.BIZ_KEY_PREFIX_SEAL);
            assertThat(ev.getBusinessId()).isEqualTo(APPLY_ID);
            assertThat(ev.getWfInstanceId()).isEqualTo(WF_INSTANCE_ID);
        }

        @Test
        @DisplayName("非 DRAFT 状态 — 抛 BizException(BAD_REQUEST)")
        void submit_invalidStatus() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setEmpId(EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when & then
            assertThatThrownBy(() -> service.submit(APPLY_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 DRAFT 状态可提交");

            verify(wfInstanceService, never()).start(any(), any(), any());
        }

        @Test
        @DisplayName("非自己的申请 — 抛 BizException(FORBIDDEN)")
        void submit_notOwner() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setEmpId(OTHER_EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when & then
            assertThatThrownBy(() -> service.submit(APPLY_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能提交自己的印章申请")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.FORBIDDEN.getCode()));
        }
    }

    @Nested
    @DisplayName("use() / archive() 用印归档")
    class UseAndArchive {

        @Test
        @DisplayName("APPROVED → use — 状态 USED + 设 useDate")
        void use_success() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            service.use(APPLY_ID);

            // then
            verify(mapper).updateById(applyCaptor.capture());
            AdmSealApply used = applyCaptor.getValue();
            assertThat(used.getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_USED);
            assertThat(used.getUseDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("非 APPROVED 状态 — 抛 BizException")
        void use_invalidStatus() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when & then
            assertThatThrownBy(() -> service.use(APPLY_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 APPROVED 状态可用印");
        }

        @Test
        @DisplayName("USED → archive — 状态 ARCHIVED")
        void archive_success() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_USED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            service.archive(APPLY_ID);

            // then
            verify(mapper).updateById(applyCaptor.capture());
            assertThat(applyCaptor.getValue().getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_ARCHIVED);
        }
    }

    @Nested
    @DisplayName("delete() / getById() / listPage()")
    class QueryAndDelete {

        @Test
        @DisplayName("DRAFT + 自己 — delete 成功")
        void delete_success() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setEmpId(EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when
            service.delete(APPLY_ID, EMP_ID);

            // then
            verify(mapper).deleteById(APPLY_ID);
        }

        @Test
        @DisplayName("已 PENDING 状态 — delete 抛异常")
        void delete_pendingThrows() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setEmpId(EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            // when & then
            assertThatThrownBy(() -> service.delete(APPLY_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 DRAFT 状态可删除");
        }

        @Test
        @DisplayName("getById — 关联 sealName + empName")
        void getById_withAssociations() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setApplyNo("SEAL123");
            apply.setSealId(SEAL_ID);
            apply.setEmpId(EMP_ID);
            apply.setPurpose("盖章");
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
            when(mapper.selectById(APPLY_ID)).thenReturn(apply);

            AdmSeal seal = new AdmSeal();
            seal.setId(SEAL_ID);
            seal.setSealName("公司公章");
            when(sealMapper.selectById(SEAL_ID)).thenReturn(seal);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("张三");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            // when
            AdmSealApplyVO vo = service.getById(APPLY_ID);

            // then
            assertThat(vo.getId()).isEqualTo(APPLY_ID);
            assertThat(vo.getApplyNo()).isEqualTo("SEAL123");
            assertThat(vo.getSealName()).isEqualTo("公司公章");
            assertThat(vo.getEmpName()).isEqualTo("张三");
            assertThat(vo.getStatus()).isEqualTo(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
        }

        @Test
        @DisplayName("getById — 申请不存在抛 NOT_FOUND")
        void getById_notFound() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("印章申请不存在");
        }

        @Test
        @DisplayName("listPage — empId 强过滤 + 状态条件 + 日期范围")
        void listPage() {
            // given
            AdmSealApply apply = new AdmSealApply();
            apply.setId(APPLY_ID);
            apply.setSealId(SEAL_ID);
            apply.setEmpId(EMP_ID);
            apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            apply.setExpectDate(LocalDate.of(2026, 6, 10));

            Page<AdmSealApply> page = new Page<>(1, 10);
            page.setRecords(java.util.List.of(apply));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            AdmSeal seal = new AdmSeal();
            seal.setId(SEAL_ID);
            seal.setSealName("公司公章");
            when(sealMapper.selectById(SEAL_ID)).thenReturn(seal);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("张三");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            AdmSealApplyQueryDTO query = new AdmSealApplyQueryDTO();
            query.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
            query.setStartDate(LocalDate.of(2026, 6, 1));
            query.setEndDate(LocalDate.of(2026, 6, 30));
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<AdmSealApplyVO> result = service.listPage(query, EMP_ID);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getSealName()).isEqualTo("公司公章");
            assertThat(result.getList().get(0).getEmpName()).isEqualTo("张三");
        }
    }

}
