package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {

    @Select("SELECT * FROM wf_task WHERE instance_id = #{instanceId} AND status = 'PENDING' ORDER BY created_at ASC")
    List<WfTask> selectPendingByInstanceId(@Param("instanceId") Long instanceId);

    @Select("SELECT * FROM wf_task WHERE parent_task_id = #{parentTaskId}")
    List<WfTask> selectByParentTaskId(@Param("parentTaskId") Long parentTaskId);

    @Update("UPDATE wf_task SET status = 'CANCELED', end_time = NOW() WHERE instance_id = #{instanceId} AND status = 'PENDING'")
    int cancelPendingByInstanceId(@Param("instanceId") Long instanceId);

    @Select("SELECT COUNT(*) FROM wf_task WHERE parent_task_id = #{parentTaskId} AND status = 'PENDING'")
    Long countPendingByParentTaskId(@Param("parentTaskId") Long parentTaskId);
}