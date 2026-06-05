package cn.oa.hr.employee.mapper;

import cn.oa.hr.employee.entity.HrEmployeeProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 员工档案 Mapper.
 */
@Mapper
public interface HrEmployeeProfileMapper extends BaseMapper<HrEmployeeProfile> {

    /**
     * 关联 sys_employee + sys_dept 查列表.
     */
    @Select("""
        SELECT
          p.id, p.emp_id, p.work_no, p.hire_date, p.contract_type, p.contract_end_date,
          p.emergency_contact, p.emergency_phone, p.bank_name, p.bank_account, p.status,
          p.create_time, p.update_time,
          e.real_name AS emp_name, e.username, e.emp_no,
          d.id AS dept_id, d.dept_name
        FROM hr_employee_profile p
        LEFT JOIN sys_employee e ON p.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE p.del_flag = '0'
        ORDER BY p.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findAllWithJoins(@Param("limit") int limit);

    /**
     * 关联详情.
     */
    @Select("""
        SELECT
          p.id, p.emp_id, p.work_no, p.hire_date, p.contract_type, p.contract_end_date,
          p.emergency_contact, p.emergency_phone, p.bank_name, p.bank_account, p.status,
          p.create_time, p.update_time,
          e.real_name AS emp_name, e.username, e.emp_no,
          d.id AS dept_id, d.dept_name
        FROM hr_employee_profile p
        LEFT JOIN sys_employee e ON p.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE p.del_flag = '0' AND p.id = #{id}
        """)
    Map<String, Object> findDetail(@Param("id") Long id);
}
