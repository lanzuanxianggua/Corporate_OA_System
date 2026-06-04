package cn.oa.system.service;

import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.system.mapper.SysEmpRoleMapper;
import cn.oa.system.mapper.SysRoleMapper;
import cn.oa.system.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务.
 */
@Service
public class AuthService {

    private final SysEmpMapper empMapper;
    private final SysEmpRoleMapper empRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermMapper;

    public AuthService(SysEmpMapper empMapper, SysEmpRoleMapper empRoleMapper,
                       SysRoleMapper roleMapper, SysRolePermissionMapper rolePermMapper) {
        this.empMapper = empMapper;
        this.empRoleMapper = empRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermMapper = rolePermMapper;
    }

    public SysEmp findByUsername(String username) {
        return empMapper.selectOne(new LambdaQueryWrapper<SysEmp>()
                .eq(SysEmp::getUsername, username)
                .last("LIMIT 1"));
    }

    public SysEmp findById(Long empId) {
        return empMapper.selectById(empId);
    }

    @Transactional
    public void recordLogin(Long empId, String ip) {
        SysEmp emp = new SysEmp();
        emp.setId(empId);
        emp.setLastLoginTime(LocalDateTime.now());
        emp.setLastLoginIp(ip);
        empMapper.updateById(emp);
    }

    public List<String> findRolesByEmpId(Long empId) {
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(r -> "ROLE_" + r.getRoleCode())
                .collect(Collectors.toList());
    }

    public List<String> findPermCodesByEmpId(Long empId) {
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return rolePermMapper.selectPermCodesByRoleIds(roleIds);
    }
}
