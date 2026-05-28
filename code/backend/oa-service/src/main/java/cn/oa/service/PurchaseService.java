package cn.oa.service;

import cn.oa.entity.OaPurchase;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PurchaseService extends IService<OaPurchase> {

    void submit(OaPurchase purchase);

    void approve(Long applyId, Long approverId, Integer status, String remark);

    void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId);

    IPage<OaPurchase> pageList(int pageNum, int pageSize, Long empId, Integer status);

    void updateStatus(Long id, Integer status);
}
