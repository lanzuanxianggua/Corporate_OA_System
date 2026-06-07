package cn.oa.meeting.controller;

import cn.oa.meeting.dto.MtMeetingCreateDTO;
import cn.oa.meeting.dto.MtMeetingQueryDTO;
import cn.oa.meeting.service.MtMeetingService;
import cn.oa.meeting.vo.MtMeetingVO;
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
 * 会议记录 Controller.
 */
@Tag(name = "会议记录")
@RestController
@RequestMapping("/api/v1/meeting/meetings")
@RequiredArgsConstructor
public class MtMeetingController {

    private final MtMeetingService service;

    @Operation(summary = "创建会议记录")
    @PostMapping
    @RequirePermission("meeting:meeting:create")
    public R<Long> create(@RequestBody @Valid MtMeetingCreateDTO dto) {
        return R.ok(service.create(dto, UserContext.get().getEmpId()));
    }

    @Operation(summary = "开始会议 (SCHEDULED -> IN_PROGRESS)")
    @PostMapping("/{id}/actions/start")
    @RequirePermission("meeting:meeting:update")
    public R<Void> start(@PathVariable Long id) {
        service.start(id);
        return R.ok();
    }

    @Operation(summary = "完成会议 (IN_PROGRESS -> COMPLETED)")
    @PostMapping("/{id}/actions/complete")
    @RequirePermission("meeting:meeting:update")
    public R<Void> complete(@PathVariable Long id, @RequestBody(required = false) MtMeetingCreateDTO body) {
        service.complete(id, body == null ? null : body.getSummary());
        return R.ok();
    }

    @Operation(summary = "取消会议")
    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("meeting:meeting:update")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @Operation(summary = "删除会议")
    @DeleteMapping("/{id}")
    @RequirePermission("meeting:meeting:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "会议详情")
    @GetMapping("/{id}")
    @RequirePermission("meeting:meeting:view")
    public R<MtMeetingVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "会议记录分页列表")
    @GetMapping
    @RequirePermission("meeting:meeting:list")
    public R<PageResult<MtMeetingVO>> list(MtMeetingQueryDTO query) {
        return R.ok(service.listPage(query));
    }
}
