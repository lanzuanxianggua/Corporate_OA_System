package cn.oa.workflow.mapper;

import cn.oa.workflow.entity.WfDelegation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WfDelegationMapper extends BaseMapper<WfDelegation> {

    @Select("SELECT * FROM wf_delegations WHERE from_emp_id = #{empId} AND status = 'ACTIVE' AND start_time <= #{now} AND end_time >= #{now}")
    List<WfDelegation> findActiveByFromEmp(Long empId, LocalDateTime now);

    @Select("SELECT * FROM wf_delegations WHERE to_emp_id = #{empId} AND status = 'ACTIVE' AND start_time <= #{now} AND end_time >= #{now}")
    List<WfDelegation> findActiveByToEmp(Long empId, LocalDateTime now);
}
