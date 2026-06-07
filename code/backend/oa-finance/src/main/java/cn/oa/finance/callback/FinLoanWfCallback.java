package cn.oa.finance.callback;

import cn.oa.finance.entity.FinLoan;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinLoanMapper;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 借款流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey (前缀 {@code LOAN_})
 * 得到 loanId, 依据终态:
 * <ul>
 *   <li>APPROVED — 状态置 APPROVED, 兜底设置 deadlineDate = +30 天</li>
 *   <li>REJECTED — 状态置 REJECTED</li>
 * </ul>
 *
 * <p>通知派发由 oa-message 的 {@code WfInstanceNotifyListener} 统一处理 (按
 * businessKey 前缀 + 状态选择 {@code TYPE_LOAN_APPROVE/REJECT} 模板).
 *
 * <p>借款不涉及预算冻结/解冻, 仅做状态回写.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinLoanWfCallback {

    /** 业务前缀: oa-finance Service 约定 "LOAN_" + loanId. */
    static final String BIZ_PREFIX = "LOAN_";

    private final FinLoanMapper loanMapper;

    /**
     * 监听流程完成事件.
     */
    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[FinLoanCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[FinLoanCallback] 非借款业务流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long loanId = parseLoanId(businessKey);
            if (loanId == null) {
                log.warn("[FinLoanCallback] 解析 loanId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (FinConstants.LOAN_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(loanId);
            } else if (FinConstants.LOAN_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(loanId);
            } else {
                log.info("[FinLoanCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (Exception ex) {
            log.error("[FinLoanCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long loanId) {
        FinLoan loan = loadLoan(loanId);
        if (loan == null) return;
        if (FinConstants.LOAN_STATUS_APPROVED.equals(loan.getStatus())
                || FinConstants.LOAN_STATUS_SETTLED.equals(loan.getStatus())) {
            log.info("[FinLoanCallback] 借款已是 APPROVED/SETTLED 终态, 幂等跳过: loanId={}", loanId);
            return;
        }
        if (!FinConstants.LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            log.warn("[FinLoanCallback] 借款单非 PENDING, 跳过: loanId={}, status={}", loanId, loan.getStatus());
            return;
        }
        loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
        if (loan.getDeadlineDate() == null) {
            loan.setDeadlineDate(LocalDate.now().plusDays(30));
        }
        loanMapper.updateById(loan);
        log.info("[FinLoanCallback] 借款已审批通过: loanId={}, amount={}", loanId, loan.getAmount());
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long loanId) {
        FinLoan loan = loadLoan(loanId);
        if (loan == null) return;
        if (FinConstants.LOAN_STATUS_REJECTED.equals(loan.getStatus())) {
            log.info("[FinLoanCallback] 借款已是 REJECTED 终态, 幂等跳过: loanId={}", loanId);
            return;
        }
        if (!FinConstants.LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            log.warn("[FinLoanCallback] 借款单非 PENDING, 跳过: loanId={}, status={}", loanId, loan.getStatus());
            return;
        }
        loan.setStatus(FinConstants.LOAN_STATUS_REJECTED);
        loanMapper.updateById(loan);
        log.info("[FinLoanCallback] 借款已驳回: loanId={}", loanId);
    }

    private FinLoan loadLoan(Long loanId) {
        FinLoan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            log.warn("[FinLoanCallback] 借款单不存在: loanId={}", loanId);
            return null;
        }
        return loan;
    }

    private static Long parseLoanId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
