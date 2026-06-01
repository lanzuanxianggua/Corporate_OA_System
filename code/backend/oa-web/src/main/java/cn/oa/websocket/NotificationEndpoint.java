package cn.oa.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationEndpoint extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEndpoint.class);

    // empId -> session
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long empId = extractEmpId(session);
        if (empId != null) {
            WebSocketSession old = sessions.put(empId, session);
            if (old != null && old.isOpen()) {
                old.close(CloseStatus.NORMAL);
            }
            log.info("WebSocket connected: empId={}", empId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long empId = extractEmpId(session);
        if (empId != null) {
            sessions.remove(empId, session);
            log.info("WebSocket disconnected: empId={}", empId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // client ping/pong, ignore
    }

    private Long extractEmpId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) return null;
            String query = uri.getQuery();
            if (query == null) return null;
            Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                    .toSingleValueMap();
            String empIdStr = params.get("empId");
            return empIdStr != null ? Long.valueOf(empIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long empId = extractEmpId(session);
        if (empId != null) {
            synchronized (sessions) {
                sessions.remove(empId);
            }
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
