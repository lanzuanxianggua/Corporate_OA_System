package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaSalaryRecord;
import cn.oa.entity.OaSalaryStructure;
import cn.oa.mapper.OaSalaryRecordMapper;
import cn.oa.mapper.OaSalaryStructureMapper;
import cn.oa.service.SalaryRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class SalaryRecordServiceImpl extends ServiceImpl<OaSalaryRecordMapper, OaSalaryRecord> implements SalaryRecordService {

    @Autowired
    private OaSalaryStructureMapper structureMapper;

    @Override
    public IPage<OaSalaryRecord> pageList(int pageNum, int pageSize, Long empId, String salaryMonth, String searchKey) {
        Page<OaSalaryRecord> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageWithEmpInfo(page, empId, salaryMonth, searchKey);
    }

    @Override
    public OaSalaryRecord myLatestRecord(Long empId) {
        return this.getOne(new LambdaQueryWrapper<OaSalaryRecord>()
                .eq(OaSalaryRecord::getEmpId, empId)
                .orderByDesc(OaSalaryRecord::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public void generateMonthlyRecord(Long empId, String month) {
        OaSalaryStructure structure = structureMapper.selectOne(
                new LambdaQueryWrapper<OaSalaryStructure>()
                        .eq(OaSalaryStructure::getEmpId, empId)
                        .eq(OaSalaryStructure::getStatus, "0")
                        .orderByDesc(OaSalaryStructure::getEffectiveDate)
                        .last("LIMIT 1"));
        if (structure == null) {
            throw new BusinessException("员工薪资结构不存在");
        }

        BigDecimal actualAmount = structure.getBaseSalary()
                .add(structure.getPostSalary())
                .add(structure.getMeritSalary())
                .add(structure.getAllowance());

        OaSalaryRecord record = new OaSalaryRecord();
        record.setEmpId(empId);
        record.setSalaryMonth(month);
        record.setBaseSalary(structure.getBaseSalary());
        record.setPostSalary(structure.getPostSalary());
        record.setMeritSalary(structure.getMeritSalary());
        record.setAllowance(structure.getAllowance());
        record.setDeduction(BigDecimal.ZERO);
        record.setActualAmount(actualAmount);
        record.setPayTime(LocalDateTime.now());
        this.save(record);
        log.info("Salary record generated: empId={}, month={}, amount={}", empId, month, actualAmount);
    }
}
