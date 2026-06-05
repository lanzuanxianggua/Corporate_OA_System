package cn.oa.finance.controller;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.service.FinBudgetService;
import cn.oa.finance.vo.FinBudgetVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预算 Controller.
 */
@Tag(name = "预算管理")
@RestController
@RequestMapping("/api/v1/finance/budgets")
@RequiredArgsConstructor
public class FinBudgetController {

    private final FinBudgetService service;

    @Operation(summary = "创建预算")
    @PostMapping
    @RequirePermission("finance:budget:create")
    public R<Long> create(@RequestBody @Valid FinBudgetCreateDTO dto) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.create(dto, empId));
    }

    @Operation(summary = "更新预算")
    @PutMapping("/{id}")
    @RequirePermission("finance:budget:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid FinBudgetCreateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除预算")
    @DeleteMapping("/{id}")
    @RequirePermission("finance:budget:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "预算详情")
    @GetMapping("/{id}")
    @RequirePermission("finance:budget:view")
    public R<FinBudget> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "预算列表(分页)")
    @GetMapping
    @RequirePermission("finance:budget:list")
    public R<PageResult<FinBudgetVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long deptId = UserContext.get().getDeptId();
        return R.ok(service.listPage(pageNum, pageSize, deptId));
    }
}
