package cn.oa.service.impl;

import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public Map<String, Object> getDashboardStats(String period) {
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
            case "week":
                startDate = today.minusDays(6); // 最近7天
                break;
            case "month":
                startDate = today.withDayOfMonth(1); // 本月1号
                break;
            default:
                startDate = today;
        }

        // 3. 考勤统计
        Map<String, Object> attendance = new LinkedHashMap<>();

        // 今日打卡人数
        Long clockedIn = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .eq(OaAttendance::getWorkDate, today)
                .isNotNull(OaAttendance::getClockIn));
        attendance.put("clockedIn", clockedIn);
        attendance.put("notClockedIn", Math.max(0, employeeTotal - clockedIn));

        // 迟到人数 (clockIn > 09:00)
        LocalDateTime nineOClock = today.atTime(9, 0);
        Long late = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .eq(OaAttendance::getWorkDate, today)
                .gt(OaAttendance::getClockIn, nineOClock));
        attendance.put("late", late);

        // 早退人数 (clockOut < 18:00 且已打卡)
        LocalDateTime sixOClock = today.atTime(18, 0);
        Long earlyLeave = attendanceMapper.selectCount(
            new LambdaQueryWrapper<OaAttendance>()
                .eq(OaAttendance::getWorkDate, today)
                .isNotNull(OaAttendance::getClockOut)
                .lt(OaAttendance::getClockOut, sixOClock));
        attendance.put("earlyLeave", earlyLeave);

        // 缺勤人数
        attendance.put("absent", Math.max(0, employeeTotal - clockedIn));
        result.put("attendance", attendance);

        // 4. 请假统计
        Map<String, Object> leave = new LinkedHashMap<>();

        LambdaQueryWrapper<OaLeaveApply> leaveWrapper = new LambdaQueryWrapper<>();
        // 请假时间段与查询范围有交集
        leaveWrapper.le(OaLeaveApply::getStartTime, endDate.atTime(23, 59, 59))
                     .ge(OaLeaveApply::getEndTime, startDate.atTime(0, 0, 0));

        List<OaLeaveApply> leaveList = leaveApplyMapper.selectList(leaveWrapper);
        leave.put("total", leaveList.size());
        leave.put("pending", leaveList.stream().filter(l -> l.getStatus() == 0).count());
        leave.put("approved", leaveList.stream().filter(l -> l.getStatus() == 1).count());

        // 按类型统计
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<Integer, String> typeNames = Map.of(0, "事假", 1, "病假", 2, "年假", 3, "婚假", 4, "产假", 5, "其他");
        leaveList.stream()
            .collect(Collectors.groupingBy(OaLeaveApply::getLeaveType, Collectors.counting()))
            .forEach((type, count) -> byType.put(typeNames.getOrDefault(type, "其他"), count));
        leave.put("byType", byType);
        result.put("leave", leave);

        // 5. 部门人数分布
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

        // 6. 出勤趋势（最近7天）
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
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

        return result;
    }
}
