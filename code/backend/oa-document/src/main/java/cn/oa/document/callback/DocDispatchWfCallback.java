package cn.oa.document.callback;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.entity.DocDispatch;
import cn.oa.document.mapper.DocDispatchMapper;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 发文流程完成回调.
 *
 * <p>oa-document 借鉴 oa-finance 模式 (FinExpenseWfCallback):
 * <ul>
 *   <li>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey 前缀 {@code DISPATCH_}</li>
 *   <li>终态 APPROVED: 状态置 APPROVED (后续可触发 publish/archive)</li>
 *   <li>终态 REJECTED: 状态置 REJECTED</li>
 *   <li>独立事务, 异常吞掉 (不污染上游 oa-workflow 事务)</li>
 *   <li>幂等检查</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocDispatchWfCallback {

    static final String BIZ_PREFIX = DocConstants.BIZ_KEY_PREFIX_DISPATCH;

    private final DocDispatchMapper dispatchMapper;

    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[DocDispatchCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[DocDispatchCallback] 非发文流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long dispatchId = parseId(businessKey);
            if (dispatchId == null) {
                log.warn("[DocDispatchCallback] 解析 dispatchId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (DocConstants.DISPATCH_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(dispatchId);
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                onRejected(dispatchId);
            } else {
                log.info("[DocDispatchCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            log.error("[DocDispatchCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[DocDispatchCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long dispatchId) {
        DocDispatch dispatch = loadDispatch(dispatchId);
        if (dispatch == null) return;
        if (DocConstants.DISPATCH_STATUS_APPROVED.equals(dispatch.getStatus())
                || DocConstants.DISPATCH_STATUS_PUBLISHED.equals(dispatch.getStatus())
                || DocConstants.DISPATCH_STATUS_ARCHIVED.equals(dispatch.getStatus())) {
            log.info("[DocDispatchCallback] 发文已是终态, 幂等跳过: dispatchId={}", dispatchId);
            return;
        }
        if (!DocConstants.DISPATCH_STATUS_PENDING.equals(dispatch.getStatus())) {
            log.warn("[DocDispatchCallback] 发文非 PENDING 状态, 跳过: dispatchId={}, status={}",
                    dispatchId, dispatch.getStatus());
            return;
        }
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_APPROVED);
        dispatchMapper.updateById(dispatch);
        log.info("[DocDispatchCallback] 发文已审批通过: dispatchId={}", dispatchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long dispatchId) {
        DocDispatch dispatch = loadDispatch(dispatchId);
        if (dispatch == null) return;
        if ("REJECTED".equals(dispatch.getStatus())) {
            log.info("[DocDispatchCallback] 发文已是 REJECTED 终态, 幂等跳过: dispatchId={}", dispatchId);
            return;
        }
        if (!DocConstants.DISPATCH_STATUS_PENDING.equals(dispatch.getStatus())) {
            log.warn("[DocDispatchCallback] 发文非 PENDING 状态, 跳过: dispatchId={}, status={}",
                    dispatchId, dispatch.getStatus());
            return;
        }
        dispatch.setStatus("REJECTED");
        dispatchMapper.updateById(dispatch);
        log.info("[DocDispatchCallback] 发文已驳回: dispatchId={}", dispatchId);
    }

    private DocDispatch loadDispatch(Long id) {
        DocDispatch d = dispatchMapper.selectById(id);
        if (d == null) {
            log.warn("[DocDispatchCallback] 发文不存在: id={}", id);
            return null;
        }
        return d;
    }

    private static Long parseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
