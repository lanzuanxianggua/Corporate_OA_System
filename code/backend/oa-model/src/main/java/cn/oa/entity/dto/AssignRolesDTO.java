package cn.oa.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色分配DTO
 */
@Data
public class AssignRolesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 员工ID */
    @NotNull(message = "empId不能为空")
    private Long empId;

    /** 角色ID列表 */
    @NotNull(message = "roleIds不能为空")
    private List<Long> roleIds;
}
