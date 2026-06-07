package cn.oa.task.controller;

import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.service.TaskItemService;
import cn.oa.task.vo.TaskItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/api/v1/task/items")
@RequiredArgsConstructor
public class TaskItemController {
    private final TaskItemService service;

    @PostMapping
    @Operation(summary = "创建任务")
    @RequirePermission("task:item:create")
    public R<Long> create(@RequestBody @Valid TaskItemCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    @RequirePermission("task:item:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid TaskItemCreateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/actions/assign")
    @Operation(summary = "分配负责人")
    @RequirePermission("task:item:assign")
    public R<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        service.assign(id, body.get("assigneeId"));
        return R.ok();
    }

    @PutMapping("/{id}/actions/status")
    @Operation(summary = "更改状态")
    @RequirePermission("task:item:update")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @PutMapping("/{id}/actions/progress")
    @Operation(summary = "更新进度")
    @RequirePermission("task:item:update")
    public R<Void> updateProgress(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        service.updateProgress(id, body.get("progress"));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    @RequirePermission("task:item:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    @RequirePermission("task:item:view")
    public R<TaskItemVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "任务列表")
    @RequirePermission("task:item:list")
    public R<PageResult<TaskItemVO>> list(TaskItemQueryDTO query) {
        return R.ok(service.listPage(query));
    }
}
