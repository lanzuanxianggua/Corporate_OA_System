package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaPurchase;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.OaBudget;
import cn.oa.mapper.OaPurchaseMapper;
import cn.oa.service.BudgetService;
import cn.oa.service.PurchaseService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class PurchaseServiceImpl extends BaseApprovalServiceImpl<OaPurchaseMapper, OaPurchase>
        implements PurchaseService {

    @Lazy
    @Autowired
    private BudgetService budgetService;

    public PurchaseServiceImpl() {
        this.empIdGetter = OaPurchase::getEmpId;
        this.statusGetter = OaPurchase::getStatus;
        this.createTimeGetter = OaPurchase::getCreateTime;
        this.idGetter = OaPurchase::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.PURCHASE;
    }

    @Override
    protected void setStatus(OaPurchase entity, Integer status) {
        entity.setStatus(status);
    }

    @Override
    protected void setEmpName(OaPurchase entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaPurchase entity, String remark) {
        entity.setRemark(remark);
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaPurchase entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", entity.getAmount().doubleValue());
        ctx.put("quantity", entity.getQuantity());
        return ctx;
    }

    @Override
    protected void onUpdateStatus(OaPurchase entity, Integer newStatus, Integer oldStatus) {
        SysEmployee emp = employeeMapper.selectById(entity.getEmpId());
        if (emp == null || emp.getDeptId() == null) return;

        LocalDate now = LocalDate.now();
        OaBudget budget = budgetService.getByDeptMonth(emp.getDeptId(), now.getYear(), now.getMonthValue());

        if (newStatus == 1 && !Integer.valueOf(1).equals(oldStatus)) {
            if (budget != null) {
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
    public void submit(OaPurchase purchase) {
        if (purchase.getAmount() == null) {
            throw new BusinessException("采购金额不能为空");
        }
        doSubmit(purchase);
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
    public IPage<OaPurchase> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
