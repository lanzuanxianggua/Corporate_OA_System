package cn.oa.service.impl;

import cn.oa.entity.OaAsset;
import cn.oa.entity.OaAssetBorrow;
import cn.oa.mapper.OaAssetMapper;
import cn.oa.mapper.OaAssetBorrowMapper;
import cn.oa.service.AssetBorrowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AssetBorrowServiceImpl extends ServiceImpl<OaAssetBorrowMapper, OaAssetBorrow> implements AssetBorrowService {

    @Autowired
    private OaAssetMapper assetMapper;

    @Override
    @Transactional
    public void borrowAsset(OaAssetBorrow borrow) {
        OaAsset asset = assetMapper.selectById(borrow.getAssetId());
        if (asset == null) {
            throw new RuntimeException("资产不存在");
        }
        if (asset.getStatus() != '0') {
            throw new RuntimeException("资产当前不可借出");
        }
        borrow.setStatus('0');
        borrow.setBorrowTime(LocalDateTime.now());
        this.save(borrow);

        asset.setStatus('1');
        asset.setCurrentUserId(borrow.getBorrowerId());
        assetMapper.updateById(asset);
    }

    @Override
    @Transactional
    public void returnAsset(Long borrowId) {
        OaAssetBorrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new RuntimeException("借出记录不存在");
        }
        borrow.setActualReturn(LocalDateTime.now());
        borrow.setStatus('1');
        this.updateById(borrow);

        OaAsset asset = assetMapper.selectById(borrow.getAssetId());
        if (asset != null) {
            asset.setStatus('0');
            asset.setCurrentUserId(null);
            assetMapper.updateById(asset);
        }
    }

    @Override
    public IPage<OaAssetBorrow> pageList(int pageNum, int pageSize, Long borrowerId, Character status) {
        Page<OaAssetBorrow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaAssetBorrow> wrapper = new LambdaQueryWrapper<>();
        if (borrowerId != null) {
            wrapper.eq(OaAssetBorrow::getBorrowerId, borrowerId);
        }
        if (status != null) {
            wrapper.eq(OaAssetBorrow::getStatus, status);
        }
        wrapper.orderByDesc(OaAssetBorrow::getCreateTime);
        return this.page(page, wrapper);
    }
}
