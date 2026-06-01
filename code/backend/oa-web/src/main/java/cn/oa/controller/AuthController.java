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

    @PostMapping("/login")
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
