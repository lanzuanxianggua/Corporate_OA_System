package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfTransition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfTransitionMapper extends BaseMapper<WfTransition> {

    @Select("SELECT * FROM wf_transition WHERE from_node_id = #{nodeId} ORDER BY priority DESC")
    List<WfTransition> selectByFromNodeId(@Param("nodeId") Long nodeId);
}