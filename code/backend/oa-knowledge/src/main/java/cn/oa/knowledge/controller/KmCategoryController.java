package cn.oa.knowledge.controller;

import cn.oa.knowledge.dto.KmCategoryCreateDTO;
import cn.oa.knowledge.service.KmCategoryService;
import cn.oa.knowledge.vo.KmCategoryVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "知识分类") @RestController
@RequestMapping("/api/v1/knowledge/categories")
@RequiredArgsConstructor
public class KmCategoryController {
    private final KmCategoryService service;

    @PostMapping @Operation(summary = "创建分类")
    @RequirePermission("knowledge:category:create")
    public R<Long> create(@RequestBody @Valid KmCategoryCreateDTO dto) { return R.ok(service.create(dto)); }

    @PutMapping("/{id}") @Operation(summary = "更新分类")
    @RequirePermission("knowledge:category:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid KmCategoryCreateDTO dto) { service.update(id, dto); return R.ok(); }

    @DeleteMapping("/{id}") @Operation(summary = "删除分类")
    @RequirePermission("knowledge:category:delete")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }

    @GetMapping("/tree") @Operation(summary = "分类树")
    @RequirePermission("knowledge:category:list")
    public R<List<KmCategoryVO>> tree() { return R.ok(service.listTree()); }
}