package cn.oa.service.impl;

import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaLeaveApply;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaLeaveApplyMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.LeaveApplyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveApplyServiceImpl extends ServiceImpl<OaLeaveApplyMapper, OaLeaveApply> implements LeaveApplyService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void submit(OaLeaveApply apply) {
        apply.setStatus(0);
        this.save(apply);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        OaLeaveApply apply = this.getById(applyId);
        if (apply == null) {
            throw new RuntimeException("请假申请不存在");
        }
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(applyId);
        record.setApproverId(approverId);
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
        apply.setStatus(status);
        this.updateById(apply);
    }

    @Override
    public IPage<OaLeaveApply> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaLeaveApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaLeaveApply> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaLeaveApply::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaLeaveApply::getStatus, status);
        }
        wrapper.orderByDesc(OaLeaveApply::getCreateTime);
        IPage<OaLeaveApply> result = this.page(page, wrapper);

        // 填充 empName
        fillEmpNames(result.getRecords());
        // 填充 remark（取最新审批记录）
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaLeaveApply> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaLeaveApply::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaLeaveApply record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaLeaveApply> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaLeaveApply::getId)
                .collect(Collectors.toList());
        if (applyIds.isEmpty()) return;

        // 查询这些申请的审批记录
        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OaApprovalRecord::getApplyId, applyIds)
                .orderByDesc(OaApprovalRecord::getApproveTime);
        List<OaApprovalRecord> approvalRecords = approvalRecordMapper.selectList(wrapper);

        // 按 applyId 分组，取最新的 remark
        Map<Long, String> remarkMap = new HashMap<>();
        for (OaApprovalRecord ar : approvalRecords) {
            remarkMap.putIfAbsent(ar.getApplyId(), ar.getRemark());
        }

        for (OaLeaveApply record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
