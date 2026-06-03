package cn.oa.finance.service.impl;

import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanRepayDTO;
import cn.oa.finance.entity.FinLoan;
import cn.oa.finance.entity.FinLoanRepayment;
import cn.oa.finance.mapper.FinLoanMapper;
import cn.oa.finance.mapper.FinLoanRepaymentMapper;
import cn.oa.finance.service.FinLoanService;
import cn.oa.finance.vo.FinLoanVO;
import cn.oa.platform.core.exception.BusinessException;
import cn.oa.workflow.core.engine.IWorkflowEngine;
import cn.oa.workflow.model.dto.StartProcessDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 借款服务实现
 *
 * @author oa-finance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinLoanServiceImpl implements FinLoanService {

    private final FinLoanMapper loanMapper;
    private final FinLoanRepaymentMapper repaymentMapper;
    private final IWorkflowEngine workflowEngine;

    private static final String BUSINESS_TYPE = "loan";

    @Override
    @Transactional
    public Long createLoan(FinLoanCreateDTO dto, Long empId) {
        if (dto.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("借款金额必须大于0");
        }

        FinLoan loan = new FinLoan();
        loan.setEmpId(empId);
        loan.setLoanAmount(dto.getLoanAmount());
        loan.setRepaidAmount(BigDecimal.ZERO);
        loan.setLoanReason(dto.getLoanReason());
        loan.setRepaymentPlan(dto.getRepaymentPlan());
        loan.setStatus("0"); // 草稿

        loanMapper.insert(loan);
        log.info("Loan created: id={}, empId={}, amount={}", loan.getId(), empId, dto.getLoanAmount());

        return loan.getId();
    }

    @Override
    @Transactional
    public void submitToWorkflow(Long id, Long empId) {
        FinLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new BusinessException("借款申请不存在");
        }

        if (!"0".equals(loan.getStatus()) && !"2".equals(loan.getStatus())) {
            throw new BusinessException("当前状态不允许提交审批");
        }

        // 更新状态为审批中
        loan.setStatus("1");
        loanMapper.updateById(loan);

        // 启动工作流
        try {
            StartProcessDTO startDTO = new StartProcessDTO();
            startDTO.setBusinessType(BUSINESS_TYPE);
            startDTO.setBusinessId(loan.getId());

            Map<String, Object> conditionContext = new HashMap<>();
            conditionContext.put("amount", loan.getLoanAmount());
            conditionContext.put("empId", empId);
            startDTO.setConditionContext(conditionContext);

            Long processInstanceId = workflowEngine.startWorkflow(startDTO);

            loan.setProcessInstanceId(processInstanceId);
            loanMapper.updateById(loan);

            log.info("Loan submitted to workflow: id={}, processInstanceId={}", id, processInstanceId);

        } catch (Exception e) {
            log.error("Failed to start workflow for loan: id={}", id, e);
            loan.setStatus("0");
            loanMapper.updateById(loan);
            throw new BusinessException("启动审批流程失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void repay(Long loanId, FinLoanRepayDTO dto, Long empId) {
        FinLoan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new BusinessException("借款记录不存在");
        }

        // 只有已通过的借款可以还款
        if (!"4".equals(loan.getStatus())) {
            throw new BusinessException("当前借款状态不允许还款");
        }

        // 校验还款金额
        BigDecimal remaining = loan.getLoanAmount().subtract(loan.getRepaidAmount());
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("还款金额必须大于0");
        }
        if (dto.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException("还款金额不能超过剩余应还金额");
        }

        // 创建还款记录
        FinLoanRepayment repayment = new FinLoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setAmount(dto.getAmount());
        repayment.setRepayTime(LocalDateTime.now());
        repayment.setRemark(dto.getRemark());
        repaymentMapper.insert(repayment);

        // 更新已还金额
        loan.setRepaidAmount(loan.getRepaidAmount().add(dto.getAmount()));
        loanMapper.updateById(loan);

        log.info("Loan repayment: loanId={}, amount={}, repaidAmount={}",
                loanId, dto.getAmount(), loan.getRepaidAmount());
    }

    @Override
    public IPage<FinLoanVO> pageQuery(Integer pageNum, Integer pageSize, Long empId, boolean isAdmin, String status) {
        LambdaQueryWrapper<FinLoan> wrapper = new LambdaQueryWrapper<>();

        if (!isAdmin) {
            wrapper.eq(FinLoan::getEmpId, empId);
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq(FinLoan::getStatus, status);
        }

        wrapper.orderByDesc(FinLoan::getCreateTime);

        IPage<FinLoan> page = loanMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public FinLoanVO getDetail(Long id) {
        FinLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            return null;
        }
        return toVO(loan);
    }

    @Override
    @Transactional
    public void onWorkflowApproved(Long id) {
        FinLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            log.warn("Loan not found for approval callback: id={}", id);
            return;
        }

        if (!"1".equals(loan.getStatus())) {
            log.info("Loan already processed: id={}, status={}", id, loan.getStatus());
            return;
        }

        loan.setStatus("4"); // 已通过
        loanMapper.updateById(loan);

        log.info("Loan approved: id={}", id);
    }

    @Override
    @Transactional
    public void onWorkflowRejected(Long id, String rejectReason) {
        FinLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            log.warn("Loan not found for rejection callback: id={}", id);
            return;
        }

        if (!"1".equals(loan.getStatus())) {
            log.info("Loan already processed: id={}, status={}", id, loan.getStatus());
            return;
        }

        loan.setStatus("2"); // 已驳回
        loanMapper.updateById(loan);

        log.info("Loan rejected: id={}, reason={}", id, rejectReason);
    }

    @Override
    @Transactional
    public void onWorkflowWithdrawn(Long id) {
        FinLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            log.warn("Loan not found for withdrawal callback: id={}", id);
            return;
        }

        if (!"1".equals(loan.getStatus())) {
            log.info("Loan already processed: id={}, status={}", id, loan.getStatus());
            return;
        }

        loan.setStatus("3"); // 已撤回
        loanMapper.updateById(loan);

        log.info("Loan withdrawn via callback: id={}", id);
    }

    // ==================== 私有方法 ====================

    private FinLoanVO toVO(FinLoan loan) {
        FinLoanVO vo = new FinLoanVO();
        vo.setId(loan.getId());
        vo.setEmpId(loan.getEmpId());
        vo.setLoanAmount(loan.getLoanAmount());
        vo.setRepaidAmount(loan.getRepaidAmount());
        vo.setRemainingAmount(loan.getLoanAmount().subtract(loan.getRepaidAmount()));
        vo.setLoanReason(loan.getLoanReason());
        vo.setRepaymentPlan(loan.getRepaymentPlan());
        vo.setStatus(loan.getStatus());
        vo.setProcessInstanceId(loan.getProcessInstanceId());
        vo.setCreateTime(loan.getCreateTime());
        vo.setUpdateTime(loan.getUpdateTime());

        // 可操作状态
        vo.setCanRevoke("1".equals(loan.getStatus()));
        vo.setCanRepay("4".equals(loan.getStatus())
                && loan.getRepaidAmount().compareTo(loan.getLoanAmount()) < 0);

        return vo;
    }
}
