package cn.oa.hr.attendance.controller;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceStat;
import cn.oa.hr.attendance.service.HrAttendanceStatService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="考勤统计") @RestController @RequestMapping("/api/v1/hr-attendance/stats") @RequiredArgsConstructor
public class HrAttendanceStatController {
    private final HrAttendanceStatService service;
    @GetMapping @Operation(summary="统计列表") @RequirePermission("hr-attendance:stat:list")
    public R<Page<HrAttendanceStat>> list(HrAttendanceQueryDTO q) { return R.ok(service.listPage(q)); }
}