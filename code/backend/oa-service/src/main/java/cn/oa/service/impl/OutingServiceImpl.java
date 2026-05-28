package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaOuting;
import cn.oa.entity.WfTask;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaOutingMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.mapper.WfTaskMapper;
import cn.oa.service.OutingService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OutingServiceImpl extends ServiceImpl<OaOutingMapper, OaOuting> implements OutingService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WfTaskMapper wfTaskMapper;

    @Override
    @Transactional
    public void submit(OaOuting outing) {
        if (outing.getStartTime() == null || outing.getEndTime() == null) {
            throw new BusinessException("外出起止时间不能为空");
        }
        outing.setStatus(0);
        this.save(outing);
        long days = java.time.temporal.ChronoUnit.DAYS.between(outing.getStartTime().toLocalDate(), outing.getEndTime().toLocalDate()) + 1;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("days", days);
        workflowService.startProcess(BusinessType.OUTING, outing.getId(), outing.getEmpId(), ctx);
        log.info("Outing submitted: id={}, empId={}", outing.getId(), outing.getEmpId());
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        approve(applyId, approverId, status, remark, null);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        WfTask task = null;
        if (taskId != null) {
            task = wfTaskMapper.selectById(taskId);
        }
        if (task == null) {
            task = workflowService.findPendingTask(BusinessType.OUTING, applyId, approverId);
        }
        if (task == null) {
            cn.oa.entity.WfProcessInstance instance = workflowService.getByBusiness(BusinessType.OUTING, applyId);
            if (instance != null) {
                LambdaQueryWrapper<WfTask> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WfTask::getInstanceId, instance.getId())
                       .eq(WfTask::getStatus, "0")
                       .orderByAsc(WfTask::getCreateTime)
                       .last("LIMIT 1");
                task = wfTaskMapper.selectOne(wrapper);
            }
        }
        if (task != null) {
            workflowService.handleTask(task.getId(), approverId, status, remark);
        } else {
            throw new BusinessException("未找到待审批的任务");
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        OaOuting outing = this.getById(id);
        if (outing != null) {
            outing.setStatus(status);
            this.updateById(outing);
        }
    }

    @Override
    public IPage<OaOuting> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaOuting> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaOuting> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaOuting::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaOuting::getStatus, status);
        }
        wrapper.orderByDesc(OaOuting::getCreateTime);
        IPage<OaOuting> result = this.page(page, wrapper);

        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaOuting> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaOuting::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaOuting record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaOuting> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaOuting::getId)
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

        for (OaOuting record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
