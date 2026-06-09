package cn.oa.finance.controller;

import cn.oa.finance.entity.FinPayment;
import cn.oa.finance.service.FinPaymentService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/payments")
@RequiredArgsConstructor
public class FinPaymentController {
    private final FinPaymentService service;

    @PostMapping @RequirePermission("finance:payment:list")
    public R<Long> create(@RequestBody FinPayment payment) { return R.ok(service.create(payment)); }
    @PutMapping("/{id}") @RequirePermission("finance:payment:list")
    public R<Void> update(@PathVariable Long id, @RequestBody FinPayment payment) { payment.setId(id); service.update(payment); return R.ok(); }
    @DeleteMapping("/{id}") @RequirePermission("finance:payment:list")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @GetMapping("/{id}") @RequirePermission("finance:payment:list")
    public R<FinPayment> get(@PathVariable Long id) { return R.ok(service.getById(id)); }
    @PostMapping("/{id}/submit") @RequirePermission("finance:payment:list")
    public R<Void> submit(@PathVariable Long id) { service.submit(id); return R.ok(); }
    @PostMapping("/{id}/paid") @RequirePermission("finance:payment:list")
    public R<Void> paid(@PathVariable Long id, @RequestBody Map<String, String> body) { service.markPaid(id, body.get("payMethod")); return R.ok(); }
    @GetMapping @RequirePermission("finance:payment:list")
    public R<Page<FinPayment>> list(@RequestParam(required = false) Long contractId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(contractId, status, pn, ps));
    }
}
