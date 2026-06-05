package cn.oa.finance.service;

import cn.oa.finance.dto.FinExpenseCreateDTO;
import cn.oa.finance.dto.FinExpenseQueryDTO;
import cn.oa.finance.entity.FinBudget;
import cn.oa.finance.entity.FinExpense;
import cn.oa.platform.common.context.UserContext;
import cn.oa.finance.entity.FinExpenseDetail;
import cn.oa.finance.enums.FinConstants;
import cn.oa.finance.mapper.FinBudgetMapper;
import cn.oa.finance.mapper.FinExpenseDetailMapper;
import cn.oa.finance.mapper.FinExpenseMapper;
import cn.oa.finance.vo.FinExpenseVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报销 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinExpenseService {

    private final FinExpenseMapper mapper;
    private final FinExpenseDetailMapper detailMapper;
    private final FinBudgetMapper budgetMapper;
    private final WfInstanceService wfInstanceService;

    /**
     * 创建报销单.
     * 1) 创建 FinExpense (DRAFT)
     * 2) 批量创建 FinExpenseDetail
     * 3) 启动工作流, 回写 wf_instance_id
     * 4) 冻结预算
     *
     * @param dto   创建参数
     * @param empId 报销人
     * @return 报销单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(FinExpenseCreateDTO dto, Long empId) {
        // 1) 创建报销单头
        FinExpense expense = new FinExpense();
        expense.setApplyNo(generateApplyNo());
        expense.setEmpId(empId);
        expense.setExpenseType(dto.getExpenseType());
        expense.setTotalAmount(dto.getTotalAmount());
        expense.setReason(dto.getReason());
        expense.setStatus(FinConstants.EXPENSE_STATUS_DRAFT);
        expense.setLoanOffsetAmount(BigDecimal.ZERO);
        mapper.insert(expense);
        Long expenseId = expense.getId();

        // 2) 批量创建明细
        if (dto.getDetails() != null) {
            for (FinExpenseCreateDTO.ExpenseDetailItem item : dto.getDetails()) {
                FinExpenseDetail detail = new FinExpenseDetail();
                detail.setExpenseId(expenseId);
                detail.setFeeDate(item.getFeeDate());
                detail.setFeeType(item.getFeeType());
                detail.setAmount(item.getAmount());
                detail.setInvoiceNo(item.getInvoiceNo());
                detail.setRemark(item.getRemark());
                detailMapper.insert(detail);
            }
        }

        // 3) 启动工作流
        String businessKey = "EXP_" + expenseId;
        Long wfInstanceId = wfInstanceService.start("finance_expense", businessKey, empId);
        expense.setWfInstanceId(wfInstanceId);
        expense.setStatus(FinConstants.EXPENSE_STATUS_PENDING);
        mapper.updateById(expense);

        // 4) 冻结预算
        freezeBudget(expense.getDeptId(), dto.getTotalAmount());

        log.info("报销单已创建: expenseId={}, applyNo={}, empId={}, amount={}",
                expenseId, expense.getApplyNo(), empId, dto.getTotalAmount());
        return expenseId;
    }

    /**
     * 撤回报销单.
     * 仅 DRAFT/PENDING 状态可撤回, 撤回后解冻预算.
     *
     * @param id    报销单 ID
     * @param empId 操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id, Long empId) {
        FinExpense expense = checkExpenseExists(id);
        if (!FinConstants.EXPENSE_STATUS_DRAFT.equals(expense.getStatus())
                && !FinConstants.EXPENSE_STATUS_PENDING.equals(expense.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅草稿/待审批状态可撤回, 当前状态: " + expense.getStatus());
        }

        expense.setStatus(FinConstants.EXPENSE_STATUS_DRAFT); // revert to DRAFT on withdraw
        mapper.updateById(expense);

        // 解冻预算
        unfreezeBudget(expense.getDeptId(), expense.getTotalAmount());

        log.info("报销单已撤回: expenseId={}, empId={}", id, empId);
    }

    /**
     * 审批通过报销单.
     * 将冻结金额转为已使用金额.
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        FinExpense expense = checkExpenseExists(id);
        if (!FinConstants.EXPENSE_STATUS_PENDING.equals(expense.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅待审批状态可审批通过, 当前状态: " + expense.getStatus());
        }

        // 冻结 → 已使用
        approveBudget(expense.getDeptId(), expense.getTotalAmount());

        expense.setStatus(FinConstants.EXPENSE_STATUS_APPROVED);
        mapper.updateById(expense);

        log.info("报销单已审批通过: expenseId={}", id);
    }

    /**
     * 驳回报销单.
     * 驳回后解冻预算.
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        FinExpense expense = checkExpenseExists(id);
        if (!FinConstants.EXPENSE_STATUS_PENDING.equals(expense.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST,
                    "仅待审批状态可驳回, 当前状态: " + expense.getStatus());
        }

        // 解冻预算
        unfreezeBudget(expense.getDeptId(), expense.getTotalAmount());

        expense.setStatus(FinConstants.EXPENSE_STATUS_REJECTED);
        mapper.updateById(expense);

        log.info("报销单已驳回: expenseId={}", id);
    }

    /**
     * 查询报销单详情 (含明细).
     *
     * @param id 报销单 ID
     * @return Map 包含报销单信息和明细列表
     */
    public Map<String, Object> getById(Long id) {
        FinExpense expense = checkExpenseExists(id);
        List<FinExpenseDetail> details = detailMapper.findByExpenseId(id);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", expense.getId());
        result.put("applyNo", expense.getApplyNo());
        result.put("empId", expense.getEmpId());
        result.put("deptId", expense.getDeptId());
        result.put("expenseType", expense.getExpenseType());
        result.put("totalAmount", expense.getTotalAmount());
        result.put("reason", expense.getReason());
        result.put("status", expense.getStatus());
        result.put("wfInstanceId", expense.getWfInstanceId());
        result.put("loanOffsetAmount", expense.getLoanOffsetAmount());
        result.put("paidTime", expense.getPaidTime());
        result.put("createTime", expense.getCreateTime());
        result.put("updateTime", expense.getUpdateTime());
        result.put("details", details);
        return result;
    }

    /**
     * 分页查询报销单列表.
     *
     * @param query 查询参数
     * @param empId 报销人
     * @return 分页结果
     */
    public PageResult<FinExpenseVO> listPage(FinExpenseQueryDTO query, Long empId) {
        Page<FinExpense> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<FinExpense> wrapper = new LambdaQueryWrapper<FinExpense>()
                .eq(FinExpense::getEmpId, empId)
                .eq(query.getStatus() != null, FinExpense::getStatus, query.getStatus())
                .orderByDesc(FinExpense::getCreateTime);

        Page<FinExpense> result = mapper.selectPage(page, wrapper);

        List<FinExpenseVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 冻结预算 (提交报销时).
     */
    private void freezeBudget(Long deptId, BigDecimal amount) {
        FinBudget budget = findActiveBudget(deptId);
        if (budget == null) {
            log.warn("未找到可用预算, 跳过预算冻结: deptId={}", deptId);
            return;
        }
        BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
        BigDecimal frozen = budget.getFrozenAmount() != null ? budget.getFrozenAmount() : BigDecimal.ZERO;
        if (used.add(frozen).add(amount).compareTo(budget.getTotalAmount()) > 0) {
            throw new BizException(RCode.BAD_REQUEST, "预算不足, 可用预算: "
                    + budget.getTotalAmount().subtract(used).subtract(frozen));
        }
        budget.setFrozenAmount(frozen.add(amount));
        budgetMapper.updateById(budget);
    }

    /**
     * 解冻预算 (撤回/驳回时).
     */
    private void unfreezeBudget(Long deptId, BigDecimal amount) {
        FinBudget budget = findActiveBudget(deptId);
        if (budget == null) {
            log.warn("未找到可用预算, 跳过预算解冻: deptId={}", deptId);
            return;
        }
        BigDecimal frozen = budget.getFrozenAmount() != null ? budget.getFrozenAmount() : BigDecimal.ZERO;
        budget.setFrozenAmount(frozen.subtract(amount));
        budgetMapper.updateById(budget);
    }

    /**
     * 审批通过预算 (冻结 → 已使用).
     */
    private void approveBudget(Long deptId, BigDecimal amount) {
        FinBudget budget = findActiveBudget(deptId);
        if (budget == null) {
            log.warn("未找到可用预算, 跳过预算审批: deptId={}", deptId);
            return;
        }
        BigDecimal frozen = budget.getFrozenAmount() != null ? budget.getFrozenAmount() : BigDecimal.ZERO;
        BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
        budget.setFrozenAmount(frozen.subtract(amount));
        budget.setUsedAmount(used.add(amount));
        budgetMapper.updateById(budget);
    }

    /**
     * 查找当前年度有效的预算.
     */
    private FinBudget findActiveBudget(Long deptId) {
        if (deptId == null) return null;
        int year = LocalDate.now().getYear();
        LambdaQueryWrapper<FinBudget> wrapper = new LambdaQueryWrapper<FinBudget>()
                .eq(FinBudget::getDeptId, deptId)
                .eq(FinBudget::getBudgetYear, year)
                .eq(FinBudget::getStatus, FinConstants.BUDGET_STATUS_ACTIVE)
                .last("LIMIT 1");
        return budgetMapper.selectOne(wrapper);
    }

    /**
     * 检查报销单是否存在.
     */
    private FinExpense checkExpenseExists(Long id) {
        FinExpense expense = mapper.selectById(id);
        if (expense == null) {
            throw new BizException(RCode.NOT_FOUND, "报销单不存在: " + id);
        }
        return expense;
    }

    /**
     * 生成报销单号.
     */
    private String generateApplyNo() {
        return "EXP" + System.currentTimeMillis();
    }

    /**
     * Entity -> VO 转换.
     */
    private FinExpenseVO toVO(FinExpense expense) {
        FinExpenseVO vo = new FinExpenseVO();
        vo.setId(expense.getId());
        vo.setApplyNo(expense.getApplyNo());
        vo.setEmpId(expense.getEmpId());
        vo.setDeptId(expense.getDeptId());
        vo.setExpenseType(expense.getExpenseType());
        vo.setTotalAmount(expense.getTotalAmount());
        vo.setReason(expense.getReason());
        vo.setStatus(expense.getStatus());
        vo.setWfInstanceId(expense.getWfInstanceId());
        vo.setLoanOffsetAmount(expense.getLoanOffsetAmount());
        vo.setPaidTime(expense.getPaidTime());
        vo.setCreateTime(expense.getCreateTime());
        vo.setUpdateTime(expense.getUpdateTime());
        return vo;
    }
}
