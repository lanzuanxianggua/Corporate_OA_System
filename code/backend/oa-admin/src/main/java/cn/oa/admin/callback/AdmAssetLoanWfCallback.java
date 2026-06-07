package cn.oa.admin.callback;

import cn.oa.admin.constant.AdmConstants;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.entity.AdmAssetLoan;
import cn.oa.admin.mapper.AdmAssetLoanMapper;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.service.AdmAssetLoanService;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产领用流程完成回调.
 *
 * <p>监听 {@link WfInstanceCompletedEvent}, 解析 businessKey 前缀 {@code ASSET_}.
 * 终态 APPROVED → 状态置 APPROVED + 资产变 IN_USE (BORROW) 或 SCRAPPED (SCRAP);
 * REJECTED → 状态置 REJECTED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdmAssetLoanWfCallback {

    static final String BIZ_PREFIX = AdmConstants.BIZ_KEY_PREFIX_ASSET;

    private final AdmAssetLoanMapper mapper;
    private final AdmAssetMapper assetMapper;

    @EventListener
    public void handleEvent(WfInstanceCompletedEvent event) {
        log.info("[AdmAssetLoanCallback] 收到流程完成事件: instanceId={}, status={}, businessKey={}",
                event.getInstanceId(), event.getStatus(), event.getBusinessKey());
        try {
            String businessKey = event.getBusinessKey();
            if (businessKey == null || !businessKey.startsWith(BIZ_PREFIX)) {
                log.debug("[AdmAssetLoanCallback] 非资产领用流程, 跳过: businessKey={}", businessKey);
                return;
            }
            Long loanId = parseId(businessKey);
            if (loanId == null) {
                log.warn("[AdmAssetLoanCallback] 解析 loanId 失败: businessKey={}", businessKey);
                return;
            }
            String status = event.getStatus();
            if (AdmConstants.ASSET_LOAN_STATUS_APPROVED.equalsIgnoreCase(status)) {
                onApproved(loanId);
            } else if (AdmConstants.ASSET_LOAN_STATUS_REJECTED.equalsIgnoreCase(status)) {
                onRejected(loanId);
            } else {
                log.info("[AdmAssetLoanCallback] 非终态事件, 跳过: status={}", status);
            }
        } catch (BizException ex) {
            log.error("[AdmAssetLoanCallback] 处理流程完成事件业务异常: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[AdmAssetLoanCallback] 处理流程完成事件失败: instanceId={}, err={}",
                    event.getInstanceId(), ex.getMessage(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void onApproved(Long loanId) {
        AdmAssetLoan loan = mapper.selectById(loanId);
        if (loan == null) {
            log.warn("[AdmAssetLoanCallback] 领用单不存在: id={}", loanId);
            return;
        }
        if (AdmConstants.ASSET_LOAN_STATUS_APPROVED.equals(loan.getStatus())
                || AdmConstants.ASSET_LOAN_STATUS_RETURNED.equals(loan.getStatus())) {
            log.info("[AdmAssetLoanCallback] 领用单已是终态, 幂等跳过: id={}, status={}",
                    loanId, loan.getStatus());
            return;
        }
        if (!AdmConstants.ASSET_LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            log.warn("[AdmAssetLoanCallback] 领用单非 PENDING 状态, 跳过: id={}, status={}",
                    loanId, loan.getStatus());
            return;
        }
        loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_APPROVED);
        mapper.updateById(loan);

        // 资产状态联动: BORROW -> IN_USE, SCRAP -> SCRAPPED
        if (loan.getAssetId() != null) {
            AdmAsset asset = assetMapper.selectById(loan.getAssetId());
            if (asset != null) {
                if (AdmAssetLoanService.LOAN_TYPE_BORROW.equals(loan.getLoanType())) {
                    asset.setStatus(AdmConstants.ASSET_STATUS_IN_USE);
                } else if (AdmAssetLoanService.LOAN_TYPE_SCRAP.equals(loan.getLoanType())) {
                    asset.setStatus(AdmConstants.ASSET_STATUS_SCRAPPED);
                }
                assetMapper.updateById(asset);
            }
        }
        log.info("[AdmAssetLoanCallback] 资产领用已审批通过: id={}, type={}", loanId, loan.getLoanType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void onRejected(Long loanId) {
        AdmAssetLoan loan = mapper.selectById(loanId);
        if (loan == null) {
            log.warn("[AdmAssetLoanCallback] 领用单不存在: id={}", loanId);
            return;
        }
        if (AdmConstants.ASSET_LOAN_STATUS_REJECTED.equals(loan.getStatus())) {
            log.info("[AdmAssetLoanCallback] 领用单已是 REJECTED 终态, 幂等跳过: id={}", loanId);
            return;
        }
        if (!AdmConstants.ASSET_LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            log.warn("[AdmAssetLoanCallback] 领用单非 PENDING 状态, 跳过: id={}, status={}",
                    loanId, loan.getStatus());
            return;
        }
        loan.setStatus(AdmConstants.ASSET_LOAN_STATUS_REJECTED);
        mapper.updateById(loan);
        log.info("[AdmAssetLoanCallback] 资产领用已驳回: id={}", loanId);
    }

    private static Long parseId(String businessKey) {
        try {
            return Long.parseLong(businessKey.substring(BIZ_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
