package cn.oa.system.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.system.dto.LoginReq;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.service.AuthService;
import cn.oa.system.service.CaptchaService;
import cn.oa.system.vo.CaptchaResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证 Controller.
 */
@Tag(name = "认证", description = "登录/登出/当前用户")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService,
                           JwtUtil jwtUtil,
                           @Autowired(required = false) CaptchaService captchaService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginReq req,
                                        HttpServletRequest request) {
        var resp = authService.login(req, clientIp(request));
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", resp.getAccessToken());
        data.put("refreshToken", resp.getRefreshToken());
        data.put("tokenType", resp.getTokenType());
        data.put("expiresIn", resp.getExpiresIn());
        data.put("userInfo", resp.getUserInfo());
        return R.ok("登录成功", data);
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestBody @jakarta.validation.Valid RefreshRequest req) {
        var resp = authService.refreshToken(req.refreshToken());
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", resp.getAccessToken());
        data.put("refreshToken", resp.getRefreshToken());
        data.put("tokenType", resp.getTokenType());
        data.put("expiresIn", resp.getExpiresIn());
        return R.ok(data);
    }

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public R<CaptchaResp> captcha() {
        if (captchaService == null) {
            // 测试环境 / 未启用 captcha 时的兜底
            return R.fail(10012, "图形验证码服务未启用");
        }
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    @RequirePermission("system:user:view")
    public R<Map<String, Object>> me() {
        var ctx = cn.oa.platform.common.context.UserContext.get();
        if (ctx == null) {
            return R.fail(10001, "未登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("empId", ctx.getEmpId());
        data.put("username", ctx.getUsername());
        data.put("roles", ctx.getRoles());
        data.put("permissions", ctx.getPermissions());
        return R.ok(data);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {}
}
