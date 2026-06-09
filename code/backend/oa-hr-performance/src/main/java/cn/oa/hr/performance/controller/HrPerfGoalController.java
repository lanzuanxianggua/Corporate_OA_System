package cn.oa.hr.performance.controller;

import cn.oa.hr.performance.entity.HrPerfGoal;
import cn.oa.hr.performance.service.HrPerfGoalService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-performance/goals")
@RequiredArgsConstructor
public class HrPerfGoalController {
    private final HrPerfGoalService service;

    @PostMapping
    @RequirePermission("hr-performance:goal:list")
    public R<Long> create(@RequestBody HrPerfGoal goal) {
        return R.ok(service.create(goal));
    }

    @PutMapping("/{id}")
    @RequirePermission("hr-performance:goal:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrPerfGoal goal) {
        goal.setId(id);
        service.update(goal);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @RequirePermission("hr-performance:cycle:operate")
    public R<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @RequirePermission("hr-performance:goal:list")
    public R<HrPerfGoal> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @GetMapping
    @RequirePermission("hr-performance:goal:list")
    public R<Page<HrPerfGoal>> list(@RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(cycleId, empId, status, pn, ps));
    }
}
