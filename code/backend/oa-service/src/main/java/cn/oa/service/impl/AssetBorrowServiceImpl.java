package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaAsset;
import cn.oa.entity.OaAssetBorrow;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaAssetMapper;
import cn.oa.mapper.OaAssetBorrowMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.AssetBorrowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssetBorrowServiceImpl extends ServiceImpl<OaAssetBorrowMapper, OaAssetBorrow> implements AssetBorrowService {

    @Autowired
    private OaAssetMapper assetMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void borrowAsset(OaAssetBorrow borrow) {
        OaAsset asset = assetMapper.selectById(borrow.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"0".equals(asset.getStatus())) {
            throw new BusinessException("资产当前不可借出");
        }
        long activeBorrowCount = this.count(new LambdaQueryWrapper<OaAssetBorrow>()
                .eq(OaAssetBorrow::getAssetId, borrow.getAssetId())
                .eq(OaAssetBorrow::getStatus, "0"));
        if (activeBorrowCount > 0) {
            throw new BusinessException("该资产已有未归还的借出记录");
        }
        borrow.setStatus("0");
        borrow.setBorrowTime(LocalDateTime.now());
        this.save(borrow);

        asset.setStatus("1");
        asset.setCurrentUserId(borrow.getBorrowerId());
        assetMapper.updateById(asset);
    }

    @Override
    @Transactional
    public void returnAsset(Long borrowId) {
        OaAssetBorrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new BusinessException("借出记录不存在");
        }
        borrow.setActualReturn(LocalDateTime.now());
        borrow.setStatus("1");
        this.updateById(borrow);

        OaAsset asset = assetMapper.selectById(borrow.getAssetId());
        if (asset != null) {
            asset.setStatus("0");
            asset.setCurrentUserId(null);
            assetMapper.updateById(asset);
        }
    }

    @Override
    public IPage<OaAssetBorrow> pageList(int pageNum, int pageSize, Long borrowerId, String status) {
        Page<OaAssetBorrow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaAssetBorrow> wrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null) {
            wrapper.eq(OaAssetBorrow::getBorrowerId, borrowerId);
        }
        if (status != null) {
            wrapper.eq(OaAssetBorrow::getStatus, status);
        }
        wrapper.orderByDesc(OaAssetBorrow::getCreateTime);
        IPage<OaAssetBorrow> result = this.page(page, wrapper);

        if (result.getRecords().isEmpty()) return result;

        // Batch fill asset name
        Set<Long> assetIds = result.getRecords().stream()
                .map(OaAssetBorrow::getAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> assetNameMap = Map.of();
        if (!assetIds.isEmpty()) {
            List<OaAsset> assets = assetMapper.selectBatchIds(assetIds);
            assetNameMap = assets.stream()
                    .collect(Collectors.toMap(OaAsset::getId, OaAsset::getAssetName, (a, b) -> a));
        }

        // Batch fill borrower name
        Set<Long> borrowerIds = result.getRecords().stream()
                .map(OaAssetBorrow::getBorrowerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> borrowerNameMap = Map.of();
        if (!borrowerIds.isEmpty()) {
            List<SysEmployee> emps = employeeMapper.selectBatchIds(borrowerIds);
            borrowerNameMap = emps.stream()
                    .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));
        }

        for (OaAssetBorrow borrow : result.getRecords()) {
            if (borrow.getAssetId() != null) {
                borrow.setAssetName(assetNameMap.getOrDefault(borrow.getAssetId(), ""));
            }
            if (borrow.getBorrowerId() != null) {
                borrow.setBorrower(borrowerNameMap.getOrDefault(borrow.getBorrowerId(), ""));
            }
        }
        return result;
    }
}
