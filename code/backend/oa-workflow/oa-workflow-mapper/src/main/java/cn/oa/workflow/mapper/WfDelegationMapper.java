package cn.oa.workflow.mapper;

import cn.oa.workflow.model.entity.WfDelegation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface WfDelegationMapper extends BaseMapper<WfDelegation> {

    @Select("SELECT * FROM wf_delegation WHERE delegator_id = #{empId} AND status = 'ACTIVE' AND start_time <= #{now} AND end_time >= #{now} LIMIT 1")
    WfDelegation findActiveByDelegator(@Param("empId") Long empId, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM wf_delegation WHERE delegate_id = #{empId} AND status = 'ACTIVE' AND start_time <= #{now} AND end_time >= #{now} LIMIT 1")
    WfDelegation findActiveByDelegate(@Param("empId") Long empId, @Param("now") LocalDateTime now);
}