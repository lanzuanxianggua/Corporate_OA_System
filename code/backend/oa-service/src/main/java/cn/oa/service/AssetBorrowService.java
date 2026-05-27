package cn.oa.service;

import cn.oa.entity.OaAssetBorrow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AssetBorrowService extends IService<OaAssetBorrow> {

    void borrowAsset(OaAssetBorrow borrow);

    void returnAsset(Long borrowId);

    IPage<OaAssetBorrow> pageList(int pageNum, int pageSize, Long borrowerId, String status);
}
