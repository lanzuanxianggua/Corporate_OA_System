package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaOvertime;
import cn.oa.mapper.OaOvertimeMapper;
import cn.oa.service.LeaveBalanceService;
import cn.oa.service.OvertimeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class OvertimeServiceImpl extends BaseApprovalServiceImpl<OaOvertimeMapper, OaOvertime>
        implements OvertimeService {

    @Lazy
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    public OvertimeServiceImpl() {
        this.empIdGetter = OaOvertime::getEmpId;
        // Note: OaOvertime.status is String, so statusGetter won't work directly with Integer filter.
        // We override doPageList to handle this.
        this.createTimeGetter = OaOvertime::getCreateTime;
        this.idGetter = OaOvertime::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.OVERTIME;
    }

    @Override
    protected void setStatus(OaOvertime entity, Integer status) {
        entity.setStatus(String.valueOf(status));
    }

    @Override
    protected void setEmpName(OaOvertime entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaOvertime entity, String remark) {
        // OaOvertime has no remark transient field — no-op
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaOvertime entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("hours", entity.getHours().doubleValue());
        return ctx;
    }

    @Override
    protected void onUpdateStatus(OaOvertime entity, Integer newStatus, Integer oldStatus) {
        if (entity.getHours() == null || entity.getOvertimeDate() == null) return;

        BigDecimal hours = entity.getHours();
        BigDecimal days = hours.divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP);
        int year = entity.getOvertimeDate().getYear();

        String oldStr = String.valueOf(oldStatus);

        if (newStatus == 1 && !"1".equals(oldStr)) {
            leaveBalanceService.addCompensatoryBalance(entity.getEmpId(), year, days);
        }

        if ((newStatus == 2 || newStatus == 3) && "1".equals(oldStr)) {
            leaveBalanceService.addCompensatoryBalance(entity.getEmpId(), year, days.negate());
        }
    }

    /**
     * Override fillRemarks — OaOvertime has no remark field.
     */
    @Override
    protected void fillRemarks(java.util.List<OaOvertime> records) {
        // no-op
    }

    /**
     * Override pageList to handle String status properly.
     */
    @Override
    public IPage<OaOvertime> doPageList(int pageNum, int pageSize, Long empId, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OaOvertime> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OaOvertime> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaOvertime::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaOvertime::getStatus, String.valueOf(status));
        }
        if (createTimeGetter != null) {
            wrapper.orderByDesc(createTimeGetter);
        }
        IPage<OaOvertime> result = this.page(page, wrapper);
        fillEmpNames(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void submit(OaOvertime overtime) {
        if (overtime.getHours() == null) {
            throw new BusinessException("加班时长不能为空");
        }
        doSubmit(overtime);
    }

    @Override
    @Transactional
    public void approve(Long overtimeId, Long approverId, Integer status, String remark) {
        doApprove(overtimeId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void approve(Long overtimeId, Long approverId, Integer status, String remark, Long taskId) {
        doApprove(overtimeId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        doUpdateStatus(id, status);
    }

    @Override
    public IPage<OaOvertime> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
