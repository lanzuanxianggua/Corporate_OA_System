package cn.oa.workflow.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.workflow.entity.WfDefinition;
import cn.oa.workflow.service.WfDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义 Controller.
 */
@Tag(name = "流程定义")
@RestController
@RequestMapping("/api/v1/workflow/definitions")
public class WfDefinitionController {

    private final WfDefinitionService service;

    public WfDefinitionController(WfDefinitionService service) {
        this.service = service;
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    @RequirePermission("workflow:definition:read")
    public R<WfDefinition> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "启用中的定义")
    @GetMapping("/active")
    public R<List<WfDefinition>> listActive() {
        return R.ok(service.listActive());
    }

    @Operation(summary = "分页")
    @GetMapping
    @RequirePermission("workflow:definition:read")
    public R<?> page(@RequestParam(defaultValue = "1") int pageNo,
                     @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(service.page(pageNo, pageSize));
    }

    @Operation(summary = "创建")
    @PostMapping
    @RequirePermission("workflow:definition:create")
    public R<Long> create(@RequestBody WfDefinition entity) {
        return R.ok(service.create(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @RequirePermission("workflow:definition:update")
    public R<Void> update(@PathVariable Long id, @RequestBody WfDefinition entity) {
        entity.setId(id);
        service.update(entity);
        return R.ok();
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @RequirePermission("workflow:definition:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
