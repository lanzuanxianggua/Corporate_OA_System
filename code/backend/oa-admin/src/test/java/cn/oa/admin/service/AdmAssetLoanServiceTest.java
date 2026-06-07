package cn.oa.admin.service;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.dto.AdmAssetLoanCreateDTO;
import cn.oa.admin.dto.AdmAssetLoanQueryDTO;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.entity.AdmAssetLoan;
import cn.oa.admin.event.AdmBusinessSubmittedEvent;
import cn.oa.admin.mapper.AdmAssetLoanMapper;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.vo.AdmAssetLoanVO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdmAssetLoanService 单元测试.
 *
 * <p>覆盖 create / submit / returnAsset / delete / getById / listPage 6 个核心场景.
 */
@ExtendWith(MockitoExtension.class)
class AdmAssetLoanServiceTest {

    @Mock private AdmAssetLoanMapper mapper;
    @Mock private AdmAssetMapper assetMapper;
    @Mock private SysEmpMapper empMapper;
    @Mock private WfInstanceService wfInstanceService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Captor private ArgumentCaptor<AdmAssetLoan> loanCaptor;
    @Captor private ArgumentCaptor<AdmAsset> assetCaptor;
    @Captor private ArgumentCaptor<AdmBusinessSubmittedEvent> eventCaptor;

    private AdmAssetLoanService service;

    private static final Long EMP_ID = 1L;
    private static final Long OTHER_EMP_ID = 2L;
    private static final Long ASSET_ID = 50L;
    private static final Long LOAN_ID = 2000L;
    private static final Long WF_INSTANCE_ID = 8888L;

    @BeforeEach
    void setUp() {
        service = new AdmAssetLoanService(mapper, assetMapper, empMapper, wfInstanceService, eventPublisher);
    }

    @Nested
    @DisplayName("create() 创建领用单")
    class Create {

