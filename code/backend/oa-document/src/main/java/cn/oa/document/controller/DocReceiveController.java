package cn.oa.document.controller;

import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveQueryDTO;
import cn.oa.document.service.DocReceiveService;
import cn.oa.document.vo.DocReceiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收文 Controller.
 */
@Tag(name = "收文管理")
@RestController
@RequestMapping("/api/v1/document/receives")
@RequiredArgsConstructor
public class DocReceiveController {

    private final DocReceiveService service;

    @Operation(summary = "登记收文")
    @PostMapping
    @RequirePermission("document:receive:create")
    public R<Long> create(@RequestBody @Valid DocReceiveCreateDTO dto) {
        Long deptId = UserContext.get().getDeptId();
        return R.ok(service.create(dto, deptId));
    }

    @Operation(summary = "更新收文")
    @PutMapping("/{id}")
    @RequirePermission("document:receive:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid DocReceiveCreateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "拟办 (设置拟办意见 + 状态置 PROCESSING)")
    @PostMapping("/{id}/actions/process")
    @RequirePermission("document:receive:update")
    public R<Void> process(@PathVariable Long id, @RequestBody Map<String, String> body) {
        service.process(id, body.get("opinion"));
        return R.ok();
    }

    @Operation(summary = "办结")
    @PostMapping("/{id}/actions/complete")
    @RequirePermission("document:receive:update")
    public R<Void> complete(@PathVariable Long id) {
        service.complete(id);
        return R.ok();
    }

    @Operation(summary = "归档收文")
    @PostMapping("/{id}/actions/archive")
    @RequirePermission("document:receive:archive")
    public R<Void> archive(@PathVariable Long id) {
        service.archive(id);
        return R.ok();
    }

    @Operation(summary = "收文详情")
    @GetMapping("/{id}")
    @RequirePermission("document:receive:view")
    public R<DocReceiveVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "收文列表(分页)")
    @GetMapping
    @RequirePermission("document:receive:list")
    public R<PageResult<DocReceiveVO>> list(DocReceiveQueryDTO query) {
        Long deptId = UserContext.get().getDeptId();
        return R.ok(service.listPage(query, deptId));
    }
}
