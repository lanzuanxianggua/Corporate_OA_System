package cn.oa.service;

import cn.oa.entity.SysEmployee;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EmployeeService extends IService<SysEmployee> {

    /**
     * 分页查询员工列表
     */
    IPage<SysEmployee> pageList(int pageNum, int pageSize, String empName, Long deptId, Integer status);

    /**
     * 修改密码
     */
    void updatePassword(Long empId, String oldPwd, String newPwd);

    /**
     * 新增员工（密码加密）
     */
    void addEmployee(SysEmployee employee);
}
