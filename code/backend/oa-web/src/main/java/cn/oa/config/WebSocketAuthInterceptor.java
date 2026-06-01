package cn.oa.config;

import cn.oa.common.service.RedisService;
import cn.oa.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器 — 通过 JWT Token 验证连接身份
 */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query == null) {
            log.warn("WebSocket handshake rejected: no query string");
            return false;
        }

        // 从查询参数中提取 token 和 empId
        Map<String, String> params = parseQuery(query);
        String token = params.get("token");
        String empIdStr = params.get("empId");

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket handshake rejected: missing token");
            return false;
        }

        try {
            // 去掉 Bearer 前缀（如果有）
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 验证 JWT Token 有效性
            Claims claims = jwtUtil.parseToken(token);
            Long tokenEmpId = claims.get("empId", Long.class);

            // 如果同时传了 empId，校验 token 中的 empId 与参数一致
            if (empIdStr != null && !empIdStr.isEmpty()) {
                Long paramEmpId = Long.valueOf(empIdStr);
                if (!tokenEmpId.equals(paramEmpId)) {
                    log.warn("WebSocket handshake rejected: token empId={} does not match param empId={}", tokenEmpId, paramEmpId);
                    return false;
                }
            }

            // 校验 Token 在 Redis 中是否有效（未被登出）
            String redisKey = "token:" + tokenEmpId;
            Object storedToken = redisService.get(redisKey);
            if (storedToken == null) {
                log.warn("WebSocket handshake rejected: no active session for empId={}", tokenEmpId);
                return false;
            }

            // 将 empId 存入 WebSocket attributes，供 Endpoint 使用
            attributes.put("empId", tokenEmpId);
            log.info("WebSocket handshake authenticated: empId={}", tokenEmpId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: invalid token - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                params.put(key, java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return params;
    }
}
