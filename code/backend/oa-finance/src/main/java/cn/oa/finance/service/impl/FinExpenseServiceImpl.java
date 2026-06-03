package cn.oa.finance.service.impl;

import cn.oa.finance.dto.FinExpenseCreateDTO;
import cn.oa.finance.dto.FinExpenseCreateDTO.FinExpenseDetailDTO;
import cn.oa.finance.dto.FinExpenseQueryDTO;
import cn.oa.finance.entity.FinExpense;
import cn.oa.finance.entity.FinExpenseDetail;
import cn.oa.finance.mapper.FinExpenseDetailMapper;
import cn.oa.finance.mapper.FinExpenseMapper;
import cn.oa.finance.service.FinExpenseService;
import cn.oa.finance.vo.FinExpenseVO;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用报销服务实现
 *
 * @author oa-finance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinExpenseServiceImpl implements FinExpenseService {

    private final FinExpenseMapper expenseMapper;
    private final FinExpenseDetailMapper detailMapper;
    private final IWorkflowEngine workflowEngine;

    private static final String BUSINESS_TYPE = "expense";

    @Override
    @Transactional
    public Long createExpense(FinExpenseCreateDTO dto, Long empId) {
        // 校验总金额
        BigDecimal total = dto.getDetails().stream()
                .map(FinExpenseDetailDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报销总金额必须大于0");
        }

        // 创建报销单
        FinExpense expense = new FinExpense();
        expense.setEmpId(empId);
        expense.setTitle(dto.getTitle());
        expense.setTotalAmount(total);
        expense.setCategory(dto.getCategory());
        expense.setDescription(dto.getDescription());
        expense.setRelatedTripId(dto.getRelatedTripId());
        expense.setRelatedLoanId(dto.getRelatedLoanId());
        expense.setLoanOffsetAmount(dto.getLoanOffsetAmount());
        expense.setStatus("0"); // 草稿

        expenseMapper.insert(expense);
        log.info("Expense created: id={}, empId={}, amount={}", expense.getId(), empId, total);

        // 创建报销明细
        for (FinExpenseDetailDTO detailDTO : dto.getDetails()) {
            FinExpenseDetail detail = new FinExpenseDetail();
            detail.setExpenseId(expense.getId());
            detail.setExpenseType(detailDTO.getExpenseType());
            detail.setAmount(detailDTO.getAmount());
            if (detailDTO.getExpenseDate() != null) {
                detail.setExpenseDate(LocalDate.parse(detailDTO.getExpenseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            detail.setInvoiceNo(detailDTO.getInvoiceNo());
            detail.setInvoiceImage(detailDTO.getInvoiceImage());
            detail.setRemark(detailDTO.getRemark());
            detailMapper.insert(detail);
        }

        return expense.getId();
    }

    @Override
    @Transactional
    public void submitToWorkflow(Long id, Long empId) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        if (!"0".equals(expense.getStatus()) && !"2".equals(expense.getStatus())) {
            throw new BusinessException("当前状态不允许提交审批");
        }

        // 更新状态为审批中
        expense.setStatus("1");
        expenseMapper.updateById(expense);

        // 启动工作流
        try {
            StartProcessDTO startDTO = new StartProcessDTO();
            startDTO.setBusinessType(BUSINESS_TYPE);
            startDTO.setBusinessId(expense.getId());

            Map<String, Object> conditionContext = new HashMap<>();
            conditionContext.put("amount", expense.getTotalAmount());
            conditionContext.put("category", expense.getCategory());
            conditionContext.put("empId", empId);
            startDTO.setConditionContext(conditionContext);

            Long processInstanceId = workflowEngine.startWorkflow(startDTO);

            expense.setProcessInstanceId(processInstanceId);
            expenseMapper.updateById(expense);

            log.info("Expense submitted to workflow: id={}, processInstanceId={}", id, processInstanceId);

        } catch (Exception e) {
            log.error("Failed to start workflow for expense: id={}", id, e);
            expense.setStatus("0");
            expenseMapper.updateById(expense);
            throw new BusinessException("启动审批流程失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void revoke(Long id, Long empId, boolean isAdmin) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        if (!"1".equals(expense.getStatus())) {
            throw new BusinessException("当前状态不允许撤回");
        }

        if (!expense.getEmpId().equals(empId) && !isAdmin) {
            throw new BusinessException("只有申请人或管理员可以撤回");
        }

        // 更新状态为已撤回
        expense.setStatus("3");
        expenseMapper.updateById(expense);

        // 撤回工作流
        try {
            workflowEngine.withdrawInstance(expense.getProcessInstanceId(), empId);
        } catch (Exception e) {
            log.warn("Failed to withdraw workflow instance: {}", expense.getProcessInstanceId(), e);
        }

        log.info("Expense revoked: id={}, empId={}", id, empId);
    }

    @Override
    public IPage<FinExpenseVO> pageQuery(FinExpenseQueryDTO query, Long empId, boolean isAdmin) {
        LambdaQueryWrapper<FinExpense> wrapper = new LambdaQueryWrapper<>();

        if (!isAdmin) {
            wrapper.eq(FinExpense::getEmpId, empId);
        } else if (query.getEmpId() != null) {
            wrapper.eq(FinExpense::getEmpId, query.getEmpId());
        }

        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            wrapper.eq(FinExpense::getCategory, query.getCategory());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(FinExpense::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            wrapper.ge(FinExpense::getCreateTime, LocalDate.parse(query.getStartDate()).atStartOfDay());
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            wrapper.le(FinExpense::getCreateTime, LocalDate.parse(query.getEndDate()).atTime(23, 59, 59));
        }

        wrapper.orderByDesc(FinExpense::getCreateTime);

        IPage<FinExpense> page = expenseMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public FinExpenseVO getDetail(Long id) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            return null;
        }
        FinExpenseVO vo = toVO(expense);

        // 查询明细
        LambdaQueryWrapper<FinExpenseDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(FinExpenseDetail::getExpenseId, id);
        List<FinExpenseDetail> details = detailMapper.selectList(detailWrapper);
        vo.setDetails(details);

        return vo;
    }

    @Override
    @Transactional
    public void onWorkflowApproved(Long id) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            log.warn("Expense not found for approval callback: id={}", id);
            return;
        }

        if (!"1".equals(expense.getStatus())) {
            log.info("Expense already processed: id={}, status={}", id, expense.getStatus());
            return;
        }

        expense.setStatus("4"); // 已通过
        expenseMapper.updateById(expense);

        log.info("Expense approved: id={}", id);
    }

    @Override
    @Transactional
    public void onWorkflowRejected(Long id, String rejectReason) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            log.warn("Expense not found for rejection callback: id={}", id);
            return;
        }

        if (!"1".equals(expense.getStatus())) {
            log.info("Expense already processed: id={}, status={}", id, expense.getStatus());
            return;
        }

        expense.setStatus("2"); // 已驳回
        expenseMapper.updateById(expense);

        log.info("Expense rejected: id={}, reason={}", id, rejectReason);
    }

    @Override
    @Transactional
    public void onWorkflowWithdrawn(Long id) {
        FinExpense expense = expenseMapper.selectById(id);
        if (expense == null) {
            log.warn("Expense not found for withdrawal callback: id={}", id);
            return;
        }

        if (!"1".equals(expense.getStatus())) {
            log.info("Expense already processed: id={}, status={}", id, expense.getStatus());
            return;
        }

        expense.setStatus("3"); // 已撤回
        expenseMapper.updateById(expense);

        log.info("Expense withdrawn via callback: id={}", id);
    }

    // ==================== 私有方法 ====================

    private FinExpenseVO toVO(FinExpense expense) {
        FinExpenseVO vo = new FinExpenseVO();
        vo.setId(expense.getId());
        vo.setEmpId(expense.getEmpId());
        vo.setTitle(expense.getTitle());
        vo.setTotalAmount(expense.getTotalAmount());
        vo.setCategory(expense.getCategory());
        vo.setDescription(expense.getDescription());
        vo.setRelatedTripId(expense.getRelatedTripId());
        vo.setRelatedLoanId(expense.getRelatedLoanId());
        vo.setLoanOffsetAmount(expense.getLoanOffsetAmount());
        vo.setStatus(expense.getStatus());
        vo.setProcessInstanceId(expense.getProcessInstanceId());
        vo.setCreateTime(expense.getCreateTime());
        vo.setUpdateTime(expense.getUpdateTime());

        // 可操作状态
        vo.setCanRevoke("1".equals(expense.getStatus()));

        return vo;
    }
}
