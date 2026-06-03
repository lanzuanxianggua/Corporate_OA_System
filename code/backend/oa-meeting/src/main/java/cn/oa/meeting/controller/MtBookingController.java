package cn.oa.meeting.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.meeting.dto.MtBookingCreateDTO;
import cn.oa.meeting.service.MtMeetingService;
import cn.oa.meeting.vo.MtBookingVO;
import cn.oa.meeting.vo.MtResolutionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting/bookings")
@Tag(name = "会议室预订管理")
@RequiredArgsConstructor
public class MtBookingController {

    private final MtMeetingService meetingService;

    @PostMapping
    @Operation(summary = "预订会议室")
    @OperationLog(module = "会议管理", operation = "预订会议室")
    public R<Long> book(@RequestBody @Valid MtBookingCreateDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(meetingService.book(dto, empId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消预订")
    @OperationLog(module = "会议管理", operation = "取消预订")
    public R<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        meetingService.cancel(id, empId);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询预订列表")
    public R<PageResult<MtBookingVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long bookEmpId,
            @RequestParam(required = false) Integer status) {
        IPage<MtBookingVO> page = meetingService.pageQuery(pageNum, pageSize, roomId, bookEmpId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预订详情")
    public R<MtBookingVO> detail(@PathVariable Long id) {
        MtBookingVO vo = meetingService.getBookingDetail(id);
        return vo != null ? R.ok(vo) : R.fail("预订不存在");
    }

    @GetMapping("/{id}/resolutions")
    @Operation(summary = "查询预订的决议列表")
    public R<List<MtResolutionVO>> resolutions(@PathVariable Long id) {
        return R.ok(meetingService.getResolutions(id));
    }
}
