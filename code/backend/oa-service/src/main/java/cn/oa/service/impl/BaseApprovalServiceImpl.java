package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.WfDelegation;
import cn.oa.entity.WfTask;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.DelegationService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic base class for all approval-type ServiceImpl classes.
 * Encapsulates the common patterns found across LeaveApply, BusinessTrip, Outing,
 * Purchase, Expense, Overtime, and Loan services:
 * <ul>
 *   <li>submit — set status=0, save, start workflow</li>
 *   <li>approve — find pending task, handle it</li>
 *   <li>updateStatus — set status, invoke hook, persist</li>
 *   <li>pageList — paginated query with empId/status filter, fill empName and remark</li>
 *   <li>fillEmpNames — batch fill transient empName from SysEmployee</li>
 *   <li>fillRemarks — batch fill transient remark from OaApprovalRecord</li>
 * </ul>
 *
 * <p>Type parameters:
 * <ul>
 *   <li>M — MyBatis-Plus BaseMapper for the entity</li>
 *   <li>T — the OA approval entity (e.g. OaLeaveApply, OaBusinessTrip)</li>
 * </ul>
 *
 * <p>Subclasses must:
 * <ul>
 *   <li>Implement {@link #getBusinessType()}</li>
 *   <li>Set the SFunction fields in their no-arg constructor</li>
 *   <li>Override {@link #setStatus}, {@link #setEmpName}, {@link #setRemark} (entity-specific setters)</li>
 *   <li>Optionally override {@link #buildConditionContext}, {@link #onUpdateStatus}, {@link #fillRemarks}</li>
 * </ul>
 */
@Slf4j
public abstract class BaseApprovalServiceImpl<M extends BaseMapper<T>, T>
        extends ServiceImpl<M, T> {

    @Autowired
    protected SysEmployeeMapper employeeMapper;

    @Autowired
    protected OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    protected WorkflowService workflowService;

    @Autowired
    protected DelegationService delegationService;

    @Autowired
    protected cn.oa.mapper.WfTaskMapper wfTaskMapper;

    @Autowired
    protected cn.oa.mapper.WfProcessInstanceMapper wfProcessInstanceMapper;

    // ====== Abstract / overridable methods ======

    /** Return the BusinessType constant, e.g. BusinessType.LEAVE */
    protected abstract String getBusinessType();

    // ====== SFunction references — set by subclass constructor ======

    /** Getter reference for empId field, e.g. OaLeaveApply::getEmpId */
    protected SFunction<T, Long> empIdGetter;
    /** Getter reference for status field, e.g. OaLeaveApply::getStatus */
    protected SFunction<T, Integer> statusGetter;
    /** Getter reference for createTime field, e.g. OaLeaveApply::getCreateTime */
    protected SFunction<T, ?> createTimeGetter;
    /** Getter reference for id field, e.g. OaLeaveApply::getId */
    protected SFunction<T, Long> idGetter;

    // ====== submit ======

    /**
     * Common submit logic: set status=0, save entity, start workflow process.
     * Subclasses call this from their submit() method.
     */
    @Transactional
    public void doSubmit(T entity) {
        setStatus(entity, 0);
        this.save(entity);
        Map<String, Object> ctx = buildConditionContext(entity);
        workflowService.startProcess(getBusinessType(), getId(entity), getEmpId(entity), ctx);
    }

    /**
     * Build condition context map for workflow startProcess.
     * Default returns empty map. Subclasses override to put "days", "amount", "hours" etc.
     */
    protected Map<String, Object> buildConditionContext(T entity) {
        return new HashMap<>();
    }

    // ====== approve ======

    /**
     * Common approve logic: find pending task for this business, handle it.
     * Enhanced to support delegation: if the current user is a delegator whose
     * tasks were delegated, or a delegate acting on behalf of a delegator,
     * the task lookup will still succeed.
     */
    @Transactional
    public void doApprove(Long id, Long approverId, Integer status, String remark) {
        log.debug("doApprove: businessType={}, id={}, approverId={}, status={}", getBusinessType(), id, approverId, status);

        WfTask task = workflowService.findPendingTask(getBusinessType(), id, approverId);
        if (task != null) {
            Long taskAssigneeId = task.getAssigneeId();
            if (!taskAssigneeId.equals(approverId)) {
                boolean authorized = isAuthorizedForTask(approverId, taskAssigneeId);
                if (!authorized) {
                    log.warn("doApprove: user {} not authorized for task {} assigned to {}", approverId, task.getId(), taskAssigneeId);
                    throw new BusinessException("无权处理此任务");
                }
                log.info("doApprove: delegation approval - user {} acting on task {} assigned to {}", approverId, task.getId(), taskAssigneeId);
                workflowService.handleTask(task.getId(), taskAssigneeId, status, remark);
            } else {
                workflowService.handleTask(task.getId(), approverId, status, remark);
            }
        } else {
            log.warn("doApprove: no pending task found for businessType={}, id={}, approverId={}", getBusinessType(), id, approverId);
            throw new BusinessException("未找到待审批的任务");
        }
    }

    /**
     * Check if the current user is authorized to approve a task that is assigned
     * to another person. This covers delegation scenarios:
     * 1. The current user delegated their approval to the task assignee (delegator scenario)
     * 2. The current user is a delegate for the task assignee (delegate scenario)
     */
    private boolean isAuthorizedForTask(Long currentUserId, Long taskAssigneeId) {
        // Case 1: Current user is a delegator, task is assigned to their delegate
        Long delegateId = delegationService.resolveDelegate(currentUserId);
        if (delegateId != null && delegateId.equals(taskAssigneeId)) {
            return true;
        }

        // Case 2: Current user is a delegate, task is assigned to the delegator
        WfDelegation reverseDelegation = delegationService.findActiveDelegationForDelegate(currentUserId);
        if (reverseDelegation != null && reverseDelegation.getDelegatorId().equals(taskAssigneeId)) {
            return true;
        }

        return false;
    }

    // ====== updateStatus ======

    /**
     * Common updateStatus logic: fetch entity, set status, invoke hook, updateById.
     * Subclasses call this from their updateStatus() and override
     * {@link #onUpdateStatus(Object, Integer, Integer)} for custom side-effects.
     */
    @Transactional
    public void doUpdateStatus(Long id, Integer status) {
        if (id == null || status == null) return;
        T entity = this.getById(id);
        if (entity == null) return;

        Integer oldStatus = getStatus(entity);
        setStatus(entity, status);

        // Hook for subclass custom logic (budget, balance, attendance, etc.)
        onUpdateStatus(entity, status, oldStatus);

        this.updateById(entity);
    }

    /**
     * Hook called during updateStatus after status is set but before persist.
     * Default is no-op. Subclasses override for custom side-effects
     * (leave-balance deduction, attendance marking, budget validation, etc.).
     *
     * @param entity    the entity being updated (status already set to new value)
     * @param newStatus the new status value
     * @param oldStatus the previous status value before this change
     */
    protected void onUpdateStatus(T entity, Integer newStatus, Integer oldStatus) {
        // no-op by default
    }

    // ====== pageList ======

    /**
     * Common paginated query: filter by empId/status, order by createTime desc,
     * then fill empName and remark on results.
     */
    public IPage<T> doPageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<T> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        if (empId != null && empIdGetter != null) {
            wrapper.eq(empIdGetter, empId);
        }
        if (status != null && statusGetter != null) {
            wrapper.eq(statusGetter, status);
        }
        if (createTimeGetter != null) {
            wrapper.orderByDesc(createTimeGetter);
        }
        IPage<T> result = this.page(page, wrapper);
        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());
        return result;
    }

    // ====== fillEmpNames ======

    /**
     * Batch-fill the transient empName field on records by looking up SysEmployee.
     */
    protected void fillEmpNames(List<T> records) {
        if (records == null || records.isEmpty()) return;
        if (empIdGetter == null) return;

        Set<Long> empIds = records.stream()
                .map(empIdGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (T record : records) {
            Long eid = empIdGetter.apply(record);
            if (eid != null) {
                setEmpName(record, nameMap.getOrDefault(eid, ""));
            }
        }
    }

    // ====== fillRemarks ======

    /**
     * Batch-fill the transient remark field by querying the latest OaApprovalRecord
     * for each entity. 5 of 7 ServiceImpl classes use this pattern.
     * Subclasses that don't need remarks (LoanServiceImpl, OvertimeServiceImpl)
     * should override to no-op.
     */
    protected void fillRemarks(List<T> records) {
        if (records == null || records.isEmpty()) return;
        if (idGetter == null) return;

        List<Long> applyIds = records.stream()
                .map(idGetter)
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

        for (T record : records) {
            Long rid = idGetter.apply(record);
            if (rid != null) {
                setRemark(record, remarkMap.getOrDefault(rid, ""));
            }
        }
    }

    // ====== entity field access — subclasses must implement ======

    /** Get the entity id. Default uses idGetter. */
    protected Long getId(T entity) {
        return idGetter != null ? idGetter.apply(entity) : null;
    }

    /** Get the empId. Default uses empIdGetter. */
    protected Long getEmpId(T entity) {
        return empIdGetter != null ? empIdGetter.apply(entity) : null;
    }

    /** Get the status. Default uses statusGetter. */
    protected Integer getStatus(T entity) {
        return statusGetter != null ? statusGetter.apply(entity) : null;
    }

    /**
     * Set the status on an entity.
     * Must be overridden by subclass since generics can't call setter directly.
     */
    protected abstract void setStatus(T entity, Integer status);

    /**
     * Set the transient empName on an entity.
     * Must be overridden by subclass since generics can't call setter directly.
     */
    protected abstract void setEmpName(T entity, String name);

    /**
     * Set the transient remark on an entity.
     * Must be overridden by subclass since generics can't call setter directly.
     */
    protected abstract void setRemark(T entity, String remark);
}
