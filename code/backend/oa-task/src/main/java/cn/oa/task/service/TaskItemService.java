package cn.oa.task.service;

import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.mapper.TaskItemMapper;
import cn.oa.task.vo.TaskItemVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class TaskItemService {
    private final TaskItemMapper mapper;

    @Transactional public Long create(TaskItemCreateDTO dto, Long empId) {
        TaskItem t = new TaskItem(); t.setProjectId(dto.getProjectId()); t.setTaskName(dto.getTaskName());
        t.setDescription(dto.getDescription()); t.setAssigneeId(dto.getAssigneeId());
        t.setPriority(dto.getPriority()); t.setPlanStartDate(dto.getPlanStartDate());
        t.setPlanEndDate(dto.getPlanEndDate()); t.setParentTaskId(dto.getParentTaskId());
        t.setStatus("TODO"); t.setProgress(0);
        mapper.insert(t); log.info("任务创建成功: id={}", t.getId()); return t.getId();
    }

    @Transactional public void update(Long id, TaskItemCreateDTO dto) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setTaskName(dto.getTaskName()); t.setDescription(dto.getDescription());
        t.setAssigneeId(dto.getAssigneeId()); t.setPriority(dto.getPriority());
        t.setPlanStartDate(dto.getPlanStartDate()); t.setPlanEndDate(dto.getPlanEndDate());
        mapper.updateById(t);
    }

    @Transactional public void assign(Long id, Long assigneeId) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setAssigneeId(assigneeId); mapper.updateById(t); log.info("任务分配: id={}, assignee={}", id, assigneeId);
    }

    @Transactional public void changeStatus(Long id, String status) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        t.setStatus(status); mapper.updateById(t);
    }

    @Transactional public void delete(Long id) { mapper.deleteById(id); }

    public TaskItemVO getById(Long id) {
        TaskItem t = mapper.selectById(id);
        if (t == null) throw new BizException(RCode.NOT_FOUND, "任务不存在");
        TaskItemVO vo = new TaskItemVO(); vo.setId(t.getId()); vo.setProjectId(t.getProjectId());
        vo.setTaskName(t.getTaskName()); vo.setDescription(t.getDescription());
        vo.setAssigneeId(t.getAssigneeId()); vo.setStatus(t.getStatus()); vo.setPriority(t.getPriority());
        vo.setPlanStartDate(t.getPlanStartDate()); vo.setPlanEndDate(t.getPlanEndDate());
        vo.setProgress(t.getProgress()); vo.setParentTaskId(t.getParentTaskId()); vo.setCreateTime(t.getCreateTime());
        List<TaskItem> subs = mapper.findByParentId(id);
        vo.setSubTaskCount(subs != null ? subs.size() : 0);
        return vo;
    }

    public PageResult<TaskItem> listPage(TaskItemQueryDTO query) {
        Page<TaskItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<TaskItem> w = new LambdaQueryWrapper<TaskItem>()
                .eq(query.getProjectId() != null, TaskItem::getProjectId, query.getProjectId())
                .eq(query.getAssigneeId() != null, TaskItem::getAssigneeId, query.getAssigneeId())
                .eq(query.getStatus() != null, TaskItem::getStatus, query.getStatus())
                .orderByAsc(TaskItem::getSortOrder).orderByDesc(TaskItem::getCreateTime);
        Page<TaskItem> result = mapper.selectPage(page, w);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }
}