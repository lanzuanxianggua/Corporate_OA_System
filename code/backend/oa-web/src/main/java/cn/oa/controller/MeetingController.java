package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaMeeting;
import cn.oa.entity.OaMeetingRoom;
import cn.oa.entity.dto.MeetingDTO;
import cn.oa.service.MeetingRoomService;
import cn.oa.service.MeetingService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting")
@Tag(name = "会议管理")
public class MeetingController {

    @Autowired
    private MeetingRoomService meetingRoomService;

    @Autowired
    private MeetingService meetingService;

    @GetMapping("/room/list")
    @Operation(summary = "会议室列表")
    public R<List<OaMeetingRoom>> roomList() {
        return R.ok(meetingRoomService.list());
    }

    @PostMapping("/room")
    @RequireAdmin
    @Operation(summary = "新增会议室")
    @OperationLog(module = "会议室管理", operation = "新增会议室")
    public R<Void> addRoom(@RequestBody @Valid OaMeetingRoom room) {
        meetingRoomService.save(room);
        log.info("Meeting room created: id={}", room.getId());
        return R.ok();
    }

    @PutMapping("/room")
    @RequireAdmin
    @OperationLog(module = "会议室管理", operation = "修改会议室")
    @Operation(summary = "修改会议室")
    public R<Void> updateRoom(@RequestBody @Valid OaMeetingRoom room) {
        if (room.getId() == null) {
            return R.fail("会议室ID不能为空");
        }
        meetingRoomService.updateById(room);
        log.info("Meeting room updated: id={}", room.getId());
        return R.ok();
    }

    @DeleteMapping("/room/{id}")
    @RequireAdmin
    @OperationLog(module = "会议室管理", operation = "删除会议室")
    @Operation(summary = "删除会议室")
    public R<Void> deleteRoom(@PathVariable Long id) {
        meetingRoomService.removeById(id);
        log.info("Meeting room deleted: id={}", id);
        return R.ok();
    }

    @PostMapping("/submit")
    @Operation(summary = "创建会议")
    @OperationLog(module = "会议管理", operation = "创建会议")
    public R<Void> submit(@RequestBody @Valid MeetingDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        OaMeeting meeting = new OaMeeting();
        meeting.setTitle(dto.getTitle());
        meeting.setRoomId(dto.getRoomId());
        meeting.setOrganizerId(empId);
        meeting.setStartTime(dto.getStartTime());
        meeting.setEndTime(dto.getEndTime());
        meeting.setDescription(dto.getDescription());
        meeting.setParticipants(dto.getParticipants());
        meeting.setStatus(dto.getStatus());
        meetingService.submit(meeting);
        log.info("Meeting created: title={}, organizerId={}", meeting.getTitle(), empId);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "我的会议列表")
    public R<PageResult<OaMeeting>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize,
                                          @RequestParam(required = false) Long organizerId,
                                          HttpServletRequest request) {
        IPage<OaMeeting> page = meetingService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), organizerId);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/cancel/{id}")
    @Operation(summary = "取消会议")
    @OperationLog(module = "会议管理", operation = "取消会议")
    public R<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        meetingService.cancel(id, empId);
        log.info("Meeting cancelled: id={}, empId={}", id, empId);
        return R.ok();
    }
}
