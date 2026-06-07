package cn.oa.system.support;

import cn.oa.platform.security.config.SecurityProperties;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.platform.security.password.BCryptPasswordEncoder;
import cn.oa.platform.security.password.PasswordEncoder;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.entity.SysPermission;
import cn.oa.system.entity.SysRole;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.system.mapper.SysEmpRoleMapper;
import cn.oa.system.mapper.SysPermissionMapper;
import cn.oa.system.mapper.SysRoleMapper;
import cn.oa.system.mapper.SysRolePermissionMapper;
import cn.oa.system.service.AuthService;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单测工具: 集中构建 AuthService 及其依赖 mock, 减少样板.
 */
public final class AuthServiceFixture {

    private AuthServiceFixture() {}

    public static SysEmpMapper empMapper() { return mock(SysEmpMapper.class); }
    public static SysEmpRoleMapper empRoleMapper() { return mock(SysEmpRoleMapper.class); }
    public static SysRoleMapper roleMapper() { return mock(SysRoleMapper.class); }
    public static SysRolePermissionMapper rolePermMapper() { return mock(SysRolePermissionMapper.class); }
    public static SysPermissionMapper permissionMapper() { return mock(SysPermissionMapper.class); }
    public static SysDeptMapper deptMapper() { return mock(SysDeptMapper.class); }
    public static JwtUtil jwtUtil() { return mock(JwtUtil.class); }
    public static SecurityProperties securityProperties() { return mock(SecurityProperties.class); }

    /**
     * 创建测试用的 BCrypt 密码编码器 (cost=4 加速, 生产环境 cost=10).
     */
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    /**
     * 一行替: 把员工密码字段填为指定明文的 BCrypt 哈希.
     */
    public static SysEmp withBcryptHash(SysEmp emp, String rawPassword) {
        emp.setPassword(passwordEncoder().encode(rawPassword));
        return emp;
    }

    /**
     * 构造带全部依赖的 AuthService (主构造器, 10 参含 PasswordEncoder + CaptchaService 可选).
     *
     * <p>captchaService 传 {@code null} 时, 登录路径跳过 captcha 校验 (与 Spring 注入缺失行为一致).
     */
    public static AuthService newAuthService(SysEmpMapper empMapper,
                                             SysEmpRoleMapper empRoleMapper,
                                             SysRoleMapper roleMapper,
                                             SysRolePermissionMapper rolePermMapper,
                                             SysPermissionMapper permissionMapper,
                                             SysDeptMapper deptMapper,
                                             JwtUtil jwtUtil,
                                             SecurityProperties securityProperties,
                                             PasswordEncoder passwordEncoder,
                                             cn.oa.system.service.CaptchaService captchaService) {
        return new AuthService(empMapper, empRoleMapper, roleMapper, rolePermMapper,
                permissionMapper, deptMapper, jwtUtil, securityProperties, passwordEncoder,
                captchaService);
    }

    /**
     * 构造一个 ACTIVE 状态的员工.
     */
    public static SysEmp activeEmp(Long id, String username) {
        SysEmp emp = new SysEmp();
        emp.setId(id);
        emp.setEmpCode("E" + String.format("%04d", id));
        emp.setUsername(username);
        emp.setPassword("plain-pwd");
        emp.setRealName(username + "-real");
        emp.setDeptId(10L);
        emp.setStatus("ACTIVE");
        return emp;
    }

    /**
     * 构造一个非 ACTIVE 状态的员工 (如 INACTIVE / LOCKED / LEAVE / SUSPENDED).
     */
    public static SysEmp inactiveEmp(Long id, String username, String status) {
        SysEmp emp = activeEmp(id, username);
        emp.setStatus(status);
        return emp;
    }

    /**
     * 构造角色.
     */
    public static SysRole role(Long id, String code, String dataScope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleCode(code);
        r.setRoleName(code + "-name");
        r.setDataScope(dataScope);
        r.setStatus("ACTIVE");
        return r;
    }

    /**
     * 构造部门.
     */
    public static SysDept dept(Long id, String name) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setDeptName(name);
        d.setDeptCode("D" + id);
        return d;
    }

    /**
     * 构造菜单权限节点.
     */
    public static SysPermission permMenu(Long id, Long parentId, String code, String name,
                                          String path, String icon, Integer sort) {
        SysPermission p = new SysPermission();
        p.setId(id);
        p.setParentId(parentId == null ? 0L : parentId);
        p.setPermCode(code);
        p.setPermName(name);
        p.setPath(path);
        p.setIcon(icon);
        p.setSortOrder(sort);
        p.setPermType("MENU");
        p.setStatus("ACTIVE");
        return p;
    }

    /**
     * 配置 SecurityProperties 使 jwt.getAccessTtlSeconds() 返回指定值.
     */
    public static void withAccessTtl(SecurityProperties sp, long ttl) {
        SecurityProperties.Jwt jwt = new SecurityProperties.Jwt();
        jwt.setAccessTtlSeconds(ttl);
        jwt.setRefreshTtlSeconds(604800L);
        Mockito.when(sp.getJwt()).thenReturn(jwt);
    }
}
