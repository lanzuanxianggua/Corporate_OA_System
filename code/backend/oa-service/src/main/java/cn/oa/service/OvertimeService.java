package cn.oa.service;

import cn.oa.entity.OaOvertime;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OvertimeService extends IService<OaOvertime> {

    void submit(OaOvertime overtime);

    void approve(Long overtimeId, Long approverId, Integer status, String remark);

    IPage<OaOvertime> pageList(int pageNum, int pageSize, Long empId, Integer status);

    void updateStatus(Long id, Integer status);
}
