package cn.oa.workflow.service;

import cn.oa.workflow.entity.WfDelegation;
import cn.oa.workflow.mapper.WfDelegationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 委托服务.
 */
@Service
public class WfDelegationService {

    private final WfDelegationMapper mapper;

    public WfDelegationService(WfDelegationMapper mapper) {
        this.mapper = mapper;
    }

    public Long create(WfDelegation entity) {
        if (entity.getStatus() == null) entity.setStatus("ACTIVE");
        entity.setCreateTime(LocalDateTime.now());
        mapper.insert(entity);
        return entity.getId();
    }

    public void revoke(Long id) {
        WfDelegation entity = new WfDelegation();
        entity.setId(id);
        entity.setStatus("REVOKED");
        mapper.updateById(entity);
    }

    public List<WfDelegation> myOutgoing(Long empId) {
        return mapper.findActiveByFromEmp(empId, LocalDateTime.now());
    }

    public List<WfDelegation> myIncoming(Long empId) {
        return mapper.findActiveByToEmp(empId, LocalDateTime.now());
    }
}
