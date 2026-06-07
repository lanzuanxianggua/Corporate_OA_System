package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmAssetLoanCreateDTO;
import cn.oa.admin.dto.AdmAssetLoanQueryDTO;
import cn.oa.admin.service.AdmAssetLoanService;
import cn.oa.admin.vo.AdmAssetLoanVO;
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
 * 资产领用 Controller.
 */
@Tag(name = "资产领用")
@RestController
@RequestMapping("/api/v1/admin/asset-loans")
@RequiredArgsConstructor
public class AdmAssetLoanController {

    private final AdmAssetLoanService service;

    @Operation(summary = "创建领用单 (DRAFT)")
    @PostMapping
    @RequirePermission("admin:asset-loan:create")
    public R<Long> create(@RequestBody @Valid AdmAssetLoanCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @Operation(summary = "提交领用单 (启动工作流)")
    @PostMapping("/{id}/actions/submit")
    @RequirePermission("admin:asset-loan:submit")
    public R<Long> submit(@PathVariable Long id) {
        return R.ok(service.submit(id, UserContext.get().getEmpId()));
    }

    @Operation(summary = "资产归还 (APPROVED -> RETURNED)")
    @PostMapping("/{id}/actions/return")
    @RequirePermission("admin:asset-loan:return")
    public R<Void> returnAsset(@PathVariable Long id) {
        service.returnAsset(id);
        return R.ok();
    }

    @Operation(summary = "删除领用单 (仅 DRAFT)")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:asset-loan:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "领用单详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:asset-loan:view")
    public R<AdmAssetLoanVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "领用单分页列表")
    @GetMapping
    @RequirePermission("admin:asset-loan:list")
    public R<PageResult<AdmAssetLoanVO>> list(AdmAssetLoanQueryDTO query) {
        return R.ok(service.listPage(query, UserContext.get().getEmpId()));
    }
}
