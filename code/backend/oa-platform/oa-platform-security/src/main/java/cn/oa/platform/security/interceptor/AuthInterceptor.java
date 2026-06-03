package cn.oa.platform.security.interceptor;

import cn.oa.platform.core.constant.Constants;
import cn.oa.platform.core.exception.AuthException;
import cn.oa.platform.security.auth.TokenService;
import cn.oa.platform.security.auth.UserContext;
import cn.oa.platform.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 拦截请求并验证 Token
 *
 * @author oa-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法，直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 获取 Token
        String token = getTokenFromRequest(request);
        if (token == null || token.isEmpty()) {
            log.warn("请求未携带Token, URI={}", request.getRequestURI());
            throw new AuthException(401, "未授权，请先登录");
        }

        // 验证 Token
        if (!tokenService.validateToken(token)) {
            log.warn("Token验证失败, URI={}", request.getRequestURI());
            throw new AuthException(401, "Token无效或已过期，请重新登录");
        }

        // 解析并设置用户上下文
        UserContext userContext = tokenService.parseAndSetUserContext(token);
        log.debug("用户认证成功, empId={}, username={}, URI={}",
                userContext.getEmpId(), userContext.getUsername(), request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除用户上下文，防止内存泄漏
        tokenService.clearUserContext();
    }

    /**
     * 从请求中获取 Token
     *
     * @param request HTTP 请求
     * @return Token 或 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从 Header 获取
        String authorization = request.getHeader(Constants.TOKEN_HEADER);
        if (authorization != null && authorization.startsWith(Constants.TOKEN_PREFIX)) {
            return authorization.substring(Constants.TOKEN_PREFIX.length());
        }

        // 从 Query 参数获取（兼容 WebSocket 等）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        return null;
    }
}