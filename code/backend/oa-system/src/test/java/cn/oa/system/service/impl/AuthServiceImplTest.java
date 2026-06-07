package cn.oa.system.service.impl;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.exception.BizException;
import cn.oa.platform.security.config.SecurityProperties;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.platform.security.password.BCryptPasswordEncoder;
import cn.oa.platform.security.password.PasswordEncoder;
import cn.oa.system.dto.ChangePasswordReq;
import cn.oa.system.dto.LoginReq;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.entity.SysPermission;
import cn.oa.system.entity.SysRole;
import cn.oa.system.exception.AuthDomainException;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.system.mapper.SysEmpRoleMapper;
import cn.oa.system.mapper.SysPermissionMapper;
import cn.oa.system.mapper.SysRoleMapper;
import cn.oa.system.mapper.SysRolePermissionMapper;
import cn.oa.system.service.AuthService;
import cn.oa.system.vo.LoginResp;
import cn.oa.system.vo.MenuTreeVO;
import cn.oa.system.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 单测 (v2 Phase 2).
 *
 * <p>密码相关用例已切换到 BCrypt: 员工 password 字段为 BCrypt 哈希,
 * 登录/改密路径走 {@link PasswordEncoder#matches} / {@link PasswordEncoder#encode}.
 *
 * <p>Lazy Rehash 行为见 {@link BcryptScenarios}.
 */
class AuthServiceImplTest {

    private SysEmpMapper empMapper;
    private SysEmpRoleMapper empRoleMapper;
    private SysRoleMapper roleMapper;
    private SysRolePermissionMapper rolePermMapper;
    private SysPermissionMapper permissionMapper;
    private SysDeptMapper deptMapper;
    private JwtUtil jwtUtil;
    private SecurityProperties securityProperties;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        empMapper = mock(SysEmpMapper.class);
        empRoleMapper = mock(SysEmpRoleMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        rolePermMapper = mock(SysRolePermissionMapper.class);
        permissionMapper = mock(SysPermissionMapper.class);
        deptMapper = mock(SysDeptMapper.class);
        jwtUtil = mock(JwtUtil.class);
        securityProperties = mock(SecurityProperties.class);
        // 测试用真实 BCrypt 编码器, cost=4 加速; 生产 strength=10
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(empMapper, empRoleMapper, roleMapper, rolePermMapper,
                permissionMapper, deptMapper, jwtUtil, securityProperties, passwordEncoder,
                null /* captchaService */);

        // default: jwt TTL = 3600
        SecurityProperties.Jwt jwtCfg = new SecurityProperties.Jwt();
        jwtCfg.setAccessTtlSeconds(3600L);
        jwtCfg.setRefreshTtlSeconds(604800L);
        when(securityProperties.getJwt()).thenReturn(jwtCfg);

        // default: Bcrypt (Lazy Rehash 默认开)
        SecurityProperties.Bcrypt bcryptCfg = new SecurityProperties.Bcrypt();
        bcryptCfg.setStrength(4);
        bcryptCfg.setEnableLazyRehash(true);
        lenient().when(securityProperties.getBcrypt()).thenReturn(bcryptCfg);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ===================== login =====================

    @Test
    @DisplayName("login 成功: 返回 LoginResp 含 token + userInfo (deptName/dataScope 完整), 触发 Lazy Rehash")
    void login_success() {
        // arrange — 库中存的是明文 'admin123' (V910 状态), 登录成功后应被升级
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword("admin123");
        when(empMapper.selectByUsername("admin")).thenReturn(emp);
        when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(
                role(10L, "SUPER_ADMIN", "ALL")));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                .thenReturn(List.of("system:user:list", "hr-leave:leave:create"));
        when(jwtUtil.generateAccessToken(eq(1L), eq("admin"), anyList(), anyList()))
                .thenReturn("access-token-xxx");
        when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh-token-xxx");
        when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));
        when(empMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("admin123");

        // act
        LoginResp resp = authService.login(req, "127.0.0.1");

        // assert
        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isEqualTo("access-token-xxx");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token-xxx");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isEqualTo(3600L);

        UserInfoVO info = resp.getUserInfo();
        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(1L);
        assertThat(info.getUsername()).isEqualTo("admin");
        assertThat(info.getDeptId()).isEqualTo(10L);
        assertThat(info.getDeptName()).isEqualTo("技术部");
        assertThat(info.getDataScope()).isEqualTo("ALL");
        assertThat(info.getRoles()).containsExactly("SUPER_ADMIN");
        assertThat(info.getPermissions())
                .containsExactlyInAnyOrder("system:user:list", "hr-leave:leave:create");

        // updateLastLogin 应被调用
        ArgumentCaptor<Long> idCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<LocalDateTime> tsCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> ipCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> verCap = ArgumentCaptor.forClass(Integer.class);
        verify(empMapper).updateLastLogin(idCap.capture(), tsCap.capture(), ipCap.capture(), verCap.capture());
        assertThat(idCap.getValue()).isEqualTo(1L);
        assertThat(ipCap.getValue()).isEqualTo("127.0.0.1");

        // Lazy Rehash: updatePassword 应被调用一次, 参数是 BCrypt 哈希
        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        verify(empMapper).updatePassword(eq(1L), hashCap.capture());
        assertThat(hashCap.getValue()).startsWith("$2a$04$");
    }

    @Test
    @DisplayName("login 密码错误: 抛 AuthDomainException (RCode.UNAUTHORIZED)")
    void login_wrongPassword() {
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword("admin123");
        when(empMapper.selectByUsername("admin")).thenReturn(emp);
        // roleMapper / permMapper / jwtUtil 都不应被调用

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("wrong-pwd");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(roleMapper, never()).selectByEmpId(anyLong());
        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
        verify(empMapper, never()).updateLastLogin(anyLong(), any(), anyString(), any());
        verify(empMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("login 用户不存在: 抛 AuthDomainException 消息防枚举 (统一为 用户名或密码错误)")
    void login_userNotFound() {
        when(empMapper.selectByUsername("ghost")).thenReturn(null);

        LoginReq req = new LoginReq();
        req.setUsername("ghost");
        req.setPassword("any-pwd");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("用户名或密码错误");

        verify(roleMapper, never()).selectByEmpId(anyLong());
        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("login 账号非 ACTIVE: 抛 AuthDomainException (RCode.FORBIDDEN)")
    void login_accountDisabled() {
        SysEmp emp = inactiveEmp(1L, "admin", "INACTIVE");
        when(empMapper.selectByUsername("admin")).thenReturn(emp);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("admin123");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("账号已停用");

        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("login 账号 LOCKED 状态: 当前实现未单独区分 LOCKED, 走 !ACTIVE 分支抛 accountDisabled")
    void login_accountLocked() {
        SysEmp emp = inactiveEmp(1L, "admin", "LOCKED");
        when(empMapper.selectByUsername("admin")).thenReturn(emp);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("admin123");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("账号已停用");
    }

    @Test
    @DisplayName("login 员工无角色: 仍签发 token, userInfo.roles/permissions 为空但 token 仍生成")
    void login_noRolesButStillIssueToken() {
        SysEmp emp = activeEmp(1L, "loner");
        emp.setPassword("pwd12345");
        when(empMapper.selectByUsername("loner")).thenReturn(emp);
        when(roleMapper.selectByEmpId(1L)).thenReturn(List.of());
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of());
        when(rolePermMapper.selectPermCodesByRoleIds(List.of())).thenReturn(List.of());
        when(jwtUtil.generateAccessToken(eq(1L), eq("loner"), anyList(), anyList()))
                .thenReturn("access-empty-roles");
        when(jwtUtil.generateRefreshToken(1L, "loner")).thenReturn("refresh-empty-roles");
        when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));
        when(empMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

        LoginReq req = new LoginReq();
        req.setUsername("loner");
        req.setPassword("pwd12345");

        LoginResp resp = authService.login(req, "1.2.3.4");
        assertThat(resp.getAccessToken()).isEqualTo("access-empty-roles");
        // dataScope 无角色时回退 SELF
        assertThat(resp.getUserInfo().getDataScope()).isEqualTo("SELF");
        assertThat(resp.getUserInfo().getRoles()).isEmpty();
        assertThat(resp.getUserInfo().getPermissions()).isEmpty();
    }

    // ===================== refreshToken =====================

    @Test
    @DisplayName("refreshToken 合法: 重新签发 access, refreshToken 保持原值 (不轮换)")
    void refreshToken_valid() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "refresh");
        map.put("uid", 1L);
        map.put("uname", "admin");
        Claims claims = Jwts.claims(map);
        when(jwtUtil.parse("good-refresh")).thenReturn(claims);

        SysEmp emp = activeEmp(1L, "admin");
        when(empMapper.selectById(1L)).thenReturn(emp);
        when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(role(10L, "ADMIN", "ALL")));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                .thenReturn(List.of("system:user:list"));
        when(jwtUtil.generateAccessToken(eq(1L), eq("admin"), anyList(), anyList()))
                .thenReturn("new-access");
        when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));

        LoginResp resp = authService.refreshToken("good-refresh");

        assertThat(resp.getAccessToken()).isEqualTo("new-access");
        // refresh token 保持原值 (不轮换)
        assertThat(resp.getRefreshToken()).isEqualTo("good-refresh");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getUserInfo().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("refreshToken 类型非 refresh: 抛 AuthDomainException (RCode.INVALID_TOKEN)")
    void refreshToken_wrongType() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "access");  // 错误类型
        map.put("uid", 1L);
        map.put("uname", "admin");
        Claims claims = Jwts.claims(map);
        when(jwtUtil.parse("access-token")).thenReturn(claims);

        assertThatThrownBy(() -> authService.refreshToken("access-token"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("Token 无效");

        verify(empMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("refreshToken 解析抛 BizException (TOKEN_EXPIRED): 透传")
    void refreshToken_expired() {
        when(jwtUtil.parse("expired"))
                .thenThrow(new BizException(RCode.TOKEN_EXPIRED, "Token 过期"));

        assertThatThrownBy(() -> authService.refreshToken("expired"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(10002);
    }

    @Test
    @DisplayName("refreshToken 员工不存在: 抛 AuthDomainException userNotFound")
    void refreshToken_empMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "refresh");
        map.put("uid", 999L);
        map.put("uname", "ghost");
        Claims claims = Jwts.claims(map);
        when(jwtUtil.parse("good")).thenReturn(claims);
        when(empMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken("good"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("refreshToken 员工已停用: 抛 AuthDomainException userNotFound")
    void refreshToken_empDisabled() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "refresh");
        map.put("uid", 1L);
        map.put("uname", "admin");
        Claims claims = Jwts.claims(map);
        when(jwtUtil.parse("good")).thenReturn(claims);
        when(empMapper.selectById(1L)).thenReturn(inactiveEmp(1L, "admin", "INACTIVE"));

        assertThatThrownBy(() -> authService.refreshToken("good"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("用户名或密码错误");
    }

    // ===================== logout =====================

    @Test
    @DisplayName("logout: 静默清空 UserContext, 不抛异常")
    void logout_clearsContext() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "real", 10L, "技术部",
                "ALL", List.of("ADMIN"), List.of("system:user:list")));
        assertThat(UserContext.get()).isNotNull();

        authService.logout();

        assertThat(UserContext.get()).isNull();
    }

    // ===================== getCurrentUser =====================

    @Test
    @DisplayName("getCurrentUser: UserContext 为空时抛 BizException UNAUTHORIZED")
    void getCurrentUser_noContext() {
        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(10001);
    }

    @Test
    @DisplayName("getCurrentUser: 正常返回组装后的 UserInfoVO (含 deptName + dataScope)")
    void getCurrentUser_normal() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "real", 10L, "技术部",
                "ALL", List.of("ADMIN"), List.of("system:user:list")));

        SysEmp emp = activeEmp(1L, "admin");
        when(empMapper.selectById(1L)).thenReturn(emp);
        when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(role(10L, "ADMIN", "COMPANY")));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                .thenReturn(List.of("system:user:list"));
        when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));

        UserInfoVO info = authService.getCurrentUser();

        assertThat(info.getId()).isEqualTo(1L);
        assertThat(info.getUsername()).isEqualTo("admin");
        assertThat(info.getDeptId()).isEqualTo(10L);
        assertThat(info.getDeptName()).isEqualTo("技术部");
        assertThat(info.getDataScope()).isEqualTo("COMPANY");
        assertThat(info.getRoles()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("getCurrentUser: 员工已被物理删除时抛 userNotFound")
    void getCurrentUser_empMissing() {
        UserContext.set(new UserContext.UserInfo(1L, "ghost", null, null, null,
                null, List.of(), List.of()));
        when(empMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(AuthDomainException.class);
    }

    // ===================== getCurrentUserMenus =====================

    @Test
    @DisplayName("getCurrentUserMenus: UserContext 为空抛 UNAUTHORIZED")
    void getCurrentUserMenus_noContext() {
        assertThatThrownBy(() -> authService.getCurrentUserMenus())
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(10001);
    }

    @Test
    @DisplayName("getCurrentUserMenus: 用户无角色时返回空列表不抛异常")
    void getCurrentUserMenus_noRoles() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of());

        assertThat(authService.getCurrentUserMenus()).isEmpty();

        verify(permissionMapper, never()).selectMenusByRoleIds(anyList());
    }

    @Test
    @DisplayName("getCurrentUserMenus: 1 父 + 2 子菜单按 sort 升序组装为树")
    void getCurrentUserMenus_buildsTree() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        when(permissionMapper.selectMenusByRoleIds(List.of(10L))).thenReturn(List.of(
                permMenu(1L, 0L,  "system",        "系统管理", "/system", "Setting",  1),
                permMenu(11L, 1L, "system:user",   "用户管理", "/system/user", "User", 1),
                permMenu(12L, 1L, "system:role",   "角色管理", "/system/role", "UserFilled", 2)
        ));

        List<MenuTreeVO> menus = authService.getCurrentUserMenus();

        assertThat(menus).hasSize(1);
        MenuTreeVO root = menus.get(0);
        assertThat(root.getId()).isEqualTo(1L);
        assertThat(root.getPermCode()).isEqualTo("system");
        assertThat(root.getChildren()).hasSize(2);
        // 子节点按 sort ASC: system:user (1) < system:role (2)
        assertThat(root.getChildren().get(0).getPermCode()).isEqualTo("system:user");
        assertThat(root.getChildren().get(1).getPermCode()).isEqualTo("system:role");
    }

    @Test
    @DisplayName("getCurrentUserMenus: 多根菜单按 sort ASC 平铺, 子节点各自递归排序")
    void getCurrentUserMenus_multipleRoots() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        when(permissionMapper.selectMenusByRoleIds(List.of(10L))).thenReturn(List.of(
                permMenu(2L, 0L, "hr",     "人事",   "/hr",     "User",  2),
                permMenu(1L, 0L, "system", "系统",   "/system", "Setting", 1),
                permMenu(11L, 1L, "system:user", "用户", "/system/user", "User", 1)
        ));

        List<MenuTreeVO> menus = authService.getCurrentUserMenus();

        // 根: system (sort=1) 在前, hr (sort=2) 在后
        assertThat(menus).extracting(MenuTreeVO::getPermCode)
                .containsExactly("system", "hr");
        // system 节点有 1 个子节点 system:user
        assertThat(menus.get(0).getChildren()).hasSize(1);
        assertThat(menus.get(0).getChildren().get(0).getPermCode()).isEqualTo("system:user");
        // hr 节点无子节点 (children 应为空 list, 不为 null)
        assertThat(menus.get(1).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("getCurrentUserMenus: 孤立节点 (parentId 不在结果集) 提升为根")
    void getCurrentUserMenus_orphanNodeBecomesRoot() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
        // 11 节点的 parentId=99 (不在结果集), 应被提升为根
        when(permissionMapper.selectMenusByRoleIds(List.of(10L))).thenReturn(List.of(
                permMenu(11L, 99L, "orphan", "孤立节点", "/orphan", "Help", 1)
        ));

        List<MenuTreeVO> menus = authService.getCurrentUserMenus();

        assertThat(menus).hasSize(1);
        assertThat(menus.get(0).getPermCode()).isEqualTo("orphan");
        assertThat(menus.get(0).getChildren()).isEmpty();
    }

    // ===================== changePassword =====================

    @Test
    @DisplayName("changePassword: UserContext 为空抛 UNAUTHORIZED")
    void changePassword_noContext() {
        ChangePasswordReq req = new ChangePasswordReq();
        req.setOldPassword("old12345");
        req.setNewPassword("new12345");
        req.setConfirmPassword("new12345");

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(10001);

        verify(empMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("changePassword: 旧密码错误抛 PASSWORD_INVALID, 不更新密码")
    void changePassword_wrongOldPwd() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword(passwordEncoder.encode("old12345"));
        when(empMapper.selectById(1L)).thenReturn(emp);

        ChangePasswordReq req = new ChangePasswordReq();
        req.setOldPassword("wrong-old");
        req.setNewPassword("new12345");
        req.setConfirmPassword("new12345");

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(AuthDomainException.class)
                .hasMessage("用户名或密码错误");

        verify(empMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("changePassword: 新密码与确认密码不一致抛 BAD_REQUEST")
    void changePassword_mismatchConfirm() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword(passwordEncoder.encode("old12345"));
        when(empMapper.selectById(1L)).thenReturn(emp);

        ChangePasswordReq req = new ChangePasswordReq();
        req.setOldPassword("old12345");
        req.setNewPassword("new12345");
        req.setConfirmPassword("different999");

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(BizException.class)
                .hasMessage("两次输入的密码不一致");

        verify(empMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("changePassword: 新密码长度 < 8 抛 PASSWORD_INVALID (RCode 10010)")
    void changePassword_tooShort() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword(passwordEncoder.encode("old12345"));
        when(empMapper.selectById(1L)).thenReturn(emp);

        ChangePasswordReq req = new ChangePasswordReq();
        req.setOldPassword("old12345");
        req.setNewPassword("abc");   // 长度 < 8
        req.setConfirmPassword("abc");

        assertThatThrownBy(() -> authService.changePassword(req))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(10010);

        verify(empMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("changePassword: 成功路径 - updatePassword 写入 BCrypt 哈希 (非明文)")
    void changePassword_success() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                null, List.of(), List.of()));
        SysEmp emp = activeEmp(1L, "admin");
        emp.setPassword(passwordEncoder.encode("old12345"));
        when(empMapper.selectById(1L)).thenReturn(emp);
        when(empMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

        ChangePasswordReq req = new ChangePasswordReq();
        req.setOldPassword("old12345");
        req.setNewPassword("new12345");
        req.setConfirmPassword("new12345");

        authService.changePassword(req);

        // 验证写入的是 BCrypt 哈希, 不是明文
        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        verify(empMapper).updatePassword(eq(1L), hashCap.capture());
        String written = hashCap.getValue();
        assertThat(written).startsWith("$2a$04$");
        assertThat(written).isNotEqualTo("new12345");
        // 新密码能用 BCrypt 验证
        assertThat(passwordEncoder.matches("new12345", written)).isTrue();
    }

    // ===================== BCrypt 切换 + Lazy Rehash 集成测试 =====================

    @Nested
    @DisplayName("BCrypt 集成: 登录/改密走 PasswordEncoder, Lazy Rehash 自动升级")
    class BcryptScenarios {

        @Test
        @DisplayName("login: 库中已是 BCrypt 哈希时直接走 matches, 不触发 Lazy Rehash")
        void login_bcryptHashed_emp() {
            SysEmp emp = activeEmp(1L, "admin");
            String hash = passwordEncoder.encode("admin123");
            emp.setPassword(hash);
            when(empMapper.selectByUsername("admin")).thenReturn(emp);
            when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(role(10L, "ADMIN", "ALL")));
            when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
            when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                    .thenReturn(List.of("system:user:list"));
            when(jwtUtil.generateAccessToken(eq(1L), eq("admin"), anyList(), anyList()))
                    .thenReturn("access");
            when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh");
            when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));

            LoginReq req = new LoginReq();
            req.setUsername("admin");
            req.setPassword("admin123");

            LoginResp resp = authService.login(req, "127.0.0.1");

            assertThat(resp.getAccessToken()).isEqualTo("access");
            // 已经是 BCrypt, 不应触发 updatePassword
            verify(empMapper, never()).updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("login: 库中明文 + Lazy Rehash 开启 → 登录成功, updatePassword 被调用写入 BCrypt 哈希")
        void login_plainText_triggersRehash() {
            SysEmp emp = activeEmp(1L, "admin");
            emp.setPassword("admin123");  // 明文 (V910 状态)
            when(empMapper.selectByUsername("admin")).thenReturn(emp);
            when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(role(10L, "ADMIN", "ALL")));
            when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
            when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                    .thenReturn(List.of("system:user:list"));
            when(jwtUtil.generateAccessToken(eq(1L), eq("admin"), anyList(), anyList()))
                    .thenReturn("access");
            when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh");
            when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));
            when(empMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

            LoginReq req = new LoginReq();
            req.setUsername("admin");
            req.setPassword("admin123");

            LoginResp resp = authService.login(req, "127.0.0.1");

            assertThat(resp.getAccessToken()).isEqualTo("access");
            // 触发 Lazy Rehash: 写入 BCrypt 哈希
            ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
            verify(empMapper).updatePassword(eq(1L), hashCap.capture());
            assertThat(hashCap.getValue()).startsWith("$2a$04$");
            // 写入的 hash 能被相同明文校验通过
            assertThat(passwordEncoder.matches("admin123", hashCap.getValue())).isTrue();
        }

        @Test
        @DisplayName("login: 库中明文 + Lazy Rehash 关闭 → 登录成功, 不回写 updatePassword")
        void login_plainText_rehashDisabled() {
            SecurityProperties.Bcrypt cfg = new SecurityProperties.Bcrypt();
            cfg.setStrength(4);
            cfg.setEnableLazyRehash(false);
            when(securityProperties.getBcrypt()).thenReturn(cfg);

            SysEmp emp = activeEmp(1L, "admin");
            emp.setPassword("admin123");
            when(empMapper.selectByUsername("admin")).thenReturn(emp);
            when(roleMapper.selectByEmpId(1L)).thenReturn(List.of(role(10L, "ADMIN", "ALL")));
            when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L));
            when(rolePermMapper.selectPermCodesByRoleIds(List.of(10L)))
                    .thenReturn(List.of("system:user:list"));
            when(jwtUtil.generateAccessToken(eq(1L), eq("admin"), anyList(), anyList()))
                    .thenReturn("access");
            when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh");
            when(deptMapper.selectById(10L)).thenReturn(dept(10L, "技术部"));

            LoginReq req = new LoginReq();
            req.setUsername("admin");
            req.setPassword("admin123");

            LoginResp resp = authService.login(req, "127.0.0.1");

            assertThat(resp.getAccessToken()).isEqualTo("access");
            // Lazy Rehash 关闭 → 不回写
            verify(empMapper, never()).updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("changePassword: 旧密码是明文, 改密后入库变 BCrypt (兼容老数据)")
        void changePassword_fromPlainText() {
            // 关闭 Lazy Rehash, 只验证 changePassword 自身的 updatePassword 行为
            SecurityProperties.Bcrypt cfg = new SecurityProperties.Bcrypt();
            cfg.setStrength(4);
            cfg.setEnableLazyRehash(false);
            when(securityProperties.getBcrypt()).thenReturn(cfg);

            UserContext.set(new UserContext.UserInfo(1L, "admin", null, null, null,
                    null, List.of(), List.of()));
            SysEmp emp = activeEmp(1L, "admin");
            emp.setPassword("old12345");  // 旧库中明文
            when(empMapper.selectById(1L)).thenReturn(emp);
            when(empMapper.updatePassword(eq(1L), anyString())).thenReturn(1);

            ChangePasswordReq req = new ChangePasswordReq();
            req.setOldPassword("old12345");
            req.setNewPassword("new12345");
            req.setConfirmPassword("new12345");

            authService.changePassword(req);

            // Lazy Rehash 关闭 → 旧密码验证时不会回写, 只由 changePassword 自身写入一次
            ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
            verify(empMapper, times(1)).updatePassword(eq(1L), hashCap.capture());
            assertThat(hashCap.getValue()).startsWith("$2a$04$");
            // 新密码能用 BCrypt 验证
            assertThat(passwordEncoder.matches("new12345", hashCap.getValue())).isTrue();
        }
    }

    // ===================== fixtures =====================

    private static SysEmp activeEmp(Long id, String username) {
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

    private static SysEmp inactiveEmp(Long id, String username, String status) {
        SysEmp emp = activeEmp(id, username);
        emp.setStatus(status);
        return emp;
    }

    private static SysRole role(Long id, String code, String dataScope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleCode(code);
        r.setRoleName(code + "-name");
        r.setDataScope(dataScope);
        r.setStatus("ACTIVE");
        return r;
    }

    private static SysDept dept(Long id, String name) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setDeptName(name);
        d.setDeptCode("D" + id);
        return d;
    }

    private static SysPermission permMenu(Long id, Long parentId, String code, String name,
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
}
