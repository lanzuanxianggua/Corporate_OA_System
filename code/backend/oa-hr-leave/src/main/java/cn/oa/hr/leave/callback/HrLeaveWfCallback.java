package cn.oa.hr.leave.callback;

import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.hr.leave.service.HrLeaveBalanceService;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import cn.oa.workflow.mapper.WfInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 请假流程回调.
 *
 * <p>当工作流状态变更时 (审批通过/拒绝), 更新 hr_leave 状态并操作余额.
 * <p>由审批流程在任务完成时显式调用.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrLeaveWfCallback {

    private final HrLeaveMapper leaveMapper;
    private final HrLeaveBalanceService balanceService;
    private final WfInstanceMapper instanceMapper;

    /**
     * 处理流程结束回调.
     *
     * @param wfInstanceId 流程实例 ID
     */
    public void onWorkflowFinished(Long wfInstanceId) {
        WfInstance instance = instanceMapper.selectById(wfInstanceId);
        if (instance == null) {
            throw new BizException(RCode.NOT_FOUND, "流程实例不存在: " + wfInstanceId);
        }

        String businessKey = instance.getBusinessKey();
        if (businessKey == null || !businessKey.startsWith("LEAVE_")) {
            log.warn("非请假业务流程, 跳过: businessKey={}", businessKey);
            return;
        }

        Long leaveId = Long.parseLong(businessKey.substring("LEAVE_".length()));
        HrLeave leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            log.warn("请假单不存在: leaveId={}", leaveId);
            return;
        }

        String wfStatus = instance.getStatus();
        int year = leave.getStartDate().getYear();

        if ("APPROVED".equals(wfStatus)) {
            // 审批通过: 更新请假单状态 + 扣减余额
            leave.setStatus("APPROVED");
            leaveMapper.updateById(leave);
            balanceService.deductOnApprove(leave.getEmpId(), leave.getLeaveType(), year, leave.getTotalDays());
            log.info("请假审批通过: leaveId={}, empId={}, days={}", leaveId, leave.getEmpId(), leave.getTotalDays());

        } else if ("REJECTED".equals(wfStatus)) {
            // 审批拒绝: 更新请假单状态 + 解冻余额
            leave.setStatus("REJECTED");
            leaveMapper.updateById(leave);
            balanceService.unfreezeOnReject(leave.getEmpId(), leave.getLeaveType(), year, leave.getTotalDays());
            log.info("请假审批拒绝: leaveId={}, empId={}", leaveId, leave.getEmpId());
        }
    }

    /**
     * 工作流事件监听器.
     *
     * <p>当工作流引擎发布 WfInstanceCompletedEvent 时触发, 委托给 onWorkflowFinished 处理.
     */
    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        onWorkflowFinished(event.getInstanceId());
    }
}
