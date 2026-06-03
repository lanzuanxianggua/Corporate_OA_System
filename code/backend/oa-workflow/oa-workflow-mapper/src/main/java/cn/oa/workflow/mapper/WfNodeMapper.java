package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfNodeMapper extends BaseMapper<WfNode> {

    @Select("SELECT * FROM wf_node WHERE def_id = #{defId} ORDER BY sort_order ASC")
    List<WfNode> selectByDefId(@Param("defId") Long defId);
}