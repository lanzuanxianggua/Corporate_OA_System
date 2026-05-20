package cn.oa.service;

import cn.oa.entity.OaOuting;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OutingService extends IService<OaOuting> {

    void submit(OaOuting outing);

    void approve(Long applyId, Long approverId, Integer status, String remark);

    IPage<OaOuting> pageList(int pageNum, int pageSize, Long empId, Integer status);
}
