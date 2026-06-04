package cn.oa.system.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
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

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginRequest req,
                                        HttpServletRequest request) {
        SysEmp emp = authService.findByUsername(req.username());
        if (emp == null) {
            return R.fail(10001, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(emp.getStatus())) {
            return R.fail(10001, "账号已停用");
        }
        if (!matchesPassword(req.password(), emp.getPassword())) {
            return R.fail(10001, "用户名或密码错误");
        }
        List<String> roles = authService.findRolesByEmpId(emp.getId());
        List<String> perms = authService.findPermCodesByEmpId(emp.getId());

        String access = jwtUtil.generateAccessToken(emp.getId(), emp.getUsername(), roles, perms);
        String refresh = jwtUtil.generateRefreshToken(emp.getId(), emp.getUsername());
        authService.recordLogin(emp.getId(), clientIp(request));

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", access);
        data.put("refreshToken", refresh);
        data.put("tokenType", "Bearer");
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("empId", emp.getId());
        userInfo.put("empCode", emp.getEmpCode());
        userInfo.put("username", emp.getUsername());
        userInfo.put("realName", emp.getRealName());
        userInfo.put("avatar", emp.getAvatar());
        userInfo.put("deptId", emp.getDeptId());
        userInfo.put("roles", roles);
        userInfo.put("permissions", perms);
        data.put("userInfo", userInfo);

        return R.ok("登录成功", data);
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@RequestBody @jakarta.validation.Valid RefreshRequest req) {
        var claims = jwtUtil.parse(req.refreshToken());
        Long empId = claims.get("uid", Long.class);
        String username = claims.get("uname", String.class);
        if (!"refresh".equals(claims.get("type", String.class))) {
            return R.fail(10003, "非刷新 Token");
        }
        List<String> roles = authService.findRolesByEmpId(empId);
        List<String> perms = authService.findPermCodesByEmpId(empId);
        String access = jwtUtil.generateAccessToken(empId, username, roles, perms);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", access);
        return R.ok(data);
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

    private boolean matchesPassword(String raw, String hashed) {
        // v2 Phase 2 简化: 明文比较 (生产应使用 BCrypt)
        return hashed != null && hashed.equals(raw);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {}

    public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {}
}
