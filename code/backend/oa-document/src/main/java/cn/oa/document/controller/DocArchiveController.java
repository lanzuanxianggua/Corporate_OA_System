package cn.oa.document.controller;

import cn.oa.document.service.DocArchiveService;
import cn.oa.document.vo.DocArchiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 档案 Controller (只读).
 */
@Tag(name = "档案管理")
@RestController
@RequestMapping("/api/v1/document/archives")
@RequiredArgsConstructor
public class DocArchiveController {

    private final DocArchiveService service;

    @Operation(summary = "档案详情")
    @GetMapping("/{id}")
    @RequirePermission("document:archive:view")
    public R<DocArchiveVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "档案列表(分页)")
    @GetMapping
    @RequirePermission("document:archive:list")
    public R<PageResult<DocArchiveVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(service.listPage(pageNum, pageSize));
    }
}
