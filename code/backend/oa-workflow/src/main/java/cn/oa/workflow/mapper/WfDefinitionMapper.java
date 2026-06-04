package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WfDefinitionMapper extends BaseMapper<WfDefinition> {

    @Select("SELECT * FROM wf_definitions WHERE def_key = #{defKey} AND status = 'ACTIVE' ORDER BY version DESC LIMIT 1")
    WfDefinition findActiveByKey(String defKey);
}
