package cn.oa.service.impl;

import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.OaPurchase;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaApprovalRecordMapper;
import cn.oa.mapper.OaPurchaseMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.PurchaseService;
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
public class PurchaseServiceImpl extends ServiceImpl<OaPurchaseMapper, OaPurchase> implements PurchaseService {

    @Autowired
    private OaApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void submit(OaPurchase purchase) {
        purchase.setStatus(0);
        this.save(purchase);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        OaPurchase purchase = this.getById(applyId);
        if (purchase == null) {
            throw new RuntimeException("采购申请不存在");
        }
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApplyId(applyId);
        record.setApproverId(approverId);
        record.setApproveStatus(status);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
        purchase.setStatus(status);
        this.updateById(purchase);
    }

    @Override
    public IPage<OaPurchase> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        Page<OaPurchase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaPurchase> wrapper = new LambdaQueryWrapper<>();
        if (empId != null) {
            wrapper.eq(OaPurchase::getEmpId, empId);
        }
        if (status != null) {
            wrapper.eq(OaPurchase::getStatus, status);
        }
        wrapper.orderByDesc(OaPurchase::getCreateTime);
        IPage<OaPurchase> result = this.page(page, wrapper);

        fillEmpNames(result.getRecords());
        fillRemarks(result.getRecords());

        return result;
    }

    private void fillEmpNames(List<OaPurchase> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> empIds = records.stream()
                .map(OaPurchase::getEmpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (empIds.isEmpty()) return;

        List<SysEmployee> employees = employeeMapper.selectBatchIds(empIds);
        Map<Long, String> nameMap = employees.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaPurchase record : records) {
            if (record.getEmpId() != null) {
                record.setEmpName(nameMap.getOrDefault(record.getEmpId(), ""));
            }
        }
    }

    private void fillRemarks(List<OaPurchase> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> applyIds = records.stream()
                .map(OaPurchase::getId)
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

        for (OaPurchase record : records) {
            record.setRemark(remarkMap.getOrDefault(record.getId(), ""));
        }
    }
}
