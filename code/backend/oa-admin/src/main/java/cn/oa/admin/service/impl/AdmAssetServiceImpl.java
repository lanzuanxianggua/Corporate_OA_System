package cn.oa.admin.service.impl;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.dto.AdmAssetOperateDTO;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.entity.AdmAssetLog;
import cn.oa.admin.mapper.AdmAssetLogMapper;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.service.AdmAssetService;
import cn.oa.admin.vo.AdmAssetLogVO;
import cn.oa.admin.vo.AdmAssetVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 固定资产管理服务实现
 *
 * @author oa-admin
 */
@Service
public class AdmAssetServiceImpl implements AdmAssetService {

    @Autowired
    private AdmAssetMapper admAssetMapper;

    @Autowired
    private AdmAssetLogMapper admAssetLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAsset(AdmAssetCreateDTO dto) {
        // 校验资产编码唯一性
        LambdaQueryWrapper<AdmAsset> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(AdmAsset::getAssetCode, dto.getAssetCode());
        if (admAssetMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException("资产编码已存在");
        }

        AdmAsset entity = new AdmAsset();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("IDLE");
        admAssetMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAsset(Long id, AdmAssetCreateDTO dto) {
        AdmAsset existing = admAssetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("资产不存在");
        }

        // 校验资产编码唯一性（排除自身）
        LambdaQueryWrapper<AdmAsset> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(AdmAsset::getAssetCode, dto.getAssetCode());
        codeWrapper.ne(AdmAsset::getId, id);
        if (admAssetMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException("资产编码已存在");
        }

        BeanUtils.copyProperties(dto, existing);
        admAssetMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsset(Long id) {
        AdmAsset existing = admAssetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("资产不存在");
        }
        admAssetMapper.deleteById(id);
    }

    @Override
    public IPage<AdmAssetVO> pageAssets(String keyword, String status, String category, Integer pageNum, Integer pageSize) {
        Page<AdmAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdmAsset> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AdmAsset::getAssetName, keyword)
                    .or().like(AdmAsset::getAssetCode, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AdmAsset::getStatus, status);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AdmAsset::getCategory, category);
        }
        wrapper.orderByDesc(AdmAsset::getCreateTime);

        IPage<AdmAsset> entityPage = admAssetMapper.selectPage(page, wrapper);

        IPage<AdmAssetVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toAssetVO).toList());
        return voPage;
    }

    @Override
    public AdmAssetVO getAssetDetail(Long id) {
        AdmAsset entity = admAssetMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("资产不存在");
        }
        return toAssetVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void operateAsset(AdmAssetOperateDTO dto, Long operatorId) {
        AdmAsset asset = admAssetMapper.selectById(dto.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }

        AdmAssetLog logEntry = new AdmAssetLog();
        logEntry.setAssetId(dto.getAssetId());
        logEntry.setOperatorId(operatorId);
        logEntry.setRemark(dto.getRemark());
        logEntry.setCreatedAt(LocalDateTime.now());

        switch (dto.getOperation()) {
            case "ALLOCATE":
                if (dto.getToUserId() == null) {
                    throw new BusinessException("领用时必须指定使用人");
                }
                logEntry.setFromUserId(asset.getCurrentUserId());
                logEntry.setToUserId(dto.getToUserId());
                logEntry.setOperation("ALLOCATE");
                asset.setCurrentUserId(dto.getToUserId());
                asset.setStatus("IN_USE");
                break;

            case "RETURN":
                logEntry.setFromUserId(asset.getCurrentUserId());
                logEntry.setOperation("RETURN");
                asset.setCurrentUserId(null);
                asset.setStatus("IDLE");
                break;

            case "MAINTAIN":
                logEntry.setOperation("MAINTAIN");
                asset.setStatus("MAINTAINING");
                break;

            case "SCRAP":
                logEntry.setOperation("SCRAP");
                asset.setStatus("SCRAPPED");
                break;

            default:
                throw new BusinessException("不支持的操作类型: " + dto.getOperation());
        }

        admAssetMapper.updateById(asset);
        admAssetLogMapper.insert(logEntry);
    }

    @Override
    public IPage<AdmAssetLogVO> pageAssetLogs(Long assetId, Integer pageNum, Integer pageSize) {
        Page<AdmAssetLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdmAssetLog> wrapper = new LambdaQueryWrapper<>();
        if (assetId != null) {
            wrapper.eq(AdmAssetLog::getAssetId, assetId);
        }
        wrapper.orderByDesc(AdmAssetLog::getCreatedAt);

        IPage<AdmAssetLog> entityPage = admAssetLogMapper.selectPage(page, wrapper);

        IPage<AdmAssetLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toAssetLogVO).toList());
        return voPage;
    }

    // ============ 内部转换方法 ============

    private AdmAssetVO toAssetVO(AdmAsset entity) {
        AdmAssetVO vo = new AdmAssetVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AdmAssetLogVO toAssetLogVO(AdmAssetLog entity) {
        AdmAssetLogVO vo = new AdmAssetLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
