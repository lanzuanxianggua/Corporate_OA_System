package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaMessage;
import cn.oa.service.MessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/message")
@Tag(name = "消息管理")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量")
    public R<Long> unreadCount(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        Long count = messageService.getUnreadCount(empId);
        return R.ok(count);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询消息列表")
    public R<cn.oa.common.result.PageResult<OaMessage>> page(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        IPage<OaMessage> page = messageService.pageList(pageNum, pageSize, empId);
        return R.ok(cn.oa.common.result.PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/send")
    @RequireAdmin
    @Operation(summary = "发送消息")
    @OperationLog(module = "消息管理", operation = "发送消息")
    public R<Void> send(@RequestBody @Valid OaMessage message, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        message.setSenderId(empId);
        if (message.getReceiverId() == null) {
            return R.fail("请输入接收人ID");
        }
        messageService.send(message);
        log.info("Message sent: from={}, to={}", empId, message.getReceiverId());
        return R.ok();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记消息已读")
    public R<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        OaMessage message = messageService.getById(id);
        if (message == null) {
            return R.fail("消息不存在");
        }
        if (!message.getReceiverId().equals(empId)) {
            return R.fail("无权操作此消息");
        }
        messageService.markAsRead(id, empId);
        return R.ok();
    }
}
