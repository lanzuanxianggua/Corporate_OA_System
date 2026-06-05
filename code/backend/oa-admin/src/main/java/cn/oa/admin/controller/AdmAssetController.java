package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.service.AdmAssetService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.oa.admin.vo.AdmAssetVO;

import java.util.Map;

/**
 * 资产管理 Controller.
 */
@Tag(name = "资产管理")
@RestController
@RequestMapping("/api/v1/admin/assets")
@RequiredArgsConstructor
public class AdmAssetController {

    private final AdmAssetService assetService;

    @Operation(summary = "新增资产")
    @PostMapping
    @RequirePermission("admin:asset:create")
    public R<Long> create(@RequestBody @Valid AdmAssetCreateDTO dto) {
        return R.ok(assetService.create(dto));
    }

    @Operation(summary = "修改资产")
    @PutMapping("/{id}")
    @RequirePermission("admin:asset:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AdmAssetCreateDTO dto) {
        assetService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除资产")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:asset:delete")
    public R<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return R.ok();
    }

    @Operation(summary = "资产详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:asset:view")
    public R<AdmAssetVO> getById(@PathVariable Long id) {
        return R.ok(assetService.getById(id));
    }

    @Operation(summary = "资产分页列表")
    @GetMapping
    @RequirePermission("admin:asset:list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(assetService.list(category, status, pageNum, pageSize));
    }
}
