package cn.oa.hr.employee.controller;

import cn.oa.hr.employee.entity.HrEmployeeProfile;
import cn.oa.hr.employee.service.HrEmployeeProfileService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工档案 Controller.
 */
@Tag(name = "员工档案")
@RestController
@RequestMapping("/api/v1/hr/employees")
@RequiredArgsConstructor
public class HrEmployeeProfileController {

    private final HrEmployeeProfileService service;

    @Operation(summary = "新增员工档案")
    @PostMapping
    @RequirePermission("hr-employee:profile:create")
    public R<Long> create(@RequestBody HrEmployeeProfile profile) {
        return R.ok(service.create(profile));
    }

    @Operation(summary = "修改员工档案")
    @PutMapping("/{id}")
    @RequirePermission("hr-employee:profile:update")
    public R<Void> update(@PathVariable Long id, @RequestBody HrEmployeeProfile patch) {
        service.update(id, patch);
        return R.ok();
    }

    @Operation(summary = "删除员工档案 (逻辑删除)")
    @DeleteMapping("/{id}")
    @RequirePermission("hr-employee:profile:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "员工档案列表")
    @GetMapping
    @RequirePermission("hr-employee:profile:list")
    public R<List<Map<String, Object>>> list(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(service.list(limit));
    }

    @Operation(summary = "员工档案详情")
    @GetMapping("/{id}")
    @RequirePermission("hr-employee:profile:list")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getDetail(id));
    }
}
