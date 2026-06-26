package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.common.utils.JwtUtil;
import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.OnlineUserService;
import cn.oa.vo.LoginVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import cn.hutool.crypto.digest.BCrypt;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private SysEmployeeMapper employeeMapper;

    @Mock
    private SysEmpRoleMapper empRoleMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private OnlineUserService onlineUserService;

    @Mock
    private OaLoginLogMapper loginLogMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private SysEmployee employee;
    private String passwordHash;

    @BeforeEach
    void setUp() {
        passwordHash = BCrypt.hashpw("password123");

        employee = new SysEmployee();
        employee.setId(100L);
        employee.setEmpCode("EMP001");
        employee.setEmpName("张三");
        employee.setPassword(passwordHash);
        employee.setAvatar("http://avatar.url");
    }

    // ========== login ==========

    @Test
    void login_success() {
        when(employeeMapper.selectOne(any())).thenReturn(employee);

        List<SysEmpRole> empRoles = new ArrayList<>();
        SysEmpRole empRole = new SysEmpRole();
        empRole.setEmpId(100L);
        empRole.setRoleId(1L);
        empRoles.add(empRole);
        when(empRoleMapper.selectList(any())).thenReturn(empRoles);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("USER");
        when(roleMapper.selectBatchIds(any())).thenReturn(Arrays.asList(role));

        when(jwtUtil.generateToken(100L, "张三")).thenReturn("access-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        LoginVO vo = authService.login("EMP001", "password123", request);

        assertNotNull(vo);
        assertEquals("access-token", vo.getAccessToken());
        assertEquals("access-token", vo.getRefreshToken());
        assertEquals("张三", vo.getNickname());
        assertEquals("EMP001", vo.getUsername());
        assertTrue(vo.getRoles().contains("USER"));

        verify(redisTemplate, times(3)).opsForValue();
        verify(onlineUserService).userLogin(eq(100L), eq("张三"), any(), any());
        verify(loginLogMapper).insert(any(OaLoginLog.class));
    }

    @Test
    void login_userNotFound_throws() {
        when(employeeMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login("UNKNOWN", "pwd", request));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void login_wrongPassword_throws() {
        when(employeeMapper.selectOne(any())).thenReturn(employee);
        // request needed for login log
        when(request.getHeader(eq("User-Agent"))).thenReturn("Mozilla/5.0");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login("EMP001", "wrong-password", request));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    @Test
    void login_noRole_defaultsToUser() {
        when(employeeMapper.selectOne(any())).thenReturn(employee);
        when(empRoleMapper.selectList(any())).thenReturn(new ArrayList<>()); // no roles
        when(jwtUtil.generateToken(100L, "张三")).thenReturn("access-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        LoginVO vo = authService.login("EMP001", "password123", request);

        assertTrue(vo.getRoles().contains("USER"));
    }

    @Test
    void login_withAdminRole_hasWildcardPermission() {
        when(employeeMapper.selectOne(any())).thenReturn(employee);

        List<SysEmpRole> empRoles = new ArrayList<>();
        SysEmpRole empRole = new SysEmpRole();
        empRole.setEmpId(100L);
        empRole.setRoleId(1L);
        empRoles.add(empRole);
        when(empRoleMapper.selectList(any())).thenReturn(empRoles);

        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleKey("ADMIN");
        when(roleMapper.selectBatchIds(any())).thenReturn(Arrays.asList(adminRole));

        when(jwtUtil.generateToken(100L, "张三")).thenReturn("access-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        LoginVO vo = authService.login("EMP001", "password123", request);

        // Login always sets wildcard permission *:*:*
        assertTrue(vo.getPermissions().contains("*:*:*"));
        assertTrue(vo.getRoles().contains("ADMIN"));
    }

    // ========== logout ==========

    @Test
    void logout_clearsCache() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        authService.logout(100L);

        verify(redisTemplate, times(2)).delete(anyString());
        verify(onlineUserService).userLogout(100L);
    }

    // ========== refreshToken ==========

    @Test
    void refreshToken_success() {
        String oldRefresh = "old-refresh-token";

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("token:" + 100L)).thenReturn(oldRefresh);

        Claims claims = mock(Claims.class);
        when(claims.get("empId", Long.class)).thenReturn(100L);
        when(claims.get("empName", String.class)).thenReturn("张三");
        when(claims.get("tokenType", String.class)).thenReturn("refresh");
        when(jwtUtil.parseToken(oldRefresh)).thenReturn(claims);

        when(jwtUtil.generateToken(100L, "张三")).thenReturn("new-access-token");

        LoginVO vo = authService.refreshToken(oldRefresh);

        assertNotNull(vo);
        assertEquals("new-access-token", vo.getAccessToken());
        assertEquals("new-access-token", vo.getRefreshToken());
    }

    @Test
    void refreshToken_invalidTokenType_throws() {
        String staleToken = "access-token";

        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("access");
        when(jwtUtil.parseToken(staleToken)).thenReturn(claims);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.refreshToken(staleToken));
        assertTrue(ex.getMessage().contains("无效"));
    }

    @Test
    void refreshToken_staleToken_throws() {
        String oldRefresh = "expired-refresh-token";

        Claims claims = mock(Claims.class);
        when(claims.get("empId", Long.class)).thenReturn(100L);
        when(claims.get("empName", String.class)).thenReturn("张三");
        when(claims.get("tokenType", String.class)).thenReturn("refresh");
        when(jwtUtil.parseToken(oldRefresh)).thenReturn(claims);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refreshToken:" + 100L)).thenReturn("different-token"); // not matched

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.refreshToken(oldRefresh));
        assertTrue(ex.getMessage().contains("已失效"));
    }

    // ========== register ==========

    @Test
    void register_success() {
        SysEmployee newEmp = new SysEmployee();
        newEmp.setEmpCode("EMP002");
        newEmp.setEmpName("李四");
        newEmp.setPassword("password456");

        when(employeeMapper.insert(any(SysEmployee.class))).thenReturn(1);

        authService.register(newEmp);

        assertNotNull(newEmp.getPassword());
        assertTrue(newEmp.getPassword().startsWith("$2a$"));
        verify(employeeMapper).insert(newEmp);
    }
}
