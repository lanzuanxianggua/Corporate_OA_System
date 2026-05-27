package cn.oa.controller;

import cn.oa.common.service.RedisService;
import cn.oa.entity.LoginDTO;
import cn.oa.entity.LoginVO;
import cn.oa.service.AuthService;
import cn.oa.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private RedisService redisService;

    private LoginVO buildLoginVO() {
        LoginVO vo = new LoginVO();
        vo.setAccessToken("test-token-xxx");
        vo.setRefreshToken("refresh-token-xxx");
        vo.setExpires(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        vo.setUsername("admin");
        vo.setNickname("管理员");
        vo.setAvatar("");
        vo.setRoles(List.of("ROLE_ADMIN"));
        vo.setPermissions(List.of("*:*:*"));
        return vo;
    }

    @Test
    @DisplayName("获取验证码")
    void captcha() throws Exception {
        mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uuid").exists());
    }

    @Test
    @DisplayName("登录成功")
    void loginSuccess() throws Exception {
        // Mock captcha: get returns the answer, delete is a no-op
        when(redisService.get("captcha:test-uuid")).thenReturn("abcd");
        when(redisService.delete("captcha:test-uuid")).thenReturn(true);
        when(authService.login(eq("admin"), eq("123456"), any(HttpServletRequest.class))).thenReturn(buildLoginVO());

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        dto.setCaptchaUuid("test-uuid");
        dto.setCaptchaCode("abcd");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("test-token-xxx"));
    }

    @Test
    @DisplayName("登录失败 - 验证码错误")
    void loginFailCaptchaError() throws Exception {
        // Mock captcha: get returns null -> verify fails
        when(redisService.get("captcha:bad-uuid")).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");
        dto.setCaptchaUuid("bad-uuid");
        dto.setCaptchaCode("wrong");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void loginFailWrongPassword() throws Exception {
        // Mock captcha to pass
        when(redisService.get("captcha:test-uuid")).thenReturn("abcd");
        when(authService.login(eq("admin"), eq("wrong"), any(HttpServletRequest.class)))
                .thenThrow(new cn.oa.common.exception.BusinessException("密码错误"));

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong");
        dto.setCaptchaUuid("test-uuid");
        dto.setCaptchaCode("abcd");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }
}
