package cn.oa.task.controller;

import cn.oa.common.result.R;
import cn.oa.task.service.TaskItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 甘特图 Controller
 */
@RestController
@RequestMapping("/api/task/projects/{projectId}/gantt")
@Tag(name = "甘特图")
public class TaskGanttController {

    @Autowired
    private TaskItemService taskItemService;

    @GetMapping
    @Operation(summary = "获取甘特图数据")
    public R<Map<String, Object>> ganttData(@PathVariable Long projectId) {
        return R.ok(taskItemService.getGanttData(projectId));
    }
}
