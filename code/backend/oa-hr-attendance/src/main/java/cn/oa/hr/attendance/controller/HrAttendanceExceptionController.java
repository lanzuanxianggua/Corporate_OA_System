package cn.oa.hr.attendance.controller;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceException;
import cn.oa.hr.attendance.service.HrAttendanceExceptionService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@Tag(name="考勤异常") @RestController @RequestMapping("/api/v1/hr-attendance/exceptions") @RequiredArgsConstructor
public class HrAttendanceExceptionController {
    private final HrAttendanceExceptionService service;
    @GetMapping @Operation(summary="异常列表") @RequirePermission("hr-attendance:exception:list")
    public R<Page<HrAttendanceException>> list(HrAttendanceQueryDTO q) { return R.ok(service.listPage(q)); }
    @PostMapping("/{id}/actions/appeal") @Operation(summary="申诉")
    public R<Void> appeal(@PathVariable Long id, @RequestBody Map<String,String> body) { service.appeal(id, body.get("content")); return R.ok(); }
    @PostMapping("/{id}/actions/handle") @Operation(summary="处理异常") @RequirePermission("hr-attendance:exception:handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String,String> body) { service.handle(id, body.get("status"), body.get("comment"), UserContext.get().getEmpId()); return R.ok(); }
}