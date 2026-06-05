package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfTransition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfTransitionMapper extends BaseMapper<WfTransition> {

    @Select("SELECT * FROM wf_transitions WHERE from_node_id = #{fromNodeId} AND action = #{action}")
    List<WfTransition> findByFromNodeAndAction(@Param("fromNodeId") Long fromNodeId, @Param("action") String action);
}
