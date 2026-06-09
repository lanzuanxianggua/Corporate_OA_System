package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.WfDelegation;
import cn.oa.mapper.WfDelegationMapper;
import cn.oa.service.DelegationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DelegationServiceImpl extends ServiceImpl<WfDelegationMapper, WfDelegation> implements DelegationService {

    @Override
    public void setDelegation(WfDelegation delegation) {
        // 验证不能委托给自己
        if (delegation.getDelegatorId() != null && delegation.getDelegatorId().equals(delegation.getDelegateToId())) {
            throw new BusinessException("不能将审批委托给自己");
        }
        // 验证委托时间
        if (delegation.getStartDate() == null || delegation.getEndDate() == null) {
            throw new BusinessException("委托开始时间和结束时间不能为空");
        }
        if (delegation.getStartDate().isAfter(delegation.getEndDate())) {
            throw new BusinessException("委托开始时间不能晚于结束时间");
        }
        delegation.setStatus("ACTIVE");
        delegation.setCreateTime(LocalDateTime.now());
        this.save(delegation);
        log.info("Delegation set: delegatorId={}, delegateToId={}", delegation.getDelegatorId(), delegation.getDelegateToId());
    }

    @Override
    public List<WfDelegation> getMyDelegations(Long delegatorId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegatorId, delegatorId)
                .orderByDesc(WfDelegation::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public void cancelDelegation(Long id, Long delegatorId) {
        WfDelegation delegation = this.getById(id);
        if (delegation != null && delegation.getDelegatorId().equals(delegatorId)) {
            delegation.setStatus("CANCELLED");
            this.updateById(delegation);
            log.info("Delegation cancelled: id={}, delegatorId={}", id, delegatorId);
        }
    }

    @Override
    public Long resolveDelegate(Long delegatorId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegatorId, delegatorId)
                .eq(WfDelegation::getStatus, "ACTIVE")
                .le(WfDelegation::getStartDate, LocalDate.now())
                .ge(WfDelegation::getEndDate, LocalDate.now())
                .last("LIMIT 1");
        WfDelegation delegation = this.getOne(wrapper);
        return delegation != null ? delegation.getDelegateToId() : null;
    }

    @Override
    public WfDelegation findActiveDelegationForDelegate(Long delegateToId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegateToId, delegateToId)
                .eq(WfDelegation::getStatus, "ACTIVE")
                .le(WfDelegation::getStartDate, LocalDate.now())
                .ge(WfDelegation::getEndDate, LocalDate.now())
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /**
     * Find an active delegation from a delegator to a specific delegate.
     * Used for authorization checks in approval flows.
     */
    public WfDelegation findActiveDelegation(Long delegatorId, Long delegateToId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegatorId, delegatorId)
                .eq(WfDelegation::getDelegateToId, delegateToId)
                .eq(WfDelegation::getStatus, "ACTIVE")
                .le(WfDelegation::getStartDate, LocalDate.now())
                .ge(WfDelegation::getEndDate, LocalDate.now())
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
