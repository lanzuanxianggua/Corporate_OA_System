package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.constant.LeaveType;
import cn.oa.common.exception.BusinessException;
import cn.oa.common.utils.LeaveDurationUtil;
import cn.oa.entity.OaLeaveApply;
import cn.oa.mapper.OaLeaveApplyMapper;
import cn.oa.service.AttendanceService;
import cn.oa.service.LeaveApplyService;
import cn.oa.service.LeaveBalanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class LeaveApplyServiceImpl extends BaseApprovalServiceImpl<OaLeaveApplyMapper, OaLeaveApply>
        implements LeaveApplyService {

    @Lazy
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Lazy
    @Autowired
    private AttendanceService attendanceService;

    public LeaveApplyServiceImpl() {
        this.empIdGetter = OaLeaveApply::getEmpId;
        this.statusGetter = OaLeaveApply::getStatus;
        this.createTimeGetter = OaLeaveApply::getCreateTime;
        this.idGetter = OaLeaveApply::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.LEAVE;
    }

    @Override
    protected void setStatus(OaLeaveApply entity, Integer status) {
        entity.setStatus(status);
    }

    @Override
    protected void setEmpName(OaLeaveApply entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaLeaveApply entity, String remark) {
        entity.setRemark(remark);
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaLeaveApply entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("days", calculateLeaveDays(entity));
        return ctx;
    }

    @Override
    protected void onUpdateStatus(OaLeaveApply entity, Integer newStatus, Integer oldStatus) {
        Integer leaveType = parseLeaveType(entity.getLeaveType());
        if (leaveType == null || entity.getEmpId() == null) {
            return;
        }
        if (entity.getStartTime() == null || entity.getEndTime() == null) {
            return;
        }

        BigDecimal days = calculateLeaveDays(entity);
        LocalDate startDate = entity.getStartTime().toLocalDate();
        LocalDate endDate = entity.getEndTime().toLocalDate();
        int year = startDate.getYear();

        if (newStatus == 1 && !Integer.valueOf(1).equals(oldStatus)) {
            leaveBalanceService.deductBalance(entity.getEmpId(), leaveType, year, days);
            attendanceService.markLeaveAttendance(entity.getEmpId(), startDate, endDate);
        }

        if ((newStatus == 2 || newStatus == 3) && Integer.valueOf(1).equals(oldStatus)) {
            leaveBalanceService.restoreBalance(entity.getEmpId(), leaveType, year, days);
            attendanceService.removeMarkedAttendance(entity.getEmpId(), startDate, endDate, 5);
        }
    }

    private Integer parseLeaveType(String leaveType) {
        if (leaveType == null || !leaveType.matches("\\d+")) {
            return null;
        }
        return Integer.valueOf(leaveType);
    }

    private BigDecimal calculateLeaveDays(OaLeaveApply apply) {
        return LeaveDurationUtil.calculateLeaveDays(apply.getStartTime(), apply.getEndTime(), apply.getLeavePeriod());
    }

    @Override
    @Transactional
    public void submit(OaLeaveApply apply) {
        if (apply.getStartTime() == null || apply.getEndTime() == null) {
            throw new BusinessException("\u8bf7\u5047\u8d77\u6b62\u65f6\u95f4\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (apply.getStartTime().isAfter(apply.getEndTime())) {
            throw new BusinessException("\u8bf7\u5047\u7ed3\u675f\u65f6\u95f4\u4e0d\u80fd\u65e9\u4e8e\u5f00\u59cb\u65f6\u95f4");
        }
        apply.setDays(calculateLeaveDays(apply));
        Integer leaveType = parseLeaveType(apply.getLeaveType());
        if (leaveType != null && (LeaveType.ANNUAL == leaveType || LeaveType.COMPENSATORY == leaveType)) {
            leaveBalanceService.assertSufficientBalance(
                    apply.getEmpId(),
                    leaveType,
                    apply.getStartTime().toLocalDate().getYear(),
                    apply.getDays());
        }
        doSubmit(apply);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        doApprove(applyId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        doApprove(applyId, approverId, status, remark, taskId);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        doUpdateStatus(id, status);
    }

    @Override
    public IPage<OaLeaveApply> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
