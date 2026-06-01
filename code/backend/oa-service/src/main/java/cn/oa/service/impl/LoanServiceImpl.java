package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaLoan;
import cn.oa.entity.OaLoanRepayment;
import cn.oa.mapper.OaLoanMapper;
import cn.oa.mapper.OaLoanRepaymentMapper;
import cn.oa.service.LoanService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoanServiceImpl extends BaseApprovalServiceImpl<OaLoanMapper, OaLoan>
        implements LoanService {

    @Autowired
    private OaLoanRepaymentMapper repaymentMapper;

    public LoanServiceImpl() {
        this.empIdGetter = OaLoan::getEmpId;
        // OaLoan.status is String — override doPageList for proper filtering
        this.createTimeGetter = OaLoan::getCreateTime;
        this.idGetter = OaLoan::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.LOAN;
    }

    @Override
    protected void setStatus(OaLoan entity, Integer status) {
        entity.setStatus(String.valueOf(status));
    }

    @Override
    protected void setEmpName(OaLoan entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaLoan entity, String remark) {
        // OaLoan has no remark transient field — no-op
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaLoan entity) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("amount", entity.getLoanAmount().doubleValue());
        return ctx;
    }

    @Override
    protected void fillRemarks(java.util.List<OaLoan> records) {
        // no-op — OaLoan has no remark field
    }

    @Override
    public IPage<OaLoan> doPageList(int pageNum, int pageSize, Long empId, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OaLoan> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OaLoan> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaLoan::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaLoan::getStatus, String.valueOf(status));
        }
        if (createTimeGetter != null) {
            wrapper.orderByDesc(createTimeGetter);
        }
        IPage<OaLoan> result = this.page(page, wrapper);
        fillEmpNames(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public void submit(OaLoan loan) {
        if (loan.getLoanAmount() == null) {
            throw new BusinessException("借支金额不能为空");
        }
        doSubmit(loan);
    }

    @Override
    @Transactional
    public void approve(Long loanId, Long approverId, Integer status, String remark) {
        doApprove(loanId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void approve(Long loanId, Long approverId, Integer status, String remark, Long taskId) {
        doApprove(loanId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        doUpdateStatus(id, status);
    }

    @Override
    public IPage<OaLoan> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }

    @Override
    @Transactional
    public void addRepayment(Long loanId, BigDecimal amount, String remark) {
        OaLoan loan = this.getById(loanId);
        if (loan == null) {
            throw new BusinessException("借支记录不存在");
        }
        // 超还校验：还款金额不能超过剩余未还金额
        BigDecimal repaid = loan.getRepaidAmount() != null ? loan.getRepaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = loan.getLoanAmount().subtract(repaid);
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("还款金额超过剩余未还金额（剩余未还：" + remaining + "）");
        }

        // 插入还款记录
        OaLoanRepayment repayment = new OaLoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setAmount(amount);
        repayment.setRepayTime(LocalDateTime.now());
        repayment.setRemark(remark);
        repaymentMapper.insert(repayment);

        // 原子更新 repaid_amount 和 status，避免并发读-改-写竞态
        BigDecimal newRepaid = repaid.add(amount);
        String newStatus = newRepaid.compareTo(loan.getLoanAmount()) >= 0 ? "4" : "3";
        boolean updated = lambdaUpdate()
                .setSql("repaid_amount = repaid_amount + " + amount)
                .set(OaLoan::getStatus, newStatus)
                .eq(OaLoan::getId, loanId)
                .update();
        if (!updated) {
            throw new BusinessException("借支记录已被删除");
        }
    }
}
