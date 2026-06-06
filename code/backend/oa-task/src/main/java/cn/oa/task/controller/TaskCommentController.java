package cn.oa.task.controller;

import cn.oa.task.dto.TaskCommentCreateDTO;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.service.TaskCommentService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "任务评论") @RestController
@RequestMapping("/api/v1/task/comments")
@RequiredArgsConstructor
public class TaskCommentController {
    private final TaskCommentService service;

    @Operation(summary = "添加评论") @PostMapping
    @RequirePermission("task:item:list")
    public R<Long> create(@RequestBody @Valid TaskCommentCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @Operation(summary = "评论列表") @GetMapping
    @RequirePermission("task:item:list")
    public R<List<TaskComment>> list(@RequestParam Long itemId) {
        return R.ok(service.listByItem(itemId));
    }
}