package cn.oa.websocket;

import cn.oa.common.websocket.WebSocketSender;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSenderImpl implements WebSocketSender {
    @Override
    public void sendToUser(Long empId, String message) {
        NotificationEndpoint.sendToUser(empId, message);
    }
}
