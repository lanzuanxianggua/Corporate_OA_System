package cn.oa.finance.service;

import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanQueryDTO;
import cn.oa.finance.entity.FinLoan;
import cn.oa.finance.event.FinBusinessSubmittedEvent;
import cn.oa.platform.common.context.UserContext;
import cn.oa.finance.entity.FinLoan;
import cn.oa.finance.entity.FinLoanRepayment;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinLoanMapper;
import cn.oa.finance.mapper.FinLoanRepaymentMapper;
import cn.oa.finance.vo.FinLoanVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 借款 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinLoanService {

    private final FinLoanMapper mapper;
    private final FinLoanRepaymentMapper repaymentMapper;
    private final WfInstanceService wfInstanceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建借款单.
     * 1) 创建 FinLoan (DRAFT)
     * 2) 启动工作流, 回写 wf_instance_id
     *
     * @param dto   创建参数
     * @param empId 借款人
     * @return 借款单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(FinLoanCreateDTO dto, Long empId) {
        FinLoan loan = new FinLoan();
        loan.setApplyNo(generateApplyNo());
        loan.setEmpId(empId);
        loan.setLoanType(dto.getLoanType());
        loan.setAmount(dto.getAmount());
        loan.setPurpose(dto.getPurpose());
        loan.setDeadlineDate(dto.getDeadlineDate());
        loan.setStatus(FinConstants.LOAN_STATUS_DRAFT);
        loan.setRepaidAmount(BigDecimal.ZERO);
        mapper.insert(loan);
        Long loanId = loan.getId();

        // 启动工作流
        String businessKey = "LOAN_" + loanId;
        Long wfInstanceId = wfInstanceService.start("finance_loan", businessKey, empId);
        loan.setWfInstanceId(wfInstanceId);
        loan.setStatus(FinConstants.LOAN_STATUS_PENDING);
        mapper.updateById(loan);

        // 发布业务提交事件
        eventPublisher.publishEvent(new FinBusinessSubmittedEvent(
                "LOAN_", loanId, loan.getApplyNo(), empId, wfInstanceId));

        log.info("借款单已创建: loanId={}, applyNo={}, empId={}, amount={}",
                loanId, loan.getApplyNo(), empId, dto.getAmount());
        return loanId;
    }

    /**
     * 还款.
     * 创建还款记录, 更新已还金额; 若还清则更新状态为 SETTLED.
     *
     * @param loanId    借款单 ID
     * @param amount    还款金额
     * @param expenseId 关联报销单 ID (冲抵时传入)
     */
    @Transactional(rollbackFor = Exception.class)
    public void repay(Long loanId, BigDecimal amount, Long expenseId) {
        FinLoan loan = checkLoanExists(loanId);
        if (!FinConstants.LOAN_STATUS_APPROVED.equals(loan.getStatus())
                && !FinConstants.LOAN_STATUS_SETTLED.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅已审批的借款可还款, 当前状态: " + loan.getStatus());
        }

        BigDecimal repaid = loan.getRepaidAmount() != null ? loan.getRepaidAmount() : BigDecimal.ZERO;
        BigDecimal newRepaid = repaid.add(amount);
        if (newRepaid.compareTo(loan.getAmount()) > 0) {
            throw new BizException(RCode.BAD_REQUEST, "还款金额超出借款总额, 剩余未还: "
                    + loan.getAmount().subtract(repaid));
        }

        // 创建还款记录
        FinLoanRepayment repayment = new FinLoanRepayment();
        repayment.setLoanId(loanId);
        repayment.setRepayAmount(amount);
        repayment.setRepayType(expenseId != null
                ? FinConstants.REPAY_TYPE_EXPENSE_OFFSET
                : FinConstants.REPAY_TYPE_CASH);
        repayment.setExpenseId(expenseId);
        repayment.setRepayDate(LocalDate.now());
        repaymentMapper.insert(repayment);

        // 更新已还金额
        loan.setRepaidAmount(newRepaid);
        if (newRepaid.compareTo(loan.getAmount()) >= 0) {
            loan.setStatus(FinConstants.LOAN_STATUS_SETTLED);
        }
        mapper.updateById(loan);

        log.info("借款已还款: loanId={}, amount={}, repaid={}/{}, newStatus={}",
                loanId, amount, newRepaid, loan.getAmount(), loan.getStatus());
    }

    /**
     * 审批通过借款单.
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        FinLoan loan = checkLoanExists(id);
        if (!FinConstants.LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅待审批状态可审批通过, 当前状态: " + loan.getStatus());
        }
        loan.setStatus(FinConstants.LOAN_STATUS_APPROVED);
        mapper.updateById(loan);
        log.info("借款单已审批通过: loanId={}", id);
    }

    /**
     * 驳回借款单 (业务层兜底 — 与 oa-workflow 任务完成路径并列).
     * <p>状态机: PENDING → REJECTED. 不涉及预算冻结/解冻 (借款本身不占预算).
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        FinLoan loan = checkLoanExists(id);
        if (!FinConstants.LOAN_STATUS_PENDING.equals(loan.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅待审批状态可驳回, 当前状态: " + loan.getStatus());
        }
        loan.setStatus(FinConstants.LOAN_STATUS_REJECTED);
        mapper.updateById(loan);
        log.info("借款单已驳回: loanId={}", id);
    }

    /**
     * 查询借款单详情.
     *
     * @param id 借款单 ID
     * @return Map 包含借款单信息和还款记录
     */
    public Map<String, Object> getById(Long id) {
        FinLoan loan = checkLoanExists(id);
        List<FinLoanRepayment> repayments = repaymentMapper.findByLoanId(id);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", loan.getId());
        result.put("applyNo", loan.getApplyNo());
        result.put("empId", loan.getEmpId());
        result.put("deptId", loan.getDeptId());
        result.put("loanType", loan.getLoanType());
        result.put("amount", loan.getAmount());
        result.put("purpose", loan.getPurpose());
        result.put("status", loan.getStatus());
        result.put("wfInstanceId", loan.getWfInstanceId());
        result.put("repaidAmount", loan.getRepaidAmount());
        result.put("deadlineDate", loan.getDeadlineDate());
        result.put("createTime", loan.getCreateTime());
        result.put("updateTime", loan.getUpdateTime());
        result.put("repayments", repayments);
        return result;
    }

    /**
     * 分页查询借款单列表.
     *
     * @param query 查询参数
     * @param empId 借款人
     * @return 分页结果
     */
    public PageResult<FinLoanVO> listPage(FinLoanQueryDTO query, Long empId) {
        Page<FinLoan> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<FinLoan> wrapper = new LambdaQueryWrapper<FinLoan>()
                .eq(FinLoan::getEmpId, empId)
                .eq(query.getStatus() != null, FinLoan::getStatus, query.getStatus())
                .orderByDesc(FinLoan::getCreateTime);

        Page<FinLoan> result = mapper.selectPage(page, wrapper);

        List<FinLoanVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 检查借款单是否存在.
     */
    private FinLoan checkLoanExists(Long id) {
        FinLoan loan = mapper.selectById(id);
        if (loan == null) {
            throw new BizException(RCode.NOT_FOUND, "借款单不存在: " + id);
        }
        return loan;
    }

    /**
     * 生成借款单号.
     */
    private String generateApplyNo() {
        return "LOAN" + System.currentTimeMillis();
    }

    /**
     * Entity -> VO 转换.
     */
    private FinLoanVO toVO(FinLoan loan) {
        FinLoanVO vo = new FinLoanVO();
        vo.setId(loan.getId());
        vo.setApplyNo(loan.getApplyNo());
        vo.setEmpId(loan.getEmpId());
        vo.setDeptId(loan.getDeptId());
        vo.setLoanType(loan.getLoanType());
        vo.setAmount(loan.getAmount());
        vo.setPurpose(loan.getPurpose());
        vo.setStatus(loan.getStatus());
        vo.setWfInstanceId(loan.getWfInstanceId());
        vo.setRepaidAmount(loan.getRepaidAmount());
        vo.setDeadlineDate(loan.getDeadlineDate());
        vo.setCreateTime(loan.getCreateTime());
        vo.setUpdateTime(loan.getUpdateTime());
        return vo;
    }
}
