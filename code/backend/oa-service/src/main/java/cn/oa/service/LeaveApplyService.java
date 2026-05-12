package cn.oa.service;

import cn.oa.entity.OaLeaveApply;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LeaveApplyService extends IService<OaLeaveApply> {

    /**
     * 提交请假申请
     */
    void submit(OaLeaveApply apply);

    /**
     * 审批请假申请
     */
    void approve(Long applyId, Long approverId, Integer status, String remark);

    /**
     * 分页查询请假申请
     */
    IPage<OaLeaveApply> pageList(int pageNum, int pageSize, Long empId, Integer status);
}
