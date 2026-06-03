package cn.oa.meeting.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.meeting.dto.MtRoomSaveDTO;
import cn.oa.meeting.service.MtRoomService;
import cn.oa.meeting.vo.MtRoomVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting/rooms")
@Tag(name = "会议室管理")
@RequiredArgsConstructor
public class MtRoomController {

    private final MtRoomService roomService;

    @GetMapping("/page")
    @Operation(summary = "分页查询会议室")
    public R<PageResult<MtRoomVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<MtRoomVO> list = roomService.listAll();
        int total = list.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<MtRoomVO> pageList = fromIndex >= total ? List.of() : list.subList(fromIndex, toIndex);
        return R.ok(PageResult.of(total, pageList));
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有会议室")
    public R<List<MtRoomVO>> list() {
        return R.ok(roomService.listAll());
    }

    @GetMapping("/available")
    @Operation(summary = "查询可用会议室")
    public R<List<MtRoomVO>> listAvailable(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        return R.ok(roomService.listAvailable(startTime, endTime));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询会议室详情")
    public R<MtRoomVO> get(@PathVariable Long id) {
        MtRoomVO vo = roomService.getDetail(id);
        return vo != null ? R.ok(vo) : R.fail("会议室不存在");
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "创建会议室")
    @OperationLog(module = "会议管理", operation = "创建会议室")
    public R<Long> create(@RequestBody @Valid MtRoomSaveDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(roomService.create(dto, empId));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "更新会议室")
    @OperationLog(module = "会议管理", operation = "更新会议室")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid MtRoomSaveDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        roomService.update(id, dto, empId);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除会议室")
    @OperationLog(module = "会议管理", operation = "删除会议室")
    public R<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return R.ok();
    }
}
