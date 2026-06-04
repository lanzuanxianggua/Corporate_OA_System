package cn.oa.workflow.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.workflow.entity.WfDelegation;
import cn.oa.workflow.service.WfDelegationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 委托 Controller.
 */
@Tag(name = "审批委托")
@RestController
@RequestMapping("/api/v1/workflow/delegations")
public class WfDelegationController {

    private final WfDelegationService service;

    public WfDelegationController(WfDelegationService service) {
        this.service = service;
    }

    @Operation(summary = "新增委托")
    @PostMapping
    @RequirePermission("workflow:delegation:create")
    public R<Long> create(@RequestBody WfDelegation entity) {
        return R.ok(service.create(entity));
    }

    @Operation(summary = "撤销委托")
    @DeleteMapping("/{id}")
    @RequirePermission("workflow:delegation:revoke")
    public R<Void> revoke(@PathVariable Long id) {
        service.revoke(id);
        return R.ok();
    }

    @Operation(summary = "我发起的委托")
    @GetMapping("/outgoing")
    @RequirePermission("workflow:delegation:read")
    public R<List<WfDelegation>> outgoing() {
        return R.ok(service.myOutgoing(UserContext.get().getEmpId()));
    }

    @Operation(summary = "我收到的委托")
    @GetMapping("/incoming")
    @RequirePermission("workflow:delegation:read")
    public R<List<WfDelegation>> incoming() {
        return R.ok(service.myIncoming(UserContext.get().getEmpId()));
    }
}
