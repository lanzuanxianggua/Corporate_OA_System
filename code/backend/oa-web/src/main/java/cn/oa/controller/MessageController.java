package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaMessage;
import cn.oa.service.MessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        Long count = messageService.getUnreadCount(empId);
        return R.ok(count);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询消息列表")
    public R<PageResult<OaMessage>> page(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        IPage<OaMessage> page = messageService.pageList(pageNum, pageSize, empId);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/send")
    @RequireAdmin
    @Operation(summary = "发送消息")
    @OperationLog(module = "消息管理", operation = "发送消息")
    public R<Void> send(@RequestBody OaMessage message, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        message.setSenderId(empId);
        if (message.getReceiverId() == null) {
            return R.fail("请输入接收人ID");
        }
        messageService.send(message);
        return R.ok();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记消息已读")
    public R<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        messageService.markAsRead(id, empId);
        return R.ok();
    }
}
