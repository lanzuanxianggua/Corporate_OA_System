package cn.oa.system.mapper;

import cn.oa.system.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Select("<script>" +
            "SELECT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "WHERE p.del_flag = '0' AND p.status = 'ACTIVE' " +
            "<if test='roleIds != null and roleIds.size() > 0'>" +
            "AND rp.role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach> " +
            "</if>" +
            "</script>")
    List<String> selectPermCodesByRoleIds(List<Long> roleIds);
}
