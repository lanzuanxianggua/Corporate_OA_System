package cn.oa.hr.performance.controller;
import cn.oa.hr.performance.entity.HrPerfResult;
import cn.oa.hr.performance.service.HrPerfResultService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="绩效结果") @RestController @RequestMapping("/api/v1/hr-performance/results") @RequiredArgsConstructor
public class HrPerfResultController {
    private final HrPerfResultService service;
    @GetMapping @Operation(summary="结果列表") @RequirePermission("hr-performance:result:list")
    public R<Page<HrPerfResult>> list(@RequestParam(required=false) Long cycleId, @RequestParam(required=false) Long empId,
        @RequestParam(defaultValue="1") int pn, @RequestParam(defaultValue="10") int ps) { return R.ok(service.listPage(cycleId, empId, pn, ps)); }
}