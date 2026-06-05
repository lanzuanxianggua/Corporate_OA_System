package cn.oa.system.mapper;

import cn.oa.system.entity.SysEmpRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysEmpRoleMapper extends BaseMapper<SysEmpRole> {

    @Select("SELECT role_id FROM sys_employee_role WHERE emp_id = #{empId}")
    List<Long> selectRoleIdsByEmpId(Long empId);
}
