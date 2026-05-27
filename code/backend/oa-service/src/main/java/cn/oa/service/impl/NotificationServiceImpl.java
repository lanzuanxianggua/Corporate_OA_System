package cn.oa.service.impl;

import cn.oa.service.NotificationService;
import cn.oa.service.notification.NotificationChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private List<NotificationChannel> channels;

    @Override
    public void notifyApproval(Long empId, String businessType, Long businessId, String action, String remark) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("type", "approval");
        extra.put("businessType", businessType);
        extra.put("businessId", businessId);
        extra.put("action", action);
        extra.put("remark", remark);
        String title = "审批通知";
        String content = "您的 " + businessType + " 申请已" + ("approved".equals(action) ? "通过" : "rejected".equals(action) ? "驳回" : "更新");
        dispatch(empId, title, content, extra);
    }

    @Override
    public void notifyTask(Long empId, String businessType, Long businessId, String description) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("type", "task");
        extra.put("businessType", businessType);
        extra.put("businessId", businessId);
        extra.put("description", description);
        dispatch(empId, "审批任务", description, extra);
    }

    private void dispatch(Long empId, String title, String content, Map<String, Object> extra) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(empId, title, content, extra);
            } catch (Exception ignored) {
            }
        }
    }
}
