package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {

    @Select("SELECT * FROM wf_tasks WHERE assignee_id = #{assigneeId} AND status = 'PENDING' AND del_flag = 0 ORDER BY create_time ASC")
    List<WfTask> findPendingByAssignee(Long assigneeId);

    @Select("SELECT * FROM wf_tasks WHERE instance_id = #{instanceId} AND del_flag = 0 ORDER BY create_time ASC")
    List<WfTask> findByInstanceId(Long instanceId);
}
