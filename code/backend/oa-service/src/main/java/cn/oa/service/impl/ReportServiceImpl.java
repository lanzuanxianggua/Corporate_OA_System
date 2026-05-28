package cn.oa.service.impl;

import cn.oa.common.service.RedisService;
import cn.oa.entity.*;
import cn.oa.vo.AdminReportVO;
import cn.oa.vo.PersonalReportVO;
import cn.oa.mapper.OaAttendanceMapper;
import cn.oa.mapper.OaLeaveApplyMapper;
import cn.oa.mapper.SysDeptMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.ReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("unchecked")
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private OaAttendanceMapper attendanceMapper;

    @Autowired
    private OaLeaveApplyMapper leaveApplyMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public PersonalReportVO.AttendanceSummary getPersonalAttendanceSummary(Long empId, String month) {
        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        return getPersonalAttendanceSummary(empId, ym.atDay(1), ym.atEndOfMonth());
    }

    @Override
    public PersonalReportVO.AttendanceSummary getPersonalAttendanceSummary(Long empId, LocalDate start, LocalDate end) {
        String cacheKey = "cache:report:personal:" + empId + ":" + start + ":" + end;
        PersonalReportVO.AttendanceSummary cached = redisService.getJson(cacheKey, PersonalReportVO.AttendanceSummary.class);
        if (cached != null) return cached;

        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAttendance::getEmpId, empId)
               .between(OaAttendance::getWorkDate, start, end);
        List<OaAttendance> records = attendanceMapper.selectList(wrapper);

        PersonalReportVO.AttendanceSummary summary = new PersonalReportVO.AttendanceSummary();
        int normalDays = 0, lateDays = 0, earlyLeaveDays = 0, absentDays = 0;
        for (OaAttendance a : records) {
            switch (a.getStatus()) {
                case 0 -> normalDays++;
                case 1 -> lateDays++;
                case 2 -> earlyLeaveDays++;
                case 3 -> absentDays++;
                default -> normalDays++;
            }
        }
        int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        summary.setNormalDays(normalDays);
        summary.setLateDays(lateDays);
        summary.setEarlyLeaveDays(earlyLeaveDays);
        summary.setAbsentDays(absentDays);
        summary.setTotalDays(totalDays);
        summary.setAttendanceRate(totalDays > 0 ? Math.round((double) normalDays / totalDays * 10000) / 100.0 : 0);

        redisService.set(cacheKey, summary, 10, TimeUnit.MINUTES);
        return summary;
    }

    @Override
    public PersonalReportVO getPersonalReport(Long empId, String month, int months) {
        PersonalReportVO report = new PersonalReportVO();
        report.setAttendanceSummary(getPersonalAttendanceSummary(empId, month));

        // 出勤趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth current = YearMonth.parse(month, MONTH_FMT);
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String monthStr = ym.format(MONTH_FMT);
            PersonalReportVO.AttendanceSummary s = getPersonalAttendanceSummary(empId, monthStr);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthStr);
            item.put("rate", s.getAttendanceRate());
            item.put("normalDays", s.getNormalDays());
            trend.add(item);
        }
        report.setAttendanceTrend(trend);

        // 请假统计
        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LambdaQueryWrapper<OaLeaveApply> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(OaLeaveApply::getEmpId, empId)
                    .ge(OaLeaveApply::getStartTime, ym.atDay(1).atStartOfDay())
                    .le(OaLeaveApply::getEndTime, ym.atEndOfMonth().atTime(23, 59, 59))
                    .eq(OaLeaveApply::getStatus, 1);
        List<OaLeaveApply> leaves = leaveApplyMapper.selectList(leaveWrapper);
        Map<String, Long> leaveByType = leaves.stream()
                .collect(Collectors.groupingBy(l -> String.valueOf(l.getLeaveType()), Collectors.counting()));
        List<Map<String, Object>> leaveSummary = leaveByType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                }).collect(Collectors.toList());
        report.setLeaveSummary(leaveSummary);

        // 月度对比
        PersonalReportVO.MonthlyCompare compare = new PersonalReportVO.MonthlyCompare();
        YearMonth lastYm = current.minusMonths(1);
        PersonalReportVO.AttendanceSummary lastSummary = getPersonalAttendanceSummary(empId, lastYm.format(MONTH_FMT));
        compare.setCurrentMonthNormal(report.getAttendanceSummary().getNormalDays());
        compare.setLastMonthNormal(lastSummary.getNormalDays());
        compare.setCurrentMonthLate(report.getAttendanceSummary().getLateDays());
        compare.setLastMonthLate(lastSummary.getLateDays());
        compare.setCurrentMonthAbsent(report.getAttendanceSummary().getAbsentDays());
        compare.setLastMonthAbsent(lastSummary.getAbsentDays());
        report.setMonthlyCompare(compare);

        return report;
    }

    @Override
    public AdminReportVO.AttendanceSummary getAdminAttendanceSummary(String month) {
        String cacheKey = "cache:report:admin:attendance:" + month;
        AdminReportVO.AttendanceSummary cached = redisService.getJson(cacheKey, AdminReportVO.AttendanceSummary.class);
        if (cached != null) return cached;

        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(OaAttendance::getWorkDate, start, end);
        List<OaAttendance> records = attendanceMapper.selectList(wrapper);

        AdminReportVO.AttendanceSummary summary = new AdminReportVO.AttendanceSummary();
        long normalCount = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 0).count();
        long lateCount = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
        long earlyLeaveCount = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 2).count();
        long absentCount = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 3).count();
        long total = records.size();

        summary.setTotalRecords(total);
        summary.setNormalCount(normalCount);
        summary.setLateCount(lateCount);
        summary.setEarlyLeaveCount(earlyLeaveCount);
        summary.setAbsentCount(absentCount);
        summary.setAvgAttendanceRate(total > 0 ? Math.round((double) normalCount / total * 10000) / 100.0 : 0);

        redisService.set(cacheKey, summary, 10, TimeUnit.MINUTES);
        return summary;
    }

    @Override
    public AdminReportVO getAdminReport(String month, int months, Long deptId) {
        AdminReportVO report = new AdminReportVO();
        report.setAttendanceSummary(getAdminAttendanceSummary(month));

        // 部门对比
        report.setDeptCompare(getDeptCompare(month));

        // 出勤趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth current = YearMonth.parse(month, MONTH_FMT);
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            String monthStr = ym.format(MONTH_FMT);
            AdminReportVO.AttendanceSummary s = getAdminAttendanceSummary(monthStr);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthStr);
            item.put("rate", s.getAvgAttendanceRate());
            item.put("normalCount", s.getNormalCount());
            trend.add(item);
        }
        report.setAttendanceTrend(trend);

        // 请假分析
        report.setLeaveAnalysis(getLeaveAnalysis(month));

        // 员工排名
        report.setEmployeeRanking(getEmployeeRanking(month, "best"));

        // 今日概览
        report.setTodayOverview(getTodayOverview());

        return report;
    }

    @Override
    public AdminReportVO.TodayOverview getTodayOverview() {
        String cacheKey = "cache:report:admin:today";
        AdminReportVO.TodayOverview cached = redisService.getJson(cacheKey, AdminReportVO.TodayOverview.class);
        if (cached != null) return cached;

        LocalDate today = LocalDate.now();

        long totalEmployees = employeeMapper.selectCount(
                new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));

        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAttendance::getWorkDate, today);
        List<OaAttendance> todayRecords = attendanceMapper.selectList(wrapper);

        long clockedIn = todayRecords.stream().filter(a -> a.getClockIn() != null).count();
        long late = todayRecords.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();

        AdminReportVO.TodayOverview overview = new AdminReportVO.TodayOverview();
        overview.setTotalEmployees(totalEmployees);
        overview.setClockedIn(clockedIn);
        overview.setNotClockedIn(totalEmployees - clockedIn);
        overview.setLate(late);

        redisService.set(cacheKey, overview, 5, TimeUnit.MINUTES);
        return overview;
    }

    @Override
    public List<Map<String, Object>> getDeptCompare(String month) {
        String cacheKey = "cache:report:admin:dept:" + month;
        List<Map<String, Object>> cached = redisService.getJson(cacheKey, new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<SysDept> depts = deptMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SysDept dept : depts) {
            List<SysEmployee> empList = employeeMapper.selectList(
                    new LambdaQueryWrapper<SysEmployee>()
                            .eq(SysEmployee::getDeptId, dept.getId())
                            .eq(SysEmployee::getStatus, 1));
            if (empList.isEmpty()) continue;

            List<Long> empIds = empList.stream().map(SysEmployee::getId).collect(Collectors.toList());
            LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OaAttendance::getEmpId, empIds)
                   .between(OaAttendance::getWorkDate, start, end);
            List<OaAttendance> records = attendanceMapper.selectList(wrapper);

            long normal = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 0).count();
            double rate = records.isEmpty() ? 0 : Math.round((double) normal / records.size() * 10000) / 100.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deptName", dept.getDeptName());
            item.put("totalRecords", records.size());
            item.put("normalCount", normal);
            item.put("rate", rate);
            result.add(item);
        }

        redisService.set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<Map<String, Object>> getLeaveAnalysis(String month) {
        String cacheKey = "cache:report:admin:leave:" + month;
        List<Map<String, Object>> cached = redisService.getJson(cacheKey, new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LambdaQueryWrapper<OaLeaveApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OaLeaveApply::getStartTime, ym.atDay(1).atStartOfDay())
               .le(OaLeaveApply::getEndTime, ym.atEndOfMonth().atTime(23, 59, 59))
               .eq(OaLeaveApply::getStatus, 1);
        List<OaLeaveApply> leaves = leaveApplyMapper.selectList(wrapper);

        Map<String, Long> byType = leaves.stream()
                .collect(Collectors.groupingBy(l -> String.valueOf(l.getLeaveType()), Collectors.counting()));

        List<Map<String, Object>> result = byType.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                }).collect(Collectors.toList());

        redisService.set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public List<Map<String, Object>> getEmployeeRanking(String month, String type) {
        String cacheKey = "cache:report:admin:ranking:" + month + ":" + type;
        List<Map<String, Object>> cached = redisService.getJson(cacheKey, new TypeReference<List<Map<String, Object>>>() {});
        if (cached != null) return cached;

        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<SysEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<SysEmployee>().eq(SysEmployee::getStatus, 1));

        List<Map<String, Object>> rankings = new ArrayList<>();
        for (SysEmployee emp : employees) {
            LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OaAttendance::getEmpId, emp.getId())
                   .between(OaAttendance::getWorkDate, start, end);
            List<OaAttendance> records = attendanceMapper.selectList(wrapper);

            long normal = records.stream().filter(a -> a.getStatus() != null && a.getStatus() == 0).count();
            double rate = records.isEmpty() ? 0 : Math.round((double) normal / records.size() * 10000) / 100.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empId", emp.getId());
            item.put("empName", emp.getEmpName());
            item.put("normalDays", normal);
            item.put("totalDays", records.size());
            item.put("rate", rate);
            rankings.add(item);
        }

        if ("worst".equals(type)) {
            rankings.sort(Comparator.comparingDouble(a -> ((Number) a.get("rate")).doubleValue()));
        } else {
            rankings.sort((a, b) -> Double.compare(((Number) b.get("rate")).doubleValue(), ((Number) a.get("rate")).doubleValue()));
        }

        List<Map<String, Object>> top10 = rankings.stream().limit(10).collect(Collectors.toList());
        redisService.set(cacheKey, top10, 10, TimeUnit.MINUTES);
        return top10;
    }

    @Override
    public void clearPersonalCache(Long empId, String month) {
        redisService.delete("cache:report:personal:" + empId + ":" + month);
    }

    @Override
    public void clearAdminCache(String month) {
        redisService.delete("cache:report:admin:attendance:" + month);
        redisService.delete("cache:report:admin:dept:" + month);
        redisService.delete("cache:report:admin:leave:" + month);
        redisService.delete("cache:report:admin:today");
        redisService.deleteByPattern("cache:report:admin:ranking:*");
    }
}
