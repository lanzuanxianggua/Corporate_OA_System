package cn.oa.service.impl;

import cn.oa.common.service.RedisService;
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
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private WfTaskMapper taskMapper;
    @Autowired
    private WfProcessInstanceMapper processInstanceMapper;
    @Autowired
    private RedisService redisService;

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
    public Map<String, Object> getDashboardStats(String period, Integer year, LocalDate date) {
        // 尝试从缓存获取
        String cacheKey = "cache:stats:dashboard:v3:" + (period == null ? "today" : period)
                + ":" + (year != null ? year : LocalDate.now().getYear())
                + ":" + (date != null ? date : LocalDate.now());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = redisService.getJson(cacheKey, Map.class);
            if (cached != null) {
                log.debug("仪表盘数据命中缓存: {}", cacheKey);
                return cached;
            }
        } catch (Exception e) {
            log.warn("仪表盘缓存读取失败，删除缓存后重新计算: {}", cacheKey, e);
            redisService.delete(cacheKey);
        }

        Map<String, Object> result = doGetDashboardStats(period, year, date);

        // 缓存5分钟，避免频繁查询
        redisService.set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 实际计算仪表盘数据（无缓存）
     */
    private Map<String, Object> doGetDashboardStats(String period, Integer year, LocalDate selectedDate) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 员工总数
        Long employeeTotal = employeeMapper.selectCount(
            new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));
        result.put("employeeTotal", employeeTotal);

        // 2. 计算日期范围
        LocalDate today = selectedDate != null ? selectedDate : LocalDate.now();
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
                .lt(OaLeaveApply::getStartTime, monthEnd.plusDays(1).atStartOfDay())
                .gt(OaLeaveApply::getEndTime, monthStart.atStartOfDay()));
        result.put("leaveCountThisMonth", leaveCountThisMonth);

        // 本月出差人数（去重）
        Long businessTripCountThisMonth = businessTripMapper.selectCount(
            new LambdaQueryWrapper<OaBusinessTrip>()
                .lt(OaBusinessTrip::getStartTime, monthEnd.plusDays(1).atStartOfDay())
                .gt(OaBusinessTrip::getEndTime, monthStart.atStartOfDay()));
        result.put("businessTripCountThisMonth", businessTripCountThisMonth);

        // 待审批数量以 wf_task 为准。oa_approval_record 是已发生的审批流水，不能表示当前待办。
        Long pendingApprovals = taskMapper.selectCount(
            new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getStatus, "0"));
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
        long rangeDays = countWorkdays(startDate, endDate);
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

        // 10. 缺勤排行榜
        List<Map<String, Object>> absenceRanking = buildAbsenceRanking(startDate, endDate);
        result.put("absenceRanking", absenceRanking);

        // 11. 管理层看板扩展指标
        result.put("dateRange", buildDateRange(startDate, endDate, rangeDays));
        result.put("attendanceTrendDetailed", buildAttendanceTrendDetailed(today.minusDays(13), today, employeeTotal));
        result.put("officeActivityTrend", buildOfficeActivityTrend(today.minusDays(29), today));
        result.put("officeActivityHeatmap", buildOfficeActivityHeatmap(today.minusDays(29), today));
        result.put("attendanceStatusDistribution", buildAttendanceStatusDistribution(startDate, endDate));
        result.put("approvalFunnel", buildApprovalFunnel());
        result.put("approvalStatusDistribution", buildApprovalStatusDistribution());
        result.put("approvalBusinessDistribution", buildApprovalBusinessDistribution());
        result.put("monthlyOperationTrend", buildMonthlyOperationTrend(today));
        result.put("departmentWorkload", buildDepartmentWorkload(depts, employees, startDate, endDate));
        result.put("riskIndicators", buildRiskIndicators(result));

        return result;
    }

    private Map<String, Object> buildDateRange(LocalDate startDate, LocalDate endDate, long workdays) {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("startDate", startDate.toString());
        range.put("endDate", endDate.toString());
        range.put("workdays", workdays);
        return range;
    }

    private List<Map<String, Object>> buildOfficeActivityTrend(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM-dd");

        List<OaAttendance> attendanceList = attendanceMapper.selectList(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate));
        List<OaApprovalRecord> approvalRecords = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<OaApprovalRecord>()
                .between(OaApprovalRecord::getApproveTime, start, end));

        Map<LocalDate, List<OaAttendance>> attendanceByDate = attendanceList.stream()
            .filter(item -> item.getWorkDate() != null)
            .collect(Collectors.groupingBy(OaAttendance::getWorkDate));
        Map<LocalDate, List<OaApprovalRecord>> approvalsByDate = approvalRecords.stream()
            .filter(item -> item.getApproveTime() != null)
            .collect(Collectors.groupingBy(item -> item.getApproveTime().toLocalDate()));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<OaAttendance> dailyAttendance = attendanceByDate.getOrDefault(date, Collections.emptyList());
            List<OaApprovalRecord> dailyApprovals = approvalsByDate.getOrDefault(date, Collections.emptyList());
            Set<Long> activeEmployeeIds = new HashSet<>();
            double workHours = 0.0;

            for (OaAttendance attendance : dailyAttendance) {
                if (attendance.getEmpId() != null && (attendance.getClockIn() != null || attendance.getClockOut() != null)) {
                    activeEmployeeIds.add(attendance.getEmpId());
                }
                workHours += calculateWorkHours(attendance.getClockIn(), attendance.getClockOut());
            }
            for (OaApprovalRecord record : dailyApprovals) {
                if (record.getApproverId() != null) {
                    activeEmployeeIds.add(record.getApproverId());
                }
            }

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.format(labelFormatter));
            point.put("fullDate", date.toString());
            point.put("activeEmployees", activeEmployeeIds.size());
            point.put("workHours", Math.round(workHours * 10.0) / 10.0);
            point.put("approvalActions", dailyApprovals.size());
            trend.add(point);
        }
        return trend;
    }

    private List<Map<String, Object>> buildOfficeActivityHeatmap(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        Map<String, Map<String, Object>> data = new LinkedHashMap<>();

        for (int weekday = 0; weekday < 7; weekday++) {
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("weekday", weekday);
                item.put("hour", hour);
                item.put("events", 0L);
                item.put("clockInEvents", 0L);
                item.put("clockOutEvents", 0L);
                item.put("approvalEvents", 0L);
                data.put(heatmapKey(weekday, hour), item);
            }
        }

        List<OaAttendance> attendanceList = attendanceMapper.selectList(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate));
        for (OaAttendance attendance : attendanceList) {
            incrementHeatmap(data, attendance.getClockIn(), "clockInEvents");
            incrementHeatmap(data, attendance.getClockOut(), "clockOutEvents");
        }

        List<OaApprovalRecord> approvalRecords = approvalRecordMapper.selectList(
            new LambdaQueryWrapper<OaApprovalRecord>()
                .between(OaApprovalRecord::getApproveTime, start, end));
        for (OaApprovalRecord record : approvalRecords) {
            incrementHeatmap(data, record.getApproveTime(), "approvalEvents");
        }

        return new ArrayList<>(data.values());
    }

    private double calculateWorkHours(LocalDateTime clockIn, LocalDateTime clockOut) {
        if (clockIn == null || clockOut == null || !clockOut.isAfter(clockIn)) {
            return 0.0;
        }
        long minutes = Duration.between(clockIn, clockOut).toMinutes();
        return Math.min(minutes / 60.0, 16.0);
    }

    private void incrementHeatmap(Map<String, Map<String, Object>> data, LocalDateTime time, String field) {
        if (time == null) {
            return;
        }
        int weekday = time.getDayOfWeek().getValue() - 1;
        int hour = time.getHour();
        Map<String, Object> item = data.get(heatmapKey(weekday, hour));
        if (item == null) {
            return;
        }
        item.put("events", numberValue(item.get("events")) + 1);
        item.put(field, numberValue(item.get(field)) + 1);
    }

    private String heatmapKey(int weekday, int hour) {
        return weekday + ":" + hour;
    }

    private List<Map<String, Object>> buildAttendanceTrendDetailed(LocalDate startDate, LocalDate endDate, Long employeeTotal) {
        long activeEmployeeTotal = employeeTotal == null ? 0L : employeeTotal;
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM-dd");

        List<OaAttendance> attendanceList = attendanceMapper.selectList(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate));
        Map<LocalDate, List<OaAttendance>> attendanceByDate = attendanceList.stream()
            .filter(item -> item.getWorkDate() != null)
            .collect(Collectors.groupingBy(OaAttendance::getWorkDate));

        List<OaLeaveApply> approvedLeaves = leaveApplyMapper.selectList(
            new LambdaQueryWrapper<OaLeaveApply>()
                .eq(OaLeaveApply::getStatus, 1)
                .le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0)));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            boolean workday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            long required = workday ? activeEmployeeTotal : 0L;

            List<OaAttendance> dailyAttendance = attendanceByDate.getOrDefault(date, Collections.emptyList());
            long clocked = dailyAttendance.stream().filter(item -> item.getClockIn() != null).count();
            long late = dailyAttendance.stream().filter(item -> item.getStatus() != null && (item.getStatus() == 1 || item.getStatus() == 4)).count();
            long earlyLeave = dailyAttendance.stream().filter(item -> item.getStatus() != null && (item.getStatus() == 2 || item.getStatus() == 4)).count();
            long leave = countApprovedLeaveOnDate(approvedLeaves, date);
            long absent = Math.max(0, required - clocked - leave);
            long rate = required > 0 ? Math.round(clocked * 100.0 / required) : 0L;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.format(labelFormatter));
            point.put("fullDate", date.toString());
            point.put("clockedIn", clocked);
            point.put("late", late);
            point.put("earlyLeave", earlyLeave);
            point.put("leave", leave);
            point.put("absent", absent);
            point.put("attendanceRate", rate);
            trend.add(point);
        }
        return trend;
    }

    private long countApprovedLeaveOnDate(List<OaLeaveApply> approvedLeaves, LocalDate date) {
        return approvedLeaves.stream()
            .filter(leave -> leave.getStartTime() != null && leave.getEndTime() != null)
            .filter(leave -> !leave.getStartTime().toLocalDate().isAfter(date) && !leave.getEndTime().toLocalDate().isBefore(date))
            .filter(leave -> {
                DayOfWeek dow = date.getDayOfWeek();
                return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            })
            .count();
    }

    private List<Map<String, Object>> buildAttendanceStatusDistribution(LocalDate startDate, LocalDate endDate) {
        Map<Integer, String> labels = Map.of(
            0, "正常",
            1, "迟到",
            2, "早退",
            3, "缺勤",
            4, "迟到且早退",
            5, "请假",
            6, "出差"
        );
        List<OaAttendance> attendanceList = attendanceMapper.selectList(
            new LambdaQueryWrapper<OaAttendance>()
                .between(OaAttendance::getWorkDate, startDate, endDate));
        Map<Integer, Long> grouped = attendanceList.stream()
            .filter(item -> item.getStatus() != null)
            .collect(Collectors.groupingBy(OaAttendance::getStatus, Collectors.counting()));

        return grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", labels.getOrDefault(entry.getKey(), "未知"));
                item.put("value", entry.getValue());
                item.put("status", entry.getKey());
                return item;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildApprovalFunnel() {
        long submitted = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>());
        long approved = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStatus, "1"));
        long rejected = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStatus, "2"));
        long canceled = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStatus, "3"));
        long completed = approved + rejected + canceled;

        List<Map<String, Object>> funnel = new ArrayList<>();
        funnel.add(namedValue("发起申请", submitted));
        funnel.add(namedValue("已完结", completed));
        funnel.add(namedValue("已通过", approved));
        return funnel;
    }

    private List<Map<String, Object>> buildApprovalStatusDistribution() {
        Map<String, String> labels = Map.of(
            "0", "审批中",
            "1", "已通过",
            "2", "已拒绝",
            "3", "已撤回",
            "5", "已退回"
        );
        List<WfProcessInstance> instances = processInstanceMapper.selectList(new LambdaQueryWrapper<WfProcessInstance>());
        Map<String, Long> grouped = instances.stream()
            .collect(Collectors.groupingBy(instance -> instance.getStatus() == null ? "0" : instance.getStatus(), Collectors.counting()));
        return grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", labels.getOrDefault(entry.getKey(), "其他"));
                item.put("value", entry.getValue());
                item.put("status", entry.getKey());
                return item;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildApprovalBusinessDistribution() {
        List<WfProcessInstance> instances = processInstanceMapper.selectList(new LambdaQueryWrapper<WfProcessInstance>());
        Map<String, List<WfProcessInstance>> grouped = instances.stream()
            .collect(Collectors.groupingBy(instance -> instance.getBusinessType() == null ? "unknown" : instance.getBusinessType()));

        return grouped.entrySet().stream()
            .map(entry -> {
                List<WfProcessInstance> items = entry.getValue();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("businessType", entry.getKey());
                item.put("name", businessTypeLabel(entry.getKey()));
                item.put("total", items.size());
                item.put("pending", items.stream().filter(instance -> "0".equals(instance.getStatus())).count());
                item.put("approved", items.stream().filter(instance -> "1".equals(instance.getStatus())).count());
                item.put("rejected", items.stream().filter(instance -> "2".equals(instance.getStatus())).count());
                item.put("canceled", items.stream().filter(instance -> "3".equals(instance.getStatus())).count());
                return item;
            })
            .sorted((a, b) -> Long.compare(((Number) b.get("total")).longValue(), ((Number) a.get("total")).longValue()))
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildMonthlyOperationTrend(LocalDate today) {
        YearMonth endMonth = YearMonth.from(today);
        YearMonth startMonth = endMonth.minusMonths(5);
        LocalDateTime start = startMonth.atDay(1).atStartOfDay();
        LocalDateTime end = endMonth.atEndOfMonth().atTime(23, 59, 59);

        List<OaLeaveApply> leaves = leaveApplyMapper.selectList(
            new LambdaQueryWrapper<OaLeaveApply>()
                .between(OaLeaveApply::getCreateTime, start, end));
        List<OaBusinessTrip> trips = businessTripMapper.selectList(
            new LambdaQueryWrapper<OaBusinessTrip>()
                .between(OaBusinessTrip::getCreateTime, start, end));
        List<WfProcessInstance> instances = processInstanceMapper.selectList(
            new LambdaQueryWrapper<WfProcessInstance>()
                .between(WfProcessInstance::getCreateTime, start, end));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            YearMonth current = month;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", current.getMonthValue() + "月");
            item.put("leave", leaves.stream().filter(leave -> sameMonth(leave.getCreateTime(), current)).count());
            item.put("trip", trips.stream().filter(trip -> sameMonth(trip.getCreateTime(), current)).count());
            item.put("submitted", instances.stream().filter(instance -> sameMonth(instance.getCreateTime(), current)).count());
            item.put("approved", instances.stream().filter(instance -> sameMonth(instance.getEndTime(), current) && "1".equals(instance.getStatus())).count());
            item.put("rejected", instances.stream().filter(instance -> sameMonth(instance.getEndTime(), current) && "2".equals(instance.getStatus())).count());
            trend.add(item);
        }
        return trend;
    }

    private boolean sameMonth(LocalDateTime time, YearMonth month) {
        return time != null && YearMonth.from(time).equals(month);
    }

    private List<Map<String, Object>> buildDepartmentWorkload(
            List<SysDept> depts,
            List<SysEmployee> employees,
            LocalDate startDate,
            LocalDate endDate) {
        int workdays = countWorkdays(startDate, endDate);
        Map<Long, String> deptNames = depts.stream()
            .filter(dept -> dept.getId() != null)
            .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        Map<Long, Long> empDeptMap = employees.stream()
            .filter(emp -> emp.getId() != null && emp.getDeptId() != null)
            .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getDeptId, (a, b) -> a));
        Map<Long, Long> employeeCountByDept = employees.stream()
            .filter(emp -> emp.getDeptId() != null)
            .collect(Collectors.groupingBy(SysEmployee::getDeptId, Collectors.counting()));

        Map<Long, Long> clockedByDept = attendanceMapper.selectList(
                new LambdaQueryWrapper<OaAttendance>()
                    .between(OaAttendance::getWorkDate, startDate, endDate)
                    .isNotNull(OaAttendance::getClockIn))
            .stream()
            .filter(item -> item.getEmpId() != null && empDeptMap.containsKey(item.getEmpId()))
            .collect(Collectors.groupingBy(item -> empDeptMap.get(item.getEmpId()), Collectors.counting()));

        Map<Long, Long> leaveByDept = leaveApplyMapper.selectList(
                new LambdaQueryWrapper<OaLeaveApply>()
                    .le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                    .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0)))
            .stream()
            .filter(item -> item.getEmpId() != null && empDeptMap.containsKey(item.getEmpId()))
            .collect(Collectors.groupingBy(item -> empDeptMap.get(item.getEmpId()), Collectors.counting()));

        Map<Long, Long> tripByDept = businessTripMapper.selectList(
                new LambdaQueryWrapper<OaBusinessTrip>()
                    .le(OaBusinessTrip::getStartTime, endDate.atTime(23, 59, 59))
                    .ge(OaBusinessTrip::getEndTime, startDate.atTime(0, 0, 0)))
            .stream()
            .filter(item -> item.getEmpId() != null && empDeptMap.containsKey(item.getEmpId()))
            .collect(Collectors.groupingBy(item -> empDeptMap.get(item.getEmpId()), Collectors.counting()));

        return employeeCountByDept.entrySet().stream()
            .map(entry -> {
                long deptId = entry.getKey();
                long employeeCount = entry.getValue();
                long required = employeeCount * workdays;
                long clocked = clockedByDept.getOrDefault(deptId, 0L);
                long leaveCount = leaveByDept.getOrDefault(deptId, 0L);
                long tripCount = tripByDept.getOrDefault(deptId, 0L);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", deptNames.getOrDefault(deptId, "未分配部门"));
                item.put("employeeCount", employeeCount);
                item.put("clockedIn", clocked);
                item.put("leaveCount", leaveCount);
                item.put("tripCount", tripCount);
                item.put("load", leaveCount + tripCount);
                item.put("attendanceRate", required > 0 ? Math.round(clocked * 100.0 / required) : 0);
                return item;
            })
            .sorted((a, b) -> Long.compare(((Number) b.get("load")).longValue(), ((Number) a.get("load")).longValue()))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildRiskIndicators(Map<String, Object> result) {
        Map<String, Object> attendance = (Map<String, Object>) result.getOrDefault("attendance", Collections.emptyMap());
        Map<String, Object> leave = (Map<String, Object>) result.getOrDefault("leave", Collections.emptyMap());
        long totalRequired = numberValue(attendance.get("totalRequired"));
        long clockedIn = numberValue(attendance.get("clockedIn"));
        long absent = numberValue(attendance.get("absent"));
        long late = numberValue(attendance.get("late"));
        long earlyLeave = numberValue(attendance.get("earlyLeave"));
        long pending = numberValue(result.get("pendingApprovals"));
        long approved = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStatus, "1"));
        long rejected = processInstanceMapper.selectCount(new LambdaQueryWrapper<WfProcessInstance>().eq(WfProcessInstance::getStatus, "2"));
        long completed = approved + rejected;

        double attendanceRate = totalRequired > 0 ? clockedIn * 100.0 / totalRequired : 0.0;
        double absenceRate = totalRequired > 0 ? absent * 100.0 / totalRequired : 0.0;
        double exceptionRate = totalRequired > 0 ? (late + earlyLeave + absent) * 100.0 / totalRequired : 0.0;
        double approvalPassRate = completed > 0 ? approved * 100.0 / completed : 0.0;

        List<Map<String, Object>> indicators = new ArrayList<>();
        indicators.add(riskItem("出勤健康", Math.round(attendanceRate) + "%", attendanceRate >= 90 ? "good" : attendanceRate >= 75 ? "warning" : "danger",
            "已打卡 " + clockedIn + " / 应出勤 " + totalRequired));
        indicators.add(riskItem("缺勤风险", Math.round(absenceRate) + "%", absenceRate <= 5 ? "good" : absenceRate <= 15 ? "warning" : "danger",
            "缺勤 " + absent + " 人次，需结合请假与外出记录复核"));
        indicators.add(riskItem("审批积压", pending + " 条", pending <= 10 ? "good" : pending <= 50 ? "warning" : "danger",
            "当前仍处于待审批状态的工作流任务"));
        indicators.add(riskItem("审批通过率", Math.round(approvalPassRate) + "%", approvalPassRate >= 80 ? "good" : approvalPassRate >= 60 ? "warning" : "danger",
            "已完成审批中通过 " + approved + " 条、拒绝 " + rejected + " 条"));
        indicators.add(riskItem("异常负载", Math.round(exceptionRate) + "%", exceptionRate <= 8 ? "good" : exceptionRate <= 20 ? "warning" : "danger",
            "迟到、早退、缺勤合计 " + (late + earlyLeave + absent) + " 人次"));
        indicators.add(riskItem("请假待审", numberValue(leave.get("pending")) + " 条", numberValue(leave.get("pending")) <= 5 ? "good" : "warning",
            "所选日期范围内仍待确认的请假申请"));
        return indicators;
    }

    private Map<String, Object> namedValue(String name, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private Map<String, Object> riskItem(String label, String value, String level, String note) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("level", level);
        item.put("note", note);
        return item;
    }

    private long numberValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private String businessTypeLabel(String businessType) {
        Map<String, String> labels = Map.of(
            "leave", "请假",
            "trip", "出差",
            "outing", "外出",
            "overtime", "加班",
            "expense", "报销",
            "purchase", "采购",
            "loan", "借支"
        );
        return labels.getOrDefault(businessType, businessType == null ? "未知业务" : businessType);
    }

    private List<Map<String, Object>> buildLateRanking(LocalDate startDate, LocalDate endDate) {
        // 使用 GROUP BY 一次查询所有员工的迟到次数
        List<Map<String, Object>> lateStats = attendanceMapper.selectLateCountGroupByEmp(startDate, endDate);

        if (lateStats.isEmpty()) return Collections.emptyList();

        // 批量查询员工信息
        Set<Long> empIds = lateStats.stream()
                .map(row -> ((Number) row.get("emp_id")).longValue())
                .collect(Collectors.toSet());
        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> empNameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : lateStats) {
            Long empId = ((Number) row.get("emp_id")).longValue();
            String empName = empNameMap.get(empId);
            if (empName != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("empName", empName);
                item.put("lateCount", ((Number) row.get("late_count")).longValue());
                ranking.add(item);
            }
        }
        ranking.sort((a, b) -> Long.compare((Long) b.get("lateCount"), (Long) a.get("lateCount")));
        return limitRanking(ranking);
    }

    private List<Map<String, Object>> buildAttendanceRanking(LocalDate startDate, LocalDate endDate) {
        // 使用 GROUP BY 一次查询所有员工的出勤统计
        List<Map<String, Object>> attendanceStats = attendanceMapper.selectAttendanceStatsGroupByEmp(startDate, endDate);

        if (attendanceStats.isEmpty()) return Collections.emptyList();

        // 批量查询员工信息
        Set<Long> empIds = attendanceStats.stream()
                .map(row -> ((Number) row.get("emp_id")).longValue())
                .collect(Collectors.toSet());
        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> empNameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : attendanceStats) {
            Long empId = ((Number) row.get("emp_id")).longValue();
            String empName = empNameMap.get(empId);
            if (empName == null) continue;

            long totalDays = ((Number) row.get("total")).longValue();
            long normalDays = ((Number) row.get("normal")).longValue();
            double rate = Math.round(normalDays * 1000.0 / totalDays) / 10.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empName", empName);
            item.put("rate", rate);
            item.put("normalDays", normalDays);
            item.put("totalDays", totalDays);
            ranking.add(item);
        }
        ranking.sort((a, b) -> Double.compare((Double) b.get("rate"), (Double) a.get("rate")));
        return limitRanking(ranking);
    }

    private List<Map<String, Object>> buildAbsenceRanking(LocalDate startDate, LocalDate endDate) {
        int requiredWorkdays = countWorkdays(startDate, endDate);
        if (requiredWorkdays <= 0) return Collections.emptyList();

        List<SysEmployee> employees = employeeMapper.selectList(
            new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));
        if (employees.isEmpty()) return Collections.emptyList();

        Map<Long, Long> clockedDaysMap = attendanceMapper.selectList(
                new LambdaQueryWrapper<OaAttendance>()
                    .between(OaAttendance::getWorkDate, startDate, endDate)
                    .isNotNull(OaAttendance::getClockIn))
            .stream()
            .filter(a -> a.getEmpId() != null && a.getWorkDate() != null)
            .collect(Collectors.groupingBy(
                OaAttendance::getEmpId,
                Collectors.mapping(OaAttendance::getWorkDate, Collectors.collectingAndThen(Collectors.toSet(), Set::size))))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue()));

        Map<Long, Set<LocalDate>> approvedLeaveDatesMap = new HashMap<>();
        List<OaLeaveApply> approvedLeaves = leaveApplyMapper.selectList(
            new LambdaQueryWrapper<OaLeaveApply>()
                .eq(OaLeaveApply::getStatus, 1)
                .le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0)));
        for (OaLeaveApply leave : approvedLeaves) {
            if (leave.getEmpId() == null || leave.getStartTime() == null || leave.getEndTime() == null) continue;

            LocalDate leaveStart = leave.getStartTime().toLocalDate().isBefore(startDate)
                ? startDate : leave.getStartTime().toLocalDate();
            LocalDate leaveEnd = leave.getEndTime().toLocalDate().isAfter(endDate)
                ? endDate : leave.getEndTime().toLocalDate();

            Set<LocalDate> leaveDates = approvedLeaveDatesMap.computeIfAbsent(leave.getEmpId(), key -> new HashSet<>());
            for (LocalDate date = leaveStart; !date.isAfter(leaveEnd); date = date.plusDays(1)) {
                DayOfWeek dow = date.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    leaveDates.add(date);
                }
            }
        }

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysEmployee employee : employees) {
            Long empId = employee.getId();
            long clockedDays = clockedDaysMap.getOrDefault(empId, 0L);
            long approvedLeaveDays = approvedLeaveDatesMap.getOrDefault(empId, Collections.emptySet()).size();
            long absentCount = Math.max(0, requiredWorkdays - clockedDays - approvedLeaveDays);
            if (absentCount <= 0) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empName", employee.getEmpName());
            item.put("absentCount", absentCount);
            item.put("requiredDays", requiredWorkdays);
            item.put("clockedDays", clockedDays);
            item.put("approvedLeaveDays", approvedLeaveDays);
            ranking.add(item);
        }

        ranking.sort((a, b) -> Long.compare((Long) b.get("absentCount"), (Long) a.get("absentCount")));
        return limitRanking(ranking);
    }

    private List<Map<String, Object>> limitRanking(List<Map<String, Object>> ranking) {
        return ranking.size() > 10 ? new ArrayList<>(ranking.subList(0, 10)) : new ArrayList<>(ranking);
    }
}
