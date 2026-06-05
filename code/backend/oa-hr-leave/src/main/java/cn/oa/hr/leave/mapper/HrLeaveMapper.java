package cn.oa.hr.leave.mapper;

import cn.oa.hr.leave.entity.HrLeave;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 请假申请 Mapper.
 */
@Mapper
public interface HrLeaveMapper extends BaseMapper<HrLeave> {

    /**
     * 按 empId 查请假单列表 (不通过 BaseEntity.getByPage, 直接 SQL 关联 sys_employee 拿申请人姓名).
     */
    @Select("""
        SELECT
          l.id, l.emp_id, l.leave_type, l.start_date, l.end_date, l.total_days,
          l.reason, l.status, l.wf_instance_id, l.create_time, l.update_time,
          e.real_name AS emp_name, e.dept_id, d.dept_name
        FROM hr_leave l
        LEFT JOIN sys_employee e ON l.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE l.del_flag = '0'
          AND l.emp_id = #{empId}
        ORDER BY l.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findByEmpId(@Param("empId") Long empId, @Param("limit") int limit);

    /**
     * 详情: 同 findByEmpId 但按主键.
     */
    @Select("""
        SELECT
          l.id, l.emp_id, l.leave_type, l.start_date, l.end_date, l.total_days,
          l.reason, l.status, l.wf_instance_id, l.create_time, l.update_time,
          e.real_name AS emp_name, e.dept_id, d.dept_name
        FROM hr_leave l
        LEFT JOIN sys_employee e ON l.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE l.del_flag = '0'
          AND l.id = #{id}
        """)
    Map<String, Object> findDetail(@Param("id") Long id);

    /**
     * 查询员工所有假期余额.
     */
    @Select("""
        SELECT
          b.id, b.emp_id, b.leave_type, b.year,
          b.total_days, b.used_days, b.frozen_days, b.remaining_days, b.status
        FROM hr_leave_balance b
        WHERE b.del_flag = '0'
          AND b.emp_id = #{empId}
          AND b.status = 'ACTIVE'
        ORDER BY b.leave_type, b.year DESC
        """)
    List<Map<String, Object>> findBalancesByEmpId(@Param("empId") Long empId);
}
