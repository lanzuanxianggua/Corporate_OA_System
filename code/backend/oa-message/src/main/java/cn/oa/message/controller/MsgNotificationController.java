package cn.oa.message.controller;

import cn.oa.common.result.R;
import cn.oa.message.dto.MsgNotificationQueryDTO;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.platform.core.base.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/message/notifications")
@RequiredArgsConstructor
public class MsgNotificationController {

    private final MsgNotificationService msgNotificationService;

    @Operation(summary = "分页查询消息通知")
    @GetMapping
    public R<PageResult<MsgNotificationVO>> pageQuery(@Valid MsgNotificationQueryDTO dto) {
        return R.ok(msgNotificationService.pageQuery(dto.getEmpId(), dto.getPageNum(), dto.getPageSize()));
    }

    @Operation(summary = "获取未读消息数量")
    @GetMapping("/unread-count")
    public R<Long> getUnreadCount(@RequestParam Long empId) {
        return R.ok(msgNotificationService.getUnreadCount(empId));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        msgNotificationService.markRead(id);
        return R.ok();
    }

    @Operation(summary = "标记全部已读")
    @PostMapping("/read-all")
    public R<Void> markAllRead(@RequestParam Long empId) {
        msgNotificationService.markAllRead(empId);
        return R.ok();
    }
}
