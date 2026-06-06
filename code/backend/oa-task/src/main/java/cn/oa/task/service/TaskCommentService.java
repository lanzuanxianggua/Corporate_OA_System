package cn.oa.task.service;

import cn.oa.task.dto.TaskCommentCreateDTO;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.mapper.TaskCommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class TaskCommentService {
    private final TaskCommentMapper mapper;

    @Transactional public Long create(TaskCommentCreateDTO dto, Long empId) {
        TaskComment c = new TaskComment(); c.setItemId(dto.getItemId()); c.setContent(dto.getContent());
        c.setEmpId(empId); c.setParentCommentId(dto.getParentCommentId());
        mapper.insert(c); return c.getId();
    }

    public List<TaskComment> listByItem(Long itemId) { return mapper.findByItemIdOrderByTime(itemId); }
}