package cn.oa.task.mapper;

import cn.oa.task.entity.TaskHour;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 工时 Mapper.
 */
@Mapper
public interface TaskHourMapper extends BaseMapper<TaskHour> {

    /**
     * 按任务查询工时列表.
     */
    @Select("SELECT * FROM task_hours WHERE del_flag = '0' AND item_id = #{itemId} ORDER BY work_date")
    List<TaskHour> findByItemId(@Param("itemId") Long itemId);

    /**
     * 按员工和日期范围查询工时列表.
     */
    @Select("SELECT * FROM task_hours WHERE del_flag = '0' AND emp_id = #{empId} AND work_date BETWEEN #{startDate} AND #{endDate} ORDER BY work_date")
    List<TaskHour> findByEmpAndDateRange(@Param("empId") Long empId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
