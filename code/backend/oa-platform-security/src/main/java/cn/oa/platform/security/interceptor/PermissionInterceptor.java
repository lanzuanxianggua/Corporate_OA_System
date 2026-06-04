package cn.oa.platform.security.interceptor;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.exception.AuthException;
import cn.oa.platform.common.exception.ForbiddenException;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.platform.security.annotation.RequireRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 权限拦截器.
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PermissionInterceptor.class);

    private final ObjectMapper objectMapper;

    public PermissionInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequirePermission requirePerm = hm.getMethodAnnotation(RequirePermission.class);
        if (requirePerm == null) {
            requirePerm = hm.getBeanType().getAnnotation(RequirePermission.class);
        }
        RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requirePerm == null && requireRole == null) {
            return true;
        }

        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new AuthException();
        }

        if (requirePerm != null && !checkPermission(user, requirePerm)) {
            log.warn("Permission denied: user={}, required={}", user.getUsername(), Arrays.toString(requirePerm.value()));
            throw new ForbiddenException("缺少权限: " + String.join(",", requirePerm.value()));
        }
        if (requireRole != null && !checkRole(user, requireRole)) {
            log.warn("Role denied: user={}, required={}", user.getUsername(), Arrays.toString(requireRole.value()));
            throw new ForbiddenException("缺少角色: " + String.join(",", requireRole.value()));
        }
        return true;
    }

    private boolean checkPermission(UserContext.UserInfo user, RequirePermission anno) {
        List<String> userPerms = user.getPermissions() == null ? List.of() : user.getPermissions();
        if (anno.logical() == RequirePermission.Logical.OR) {
            return Arrays.stream(anno.value()).anyMatch(userPerms::contains);
        }
        return Arrays.stream(anno.value()).allMatch(userPerms::contains);
    }

    private boolean checkRole(UserContext.UserInfo user, RequireRole anno) {
        List<String> userRoles = user.getRoles() == null ? List.of() : user.getRoles();
        if (anno.logical() == RequirePermission.Logical.OR) {
            return Arrays.stream(anno.value()).anyMatch(userRoles::contains);
        }
        return Arrays.stream(anno.value()).allMatch(userRoles::contains);
    }
}
