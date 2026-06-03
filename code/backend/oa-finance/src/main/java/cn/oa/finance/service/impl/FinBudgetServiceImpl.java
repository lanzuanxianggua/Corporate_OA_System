package cn.oa.finance.service.impl;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.dto.FinBudgetQueryDTO;
import cn.oa.finance.dto.FinBudgetUpdateDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.mapper.FinBudgetMapper;
import cn.oa.finance.service.FinBudgetService;
import cn.oa.finance.vo.FinBudgetVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 预算服务实现
 *
 * @author oa-finance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinBudgetServiceImpl implements FinBudgetService {

    private final FinBudgetMapper budgetMapper;

    @Override
    @Transactional
    public Long createBudget(FinBudgetCreateDTO dto) {
        FinBudget budget = new FinBudget();
        budget.setDeptId(dto.getDeptId());
        budget.setProjectId(dto.getProjectId());
        budget.setExpenseCategory(dto.getExpenseCategory());
        budget.setYear(dto.getYear());
        budget.setMonth(dto.getMonth());
        budget.setAmount(dto.getAmount());
        budget.setOccupiedAmount(BigDecimal.ZERO);
        budget.setExecutedAmount(BigDecimal.ZERO);
        budget.setControlStrategy(dto.getControlStrategy());
        budget.setStatus("0"); // 启用
        budget.setVersion(0);

        budgetMapper.insert(budget);
        log.info("Budget created: id={}, deptId={}, amount={}", budget.getId(), dto.getDeptId(), dto.getAmount());
        return budget.getId();
    }

    @Override
    @Transactional
    public void updateBudget(FinBudgetUpdateDTO dto) {
        FinBudget budget = budgetMapper.selectById(dto.getId());
        if (budget == null) {
            throw new BusinessException("预算记录不存在");
        }

        if (dto.getDeptId() != null) budget.setDeptId(dto.getDeptId());
        if (dto.getProjectId() != null) budget.setProjectId(dto.getProjectId());
        if (dto.getExpenseCategory() != null) budget.setExpenseCategory(dto.getExpenseCategory());
        if (dto.getYear() != null) budget.setYear(dto.getYear());
        if (dto.getMonth() != null) budget.setMonth(dto.getMonth());
        if (dto.getAmount() != null) budget.setAmount(dto.getAmount());
        if (dto.getControlStrategy() != null) budget.setControlStrategy(dto.getControlStrategy());
        if (dto.getStatus() != null) budget.setStatus(dto.getStatus());

        budgetMapper.updateById(budget);
        log.info("Budget updated: id={}", dto.getId());
    }

    @Override
    public IPage<FinBudgetVO> pageQuery(FinBudgetQueryDTO query) {
        LambdaQueryWrapper<FinBudget> wrapper = new LambdaQueryWrapper<>();

        if (query.getDeptId() != null) {
            wrapper.eq(FinBudget::getDeptId, query.getDeptId());
        }
        if (query.getProjectId() != null) {
            wrapper.eq(FinBudget::getProjectId, query.getProjectId());
        }
        if (query.getExpenseCategory() != null && !query.getExpenseCategory().isEmpty()) {
            wrapper.eq(FinBudget::getExpenseCategory, query.getExpenseCategory());
        }
        if (query.getYear() != null) {
            wrapper.eq(FinBudget::getYear, query.getYear());
        }
        if (query.getMonth() != null) {
            wrapper.eq(FinBudget::getMonth, query.getMonth());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(FinBudget::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(FinBudget::getCreateTime);

        IPage<FinBudget> page = budgetMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public FinBudgetVO getDetail(Long id) {
        FinBudget budget = budgetMapper.selectById(id);
        if (budget == null) {
            return null;
        }
        return toVO(budget);
    }

    @Override
    @Transactional
    public void deleteBudget(Long id) {
        budgetMapper.deleteById(id);
        log.info("Budget deleted: id={}", id);
    }

    @Override
    @Transactional
    public boolean occupyBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount) {
        // 预算占用 - 乐观锁CAS重试
        for (int i = 0; i < 3; i++) {
            FinBudget budget = budgetMapper.selectOne(new LambdaQueryWrapper<FinBudget>()
                    .eq(FinBudget::getDeptId, deptId)
                    .eq(FinBudget::getExpenseCategory, category)
                    .eq(FinBudget::getYear, year)
                    .eq(month != null ? FinBudget::getMonth : null, month));
            if (budget == null) {
                throw new BusinessException("预算不存在");
            }

            BigDecimal available = budget.getAmount()
                    .subtract(budget.getOccupiedAmount())
                    .subtract(budget.getExecutedAmount());
            if (available.compareTo(amount) < 0) {
                throw new BusinessException("预算不足: 可用" + available + ", 需要" + amount);
            }

            budget.setOccupiedAmount(budget.getOccupiedAmount().add(amount));
            int updated = budgetMapper.update(budget, new LambdaQueryWrapper<FinBudget>()
                    .eq(FinBudget::getId, budget.getId())
                    .eq(FinBudget::getVersion, budget.getVersion()));
            if (updated > 0) {
                log.info("Budget occupied: budgetId={}, amount={}", budget.getId(), amount);
                return true;
            }
        }
        throw new BusinessException("预算操作并发冲突，请重试");
    }

    @Override
    @Transactional
    public boolean releaseBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount) {
        FinBudget budget = budgetMapper.selectOne(new LambdaQueryWrapper<FinBudget>()
                .eq(FinBudget::getDeptId, deptId)
                .eq(FinBudget::getExpenseCategory, category)
                .eq(FinBudget::getYear, year)
                .eq(month != null ? FinBudget::getMonth : null, month));
        if (budget == null) {
            return false;
        }

        budget.setOccupiedAmount(budget.getOccupiedAmount().subtract(amount));
        budgetMapper.updateById(budget);
        log.info("Budget released: budgetId={}, amount={}", budget.getId(), amount);
        return true;
    }

    @Override
    @Transactional
    public boolean executeBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount) {
        FinBudget budget = budgetMapper.selectOne(new LambdaQueryWrapper<FinBudget>()
                .eq(FinBudget::getDeptId, deptId)
                .eq(FinBudget::getExpenseCategory, category)
                .eq(FinBudget::getYear, year)
                .eq(month != null ? FinBudget::getMonth : null, month));
        if (budget == null) {
            return false;
        }

        budget.setOccupiedAmount(budget.getOccupiedAmount().subtract(amount));
        budget.setExecutedAmount(budget.getExecutedAmount().add(amount));
        budgetMapper.updateById(budget);
        log.info("Budget executed: budgetId={}, amount={}", budget.getId(), amount);
        return true;
    }

    // ==================== 私有方法 ====================

    private FinBudgetVO toVO(FinBudget budget) {
        FinBudgetVO vo = new FinBudgetVO();
        vo.setId(budget.getId());
        vo.setDeptId(budget.getDeptId());
        vo.setProjectId(budget.getProjectId());
        vo.setExpenseCategory(budget.getExpenseCategory());
        vo.setYear(budget.getYear());
        vo.setMonth(budget.getMonth());
        vo.setAmount(budget.getAmount());
        vo.setOccupiedAmount(budget.getOccupiedAmount());
        vo.setExecutedAmount(budget.getExecutedAmount());
        vo.setAvailableAmount(budget.getAmount()
                .subtract(budget.getOccupiedAmount())
                .subtract(budget.getExecutedAmount()));
        vo.setControlStrategy(budget.getControlStrategy());
        vo.setStatus(budget.getStatus());
        vo.setCreateTime(budget.getCreateTime());
        vo.setUpdateTime(budget.getUpdateTime());
        return vo;
    }
}
