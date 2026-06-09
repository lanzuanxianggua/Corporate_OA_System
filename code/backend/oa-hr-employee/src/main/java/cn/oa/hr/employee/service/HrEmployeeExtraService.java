package cn.oa.hr.employee.service;

import cn.oa.hr.employee.entity.HrEmployeeCertificate;
import cn.oa.hr.employee.entity.HrEmployeeChange;
import cn.oa.hr.employee.entity.HrEmployeeContract;
import cn.oa.hr.employee.entity.HrEmployeeEducation;
import cn.oa.hr.employee.mapper.HrEmployeeCertificateMapper;
import cn.oa.hr.employee.mapper.HrEmployeeChangeMapper;
import cn.oa.hr.employee.mapper.HrEmployeeContractMapper;
import cn.oa.hr.employee.mapper.HrEmployeeEducationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrEmployeeExtraService {
    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeCertificateMapper certificateMapper;
    private final HrEmployeeEducationMapper educationMapper;

    @Transactional public Long createContract(HrEmployeeContract item) { if (item.getStatus() == null) item.setStatus("ACTIVE"); contractMapper.insert(item); return item.getId(); }
    @Transactional public void updateContract(HrEmployeeContract item) { contractMapper.updateById(item); }
    public Page<HrEmployeeContract> listContracts(Long empId, int pn, int ps) {
        return contractMapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrEmployeeContract>().eq(HrEmployeeContract::getEmpId, empId).orderByDesc(HrEmployeeContract::getStartDate));
    }

    @Transactional public Long createChange(HrEmployeeChange item) { if (item.getStatus() == null) item.setStatus("EFFECTIVE"); changeMapper.insert(item); return item.getId(); }
    @Transactional public void updateChange(HrEmployeeChange item) { changeMapper.updateById(item); }
    public Page<HrEmployeeChange> listChanges(Long empId, int pn, int ps) {
        return changeMapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrEmployeeChange>().eq(HrEmployeeChange::getEmpId, empId).orderByDesc(HrEmployeeChange::getEffectiveDate));
    }

    @Transactional public Long createCertificate(HrEmployeeCertificate item) { if (item.getStatus() == null) item.setStatus("VALID"); certificateMapper.insert(item); return item.getId(); }
    @Transactional public void updateCertificate(HrEmployeeCertificate item) { certificateMapper.updateById(item); }
    public Page<HrEmployeeCertificate> listCertificates(Long empId, int pn, int ps) {
        return certificateMapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrEmployeeCertificate>().eq(HrEmployeeCertificate::getEmpId, empId).orderByDesc(HrEmployeeCertificate::getIssueDate));
    }

    @Transactional public Long createEducation(HrEmployeeEducation item) { educationMapper.insert(item); return item.getId(); }
    @Transactional public void updateEducation(HrEmployeeEducation item) { educationMapper.updateById(item); }
    public Page<HrEmployeeEducation> listEducations(Long empId, int pn, int ps) {
        return educationMapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrEmployeeEducation>().eq(HrEmployeeEducation::getEmpId, empId).orderByDesc(HrEmployeeEducation::getStartDate));
    }
}
