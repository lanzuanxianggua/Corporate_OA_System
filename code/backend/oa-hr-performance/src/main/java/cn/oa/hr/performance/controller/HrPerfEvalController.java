package cn.oa.hr.performance.controller;

import cn.oa.hr.performance.entity.HrPerfEval;
import cn.oa.hr.performance.service.HrPerfEvalService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-performance/evals")
@RequiredArgsConstructor
public class HrPerfEvalController {
    private final HrPerfEvalService service;

    @PostMapping
    @RequirePermission("hr-performance:eval:list")
    public R<Long> create(@RequestBody HrPerfEval eval) {
        return R.ok(service.create(eval));
    }

    @PutMapping("/{id}")
    @RequirePermission("hr-performance:eval:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrPerfEval eval) {
        eval.setId(id);
        service.update(eval);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @RequirePermission("hr-performance:cycle:operate")
    public R<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return R.ok();
    }

    @GetMapping
    @RequirePermission("hr-performance:eval:list")
    public R<Page<HrPerfEval>> list(@RequestParam(required = false) Long goalId,
            @RequestParam(required = false) Long evaluatorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(goalId, evaluatorId, status, pn, ps));
    }
}
