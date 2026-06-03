package cn.oa.task.service.impl;

import cn.oa.platform.core.exception.BusinessException;
import cn.oa.task.dto.TaskDependencyDTO;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.dto.TaskItemUpdateDTO;
import cn.oa.task.entity.TaskDependency;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.mapper.TaskDependencyMapper;
import cn.oa.task.mapper.TaskItemMapper;
import cn.oa.task.service.TaskItemService;
import cn.oa.task.vo.TaskItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 任务管理服务实现
 */
@Service
public class TaskItemServiceImpl extends ServiceImpl<TaskItemMapper, TaskItem> implements TaskItemService {

    @Autowired
    private TaskItemMapper itemMapper;

    @Autowired
    private TaskDependencyMapper dependencyMapper;

    @Override
    public IPage<TaskItemVO> pageQuery(TaskItemQueryDTO dto) {
        Page<TaskItem> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<TaskItem> wrapper = new LambdaQueryWrapper<>();

        if (dto.getProjectId() != null) {
            wrapper.eq(TaskItem::getProjectId, dto.getProjectId());
        }
        if (dto.getParentTaskId() != null) {
            wrapper.eq(TaskItem::getParentTaskId, dto.getParentTaskId());
        }
        if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
            wrapper.like(TaskItem::getTitle, dto.getTitle());
        }
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            wrapper.eq(TaskItem::getStatus, dto.getStatus());
        }
        if (dto.getPriority() != null && !dto.getPriority().isEmpty()) {
            wrapper.eq(TaskItem::getPriority, dto.getPriority());
        }
        if (dto.getAssigneeId() != null) {
            wrapper.eq(TaskItem::getAssigneeId, dto.getAssigneeId());
        }
        if (dto.getCreatorId() != null) {
            wrapper.eq(TaskItem::getCreatorId, dto.getCreatorId());
        }
        if (Boolean.TRUE.equals(dto.getRootOnly())) {
            wrapper.isNull(TaskItem::getParentTaskId);
        }

        // 排序
        boolean asc = dto.getAsc() != null && dto.getAsc();
        if (dto.getOrderBy() != null && !dto.getOrderBy().isEmpty()) {
            wrapper.last("ORDER BY " + dto.getOrderBy() + (asc ? " ASC" : " DESC"));
        } else {
            wrapper.orderByAsc(TaskItem::getSortOrder).orderByDesc(TaskItem::getCreateTime);
        }

        IPage<TaskItem> entityPage = itemMapper.selectPage(page, wrapper);
        IPage<TaskItemVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public TaskItemVO getDetail(Long id) {
        TaskItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("任务不存在");
        }
        TaskItemVO vo = toVO(item);
        // 查询子任务数量
        Long childCount = itemMapper.selectCount(new LambdaQueryWrapper<TaskItem>()
                .eq(TaskItem::getParentTaskId, id));
        vo.setChildCount(childCount.intValue());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskItemCreateDTO dto, Long creatorId) {
        TaskItem item = new TaskItem();
        BeanUtils.copyProperties(dto, item);
        item.setStatus("TODO");
        item.setProgress(0);
        item.setCreatorId(creatorId);
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, TaskItemUpdateDTO dto) {
        TaskItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("任务不存在");
        }
        BeanUtils.copyProperties(dto, item);
        item.setId(id);
        itemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        TaskItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("任务不存在");
        }
        item.setStatus(status);
        itemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        TaskItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("任务不存在");
        }
        itemMapper.deleteById(id);
    }

    @Override
    public List<TaskItemVO> getSubtasks(Long parentId) {
        List<TaskItem> items = itemMapper.selectList(new LambdaQueryWrapper<TaskItem>()
                .eq(TaskItem::getParentTaskId, parentId)
                .orderByAsc(TaskItem::getSortOrder)
                .orderByDesc(TaskItem::getCreateTime));
        return items.stream().map(this::toVO).toList();
    }

    @Override
    public Map<String, Object> getGanttData(Long projectId) {
        // 查询项目下所有任务
        List<TaskItem> items = itemMapper.selectList(new LambdaQueryWrapper<TaskItem>()
                .eq(TaskItem::getProjectId, projectId)
                .orderByAsc(TaskItem::getSortOrder));

        // 查询所有依赖关系
        List<TaskDependency> deps = dependencyMapper.selectList(new LambdaQueryWrapper<TaskDependency>()
                .in(TaskDependency::getTaskId, items.stream().map(TaskItem::getId).toList()));

        // 构建tasks
        List<Map<String, Object>> tasks = items.stream().map(item -> {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", item.getId());
            task.put("parentId", item.getParentTaskId());
            task.put("title", item.getTitle());
            task.put("status", item.getStatus());
            task.put("progress", item.getProgress());
            task.put("plannedStartDate", item.getPlannedStartDate());
            task.put("plannedEndDate", item.getPlannedEndDate());
            task.put("assigneeId", item.getAssigneeId());
            return task;
        }).toList();

        // 构建links
        List<Map<String, Object>> links = deps.stream().map(dep -> {
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("id", dep.getId());
            link.put("source", dep.getDependsOnTaskId());
            link.put("target", dep.getTaskId());
            link.put("type", dep.getDependencyType());
            return link;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tasks", tasks);
        result.put("links", links);
        return result;
    }

    @Override
    public boolean checkDependency(Long taskId, Long dependsOnTaskId) {
        // DFS从dependsOnTaskId出发，看能否到达taskId，即检查是否存在循环依赖
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(dependsOnTaskId);
        visited.add(dependsOnTaskId);

        while (!stack.isEmpty()) {
            Long current = stack.pop();

            // 如果当前节点的依赖指向taskId，则存在循环
            List<TaskDependency> deps = dependencyMapper.selectList(
                    new LambdaQueryWrapper<TaskDependency>().eq(TaskDependency::getTaskId, current));
            for (TaskDependency dep : deps) {
                if (dep.getDependsOnTaskId().equals(taskId)) {
                    return false; // 循环依赖
                }
                if (visited.add(dep.getDependsOnTaskId())) {
                    stack.push(dep.getDependsOnTaskId());
                }
            }
        }
        return true; // 无循环依赖
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDependency(TaskDependencyDTO dto) {
        // 先查任务存在
        TaskItem task = itemMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        TaskItem dependsOn = itemMapper.selectById(dto.getDependsOnTaskId());
        if (dependsOn == null) {
            throw new BusinessException("依赖任务不存在");
        }

        // 检查是否已存在相同依赖
        Long count = dependencyMapper.selectCount(new LambdaQueryWrapper<TaskDependency>()
                .eq(TaskDependency::getTaskId, dto.getTaskId())
                .eq(TaskDependency::getDependsOnTaskId, dto.getDependsOnTaskId()));
        if (count > 0) {
            throw new BusinessException("依赖关系已存在");
        }

        // 检查循环依赖
        if (!checkDependency(dto.getTaskId(), dto.getDependsOnTaskId())) {
            throw new BusinessException("添加该依赖会导致循环依赖");
        }

        TaskDependency dependency = new TaskDependency();
        BeanUtils.copyProperties(dto, dependency);
        dependencyMapper.insert(dependency);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDependency(Long id) {
        TaskDependency dep = dependencyMapper.selectById(id);
        if (dep == null) {
            throw new BusinessException("依赖关系不存在");
        }
        dependencyMapper.deleteById(id);
    }

    // ============ 内部方法 ============

    private TaskItemVO toVO(TaskItem item) {
        TaskItemVO vo = new TaskItemVO();
        BeanUtils.copyProperties(item, vo);
        vo.setStatusName(getStatusName(item.getStatus()));
        vo.setPriorityName(getPriorityName(item.getPriority()));

        // 父任务标题
        if (item.getParentTaskId() != null) {
            TaskItem parent = itemMapper.selectById(item.getParentTaskId());
            if (parent != null) {
                vo.setParentTaskTitle(parent.getTitle());
            }
        }
        return vo;
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "TODO" -> "待办";
            case "IN_PROGRESS" -> "进行中";
            case "IN_REVIEW" -> "评审中";
            case "DONE" -> "已完成";
            case "OVERDUE" -> "已逾期";
            default -> status;
        };
    }

    private String getPriorityName(String priority) {
        if (priority == null) return "";
        return switch (priority) {
            case "LOWEST" -> "最低";
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            case "URGENT" -> "紧急";
            default -> priority;
        };
    }
}
