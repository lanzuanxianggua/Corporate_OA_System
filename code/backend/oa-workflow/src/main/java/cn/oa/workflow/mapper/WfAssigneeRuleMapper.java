package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfAssigneeRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfAssigneeRuleMapper extends BaseMapper<WfAssigneeRule> {

    @Select("SELECT * FROM wf_assignee_rules WHERE def_id = #{defId} ORDER BY priority DESC")
    List<WfAssigneeRule> findByDefId(Long defId);
}
