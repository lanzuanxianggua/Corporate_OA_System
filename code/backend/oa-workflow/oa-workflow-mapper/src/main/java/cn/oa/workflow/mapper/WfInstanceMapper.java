package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {

    @Select("SELECT * FROM wf_instance WHERE business_type = #{businessType} AND business_id = #{businessId} AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 1")
    WfInstance selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Long businessId);
}