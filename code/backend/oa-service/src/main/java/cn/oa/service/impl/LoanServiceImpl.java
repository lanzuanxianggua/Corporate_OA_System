package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaLoan;
import cn.oa.entity.OaLoanRepayment;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.WfTask;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaLoanMapper;
import cn.oa.mapper.OaLoanRepaymentMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.LoanService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoanServiceImpl extends ServiceImpl<OaLoanMapper, OaLoan> implements LoanService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private OaLoanRepaymentMapper repaymentMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public void submit(OaLoan loan) {
        loan.setStatus(0);
        this.save(loan);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", loan.getLoanAmount().doubleValue());
        workflowService.startProcess(BusinessType.LOAN, loan.getId(), loan.getEmpId(), ctx);
    }

    @Override
    @Transactional
    public void approve(Long loanId, Long approverId, Integer status, String remark) {
        WfTask task = workflowService.findPendingTask(BusinessType.LOAN, loanId, approverId);
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        OaLoan loan = this.getById(id);
        if (loan != null) {
            loan.setStatus(status);
            this.updateById(loan);
        }
    }

    @Override
    public IPage<OaLoan> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaLoan> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaLoan> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaLoan::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaLoan::getStatus, status);
        }
        wrapper.orderByDesc(OaLoan::getCreateTime);
        IPage<OaLoan> result = this.page(page, wrapper);
        fillEmpNames(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void addRepayment(Long loanId, BigDecimal amount, String remark) {
        OaLoan loan = this.getById(loanId);
        if (loan == null) {
            throw new RuntimeException("借支记录不存在");
        }
        OaLoanRepayment repayment = new OaLoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setAmount(amount);
        repayment.setRepayTime(LocalDateTime.now());
        repayment.setRemark(remark);
        repaymentMapper.insert(repayment);
    }

    private void fillEmpNames(List<OaLoan> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaLoan::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaLoan record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }
}