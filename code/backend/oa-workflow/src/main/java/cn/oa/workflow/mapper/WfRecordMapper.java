package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfRecordMapper extends BaseMapper<WfRecord> {

    @Select("SELECT * FROM wf_records WHERE instance_id = #{instanceId} ORDER BY action_time ASC")
    List<WfRecord> findByInstanceId(Long instanceId);
}
