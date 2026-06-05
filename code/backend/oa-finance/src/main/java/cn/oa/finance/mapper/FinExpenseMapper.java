package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinExpense;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报销 Mapper.
 */
@Mapper
public interface FinExpenseMapper extends BaseMapper<FinExpense> {

    /**
     * 按 empId 查报销单列表.
     */
    @Select("""
        SELECT
          e.id, e.apply_no, e.emp_id, e.dept_id, e.expense_type,
          e.total_amount, e.reason, e.status, e.wf_instance_id,
          e.loan_offset_amount, e.paid_time, e.create_time, e.update_time,
          emp.real_name AS emp_name, d.dept_name
        FROM fin_expenses e
        LEFT JOIN sys_employee emp ON e.emp_id = emp.id AND emp.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE e.del_flag = '0'
          AND e.emp_id = #{empId}
        ORDER BY e.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findByEmpId(@Param("empId") Long empId, @Param("limit") int limit);

    /**
     * 按 apply_no 查详情.
     */
    @Select("""
        SELECT
          e.id, e.apply_no, e.emp_id, e.dept_id, e.expense_type,
          e.total_amount, e.reason, e.status, e.wf_instance_id,
          e.loan_offset_amount, e.paid_time, e.create_time, e.update_time,
          emp.real_name AS emp_name, d.dept_name
        FROM fin_expenses e
        LEFT JOIN sys_employee emp ON e.emp_id = emp.id AND emp.del_flag = '0'
        LEFT JOIN sys_dept d ON e.dept_id = d.id AND d.del_flag = '0'
        WHERE e.del_flag = '0'
          AND e.apply_no = #{applyNo}
        """)
    Map<String, Object> findByApplyNo(@Param("applyNo") String applyNo);
}
