package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaBudget;
import cn.oa.mapper.OaBudgetMapper;
import cn.oa.service.BudgetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BudgetServiceImpl extends ServiceImpl<OaBudgetMapper, OaBudget> implements BudgetService {

    @Override
    public IPage<OaBudget> pageList(int pageNum, int pageSize, Long deptId, Integer budgetYear) {
        Page<OaBudget> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaBudget> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(OaBudget::getDeptId, deptId);
        }
        if (budgetYear != null) {
            wrapper.eq(OaBudget::getBudgetYear, budgetYear);
        }
        wrapper.orderByDesc(OaBudget::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public OaBudget getByDeptMonth(Long deptId, Integer year, Integer month) {
        return this.getOne(new LambdaQueryWrapper<OaBudget>()
                .eq(OaBudget::getDeptId, deptId)
                .eq(OaBudget::getBudgetYear, year)
                .eq(OaBudget::getBudgetMonth, month));
    }

    @Override
    @Transactional
    public void updateUsedAmount(Long budgetId, BigDecimal amount) {
        boolean updated = lambdaUpdate()
                .setSql("used_amount = used_amount + " + amount)
                .eq(OaBudget::getId, budgetId)
                .update();
        if (!updated) {
            throw new BusinessException("预算不存在或已被删除");
        }
    }
}