        @Test
        @DisplayName("BORROW + 资产 IDLE — 创建成功, loanNo 以 BRW 开头")
        void create_borrow_success() {
            // given
            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setAssetCode("AST2026060001");
            asset.setStatus(AdmConstants.ASSET_STATUS_IDLE);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            org.mockito.Mockito.doAnswer(inv -> {
                AdmAssetLoan l = inv.getArgument(0);
                l.setId(LOAN_ID);
                return 1;
            }).when(mapper).insert(any(AdmAssetLoan.class));

            AdmAssetLoanCreateDTO dto = new AdmAssetLoanCreateDTO();
            dto.setAssetId(ASSET_ID);
            dto.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            dto.setExpectReturnDate(LocalDate.of(2026, 7, 1));
            dto.setPurpose("外出办公");

            // when
            Long id = service.create(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(LOAN_ID);
            verify(mapper, times(1)).insert(loanCaptor.capture());
            AdmAssetLoan saved = loanCaptor.getValue();
            assertThat(saved.getAssetId()).isEqualTo(ASSET_ID);
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getLoanType()).isEqualTo("BORROW");
            assertThat(saved.getPurpose()).isEqualTo("外出办公");
            assertThat(saved.getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            assertThat(saved.getLoanNo()).startsWith("BRW");
            assertThat(saved.getLoanDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("BORROW + 资产非 IDLE — 抛 BizException(BAD_REQUEST)")
        void create_borrow_assetInUse() {
            // given
            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_IN_USE);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            AdmAssetLoanCreateDTO dto = new AdmAssetLoanCreateDTO();
            dto.setAssetId(ASSET_ID);
            dto.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            dto.setPurpose("test");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("资产非 IDLE 状态, 不可领用");

            verify(mapper, never()).insert(any(AdmAssetLoan.class));
        }

        @Test
        @DisplayName("SCRAP + 资产已报废 — 抛 BizException(BAD_REQUEST)")
        void create_scrap_alreadyScrapped() {
            // given
            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_SCRAPPED);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            AdmAssetLoanCreateDTO dto = new AdmAssetLoanCreateDTO();
            dto.setAssetId(ASSET_ID);
            dto.setLoanType(AdmAssetLoanService.LOAN_TYPE_SCRAP);
            dto.setPurpose("test");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("资产已报废");
        }

        @Test
        @DisplayName("资产不存在 — 抛 BizException(NOT_FOUND)")
        void create_assetNotFound() {
            // given
            when(assetMapper.selectById(999L)).thenReturn(null);
            AdmAssetLoanCreateDTO dto = new AdmAssetLoanCreateDTO();
            dto.setAssetId(999L);
            dto.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            dto.setPurpose("test");

            // when & then
            assertThatThrownBy(() -> service.create(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("资产不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("submit() 提交审批")
    class Submit {

        @Test
        @DisplayName("DRAFT + 自己 — 提交成功, 启动 workflow, 发布 ASSET_ 事件")
        void submit_success() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setLoanNo("BRW123");
            loan.setEmpId(EMP_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);
            when(wfInstanceService.start(eq(AdmConstants.WF_DEF_ASSET_LOAN),
                    eq(AdmConstants.BIZ_KEY_PREFIX_ASSET + LOAN_ID), eq(EMP_ID)))
                    .thenReturn(WF_INSTANCE_ID);

            // when
            Long wfId = service.submit(LOAN_ID, EMP_ID);

            // then
            assertThat(wfId).isEqualTo(WF_INSTANCE_ID);
            verify(wfInstanceService).start(AdmConstants.WF_DEF_ASSET_LOAN,
                    AdmConstants.BIZ_KEY_PREFIX_ASSET + LOAN_ID, EMP_ID);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            AdmBusinessSubmittedEvent ev = eventCaptor.getValue();
            assertThat(ev.getBusinessPrefix()).isEqualTo(AdmConstants.BIZ_KEY_PREFIX_ASSET);
            assertThat(ev.getBusinessId()).isEqualTo(LOAN_ID);
        }

        @Test
        @DisplayName("非自己 — 抛 BizException(FORBIDDEN)")
        void submit_notOwner() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setEmpId(OTHER_EMP_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.submit(LOAN_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("只能提交自己的领用单")
                    .satisfies(e -> assertThat(((BizException) e).getCode())
                            .isEqualTo(RCode.FORBIDDEN.getCode()));
        }
    }

    @Nested
    @DisplayName("returnAsset() 资产归还")
    class ReturnAsset {

        @Test
        @DisplayName("APPROVED + BORROW — 归还成功, 资产变 IDLE")
        void returnAsset_success() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setAssetId(ASSET_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setStatus(AdmConstants.ASSET_STATUS_IN_USE);
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            // when
            service.returnAsset(LOAN_ID);

            // then
            verify(mapper).updateById(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_LOAN_STATUS_RETURNED);
            assertThat(loanCaptor.getValue().getActualReturnDate()).isEqualTo(LocalDate.now());

            verify(assetMapper).updateById(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AdmConstants.ASSET_STATUS_IDLE);
        }

        @Test
        @DisplayName("非 APPROVED 状态 — 抛 BizException")
        void returnAsset_invalidStatus() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.returnAsset(LOAN_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 APPROVED 状态可归还");
        }

        @Test
        @DisplayName("非 BORROW 类型 — 抛 BizException")
        void returnAsset_wrongType() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_SCRAP);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.returnAsset(LOAN_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 BORROW 类型可走归还流程");
        }
    }

    @Nested
    @DisplayName("delete() / getById() / listPage()")
    class QueryAndDelete {

        @Test
        @DisplayName("DRAFT + 自己 — delete 成功")
        void delete_success() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setEmpId(EMP_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when
            service.delete(LOAN_ID, EMP_ID);

            // then
            verify(mapper).deleteById(LOAN_ID);
        }

        @Test
        @DisplayName("PENDING 状态 — delete 抛 BizException")
        void delete_pendingThrows() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setEmpId(EMP_ID);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            // when & then
            assertThatThrownBy(() -> service.delete(LOAN_ID, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("仅 DRAFT 状态可删除");
        }

        @Test
        @DisplayName("getById — 关联 assetCode/assetName + empName")
        void getById_withAssociations() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setLoanNo("BRW123");
            loan.setAssetId(ASSET_ID);
            loan.setEmpId(EMP_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
            when(mapper.selectById(LOAN_ID)).thenReturn(loan);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setAssetCode("AST2026060001");
            asset.setAssetName("ThinkPad");
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("李四");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            // when
            AdmAssetLoanVO vo = service.getById(LOAN_ID);

            // then
            assertThat(vo.getId()).isEqualTo(LOAN_ID);
            assertThat(vo.getAssetCode()).isEqualTo("AST2026060001");
            assertThat(vo.getAssetName()).isEqualTo("ThinkPad");
            assertThat(vo.getEmpName()).isEqualTo("李四");
            assertThat(vo.getLoanType()).isEqualTo("BORROW");
        }

        @Test
        @DisplayName("listPage — empId 强过滤 + 多条件")
        void listPage() {
            // given
            AdmAssetLoan loan = new AdmAssetLoan();
            loan.setId(LOAN_ID);
            loan.setAssetId(ASSET_ID);
            loan.setEmpId(EMP_ID);
            loan.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);

            Page<AdmAssetLoan> page = new Page<>(1, 10);
            page.setRecords(List.of(loan));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            AdmAsset asset = new AdmAsset();
            asset.setId(ASSET_ID);
            asset.setAssetCode("AST2026060001");
            asset.setAssetName("ThinkPad");
            when(assetMapper.selectById(ASSET_ID)).thenReturn(asset);

            SysEmp emp = new SysEmp();
            emp.setId(EMP_ID);
            emp.setRealName("李四");
            when(empMapper.selectById(EMP_ID)).thenReturn(emp);

            AdmAssetLoanQueryDTO query = new AdmAssetLoanQueryDTO();
            query.setLoanType(AdmAssetLoanService.LOAN_TYPE_BORROW);
            query.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
            query.setPageNum(1);
            query.setPageSize(10);

            // when
            PageResult<AdmAssetLoanVO> result = service.listPage(query, EMP_ID);

            // then
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getAssetName()).isEqualTo("ThinkPad");
            assertThat(result.getList().get(0).getLoanType()).isEqualTo("BORROW");
        }
    }

}
