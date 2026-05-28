package cn.oa.service;

import cn.oa.entity.WfDelegation;
import java.util.List;

public interface DelegationService {
    void setDelegation(WfDelegation delegation);
    List<WfDelegation> getMyDelegations(Long delegatorId);
    void cancelDelegation(Long id, Long delegatorId);
    Long resolveDelegate(Long delegatorId);

    /**
     * Find an active delegation where the given empId is the delegate (delegateToId).
     * Used for reverse delegation lookup in task finding.
     */
    WfDelegation findActiveDelegationForDelegate(Long delegateToId);
}
