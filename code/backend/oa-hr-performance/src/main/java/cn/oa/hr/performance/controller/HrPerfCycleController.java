package cn.oa.hr.performance.controller;
import cn.oa.hr.performance.entity.HrPerfCycle;
import cn.oa.hr.performance.service.HrPerfCycleService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="绩效周期") @RestController @RequestMapping("/api/v1/hr-performance/cycles") @RequiredArgsConstructor
public class HrPerfCycleController {
    private final HrPerfCycleService service;
    @PostMapping @Operation(summary="创建周期") @RequirePermission("hr-performance:cycle:list")
    public R<Long> create(@RequestBody HrPerfCycle c) { return R.ok(service.create(c)); }
    @GetMapping @Operation(summary="周期列表") @RequirePermission("hr-performance:cycle:list")
    public R<Page<HrPerfCycle>> list(@RequestParam(defaultValue="1") int pn, @RequestParam(defaultValue="10") int ps) { return R.ok(service.listPage(pn,ps)); }
}