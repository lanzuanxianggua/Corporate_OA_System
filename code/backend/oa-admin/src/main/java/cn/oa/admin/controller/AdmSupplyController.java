package cn.oa.admin.controller;

import cn.oa.admin.entity.AdmSupply;
import cn.oa.admin.entity.AdmSupplyCategory;
import cn.oa.admin.entity.AdmSupplyRequest;
import cn.oa.admin.entity.AdmSupplyRequestItem;
import cn.oa.admin.entity.AdmSupplyStock;
import cn.oa.admin.service.AdmSupplyService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/supplies")
@RequiredArgsConstructor
public class AdmSupplyController {

    private final AdmSupplyService service;

    @PostMapping("/categories")
    @RequirePermission("admin:supply:create")
    public R<Long> createCategory(@RequestBody AdmSupplyCategory category) {
        return R.ok(service.createCategory(category));
    }

    @PutMapping("/categories/{id}")
    @RequirePermission("admin:supply:update")
    public R<Void> updateCategory(@PathVariable Long id, @RequestBody AdmSupplyCategory category) {
        category.setId(id);
        service.updateCategory(category);
        return R.ok();
    }

    @GetMapping("/categories")
    @RequirePermission("admin:supply:list")
    public R<List<AdmSupplyCategory>> listCategories() {
        return R.ok(service.listCategories());
    }

    @PostMapping
    @RequirePermission("admin:supply:create")
    public R<Long> createSupply(@RequestBody AdmSupply supply) {
        return R.ok(service.createSupply(supply));
    }

    @PutMapping("/{id}")
    @RequirePermission("admin:supply:update")
    public R<Void> updateSupply(@PathVariable Long id, @RequestBody AdmSupply supply) {
        supply.setId(id);
        service.updateSupply(supply);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("admin:supply:delete")
    public R<Void> deleteSupply(@PathVariable Long id) {
        service.deleteSupply(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @RequirePermission("admin:supply:list")
    public R<AdmSupply> getSupply(@PathVariable Long id) {
        return R.ok(service.getSupply(id));
    }

    @GetMapping
    @RequirePermission("admin:supply:list")
    public R<Page<AdmSupply>> listSupplies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listSupplies(keyword, status, pn, ps));
    }

    @GetMapping("/{id}/stock")
    @RequirePermission("admin:supply:list")
    public R<AdmSupplyStock> getStock(@PathVariable Long id) {
        return R.ok(service.getStock(id));
    }

    @PostMapping("/{id}/stock-adjustments")
    @RequirePermission("admin:supply:stock")
    public R<Void> adjustStock(@PathVariable Long id, @RequestBody StockAdjustRequest request) {
        service.adjustStock(id, request.getQuantity(), request.getLocation());
        return R.ok();
    }

    @PostMapping("/requests")
    @RequirePermission("admin:supply:request")
    public R<Long> createRequest(@RequestBody SupplyRequestPayload payload) {
        return R.ok(service.createRequest(payload.getRequest(), payload.getItems()));
    }

    @GetMapping("/requests")
    @RequirePermission("admin:supply:request")
    public R<Page<AdmSupplyRequest>> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listRequests(status, pn, ps));
    }

    @GetMapping("/requests/{id}/items")
    @RequirePermission("admin:supply:request")
    public R<List<AdmSupplyRequestItem>> listRequestItems(@PathVariable Long id) {
        return R.ok(service.listRequestItems(id));
    }

    @PostMapping("/requests/{id}/approve")
    @RequirePermission("admin:supply:request")
    public R<Void> approveRequest(@PathVariable Long id) {
        service.approveRequest(id);
        return R.ok();
    }

    @PostMapping("/requests/{id}/reject")
    @RequirePermission("admin:supply:request")
    public R<Void> rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.rejectRequest(id, body.get("reason"));
        return R.ok();
    }

    @Data
    public static class StockAdjustRequest {
        private Integer quantity;
        private String location;
    }

    @Data
    public static class SupplyRequestPayload {
        private AdmSupplyRequest request;
        private List<AdmSupplyRequestItem> items;
    }
}
