package cn.oa.task.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.task.dto.TaskHoursCreateDTO;
import cn.oa.task.entity.TaskHours;
import cn.oa.task.service.TaskHoursService;
import cn.oa.task.vo.TaskHoursVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/task/hours")
@Tag(name = "工时管理")
public class TaskHoursController {

    @Autowired
    private TaskHoursService taskHoursService;

    @PostMapping
    @Operation(summary = "登记工时")
    @OperationLog(module = "工时管理", operation = "登记工时")
    public R<Void> record(@RequestBody @Valid TaskHoursCreateDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        TaskHours taskHours = new TaskHours();
        BeanUtils.copyProperties(dto, taskHours);
        taskHours.setEmpId(empId);
        taskHoursService.record(taskHours);
        log.info("工时登记成功: taskId={}, hours={}, empId={}", dto.getTaskId(), dto.getHours(), empId);
        return R.ok();
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询任务工时记录")
    public R<List<TaskHours>> getByTask(@PathVariable Long taskId) {
        return R.ok(taskHoursService.getByTask(taskId));
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的工时（支持日期范围）")
    public R<?> getMyHours(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (date != null) {
            // 查询某天明细
            return R.ok(taskHoursService.getByEmpAndDate(empId, date));
        }
        if (startDate != null && endDate != null) {
            // 统计日期范围
            BigDecimal total = taskHoursService.getStats(empId, startDate, endDate);
            return R.ok(total);
        }
        // 默认查询今天
        return R.ok(taskHoursService.getByEmpAndDate(empId, LocalDate.now()));
    }
}
