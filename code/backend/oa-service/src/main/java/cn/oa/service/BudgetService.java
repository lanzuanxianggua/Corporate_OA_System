package cn.oa.service;

import cn.oa.entity.OaBudget;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface BudgetService extends IService<OaBudget> {

    IPage<OaBudget> pageList(int pageNum, int pageSize, Long deptId, Integer budgetYear);

    OaBudget getByDeptMonth(Long deptId, Integer year, Integer month);

    void updateUsedAmount(Long budgetId, BigDecimal amount);

    void assertSufficientBudget(Long deptId, Integer year, Integer month, BigDecimal amount);
}
