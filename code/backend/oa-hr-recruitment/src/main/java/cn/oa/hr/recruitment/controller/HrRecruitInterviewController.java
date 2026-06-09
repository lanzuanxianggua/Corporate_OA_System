package cn.oa.hr.recruitment.controller;

import cn.oa.hr.recruitment.entity.HrRecruitInterview;
import cn.oa.hr.recruitment.service.HrRecruitInterviewService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr-recruitment/interviews")
@RequiredArgsConstructor
public class HrRecruitInterviewController {
    private final HrRecruitInterviewService service;

    @PostMapping
    @RequirePermission("hr-recruitment:interview:list")
    public R<Long> create(@RequestBody HrRecruitInterview interview) { return R.ok(service.create(interview)); }

    @PutMapping("/{id}")
    @RequirePermission("hr-recruitment:interview:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrRecruitInterview interview) {
        interview.setId(id);
        service.update(interview);
        return R.ok();
    }

    @GetMapping
    @RequirePermission("hr-recruitment:interview:list")
    public R<Page<HrRecruitInterview>> list(@RequestParam(required = false) Long candidateId,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(candidateId, pn, ps));
    }
}
