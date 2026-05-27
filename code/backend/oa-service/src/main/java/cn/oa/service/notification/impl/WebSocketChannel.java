package cn.oa.service.notification.impl;

import cn.oa.common.websocket.WebSocketSender;
import cn.oa.service.notification.NotificationChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketChannel implements NotificationChannel {

    @Autowired
    private WebSocketSender webSocketSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getChannelCode() {
        return "websocket";
    }

    @Override
    public void send(Long empId, String title, String content, Map<String, Object> extra) {
        try {
            Map<String, Object> msg = new HashMap<>(extra);
            msg.put("title", title);
            msg.put("content", content);
            String json = objectMapper.writeValueAsString(msg);
            webSocketSender.sendToUser(empId, json);
        } catch (Exception ignored) {
        }
    }
}
