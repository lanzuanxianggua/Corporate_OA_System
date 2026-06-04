package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfNodeMapper extends BaseMapper<WfNode> {

    @Select("SELECT * FROM wf_nodes WHERE def_id = #{defId} AND del_flag = 0 ORDER BY sort_order ASC")
    List<WfNode> findByDefId(Long defId);
}
