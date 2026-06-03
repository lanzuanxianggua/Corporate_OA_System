package cn.oa.task.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.task.dto.TaskCommentCreateDTO;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.service.TaskCommentService;
import cn.oa.task.vo.TaskCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/task/comments")
@Tag(name = "评论管理")
public class TaskCommentController {

    @Autowired
    private TaskCommentService taskCommentService;

    @PostMapping
    @Operation(summary = "添加评论")
    @OperationLog(module = "评论管理", operation = "添加评论")
    public R<Void> addComment(@RequestBody @Valid TaskCommentCreateDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        TaskComment comment = new TaskComment();
        BeanUtils.copyProperties(dto, comment);
        comment.setEmpId(empId);
        taskCommentService.addComment(comment);
        log.info("评论添加成功: taskId={}, empId={}", dto.getTaskId(), empId);
        return R.ok();
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询任务评论")
    public R<List<TaskComment>> getComments(@PathVariable Long taskId) {
        return R.ok(taskCommentService.getComments(taskId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    @OperationLog(module = "评论管理", operation = "删除评论")
    public R<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        taskCommentService.deleteComment(id, empId);
        log.info("评论删除成功: id={}, empId={}", id, empId);
        return R.ok();
    }
}
