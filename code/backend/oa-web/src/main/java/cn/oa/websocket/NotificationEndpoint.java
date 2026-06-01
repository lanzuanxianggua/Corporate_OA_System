package cn.oa.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationEndpoint extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEndpoint.class);

    // empId -> session
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long empId = getEmpIdFromAttributes(session);
        if (empId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        WebSocketSession old = sessions.put(empId, session);
        if (old != null && old.isOpen()) {
            old.close(CloseStatus.NORMAL);
        }
        log.info("WebSocket connected: empId={}", empId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long empId = getEmpIdFromAttributes(session);
        if (empId != null) {
            sessions.remove(empId, session);
            log.info("WebSocket disconnected: empId={}", empId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long empId = getEmpIdFromAttributes(session);
        if (empId != null) {
            sessions.remove(empId, session);
            log.error("WebSocket transport error for empId={}: {}", empId,
                    exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // client ping/pong, ignore
    }

    /**
     * 从握手拦截器设置的 attributes 中获取已认证的 empId
     */
    private Long getEmpIdFromAttributes(WebSocketSession session) {
        Object empId = session.getAttributes().get("empId");
        return empId instanceof Long ? (Long) empId : null;
    }

    /**
     * Send a notification to a specific user.
     */
    public static void sendToUser(Long empId, String message) {
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
