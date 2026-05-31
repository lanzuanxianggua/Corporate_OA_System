package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaAttendance;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaAttendanceMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.AttendanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl extends ServiceImpl<OaAttendanceMapper, OaAttendance> implements AttendanceService {

    @Autowired
    private SysEmployeeMapper employeeMapper;

    private static final LocalTime NINE_OCLOCK = LocalTime.of(9, 0);
    private static final LocalTime SIX_OCLOCK = LocalTime.of(18, 0);

    @Override
    @Transactional
    public void clockIn(Long empId) {
        OaAttendance existing = getTodayAttendance(empId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (existing.getClockIn() != null) {
                throw new BusinessException("今日已打卡");
            }
            existing.setClockIn(now);
            if (Integer.valueOf(5).equals(existing.getStatus()) || Integer.valueOf(4).equals(existing.getStatus())) {
                // preserve leave/auto-marked status but record actual clock-in
            } else {
                existing.setStatus(now.toLocalTime().isAfter(NINE_OCLOCK) ? 1 : 0);
            }
            this.updateById(existing);
            return;
        }
        OaAttendance attendance = new OaAttendance();
        attendance.setEmpId(empId);
        attendance.setWorkDate(LocalDate.now());
        attendance.setClockIn(now);
        attendance.setStatus(now.toLocalTime().isAfter(NINE_OCLOCK) ? 1 : 0);
        this.save(attendance);
    }

    @Override
    @Transactional
    public void clockOut(Long empId) {
        OaAttendance attendance = getTodayAttendance(empId);
        if (attendance == null) {
            throw new BusinessException("今日未打卡，请先签到");
        }
        if (attendance.getClockOut() != null) {
            throw new BusinessException("今日已签退");
        }
        LocalDateTime now = LocalDateTime.now();
        attendance.setClockOut(now);
        // Only mark early leave (2) if currently normal (0); preserve late (1) status
        if (now.toLocalTime().isBefore(SIX_OCLOCK) && Integer.valueOf(0).equals(attendance.getStatus())) {
            attendance.setStatus(2);
        }
        this.updateById(attendance);
    }

    @Override
    public OaAttendance getTodayAttendance(Long empId) {
        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAttendance::getEmpId, empId)
                .eq(OaAttendance::getWorkDate, LocalDate.now());
        return this.getOne(wrapper, false);
    }

    @Override
    public List<OaAttendance> getAttendanceHistory(Long empId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAttendance::getEmpId, empId)
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .orderByDesc(OaAttendance::getWorkDate);
        return this.list(wrapper);
    }

    @Override
    public IPage<Map<String, Object>> adminPage(int pageNum, int pageSize, String empName, Integer status, LocalDate startDate, LocalDate endDate) {
        // Resolve matching empIds first — only query employees when a name filter is provided
        Set<Long> matchEmpIds = null;
        Map<Long, SysEmployee> empMap;
        if (empName != null && !empName.isBlank()) {
            LambdaQueryWrapper<SysEmployee> empWrapper = new LambdaQueryWrapper<>();
            empWrapper.like(SysEmployee::getEmpName, empName);
            List<SysEmployee> matchedEmps = employeeMapper.selectList(empWrapper);
            if (matchedEmps.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            matchEmpIds = matchedEmps.stream().map(SysEmployee::getId).collect(Collectors.toSet());
            empMap = matchedEmps.stream().collect(Collectors.toMap(SysEmployee::getId, e -> e));
        } else {
            empMap = new HashMap<>();
        }

        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        if (matchEmpIds != null) {
            wrapper.in(OaAttendance::getEmpId, matchEmpIds);
        }
        if (status != null) {
            wrapper.eq(OaAttendance::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(OaAttendance::getWorkDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(OaAttendance::getWorkDate, endDate);
        }
        wrapper.orderByDesc(OaAttendance::getWorkDate);

        IPage<OaAttendance> page = this.page(new Page<>(pageNum, pageSize), wrapper);

        // Batch-load only the employees that appear in the attendance records
        Set<Long> neededEmpIds = page.getRecords().stream()
                .map(OaAttendance::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        neededEmpIds.removeAll(empMap.keySet());
        if (!neededEmpIds.isEmpty()) {
            List<SysEmployee> extraEmps = employeeMapper.selectBatchIds(neededEmpIds);
            for (SysEmployee e : extraEmps) {
                empMap.put(e.getId(), e);
            }
        }

        List<Map<String, Object>> records = page.getRecords().stream().map(att -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", att.getId());
            map.put("empId", att.getEmpId());
            SysEmployee emp = empMap.get(att.getEmpId());
            map.put("empName", emp != null ? emp.getEmpName() : "");
            map.put("deptId", emp != null ? emp.getDeptId() : null);
            map.put("workDate", att.getWorkDate());
            map.put("clockIn", att.getClockIn());
            map.put("clockOut", att.getClockOut());
            map.put("status", att.getStatus());
            map.put("remark", att.getRemark());
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(page.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional
    public void markLeaveAttendance(Long empId, LocalDate startDate, LocalDate endDate) {
        markAutoAttendance(empId, startDate, endDate, 5, "请假自动标记");
    }

    @Override
    @Transactional
    public void markTripAttendance(Long empId, LocalDate startDate, LocalDate endDate) {
        markAutoAttendance(empId, startDate, endDate, 6, "出差自动标记");
    }

    @Override
    @Transactional
    public void removeMarkedAttendance(Long empId, LocalDate startDate, LocalDate endDate, Integer status) {
        this.remove(new LambdaQueryWrapper<OaAttendance>()
                .eq(OaAttendance::getEmpId, empId)
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .eq(OaAttendance::getStatus, status)
                .like(OaAttendance::getRemark, "自动标记"));
    }

    private void markAutoAttendance(Long empId, LocalDate startDate, LocalDate endDate, int status, String remark) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            OaAttendance existing = this.getOne(new LambdaQueryWrapper<OaAttendance>()
                    .eq(OaAttendance::getEmpId, empId)
                    .eq(OaAttendance::getWorkDate, date));
            if (existing == null) {
                OaAttendance attendance = new OaAttendance();
                attendance.setEmpId(empId);
                attendance.setWorkDate(date);
                attendance.setStatus(status);
                attendance.setRemark(remark);
                this.save(attendance);
            } else if (existing.getStatus() != null && existing.getStatus() == 3) {
                existing.setStatus(status);
                existing.setRemark(remark);
                this.updateById(existing);
            }
        }
    }

    @Override
    public List<OaAttendance> getHistoryByDateRange(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(OaAttendance::getWorkDate, startDate, endDate)
                .orderByDesc(OaAttendance::getWorkDate);
        return this.list(wrapper);
    }
}
