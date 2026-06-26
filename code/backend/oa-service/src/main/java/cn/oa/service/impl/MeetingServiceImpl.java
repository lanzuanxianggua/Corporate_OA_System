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
        validateMeetingTime(meeting);
        meeting.setStatus("0");
        meeting.setCreateTime(LocalDateTime.now());

        if (meeting.getRoomId() != null) {
            Long lockedRoomId = roomMapper.lockById(meeting.getRoomId());
            if (lockedRoomId == null) {
                throw new BusinessException("??????");
            }
            assertNoRoomConflict(meeting.getRoomId(), meeting.getStartTime(), meeting.getEndTime(), null);
        }

        this.save(meeting);

        if (meeting.getParticipants() != null) {
            List<Long> participantIds = JSONUtil.toList(meeting.getParticipants(), Long.class);
            for (Long empId : participantIds) {
                todoService.addTodo(empId, "????: " + meeting.getTitle(), "meeting", meeting.getId(), "meeting");
            }
        }
    }

    @Override
    @Transactional
    public void cancel(Long meetingId, Long organizerId) {
        OaMeeting meeting = this.getById(meetingId);
        if (meeting == null) {
            throw new BusinessException("?????");
        }
        if (!meeting.getOrganizerId().equals(organizerId)) {
            throw new BusinessException("???????????");
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
        fillExtraInfo(result.getRecords());
        return result;
    }

    private void validateMeetingTime(OaMeeting meeting) {
        if (meeting.getStartTime() == null || meeting.getEndTime() == null) {
            throw new BusinessException("?????????????");
        }
        if (!meeting.getEndTime().isAfter(meeting.getStartTime())) {
            throw new BusinessException("??????????????");
        }
    }

    private void assertNoRoomConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeMeetingId) {
        LambdaQueryWrapper<OaMeeting> conflictQuery = new LambdaQueryWrapper<OaMeeting>()
                .eq(OaMeeting::getRoomId, roomId)
                .ne(OaMeeting::getStatus, "3")
                .lt(OaMeeting::getStartTime, endTime)
                .gt(OaMeeting::getEndTime, startTime);
        if (excludeMeetingId != null) {
            conflictQuery.ne(OaMeeting::getId, excludeMeetingId);
        }
        if (this.count(conflictQuery) > 0) {
            throw new BusinessException("??????????????");
        }
    }

    private void fillExtraInfo(List<OaMeeting> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

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
