package cn.oa.hr.attendance.controller;
import cn.oa.hr.attendance.dto.HrAttendanceRecordCreateDTO;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceRecord;
import cn.oa.hr.attendance.service.HrAttendanceRecordService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="考勤打卡") @RestController @RequestMapping("/api/v1/hr-attendance/records") @RequiredArgsConstructor
public class HrAttendanceRecordController {
    private final HrAttendanceRecordService service;
    @PostMapping("/clock-in") @Operation(summary="签到打卡") @RequirePermission("hr-attendance:record:create")
    public R<Long> clockIn(@RequestBody @Valid HrAttendanceRecordCreateDTO dto) { return R.ok(service.clockIn(dto, UserContext.get().getEmpId())); }
    @PostMapping("/clock-out") @Operation(summary="签退") @RequirePermission("hr-attendance:record:create")
    public R<Long> clockOut() { return R.ok(service.clockOut(UserContext.get().getEmpId())); }
    @GetMapping @Operation(summary="打卡记录") @RequirePermission("hr-attendance:record:list")
    public R<Page<HrAttendanceRecord>> list(HrAttendanceQueryDTO q) { return R.ok(service.listPage(q)); }
}