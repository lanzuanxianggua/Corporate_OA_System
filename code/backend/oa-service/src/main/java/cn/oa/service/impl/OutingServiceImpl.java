package cn.oa.service.impl;

import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaOuting;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaOutingMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.OutingService;
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
public class OutingServiceImpl extends ServiceImpl<OaOutingMapper, OaOuting> implements OutingService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void submit(OaOuting outing) {
        outing.setStatus(0);
        this.save(outing);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        OaOuting outing = this.getById(applyId);
        if (outing == null) {
            throw new RuntimeException("外出申请不存在");
        }
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(applyId);
        record.setApproverId(approverId);
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
        outing.setStatus(status);
        this.updateById(outing);
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
