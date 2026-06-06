package cn.oa.knowledge.controller;

import cn.oa.knowledge.dto.KmEntryCreateDTO;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.entity.KmEntry;
import cn.oa.knowledge.service.KmEntryService;
import cn.oa.knowledge.vo.KmEntryVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "知识条目") @RestController
@RequestMapping("/api/v1/knowledge/entries")
@RequiredArgsConstructor
public class KmEntryController {
    private final KmEntryService service;

    @PostMapping @Operation(summary = "创建条目")
    @RequirePermission("knowledge:entry:create")
    public R<Long> create(@RequestBody @Valid KmEntryCreateDTO dto) { return R.ok(service.create(dto, UserContext.get().getEmpId())); }

    @PutMapping("/{id}") @Operation(summary = "更新条目")
    @RequirePermission("knowledge:entry:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid KmEntryCreateDTO dto) { service.update(id, dto, UserContext.get().getEmpId()); return R.ok(); }

    @PostMapping("/{id}/actions/publish") @Operation(summary = "发布")
    @RequirePermission("knowledge:entry:update")
    public R<Void> publish(@PathVariable Long id) { service.publish(id); return R.ok(); }

    @PostMapping("/{id}/actions/archive") @Operation(summary = "归档")
    @RequirePermission("knowledge:entry:update")
    public R<Void> archive(@PathVariable Long id) { service.archive(id); return R.ok(); }

    @DeleteMapping("/{id}") @Operation(summary = "删除条目")
    @RequirePermission("knowledge:entry:delete")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }

    @GetMapping("/{id}") @Operation(summary = "条目详情")
    @RequirePermission("knowledge:entry:view")
    public R<KmEntryVO> get(@PathVariable Long id) { return R.ok(service.getById(id)); }

    @GetMapping @Operation(summary = "条目列表")
    @RequirePermission("knowledge:entry:list")
    public R<Page<KmEntry>> list(KmEntryQueryDTO query) { return R.ok(service.listPage(query)); }

    @GetMapping("/search") @Operation(summary = "搜索条目")
    @RequirePermission("knowledge:entry:list")
    public R<List<KmEntry>> search(@RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) { return R.ok(service.search(keyword, limit)); }
}