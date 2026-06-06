package cn.oa.system.mapper;

import cn.oa.system.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 按 empId 查找关联角色 (过滤软删 + 角色启用).
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_employee_role er ON r.id = er.role_id " +
            "WHERE er.emp_id = #{empId} " +
            "AND r.del_flag = '0' AND r.status = 'ACTIVE' " +
            "ORDER BY r.sort_order ASC, r.id ASC")
    List<SysRole> selectByEmpId(@Param("empId") Long empId);
}
