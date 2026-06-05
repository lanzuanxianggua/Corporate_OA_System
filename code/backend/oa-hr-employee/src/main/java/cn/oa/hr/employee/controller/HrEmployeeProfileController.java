package cn.oa.hr.employee.controller;

import cn.oa.hr.employee.dto.HrEmployeeProfileCreateDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileQueryDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileUpdateDTO;
import cn.oa.hr.employee.service.HrEmployeeProfileService;
import cn.oa.hr.employee.vo.HrEmployeeProfileVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public R<Long> create(@RequestBody @Valid HrEmployeeProfileCreateDTO dto) {
        return R.ok(service.create(dto));
    }

    @Operation(summary = "修改员工档案")
    @PutMapping("/{id}")
    @RequirePermission("hr-employee:profile:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid HrEmployeeProfileUpdateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除员工档案 (逻辑删除)")
    @DeleteMapping("/{id}")
    @RequirePermission("hr-employee:profile:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "员工档案列表(分页)")
    @GetMapping
    @RequirePermission("hr-employee:profile:list")
    public R<PageResult<HrEmployeeProfileVO>> list(HrEmployeeProfileQueryDTO query) {
        return R.ok(service.listPage(query));
    }

    @Operation(summary = "员工档案详情")
    @GetMapping("/{id}")
    @RequirePermission("hr-employee:profile:list")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getDetail(id));
    }
}
