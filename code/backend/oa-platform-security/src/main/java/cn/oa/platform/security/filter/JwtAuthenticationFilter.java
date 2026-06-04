package cn.oa.platform.security.filter;

import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.exception.BizException;
import cn.oa.platform.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证 Filter.
 *
 * <p>解析 Authorization Header, 验证 JWT, 写入 UserContext.
 * 业务侧通过 cn.oa.platform.common.context.UserContext 获取当前用户.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final String headerName;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, String headerName) {
        this.jwtUtil = jwtUtil;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.resolveToken(request.getHeader(headerName));
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtUtil.parse(token);
                JwtUtil.UserInfo info = jwtUtil.extract(claims);

                UserContext.UserInfo ctx = new UserContext.UserInfo(
                        info.getEmpId(),
                        info.getUsername(),
                        null,
                        null,
                        null,
                        null,
                        info.getRoles(),
                        info.getPermissions());
                UserContext.set(ctx);

            } catch (BizException e) {
                log.debug("JWT auth failed: {}", e.getMessage());
                UserContext.clear();
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
