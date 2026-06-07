package cn.oa.finance.callback;

import cn.oa.finance.entity.FinExpense;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinExpenseMapper;
import cn.oa.finance.service.FinBudgetService;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import cn.oa.workflow.mapper.WfInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报销流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent} (oa-workflow 引擎在终态时发布),
 * 解析 businessKey (前缀 {@code EXP_}) 得到 expenseId, 依据终态:
 * <ul>
 *   <li>APPROVED — 状态置 APPROVED, 调用 {@link FinBudgetService#deductOnApprove}
 *       把冻结金额转为已使用</li>
 *   <li>REJECTED — 状态置 REJECTED, 调用 {@link FinBudgetService#unfreezeOnReject}
 *       解冻预算</li>
 * </ul>
 *
 * <p>通知派发由 oa-message 的 {@code WfInstanceNotifyListener} 统一处理 (在事件路由层按
 * businessKey 前缀 + 状态选择 {@code TYPE_EXPENSE_APPROVE/REJECT} 模板).
 *
 * <p>事务边界: {@link #onApproved} / {@link #onRejected} 各自独立事务, 避免业务异常
 * 污染上游 oa-workflow 事务.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinExpenseWfCallback {

    /** 业务前缀: oa-finance Service 约定 "EXP_" + expenseId. */
    static final String BIZ_PREFIX = "EXP_";

    private final WfInstanceMapper wfInstanceMapper;
    private final FinExpenseMapper expenseMapper;
    private final FinBudgetService budgetService;

    /**
     * 监听流程完成事件.
     */
    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[FinExpenseCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[FinExpenseCallback] 非报销业务流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long expenseId = parseExpenseId(businessKey);
            if (expenseId == null) {
                log.warn("[FinExpenseCallback] 解析 expenseId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (FinConstants.EXPENSE_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(expenseId, event.getInstanceId());
            } else if (FinConstants.EXPENSE_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(expenseId, event.getInstanceId());
            } else {
                log.info("[FinExpenseCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            // 业务异常: 记录 + 抛回上层, 由 GlobalExceptionHandler 统一处理
            log.error("[FinExpenseCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[FinExpenseCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    /**
     * 审批通过: 冻结 → 已使用.
     */
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long expenseId, Long wfInstanceId) {
        FinExpense expense = loadExpense(expenseId);
        if (expense == null) return;
        if (FinConstants.EXPENSE_STATUS_APPROVED.equals(expense.getStatus())
                || FinConstants.EXPENSE_STATUS_PAID.equals(expense.getStatus())) {
            log.info("[FinExpenseCallback] 报销单已是 APPROVED/PAID 终态, 幂等跳过: expenseId={}", expenseId);
            return;
        }
        if (!FinConstants.EXPENSE_STATUS_PENDING.equals(expense.getStatus())) {
            log.warn("[FinExpenseCallback] 报销单非 PENDING 状态, 跳过: expenseId={}, status={}",
                    expenseId, expense.getStatus());
            return;
        }

        // 预算扣减: 冻结 → 已使用
        int year = LocalDate.now().getYear();
        BigDecimal amount = expense.getTotalAmount() == null ? BigDecimal.ZERO : expense.getTotalAmount();
        budgetService.deductOnApprove(expense.getDeptId(), year, amount);

        expense.setStatus(FinConstants.EXPENSE_STATUS_APPROVED);
        expense.setPaidTime(java.time.LocalDateTime.now());
        expenseMapper.updateById(expense);
        log.info("[FinExpenseCallback] 报销已审批通过: expenseId={}, amount={}", expenseId, amount);
    }

    /**
     * 审批拒绝: 解冻预算.
     */
    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long expenseId, Long wfInstanceId) {
        FinExpense expense = loadExpense(expenseId);
        if (expense == null) return;
        if (FinConstants.EXPENSE_STATUS_REJECTED.equals(expense.getStatus())) {
            log.info("[FinExpenseCallback] 报销单已是 REJECTED 终态, 幂等跳过: expenseId={}", expenseId);
            return;
        }
        if (!FinConstants.EXPENSE_STATUS_PENDING.equals(expense.getStatus())) {
            log.warn("[FinExpenseCallback] 报销单非 PENDING 状态, 跳过: expenseId={}, status={}",
                    expenseId, expense.getStatus());
            return;
        }

        int year = LocalDate.now().getYear();
        BigDecimal amount = expense.getTotalAmount() == null ? BigDecimal.ZERO : expense.getTotalAmount();
        budgetService.unfreezeOnReject(expense.getDeptId(), year, amount);

        expense.setStatus(FinConstants.EXPENSE_STATUS_REJECTED);
        expenseMapper.updateById(expense);
        log.info("[FinExpenseCallback] 报销已驳回: expenseId={}", expenseId);
    }

    /**
     * 加载报销单.
     */
    private FinExpense loadExpense(Long expenseId) {
        FinExpense expense = expenseMapper.selectById(expenseId);
        if (expense == null) {
            log.warn("[FinExpenseCallback] 报销单不存在: expenseId={}", expenseId);
            return null;
        }
        return expense;
    }

    /**
     * 可选校验: 通过 WfInstance.businessKey 与 expenseId 反查, 防止上游事件错配.
     * 不强制使用, 仅做 debug 日志.
     */
    @SuppressWarnings("unused")
    private void verifyBusinessKey(Long wfInstanceId, Long expenseId) {
        if (wfInstanceId == null) return;
        WfInstance instance = wfInstanceMapper.selectById(wfInstanceId);
        if (instance == null) return;
        if (instance.getBusinessKey() == null
                || !instance.getBusinessKey().equals(BIZ_PREFIX + expenseId)) {
            log.warn("[FinExpenseCallback] businessKey 不匹配: expected=EXP_{}, actual={}",
                    expenseId, instance.getBusinessKey());
        }
    }

    private static Long parseExpenseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
