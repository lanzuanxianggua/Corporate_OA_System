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
 * 印章申请 Service.
 *
 * <p>业务主路径: 申请人创建 DRAFT → 提交 (启动工作流) → 审批通过 → 用印 → 归档.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdmSealApplyService {

    private final AdmSealApplyMapper mapper;
    private final AdmSealMapper sealMapper;
    private final SysEmpMapper empMapper;
    private final WfInstanceService wfInstanceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建印章申请 (DRAFT).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdmSealApplyCreateDTO dto, Long empId) {
        AdmSeal seal = sealMapper.selectById(dto.getSealId());
        if (seal == null) {
            throw new BizException(RCode.NOT_FOUND, "印章不存在: " + dto.getSealId());
        }
        if (!AdmConstants.SEAL_STATUS_ACTIVE.equals(seal.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "印章非 ACTIVE 状态, 不可申请: " + seal.getStatus());
        }

        AdmSealApply apply = new AdmSealApply();
        apply.setApplyNo(generateApplyNo());
        apply.setSealId(dto.getSealId());
        apply.setEmpId(empId);
        apply.setPurpose(dto.getPurpose());
        apply.setDocName(dto.getDocName());
        apply.setDocCount(dto.getDocCount());
        apply.setExpectDate(dto.getExpectDate());
        apply.setRemark(dto.getRemark());
        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_DRAFT);
        mapper.insert(apply);
        log.info("印章申请已创建: id={}, applyNo={}, empId={}", apply.getId(), apply.getApplyNo(), empId);
        return apply.getId();
    }

    /**
     * 提交印章申请 (DRAFT -> PENDING + 启动工作流).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long id, Long empId) {
        AdmSealApply apply = mapper.selectById(id);
        if (apply == null) {
            throw new BizException(RCode.NOT_FOUND, "印章申请不存在: " + id);
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 DRAFT 状态可提交, 当前状态: " + apply.getStatus());
        }
        if (!Objects.equals(apply.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能提交自己的印章申请");
        }

        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_PENDING);
        mapper.updateById(apply);

        String businessKey = AdmConstants.BIZ_KEY_PREFIX_SEAL + id;
        Long wfInstanceId = wfInstanceService.start(AdmConstants.WF_DEF_SEAL_APPLY, businessKey, empId);
        apply.setWfInstanceId(wfInstanceId);
        mapper.updateById(apply);

        eventPublisher.publishEvent(new AdmBusinessSubmittedEvent(
                AdmConstants.BIZ_KEY_PREFIX_SEAL, id, apply.getApplyNo(), empId, wfInstanceId));

        log.info("印章申请已提交审批: id={}, empId={}, wfInstanceId={}", id, empId, wfInstanceId);
        return wfInstanceId;
    }

    /**
     * 用印 (APPROVED -> USED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void use(Long id) {
        AdmSealApply apply = mapper.selectById(id);
        if (apply == null) {
            throw new BizException(RCode.NOT_FOUND, "印章申请不存在: " + id);
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_APPROVED.equals(apply.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 APPROVED 状态可用印, 当前状态: " + apply.getStatus());
        }
        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_USED);
        apply.setUseDate(LocalDate.now());
        mapper.updateById(apply);
        log.info("印章已用印: id={}", id);
    }

    /**
     * 归档 (USED -> ARCHIVED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        AdmSealApply apply = mapper.selectById(id);
        if (apply == null) {
            throw new BizException(RCode.NOT_FOUND, "印章申请不存在: " + id);
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_USED.equals(apply.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 USED 状态可归档, 当前状态: " + apply.getStatus());
        }
        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_ARCHIVED);
        mapper.updateById(apply);
        log.info("印章申请已归档: id={}", id);
    }

    /**
     * 删除申请 (仅 DRAFT 状态).
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long empId) {
        AdmSealApply apply = mapper.selectById(id);
        if (apply == null) {
            throw new BizException(RCode.NOT_FOUND, "印章申请不存在: " + id);
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 DRAFT 状态可删除, 当前状态: " + apply.getStatus());
        }
        if (!Objects.equals(apply.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能删除自己的印章申请");
        }
        mapper.deleteById(id);
        log.info("印章申请已删除: id={}", id);
    }

    /**
     * 申请详情.
     */
    public AdmSealApplyVO getById(Long id) {
        AdmSealApply apply = mapper.selectById(id);
        if (apply == null) {
            throw new BizException(RCode.NOT_FOUND, "印章申请不存在: " + id);
        }
        return toVO(apply);
    }

    /**
     * 分页查询申请列表.
     */
    public PageResult<AdmSealApplyVO> listPage(AdmSealApplyQueryDTO query, Long empId) {
        Page<AdmSealApply> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AdmSealApply> wrapper = new LambdaQueryWrapper<AdmSealApply>()
                .eq(query.getSealId() != null, AdmSealApply::getSealId, query.getSealId())
                .eq(query.getStatus() != null, AdmSealApply::getStatus, query.getStatus())
                .eq(AdmSealApply::getEmpId, empId)
                .orderByDesc(AdmSealApply::getCreateTime);
        if (query.getStartDate() != null) {
            wrapper.ge(AdmSealApply::getExpectDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(AdmSealApply::getExpectDate, query.getEndDate());
        }

        Page<AdmSealApply> result = mapper.selectPage(page, wrapper);
        List<AdmSealApplyVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private AdmSealApplyVO toVO(AdmSealApply apply) {
        AdmSealApplyVO vo = new AdmSealApplyVO();
        vo.setId(apply.getId());
        vo.setApplyNo(apply.getApplyNo());
        vo.setSealId(apply.getSealId());
        vo.setEmpId(apply.getEmpId());
        vo.setPurpose(apply.getPurpose());
        vo.setDocName(apply.getDocName());
        vo.setDocCount(apply.getDocCount());
        vo.setExpectDate(apply.getExpectDate());
        vo.setUseDate(apply.getUseDate());
        vo.setRemark(apply.getRemark());
        vo.setStatus(apply.getStatus());
        vo.setWfInstanceId(apply.getWfInstanceId());
        vo.setCreateTime(apply.getCreateTime());

        if (apply.getSealId() != null) {
            AdmSeal seal = sealMapper.selectById(apply.getSealId());
            if (seal != null) {
                vo.setSealName(seal.getSealName());
            }
        }
        if (apply.getEmpId() != null) {
            SysEmp emp = empMapper.selectById(apply.getEmpId());
            if (emp != null) {
                vo.setEmpName(emp.getRealName());
            }
        }
        return vo;
    }

    private String generateApplyNo() {
        return "SEAL" + System.currentTimeMillis();
    }
}
