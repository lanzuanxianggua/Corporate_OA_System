package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaExpense;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.OaBudget;
import cn.oa.mapper.OaExpenseMapper;
import cn.oa.service.BudgetService;
import cn.oa.service.ExpenseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExpenseServiceImpl extends BaseApprovalServiceImpl<OaExpenseMapper, OaExpense>
        implements ExpenseService {

    @Lazy
    @Autowired
    private BudgetService budgetService;

    public ExpenseServiceImpl() {
        this.empIdGetter = OaExpense::getEmpId;
        this.statusGetter = OaExpense::getStatus;
        this.createTimeGetter = OaExpense::getCreateTime;
        this.idGetter = OaExpense::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.EXPENSE;
    }

    @Override
    protected void setStatus(OaExpense entity, Integer status) {
        entity.setStatus(status);
    }

    @Override
    protected void setEmpName(OaExpense entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaExpense entity, String remark) {
        entity.setRemark(remark);
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaExpense entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", entity.getAmount().doubleValue());
        return ctx;
    }

    @Override
    protected void onUpdateStatus(OaExpense entity, Integer newStatus, Integer oldStatus) {
        SysEmployee emp = employeeMapper.selectById(entity.getEmpId());
        if (emp == null || emp.getDeptId() == null) return;

        LocalDate now = LocalDate.now();
        OaBudget budget = budgetService.getByDeptMonth(emp.getDeptId(), now.getYear(), now.getMonthValue());

        if (newStatus == 1 && !Integer.valueOf(1).equals(oldStatus)) {
            if (budget != null) {
                BigDecimal remaining = budget.getAmount().subtract(budget.getUsedAmount());
                if (entity.getAmount().compareTo(remaining) > 0) {
                    throw new BusinessException("经费超出部门预算余额，剩余预算：" + remaining);
                }
                budgetService.updateUsedAmount(budget.getId(), entity.getAmount());
            }
        }

        if ((newStatus == 2 || newStatus == 3) && Integer.valueOf(1).equals(oldStatus)) {
            if (budget != null) {
                budgetService.updateUsedAmount(budget.getId(), entity.getAmount().negate());
            }
        }
    }

    @Override
    @Transactional
    public void submit(OaExpense expense) {
        if (expense.getAmount() == null) {
            throw new BusinessException("经费金额不能为空");
        }
        doSubmit(expense);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        doApprove(applyId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        doApprove(applyId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        doUpdateStatus(id, status);
    }

    @Override
    public IPage<OaExpense> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
