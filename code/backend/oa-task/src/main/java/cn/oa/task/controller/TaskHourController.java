package cn.oa.task.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.task.dto.TaskHourCreateDTO;
import cn.oa.task.service.TaskHourService;
import cn.oa.task.vo.TaskHourVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "工时管理")
@RestController
@RequestMapping("/api/v1/task/hours")
@RequiredArgsConstructor
public class TaskHourController {
    private final TaskHourService service;

    @PostMapping
    @Operation(summary = "登记工时")
    @RequirePermission("task:hour:create")
    public R<Long> create(@RequestBody @Valid TaskHourCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工时")
    @RequirePermission("task:hour:create")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @GetMapping
    @Operation(summary = "工时列表 (按任务)")
    @RequirePermission("task:hour:list")
    public R<List<TaskHourVO>> listByItem(@RequestParam Long itemId) {
        return R.ok(service.listByItem(itemId));
    }

    @GetMapping("/me")
    @Operation(summary = "我的工时 (按日期范围)")
    @RequirePermission("task:hour:list")
    public R<List<TaskHourVO>> listMy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(service.listByEmpAndDateRange(UserContext.get().getEmpId(), from, to));
    }
}
