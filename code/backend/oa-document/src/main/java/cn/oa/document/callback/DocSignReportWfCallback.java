package cn.oa.document.callback;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.entity.DocSignReport;
import cn.oa.document.mapper.DocSignReportMapper;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 签报流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey 前缀 {@code SIGN_REPORT_}.
 * 模式同 {@link DocDispatchWfCallback}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocSignReportWfCallback {

    static final String BIZ_PREFIX = DocConstants.BIZ_KEY_PREFIX_SIGN_REPORT;

    private final DocSignReportMapper reportMapper;

    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[DocSignReportCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[DocSignReportCallback] 非签报流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long reportId = parseId(businessKey);
            if (reportId == null) {
                log.warn("[DocSignReportCallback] 解析 reportId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (DocConstants.SIGN_REPORT_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(reportId);
            } else if (DocConstants.SIGN_REPORT_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(reportId);
            } else {
                log.info("[DocSignReportCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            log.error("[DocSignReportCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[DocSignReportCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long reportId) {
        DocSignReport report = loadReport(reportId);
        if (report == null) return;
        if (DocConstants.SIGN_REPORT_STATUS_APPROVED.equals(report.getStatus())) {
            log.info("[DocSignReportCallback] 签报已是 APPROVED 终态, 幂等跳过: reportId={}", reportId);
            return;
        }
        if (!DocConstants.SIGN_REPORT_STATUS_PENDING.equals(report.getStatus())) {
            log.warn("[DocSignReportCallback] 签报非 PENDING 状态, 跳过: reportId={}, status={}",
                    reportId, report.getStatus());
            return;
        }
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_APPROVED);
        reportMapper.updateById(report);
        log.info("[DocSignReportCallback] 签报已审批通过: reportId={}", reportId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long reportId) {
        DocSignReport report = loadReport(reportId);
        if (report == null) return;
        if (DocConstants.SIGN_REPORT_STATUS_REJECTED.equals(report.getStatus())) {
            log.info("[DocSignReportCallback] 签报已是 REJECTED 终态, 幂等跳过: reportId={}", reportId);
            return;
        }
        if (!DocConstants.SIGN_REPORT_STATUS_PENDING.equals(report.getStatus())) {
            log.warn("[DocSignReportCallback] 签报非 PENDING 状态, 跳过: reportId={}, status={}",
                    reportId, report.getStatus());
            return;
        }
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_REJECTED);
        reportMapper.updateById(report);
        log.info("[DocSignReportCallback] 签报已驳回: reportId={}", reportId);
    }

    private DocSignReport loadReport(Long id) {
        DocSignReport r = reportMapper.selectById(id);
        if (r == null) {
            log.warn("[DocSignReportCallback] 签报不存在: id={}", id);
            return null;
        }
        return r;
    }

    private static Long parseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
