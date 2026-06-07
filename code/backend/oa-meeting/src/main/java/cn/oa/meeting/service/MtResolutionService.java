package cn.oa.meeting.service;

import cn.oa.meeting.constant.MtConstants;
import cn.oa.meeting.dto.MtResolutionCreateDTO;
import cn.oa.meeting.dto.MtResolutionQueryDTO;
import cn.oa.meeting.entity.MtMeeting;
import cn.oa.meeting.entity.MtResolution;
import cn.oa.meeting.mapper.MtMeetingMapper;
import cn.oa.meeting.mapper.MtResolutionMapper;
import cn.oa.meeting.vo.MtResolutionVO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议决议 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtResolutionService {

    private final MtResolutionMapper mapper;
    private final MtMeetingMapper meetingMapper;
    private final SysEmpMapper empMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(MtResolutionCreateDTO dto) {
        MtMeeting meeting = meetingMapper.selectById(dto.getMeetingId());
        if (meeting == null) {
            throw new BizException(RCode.NOT_FOUND, "关联会议不存在: " + dto.getMeetingId());
        }

        MtResolution resolution = new MtResolution();
        resolution.setMeetingId(dto.getMeetingId());
        resolution.setTitle(dto.getTitle());
        resolution.setContent(dto.getContent());
        resolution.setAssigneeId(dto.getAssigneeId());
        resolution.setDeadline(dto.getDeadline());
        resolution.setPriority(dto.getPriority() == null ? MtConstants.PRIORITY_NORMAL : dto.getPriority());
        resolution.setStatus(MtConstants.RESOLUTION_STATUS_PENDING);
        mapper.insert(resolution);
        log.info("会议决议已创建: id={}, meetingId={}, assigneeId={}",
                resolution.getId(), dto.getMeetingId(), dto.getAssigneeId());
        return resolution.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        MtResolution resolution = mapper.selectById(id);
        if (resolution == null) {
            throw new BizException(RCode.NOT_FOUND, "决议不存在: " + id);
        }
        if (!MtConstants.RESOLUTION_STATUS_PENDING.equals(resolution.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 PENDING 状态可开始, 当前状态: " + resolution.getStatus());
        }
        resolution.setStatus(MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
        mapper.updateById(resolution);
        log.info("决议已启动: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        MtResolution resolution = mapper.selectById(id);
        if (resolution == null) {
            throw new BizException(RCode.NOT_FOUND, "决议不存在: " + id);
        }
        if (MtConstants.RESOLUTION_STATUS_COMPLETED.equals(resolution.getStatus())) {
            log.info("决议已是 COMPLETED 终态, 幂等跳过: id={}", id);
            return;
        }
        if (!MtConstants.RESOLUTION_STATUS_IN_PROGRESS.equals(resolution.getStatus())
                && !MtConstants.RESOLUTION_STATUS_PENDING.equals(resolution.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 PENDING/IN_PROGRESS 状态可完成, 当前状态: " + resolution.getStatus());
        }
        resolution.setStatus(MtConstants.RESOLUTION_STATUS_COMPLETED);
        resolution.setCompleteTime(LocalDateTime.now());
        mapper.updateById(resolution);
        log.info("决议已完成: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MtResolution resolution = mapper.selectById(id);
        if (resolution == null) {
            throw new BizException(RCode.NOT_FOUND, "决议不存在: " + id);
        }
        if (MtConstants.RESOLUTION_STATUS_IN_PROGRESS.equals(resolution.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "进行中的决议不可删除");
        }
        mapper.deleteById(id);
        log.info("决议已删除: id={}", id);
    }

    /**
     * 标记超期 (PENDING/IN_PROGRESS + deadline 已过 -> OVERDUE).
     * 可由定时任务调用, 也可手动触发.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markOverdue() {
        LambdaQueryWrapper<MtResolution> wrapper = new LambdaQueryWrapper<MtResolution>()
                .in(MtResolution::getStatus,
                        MtConstants.RESOLUTION_STATUS_PENDING,
                        MtConstants.RESOLUTION_STATUS_IN_PROGRESS)
                .lt(MtResolution::getDeadline, LocalDate.now())
                .eq(MtResolution::getStatus, MtConstants.RESOLUTION_STATUS_IN_PROGRESS);
        List<MtResolution> overdueList = mapper.selectList(wrapper);
        int count = 0;
        for (MtResolution r : overdueList) {
            r.setStatus(MtConstants.RESOLUTION_STATUS_OVERDUE);
            mapper.updateById(r);
            count++;
        }
        if (count > 0) {
            log.info("已标记 {} 条超期决议", count);
        }
        return count;
    }

    public MtResolutionVO getById(Long id) {
        MtResolution resolution = mapper.selectById(id);
        if (resolution == null) {
            throw new BizException(RCode.NOT_FOUND, "决议不存在: " + id);
        }
        return toVO(resolution);
    }

    public PageResult<MtResolutionVO> listPage(MtResolutionQueryDTO query) {
        Page<MtResolution> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MtResolution> wrapper = new LambdaQueryWrapper<MtResolution>()
                .eq(query.getMeetingId() != null, MtResolution::getMeetingId, query.getMeetingId())
                .eq(query.getAssigneeId() != null, MtResolution::getAssigneeId, query.getAssigneeId())
                .eq(query.getStatus() != null, MtResolution::getStatus, query.getStatus())
                .orderByDesc(MtResolution::getCreateTime);

        Page<MtResolution> result = mapper.selectPage(page, wrapper);
        List<MtResolutionVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private MtResolutionVO toVO(MtResolution resolution) {
        MtResolutionVO vo = new MtResolutionVO();
        vo.setId(resolution.getId());
        vo.setMeetingId(resolution.getMeetingId());
        vo.setTitle(resolution.getTitle());
        vo.setContent(resolution.getContent());
        vo.setAssigneeId(resolution.getAssigneeId());
        vo.setDeadline(resolution.getDeadline());
        vo.setPriority(resolution.getPriority());
        vo.setStatus(resolution.getStatus());
        vo.setCompleteTime(resolution.getCompleteTime());
        vo.setCreateTime(resolution.getCreateTime());

        if (resolution.getAssigneeId() != null) {
            SysEmp emp = empMapper.selectById(resolution.getAssigneeId());
            if (emp != null) {
                vo.setAssigneeName(emp.getRealName());
            }
        }
        if (resolution.getMeetingId() != null) {
            MtMeeting meeting = meetingMapper.selectById(resolution.getMeetingId());
            if (meeting != null) {
                vo.setMeetingTitle(meeting.getMeetingTitle());
            }
        }
        return vo;
    }
}
