package cn.oa.service.notification;

import java.util.Map;

public interface NotificationChannel {
    String getChannelCode();
    void send(Long empId, String title, String content, Map<String, Object> extra);
}
