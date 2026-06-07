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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资产领用 Service.
 *
 * <p>业务主路径: 领用人创建 DRAFT → 提交 (启动工作流) → 审批通过 → 资产变 IN_USE → 归还 → 资产变 IDLE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmAssetLoanService {

    private final AdmAssetLoanMapper mapper;
    private final AdmAssetMapper assetMapper;
    private final SysEmpMapper empMapper;
    private final WfInstanceService wfInstanceService;
    private final ApplicationEventPublisher eventPublisher;

    /** 领用类型: BORROW. */
    public static final String LOAN_TYPE_BORROW = "BORROW";
    /** 领用类型: RETURN. */
    public static final String LOAN_TYPE_RETURN = "RETURN";
    /** 领用类型: SCRAP. */
    public static final String LOAN_TYPE_SCRAP = "SCRAP";

    /**
     * 创建领用单 (DRAFT).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdmAssetLoanCreateDTO dto, Long empId) {
        AdmAsset asset = assetMapper.selectById(dto.getAssetId());
        if (asset == null) {
            throw new BizException(RCode.NOT_FOUND, "资产不存在: " + dto.getAssetId());
        }
        if (LOAN_TYPE_BORROW.equals(dto.getLoanType())
                && !AdmConstants.ASSET_STATUS_IDLE.equals(asset.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "资产非 IDLE 状态, 不可领用: " + asset.getStatus());
        }
        if (LOAN_TYPE_SCRAP.equals(dto.getLoanType())
                && AdmConstants.ASSET_STATUS_SCRAPPED.equals(asset.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "资产已报废, 不可重复报废");
        }

        AdmAssetLoan loan = new AdmAssetLoan();
        loan.setLoanNo(generateLoanNo(dto.getLoanType()));
        loan.setAssetId(dto.getAssetId());
        loan.setEmpId(empId);
        loan.setLoanType(dto.getLoanType());
        loan.setLoanDate(LocalDate.now());
        loan.setExpectReturnDate(dto.getExpectReturnDate());
        loan.setPurpose(dto.getPurpose());
        loan.setRemark(dto.getRemark());
        loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_DRAFT);
        mapper.insert(loan);
        log.info("资产领用单已创建: id={}, loanNo={}, type={}, empId={}",
                loan.getId(), loan.getLoanNo(), dto.getLoanType(), empId);
        return loan.getId();
    }

    /**
     * 提交领用单 (DRAFT -> PENDING + 启动工作流).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long id, Long empId) {
        AdmAssetLoan loan = mapper.selectById(id);
        if (loan == null) {
            throw new BizException(RCode.NOT_FOUND, "资产领用单不存在: " + id);
        }
        if (!AdmConstants.ASSET_LOAN_STATUS_DRAFT.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 DRAFT 状态可提交, 当前状态: " + loan.getStatus());
        }
        if (!Objects.equals(loan.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能提交自己的领用单");
        }

        loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_PENDING);
        mapper.updateById(loan);

        String businessKey = AdmConstants.BIZ_KEY_PREFIX_ASSET + id;
        Long wfInstanceId = wfInstanceService.start(AdmConstants.WF_DEF_ASSET_LOAN, businessKey, empId);
        loan.setWfInstanceId(wfInstanceId);
        mapper.updateById(loan);

        eventPublisher.publishEvent(new AdmBusinessSubmittedEvent(
                AdmConstants.BIZ_KEY_PREFIX_ASSET, id, loan.getLoanNo(), empId, wfInstanceId));

        log.info("资产领用单已提交: id={}, empId={}, wfInstanceId={}", id, empId, wfInstanceId);
        return wfInstanceId;
    }

    /**
     * 资产归还 (APPROVED 借用单 -> RETURNED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(Long id) {
        AdmAssetLoan loan = mapper.selectById(id);
        if (loan == null) {
            throw new BizException(RCode.NOT_FOUND, "资产领用单不存在: " + id);
        }
        if (!AdmConstants.ASSET_LOAN_STATUS_APPROVED.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 APPROVED 状态可归还, 当前状态: " + loan.getStatus());
        }
        if (!LOAN_TYPE_BORROW.equals(loan.getLoanType())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 BORROW 类型可走归还流程");
        }
        loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_RETURNED);
        loan.setActualReturnDate(LocalDate.now());
        mapper.updateById(loan);

        // 资产状态回 IDLE
        AdmAsset asset = assetMapper.selectById(loan.getAssetId());
        if (asset != null && AdmConstants.ASSET_STATUS_IN_USE.equals(asset.getStatus())) {
            asset.setStatus(AdmConstants.ASSET_STATUS_IDLE);
            assetMapper.updateById(asset);
        }
        log.info("资产已归还: loanId={}, assetId={}", id, loan.getAssetId());
    }

    /**
     * 删除领用单 (仅 DRAFT).
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long empId) {
        AdmAssetLoan loan = mapper.selectById(id);
        if (loan == null) {
            throw new BizException(RCode.NOT_FOUND, "资产领用单不存在: " + id);
        }
        if (!AdmConstants.ASSET_LOAN_STATUS_DRAFT.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 DRAFT 状态可删除, 当前状态: " + loan.getStatus());
        }
        if (!Objects.equals(loan.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能删除自己的领用单");
        }
        mapper.deleteById(id);
        log.info("资产领用单已删除: id={}", id);
    }

    /**
     * 详情.
     */
    public AdmAssetLoanVO getById(Long id) {
        AdmAssetLoan loan = mapper.selectById(id);
        if (loan == null) {
            throw new BizException(RCode.NOT_FOUND, "资产领用单不存在: " + id);
        }
        return toVO(loan);
    }

    /**
     * 分页查询领用单.
     */
    public PageResult<AdmAssetLoanVO> listPage(AdmAssetLoanQueryDTO query, Long empId) {
        Page<AdmAssetLoan> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AdmAssetLoan> wrapper = new LambdaQueryWrapper<AdmAssetLoan>()
                .eq(query.getAssetId() != null, AdmAssetLoan::getAssetId, query.getAssetId())
                .eq(query.getLoanType() != null, AdmAssetLoan::getLoanType, query.getLoanType())
                .eq(query.getStatus() != null, AdmAssetLoan::getStatus, query.getStatus())
                .eq(AdmAssetLoan::getEmpId, empId)
                .orderByDesc(AdmAssetLoan::getCreateTime);

        Page<AdmAssetLoan> result = mapper.selectPage(page, wrapper);
        List<AdmAssetLoanVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private AdmAssetLoanVO toVO(AdmAssetLoan loan) {
        AdmAssetLoanVO vo = new AdmAssetLoanVO();
        vo.setId(loan.getId());
        vo.setLoanNo(loan.getLoanNo());
        vo.setAssetId(loan.getAssetId());
        vo.setEmpId(loan.getEmpId());
        vo.setLoanType(loan.getLoanType());
        vo.setLoanDate(loan.getLoanDate());
        vo.setExpectReturnDate(loan.getExpectReturnDate());
        vo.setActualReturnDate(loan.getActualReturnDate());
        vo.setPurpose(loan.getPurpose());
        vo.setRemark(loan.getRemark());
        vo.setStatus(loan.getStatus());
        vo.setWfInstanceId(loan.getWfInstanceId());
        vo.setCreateTime(loan.getCreateTime());

        if (loan.getAssetId() != null) {
            AdmAsset asset = assetMapper.selectById(loan.getAssetId());
            if (asset != null) {
                vo.setAssetCode(asset.getAssetCode());
                vo.setAssetName(asset.getAssetName());
            }
        }
        if (loan.getEmpId() != null) {
            SysEmp emp = empMapper.selectById(loan.getEmpId());
            if (emp != null) {
                vo.setEmpName(emp.getRealName());
            }
        }
        return vo;
    }

    private String generateLoanNo(String loanType) {
        String prefix = switch (loanType) {
            case LOAN_TYPE_RETURN -> "RTN";
            case LOAN_TYPE_SCRAP -> "SCR";
            default -> "BRW";
        };
        return prefix + System.currentTimeMillis();
    }
}
