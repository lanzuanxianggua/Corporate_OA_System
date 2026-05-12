package cn.oa.common.interceptor;

import cn.oa.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_KEY_PREFIX = "online:user:";
    private static final long ONLINE_TTL_MINUTES = 30;

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
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            log.error("Token 解析失败：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        Long empId = claims.get("empId", Long.class);
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

        // 续期在线用户 TTL
        String onlineKey = ONLINE_KEY_PREFIX + empId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey))) {
            redisTemplate.expire(onlineKey, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return true;
    }
}
