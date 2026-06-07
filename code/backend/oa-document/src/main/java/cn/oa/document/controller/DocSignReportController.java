package cn.oa.document.controller;

import cn.oa.document.dto.DocSignReportCreateDTO;
import cn.oa.document.dto.DocSignReportQueryDTO;
import cn.oa.document.service.DocSignReportService;
import cn.oa.document.vo.DocSignReportVO;
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
 * 签报 Controller.
 */
@Tag(name = "签报管理")
@RestController
@RequestMapping("/api/v1/document/sign-reports")
@RequiredArgsConstructor
public class DocSignReportController {

    private final DocSignReportService service;

    @Operation(summary = "创建签报")
    @PostMapping
    @RequirePermission("document:sign-report:create")
    public R<Long> create(@RequestBody @Valid DocSignReportCreateDTO dto) {
        var user = UserContext.get();
        return R.ok(service.create(dto, user.getEmpId(), user.getDeptId()));
    }

    @Operation(summary = "更新签报")
    @PutMapping("/{id}")
    @RequirePermission("document:sign-report:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid DocSignReportCreateDTO dto) {
        service.update(id, dto, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "删除签报")
    @DeleteMapping("/{id}")
    @RequirePermission("document:sign-report:update")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "提交签报审批 (触发工作流)")
    @PostMapping("/{id}/actions/submit")
    @RequirePermission("document:sign-report:submit")
    public R<Long> submit(@PathVariable Long id) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.submit(id, empId));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/actions/approve")
    @RequirePermission("document:sign-report:approve")
    public R<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回签报")
    @PostMapping("/{id}/actions/reject")
    @RequirePermission("document:sign-report:approve")
    public R<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return R.ok();
    }

    @Operation(summary = "签报详情")
    @GetMapping("/{id}")
    @RequirePermission("document:sign-report:view")
    public R<DocSignReportVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "签报列表(分页)")
    @GetMapping
    @RequirePermission("document:sign-report:list")
    public R<PageResult<DocSignReportVO>> list(DocSignReportQueryDTO query) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listPage(query, empId));
    }
}
