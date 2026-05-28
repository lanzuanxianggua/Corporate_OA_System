package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.constant.BusinessType;
import cn.oa.service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class WorkflowCallbackDispatcher implements WorkflowCallback {

    @Autowired private LeaveApplyService leaveApplyService;
    @Autowired private BusinessTripService businessTripService;
    @Autowired private OutingService outingService;
    @Autowired private PurchaseService purchaseService;
    @Autowired private ExpenseService expenseService;
    @Autowired private OvertimeService overtimeService;
    @Autowired private LoanService loanService;

    private final Map<String, BiConsumer<Long, Integer>> handlers = new HashMap<>();

    @PostConstruct
    public void init() {
        handlers.put(BusinessType.LEAVE, (id, s) -> leaveApplyService.updateStatus(id, s));
        handlers.put(BusinessType.TRIP, (id, s) -> businessTripService.updateStatus(id, s));
        handlers.put(BusinessType.OUTING, (id, s) -> outingService.updateStatus(id, s));
        handlers.put(BusinessType.PURCHASE, (id, s) -> purchaseService.updateStatus(id, s));
        handlers.put(BusinessType.EXPENSE, (id, s) -> expenseService.updateStatus(id, s));
        handlers.put(BusinessType.OVERTIME, (id, s) -> overtimeService.updateStatus(id, s));
        handlers.put(BusinessType.LOAN, (id, s) -> loanService.updateStatus(id, s));
    }

    @Override
    public void onApproved(String businessType, Long businessId) {
        log.info("Workflow approved: businessType={}, businessId={}", businessType, businessId);
        dispatchStatus(businessType, businessId, 1);
    }

    @Override
    public void onRejected(String businessType, Long businessId) {
        log.info("Workflow rejected: businessType={}, businessId={}", businessType, businessId);
        dispatchStatus(businessType, businessId, 2);
    }

    @Override
    public void onWithdrawn(String businessType, Long businessId) {
        log.info("Workflow withdrawn: businessType={}, businessId={}", businessType, businessId);
        dispatchStatus(businessType, businessId, 3);
    }

    private void dispatchStatus(String businessType, Long businessId, int status) {
        BiConsumer<Long, Integer> h = handlers.get(businessType);
        if (h != null) {
            h.accept(businessId, status);
        } else {
            log.warn("No handler registered for businessType={}, skipping status update to {}", businessType, status);
        }
    }
}
