package cn.oa.task.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.task.dto.TaskMemberDTO;
import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.dto.TaskProjectQueryDTO;
import cn.oa.task.dto.TaskProjectUpdateDTO;
import cn.oa.task.entity.TaskProject;
import cn.oa.task.service.TaskProjectService;
import cn.oa.task.vo.TaskProjectVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 项目管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/task/projects")
@Tag(name = "项目管理")
public class TaskProjectController {

    @Autowired
    private TaskProjectService taskProjectService;

    @GetMapping("/page")
    @Operation(summary = "分页查询项目")
    public R<PageResult<TaskProject>> page(TaskProjectQueryDTO dto) {
        IPage<TaskProject> page = taskProjectService.pageList(
                dto.getPageNum(), dto.getPageSize(),
                dto.getName(), dto.getStatus(), dto.getOwnerId());
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询项目详情")
    public R<TaskProjectVO> detail(@PathVariable Long id) {
        return R.ok(taskProjectService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "创建项目")
    @OperationLog(module = "项目管理", operation = "创建项目")
    public R<Long> create(@RequestBody @Valid TaskProjectCreateDTO dto, HttpServletRequest request) {
        Long ownerId = WebUtil.getEmpId(request);
        Long id = taskProjectService.create(dto, ownerId);
        log.info("项目创建成功: id={}, name={}", id, dto.getName());
        return R.ok(id);
    }

    @PutMapping
    @Operation(summary = "更新项目")
    @OperationLog(module = "项目管理", operation = "更新项目")
    public R<Void> update(@RequestBody @Valid TaskProjectUpdateDTO dto) {
        taskProjectService.update(dto);
        log.info("项目更新成功: id={}", dto.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    @OperationLog(module = "项目管理", operation = "删除项目")
    public R<Void> delete(@PathVariable Long id) {
        taskProjectService.deleteProject(id);
        log.info("项目删除成功: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "添加项目成员")
    @OperationLog(module = "项目管理", operation = "添加项目成员")
    public R<Void> addMember(@PathVariable Long id, @RequestBody @Valid TaskMemberDTO dto) {
        taskProjectService.addMember(id, dto.getEmpId(), dto.getRole());
        log.info("项目成员添加成功: projectId={}, empId={}", id, dto.getEmpId());
        return R.ok();
    }

    @DeleteMapping("/{id}/members/{empId}")
    @Operation(summary = "移除项目成员")
    @OperationLog(module = "项目管理", operation = "移除项目成员")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long empId) {
        taskProjectService.removeMember(id, empId);
        log.info("项目成员移除成功: projectId={}, empId={}", id, empId);
        return R.ok();
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "刷新项目进度（从子任务汇总）")
    public R<Void> refreshProgress(@PathVariable Long id) {
        taskProjectService.updateProgress(id);
        return R.ok();
    }
}
