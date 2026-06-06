package cn.oa.task.mapper;

import cn.oa.task.entity.TaskItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务 Mapper.
 */
@Mapper
public interface TaskItemMapper extends BaseMapper<TaskItem> {

    /**
     * 按项目查询任务列表.
     */
    @Select("SELECT * FROM task_items WHERE del_flag = '0' AND project_id = #{projectId} ORDER BY sort_order, create_time")
    List<TaskItem> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 按负责人查询任务列表.
     */
    @Select("SELECT * FROM task_items WHERE del_flag = '0' AND assignee_id = #{assigneeId} ORDER BY create_time DESC")
    List<TaskItem> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    /**
     * 按父任务查询子任务列表.
     */
    @Select("SELECT * FROM task_items WHERE del_flag = '0' AND parent_task_id = #{parentId} ORDER BY sort_order, create_time")
    List<TaskItem> findByParentId(@Param("parentId") Long parentId);
}
