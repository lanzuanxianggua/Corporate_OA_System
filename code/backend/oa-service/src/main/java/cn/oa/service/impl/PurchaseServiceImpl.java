package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaBudget;
import cn.oa.entity.OaPurchase;
import cn.oa.entity.WfTask;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaPurchaseMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.service.BudgetService;
import cn.oa.service.PurchaseService;
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
@Slf4j
public class PurchaseServiceImpl extends ServiceImpl<OaPurchaseMapper, OaPurchase> implements PurchaseService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WfTaskMapper wfTaskMapper;

    @Lazy
    @Autowired
    private BudgetService budgetService;

    @Override
    @Transactional
    public void submit(OaPurchase purchase) {
        if (purchase.getAmount() == null) {
            throw new BusinessException("采购金额不能为空");
        }
        purchase.setStatus(0);
        this.save(purchase);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", purchase.getAmount().doubleValue());
        ctx.put("quantity", purchase.getQuantity());
        workflowService.startProcess(BusinessType.PURCHASE, purchase.getId(), purchase.getEmpId(), ctx);
        log.info("Purchase submitted: id={}, empId={}", purchase.getId(), purchase.getEmpId());
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        approve(applyId, approverId, status, remark, null);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        WfTask task = workflowService.findPendingTask(BusinessType.PURCHASE, applyId, approverId);
        if (task == null && taskId != null) {
            task = wfTaskMapper.selectById(taskId);
        }
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        OaPurchase purchase = this.getById(id);
        if (purchase == null) return;

        Integer oldStatus = purchase.getStatus();
        purchase.setStatus(status);
        this.updateById(purchase);

        // Budget validation and update
        SysEmployee emp = employeeMapper.selectById(purchase.getEmpId());
        if (emp == null || emp.getDeptId() == null) return;

        LocalDate now = LocalDate.now();
        OaBudget budget = budgetService.getByDeptMonth(emp.getDeptId(), now.getYear(), now.getMonthValue());

        // When approved (status=1): check budget and update usedAmount
        if (status == 1 && oldStatus != 1) {
            if (budget != null) {
                BigDecimal remaining = budget.getAmount().subtract(budget.getUsedAmount());
                if (purchase.getAmount().compareTo(remaining) > 0) {
                    throw new BusinessException("采购金额超出部门预算余额，剩余预算：" + remaining);
                }
                budgetService.updateUsedAmount(budget.getId(), purchase.getAmount());
            }
        }

        // When rejected(2) or withdrawn(4) after being approved(1): restore budget
        if ((status == 2 || status == 4) && oldStatus == 1) {
            if (budget != null) {
                budgetService.updateUsedAmount(budget.getId(), purchase.getAmount().negate());
            }
        }
    }

    @Override
    public IPage<OaPurchase> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaPurchase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaPurchase> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaPurchase::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaPurchase::getStatus, status);
        }
        wrapper.orderByDesc(OaPurchase::getCreateTime);
        IPage<OaPurchase> result = this.page(page, wrapper);

        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaPurchase> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaPurchase::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaPurchase record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaPurchase> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaPurchase::getId)
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

        for (OaPurchase record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
