package cn.oa.meeting.controller;

import cn.oa.meeting.dto.MtBookingCreateDTO;
import cn.oa.meeting.dto.MtBookingQueryDTO;
import cn.oa.meeting.service.MtBookingService;
import cn.oa.meeting.vo.MtBookingVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会议室预约 Controller.
 */
@Tag(name = "会议室预约")
@RestController
@RequestMapping("/api/v1/meeting/bookings")
@RequiredArgsConstructor
public class MtBookingController {

    private final MtBookingService service;

    @Operation(summary = "创建预约")
    @PostMapping
    @RequirePermission("meeting:booking:create")
    public R<Long> create(@RequestBody @Valid MtBookingCreateDTO dto) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.create(dto, empId));
    }

    @Operation(summary = "取消预约")
    @PostMapping("/{id}/actions/cancel")
    @RequirePermission("meeting:booking:create")
    public R<Void> cancel(@PathVariable Long id) {
        Long empId = UserContext.get().getEmpId();
        service.cancel(id, empId);
        return R.ok();
    }

    @Operation(summary = "查询预约详情")
    @GetMapping("/{id}")
    @RequirePermission("meeting:booking:list")
    public R<MtBookingVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "分页查询预约列表")
    @GetMapping
    @RequirePermission("meeting:booking:list")
    public R<PageResult<MtBookingVO>> list(MtBookingQueryDTO query) {
        Long empId = UserContext.get().getEmpId();
        return R.ok(service.listPage(query, empId));
    }
}
