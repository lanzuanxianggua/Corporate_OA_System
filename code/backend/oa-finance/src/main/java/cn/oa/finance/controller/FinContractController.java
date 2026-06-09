package cn.oa.finance.controller;

import cn.oa.finance.entity.FinContract;
import cn.oa.finance.service.FinContractService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/contracts")
@RequiredArgsConstructor
public class FinContractController {
    private final FinContractService service;

    @PostMapping @RequirePermission("finance:contract:list")
    public R<Long> create(@RequestBody FinContract contract) { return R.ok(service.create(contract)); }
    @PutMapping("/{id}") @RequirePermission("finance:contract:list")
    public R<Void> update(@PathVariable Long id, @RequestBody FinContract contract) { contract.setId(id); service.update(contract); return R.ok(); }
    @DeleteMapping("/{id}") @RequirePermission("finance:contract:list")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @GetMapping("/{id}") @RequirePermission("finance:contract:list")
    public R<FinContract> get(@PathVariable Long id) { return R.ok(service.getById(id)); }
    @PostMapping("/{id}/activate") @RequirePermission("finance:contract:list")
    public R<Void> activate(@PathVariable Long id) { service.activate(id); return R.ok(); }
    @PostMapping("/{id}/close") @RequirePermission("finance:contract:list")
    public R<Void> close(@PathVariable Long id) { service.close(id); return R.ok(); }
    @GetMapping @RequirePermission("finance:contract:list")
    public R<Page<FinContract>> list(@RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(deptId, status, pn, ps));
    }
}
