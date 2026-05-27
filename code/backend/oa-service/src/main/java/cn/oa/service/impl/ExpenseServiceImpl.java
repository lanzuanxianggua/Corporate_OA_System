package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaBudget;
import cn.oa.entity.OaExpense;
import cn.oa.entity.WfTask;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaExpenseMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.BudgetService;
import cn.oa.service.ExpenseService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseServiceImpl extends ServiceImpl<OaExpenseMapper, OaExpense> implements ExpenseService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Lazy
    @Autowired
    private BudgetService budgetService;

    @Override
    public void submit(OaExpense expense) {
        expense.setStatus(0);
        this.save(expense);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", expense.getAmount().doubleValue());
        workflowService.startProcess(BusinessType.EXPENSE, expense.getId(), expense.getEmpId(), ctx);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        WfTask task = workflowService.findPendingTask(BusinessType.EXPENSE, applyId, approverId);
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        OaExpense expense = this.getById(id);
        if (expense == null) return;

        Integer oldStatus = expense.getStatus();
        expense.setStatus(status);
        this.updateById(expense);

        // Budget validation and update
        SysEmployee emp = employeeMapper.selectById(expense.getEmpId());
        if (emp == null || emp.getDeptId() == null) return;

        LocalDate now = LocalDate.now();
        OaBudget budget = budgetService.getByDeptMonth(emp.getDeptId(), now.getYear(), now.getMonthValue());

        // When approved (status=1): check budget and update usedAmount
        if (status == 1 && oldStatus != 1) {
            if (budget != null) {
                BigDecimal remaining = budget.getAmount().subtract(budget.getUsedAmount());
                if (expense.getAmount().compareTo(remaining) > 0) {
                    throw new BusinessException("经费超出部门预算余额，剩余预算：" + remaining);
                }
                budgetService.updateUsedAmount(budget.getId(), expense.getAmount());
            }
        }

        // When rejected(2) or withdrawn(4) after being approved(1): restore budget
        if ((status == 2 || status == 4) && oldStatus == 1) {
            if (budget != null) {
                budgetService.updateUsedAmount(budget.getId(), expense.getAmount().negate());
            }
        }
    }

    @Override
    public IPage<OaExpense> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaExpense> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaExpense> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaExpense::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaExpense::getStatus, status);
        }
        wrapper.orderByDesc(OaExpense::getCreateTime);
        IPage<OaExpense> result = this.page(page, wrapper);

        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaExpense> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaExpense::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaExpense record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaExpense> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaExpense::getId)
                .collect(Collectors.toList());
        if (applyIds.isEmpty()) return;

        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OaApprovalRecord::getApplyId, applyIds)
                .orderByDesc(OaApprovalRecord::getApproveTime);
        List<OaApprovalRecord> approvalRecords = approvalRecordMapper.selectList(wrapper);

        Map<Long, String> remarkMap = new HashMap<>();
        for (OaApprovalRecord ar : approvalRecords) {
            remarkMap.putIfAbsent(ar.getApplyId(), ar.getRemark());
        }

        for (OaExpense record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
