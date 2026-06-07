package cn.oa.finance.controller;

import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanQueryDTO;
import cn.oa.finance.service.FinLoanService;
import cn.oa.finance.vo.FinLoanVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "借款管理")
@RestController
@RequestMapping("/api/v1/finance/loans")
@RequiredArgsConstructor
public class FinLoanController {
    private final FinLoanService service;
    @Operation(summary = "创建借款单")
    @PostMapping
    @RequirePermission("finance:loan:create")
    public R<Long> create(@RequestBody @Valid FinLoanCreateDTO dto) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.create(dto, empId));
    }
    @Operation(summary = "还款")
    @PostMapping("/{id}/actions/repay")
    @RequirePermission("finance:loan:repay")
    public R<Void> repay(@PathVariable Long id, @RequestParam BigDecimal amount, @RequestParam(required = false) Long expenseId) {
        service.repay(id, amount, expenseId);
        return R.ok();
    }
    @Operation(summary = "审批通过借款单")
    @PostMapping("/{id}/actions/approve")
    @RequirePermission("finance:loan:approve")
    public R<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回借款单 (业务层兜底)")
    @PostMapping("/{id}/actions/reject")
    @RequirePermission("finance:loan:approve")
    public R<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return R.ok();
    }
    @Operation(summary = "借款单详情")
    @GetMapping("/{id}")
    @RequirePermission("finance:loan:view")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }
    @Operation(summary = "借款单列表(分页)")
    @GetMapping
    @RequirePermission("finance:loan:list")
    public R<PageResult<FinLoanVO>> list(FinLoanQueryDTO query) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listPage(query, empId));
    }
}