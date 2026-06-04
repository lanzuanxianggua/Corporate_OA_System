package cn.oa.workflow.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.workflow.entity.WfTask;
import cn.oa.workflow.service.WfTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批任务 Controller.
 */
@Tag(name = "审批任务")
@RestController
@RequestMapping("/api/v1/workflow/tasks")
public class WfTaskController {

    private final WfTaskService service;

    public WfTaskController(WfTaskService service) {
        this.service = service;
    }

    @Operation(summary = "我的待办")
    @GetMapping("/pending")
    @RequirePermission("workflow:task:read")
    public R<List<WfTask>> myPending() {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.myPending(empId));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/{id}")
    @RequirePermission("workflow:task:read")
    public R<WfTask> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "审批")
    @PostMapping("/{id}/approve")
    @RequirePermission("workflow:task:approve")
    public R<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "APPROVE");
        String comment = body.getOrDefault("comment", "");
        Long empId = UserContext.get().getEmpId();
        service.approve(id, empId, action, comment);
        return R.ok();
    }
}
