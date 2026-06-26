package cn.oa.common.interceptor;

import cn.oa.common.service.RedisService;
import cn.oa.common.utils.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisService redisService;

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = IpUtil.getClientIp(request);
        String key = "rate:login:" + ip;

        // 原子操作：用 SET NX EX（setIfAbsent）初始创建带 TTL 的 Key
        // 后续请求直接用 INCR（TTL 已在第一步设好）
        Boolean created = redisService.setIfAbsent(key, 1, WINDOW_SECONDS, TimeUnit.SECONDS);
        long count;
        if (Boolean.TRUE.equals(created)) {
            count = 1;  // 首次请求
        } else {
            count = redisService.increment(key);  // 已有 Key，TTL 已在创建时设好
        }

        if (count > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }

        return true;
    }
}
