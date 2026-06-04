package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {

    @Select("SELECT * FROM wf_instances WHERE business_key = #{businessKey} AND status IN ('RUNNING')")
    WfInstance findRunningByBusinessKey(String businessKey);

    @Select("SELECT * FROM wf_instances WHERE initiator_id = #{initiatorId} AND del_flag = 0 ORDER BY start_time DESC")
    List<WfInstance> findByInitiator(Long initiatorId);
}
