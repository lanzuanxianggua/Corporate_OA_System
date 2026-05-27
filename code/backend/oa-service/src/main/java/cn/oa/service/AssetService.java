package cn.oa.service;

import cn.oa.entity.OaAsset;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AssetService extends IService<OaAsset> {

    IPage<OaAsset> pageList(int pageNum, int pageSize, String category, Character status);
}
