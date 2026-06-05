package cn.oa.hr.leave.controller;

import cn.oa.hr.leave.service.HrLeaveService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 请假申请 Controller.
 */
@Tag(name = "请假申请")
@RestController
@RequestMapping("/api/v1/hr/leaves")
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveService service;

    @Operation(summary = "提交请假")
    @PostMapping
    @RequirePermission("hr-leave:leave:create")
    public R<Long> submit(@RequestBody Map<String, String> body) {
        Long empId = UserContext.get().getEmpId();
        String leaveType = body.get("leaveType");
        String startDate = body.get("startDate");
        String endDate = body.get("endDate");
        String reason = body.getOrDefault("reason", "");
        if (leaveType == null || startDate == null || endDate == null) {
            return R.fail(400, "leaveType/startDate/endDate 必填");
        }
        return R.ok(service.submit(empId, leaveType, startDate, endDate, reason));
    }

    @Operation(summary = "我的请假列表")
    @GetMapping("/mine")
    @RequirePermission("hr-leave:leave:list")
    public R<List<Map<String, Object>>> myLeaves(@RequestParam(defaultValue = "20") int limit) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listByEmpId(empId, Math.min(limit, 100)));
    }

    @Operation(summary = "请假详情")
    @GetMapping("/{id}")
    @RequirePermission("hr-leave:leave:list")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getDetail(id));
    }
}
