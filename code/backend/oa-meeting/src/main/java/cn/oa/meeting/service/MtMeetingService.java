package cn.oa.meeting.service;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.dto.MtMeetingCreateDTO;
import cn.oa.meeting.dto.MtMeetingQueryDTO;
import cn.oa.meeting.entity.MtMeeting;
import cn.oa.meeting.mapper.MtMeetingMapper;
import cn.oa.meeting.vo.MtMeetingVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysEmpMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议记录 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtMeetingService {

    private final MtMeetingMapper mapper;
    private final SysEmpMapper empMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(MtMeetingCreateDTO dto, Long empId) {
        MtMeeting meeting = new MtMeeting();
        meeting.setBookingId(dto.getBookingId());
        meeting.setMeetingTitle(dto.getMeetingTitle());
        meeting.setSummary(dto.getSummary());
        meeting.setStartTime(dto.getStartTime());
        meeting.setEndTime(dto.getEndTime());
        meeting.setLocation(dto.getLocation());
        meeting.setOrganizerId(empId);
        meeting.setStatus(MtConstants.MEETING_STATUS_SCHEDULED);
        mapper.insert(meeting);
        log.info("会议已创建: id={}, title={}, organizerId={}", meeting.getId(), dto.getMeetingTitle(), empId);
        return meeting.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        MtMeeting meeting = mapper.selectById(id);
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "会议不存在: " + id);
        }
        if (!MtConstants.MEETING_STATUS_SCHEDULED.equals(meeting.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 SCHEDULED 状态可开始, 当前状态: " + meeting.getStatus());
        }
        meeting.setStatus(MtConstants.MEETING_STATUS_IN_PROGRESS);
        mapper.updateById(meeting);
        log.info("会议已开始: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, String summary) {
        MtMeeting meeting = mapper.selectById(id);
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "会议不存在: " + id);
        }
        if (MtConstants.MEETING_STATUS_COMPLETED.equals(meeting.getStatus())) {
            log.info("会议已是 COMPLETED 终态, 幂等跳过: id={}", id);
            return;
        }
        if (!MtConstants.MEETING_STATUS_IN_PROGRESS.equals(meeting.getStatus())
                && !MtConstants.MEETING_STATUS_SCHEDULED.equals(meeting.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 IN_PROGRESS/SCHEDULED 状态可完成, 当前状态: " + meeting.getStatus());
        }
        meeting.setStatus(MtConstants.MEETING_STATUS_COMPLETED);
        if (summary != null && !summary.isBlank()) {
            meeting.setSummary(summary);
        }
        mapper.updateById(meeting);
        log.info("会议已完成: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        MtMeeting meeting = mapper.selectById(id);
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "会议不存在: " + id);
        }
        if (MtConstants.MEETING_STATUS_COMPLETED.equals(meeting.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "已完成的会议不可取消");
        }
        meeting.setStatus(MtConstants.MEETING_STATUS_CANCELLED);
        mapper.updateById(meeting);
        log.info("会议已取消: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MtMeeting meeting = mapper.selectById(id);
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "会议不存在: " + id);
        }
        if (MtConstants.MEETING_STATUS_IN_PROGRESS.equals(meeting.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "进行中的会议不可删除");
        }
        mapper.deleteById(id);
        log.info("会议已删除: id={}", id);
    }

    public MtMeetingVO getById(Long id) {
        MtMeeting meeting = mapper.selectById(id);
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "会议不存在: " + id);
        }
        return toVO(meeting);
    }

    public PageResult<MtMeetingVO> listPage(MtMeetingQueryDTO query) {
        Page<MtMeeting> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MtMeeting> wrapper = new LambdaQueryWrapper<MtMeeting>()
                .eq(query.getBookingId() != null, MtMeeting::getBookingId, query.getBookingId())
                .eq(query.getStatus() != null, MtMeeting::getStatus, query.getStatus())
                .eq(query.getOrganizerId() != null, MtMeeting::getOrganizerId, query.getOrganizerId())
                .orderByDesc(MtMeeting::getCreateTime);

        Page<MtMeeting> result = mapper.selectPage(page, wrapper);
        List<MtMeetingVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private MtMeetingVO toVO(MtMeeting meeting) {
        MtMeetingVO vo = new MtMeetingVO();
        vo.setId(meeting.getId());
        vo.setBookingId(meeting.getBookingId());
        vo.setMeetingTitle(meeting.getMeetingTitle());
        vo.setSummary(meeting.getSummary());
        vo.setStartTime(meeting.getStartTime());
        vo.setEndTime(meeting.getEndTime());
        vo.setLocation(meeting.getLocation());
        vo.setOrganizerId(meeting.getOrganizerId());
        vo.setStatus(meeting.getStatus());
        vo.setCreateTime(meeting.getCreateTime());

        if (meeting.getOrganizerId() != null) {
            SysEmp emp = empMapper.selectById(meeting.getOrganizerId());
            if (emp != null) {
                vo.setOrganizerName(emp.getRealName());
            }
        }
        return vo;
    }
}
