package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.EmployeeDTO;
import cn.oa.service.EmployeeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/employee")
@Tag(name = "员工管理")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询员工")
    public R<PageResult<SysEmployee>> page(@RequestParam int pageNum,
                                           @RequestParam int pageSize,
                                           @RequestParam(required = false) String empName,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) Integer status) {
        IPage<SysEmployee> page = employeeService.pageList(pageNum, pageSize, empName, deptId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取员工详情")
    public R<SysEmployee> getById(@PathVariable Long id) {
        SysEmployee employee = employeeService.getById(id);
        if (employee != null) {
            employee.setPassword(null);
        }
        return R.ok(employee);
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增员工")
    @OperationLog(module = "员工管理", operation = "新增员工")
    public R<Long> add(@RequestBody @Valid EmployeeDTO dto) {
        SysEmployee employee = new SysEmployee();
        employee.setEmpCode(dto.getEmpCode());
        employee.setEmpName(dto.getEmpName());
        employee.setPassword(dto.getPassword());
        employee.setPhone(dto.getPhone());
        employee.setEmail(dto.getEmail());
        employee.setDeptId(dto.getDeptId());
        employee.setAvatar(dto.getAvatar());
        employee.setStatus(dto.getStatus());
        employee.setPostId(dto.getPostId());
        employeeService.addEmployee(employee);
        log.info("Employee created: empCode={}, id={}", employee.getEmpCode(), employee.getId());
        return R.ok(employee.getId());
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改员工")
    @OperationLog(module = "员工管理", operation = "修改员工")
    public R<Void> update(@RequestBody @Valid EmployeeDTO dto) {
        SysEmployee employee = new SysEmployee();
        employee.setId(dto.getId());
        employee.setEmpCode(dto.getEmpCode());
        employee.setEmpName(dto.getEmpName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            employee.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(dto.getPassword()));
        }
        employee.setPhone(dto.getPhone());
        employee.setEmail(dto.getEmail());
        employee.setDeptId(dto.getDeptId());
        employee.setAvatar(dto.getAvatar());
        employee.setStatus(dto.getStatus());
        employee.setPostId(dto.getPostId());
        employeeService.updateById(employee);
        log.info("Employee updated: id={}", employee.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除员工")
    @OperationLog(module = "员工管理", operation = "删除员工")
    public R<Void> delete(@PathVariable Long id) {
        employeeService.removeById(id);
        log.info("Employee deleted: id={}", id);
        return R.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    @OperationLog(module = "员工管理", operation = "修改密码")
    public R<Void> updatePassword(@RequestParam Long empId,
                                  @RequestParam String oldPwd,
                                  @RequestParam String newPwd,
                                  HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        // Admin can reset any employee's password; non-admin can only change own
        if (!currentEmpId.equals(empId) && !currentEmpId.equals(1L)) {
            return R.fail("只能修改自己的密码");
        }
        employeeService.updatePassword(empId, oldPwd, newPwd);
        log.info("Employee password changed: empId={}", empId);
        return R.ok();
    }
}
