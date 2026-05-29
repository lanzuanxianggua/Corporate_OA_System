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
        if (StringUtils.hasText(employee.getPassword())) {
            employee.setPassword(BCrypt.hashpw(employee.getPassword()));
        } else {
            employee.setPassword(BCrypt.hashpw("123456"));
        }
        this.save(employee);
    }
}
