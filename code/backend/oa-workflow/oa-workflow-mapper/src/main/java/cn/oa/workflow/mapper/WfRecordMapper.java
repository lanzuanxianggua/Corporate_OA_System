package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfRecordMapper extends BaseMapper<WfRecord> {

    @Select("SELECT * FROM wf_record WHERE instance_id = #{instanceId} ORDER BY created_at ASC")
    List<WfRecord> selectByInstanceId(@Param("instanceId") Long instanceId);
}