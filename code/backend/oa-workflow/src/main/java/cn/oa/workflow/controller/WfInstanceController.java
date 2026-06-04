package cn.oa.workflow.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.entity.WfTask;
import cn.oa.workflow.service.WfInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程实例 Controller.
 */
@Tag(name = "流程实例")
@RestController
@RequestMapping("/api/v1/workflow/instances")
public class WfInstanceController {

    private final WfInstanceService service;

    public WfInstanceController(WfInstanceService service) {
        this.service = service;
    }

    @Operation(summary = "启动流程")
    @PostMapping("/start")
    @RequirePermission("workflow:instance:start")
    public R<Long> start(@RequestBody Map<String, String> body) {
        String defKey = body.get("defKey");
        String businessKey = body.get("businessKey");
        if (defKey == null || defKey.isBlank() || businessKey == null || businessKey.isBlank()) {
            return R.fail(400, "defKey 和 businessKey 必填");
        }
        Long initiator = UserContext.get().getEmpId();
        return R.ok(service.start(defKey, businessKey, initiator));
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    @RequirePermission("workflow:instance:read")
    public R<WfInstance> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "实例下的所有任务")
    @GetMapping("/{id}/tasks")
    @RequirePermission("workflow:instance:read")
    public R<List<WfTask>> getTasks(@PathVariable Long id) {
        return R.ok(service.getTasks(id));
    }
}
