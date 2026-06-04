package cn.oa.system.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色-权限关联.
 */
@Schema(description = "角色-权限关联")
@TableName("sys_role_permission")
public class SysRolePermission extends BaseEntity {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "权限ID")
    private Long permId;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getPermId() { return permId; }
    public void setPermId(Long permId) { this.permId = permId; }
}
