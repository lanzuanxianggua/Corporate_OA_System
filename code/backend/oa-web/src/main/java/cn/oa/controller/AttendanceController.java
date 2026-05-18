package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.common.result.PageResult;
import cn.oa.entity.OaAttendance;
import cn.oa.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/history")
    @Operation(summary = "考勤历史查询")
    public R<List<OaAttendance>> history(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        return R.ok(attendanceService.getAttendanceHistory(empId, startDate, endDate));
    }

    @GetMapping("/admin/page")
    @Operation(summary = "管理员考勤分页查询")
    public R<PageResult<Map<String, Object>>> adminPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String empName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        IPage<Map<String, Object>> page = attendanceService.adminPage(pageNum, pageSize, empName, status, startDate, endDate);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
