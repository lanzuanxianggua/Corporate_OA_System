package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaOvertime;
import cn.oa.entity.WfTask;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaOvertimeMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.LeaveBalanceService;
import cn.oa.service.OvertimeService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OvertimeServiceImpl extends ServiceImpl<OaOvertimeMapper, OaOvertime> implements OvertimeService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Lazy
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Override
    public void submit(OaOvertime overtime) {
        overtime.setStatus("0");
        this.save(overtime);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("hours", overtime.getHours().doubleValue());
        workflowService.startProcess(BusinessType.OVERTIME, overtime.getId(), overtime.getEmpId(), ctx);
    }

    @Override
    @Transactional
    public void approve(Long overtimeId, Long approverId, Integer status, String remark) {
        WfTask task = workflowService.findPendingTask(BusinessType.OVERTIME, overtimeId, approverId);
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        OaOvertime overtime = this.getById(id);
        if (overtime == null) return;

        String oldStatus = overtime.getStatus();
        overtime.setStatus(String.valueOf(status));
        this.updateById(overtime);

        // Convert hours to days: hours / 8, rounded to nearest 0.5
        BigDecimal hours = overtime.getHours();
        BigDecimal days = hours.divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP);
        int year = overtime.getOvertimeDate().getYear();

        // When approved (status=1): add compensatory leave balance (leaveType=5)
        if (status == 1 && !"1".equals(oldStatus)) {
            leaveBalanceService.addCompensatoryBalance(overtime.getEmpId(), year, days);
        }

        // When rejected(2) or withdrawn(4) after being approved(1): reverse the compensatory leave
        if ((status == 2 || status == 4) && "1".equals(oldStatus)) {
            leaveBalanceService.deductBalance(overtime.getEmpId(), 5, year, days);
        }
    }

    @Override
    public IPage<OaOvertime> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaOvertime> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaOvertime> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaOvertime::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaOvertime::getStatus, status);
        }
        wrapper.orderByDesc(OaOvertime::getCreateTime);
        IPage<OaOvertime> result = this.page(page, wrapper);
        fillEmpNames(result.getRecords());
        return result;
    }

    private void fillEmpNames(List<OaOvertime> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaOvertime::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaOvertime record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }
}
