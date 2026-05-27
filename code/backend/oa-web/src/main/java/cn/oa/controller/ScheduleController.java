package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaSchedule;
import cn.oa.service.ScheduleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@Tag(name = "日程管理")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/page")
    @Operation(summary = "分页查询日程")
    public R<PageResult<OaSchedule>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize,
                                          @RequestParam(required = false) Long empId,
                                          HttpServletRequest request) {
        // 非管理员只能查看自己的日程
        if (empId != null) {
            Long currentEmpId = (Long) request.getAttribute("empId");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) redisTemplate.opsForValue().get("roles:" + currentEmpId);
            boolean isAdmin = roles != null && roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r));
            if (!isAdmin && !empId.equals(currentEmpId)) {
                empId = currentEmpId;
            }
        }
        if (empId == null) {
            empId = (Long) request.getAttribute("empId");
        }
        IPage<OaSchedule> page = scheduleService.pageList(pageNum, pageSize, empId);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @Operation(summary = "新增日程")
    @OperationLog(module = "日程管理", operation = "新增日程")
    public R<Void> add(@RequestBody OaSchedule schedule) {
        scheduleService.save(schedule);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改日程")
    @OperationLog(module = "日程管理", operation = "修改日程")
    public R<Void> update(@RequestBody OaSchedule schedule) {
        scheduleService.updateById(schedule);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除日程")
    @OperationLog(module = "日程管理", operation = "删除日程")
    public R<Void> delete(@PathVariable Long id) {
        scheduleService.removeById(id);
        return R.ok();
    }
}
