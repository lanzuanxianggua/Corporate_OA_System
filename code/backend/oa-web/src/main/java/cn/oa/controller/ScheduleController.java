package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaSchedule;
import cn.oa.entity.dto.ScheduleDTO;
import cn.oa.service.ScheduleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/schedule")
@Tag(name = "日程管理")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/page")
    @Operation(summary = "分页查询日程")
    public R<PageResult<OaSchedule>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize,
                                          @RequestParam(required = false) Long empId,
                                          HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        // Non-admin users can only view their own schedules
        if (empId == null || !empId.equals(currentEmpId)) {
            empId = currentEmpId;
        }
        IPage<OaSchedule> page = scheduleService.pageList(pageNum, pageSize, empId);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @Operation(summary = "新增日程")
    @OperationLog(module = "日程管理", operation = "新增日程")
    public R<Void> add(@RequestBody @Valid ScheduleDTO dto) {
        OaSchedule schedule = new OaSchedule();
        schedule.setEmpId(dto.getEmpId());
        schedule.setTitle(dto.getTitle());
        schedule.setContent(dto.getContent());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRemindTime(dto.getRemindTime());
        schedule.setStatus(dto.getStatus());
        scheduleService.save(schedule);
        log.info("Schedule created: empId={}", schedule.getEmpId());
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改日程")
    @OperationLog(module = "日程管理", operation = "修改日程")
    public R<Void> update(@RequestBody @Valid ScheduleDTO dto) {
        OaSchedule schedule = new OaSchedule();
        schedule.setId(dto.getId());
        schedule.setEmpId(dto.getEmpId());
        schedule.setTitle(dto.getTitle());
        schedule.setContent(dto.getContent());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRemindTime(dto.getRemindTime());
        schedule.setStatus(dto.getStatus());
        scheduleService.updateById(schedule);
        log.info("Schedule updated: id={}", dto.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日程")
    @OperationLog(module = "日程管理", operation = "删除日程")
    public R<Void> delete(@PathVariable Long id) {
        scheduleService.removeById(id);
        log.info("Schedule deleted: id={}", id);
        return R.ok();
    }
}
