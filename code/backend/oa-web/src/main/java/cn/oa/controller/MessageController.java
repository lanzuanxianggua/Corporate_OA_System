package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.entity.OaMessage;
import cn.oa.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/message")
@Tag(name = "消息管理")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量")
    public R<Long> unreadCount(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        Long count = messageService.getUnreadCount(empId);
        return R.ok(count);
    }

    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public R<Void> send(@RequestBody OaMessage message) {
        messageService.send(message);
        return R.ok();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记消息已读")
    public R<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return R.ok();
    }
}
