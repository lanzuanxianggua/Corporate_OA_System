package cn.oa.finance.controller;

import cn.oa.finance.dto.FinExpenseCreateDTO;
import cn.oa.finance.dto.FinExpenseQueryDTO;
import cn.oa.finance.service.FinExpenseService;
import cn.oa.finance.vo.FinExpenseVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报销 Controller.
 */
@Tag(name = "报销管理")
@RestController
@RequestMapping("/api/v1/finance/expenses")
@RequiredArgsConstructor
public class FinExpenseController {

    private final FinExpenseService service;

    @Operation(summary = "创建报销单")
    @PostMapping
    @RequirePermission("finance:expense:create")
    public R<Long> create(@RequestBody @Valid FinExpenseCreateDTO dto) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.create(dto, empId));
    }

    @Operation(summary = "撤回报销单")
    @PostMapping("/{id}/actions/withdraw")
    @RequirePermission("finance:expense:create")
    public R<Void> withdraw(@PathVariable Long id) {
        Long empId = UserContext.get().getEmpId();
        service.withdraw(id, empId);
        return R.ok();
    }

    @Operation(summary = "审批通过报销单")
    @PostMapping("/{id}/actions/approve")
    @RequirePermission("finance:expense:approve")
    public R<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回报销单")
    @PostMapping("/{id}/actions/reject")
    @RequirePermission("finance:expense:approve")
    public R<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return R.ok();
    }

    @Operation(summary = "报销单详情")
    @GetMapping("/{id}")
    @RequirePermission("finance:expense:view")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "报销单列表(分页)")
    @GetMapping
    @RequirePermission("finance:expense:list")
    public R<PageResult<FinExpenseVO>> list(FinExpenseQueryDTO query) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listPage(query, empId));
    }
}
