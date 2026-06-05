package cn.oa.hr.leave.controller;

import cn.oa.hr.leave.dto.HrLeaveCreateDTO;
import cn.oa.hr.leave.dto.HrLeaveQueryDTO;
import cn.oa.hr.leave.service.HrLeaveService;
import cn.oa.hr.leave.vo.HrLeaveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 请假申请 Controller.
 */
@Tag(name = "请假申请")
@RestController
@RequestMapping("/api/v1/hr-leave/leaves")
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveService service;

    @Operation(summary = "提交请假")
    @PostMapping
    @RequirePermission("hr-leave:leave:create")
    public R<Long> submit(@RequestBody @Valid HrLeaveCreateDTO dto) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.submit(empId, dto));
    }

    @Operation(summary = "撤回请假")
    @PostMapping("/{id}/actions/revoke")
    @RequirePermission("hr-leave:leave:create")
    public R<Void> revoke(@PathVariable Long id) {
        Long empId = UserContext.get().getEmpId();
        service.revoke(id, empId);
        return R.ok();
    }

    @Operation(summary = "我的请假列表(分页)")
    @GetMapping("/mine")
    @RequirePermission("hr-leave:leave:list")
    public R<PageResult<HrLeaveVO>> myLeaves(HrLeaveQueryDTO query) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listPage(empId, query));
    }

    @Operation(summary = "请假详情")
    @GetMapping("/{id}")
    @RequirePermission("hr-leave:leave:list")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getDetail(id));
    }

    @Operation(summary = "我的假期余额")
    @GetMapping("/balances/me")
    @RequirePermission("hr-leave:leave:list")
    public R<List<Map<String, Object>>> myBalances() {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listMyBalances(empId));
    }
}
