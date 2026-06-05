package cn.oa.message.controller;

import cn.oa.message.dto.MsgSendDTO;
import cn.oa.message.dto.MsgNotificationQueryDTO;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.message.vo.MsgUnreadCountVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "消息通知") @RestController
@RequestMapping("/api/v1/message/notifications")
@RequiredArgsConstructor
public class MsgNotificationController {
    private final MsgNotificationService service;

    @Operation(summary = "发送消息")
    @PostMapping
    @RequirePermission("message:notification:create")
    public R<Void> send(@RequestBody @Valid MsgSendDTO dto) {
        service.send(dto, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "我的消息列表")
    @GetMapping
    @RequirePermission("message:notification:list")
    public R<List<MsgNotificationVO>> list(MsgNotificationQueryDTO query) {
        return R.ok(service.listByRecipient(UserContext.get().getEmpId(), query));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    @RequirePermission("message:notification:list")
    public R<MsgUnreadCountVO> unreadCount() {
        return R.ok(service.countUnread(UserContext.get().getEmpId()));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    @RequirePermission("message:notification:list")
    public R<Void> markRead(@PathVariable Long id) {
        service.markRead(id, UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/actions/read-all")
    @RequirePermission("message:notification:list")
    public R<Void> markAllRead() {
        service.markAllRead(UserContext.get().getEmpId());
        return R.ok();
    }

    @Operation(summary = "消息详情")
    @GetMapping("/{id}")
    @RequirePermission("message:notification:view")
    public R<MsgNotificationVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }
}