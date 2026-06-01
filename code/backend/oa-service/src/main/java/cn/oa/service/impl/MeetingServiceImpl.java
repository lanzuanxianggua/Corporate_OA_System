package cn.oa.service.impl;

import cn.hutool.json.JSONUtil;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaMeeting;
import cn.oa.entity.OaMeetingRoom;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaMeetingMapper;
import cn.oa.mapper.OaMeetingRoomMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.MeetingService;
import cn.oa.service.TodoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MeetingServiceImpl extends ServiceImpl<OaMeetingMapper, OaMeeting> implements MeetingService {

    @Autowired
    private OaMeetingRoomMapper roomMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private TodoService todoService;

    @Override
    @Transactional
    public void submit(OaMeeting meeting) {
        meeting.setStatus("0");
        meeting.setCreateTime(LocalDateTime.now());

        // check for time conflicts with the same room
        if (meeting.getRoomId() != null && meeting.getStartTime() != null && meeting.getEndTime() != null) {
            LambdaQueryWrapper<OaMeeting> conflictQuery = new LambdaQueryWrapper<OaMeeting>()
                    .eq(OaMeeting::getRoomId, meeting.getRoomId())
                    .ne(OaMeeting::getStatus, "3") // exclude canceled meetings
                    .lt(OaMeeting::getStartTime, meeting.getEndTime())
                    .gt(OaMeeting::getEndTime, meeting.getStartTime());
            long conflictCount = this.count(conflictQuery);
            if (conflictCount > 0) {
                throw new BusinessException("该会议室在指定时间段已被预定");
            }
        }

        this.save(meeting);

        // create todo for all participants
        if (meeting.getParticipants() != null) {
            List<Long> participantIds = JSONUtil.toList(meeting.getParticipants(), Long.class);
            for (Long empId : participantIds) {
                todoService.addTodo(empId, "会议通知: " + meeting.getTitle(), "meeting", meeting.getId(), "meeting");
            }
        }
    }

    @Override
    @Transactional
    public void cancel(Long meetingId, Long organizerId) {
        OaMeeting meeting = this.getById(meetingId);
        if (meeting == null) {
            throw new BusinessException("会议不存在");
        }
        if (!meeting.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("只有组织者才能取消会议");
        }
        meeting.setStatus("3");
        meeting.setUpdateTime(LocalDateTime.now());
        this.updateById(meeting);
    }

    @Override
    public IPage<OaMeeting> pageList(int pageNum, int pageSize, Long organizerId) {
        Page<OaMeeting> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaMeeting> wrapper = new LambdaQueryWrapper<>();
        if (organizerId != null) {
            wrapper.eq(OaMeeting::getOrganizerId, organizerId);
        }
        wrapper.orderByDesc(OaMeeting::getCreateTime);
        IPage<OaMeeting> result = this.page(page, wrapper);

        // fill roomName and organizerName
        fillExtraInfo(result.getRecords());
        return result;
    }

    private void fillExtraInfo(List<OaMeeting> records) {
        if (records == null || records.isEmpty()) return;

        Set<Long> roomIds = records.stream()
                .map(OaMeeting::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roomNameMap = Map.of();
        if (!roomIds.isEmpty()) {
            List<OaMeetingRoom> rooms = roomMapper.selectBatchIds(roomIds);
            roomNameMap = rooms.stream()
                    .collect(Collectors.toMap(OaMeetingRoom::getId, OaMeetingRoom::getRoomName, (a, b) -> a));
        }

        Set<Long> organizerIds = records.stream()
                .map(OaMeeting::getOrganizerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> empNameMap = Map.of();
        if (!organizerIds.isEmpty()) {
            List<SysEmployee> employees = employeeMapper.selectBatchIds(organizerIds);
            empNameMap = employees.stream()
                    .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));
        }

        for (OaMeeting meeting : records) {
            if (meeting.getRoomId() != null) {
                meeting.setRoomName(roomNameMap.getOrDefault(meeting.getRoomId(), ""));
            }
            if (meeting.getOrganizerId() != null) {
                meeting.setOrganizerName(empNameMap.getOrDefault(meeting.getOrganizerId(), ""));
            }
        }
    }
}
