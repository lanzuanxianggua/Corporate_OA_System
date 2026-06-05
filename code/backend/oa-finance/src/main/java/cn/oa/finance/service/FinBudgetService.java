package cn.oa.finance.service;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinBudgetMapper;
import cn.oa.finance.vo.FinBudgetVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 预算 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinBudgetService {

    private final FinBudgetMapper mapper;

    /**
     * 创建预算.
     *
     * @param dto   创建参数
     * @param empId 预算责任人
     * @return 预算 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(FinBudgetCreateDTO dto, Long empId) {
        FinBudget budget = new FinBudget();
        budget.setBudgetName(dto.getBudgetName());
        budget.setBudgetYear(dto.getBudgetYear());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setFrozenAmount(BigDecimal.ZERO);
        budget.setEmpId(empId);
        budget.setStatus(FinConstants.BUDGET_STATUS_ACTIVE);

        mapper.insert(budget);
        log.info("预算已创建: budgetId={}, year={}, name={}, amount={}",
                budget.getId(), dto.getBudgetYear(), dto.getBudgetName(), dto.getTotalAmount());
        return budget.getId();
    }

    /**
     * 更新预算.
     * 仅 ACTIVE 状态可更新.
     *
     * @param id  预算 ID
     * @param dto 更新参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinBudgetCreateDTO dto) {
        FinBudget budget = checkBudgetExists(id);
        if (!FinConstants.BUDGET_STATUS_ACTIVE.equals(budget.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅 ACTIVE 状态的预算可更新, 当前状态: " + budget.getStatus());
        }

        budget.setBudgetName(dto.getBudgetName());
        budget.setBudgetYear(dto.getBudgetYear());
        budget.setTotalAmount(dto.getTotalAmount());
        mapper.updateById(budget);
        log.info("预算已更新: budgetId={}", id);
    }

    /**
     * 软删除预算.
     *
     * @param id 预算 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        checkBudgetExists(id);
        mapper.deleteById(id);
        log.info("预算已删除: budgetId={}", id);
    }

    /**
     * 按 ID 查询预算详情.
     *
     * @param id 预算 ID
     * @return FinBudget
     */
    public FinBudget getById(Long id) {
        return checkBudgetExists(id);
    }

    /**
     * 分页查询预算列表, 按 deptId 过滤.
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param deptId  部门 ID (null 表示不按部门过滤)
     * @return 分页结果
     */
    public PageResult<FinBudgetVO> listPage(Integer pageNum, Integer pageSize, Long deptId) {
        Page<FinBudget> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<FinBudget> wrapper = new LambdaQueryWrapper<FinBudget>()
                .eq(deptId != null, FinBudget::getDeptId, deptId)
                .orderByDesc(FinBudget::getBudgetYear)
                .orderByDesc(FinBudget::getCreateTime);

        Page<FinBudget> result = mapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), pageNum, pageSize);
    }

    /**
     * 检查预算是否存在.
     */
    private FinBudget checkBudgetExists(Long id) {
        FinBudget budget = mapper.selectById(id);
        if (budget == null) {
            throw new BizException(RCode.NOT_FOUND, "预算不存在: " + id);
        }
        return budget;
    }

    /**
     * Entity -> VO 转换.
     */
    private FinBudgetVO toVO(FinBudget budget) {
        FinBudgetVO vo = new FinBudgetVO();
        vo.setId(budget.getId());
        vo.setEmpId(budget.getEmpId());
        vo.setDeptId(budget.getDeptId());
        vo.setBudgetName(budget.getBudgetName());
        vo.setBudgetYear(budget.getBudgetYear());
        vo.setTotalAmount(budget.getTotalAmount());
        vo.setUsedAmount(budget.getUsedAmount());
        vo.setFrozenAmount(budget.getFrozenAmount());
        vo.setStatus(budget.getStatus());
        vo.setCreateTime(budget.getCreateTime());
        return vo;
    }
}
