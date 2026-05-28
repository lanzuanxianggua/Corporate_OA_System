package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.entity.WfDelegation;
import cn.oa.mapper.WfDelegationMapper;
import cn.oa.service.DelegationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DelegationServiceImpl extends ServiceImpl<WfDelegationMapper, WfDelegation> implements DelegationService {

    @Override
    public void setDelegation(WfDelegation delegation) {
        delegation.setStatus("0");
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
            delegation.setStatus("1");
            this.updateById(delegation);
            log.info("Delegation cancelled: id={}, delegatorId={}", id, delegatorId);
        }
    }

    @Override
    public Long resolveDelegate(Long delegatorId) {
        LambdaQueryWrapper<WfDelegation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfDelegation::getDelegatorId, delegatorId)
                .eq(WfDelegation::getStatus, "0")
                .le(WfDelegation::getStartTime, LocalDateTime.now())
                .ge(WfDelegation::getEndTime, LocalDateTime.now())
                .last("LIMIT 1");
        WfDelegation delegation = this.getOne(wrapper);
        return delegation != null ? delegation.getDelegateToId() : null;
    }
}
