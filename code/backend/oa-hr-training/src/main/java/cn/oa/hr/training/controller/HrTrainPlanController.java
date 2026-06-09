package cn.oa.hr.training.controller;

import cn.oa.hr.training.entity.HrTrainPlan;
import cn.oa.hr.training.service.HrTrainPlanService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-training/plans")
@RequiredArgsConstructor
public class HrTrainPlanController {
    private final HrTrainPlanService service;

    @PostMapping @RequirePermission("hr-training:plan:list")
    public R<Long> create(@RequestBody HrTrainPlan plan) { return R.ok(service.create(plan)); }

    @PutMapping("/{id}") @RequirePermission("hr-training:plan:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrTrainPlan plan) {
        plan.setId(id);
        service.update(plan);
        return R.ok();
    }

    @PostMapping("/{id}/publish") @RequirePermission("hr-training:plan:list")
    public R<Void> publish(@PathVariable Long id) { service.publish(id); return R.ok(); }

    @GetMapping @RequirePermission("hr-training:plan:list")
    public R<Page<HrTrainPlan>> list(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(year, status, pn, ps));
    }
}
