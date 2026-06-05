package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmSealCreateDTO;
import cn.oa.admin.service.AdmSealService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.oa.admin.vo.AdmSealVO;

import java.util.Map;

/**
 * 印章管理 Controller.
 */
@Tag(name = "印章管理")
@RestController
@RequestMapping("/api/v1/admin/seals")
@RequiredArgsConstructor
public class AdmSealController {

    private final AdmSealService sealService;

    @Operation(summary = "新增印章")
    @PostMapping
    @RequirePermission("admin:seal:create")
    public R<Long> create(@RequestBody @Valid AdmSealCreateDTO dto) {
        return R.ok(sealService.create(dto));
    }

    @Operation(summary = "修改印章")
    @PutMapping("/{id}")
    @RequirePermission("admin:seal:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AdmSealCreateDTO dto) {
        sealService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除印章")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:seal:delete")
    public R<Void> delete(@PathVariable Long id) {
        sealService.delete(id);
        return R.ok();
    }

    @Operation(summary = "印章详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:seal:view")
    public R<AdmSealVO> getById(@PathVariable Long id) {
        return R.ok(sealService.getById(id));
    }

    @Operation(summary = "印章分页列表")
    @GetMapping
    @RequirePermission("admin:seal:list")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(sealService.list(deptId, pageNum, pageSize));
    }
}
