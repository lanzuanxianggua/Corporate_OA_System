package cn.oa.document.service.impl;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.utils.WebUtil;
import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.dto.DocDispatchUpdateDTO;
import cn.oa.document.entity.DocDispatch;
import cn.oa.document.entity.DocSerial;
import cn.oa.document.mapper.DocDispatchMapper;
import cn.oa.document.service.DocDispatchService;
import cn.oa.document.service.DocSerialService;
import cn.oa.document.vo.DocDispatchVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 发文管理服务实现
 *
 * @author oa-document
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocDispatchServiceImpl extends ServiceImpl<DocDispatchMapper, DocDispatch> implements DocDispatchService {

    private final DocSerialService docSerialService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDispatch(DocDispatchCreateDTO dto, String creator) {
        DocDispatch dispatch = new DocDispatch();
        dispatch.setTitle(dto.getTitle());
        dispatch.setSecurityLevel(Optional.ofNullable(dto.getSecurityLevel()).orElse("NORMAL"));
        dispatch.setUrgency(Optional.ofNullable(dto.getUrgency()).orElse("NORMAL"));
        dispatch.setIssuingOrg(dto.getIssuingOrg());
        dispatch.setMainRecipient(dto.getMainRecipient());
        dispatch.setCcRecipient(dto.getCcRecipient());
        dispatch.setContentLink(dto.getContentLink());
        dispatch.setStatus("DRAFT");
        dispatch.setDraftDate(LocalDate.now());
        baseMapper.insert(dispatch);
        log.info("发文已创建: id={}, title={}", dispatch.getId(), dto.getTitle());
        return dispatch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDispatch(DocDispatchUpdateDTO dto) {
        DocDispatch dispatch = baseMapper.selectById(dto.getId());
        if (dispatch == null) {
            throw new BusinessException("发文不存在");
        }
        if (!"DRAFT".equals(dispatch.getStatus())) {
            throw new BusinessException("仅草稿状态可修改");
        }
        if (dto.getSerialNo() != null) {
            dispatch.setSerialNo(dto.getSerialNo());
        }
        if (dto.getTitle() != null) {
            dispatch.setTitle(dto.getTitle());
        }
        if (dto.getSecurityLevel() != null) {
            dispatch.setSecurityLevel(dto.getSecurityLevel());
        }
        if (dto.getUrgency() != null) {
            dispatch.setUrgency(dto.getUrgency());
        }
        if (dto.getIssuingOrg() != null) {
            dispatch.setIssuingOrg(dto.getIssuingOrg());
        }
        if (dto.getMainRecipient() != null) {
            dispatch.setMainRecipient(dto.getMainRecipient());
        }
        if (dto.getCcRecipient() != null) {
            dispatch.setCcRecipient(dto.getCcRecipient());
        }
        if (dto.getReviewerId() != null) {
            dispatch.setReviewerId(dto.getReviewerId());
        }
        if (dto.getSignerId() != null) {
            dispatch.setSignerId(dto.getSignerId());
        }
        if (dto.getContentLink() != null) {
            dispatch.setContentLink(dto.getContentLink());
        }
        baseMapper.updateById(dispatch);
        log.info("发文已更新: id={}", dispatch.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDispatch(Long id) {
        DocDispatch dispatch = baseMapper.selectById(id);
        if (dispatch == null) {
            throw new BusinessException("发文不存在");
        }
        baseMapper.deleteById(id);
        log.info("发文已删除: id={}", id);
    }

    @Override
    public IPage<DocDispatchVO> pageDispatch(int pageNum, int pageSize, DocDispatchQueryDTO query) {
        Page<DocDispatch> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DocDispatch> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                wrapper.like(DocDispatch::getTitle, query.getKeyword());
            }
            if (query.getStatus() != null && !query.getStatus().isEmpty()) {
                wrapper.eq(DocDispatch::getStatus, query.getStatus());
            }
            if (query.getSecurityLevel() != null && !query.getSecurityLevel().isEmpty()) {
                wrapper.eq(DocDispatch::getSecurityLevel, query.getSecurityLevel());
            }
            if (query.getStartDate() != null) {
                wrapper.ge(DocDispatch::getDraftDate, query.getStartDate());
            }
            if (query.getEndDate() != null) {
                wrapper.le(DocDispatch::getDraftDate, query.getEndDate());
            }
        }
        wrapper.orderByDesc(DocDispatch::getCreateTime);
        IPage<DocDispatch> entityPage = baseMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public DocDispatchVO getDispatchDetail(Long id) {
        DocDispatch dispatch = baseMapper.selectById(id);
        if (dispatch == null) {
            throw new BusinessException("发文不存在");
        }
        return toVO(dispatch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToWorkflow(Long id) {
        DocDispatch dispatch = baseMapper.selectById(id);
        if (dispatch == null) {
            throw new BusinessException("发文不存在");
        }
        if (!"DRAFT".equals(dispatch.getStatus())) {
            throw new BusinessException("仅草稿状态的发文可提交");
        }
        // 更新状态为核稿中
        dispatch.setStatus("REVIEWING");
        baseMapper.updateById(dispatch);
        log.info("发文已提交工作流: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String lockSerial(String orgCode, Integer year, Long lockBy) {
        return docSerialService.lockSerial(orgCode, year, lockBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSerial(Long id) {
        docSerialService.releaseSerial(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useSerial(Long id, Long dispatchId) {
        docSerialService.useSerial(id, dispatchId);
    }

    /**
     * Entity 转 VO
     */
    private DocDispatchVO toVO(DocDispatch entity) {
        if (entity == null) {
            return null;
        }
        DocDispatchVO vo = new DocDispatchVO();
        vo.setId(entity.getId());
        vo.setSerialNo(entity.getSerialNo());
        vo.setTitle(entity.getTitle());
        vo.setSecurityLevel(entity.getSecurityLevel());
        vo.setSecurityLevelName(getSecurityLevelName(entity.getSecurityLevel()));
        vo.setUrgency(entity.getUrgency());
        vo.setUrgencyName(getUrgencyName(entity.getUrgency()));
        vo.setIssuingOrg(entity.getIssuingOrg());
        vo.setMainRecipient(entity.getMainRecipient());
        vo.setCcRecipient(entity.getCcRecipient());
        vo.setDrafterId(entity.getDrafterId());
        vo.setDraftDate(entity.getDraftDate());
        vo.setReviewerId(entity.getReviewerId());
        vo.setSignerId(entity.getSignerId());
        vo.setContentLink(entity.getContentLink());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        vo.setDispatchDate(entity.getDispatchDate());
        vo.setProcessInstanceId(entity.getProcessInstanceId());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String getSecurityLevelName(String level) {
        if (level == null) return "普通";
        return switch (level) {
            case "SECRET" -> "秘密";
            case "CONFIDENTIAL" -> "机密";
            default -> "普通";
        };
    }

    private String getUrgencyName(String urgency) {
        if (urgency == null) return "普通";
        return switch (urgency) {
            case "URGENT" -> "紧急";
            case "IMMEDIATE" -> "特急";
            default -> "普通";
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "REVIEWING" -> "核稿中";
            case "SIGNING" -> "待签发";
            case "DISPATCHED" -> "已签发";
            case "REJECTED" -> "已退回";
            default -> status;
        };
    }
}
