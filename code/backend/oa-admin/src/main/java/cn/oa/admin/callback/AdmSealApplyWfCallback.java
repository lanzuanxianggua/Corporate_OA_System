package cn.oa.admin.callback;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.entity.AdmSealApply;
import cn.oa.admin.mapper.AdmSealApplyMapper;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 印章申请流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey 前缀 {@code SEAL_}.
 * 终态 APPROVED → 状态置 APPROVED; REJECTED → 状态置 REJECTED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdmSealApplyWfCallback {

    static final String BIZ_PREFIX = AdmConstants.BIZ_KEY_PREFIX_SEAL;

    private final AdmSealApplyMapper mapper;

    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[AdmSealApplyCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[AdmSealApplyCallback] 非印章申请流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long applyId = parseId(businessKey);
            if (applyId == null) {
                log.warn("[AdmSealApplyCallback] 解析 applyId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (AdmConstants.SEAL_APPLY_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(applyId);
            } else if (AdmConstants.SEAL_APPLY_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(applyId);
            } else {
                log.info("[AdmSealApplyCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            log.error("[AdmSealApplyCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[AdmSealApplyCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long applyId) {
        AdmSealApply apply = mapper.selectById(applyId);
        if (apply == null) {
            log.warn("[AdmSealApplyCallback] 印章申请不存在: id={}", applyId);
            return;
        }
        if (AdmConstants.SEAL_APPLY_STATUS_APPROVED.equals(apply.getStatus())
                || AdmConstants.SEAL_APPLY_STATUS_USED.equals(apply.getStatus())
                || AdmConstants.SEAL_APPLY_STATUS_ARCHIVED.equals(apply.getStatus())) {
            log.info("[AdmSealApplyCallback] 印章申请已是终态, 幂等跳过: id={}, status={}",
                    applyId, apply.getStatus());
            return;
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_PENDING.equals(apply.getStatus())) {
            log.warn("[AdmSealApplyCallback] 印章申请非 PENDING 状态, 跳过: id={}, status={}",
                    applyId, apply.getStatus());
            return;
        }
        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_APPROVED);
        mapper.updateById(apply);
        log.info("[AdmSealApplyCallback] 印章申请已审批通过: id={}", applyId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long applyId) {
        AdmSealApply apply = mapper.selectById(applyId);
        if (apply == null) {
            log.warn("[AdmSealApplyCallback] 印章申请不存在: id={}", applyId);
            return;
        }
        if (AdmConstants.SEAL_APPLY_STATUS_REJECTED.equals(apply.getStatus())) {
            log.info("[AdmSealApplyCallback] 印章申请已是 REJECTED 终态, 幂等跳过: id={}", applyId);
            return;
        }
        if (!AdmConstants.SEAL_APPLY_STATUS_PENDING.equals(apply.getStatus())) {
            log.warn("[AdmSealApplyCallback] 印章申请非 PENDING 状态, 跳过: id={}, status={}",
                    applyId, apply.getStatus());
            return;
        }
        apply.setStatus(AdmConstants.SEAL_APPLY_STATUS_REJECTED);
        mapper.updateById(apply);
        log.info("[AdmSealApplyCallback] 印章申请已驳回: id={}", applyId);
    }

    private static Long parseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
