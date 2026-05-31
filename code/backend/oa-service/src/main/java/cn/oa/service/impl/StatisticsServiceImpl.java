package cn.oa.service.impl;

import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SysEmployeeMapper employeeMapper;
    @Autowired
    private OaAttendanceMapper attendanceMapper;
    @Autowired
    private OaLeaveApplyMapper leaveApplyMapper;
    @Autowired
    private SysDeptMapper deptMapper;
    @Autowired
    private OaBusinessTripMapper businessTripMapper;
    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    /** 计算一个月内的工作日数 (周一至周五) */
    private int countWorkdays(LocalDate start, LocalDate end) {
        int workdays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                workdays++;
            }
        }
        return workdays;
    }

    @Override
    public Map<String, Object> getDashboardStats(String period, Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 员工总数
        Long employeeTotal = employeeMapper.selectCount(
            new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));
        result.put("employeeTotal", employeeTotal);

        // 2. 计算日期范围
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;
        switch (period == null ? "today" : period) {
            case "year":
                int y = year != null ? year : LocalDate.now().getYear();
                startDate = LocalDate.of(y, 1, 1);
                endDate = LocalDate.of(y, 12, 31);
                break;
            case "week":
                startDate = today.minusDays(6); // 最近7天
                break;
            case "month":
                startDate = today.withDayOfMonth(1); // 本月1号
                break;
            default:
                startDate = today;
        }

        // 3. 新增统计数据（本月维度）
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today;

        // 本月请假人数（去重）
        Long leaveCountThisMonth = leaveApplyMapper.selectCount(
            new LambdaQueryWrapper<OaLeaveApply>()
                .ge(OaLeaveApply::getStartTime, monthStart.atTime(0, 0, 0))
                .le(OaLeaveApply::getEndTime, monthEnd.atTime(23, 59, 59)));
        result.put("leaveCountThisMonth", leaveCountThisMonth);

        // 本月出差人数（去重）
        Long businessTripCountThisMonth = businessTripMapper.selectCount(
            new LambdaQueryWrapper<OaBusinessTrip>()
                .ge(OaBusinessTrip::getStartTime, monthStart.atTime(0, 0, 0))
                .le(OaBusinessTrip::getEndTime, monthEnd.atTime(23, 59, 59)));
        result.put("businessTripCountThisMonth", businessTripCountThisMonth);

        // 待审批数量
        Long pendingApprovals = approvalRecordMapper.selectCount(
            new LambdaQueryWrapper<OaApprovalRecord>()
                .eq(OaApprovalRecord::getApproveStatus, 0));
        result.put("pendingApprovals", pendingApprovals);

        // 本月新员工
        Long newEmployeesThisMonth = employeeMapper.selectCount(
            new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getStatus, 1)
                .ge(SysEmployee::getCreateTime, monthStart.atTime(0, 0, 0))
                .le(SysEmployee::getCreateTime, monthEnd.atTime(23, 59, 59)));
        result.put("newEmployeesThisMonth", newEmployeesThisMonth);

        // 4. 考勤统计（按日期范围统计）
        Map<String, Object> attendance = new LinkedHashMap<>();

        // 范围内打卡总人次
        Long clockedIn = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .isNotNull(OaAttendance::getClockIn));
        attendance.put("clockedIn", clockedIn);

        // 范围内迟到总人次
        Long late = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .apply("TIME(clock_in) > '09:00:00'"));
        attendance.put("late", late);

        // 范围内早退总人次
        Long earlyLeave = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .isNotNull(OaAttendance::getClockOut)
                .apply("TIME(clock_out) < '18:00:00'"));
        attendance.put("earlyLeave", earlyLeave);

        // 范围内已批准请假人次
        Long onLeave = leaveApplyMapper.selectCount(
            new LambdaQueryWrapper<OaLeaveApply>()
                .eq(OaLeaveApply::getStatus, 1)
                .le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0)));

        // 缺勤总人次 = 范围天数 × 员工总数 - 打卡总人次 - 请假人次
        long rangeDays = startDate.until(endDate).getDays() + 1;
        long totalRequired = employeeTotal * rangeDays;
        long absent = Math.max(0, totalRequired - clockedIn - onLeave);
        attendance.put("absent", absent);
        attendance.put("totalRequired", totalRequired);
        result.put("attendance", attendance);

        // 5. 请假统计
        Map<String, Object> leave = new LinkedHashMap<>();

        LambdaQueryWrapper<OaLeaveApply> leaveWrapper = new LambdaQueryWrapper<>();
        // 请假时间段与查询范围有交集
        leaveWrapper.le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                     .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0));

        List<OaLeaveApply> leaveList = leaveApplyMapper.selectList(leaveWrapper);
        leave.put("total", leaveList.size());
        leave.put("pending", leaveList.stream().filter(l -> l.getStatus() != null && l.getStatus() == 0).count());
        leave.put("approved", leaveList.stream().filter(l -> l.getStatus() != null && l.getStatus() == 1).count());

        // 按类型统计
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<Integer, String> typeNames = Map.of(0, "事假", 1, "病假", 2, "年假", 3, "婚假", 4, "产假", 5, "其他");
        leaveList.stream()
            .collect(Collectors.groupingBy(OaLeaveApply::getLeaveType, Collectors.counting()))
            .forEach((type, count) -> byType.put(typeNames.getOrDefault(type != null && !type.isEmpty() ? Integer.valueOf(type) : null, "其他"), count));
        leave.put("byType", byType);
        result.put("leave", leave);

        // 6. 部门人数分布
        List<SysDept> depts = deptMapper.selectList(null);
        List<SysEmployee> employees = employeeMapper.selectList(
            new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));
        Map<Long, Long> deptCountMap = employees.stream()
            .filter(e -> e.getDeptId() != null)
            .collect(Collectors.groupingBy(SysEmployee::getDeptId, Collectors.counting()));

        List<Map<String, Object>> deptDistribution = new ArrayList<>();
        for (SysDept dept : depts) {
            if (deptCountMap.containsKey(dept.getId())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", dept.getDeptName());
                item.put("value", deptCountMap.get(dept.getId()));
                deptDistribution.add(item);
            }
        }
        result.put("departmentDistribution", deptDistribution);

        // 7. 出勤趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate trendStart = null;
        LocalDate trendEnd = today;
        switch (period == null ? "today" : period) {
            case "year":
                int y = year != null ? year : LocalDate.now().getYear();
                List<Map<String, Object>> yearlyTrend = new ArrayList<>();
                for (int m = 1; m <= 12; m++) {
                    YearMonth ym = YearMonth.of(y, m);
                    LocalDate mStart = ym.atDay(1);
                    LocalDate mEnd = ym.atEndOfMonth();
                    Long mClocked = attendanceMapper.selectCount(
                        new LambdaQueryWrapper<OaAttendance>()
                            .between(OaAttendance::getWorkDate, mStart, mEnd)
                            .isNotNull(OaAttendance::getClockIn));
                    // 修正：用 (员工总数 × 当月工作日数) 作为分母
                    int workdaysInMonth = countWorkdays(mStart, mEnd);
                    int mRate = (employeeTotal > 0 && workdaysInMonth > 0)
                        ? (int)(mClocked * 100 / (employeeTotal * workdaysInMonth)) : 0;
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("month", m + "月");
                    point.put("rate", mRate);
                    yearlyTrend.add(point);
                }
                result.put("yearlyAttendanceTrend", yearlyTrend);
                break;
            case "month":
                trendStart = today.withDayOfMonth(1);
                break;
            case "week":
                trendStart = today.minusDays(6);
                break;
            default:
                trendStart = today;
        }
        if (trendStart != null) {
            for (LocalDate date = trendStart; !date.isAfter(trendEnd); date = date.plusDays(1)) {
                Long dayClocked = attendanceMapper.selectCount(
                    new LambdaQueryWrapper<OaAttendance>()
                        .eq(OaAttendance::getWorkDate, date)
                        .isNotNull(OaAttendance::getClockIn));
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", date.format(fmt));
                int rate = employeeTotal > 0 ? (int) (dayClocked * 100 / employeeTotal) : 0;
                point.put("rate", rate);
                trend.add(point);
            }
            result.put("attendanceTrend", trend);
        }

        // 8. 迟到排行榜
        List<Map<String, Object>> lateRanking = buildLateRanking(startDate, endDate);
        result.put("lateRanking", lateRanking);

        // 9. 出勤排行榜
        List<Map<String, Object>> attendanceRanking = buildAttendanceRanking(startDate, endDate);
        result.put("attendanceRanking", attendanceRanking);

        return result;
    }

    private List<Map<String, Object>> buildLateRanking(LocalDate startDate, LocalDate endDate) {
        List<OaAttendance> lateRecords = attendanceMapper.selectList(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate)
                .eq(OaAttendance::getStatus, 1));

        Map<Long, Long> lateCountMap = lateRecords.stream()
            .collect(Collectors.groupingBy(OaAttendance::getEmpId, Collectors.counting()));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : lateCountMap.entrySet()) {
            SysEmployee emp = employeeMapper.selectById(entry.getKey());
            if (emp != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("empName", emp.getEmpName());
                item.put("lateCount", entry.getValue());
                ranking.add(item);
            }
        }
        ranking.sort((a, b) -> Long.compare((Long) b.get("lateCount"), (Long) a.get("lateCount")));
        return ranking.size() > 10 ? ranking.subList(0, 10) : ranking;
    }

    private List<Map<String, Object>> buildAttendanceRanking(LocalDate startDate, LocalDate endDate) {
        List<SysEmployee> activeEmployees = employeeMapper.selectList(
            new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysEmployee emp : activeEmployees) {
            Long totalDays = attendanceMapper.selectCount(
                new LambdaQueryWrapper<OaAttendance>()
                    .between(OaAttendance::getWorkDate, startDate, endDate)
                    .eq(OaAttendance::getEmpId, emp.getId()));
            if (totalDays == 0) continue;

            Long normalDays = attendanceMapper.selectCount(
                new LambdaQueryWrapper<OaAttendance>()
                    .between(OaAttendance::getWorkDate, startDate, endDate)
                    .eq(OaAttendance::getEmpId, emp.getId())
                    .eq(OaAttendance::getStatus, 0));

            double rate = Math.round(normalDays * 1000.0 / totalDays) / 10.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empName", emp.getEmpName());
            item.put("rate", rate);
            item.put("normalDays", normalDays);
            item.put("totalDays", totalDays);
            ranking.add(item);
        }
        ranking.sort((a, b) -> Double.compare((Double) b.get("rate"), (Double) a.get("rate")));
        return ranking.size() > 10 ? ranking.subList(0, 10) : ranking;
    }
}
