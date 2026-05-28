package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaLeaveApply;
import cn.oa.entity.WfTask;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaLeaveApplyMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.service.AttendanceService;
import cn.oa.service.LeaveApplyService;
import cn.oa.service.LeaveBalanceService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaveApplyServiceImpl extends ServiceImpl<OaLeaveApplyMapper, OaLeaveApply> implements LeaveApplyService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WfTaskMapper wfTaskMapper;

    @Lazy
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Lazy
    @Autowired
    private AttendanceService attendanceService;

    /**
     * Calculate leave days considering leavePeriod (full/am/pm) and weekends.
     */
    private BigDecimal calculateLeaveDays(OaLeaveApply apply) {
        LocalDate startDate = apply.getStartTime().toLocalDate();
        LocalDate endDate = apply.getEndTime().toLocalDate();
        String period = apply.getLeavePeriod() != null ? apply.getLeavePeriod() : "full";

        // Count full weekdays
        long fullWeekdays = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                fullWeekdays++;
            }
        }

        if ("full".equals(period) || fullWeekdays == 0) {
            return BigDecimal.valueOf(fullWeekdays);
        }

        // half-day logic (am/pm)
        boolean sameDay = startDate.equals(endDate);
        if (sameDay) {
            return BigDecimal.valueOf(0.5);
        }
        // Multi-day half-day: subtract 0.5 from full count (last day is half)
        return BigDecimal.valueOf(fullWeekdays - 1).add(BigDecimal.valueOf(0.5));
    }

    @Override
    @Transactional
    public void submit(OaLeaveApply apply) {
        if (apply.getStartTime() == null || apply.getEndTime() == null) {
            throw new BusinessException("请假起止时间不能为空");
        }
        apply.setStatus(0);
        this.save(apply);
        BigDecimal days = calculateLeaveDays(apply);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("days", days);
        workflowService.startProcess(BusinessType.LEAVE, apply.getId(), apply.getEmpId(), ctx);
        log.info("Leave submitted: id={}, empId={}, days={}", apply.getId(), apply.getEmpId(), days);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        approve(applyId, approverId, status, remark, null);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        WfTask task = null;
        if (taskId != null) {
            task = wfTaskMapper.selectById(taskId);
        }
        if (task == null) {
            task = workflowService.findPendingTask(BusinessType.LEAVE, applyId, approverId);
        }
        if (task == null) {
            cn.oa.entity.WfProcessInstance instance = workflowService.getByBusiness(BusinessType.LEAVE, applyId);
            if (instance != null) {
                LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WfTask::getInstanceId, instance.getId())
                       .eq(WfTask::getStatus, "0")
                       .orderByAsc(WfTask::getCreateTime)
                       .last("LIMIT 1");
                task = wfTaskMapper.selectOne(wrapper);
            }
        }
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        OaLeaveApply apply = this.getById(id);
        if (apply == null) return;
        if (apply.getStartTime() == null || apply.getEndTime() == null) return;

        Integer oldStatus = apply.getStatus();
        apply.setStatus(status);
        this.updateById(apply);

        BigDecimal days = calculateLeaveDays(apply);
        LocalDate startDate = apply.getStartTime().toLocalDate();
        LocalDate endDate = apply.getEndTime().toLocalDate();
        int year = startDate.getYear();

        // When approved (status=1): deduct balance and mark attendance
        if (status == 1 && oldStatus != 1) {
            leaveBalanceService.deductBalance(apply.getEmpId(), apply.getLeaveType(), year, days);
            attendanceService.markLeaveAttendance(apply.getEmpId(), startDate, endDate);
        }

        // When rejected(2) or withdrawn(4) after being approved(1): restore balance and remove attendance marks
        if ((status == 2 || status == 4) && oldStatus == 1) {
            leaveBalanceService.restoreBalance(apply.getEmpId(), apply.getLeaveType(), year, days);
            attendanceService.removeMarkedAttendance(apply.getEmpId(), startDate, endDate, 5);
        }
    }

    @Override
    public IPage<OaLeaveApply> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaLeaveApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaLeaveApply> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaLeaveApply::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaLeaveApply::getStatus, status);
        }
        wrapper.orderByDesc(OaLeaveApply::getCreateTime);
        IPage<OaLeaveApply> result = this.page(page, wrapper);

        // 填充 empName
        fillEmpNames(result.getRecords());
        // 填充 remark（取最新审批记录）
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaLeaveApply> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaLeaveApply::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaLeaveApply record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaLeaveApply> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaLeaveApply::getId)
                .collect(Collectors.toList());
        if (applyIds.isEmpty()) return;

        // 查询这些申请的审批记录
        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OaApprovalRecord::getApplyId, applyIds)
                .orderByDesc(OaApprovalRecord::getApproveTime);
        List<OaApprovalRecord> approvalRecords = approvalRecordMapper.selectList(wrapper);

        // 按 applyId 分组，取最新的 remark
        Map<Long, String> remarkMap = new HashMap<>();
        for (OaApprovalRecord ar : approvalRecords) {
            remarkMap.putIfAbsent(ar.getApplyId(), ar.getRemark());
        }

        for (OaLeaveApply record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
