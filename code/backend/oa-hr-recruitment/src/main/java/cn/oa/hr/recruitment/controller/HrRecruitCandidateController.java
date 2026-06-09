package cn.oa.hr.recruitment.controller;

import cn.oa.hr.recruitment.entity.HrRecruitCandidate;
import cn.oa.hr.recruitment.service.HrRecruitCandidateService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-recruitment/candidates")
@RequiredArgsConstructor
public class HrRecruitCandidateController {
    private final HrRecruitCandidateService service;

    @PostMapping
    @RequirePermission("hr-recruitment:candidate:list")
    public R<Long> create(@RequestBody HrRecruitCandidate candidate) { return R.ok(service.create(candidate)); }

    @PutMapping("/{id}")
    @RequirePermission("hr-recruitment:candidate:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrRecruitCandidate candidate) {
        candidate.setId(id);
        service.update(candidate);
        return R.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("hr-recruitment:candidate:list")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        service.updateStatus(id, status);
        return R.ok();
    }

    @GetMapping("/{id}")
    @RequirePermission("hr-recruitment:candidate:list")
    public R<HrRecruitCandidate> get(@PathVariable Long id) { return R.ok(service.getById(id)); }

    @GetMapping
    @RequirePermission("hr-recruitment:candidate:list")
    public R<Page<HrRecruitCandidate>> list(@RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(jobId, status, pn, ps));
    }
}
