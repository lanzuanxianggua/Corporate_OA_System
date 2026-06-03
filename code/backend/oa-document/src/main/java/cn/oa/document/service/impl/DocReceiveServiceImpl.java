package cn.oa.document.service.impl;

import cn.oa.document.dto.DocReceiveApproveDTO;
import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveHandleDTO;
import cn.oa.document.dto.DocReceiveProposeDTO;
import cn.oa.document.entity.DocReceive;
import cn.oa.document.mapper.DocReceiveMapper;
import cn.oa.document.service.DocReceiveService;
import cn.oa.document.vo.DocReceiveVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 收文管理服务实现
 *
 * @author oa-document
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocReceiveServiceImpl extends ServiceImpl<DocReceiveMapper, DocReceive> implements DocReceiveService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(DocReceiveCreateDTO dto, String creator) {
        DocReceive receive = new DocReceive();
        receive.setFromOrg(dto.getFromOrg());
        receive.setOriginalSerial(dto.getOriginalSerial());
        receive.setReceiveDate(dto.getReceiveDate() != null ? dto.getReceiveDate() : LocalDate.now());
        receive.setTitle(dto.getTitle());
        receive.setSecurityLevel(dto.getSecurityLevel() != null ? dto.getSecurityLevel() : "NORMAL");
        receive.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : "NORMAL");
        receive.setCopyCount(dto.getCopyCount() != null ? dto.getCopyCount() : 1);
        receive.setAttachmentId(dto.getAttachmentId());
        receive.setStatus("RECEIVED");
        baseMapper.insert(receive);
        log.info("收文已登记: id={}, title={}", receive.getId(), dto.getTitle());
        return receive.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void propose(DocReceiveProposeDTO dto) {
        DocReceive receive = baseMapper.selectById(dto.getId());
        if (receive == null) {
            throw new BusinessException("收文不存在");
        }
        if (!"RECEIVED".equals(receive.getStatus())) {
            throw new BusinessException("仅已登记的收文可拟办");
        }
        receive.setProposedOpinion(dto.getProposedOpinion());
        receive.setStatus("PROPOSED");
        baseMapper.updateById(receive);
        log.info("收文已拟办: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(DocReceiveApproveDTO dto) {
        DocReceive receive = baseMapper.selectById(dto.getId());
        if (receive == null) {
            throw new BusinessException("收文不存在");
        }
        if (!"PROPOSED".equals(receive.getStatus())) {
            throw new BusinessException("仅拟办状态的收文可批办");
        }
        receive.setApprovedOpinion(dto.getApprovedOpinion());
        receive.setApproverId(dto.getHandlerId());
        receive.setHandlerId(dto.getHandlerId());
        receive.setStatus("APPROVED");
        baseMapper.updateById(receive);
        log.info("收文已批办: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(DocReceiveHandleDTO dto) {
        DocReceive receive = baseMapper.selectById(dto.getId());
        if (receive == null) {
            throw new BusinessException("收文不存在");
        }
        if (!"APPROVED".equals(receive.getStatus())) {
            throw new BusinessException("仅批办状态的收文可承办");
        }
        receive.setHandledOpinion(dto.getHandledOpinion());
        receive.setStatus("HANDLING");
        baseMapper.updateById(receive);
        log.info("收文已承办: id={}", dto.getId());
    }

    @Override
    public IPage<DocReceiveVO> pageReceive(int pageNum, int pageSize, String keyword, String status) {
        Page<DocReceive> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DocReceive> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DocReceive::getTitle, keyword).or()
                    .like(DocReceive::getFromOrg, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DocReceive::getStatus, status);
        }
        wrapper.orderByDesc(DocReceive::getCreateTime);
        IPage<DocReceive> entityPage = baseMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public DocReceiveVO getReceiveDetail(Long id) {
        DocReceive receive = baseMapper.selectById(id);
        if (receive == null) {
            throw new BusinessException("收文不存在");
        }
        return toVO(receive);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceive(Long id) {
        DocReceive receive = baseMapper.selectById(id);
        if (receive == null) {
            throw new BusinessException("收文不存在");
        }
        baseMapper.deleteById(id);
        log.info("收文已删除: id={}", id);
    }

    /**
     * Entity 转 VO
     */
    private DocReceiveVO toVO(DocReceive entity) {
        if (entity == null) {
            return null;
        }
        DocReceiveVO vo = new DocReceiveVO();
        vo.setId(entity.getId());
        vo.setFromOrg(entity.getFromOrg());
        vo.setOriginalSerial(entity.getOriginalSerial());
        vo.setReceiveDate(entity.getReceiveDate());
        vo.setTitle(entity.getTitle());
        vo.setSecurityLevel(entity.getSecurityLevel());
        vo.setSecurityLevelName(getSecurityLevelName(entity.getSecurityLevel()));
        vo.setUrgency(entity.getUrgency());
        vo.setUrgencyName(getUrgencyName(entity.getUrgency()));
        vo.setCopyCount(entity.getCopyCount());
        vo.setProposedOpinion(entity.getProposedOpinion());
        vo.setApproverId(entity.getApproverId());
        vo.setApprovedOpinion(entity.getApprovedOpinion());
        vo.setHandlerId(entity.getHandlerId());
        vo.setHandledOpinion(entity.getHandledOpinion());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusName(entity.getStatus()));
        vo.setAttachmentId(entity.getAttachmentId());
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
            case "RECEIVED" -> "已登记";
            case "PROPOSED" -> "已拟办";
            case "APPROVED" -> "已批办";
            case "HANDLING" -> "承办中";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }
}
