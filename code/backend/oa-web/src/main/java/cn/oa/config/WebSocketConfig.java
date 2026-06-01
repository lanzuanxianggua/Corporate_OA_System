package cn.oa.config;

import cn.oa.common.service.RedisService;
import cn.oa.common.utils.JwtUtil;
import cn.oa.websocket.NotificationEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    public WebSocketConfig(JwtUtil jwtUtil, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }

    @Bean
    public NotificationEndpoint notificationEndpoint() {
        return new NotificationEndpoint();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationEndpoint(), "/ws/notification")
                .setAllowedOrigins(
                        "http://localhost:8848",
                        "http://localhost:5173",
                        "http://localhost:3000"
                )
                .addInterceptors(new WebSocketAuthInterceptor(jwtUtil, redisService));
    }
}
