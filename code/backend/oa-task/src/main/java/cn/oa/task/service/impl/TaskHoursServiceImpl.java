package cn.oa.task.service.impl;

import cn.oa.platform.core.exception.BusinessException;
import cn.oa.task.entity.TaskHours;
import cn.oa.task.mapper.TaskHoursMapper;
import cn.oa.task.service.TaskHoursService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时管理服务实现
 */
@Service
public class TaskHoursServiceImpl extends ServiceImpl<TaskHoursMapper, TaskHours> implements TaskHoursService {

    @Autowired
    private TaskHoursMapper hoursMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(TaskHours taskHours) {
        if (taskHours.getHours() == null || taskHours.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("工时必须大于0");
        }
        if (taskHours.getHours().compareTo(new BigDecimal("24")) > 0) {
            throw new BusinessException("单日工时不能超过24小时");
        }
        hoursMapper.insert(taskHours);
    }

    @Override
    public List<TaskHours> getByTask(Long taskId) {
        return hoursMapper.selectList(new LambdaQueryWrapper<TaskHours>()
                .eq(TaskHours::getTaskId, taskId)
                .orderByDesc(TaskHours::getWorkDate)
                .orderByDesc(TaskHours::getCreatedAt));
    }

    @Override
    public List<TaskHours> getByEmpAndDate(Long empId, LocalDate date) {
        return hoursMapper.selectList(new LambdaQueryWrapper<TaskHours>()
                .eq(TaskHours::getEmpId, empId)
                .eq(TaskHours::getWorkDate, date)
                .orderByDesc(TaskHours::getCreatedAt));
    }

    @Override
    public BigDecimal getStats(Long empId, LocalDate startDate, LocalDate endDate) {
        List<TaskHours> list = hoursMapper.selectList(new LambdaQueryWrapper<TaskHours>()
                .eq(TaskHours::getEmpId, empId)
                .between(TaskHours::getWorkDate, startDate, endDate));
        return list.stream()
                .map(TaskHours::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
