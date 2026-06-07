package cn.oa.task.service;

import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.mapper.TaskItemMapper;
import cn.oa.task.vo.TaskItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskItemService {

    private final TaskItemMapper mapper;

    private static final java.util.Set<String> ALLOWED_STATUSES =
            java.util.Set.of("TODO", "IN_PROGRESS", "DONE", "CLOSED");

    @Transactional
    public Long create(TaskItemCreateDTO dto, Long empId) {
        TaskItem t = new TaskItem();
        t.setProjectId(dto.getProjectId());
        t.setTaskName(dto.getTaskName());
        t.setDescription(dto.getDescription());
        t.setAssigneeId(dto.getAssigneeId());
        t.setPriority(dto.getPriority() == null ? "NORMAL" : dto.getPriority());
        t.setPlanStartDate(dto.getPlanStartDate());
        t.setPlanEndDate(dto.getPlanEndDate());
        t.setParentTaskId(dto.getParentTaskId());
        t.setStatus("TODO");
        t.setProgress(0);
        mapper.insert(t);
        log.info("任务创建成功: id={}, taskName={}", t.getId(), t.getTaskName());
        return t.getId();
    }

    @Transactional
    public void update(Long id, TaskItemCreateDTO dto) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setTaskName(dto.getTaskName());
        t.setDescription(dto.getDescription());
        t.setAssigneeId(dto.getAssigneeId());
        if (dto.getPriority() != null) t.setPriority(dto.getPriority());
        t.setPlanStartDate(dto.getPlanStartDate());
        t.setPlanEndDate(dto.getPlanEndDate());
        mapper.updateById(t);
        log.info("任务已更新: id={}", id);
    }

    @Transactional
    public void assign(Long id, Long assigneeId) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setAssigneeId(assigneeId);
        mapper.updateById(t);
        log.info("任务分配: id={}, assignee={}", id, assigneeId);
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BizException(RCode.BAD_REQUEST, "非法状态: " + status);
        }
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setStatus(status);
        // 进入 IN_PROGRESS 自动记 actual_start; DONE 记 actual_end + progress=100
        if ("IN_PROGRESS".equals(status) && t.getActualStart() == null) {
            t.setActualStart(LocalDateTime.now());
        }
        if ("DONE".equals(status)) {
            t.setActualEnd(LocalDateTime.now());
            t.setProgress(100);
        }
        mapper.updateById(t);
        log.info("任务状态变更: id={}, status={}", id, status);
    }

    @Transactional
    public void updateProgress(Long id, Integer progress) {
        if (progress == null || progress < 0 || progress > 100) {
            throw new BizException(RCode.BAD_REQUEST, "进度必须在 0-100");
        }
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setProgress(progress);
        mapper.updateById(t);
        log.info("任务进度更新: id={}, progress={}", id, progress);
    }

    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
        log.info("任务已删除: id={}", id);
    }

    public TaskItemVO getById(Long id) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        return toVO(t);
    }

    public PageResult<TaskItemVO> listPage(TaskItemQueryDTO query) {
        Page<TaskItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<TaskItem> w = new LambdaQueryWrapper<TaskItem>()
                .eq(query.getProjectId() != null, TaskItem::getProjectId, query.getProjectId())
                .eq(query.getAssigneeId() != null, TaskItem::getAssigneeId, query.getAssigneeId())
                .eq(query.getStatus() != null, TaskItem::getStatus, query.getStatus())
                .orderByAsc(TaskItem::getSortOrder)
                .orderByDesc(TaskItem::getCreateTime);
        Page<TaskItem> result = mapper.selectPage(page, w);
        List<TaskItemVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private TaskItemVO toVO(TaskItem t) {
        TaskItemVO vo = new TaskItemVO();
        vo.setId(t.getId());
        vo.setProjectId(t.getProjectId());
        vo.setTaskName(t.getTaskName());
        vo.setDescription(t.getDescription());
        vo.setAssigneeId(t.getAssigneeId());
        vo.setStatus(t.getStatus());
        vo.setPriority(t.getPriority());
        vo.setPlanStartDate(t.getPlanStartDate());
        vo.setPlanEndDate(t.getPlanEndDate());
        vo.setActualStart(t.getActualStart());
        vo.setActualEnd(t.getActualEnd());
        vo.setProgress(t.getProgress());
        vo.setParentTaskId(t.getParentTaskId());
        vo.setSortOrder(t.getSortOrder());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        List<TaskItem> subs = mapper.findByParentId(t.getId());
        vo.setSubTaskCount(subs == null ? 0 : subs.size());
        return vo;
    }
}
