package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaBusinessTrip;
import cn.oa.mapper.OaBusinessTripMapper;
import cn.oa.service.AttendanceService;
import cn.oa.service.BusinessTripService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class BusinessTripServiceImpl extends BaseApprovalServiceImpl<OaBusinessTripMapper, OaBusinessTrip>
        implements BusinessTripService {

    @Lazy
    @Autowired
    private AttendanceService attendanceService;

    public BusinessTripServiceImpl() {
        this.empIdGetter = OaBusinessTrip::getEmpId;
        this.statusGetter = OaBusinessTrip::getStatus;
        this.createTimeGetter = OaBusinessTrip::getCreateTime;
        this.idGetter = OaBusinessTrip::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.TRIP;
    }

    @Override
    protected void setStatus(OaBusinessTrip entity, Integer status) {
        entity.setStatus(status);
    }

    @Override
    protected void setEmpName(OaBusinessTrip entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaBusinessTrip entity, String remark) {
        entity.setRemark(remark);
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaBusinessTrip entity) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                entity.getStartTime().toLocalDate(), entity.getEndTime().toLocalDate()) + 1;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("days", days);
        return ctx;
    }

    @Override
    protected void onUpdateStatus(OaBusinessTrip entity, Integer newStatus, Integer oldStatus) {
        if (entity.getStartTime() == null || entity.getEndTime() == null) return;

        LocalDate startDate = entity.getStartTime().toLocalDate();
        LocalDate endDate = entity.getEndTime().toLocalDate();

        if (newStatus == 1 && !Integer.valueOf(1).equals(oldStatus)) {
            attendanceService.markTripAttendance(entity.getEmpId(), startDate, endDate);
        }

        if ((newStatus == 2 || newStatus == 3) && Integer.valueOf(1).equals(oldStatus)) {
            attendanceService.removeMarkedAttendance(entity.getEmpId(), startDate, endDate, 6);
        }
    }

    @Override
    @Transactional
    public void submit(OaBusinessTrip trip) {
        if (trip.getStartTime() == null || trip.getEndTime() == null) {
            throw new BusinessException("出差起止时间不能为空");
        }
        doSubmit(trip);
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
    public IPage<OaBusinessTrip> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
