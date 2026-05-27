package cn.oa.service.impl;

import cn.oa.entity.OaAsset;
import cn.oa.mapper.OaAssetMapper;
import cn.oa.service.AssetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AssetServiceImpl extends ServiceImpl<OaAssetMapper, OaAsset> implements AssetService {

    @Override
    public IPage<OaAsset> pageList(int pageNum, int pageSize, String category, Character status) {
        Page<OaAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaAsset> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(OaAsset::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(OaAsset::getStatus, status);
        }
        wrapper.orderByDesc(OaAsset::getCreateTime);
        return this.page(page, wrapper);
    }
}
