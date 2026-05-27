package cn.oa.service.notification.impl;

import cn.oa.service.notification.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EmailChannel implements NotificationChannel {

    @Override
    public String getChannelCode() {
        return "email";
    }

    @Override
    public void send(Long empId, String title, String content, Map<String, Object> extra) {
        // Email sending requires JavaMailSender + SMTP config in application.yml
        // Placeholder: log the notification for now
        log.info("[Email Notification] empId={}, title={}, content={}", empId, title, content);
    }
}
