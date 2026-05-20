package cn.oa.service.impl;

import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaBusinessTrip;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaBusinessTripMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.BusinessTripService;
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
public class BusinessTripServiceImpl extends ServiceImpl<OaBusinessTripMapper, OaBusinessTrip> implements BusinessTripService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void submit(OaBusinessTrip trip) {
        trip.setStatus(0);
        this.save(trip);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        OaBusinessTrip trip = this.getById(applyId);
        if (trip == null) {
            throw new RuntimeException("出差申请不存在");
        }
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(applyId);
        record.setApproverId(approverId);
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
        trip.setStatus(status);
        this.updateById(trip);
    }

    @Override
    public IPage<OaBusinessTrip> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaBusinessTrip> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaBusinessTrip> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaBusinessTrip::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaBusinessTrip::getStatus, status);
        }
        wrapper.orderByDesc(OaBusinessTrip::getCreateTime);
        IPage<OaBusinessTrip> result = this.page(page, wrapper);

        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaBusinessTrip> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaBusinessTrip::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaBusinessTrip record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaBusinessTrip> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaBusinessTrip::getId)
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

        for (OaBusinessTrip record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
