package cn.oa.message.callback;

import cn.oa.message.dto.MsgSendDTO;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.service.MsgNotificationTypeService;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工作流完成事件 → 站内通知 监听器.
 *
 * <p>当 oa-workflow 引擎发布 {@link WfInstanceCompletedEvent} (审批通过/拒绝) 时,
 * 本监听器根据 businessKey 路由到对应通知类型并发送给申请人.
 *
 * <p>使用 {@code @EventListener} 直接注册 (项目惯例, 与 {@code HrLeaveWfCallback} 保持一致).
 * 与发布方同事务同步执行, 依赖 {@code WfEngine} 已在事务内调用 {@code publishEvent}.
 *
 * <p>异常处理: 监听器内捕获所有异常并 log, 不向上抛出 — 避免污染上游事务日志.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfInstanceNotifyListener {

    private final MsgNotificationService notificationService;
    private final MsgNotificationTypeService typeService;

    /**
     * 业务前缀 → 通知类型 code 映射.
     */
    private static final String BIZ_LEAVE = "LEAVE_";
    private static final String BIZ_EXPENSE = "EXPENSE_";

    private static final String TYPE_LEAVE_APPROVE = "LEAVE_APPROVE";
    private static final String TYPE_LEAVE_REJECT = "LEAVE_REJECT";
    private static final String TYPE_EXPENSE_APPROVE = "EXPENSE_APPROVE";
    private static final String TYPE_EXPENSE_REJECT = "EXPENSE_REJECT";
    private static final String TYPE_GENERAL = "GENERAL";

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    /**
     * 处理工作流完成事件.
     *
     * <p>目前仅根据 businessKey 前缀路由, 不从事件中读 applicantId (保持事件向后兼容).
     * 申请人 ID 通过 {@code WfInstance.initiatorId} 解析 (由业务侧在调用 WfEngine 时传入, 此处
     * 简化处理: 不传 recipient, 走 GENERAL 类型广播占位 — 实际场景由业务方在自己 callback 内
     * 显式调用 MsgSendDTO).
     *
     * <p>NOTE: 当前阶段 V2 设计未在事件内扩展 recipientId, 监听器主要做"类型映射 + 标记流程节点
     * 触达" 的副作用; 真正的申请人定向通知由 {@code HrLeaveWfCallback} 等业务 callback 在
     * 自己处理业务单据时调 {@code MsgNotificationService.send()} 完成.
     */
    @EventListener
    public void onWfInstanceCompleted(WfInstanceCompletedEvent event) {
        try {
            String businessKey = event.getBusinessKey();
            String status = event.getStatus();

            if (businessKey == null || businessKey.isBlank()) {
                log.debug("事件 businessKey 为空, 跳过通知派发: instanceId={}", event.getInstanceId());
                return;
            }

            String typeCode = resolveTypeCode(businessKey, status);
            if (typeCode == null) {
                log.debug("未匹配到通知类型, 跳过: businessKey={}, status={}", businessKey, status);
                return;
            }

            // 校验类型存在且启用 — 类型禁用时不发送 (按业务策略: 静默跳过)
            try {
                typeService.requireEnabled(typeCode);
            } catch (BizException ex) {
                log.info("通知类型未启用, 跳过: type={}, reason={}", typeCode, ex.getMessage());
                return;
            }

            // 占位: 仅做类型校验 + 流程节点标记, 不实际向申请人发定向消息
            // (避免在 WfInstanceCompletedEvent 尚未扩展 recipientId 字段时发送空指针通知)
            log.info("[WfNotify] 流程完成事件路由: instanceId={}, businessKey={}, status={}, notifyType={}",
                    event.getInstanceId(), businessKey, status, typeCode);

        } catch (Exception ex) {
            // 监听器异常必须吞掉 — 防止污染上游事务日志
            log.error("[WfNotify] 处理 WfInstanceCompletedEvent 失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    /**
     * 暴露给业务 callback (如 HrLeaveWfCallback) 直接调用的便捷方法.
     *
     * <p>业务方在更新单据状态后, 调用本方法发送定向通知, 绕过事件路由层.
     */
    public void sendForBusiness(Long recipientId, String businessKeyPrefix, String status) {
        if (recipientId == null) {
            log.warn("[WfNotify] recipientId 为空, 跳过通知: biz={}, status={}", businessKeyPrefix, status);
            return;
        }
        String typeCode = resolveTypeCode(businessKeyPrefix, status);
        if (typeCode == null) {
            log.debug("[WfNotify] 未匹配到通知类型: biz={}, status={}", businessKeyPrefix, status);
            return;
        }
        try {
            typeService.requireEnabled(typeCode);
        } catch (BizException ex) {
            log.info("[WfNotify] 通知类型未启用, 跳过: type={}", typeCode);
            return;
        }

        MsgSendDTO dto = new MsgSendDTO();
        dto.setTitle(buildTitle(typeCode));
        dto.setContent(buildContent(typeCode, businessKeyPrefix));
        dto.setType(typeCode);
        dto.setRecipientIds(List.of(recipientId));

        try {
            notificationService.send(dto, 0L);  // senderId=0 表示系统
            log.info("[WfNotify] 业务通知已发送: recipient={}, type={}", recipientId, typeCode);
        } catch (BizException ex) {
            log.error("[WfNotify] 业务通知发送失败: recipient={}, type={}, err={}",
                    recipientId, typeCode, ex.getMessage());
        }
    }

    /**
     * businessKey + status → 通知类型 code.
     */
    private String resolveTypeCode(String businessKey, String status) {
        boolean approved = STATUS_APPROVED.equals(status);
        boolean rejected = STATUS_REJECTED.equals(status);
        if (!approved && !rejected) {
            return null;
        }
        if (businessKey.startsWith(BIZ_LEAVE)) {
            return approved ? TYPE_LEAVE_APPROVE : TYPE_LEAVE_REJECT;
        }
        if (businessKey.startsWith(BIZ_EXPENSE)) {
            return approved ? TYPE_EXPENSE_APPROVE : TYPE_EXPENSE_REJECT;
        }
        return TYPE_GENERAL;
    }

    private String buildTitle(String typeCode) {
        return switch (typeCode) {
            case TYPE_LEAVE_APPROVE -> "您的请假申请已通过";
            case TYPE_LEAVE_REJECT -> "您的请假申请被拒绝";
            case TYPE_EXPENSE_APPROVE -> "您的报销申请已通过";
            case TYPE_EXPENSE_REJECT -> "您的报销申请被拒绝";
            default -> "流程通知";
        };
    }

    private String buildContent(String typeCode, String businessKey) {
        return "业务单号: " + businessKey + " (类型: " + typeCode + ", 错误码: "
                + RCode.SUCCESS.getCode() + ")";
    }
}
