package cn.oa.hr.service.impl;

import cn.oa.hr.dto.HrLeaveCreateDTO;
import cn.oa.hr.dto.HrLeaveQueryDTO;
import cn.oa.hr.entity.HrLeaveApply;
import cn.oa.hr.entity.HrLeaveBalance;
import cn.oa.hr.entity.HrLeaveRule;
import cn.oa.hr.enums.HrLeavePeriod;
import cn.oa.hr.enums.HrLeaveStatus;
import cn.oa.hr.enums.HrLeaveType;
import cn.oa.hr.mapper.HrLeaveApplyMapper;
import cn.oa.hr.service.HrLeaveBalanceService;
import cn.oa.hr.service.HrLeaveRuleService;
import cn.oa.hr.service.HrLeaveService;
import cn.oa.hr.vo.HrLeaveVO;
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
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HR请假服务实现
 *
 * @author oa-hr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveServiceImpl implements HrLeaveService {

    private final HrLeaveApplyMapper applyMapper;
    private final HrLeaveBalanceService balanceService;
    private final HrLeaveRuleService ruleService;
    private final IWorkflowEngine workflowEngine;

    private static final String BUSINESS_TYPE = "leave";

    @Override
    @Transactional
    public Long createAndSubmit(HrLeaveCreateDTO dto, Long empId, Long deptId) {
        // 1. 参数校验
        validateCreateDTO(dto);

        // 2. 计算请假天数
        BigDecimal days = calculateLeaveDays(dto.getStartTime(), dto.getEndTime(), dto.getLeavePeriod());
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请假天数必须大于0");
        }

        // 3. 校验假期规则
        boolean hasAttachment = dto.getAttachments() != null && !dto.getAttachments().isEmpty();
        String validationError = ruleService.validateLeaveRequest(dto.getLeaveType(), days, hasAttachment);
        if (validationError != null) {
            throw new BusinessException(validationError);
        }

        // 4. 检查是否需要扣减余额
        Integer year = dto.getStartTime().getYear();
        HrLeaveRule rule = ruleService.getRuleByLeaveType(dto.getLeaveType());
        boolean needDeductBalance = rule != null && Integer.valueOf(1).equals(rule.getDeductBalance());

        // 5. 如果需要扣减余额，先冻结
        if (needDeductBalance) {
            boolean frozen = balanceService.freezeBalance(empId, dto.getLeaveType(), year, days);
            if (!frozen) {
                // 检查是余额不存在还是余额不足
                HrLeaveBalance balance = balanceService.getBalance(empId, dto.getLeaveType(), year);
                if (balance == null) {
                    throw new BusinessException("您没有该类型假期余额");
                } else {
                    throw new BusinessException("假期余额不足");
                }
            }
        }

        // 6. 创建请假申请
        HrLeaveApply apply = new HrLeaveApply();
        apply.setApplyNo(generateApplyNo());
        apply.setEmpId(empId);
        apply.setDeptId(deptId);
        apply.setLeaveType(dto.getLeaveType());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        apply.setLeavePeriod(dto.getLeavePeriod());
        apply.setDays(days);
        apply.setReason(dto.getReason());
        apply.setAttachments(dto.getAttachments());
        apply.setStatus(HrLeaveStatus.RUNNING.getCode());

        applyMapper.insert(apply);
        log.info("Created leave apply: id={}, applyNo={}, empId={}, days={}",
                apply.getId(), apply.getApplyNo(), empId, days);

        // 7. 启动工作流
        try {
            StartProcessDTO startDTO = new StartProcessDTO();
            startDTO.setBusinessType(BUSINESS_TYPE);
            startDTO.setBusinessId(apply.getId());

            // 构建条件上下文（用于流程条件判断）
            Map<String, Object> conditionContext = new HashMap<>();
            conditionContext.put("days", days);
            conditionContext.put("leaveType", dto.getLeaveType());
            conditionContext.put("empId", empId);
            conditionContext.put("deptId", deptId);
            startDTO.setConditionContext(conditionContext);

            Long processInstanceId = workflowEngine.startWorkflow(startDTO);

            // 更新流程实例ID
            apply.setProcessInstanceId(processInstanceId);
            applyMapper.updateById(apply);

            log.info("Started workflow for leave apply: id={}, processInstanceId={}",
                    apply.getId(), processInstanceId);

        } catch (Exception e) {
            log.error("Failed to start workflow for leave apply: id={}", apply.getId(), e);
            // 工作流启动失败，需要释放冻结的余额
            if (needDeductBalance) {
                balanceService.releaseFrozenBalance(empId, dto.getLeaveType(), year, days);
            }
            throw new BusinessException("启动审批流程失败: " + e.getMessage());
        }

        return apply.getId();
    }

    @Override
    public BigDecimal calculateLeaveDays(LocalDateTime startTime, LocalDateTime endTime, String leavePeriod) {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        // 计算工作日天数（跳过周末）
        long weekdays = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                weekdays++;
            }
        }

        // 如果没有工作日，返回0
        if (weekdays == 0) {
            return BigDecimal.ZERO;
        }

        // 根据请假时段计算实际天数
        HrLeavePeriod period = HrLeavePeriod.fromCode(leavePeriod);
        if (period == null) {
            period = HrLeavePeriod.FULL;
        }

        if (period == HrLeavePeriod.FULL) {
            return BigDecimal.valueOf(weekdays);
        }

        // 半天场景：AM/PM
        boolean sameDay = startDate.equals(endDate);
        if (sameDay) {
            // 同一天半天
            return BigDecimal.valueOf(0.5);
        } else {
            // 跨天但半天：fullDays - 0.5
            return BigDecimal.valueOf(weekdays).subtract(BigDecimal.valueOf(0.5));
        }
    }

    @Override
    @Transactional
    public void revoke(Long id, Long empId, boolean isAdmin) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("请假申请不存在");
        }

        // 检查状态
        if (!HrLeaveStatus.RUNNING.getCode().equals(apply.getStatus())) {
            throw new BusinessException("当前状态不允许撤回");
        }

        // 权限检查：申请人或管理员可以撤回
        if (!apply.getEmpId().equals(empId) && !isAdmin) {
            throw new BusinessException("只有申请人或管理员可以撤回");
        }

        // 更新状态为已撤回
        apply.setStatus(HrLeaveStatus.REVOKED.getCode());
        applyMapper.updateById(apply);

        // 释放冻结的余额
        releaseBalanceIfNeeded(apply);

        // 撤回工作流
        try {
            workflowEngine.withdrawInstance(apply.getProcessInstanceId(), empId);
        } catch (Exception e) {
            log.warn("Failed to withdraw workflow instance: {}", apply.getProcessInstanceId(), e);
            // 工作流撤回失败不影响业务状态
        }

        log.info("Revoked leave apply: id={}, empId={}", id, empId);
    }

    @Override
    @Transactional
    public void resubmit(Long id, HrLeaveCreateDTO dto, Long empId) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException("请假申请不存在");
        }

        // 检查状态：只有驳回状态可以重提
        if (!HrLeaveStatus.REJECTED.getCode().equals(apply.getStatus())) {
            throw new BusinessException("当前状态不允许重新提交");
        }

        // 检查权限
        if (!apply.getEmpId().equals(empId)) {
            throw new BusinessException("只有申请人可以重新提交");
        }

        // 计算新天数
        BigDecimal newDays = calculateLeaveDays(dto.getStartTime(), dto.getEndTime(), dto.getLeavePeriod());
        if (newDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请假天数必须大于0");
        }

        // 校验规则
        boolean hasAttachment = dto.getAttachments() != null && !dto.getAttachments().isEmpty();
        String validationError = ruleService.validateLeaveRequest(dto.getLeaveType(), newDays, hasAttachment);
        if (validationError != null) {
            throw new BusinessException(validationError);
        }

        // 检查是否需要扣减余额
        Integer year = dto.getStartTime().getYear();
        HrLeaveRule rule = ruleService.getRuleByLeaveType(dto.getLeaveType());
        boolean needDeductBalance = rule != null && Integer.valueOf(1).equals(rule.getDeductBalance());

        // 冻结余额
        if (needDeductBalance) {
            boolean frozen = balanceService.freezeBalance(empId, dto.getLeaveType(), year, newDays);
            if (!frozen) {
                throw new BusinessException("假期余额不足");
            }
        }

        // 更新申请
        apply.setLeaveType(dto.getLeaveType());
        apply.setStartTime(dto.getStartTime());
        apply.setEndTime(dto.getEndTime());
        apply.setLeavePeriod(dto.getLeavePeriod());
        apply.setDays(newDays);
        apply.setReason(dto.getReason());
        apply.setAttachments(dto.getAttachments());
        apply.setStatus(HrLeaveStatus.RUNNING.getCode());
        apply.setRejectReason(null);

        applyMapper.updateById(apply);

        // 重新启动工作流
        try {
            StartProcessDTO startDTO = new StartProcessDTO();
            startDTO.setBusinessType(BUSINESS_TYPE);
            startDTO.setBusinessId(apply.getId());

            Map<String, Object> conditionContext = new HashMap<>();
            conditionContext.put("days", newDays);
            conditionContext.put("leaveType", dto.getLeaveType());
            conditionContext.put("empId", empId);
            startDTO.setConditionContext(conditionContext);

            Long processInstanceId = workflowEngine.startWorkflow(startDTO);
            apply.setProcessInstanceId(processInstanceId);
            applyMapper.updateById(apply);

        } catch (Exception e) {
            log.error("Failed to restart workflow for leave apply: id={}", id, e);
            if (needDeductBalance) {
                balanceService.releaseFrozenBalance(empId, dto.getLeaveType(), year, newDays);
            }
            throw new BusinessException("启动审批流程失败: " + e.getMessage());
        }

        log.info("Resubmitted leave apply: id={}, empId={}", id, empId);
    }

    @Override
    public IPage<HrLeaveVO> pageQuery(HrLeaveQueryDTO query, Long empId, boolean isAdmin) {
        LambdaQueryWrapper<HrLeaveApply> wrapper = new LambdaQueryWrapper<>();

        // 非管理员只能查看自己的申请
        if (!isAdmin) {
            wrapper.eq(HrLeaveApply::getEmpId, empId);
        } else {
            // 管理员可以按员工、部门筛选
            if (query.getEmpId() != null) {
                wrapper.eq(HrLeaveApply::getEmpId, query.getEmpId());
            }
            if (query.getDeptId() != null) {
                wrapper.eq(HrLeaveApply::getDeptId, query.getDeptId());
            }
        }

        // 状态筛选
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(HrLeaveApply::getStatus, query.getStatus());
        }

        // 假期类型筛选
        if (query.getLeaveType() != null && !query.getLeaveType().isEmpty()) {
            wrapper.eq(HrLeaveApply::getLeaveType, query.getLeaveType());
        }

        // 日期范围筛选
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            wrapper.ge(HrLeaveApply::getStartTime, LocalDate.parse(query.getStartDate()).atStartOfDay());
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            wrapper.le(HrLeaveApply::getEndTime, LocalDate.parse(query.getEndDate()).atTime(23, 59, 59));
        }

        // 排序
        wrapper.orderByDesc(HrLeaveApply::getCreateTime);

        IPage<HrLeaveApply> page = applyMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        return page.convert(this::toVO);
    }

    @Override
    public HrLeaveVO getDetail(Long id) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            return null;
        }
        return toVO(apply);
    }

    @Override
    @Transactional
    public void onWorkflowApproved(Long id, LocalDateTime approvedTime) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            log.warn("Leave apply not found for approval callback: id={}", id);
            return;
        }

        // 幂等检查：只有RUNNING状态可以转为PASSED
        if (!HrLeaveStatus.RUNNING.getCode().equals(apply.getStatus())) {
            log.info("Leave apply already processed: id={}, status={}", id, apply.getStatus());
            return;
        }

        // 更新状态
        apply.setStatus(HrLeaveStatus.PASSED.getCode());
        apply.setApprovedTime(approvedTime != null ? approvedTime : LocalDateTime.now());
        applyMapper.updateById(apply);

        // 确认余额（将冻结转为已用）
        confirmBalanceIfNeeded(apply);

        log.info("Leave apply approved: id={}", id);

        // TODO: 触发考勤标记接口
    }

    @Override
    @Transactional
    public void onWorkflowRejected(Long id, String rejectReason) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            log.warn("Leave apply not found for rejection callback: id={}", id);
            return;
        }

        // 幂等检查：只有RUNNING状态可以转为REJECTED
        if (!HrLeaveStatus.RUNNING.getCode().equals(apply.getStatus())) {
            log.info("Leave apply already processed: id={}, status={}", id, apply.getStatus());
            return;
        }

        // 更新状态
        apply.setStatus(HrLeaveStatus.REJECTED.getCode());
        apply.setRejectReason(rejectReason);
        applyMapper.updateById(apply);

        // 释放冻结的余额
        releaseBalanceIfNeeded(apply);

        log.info("Leave apply rejected: id={}, reason={}", id, rejectReason);
    }

    @Override
    @Transactional
    public void onWorkflowWithdrawn(Long id) {
        HrLeaveApply apply = applyMapper.selectById(id);
        if (apply == null) {
            log.warn("Leave apply not found for withdrawal callback: id={}", id);
            return;
        }

        // 幂等检查
        if (!HrLeaveStatus.RUNNING.getCode().equals(apply.getStatus())) {
            log.info("Leave apply already processed: id={}, status={}", id, apply.getStatus());
            return;
        }

        // 更新状态
        apply.setStatus(HrLeaveStatus.REVOKED.getCode());
        applyMapper.updateById(apply);

        // 释放冻结的余额
        releaseBalanceIfNeeded(apply);

        log.info("Leave apply withdrawn via callback: id={}", id);
    }

    // ==================== 私有方法 ====================

    private void validateCreateDTO(HrLeaveCreateDTO dto) {
        if (dto.getStartTime() == null) {
            throw new BusinessException("开始时间不能为空");
        }
        if (dto.getEndTime() == null) {
            throw new BusinessException("结束时间不能为空");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (dto.getLeaveType() == null || dto.getLeaveType().isEmpty()) {
            throw new BusinessException("假期类型不能为空");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new BusinessException("请假原因不能为空");
        }
        if (dto.getReason().length() > 500) {
            throw new BusinessException("请假原因不能超过500字");
        }
    }

    private String generateApplyNo() {
        // 格式: LV + yyyyMMdd + HHmmss + 4位随机数
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "LV" + timestamp + random;
    }

    private void releaseBalanceIfNeeded(HrLeaveApply apply) {
        HrLeaveRule rule = ruleService.getRuleByLeaveType(apply.getLeaveType());
        if (rule != null && Integer.valueOf(1).equals(rule.getDeductBalance())) {
            Integer year = apply.getStartTime().getYear();
            boolean released = balanceService.releaseFrozenBalance(
                    apply.getEmpId(), apply.getLeaveType(), year, apply.getDays());
            if (!released) {
                log.warn("Failed to release frozen balance for leave apply: id={}", apply.getId());
            }
        }
    }

    private void confirmBalanceIfNeeded(HrLeaveApply apply) {
        HrLeaveRule rule = ruleService.getRuleByLeaveType(apply.getLeaveType());
        if (rule != null && Integer.valueOf(1).equals(rule.getDeductBalance())) {
            Integer year = apply.getStartTime().getYear();
            boolean confirmed = balanceService.confirmBalance(
                    apply.getEmpId(), apply.getLeaveType(), year, apply.getDays());
            if (!confirmed) {
                log.warn("Failed to confirm balance for leave apply: id={}", apply.getId());
            }
        }
    }

    private HrLeaveVO toVO(HrLeaveApply apply) {
        HrLeaveVO vo = new HrLeaveVO();
        vo.setId(apply.getId());
        vo.setApplyNo(apply.getApplyNo());
        vo.setEmpId(apply.getEmpId());
        vo.setDeptId(apply.getDeptId());
        vo.setLeaveType(apply.getLeaveType());
        vo.setStartTime(apply.getStartTime());
        vo.setEndTime(apply.getEndTime());
        vo.setLeavePeriod(apply.getLeavePeriod());
        vo.setDays(apply.getDays());
        vo.setReason(apply.getReason());
        vo.setAttachments(apply.getAttachments());
        vo.setStatus(apply.getStatus());
        vo.setProcessInstanceId(apply.getProcessInstanceId());
        vo.setCurrentTaskId(apply.getCurrentTaskId());
        vo.setApprovedTime(apply.getApprovedTime());
        vo.setRejectReason(apply.getRejectReason());
        vo.setCreateTime(apply.getCreateTime());

        // 假期类型名称
        HrLeaveType leaveType = HrLeaveType.fromCode(apply.getLeaveType());
        vo.setLeaveTypeName(leaveType != null ? leaveType.getName() : apply.getLeaveType());

        // 状态名称
        HrLeaveStatus status = HrLeaveStatus.fromCode(apply.getStatus());
        vo.setStatusName(status != null ? status.getName() : apply.getStatus());

        // 可操作状态
        vo.setCanRevoke(HrLeaveStatus.RUNNING.getCode().equals(apply.getStatus()));
        vo.setCanResubmit(HrLeaveStatus.REJECTED.getCode().equals(apply.getStatus()));

        return vo;
    }
}
