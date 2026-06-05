package cn.oa.meeting.controller;

import cn.oa.meeting.dto.MtRoomCreateDTO;
import cn.oa.meeting.dto.MtRoomQueryDTO;
import cn.oa.meeting.service.MtRoomService;
import cn.oa.meeting.vo.MtRoomVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会议室 Controller.
 */
@Tag(name = "会议室管理")
@RestController
@RequestMapping("/api/v1/meeting/rooms")
@RequiredArgsConstructor
public class MtRoomController {

    private final MtRoomService service;

    @Operation(summary = "创建会议室")
    @PostMapping
    @RequirePermission("meeting:room:create")
    public R<Long> create(@RequestBody @Valid MtRoomCreateDTO dto) {
        return R.ok(service.create(dto));
    }

    @Operation(summary = "更新会议室")
    @PutMapping("/{id}")
    @RequirePermission("meeting:room:update")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid MtRoomCreateDTO dto) {
        service.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除会议室")
    @DeleteMapping("/{id}")
    @RequirePermission("meeting:room:delete")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "查询会议室详情")
    @GetMapping("/{id}")
    @RequirePermission("meeting:room:list")
    public R<MtRoomVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "分页查询会议室列表")
    @GetMapping
    @RequirePermission("meeting:room:list")
    public R<PageResult<MtRoomVO>> list(MtRoomQueryDTO query) {
        return R.ok(service.listPage(query));
    }
}
