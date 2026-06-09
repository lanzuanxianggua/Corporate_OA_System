package cn.oa.hr.recruitment.controller;

import cn.oa.hr.recruitment.entity.HrRecruitOffer;
import cn.oa.hr.recruitment.service.HrRecruitOfferService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hr-recruitment/offers")
@RequiredArgsConstructor
public class HrRecruitOfferController {
    private final HrRecruitOfferService service;

    @PostMapping
    @RequirePermission("hr-recruitment:offer:list")
    public R<Long> create(@RequestBody HrRecruitOffer offer) { return R.ok(service.create(offer)); }

    @PutMapping("/{id}")
    @RequirePermission("hr-recruitment:offer:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrRecruitOffer offer) {
        offer.setId(id);
        service.update(offer);
        return R.ok();
    }

    @PostMapping("/{id}/accept")
    @RequirePermission("hr-recruitment:offer:list")
    public R<Void> accept(@PathVariable Long id) { service.accept(id); return R.ok(); }

    @PostMapping("/{id}/reject")
    @RequirePermission("hr-recruitment:offer:list")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.reject(id, body.get("reason"));
        return R.ok();
    }

    @PostMapping("/{id}/onboard")
    @RequirePermission("hr-recruitment:offer:list")
    public R<Void> onboard(@PathVariable Long id) { service.onboard(id); return R.ok(); }

    @GetMapping
    @RequirePermission("hr-recruitment:offer:list")
    public R<Page<HrRecruitOffer>> list(@RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pn,
            @RequestParam(defaultValue = "10") int ps) {
        return R.ok(service.listPage(candidateId, status, pn, ps));
    }
}
