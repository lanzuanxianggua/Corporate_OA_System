package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.entity.OaAttendance;
import cn.oa.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "考勤管理")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/clock-in")
    @Operation(summary = "上班打卡")
    public R<Void> clockIn(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        attendanceService.clockIn(empId);
        return R.ok();
    }

    @PostMapping("/clock-out")
    @Operation(summary = "下班打卡")
    public R<Void> clockOut(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        attendanceService.clockOut(empId);
        return R.ok();
    }

    @GetMapping("/today")
    @Operation(summary = "获取今日考勤")
    public R<OaAttendance> today(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        OaAttendance attendance = attendanceService.getTodayAttendance(empId);
        return R.ok(attendance);
    }
}
