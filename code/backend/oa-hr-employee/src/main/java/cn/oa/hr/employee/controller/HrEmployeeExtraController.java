package cn.oa.hr.employee.controller;

import cn.oa.hr.employee.entity.HrEmployeeCertificate;
import cn.oa.hr.employee.entity.HrEmployeeChange;
import cn.oa.hr.employee.entity.HrEmployeeContract;
import cn.oa.hr.employee.entity.HrEmployeeEducation;
import cn.oa.hr.employee.service.HrEmployeeExtraService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/employees/{empId}")
@RequiredArgsConstructor
public class HrEmployeeExtraController {
    private final HrEmployeeExtraService service;

    @PostMapping("/contracts") @RequirePermission("hr-employee:contract:list")
    public R<Long> createContract(@PathVariable Long empId, @RequestBody HrEmployeeContract item) { item.setEmpId(empId); return R.ok(service.createContract(item)); }
    @PutMapping("/contracts/{id}") @RequirePermission("hr-employee:contract:list")
    public R<Void> updateContract(@PathVariable Long empId, @PathVariable Long id, @RequestBody HrEmployeeContract item) { item.setId(id); item.setEmpId(empId); service.updateContract(item); return R.ok(); }
    @GetMapping("/contracts") @RequirePermission("hr-employee:contract:list")
    public R<Page<HrEmployeeContract>> contracts(@PathVariable Long empId, @RequestParam(defaultValue = "1") int pn, @RequestParam(defaultValue = "10") int ps) { return R.ok(service.listContracts(empId, pn, ps)); }

    @PostMapping("/changes") @RequirePermission("hr-employee:change:list")
    public R<Long> createChange(@PathVariable Long empId, @RequestBody HrEmployeeChange item) { item.setEmpId(empId); return R.ok(service.createChange(item)); }
    @PutMapping("/changes/{id}") @RequirePermission("hr-employee:change:list")
    public R<Void> updateChange(@PathVariable Long empId, @PathVariable Long id, @RequestBody HrEmployeeChange item) { item.setId(id); item.setEmpId(empId); service.updateChange(item); return R.ok(); }
    @GetMapping("/changes") @RequirePermission("hr-employee:change:list")
    public R<Page<HrEmployeeChange>> changes(@PathVariable Long empId, @RequestParam(defaultValue = "1") int pn, @RequestParam(defaultValue = "10") int ps) { return R.ok(service.listChanges(empId, pn, ps)); }

    @PostMapping("/certificates") @RequirePermission("hr-employee:certificate:list")
    public R<Long> createCertificate(@PathVariable Long empId, @RequestBody HrEmployeeCertificate item) { item.setEmpId(empId); return R.ok(service.createCertificate(item)); }
    @PutMapping("/certificates/{id}") @RequirePermission("hr-employee:certificate:list")
    public R<Void> updateCertificate(@PathVariable Long empId, @PathVariable Long id, @RequestBody HrEmployeeCertificate item) { item.setId(id); item.setEmpId(empId); service.updateCertificate(item); return R.ok(); }
    @GetMapping("/certificates") @RequirePermission("hr-employee:certificate:list")
    public R<Page<HrEmployeeCertificate>> certificates(@PathVariable Long empId, @RequestParam(defaultValue = "1") int pn, @RequestParam(defaultValue = "10") int ps) { return R.ok(service.listCertificates(empId, pn, ps)); }

    @PostMapping("/educations") @RequirePermission("hr-employee:education:list")
    public R<Long> createEducation(@PathVariable Long empId, @RequestBody HrEmployeeEducation item) { item.setEmpId(empId); return R.ok(service.createEducation(item)); }
    @PutMapping("/educations/{id}") @RequirePermission("hr-employee:education:list")
    public R<Void> updateEducation(@PathVariable Long empId, @PathVariable Long id, @RequestBody HrEmployeeEducation item) { item.setId(id); item.setEmpId(empId); service.updateEducation(item); return R.ok(); }
    @GetMapping("/educations") @RequirePermission("hr-employee:education:list")
    public R<Page<HrEmployeeEducation>> educations(@PathVariable Long empId, @RequestParam(defaultValue = "1") int pn, @RequestParam(defaultValue = "10") int ps) { return R.ok(service.listEducations(empId, pn, ps)); }
}
