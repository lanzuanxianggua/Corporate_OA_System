package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfAssigneeRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfAssigneeRuleMapper extends BaseMapper<WfAssigneeRule> {

    @Select("SELECT * FROM wf_assignee_rule WHERE node_id = #{nodeId} ORDER BY sort_order ASC")
    List<WfAssigneeRule> selectByNodeId(@Param("nodeId") Long nodeId);
}