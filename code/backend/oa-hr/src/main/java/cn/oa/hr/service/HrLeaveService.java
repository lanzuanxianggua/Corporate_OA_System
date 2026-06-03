package cn.oa.hr.service;

import cn.oa.hr.dto.HrLeaveCreateDTO;
import cn.oa.hr.dto.HrLeaveQueryDTO;
import cn.oa.hr.entity.HrLeaveApply;
import cn.oa.hr.vo.HrLeaveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HR请假服务接口
 *
 * @author oa-hr
 */
public interface HrLeaveService {

    /**
     * 创建并提交请假申请
     *
     * @param dto   请假创建DTO
     * @param empId 申请员工ID
     * @param deptId 申请员工部门ID
     * @return 申请ID
     */
    Long createAndSubmit(HrLeaveCreateDTO dto, Long empId, Long deptId);

    /**
     * 计算请假天数
     *
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param leavePeriod 请假时段(FULL/AM/PM)
     * @return 请假天数
     */
    BigDecimal calculateLeaveDays(LocalDateTime startTime, LocalDateTime endTime, String leavePeriod);

    /**
     * 撤回请假申请
     *
     * @param id     申请ID
     * @param empId  当前用户ID
     * @param isAdmin 是否管理员
     */
    void revoke(Long id, Long empId, boolean isAdmin);

    /**
     * 驳回后重新提交
     *
     * @param id    申请ID
     * @param dto   请假创建DTO
     * @param empId 申请员工ID
     */
    void resubmit(Long id, HrLeaveCreateDTO dto, Long empId);

    /**
     * 分页查询请假申请
     *
     * @param query 查询条件
     * @param empId 当前用户ID
     * @param isAdmin 是否管理员
     * @return 分页结果
     */
    IPage<HrLeaveVO> pageQuery(HrLeaveQueryDTO query, Long empId, boolean isAdmin);

    /**
     * 查询请假详情
     *
     * @param id 申请ID
     * @return 请假VO
     */
    HrLeaveVO getDetail(Long id);

    /**
     * 工作流审批通过回调
     *
     * @param id          申请ID
     * @param approvedTime 审批通过时间
     */
    void onWorkflowApproved(Long id, LocalDateTime approvedTime);

    /**
     * 工作流审批驳回回调
     *
     * @param id           申请ID
     * @param rejectReason 驳回原因
     */
    void onWorkflowRejected(Long id, String rejectReason);

    /**
     * 工作流撤回回调
     *
     * @param id 申请ID
     */
    void onWorkflowWithdrawn(Long id);
}
