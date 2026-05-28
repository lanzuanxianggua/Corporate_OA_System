package cn.oa.common.interceptor;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.annotation.RequireRole;
import cn.oa.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String ONLINE_KEY_PREFIX = "online:user:";
    private static final long ONLINE_TTL_MINUTES = 30;

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put("USER", Set.of(
                "attendance:checkin", "attendance:list",
                "leave:apply", "leave:list",
                "notice:list", "notice:read",
                "document:list", "document:download",
                "schedule:list", "schedule:add",
                "message:list", "message:read",
                "report:personal"
        ));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            log.error("Token 解析失败：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        Object empIdObj = claims.get("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        String redisTokenKey = "token:" + empId;
        Object cachedToken = redisTemplate.opsForValue().get(redisTokenKey);
        if (cachedToken == null || !token.equals(cachedToken.toString())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 已失效，请重新登录\",\"data\":null}");
            return false;
        }

        String empName = claims.get("empName", String.class);
        request.setAttribute("empId", empId);
        request.setAttribute("empName", empName);

        // 管理员权限校验
        if (handler instanceof HandlerMethod handlerMethod) {
            RequireAdmin methodAnnotation = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            RequireAdmin classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
            if (methodAnnotation != null || classAnnotation != null) {
                String rolesKey = "roles:" + empId;
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) redisTemplate.opsForValue().get(rolesKey);
                if (roles == null || roles.stream().noneMatch(r -> "ADMIN".equalsIgnoreCase(r))) {
                    log.warn("用户 {} (empId={}) 尝试访问管理员接口被拒绝", empName, empId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"权限不足，需要管理员权限\",\"data\":null}");
                    return false;
                }
            }

            // 角色校验 (@RequireRole)
            RequireRole methodRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            RequireRole classRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            RequireRole requireRole = methodRole != null ? methodRole : classRole;
            if (requireRole != null && requireRole.value().length > 0) {
                String rolesKey = "roles:" + empId;
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) redisTemplate.opsForValue().get(rolesKey);
                boolean isAdmin = roles != null && roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r));
                if (isAdmin) {
                    // ADMIN bypasses all role checks
                } else {
                    boolean hasRequiredRole = roles != null && roles.stream().anyMatch(userRole -> {
                        for (String required : requireRole.value()) {
                            if (required.equalsIgnoreCase(userRole)) return true;
                        }
                        return false;
                    });
                    if (!hasRequiredRole) {
                        log.warn("用户 {} (empId={}) 缺少所需角色: {}", empName, empId, String.join(", ", requireRole.value()));
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"message\":\"权限不足，需要指定角色\",\"data\":null}");
                        return false;
                    }
                }
            }

            // 细粒度权限校验
            RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (requirePermission != null && !requirePermission.value().isEmpty()) {
                String rolesKey = "roles:" + empId;
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) redisTemplate.opsForValue().get(rolesKey);
                if (roles == null || roles.stream().noneMatch(r ->
                        hasPermission(r, requirePermission.value()))) {
                    log.warn("用户 {} (empId={}) 缺少权限: {}", empName, empId, requirePermission.value());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"权限不足\",\"data\":null}");
                    return false;
                }
            }
        }

        // 续期在线用户 TTL
        String onlineKey = ONLINE_KEY_PREFIX + empId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey))) {
            redisTemplate.expire(onlineKey, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return true;
    }

    private boolean hasPermission(String role, String permission) {
        Set<String> perms = ROLE_PERMISSIONS.get(role);
        if (perms == null) return false;
        if (perms.contains(permission)) return true;
        if ("ADMIN".equalsIgnoreCase(role)) return true;
        return false;
    }
}
