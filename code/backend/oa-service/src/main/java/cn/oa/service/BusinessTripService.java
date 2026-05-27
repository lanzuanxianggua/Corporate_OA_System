package cn.oa.service;

import cn.oa.entity.OaBusinessTrip;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface BusinessTripService extends IService<OaBusinessTrip> {

    void submit(OaBusinessTrip trip);

    void approve(Long applyId, Long approverId, Integer status, String remark);

    IPage<OaBusinessTrip> pageList(int pageNum, int pageSize, Long empId, Integer status);

    void updateStatus(Long id, Integer status);
}
