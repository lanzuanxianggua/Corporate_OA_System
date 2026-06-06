package cn.oa.task.mapper;

import cn.oa.task.entity.TaskProject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目 Mapper.
 */
@Mapper
public interface TaskProjectMapper extends BaseMapper<TaskProject> {

    /**
     * 按负责人查询项目列表.
     */
    @Select("SELECT * FROM task_projects WHERE del_flag = '0' AND owner_emp_id = #{ownerId} ORDER BY create_time DESC")
    List<TaskProject> findByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 按部门查询项目列表.
     */
    @Select("SELECT * FROM task_projects WHERE del_flag = '0' AND dept_id = #{deptId} ORDER BY create_time DESC")
    List<TaskProject> findByDeptId(@Param("deptId") Long deptId);
}
