package cn.oa.document.controller;

import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.service.DocDispatchService;
import cn.oa.document.vo.DocDispatchVO;
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
 * 发文 Controller.
 */
@Tag(name = "发文管理")
@RestController
@RequestMapping("/api/v1/document/dispatches")
@RequiredArgsConstructor
public class DocDispatchController {

    private final DocDispatchService service;

    @Operation(summary = "创建发文")
    @PostMapping
    @RequirePermission("document:dispatch:create")
    public R<Long> create(@RequestBody @Valid DocDispatchCreateDTO dto) {
        var user = UserContext.get();
        return R.ok(service.create(dto, user.getEmpId(), user.getDeptId()));
    }

    @Operation(summary = "更新发文")
    @PutMapping("/{id}")
    @RequirePermission("document:dispatch:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid DocDispatchCreateDTO dto) {
        service.update(id, dto, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "提交发文审批 (触发工作流)")
    @PostMapping("/{id}/actions/submit")
    @RequirePermission("document:dispatch:submit")
    public R<Long> submit(@PathVariable Long id) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.submit(id, empId));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/actions/approve")
    @RequirePermission("document:dispatch:update")
    public R<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回发文")
    @PostMapping("/{id}/actions/reject")
    @RequirePermission("document:dispatch:update")
    public R<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return R.ok();
    }

    @Operation(summary = "发布")
    @PostMapping("/{id}/actions/publish")
    @RequirePermission("document:dispatch:update")
    public R<Void> publish(@PathVariable Long id) {
        service.publish(id);
        return R.ok();
    }

    @Operation(summary = "归档")
    @PostMapping("/{id}/actions/archive")
    @RequirePermission("document:dispatch:archive")
    public R<Void> archive(@PathVariable Long id) {
        service.archive(id);
        return R.ok();
    }

    @Operation(summary = "删除发文")
    @DeleteMapping("/{id}")
    @RequirePermission("document:dispatch:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "发文详情")
    @GetMapping("/{id}")
    @RequirePermission("document:dispatch:view")
    public R<DocDispatchVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "发文列表(分页)")
    @GetMapping
    @RequirePermission("document:dispatch:list")
    public R<PageResult<DocDispatchVO>> list(DocDispatchQueryDTO query) {
        Long deptId = UserContext.get().getDeptId();
        return R.ok(service.listPage(query, deptId));
    }
}
