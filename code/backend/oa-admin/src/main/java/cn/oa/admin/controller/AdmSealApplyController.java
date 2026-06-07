package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmSealApplyCreateDTO;
import cn.oa.admin.dto.AdmSealApplyQueryDTO;
import cn.oa.admin.service.AdmSealApplyService;
import cn.oa.admin.vo.AdmSealApplyVO;
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
 * 印章申请 Controller.
 */
@Tag(name = "印章申请")
@RestController
@RequestMapping("/api/v1/admin/seal-applys")
@RequiredArgsConstructor
public class AdmSealApplyController {

    private final AdmSealApplyService service;

    @Operation(summary = "创建印章申请 (DRAFT)")
    @PostMapping
    @RequirePermission("admin:seal-apply:create")
    public R<Long> create(@RequestBody @Valid AdmSealApplyCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @Operation(summary = "提交印章申请 (启动工作流)")
    @PostMapping("/{id}/actions/submit")
    @RequirePermission("admin:seal-apply:submit")
    public R<Long> submit(@PathVariable Long id) {
        return R.ok(service.submit(id, UserContext.get().getEmpId()));
    }

    @Operation(summary = "用印 (APPROVED -> USED)")
    @PostMapping("/{id}/actions/use")
    @RequirePermission("admin:seal-apply:use")
    public R<Void> use(@PathVariable Long id) {
        service.use(id);
        return R.ok();
    }

    @Operation(summary = "归档 (USED -> ARCHIVED)")
    @PostMapping("/{id}/actions/archive")
    @RequirePermission("admin:seal-apply:archive")
    public R<Void> archive(@PathVariable Long id) {
        service.archive(id);
        return R.ok();
    }

    @Operation(summary = "删除申请 (仅 DRAFT)")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:seal-apply:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "印章申请详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:seal-apply:view")
    public R<AdmSealApplyVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "印章申请分页列表")
    @GetMapping
    @RequirePermission("admin:seal-apply:list")
    public R<PageResult<AdmSealApplyVO>> list(AdmSealApplyQueryDTO query) {
        return R.ok(service.listPage(query, UserContext.get().getEmpId()));
    }
}
