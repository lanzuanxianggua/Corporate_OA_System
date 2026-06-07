package cn.oa.task.controller;

import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.dto.TaskProjectQueryDTO;
import cn.oa.task.service.TaskProjectService;
import cn.oa.task.vo.TaskProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "项目管理")
@RestController
@RequestMapping("/api/v1/task/projects")
@RequiredArgsConstructor
public class TaskProjectController {
    private final TaskProjectService service;

    @PostMapping
    @Operation(summary = "创建项目")
    @RequirePermission("task:project:create")
    public R<Long> create(@RequestBody @Valid TaskProjectCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    @RequirePermission("task:project:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid TaskProjectCreateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/actions/status")
    @Operation(summary = "修改项目状态")
    @RequirePermission("task:project:update")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    @RequirePermission("task:project:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    @RequirePermission("task:project:view")
    public R<TaskProjectVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "项目列表")
    @RequirePermission("task:project:list")
    public R<PageResult<TaskProjectVO>> list(TaskProjectQueryDTO query) {
        return R.ok(service.listPage(query, UserContext.get().getDeptId()));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的项目 (按负责人过滤)")
    @RequirePermission("task:project:list")
    public R<List<TaskProjectVO>> listMine() {
        return R.ok(service.listMine(UserContext.get().getEmpId()));
    }
}
