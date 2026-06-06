package cn.oa.task.service;

import cn.oa.task.dto.TaskHourCreateDTO;
import cn.oa.task.entity.TaskHour;
import cn.oa.task.mapper.TaskHourMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class TaskHourService {
    private final TaskHourMapper mapper;

    @Transactional public Long create(TaskHourCreateDTO dto, Long empId) {
        TaskHour h = new TaskHour(); h.setItemId(dto.getItemId()); h.setWorkDate(dto.getWorkDate());
        h.setHours(dto.getHours()); h.setDescription(dto.getDescription()); h.setEmpId(empId);
        mapper.insert(h); return h.getId();
    }

    public List<TaskHour> listByItem(Long itemId) { return mapper.findByItemId(itemId); }
}