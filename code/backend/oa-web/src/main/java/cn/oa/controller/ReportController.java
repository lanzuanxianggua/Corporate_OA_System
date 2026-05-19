package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.R;
import cn.oa.entity.*;
import cn.oa.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@Tag(name = "数据报表")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ===== 个人报表 =====

    @GetMapping("/personal/attendance-summary")
    @Operation(summary = "个人月度考勤汇总")
    public R<PersonalReportVO.AttendanceSummary> personalAttendanceSummary(
            @RequestParam String month, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        return R.ok(reportService.getPersonalAttendanceSummary(empId, month));
    }

    @GetMapping("/personal/attendance-trend")
    @Operation(summary = "个人出勤趋势")
    public R<List<Map<String, Object>>> personalAttendanceTrend(
            @RequestParam(defaultValue = "6") int months, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        String currentMonth = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        PersonalReportVO report = reportService.getPersonalReport(empId, currentMonth, months);
        return R.ok(report.getAttendanceTrend());
    }

    @GetMapping("/personal/leave-summary")
    @Operation(summary = "个人请假统计")
    public R<List<Map<String, Object>>> personalLeaveSummary(
            @RequestParam String month, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        PersonalReportVO report = reportService.getPersonalReport(empId, month, 1);
        return R.ok(report.getLeaveSummary());
    }

    @GetMapping("/personal/monthly-compare")
    @Operation(summary = "个人月度对比")
    public R<PersonalReportVO.MonthlyCompare> personalMonthlyCompare(
            @RequestParam String month, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        PersonalReportVO report = reportService.getPersonalReport(empId, month, 1);
        return R.ok(report.getMonthlyCompare());
    }

    // ===== 管理员报表 =====

    @GetMapping("/admin/attendance-summary")
    @RequireAdmin
    @Operation(summary = "全员考勤汇总")
    public R<AdminReportVO.AttendanceSummary> adminAttendanceSummary(@RequestParam String month) {
        return R.ok(reportService.getAdminAttendanceSummary(month));
    }

    @GetMapping("/admin/dept-compare")
    @RequireAdmin
    @Operation(summary = "部门出勤对比")
    public R<List<Map<String, Object>>> adminDeptCompare(@RequestParam String month) {
        return R.ok((List<Map<String, Object>>) reportService.getDeptCompare(month));
    }

    @GetMapping("/admin/attendance-trend")
    @RequireAdmin
    @Operation(summary = "全员出勤趋势")
    public R<List<Map<String, Object>>> adminAttendanceTrend(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam String month) {
        AdminReportVO report = reportService.getAdminReport(month, months, null);
        return R.ok(report.getAttendanceTrend());
    }

    @GetMapping("/admin/leave-analysis")
    @RequireAdmin
    @Operation(summary = "请假分析")
    public R<List<Map<String, Object>>> adminLeaveAnalysis(@RequestParam String month) {
        return R.ok((List<Map<String, Object>>) reportService.getLeaveAnalysis(month));
    }

    @GetMapping("/admin/employee-ranking")
    @RequireAdmin
    @Operation(summary = "员工出勤排名")
    public R<List<Map<String, Object>>> adminEmployeeRanking(
            @RequestParam String month,
            @RequestParam(defaultValue = "best") String type) {
        return R.ok((List<Map<String, Object>>) reportService.getEmployeeRanking(month, type));
    }

    @GetMapping("/admin/today-overview")
    @RequireAdmin
    @Operation(summary = "今日打卡概览")
    public R<AdminReportVO.TodayOverview> adminTodayOverview() {
        return R.ok(reportService.getTodayOverview());
    }
}
