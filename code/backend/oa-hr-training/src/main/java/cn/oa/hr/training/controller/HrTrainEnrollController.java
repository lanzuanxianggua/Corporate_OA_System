package cn.oa.hr.training.controller;

import cn.oa.hr.training.entity.HrTrainEnroll;
import cn.oa.hr.training.entity.HrTrainRecord;
import cn.oa.hr.training.service.HrTrainEnrollService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/hr-training/enrollments")
@RequiredArgsConstructor
public class HrTrainEnrollController {
    private final HrTrainEnrollService service;

    @PostMapping @RequirePermission("hr-training:enroll:list")
    public R<Long> enroll(@RequestBody EnrollRequest request) {
        return R.ok(service.enroll(request.getSessionId(), request.getEmpId()));
    }

    @PostMapping("/{id}/sign-in") @RequirePermission("hr-training:enroll:list")
    public R<Void> signIn(@PathVariable Long id) { service.signIn(id); return R.ok(); }

    @PostMapping("/{id}/score") @RequirePermission("hr-training:enroll:list")
    public R<Void> score(@PathVariable Long id, @RequestBody ScoreRequest request) {
        service.score(id, request.getScore());
        return R.ok();
    }

    @GetMapping @RequirePermission("hr-training:enroll:list")
    public R<Page<HrTrainEnroll>> list(@RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long empId,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(sessionId, empId, pn, ps));
    }

    @GetMapping("/records") @RequirePermission("hr-training:record:list")
    public R<Page<HrTrainRecord>> records(@RequestParam(required = false) Long empId,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listRecords(empId, pn, ps));
    }

    @Data
    public static class EnrollRequest {
        private Long sessionId;
        private Long empId;
    }

    @Data
    public static class ScoreRequest {
        private BigDecimal score;
    }
}
