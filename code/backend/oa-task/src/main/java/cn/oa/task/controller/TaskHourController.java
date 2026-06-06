package cn.oa.task.controller;

import cn.oa.task.dto.TaskHourCreateDTO;
import cn.oa.task.entity.TaskHour;
import cn.oa.task.service.TaskHourService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "工时管理") @RestController
@RequestMapping("/api/v1/task/hours")
@RequiredArgsConstructor
public class TaskHourController {
    private final TaskHourService service;

    @Operation(summary = "登记工时") @PostMapping
    @RequirePermission("task:hour:create")
    public R<Long> create(@RequestBody @Valid TaskHourCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @Operation(summary = "工时列表") @GetMapping
    @RequirePermission("task:hour:list")
    public R<List<TaskHour>> list(@RequestParam Long itemId) {
        return R.ok(service.listByItem(itemId));
    }
}