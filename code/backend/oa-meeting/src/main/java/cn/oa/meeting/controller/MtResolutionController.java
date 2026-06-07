package cn.oa.meeting.controller;

import cn.oa.meeting.dto.MtResolutionCreateDTO;
import cn.oa.meeting.dto.MtResolutionQueryDTO;
import cn.oa.meeting.service.MtResolutionService;
import cn.oa.meeting.vo.MtResolutionVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会议决议 Controller.
 */
@Tag(name = "会议决议")
@RestController
@RequestMapping("/api/v1/meeting/resolutions")
@RequiredArgsConstructor
public class MtResolutionController {

    private final MtResolutionService service;

    @Operation(summary = "创建会议决议")
    @PostMapping
    @RequirePermission("meeting:resolution:create")
    public R<Long> create(@RequestBody @Valid MtResolutionCreateDTO dto) {
        return R.ok(service.create(dto));
    }

    @Operation(summary = "启动决议 (PENDING -> IN_PROGRESS)")
    @PostMapping("/{id}/actions/start")
    @RequirePermission("meeting:resolution:update")
    public R<Void> start(@PathVariable Long id) {
        service.start(id);
        return R.ok();
    }

    @Operation(summary = "完成决议 (IN_PROGRESS -> COMPLETED)")
    @PostMapping("/{id}/actions/complete")
    @RequirePermission("meeting:resolution:update")
    public R<Void> complete(@PathVariable Long id) {
        service.complete(id);
        return R.ok();
    }

    @Operation(summary = "标记超期决议 (定时/手动)")
    @PostMapping("/actions/mark-overdue")
    @RequirePermission("meeting:resolution:update")
    public R<Integer> markOverdue() {
        return R.ok(service.markOverdue());
    }

    @Operation(summary = "删除决议")
    @DeleteMapping("/{id}")
    @RequirePermission("meeting:resolution:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "决议详情")
    @GetMapping("/{id}")
    @RequirePermission("meeting:resolution:view")
    public R<MtResolutionVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "决议分页列表")
    @GetMapping
    @RequirePermission("meeting:resolution:list")
    public R<PageResult<MtResolutionVO>> list(MtResolutionQueryDTO query) {
        return R.ok(service.listPage(query));
    }
}
