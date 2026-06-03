package cn.oa.task.service;

import cn.oa.task.entity.TaskHours;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TaskHoursService extends IService<TaskHours> {

    /**
     * 登记工时
     */
    void record(TaskHours taskHours);

    /**
     * 查询任务的工时记录
     */
    List<TaskHours> getByTask(Long taskId);

    /**
     * 查询员工某天的工时记录
     */
    List<TaskHours> getByEmpAndDate(Long empId, LocalDate date);

    /**
     * 统计员工在日期范围内的工时总和
     */
    BigDecimal getStats(Long empId, LocalDate startDate, LocalDate endDate);
}
