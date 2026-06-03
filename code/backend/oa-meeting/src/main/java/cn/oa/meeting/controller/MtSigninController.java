package cn.oa.meeting.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.meeting.dto.MtSigninDTO;
import cn.oa.meeting.service.MtMeetingService;
import cn.oa.meeting.vo.MtSigninVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting/signins")
@Tag(name = "会议签到管理")
@RequiredArgsConstructor
public class MtSigninController {

    private final MtMeetingService meetingService;

    @PostMapping
    @Operation(summary = "会议签到")
    @OperationLog(module = "会议管理", operation = "会议签到")
    public R<Long> signin(@RequestBody @Valid MtSigninDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(meetingService.signin(dto, empId));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "查询会议的签到记录")
    public R<List<MtSigninVO>> getByBooking(@PathVariable Long bookingId) {
        return R.ok(meetingService.getSignins(bookingId));
    }
}
