package cn.oa.platform.security.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户上下文信息
 * 存储在 ThreadLocal 中的用户信息
 *
 * @author oa-platform
 */
@Data
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工ID
     */
    private Long empId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色列表
     */
    private String roles;

    /**
     * Token
     */
    private String token;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 创建用户上下文
     */
    public static UserContext create(Long empId, String username, String roles) {
        UserContext context = new UserContext();
        context.setEmpId(empId);
        context.setUsername(username);
        context.setRoles(roles);
        context.setLoginTime(System.currentTimeMillis());
        return context;
    }

    /**
     * 创建用户上下文（完整信息）
     */
    public static UserContext create(Long empId, String username, String roles, Long deptId, String deptName) {
        UserContext context = create(empId, username, roles);
        context.setDeptId(deptId);
        context.setDeptName(deptName);
        return context;
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    /**
     * 判断是否拥有某个角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}