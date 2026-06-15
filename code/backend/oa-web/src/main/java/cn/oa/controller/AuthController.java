package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.R;
import cn.oa.common.service.RedisService;
import cn.oa.common.utils.CaptchaUtil;
import cn.oa.common.utils.CaptchaUtil.CaptchaResult;
import cn.oa.common.utils.PasswordUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.LoginDTO;
import cn.oa.vo.LoginVO;
import cn.oa.service.AuthService;
import cn.oa.service.EmployeeService;
import cn.hutool.crypto.digest.BCrypt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

@Slf4j
@RestController
@Tag(name = "认证管理")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RedisService redisService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/api/auth/captcha")
    @Operation(summary = "获取验证码")
    public R<Map<String, String>> captcha() {
        CaptchaResult result = CaptchaUtil.generate(redisService);
        return R.ok(Map.of("uuid", result.getUuid(), "img", result.getImg()));
    }

    @PostMapping({"/login", "/api/auth/login"})
    @Operation(summary = "登录")
    @OperationLog(module = "认证管理", operation = "用户登录")
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        if (!CaptchaUtil.verify(redisService, dto.getCaptchaUuid(), dto.getCaptchaCode())) {
            return R.fail("验证码错误或已过期");
        }
        LoginVO vo = authService.login(dto.getUsername(), dto.getPassword(), request);
        log.info("User logged in: username={}", dto.getUsername());
        return R.ok(vo);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新Token")
    public R<LoginVO> refreshToken(@RequestBody @Valid RefreshTokenDTO dto) {
        LoginVO vo = authService.refreshToken(dto.getRefreshToken());
        return R.ok(vo);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    @OperationLog(module = "认证管理", operation = "用户登出")
    public R<Void> logout(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId != null) {
            authService.logout(empId);
            log.info("User logged out: empId={}", empId);
        }
        return R.ok();
    }

    @PostMapping("/api/auth/change-password")
    @Operation(summary = "修改密码")
    @OperationLog(module = "认证管理", operation = "修改密码")
    public R<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("未登录");
        }
        SysEmployee employee = employeeService.getById(empId);
        if (employee == null) {
            return R.fail("用户不存在");
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), employee.getPassword())) {
            return R.fail("旧密码不正确");
        }
        String strength = PasswordUtil.checkPasswordStrength(dto.getNewPassword());
        if ("weak".equals(strength)) {
            return R.fail("新密码强度过弱，密码长度至少8位且必须包含字母和数字");
        }
        PasswordUtil.validatePassword(dto.getNewPassword());
        employee.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        employeeService.updateById(employee);
        // 密码修改成功后，使旧Token立即失效
        Long currentEmpId = WebUtil.getEmpId(request);
        if (currentEmpId != null) {
            redisTemplate.delete("token:" + currentEmpId);
            redisTemplate.delete("refreshToken:" + currentEmpId);
        }
        log.info("Password changed: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/api/auth/register")
    @Operation(summary = "用户注册")
    @OperationLog(module = "认证管理", operation = "用户注册")
    public R<Void> register(@RequestBody @Valid RegisterDTO dto) {
        // 验证用户名是否已存在
        SysEmployee existingUser = employeeService.getByEmpCode(dto.getUsername());
        if (existingUser != null) {
            return R.fail("用户名已存在");
        }

        // 验证邮箱是否已存在
        SysEmployee existingEmail = employeeService.getByEmail(dto.getEmail());
        if (existingEmail != null) {
            return R.fail("邮箱已被注册");
        }

        // 密码长度验证
        if (dto.getPassword().length() < 6) {
            return R.fail("密码长度至少6位");
        }

        // 创建新用户
        SysEmployee employee = new SysEmployee();
        employee.setEmpCode(dto.getUsername()); // 使用 empCode 作为登录用户名
        employee.setEmpName(dto.getUsername()); // 默认员工姓名为用户名
        employee.setEmail(dto.getEmail());
        employee.setPassword(BCrypt.hashpw(dto.getPassword()));
        employee.setStatus(1); // 默认启用

        authService.register(employee);
        log.info("User registered: username={}", dto.getUsername());
        return R.ok();
    }

    @PostMapping("/api/auth/forgot-password")
    @Operation(summary = "忘记密码")
    @OperationLog(module = "认证管理", operation = "忘记密码")
    public R<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        // 验证邮箱是否存在
        SysEmployee employee = employeeService.getByEmail(dto.getEmail());
        if (employee == null) {
            return R.fail("该邮箱未注册");
        }

        // TODO: 发送重置密码邮件
        // 1. 生成重置令牌
        // 2. 保存到 Redis（设置过期时间 30 分钟）
        // 3. 发送包含重置链接的邮件

        log.info("Password reset requested: email={}", dto.getEmail());
        return R.ok();
    }

    @Data
    public static class RegisterDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class ForgotPasswordDTO {
        @NotBlank(message = "邮箱不能为空")
        private String email;
    }

    @Data
    public static class RefreshTokenDTO {
        @NotBlank(message = "refreshToken不能为空")
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordDTO {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
