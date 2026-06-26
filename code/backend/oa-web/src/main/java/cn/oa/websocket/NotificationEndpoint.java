package cn.oa.websocket;

import cn.oa.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationEndpoint extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEndpoint.class);

    @Autowired
    private JwtUtil jwtUtil;

    // empId -> session
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long empId = extractEmpIdFromToken(session);
        if (empId != null) {
            session.getAttributes().put("empId", empId);
            WebSocketSession old = sessions.put(empId, session);
            if (old != null && old.isOpen()) {
                old.close(CloseStatus.NORMAL);
            }
            log.info("WebSocket connected: empId={}", empId);
        } else {
            session.close(CloseStatus.POLICY_VIOLATION);
            log.warn("WebSocket connection rejected: invalid or missing token");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long empId = extractEmpIdFromSession(session);
        if (empId != null) {
            sessions.remove(empId, session);
            log.info("WebSocket disconnected: empId={}", empId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // client ping/pong, ignore
    }

    /**
     * Extract empId from JWT token in the query parameter.
     * Rejects connections with invalid or missing tokens.
     */
    private Long extractEmpIdFromToken(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                    .toSingleValueMap();
            String token = params.get("token");
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket: no token provided");
                return null;
            }
            Claims claims = jwtUtil.parseToken(token);
            Object empIdObj = claims.get("empId");
            return (empIdObj instanceof Number) ? ((Number) empIdObj).longValue()
                    : Long.valueOf(empIdObj.toString());
        } catch (Exception e) {
            log.warn("WebSocket: token validation failed: {}", e.getMessage());
            return null;
        }
    }

    /** Fallback for connection close / transport error events where session attributes carry the empId. */
    private Long extractEmpIdFromSession(WebSocketSession session) {
        Long empId = (Long) session.getAttributes().get("empId");
        if (empId == null) {
            // If attributes are empty, try URI as fallback (but this path won't work for new connections)
            try {
                URI uri = session.getUri();
                if (uri == null) return null;
                Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                        .toSingleValueMap();
                String token = params.get("token");
                if (token == null) return null;
                Claims claims = jwtUtil.parseToken(token);
                Object empIdObj = claims.get("empId");
                return (empIdObj instanceof Number) ? ((Number) empIdObj).longValue()
                        : Long.valueOf(empIdObj.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return empId;
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long empId = extractEmpIdFromSession(session);
        if (empId != null) {
            sessions.remove(empId);
            log.warn("WebSocket transport error for empId={}: {}", empId, exception.getMessage());
        }
    }

    /**
     * Send a notification to a specific user.
     */
    public static void sendToUser(Long empId, String message) {
        synchronized (sessions) {
            WebSocketSession session = sessions.get(empId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.warn("Failed to send WebSocket message to empId={}: {}", empId, e.getMessage());
                }
            }
        }
    }
}
