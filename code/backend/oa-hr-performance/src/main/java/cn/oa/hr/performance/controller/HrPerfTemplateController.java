package cn.oa.hr.performance.controller;
import cn.oa.hr.performance.entity.HrPerfTemplate;
import cn.oa.hr.performance.service.HrPerfTemplateService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name="绩效模板") @RestController @RequestMapping("/api/v1/hr-performance/templates") @RequiredArgsConstructor
public class HrPerfTemplateController {
    private final HrPerfTemplateService service;
    @PostMapping @Operation(summary="创建模板") @RequirePermission("hr-performance:template:list")
    public R<Long> create(@RequestBody HrPerfTemplate t) { return R.ok(service.create(t)); }
    @PutMapping("/{id}") @Operation(summary="更新模板") @RequirePermission("hr-performance:template:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrPerfTemplate t) { t.setId(id); service.update(t); return R.ok(); }
    @DeleteMapping("/{id}") @Operation(summary="删除模板") @RequirePermission("hr-performance:template:list")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @GetMapping @Operation(summary="模板列表") @RequirePermission("hr-performance:template:list")
    public R<List<HrPerfTemplate>> list(@RequestParam(required=false) String status) { return R.ok(service.list(status)); }
}