package cn.oa.admin.service.impl;

import cn.oa.admin.dto.AdmSealUsageCreateDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.entity.AdmSealUsage;
import cn.oa.admin.mapper.AdmSealMapper;
import cn.oa.admin.mapper.AdmSealUsageMapper;
import cn.oa.admin.service.AdmSealService;
import cn.oa.admin.vo.AdmSealUsageVO;
import cn.oa.admin.vo.AdmSealVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 印章管理服务实现
 *
 * @author oa-admin
 */
@Service
public class AdmSealServiceImpl implements AdmSealService {

    @Autowired
    private AdmSealMapper admSealMapper;

    @Autowired
    private AdmSealUsageMapper admSealUsageMapper;

    // ============ 印章基础管理 ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeal(AdmSeal seal) {
        admSealMapper.insert(seal);
        return seal.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSeal(AdmSeal seal) {
        AdmSeal existing = admSealMapper.selectById(seal.getId());
        if (existing == null) {
            throw new BusinessException("印章不存在");
        }
        admSealMapper.updateById(seal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeal(Long id) {
        AdmSeal existing = admSealMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("印章不存在");
        }
        admSealMapper.deleteById(id);
    }

    @Override
    public IPage<AdmSealVO> pageSeals(String keyword, String status, Integer pageNum, Integer pageSize) {
        Page<AdmSeal> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdmSeal> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AdmSeal::getSealName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AdmSeal::getStatus, status);
        }
        wrapper.orderByDesc(AdmSeal::getCreateTime);

        IPage<AdmSeal> entityPage = admSealMapper.selectPage(page, wrapper);

        // 转换为 VO
        IPage<AdmSealVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toSealVO).toList());
        return voPage;
    }

    @Override
    public AdmSealVO getSealDetail(Long id) {
        AdmSeal entity = admSealMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("印章不存在");
        }
        return toSealVO(entity);
    }

    // ============ 印章使用申请 ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUsage(AdmSealUsageCreateDTO dto, Long applicantId) {
        // 校验印章是否存在且可用
        AdmSeal seal = admSealMapper.selectById(dto.getSealId());
        if (seal == null) {
            throw new BusinessException("印章不存在");
        }
        if (!"NORMAL".equals(seal.getStatus())) {
            throw new BusinessException("印章当前状态不允许使用");
        }

        AdmSealUsage usage = new AdmSealUsage();
        BeanUtils.copyProperties(dto, usage);
        usage.setApplicantId(applicantId);
        usage.setStatus("PENDING");
        admSealUsageMapper.insert(usage);
        return usage.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveUsage(Long usageId) {
        AdmSealUsage usage = admSealUsageMapper.selectById(usageId);
        if (usage == null) {
            throw new BusinessException("使用记录不存在");
        }
        if (!"PENDING".equals(usage.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }
        usage.setStatus("APPROVED");
        admSealUsageMapper.updateById(usage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectUsage(Long usageId, String reason) {
        AdmSealUsage usage = admSealUsageMapper.selectById(usageId);
        if (usage == null) {
            throw new BusinessException("使用记录不存在");
        }
        if (!"PENDING".equals(usage.getStatus())) {
            throw new BusinessException("当前状态不允许驳回");
        }
        usage.setStatus("REJECTED");
        admSealUsageMapper.updateById(usage);
    }

    @Override
    public IPage<AdmSealUsageVO> pageUsages(Long sealId, String status, Integer pageNum, Integer pageSize) {
        Page<AdmSealUsage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdmSealUsage> wrapper = new LambdaQueryWrapper<>();
        if (sealId != null) {
            wrapper.eq(AdmSealUsage::getSealId, sealId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AdmSealUsage::getStatus, status);
        }
        wrapper.orderByDesc(AdmSealUsage::getCreateTime);

        IPage<AdmSealUsage> entityPage = admSealUsageMapper.selectPage(page, wrapper);

        IPage<AdmSealUsageVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toUsageVO).toList());
        return voPage;
    }

    // ============ 内部转换方法 ============

    private AdmSealVO toSealVO(AdmSeal entity) {
        AdmSealVO vo = new AdmSealVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AdmSealUsageVO toUsageVO(AdmSealUsage entity) {
        AdmSealUsageVO vo = new AdmSealUsageVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
