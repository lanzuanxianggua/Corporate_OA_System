package cn.oa.controller;

import cn.oa.entity.LoginDTO;
import cn.oa.entity.LoginVO;
import cn.oa.entity.SysEmployee;
import cn.oa.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("认证管理 - AuthController")
class AuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private LoginVO buildLoginVO() {
        LoginVO vo = new LoginVO();
        vo.setAccessToken("test-token-xxx");
        vo.setRefreshToken("refresh-token-xxx");
        vo.setExpires(new Date(System.currentTimeMillis() + 7200000));
        vo.setUsername("admin");
        vo.setNickname("管理员");
        vo.setAvatar("");
        vo.setRoles(List.of("ROLE_ADMIN"));
        vo.setPermissions(List.of("*:*:*"));
        return vo;
    }

    @Test
    @DisplayName("登录成功")
    void loginSuccess() throws Exception {
        when(authService.login("admin", "123456")).thenReturn(buildLoginVO());

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("test-token-xxx"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.nickname").value("管理员"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"));

        verify(authService, times(1)).login("admin", "123456");
    }

    @Test
    @DisplayName("登录失败 - 账号不存在")
    void loginFailUserNotFound() throws Exception {
        when(authService.login("nonexist", "123456"))
                .thenThrow(new cn.oa.common.exception.BusinessException("员工编号不存在"));

        LoginDTO dto = new LoginDTO();
        dto.setUsername("nonexist");
        dto.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("员工编号不存在"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void loginFailWrongPassword() throws Exception {
        when(authService.login("admin", "wrong"))
                .thenThrow(new cn.oa.common.exception.BusinessException("密码错误"));

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("密码错误"));
    }

    @Test
    @DisplayName("注册成功")
    void registerSuccess() throws Exception {
        doNothing().when(authService).register(any(SysEmployee.class));

        SysEmployee emp = new SysEmployee();
        emp.setUsername("TEST001");
        emp.setEmpName("测试用户");
        emp.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService, times(1)).register(any(SysEmployee.class));
    }
}
