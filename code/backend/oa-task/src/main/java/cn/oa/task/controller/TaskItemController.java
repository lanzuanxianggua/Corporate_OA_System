package cn.oa.task.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.task.dto.TaskDependencyDTO;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.dto.TaskItemUpdateDTO;
import cn.oa.task.service.TaskItemService;
import cn.oa.task.vo.TaskItemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/task/tasks")
@Tag(name = "任务管理")
public class TaskItemController {

    @Autowired
    private TaskItemService taskItemService;

    @GetMapping("/page")
    @Operation(summary = "分页查询任务")
    public R<PageResult<TaskItemVO>> page(TaskItemQueryDTO dto) {
        IPage<TaskItemVO> page = taskItemService.pageQuery(dto);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询任务详情")
    public R<TaskItemVO> detail(@PathVariable Long id) {
        return R.ok(taskItemService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "创建任务")
    @OperationLog(module = "任务管理", operation = "创建任务")
    public R<Long> create(@RequestBody @Valid TaskItemCreateDTO dto, HttpServletRequest request) {
        Long creatorId = WebUtil.getEmpId(request);
        Long id = taskItemService.create(dto, creatorId);
        log.info("任务创建成功: id={}, title={}", id, dto.getTitle());
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    @OperationLog(module = "任务管理", operation = "更新任务")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid TaskItemUpdateDTO dto) {
        taskItemService.updateItem(id, dto);
        log.info("任务更新成功: id={}", id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新任务状态")
    @OperationLog(module = "任务管理", operation = "更新任务状态")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        taskItemService.updateStatus(id, status);
        log.info("任务状态更新成功: id={}, status={}", id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    @OperationLog(module = "任务管理", operation = "删除任务")
    public R<Void> delete(@PathVariable Long id) {
        taskItemService.deleteItem(id);
        log.info("任务删除成功: id={}", id);
        return R.ok();
    }

    @GetMapping("/{id}/subtasks")
    @Operation(summary = "查询子任务")
    public R<List<TaskItemVO>> subtasks(@PathVariable Long id) {
        return R.ok(taskItemService.getSubtasks(id));
    }

    @PostMapping("/{id}/dependencies")
    @Operation(summary = "添加任务依赖")
    @OperationLog(module = "任务管理", operation = "添加任务依赖")
    public R<Void> addDependency(@PathVariable Long id, @RequestBody @Valid TaskDependencyDTO dto) {
        dto.setTaskId(id);
        taskItemService.addDependency(dto);
        log.info("任务依赖添加成功: taskId={}, dependsOnTaskId={}", id, dto.getDependsOnTaskId());
        return R.ok();
    }

    @DeleteMapping("/{id}/dependencies/{depId}")
    @Operation(summary = "删除任务依赖")
    @OperationLog(module = "任务管理", operation = "删除任务依赖")
    public R<Void> removeDependency(@PathVariable Long id, @PathVariable Long depId) {
        taskItemService.removeDependency(depId);
        log.info("任务依赖删除成功: depId={}", depId);
        return R.ok();
    }
}
