package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.annotation.RequireRole;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.utils.ExcelExportUtil;
import cn.oa.entity.OaAttendance;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.AttendanceService;
import cn.oa.vo.AttendanceExportVO;
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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "考勤管理")
@Slf4j
public class AttendanceController {

    private static final String[] STATUS_TEXT = {"正常", "迟到", "早退", "缺勤", "休息", "请假", "出差"};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @PostMapping("/clock-in")
    @Operation(summary = "上班打卡")
    @OperationLog(module = "考勤管理", operation = "上班打卡")
    public R<Void> clockIn(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        attendanceService.clockIn(empId);
        log.info("Clock in: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/clock-out")
    @Operation(summary = "下班打卡")
    @OperationLog(module = "考勤管理", operation = "下班打卡")
    public R<Void> clockOut(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        attendanceService.clockOut(empId);
        log.info("Clock out: empId={}", empId);
        return R.ok();
    }

    @GetMapping("/today")
    @Operation(summary = "获取今日考勤")
    public R<OaAttendance> today(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        OaAttendance attendance = attendanceService.getTodayAttendance(empId);
        return R.ok(attendance);
    }

    @GetMapping("/history")
    @Operation(summary = "考勤历史查询")
    public R<List<OaAttendance>> history(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(attendanceService.getAttendanceHistory(empId, startDate, endDate));
    }

    @GetMapping("/admin/page")
    @RequireRole({"ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"})
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

    @GetMapping("/admin/export")
    @RequireAdmin
    @Operation(summary = "导出考勤数据")
    public void exportAttendance(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();
        List<OaAttendance> records = attendanceService.getHistoryByDateRange(startDate, endDate);
        if (records == null || records.isEmpty()) {
            ExcelExportUtil.export(response, "考勤数据", AttendanceExportVO.class, new ArrayList<>());
            return;
        }
        if (records.size() > 1000) {
            log.warn("Export result count: {}, consider async export", records.size());
        }
        if (records.size() > 5000) {
            records = records.subList(0, 5000);
        }

        Set<Long> empIds = records.stream().map(OaAttendance::getEmpId).collect(Collectors.toSet());
        Map<Long, SysEmployee> empMap = empIds.isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(empIds).stream()
                        .collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<AttendanceExportVO> exportList = new ArrayList<>();
        for (OaAttendance att : records) {
            AttendanceExportVO vo = new AttendanceExportVO();
            SysEmployee emp = empMap.get(att.getEmpId());
            vo.setEmpCode(emp != null ? emp.getEmpCode() : "");
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setWorkDate(att.getWorkDate() != null ? att.getWorkDate().format(DATE_FMT) : "");
            vo.setClockIn(att.getClockIn() != null ? att.getClockIn().format(DATETIME_FMT) : "");
            vo.setClockOut(att.getClockOut() != null ? att.getClockOut().format(DATETIME_FMT) : "");
            vo.setStatusText(att.getStatus() != null && att.getStatus() < STATUS_TEXT.length
                    ? STATUS_TEXT[att.getStatus()] : "未知");
            vo.setRemark(att.getRemark() != null ? att.getRemark() : "");
            exportList.add(vo);
        }

        ExcelExportUtil.export(response, "考勤数据", AttendanceExportVO.class, exportList);
    }
}
