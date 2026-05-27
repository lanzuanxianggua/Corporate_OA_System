package cn.oa.service;

public interface WorkflowCallback {
    void onApproved(String businessType, Long businessId);
    void onRejected(String businessType, Long businessId);
    void onWithdrawn(String businessType, Long businessId);
}
