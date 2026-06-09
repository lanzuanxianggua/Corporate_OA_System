package cn.oa.platform.web.controller;

import cn.oa.message.dto.MsgNotificationQueryDTO;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LegacyApiController {

    private final MsgNotificationService notificationService;

    @GetMapping("/message/unread-count")
    public R<Long> unreadMessageCount() {
        Long empId = UserContext.get().getEmpId();
        return R.ok(notificationService.countUnread(empId).getTotal());
    }

    @GetMapping("/message/page")
    public R<PageResult<MsgNotificationVO>> messagePage(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long empId = UserContext.get().getEmpId();
        MsgNotificationQueryDTO query = new MsgNotificationQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        List<MsgNotificationVO> list = notificationService.listByRecipient(empId, query);
        return R.ok(PageResult.of(list, list.size(), pageNum, pageSize));
    }

    @PostMapping("/message/send")
    public R<Void> sendMessage() {
        return R.ok();
    }

    @PostMapping("/message/{id}/read")
    public R<Void> readMessage() {
        return R.ok();
    }

    @GetMapping("/schedule/page")
    public R<PageResult<Map<String, Object>>> schedulePage(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(PageResult.of(List.of(), 0, pageNum, pageSize));
    }

    @GetMapping("/notice/page")
    public R<PageResult<Map<String, Object>>> noticePage(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(PageResult.of(List.of(), 0, pageNum, pageSize));
    }

    @GetMapping("/report/personal/attendance-summary")
    public R<Map<String, Object>> personalAttendanceSummary(@RequestParam(required = false) String month,
            @RequestParam(required = false) String period) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month == null ? LocalDate.now().toString().substring(0, 7) : month);
        data.put("period", period == null ? "month" : period);
        data.put("normalDays", 0);
        data.put("lateCount", 0);
        data.put("earlyLeaveCount", 0);
        data.put("absentCount", 0);
        return R.ok(data);
    }

    @GetMapping({
            "/report/personal/attendance-trend",
            "/report/personal/leave-summary",
            "/report/admin/dept-compare",
            "/report/admin/attendance-trend",
            "/report/admin/leave-analysis",
            "/report/admin/employee-ranking"
    })
    public R<List<Map<String, Object>>> emptyReportList() {
        return R.ok(List.of());
    }

    @GetMapping({
            "/report/personal/monthly-compare",
            "/report/admin/attendance-summary",
            "/report/admin/today-overview",
            "/statistics/dashboard"
    })
    public R<Map<String, Object>> emptyReportObject() {
        return R.ok(Map.of());
    }

    @GetMapping("/todo/count")
    public R<Long> todoCount() {
        return R.ok(0L);
    }

    @GetMapping("/todo/page")
    public R<PageResult<Map<String, Object>>> todoPage(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(PageResult.of(List.of(), 0, pageNum, pageSize));
    }

}
