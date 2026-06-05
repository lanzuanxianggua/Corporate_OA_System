package cn.oa.system.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 员工-角色关联.
 */
@Schema(description = "员工-角色关联")
@TableName("sys_employee_role")
public class SysEmpRole extends BaseEntity {

    @Schema(description = "员工ID")
    private Long empId;

    @Schema(description = "角色ID")
    private Long roleId;

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
