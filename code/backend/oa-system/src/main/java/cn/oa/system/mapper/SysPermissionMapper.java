package cn.oa.system.mapper;

import cn.oa.system.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 按角色集合查找所有权限实体 (包含按钮), 去重.
     */
    @Select("<script>" +
            "SELECT DISTINCT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "WHERE p.del_flag = '0' AND p.status = 'ACTIVE' " +
            "<if test='roleIds != null and roleIds.size() > 0'>" +
            "AND rp.role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach> " +
            "</if>" +
            "ORDER BY p.sort_order ASC, p.id ASC" +
            "</script>")
    List<SysPermission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 按角色集合查找菜单型权限 (MENU / OUTER_MENU).
     */
    @Select("<script>" +
            "SELECT DISTINCT p.* FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "WHERE p.del_flag = '0' AND p.status = 'ACTIVE' " +
            "AND p.perm_type IN ('MENU','OUTER_MENU') " +
            "<if test='roleIds != null and roleIds.size() > 0'>" +
            "AND rp.role_id IN " +
            "<foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach> " +
            "</if>" +
            "ORDER BY p.sort_order ASC, p.id ASC" +
            "</script>")
    List<SysPermission> selectMenusByRoleIds(@Param("roleIds") List<Long> roleIds);
}
