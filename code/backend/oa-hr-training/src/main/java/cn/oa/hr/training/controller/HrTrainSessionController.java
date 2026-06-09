package cn.oa.hr.training.controller;

import cn.oa.hr.training.entity.HrTrainSession;
import cn.oa.hr.training.service.HrTrainSessionService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-training/sessions")
@RequiredArgsConstructor
public class HrTrainSessionController {
    private final HrTrainSessionService service;

    @PostMapping @RequirePermission("hr-training:session:list")
    public R<Long> create(@RequestBody HrTrainSession session) { return R.ok(service.create(session)); }

    @PutMapping("/{id}") @RequirePermission("hr-training:session:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrTrainSession session) {
        session.setId(id);
        service.update(session);
        return R.ok();
    }

    @PostMapping("/{id}/start") @RequirePermission("hr-training:session:list")
    public R<Void> start(@PathVariable Long id) { service.start(id); return R.ok(); }

    @PostMapping("/{id}/close") @RequirePermission("hr-training:session:list")
    public R<Void> close(@PathVariable Long id) { service.close(id); return R.ok(); }

    @GetMapping @RequirePermission("hr-training:session:list")
    public R<Page<HrTrainSession>> list(@RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(planId, status, pn, ps));
    }
}
