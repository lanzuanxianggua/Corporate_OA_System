package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinLoan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 借款 Mapper.
 */
@Mapper
public interface FinLoanMapper extends BaseMapper<FinLoan> {

    /**
     * 按 empId 查借款单列表.
     */
    @Select("""
        SELECT
          l.id, l.apply_no, l.emp_id, l.dept_id, l.loan_type,
          l.amount, l.purpose, l.status, l.wf_instance_id,
          l.repaid_amount, l.deadline_date, l.create_time, l.update_time,
          e.real_name AS emp_name, d.dept_name
        FROM fin_loans l
        LEFT JOIN sys_employee e ON l.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON l.dept_id = d.id AND d.del_flag = '0'
        WHERE l.del_flag = '0'
          AND l.emp_id = #{empId}
        ORDER BY l.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findByEmpId(@Param("empId") Long empId, @Param("limit") int limit);

    /**
     * 按 apply_no 查详情.
     */
    @Select("""
        SELECT
          l.id, l.apply_no, l.emp_id, l.dept_id, l.loan_type,
          l.amount, l.purpose, l.status, l.wf_instance_id,
          l.repaid_amount, l.deadline_date, l.create_time, l.update_time,
          e.real_name AS emp_name, d.dept_name
        FROM fin_loans l
        LEFT JOIN sys_employee e ON l.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON l.dept_id = d.id AND d.del_flag = '0'
        WHERE l.del_flag = '0'
          AND l.apply_no = #{applyNo}
        """)
    Map<String, Object> findByApplyNo(@Param("applyNo") String applyNo);
}
