package cn.oa.service;

public interface NotificationService {
    void notifyApproval(Long empId, String businessType, Long businessId, String action, String remark);
    void notifyTask(Long empId, String businessType, Long businessId, String description);
}
