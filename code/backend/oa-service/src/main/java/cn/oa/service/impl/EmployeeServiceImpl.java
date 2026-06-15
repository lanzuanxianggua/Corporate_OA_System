package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.EmployeeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeServiceImpl extends ServiceImpl<SysEmployeeMapper, SysEmployee> implements EmployeeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public IPage<SysEmployee> pageList(int pageNum, int pageSize, String empName, Long deptId, Integer status) {
        Page<SysEmployee> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(empName)) {
            wrapper.like(SysEmployee::getEmpName, empName);
        }
        if (deptId != null) {
            wrapper.eq(SysEmployee::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysEmployee::getStatus, String.valueOf(status));
        }
        wrapper.eq(SysEmployee::getDelFlag, "0");
        wrapper.select(SysEmployee::getId, SysEmployee::getEmpCode, SysEmployee::getEmpName,
                SysEmployee::getPhone, SysEmployee::getEmail, SysEmployee::getDeptId,
                SysEmployee::getAvatar, SysEmployee::getStatus, SysEmployee::getPostId,
                SysEmployee::getCreateTime, SysEmployee::getUpdateTime);
        wrapper.orderByDesc(SysEmployee::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public void updatePassword(Long empId, String oldPwd, String newPwd) {
        SysEmployee employee = this.getById(empId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (!BCrypt.checkpw(oldPwd, employee.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        employee.setPassword(BCrypt.hashpw(newPwd));
        this.updateById(employee);
    }

    @Override
    public void addEmployee(SysEmployee employee) {
        // 校验员工编号是否已存在
        if (StringUtils.hasText(employee.getEmpCode())) {
            Long empCodeCount = this.lambdaQuery()
                    .eq(SysEmployee::getEmpCode, employee.getEmpCode())
                    .eq(SysEmployee::getDelFlag, "0")
                    .count();
            if (empCodeCount > 0) {
                throw new BusinessException("员工编号已存在: " + employee.getEmpCode());
            }
        }
        // 校验手机号是否已存在
        if (StringUtils.hasText(employee.getPhone())) {
            Long phoneCount = this.lambdaQuery()
                    .eq(SysEmployee::getPhone, employee.getPhone())
                    .eq(SysEmployee::getDelFlag, "0")
                    .count();
            if (phoneCount > 0) {
                throw new BusinessException("手机号已存在: " + employee.getPhone());
            }
        }
        // 校验邮箱是否已存在
        if (StringUtils.hasText(employee.getEmail())) {
            Long emailCount = this.lambdaQuery()
                    .eq(SysEmployee::getEmail, employee.getEmail())
                    .eq(SysEmployee::getDelFlag, "0")
                    .count();
            if (emailCount > 0) {
                throw new BusinessException("邮箱已存在: " + employee.getEmail());
            }
        }
        if (StringUtils.hasText(employee.getPassword())) {
            employee.setPassword(BCrypt.hashpw(employee.getPassword()));
        } else {
            employee.setPassword(BCrypt.hashpw("123456"));
        }
        this.save(employee);
    }

    @Override
    public SysEmployee getByEmpCode(String empCode) {
        if (!StringUtils.hasText(empCode)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(SysEmployee::getEmpCode, empCode)
                .eq(SysEmployee::getDelFlag, "0")
                .one();
    }

    @Override
    public SysEmployee getByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(SysEmployee::getEmail, email)
                .eq(SysEmployee::getDelFlag, "0")
                .one();
    }
}
