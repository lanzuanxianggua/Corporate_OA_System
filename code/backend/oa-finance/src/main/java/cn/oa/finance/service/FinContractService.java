package cn.oa.finance.service;

import cn.oa.finance.entity.FinContract;
import cn.oa.finance.mapper.FinContractMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinContractService {
    private final FinContractMapper mapper;

    @Transactional
    public Long create(FinContract contract) {
        if (contract.getStatus() == null) contract.setStatus("DRAFT");
        mapper.insert(contract);
        return contract.getId();
    }

    @Transactional public void update(FinContract contract) { mapper.updateById(contract); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }
    public FinContract getById(Long id) { return mapper.selectById(id); }

    @Transactional
    public void activate(Long id) {
        FinContract contract = mapper.selectById(id);
        contract.setStatus("ACTIVE");
        mapper.updateById(contract);
    }

    @Transactional
    public void close(Long id) {
        FinContract contract = mapper.selectById(id);
        contract.setStatus("CLOSED");
        mapper.updateById(contract);
    }

    /**
     * V1010: Workflow callback hook. Maps the workflow engine's integer status
     * (1=approved, 2=rejected, 3=withdrawn) to the contract lifecycle.
     * <ul>
     *   <li>1 → ACTIVE (approved and effective)</li>
     *   <li>2 → CLOSED (rejected — treat as closed because the contract is no longer live)</li>
     *   <li>3 → DRAFT (withdrawn — revert to draft so initiator can edit and resubmit)</li>
     * </ul>
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        FinContract contract = mapper.selectById(id);
        if (contract == null) return;
        if (status == null) return;
        switch (status) {
            case 1: contract.setStatus("ACTIVE"); break;
            case 2: contract.setStatus("CLOSED"); break;
            case 3: contract.setStatus("DRAFT");  break;
            default: return;
        }
        mapper.updateById(contract);
    }

    public Page<FinContract> listPage(Long deptId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<FinContract>()
                .eq(deptId != null, FinContract::getDeptId, deptId)
                .eq(status != null && !status.isBlank(), FinContract::getStatus, status)
                .orderByDesc(FinContract::getCreateTime));
    }
}
