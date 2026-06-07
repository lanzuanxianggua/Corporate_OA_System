package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinBudget;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预算 Mapper.
 */
@Mapper
public interface FinBudgetMapper extends BaseMapper<FinBudget> {

    /**
     * 按 empId 查预算列表 (关联员工姓名/部门).
     */
    @Select("""
        SELECT
          b.id, b.emp_id, b.dept_id, b.budget_year, b.budget_name,
          b.total_amount, b.used_amount, b.frozen_amount, b.status,
          b.create_time, b.update_time,
          e.real_name AS emp_name, d.dept_name
        FROM fin_budgets b
        LEFT JOIN sys_employee e ON b.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON b.dept_id = d.id AND d.del_flag = '0'
        WHERE b.del_flag = '0'
          AND b.emp_id = #{empId}
        ORDER BY b.budget_year DESC, b.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> findByEmpId(@Param("empId") Long empId, @Param("limit") int limit);

    /**
     * 按部门+年度查预算.
     */
    @Select("""
        SELECT
          b.id, b.emp_id, b.dept_id, b.budget_year, b.budget_name,
          b.total_amount, b.used_amount, b.frozen_amount, b.status,
          b.create_time, b.update_time,
          e.real_name AS emp_name, d.dept_name
        FROM fin_budgets b
        LEFT JOIN sys_employee e ON b.emp_id = e.id AND e.del_flag = '0'
        LEFT JOIN sys_dept d ON b.dept_id = d.id AND d.del_flag = '0'
        WHERE b.del_flag = '0'
          AND b.dept_id = #{deptId}
          AND b.budget_year = #{year}
        ORDER BY b.create_time DESC
        """)
    List<Map<String, Object>> findByDeptAndYear(@Param("deptId") Long deptId, @Param("year") int year);

    /**
     * 行级锁: 按 deptId + budgetYear 取一条 ACTIVE 预算并加 X 锁.
     * 同一事务内串行化并发冻结/解冻请求, 避免超扣.
     */
    @Select("""
        SELECT *
        FROM fin_budgets
        WHERE del_flag = '0'
          AND dept_id = #{deptId}
          AND budget_year = #{year}
          AND status = 'ACTIVE'
        ORDER BY create_time DESC
        LIMIT 1
        FOR UPDATE
        """)
    FinBudget selectActiveForUpdate(@Param("deptId") Long deptId, @Param("year") int year);

    /**
     * 原子冻结: 累加 frozen_amount.
     * <p>配合 selectActiveForUpdate 行级锁, 由 Service 在同一事务内调用.
     */
    @Update("""
        UPDATE fin_budgets
        SET frozen_amount = frozen_amount + #{delta},
            update_by = 'system',
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND del_flag = '0'
        """)
    int atomicFreeze(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /**
     * 原子解冻: 扣减 frozen_amount (不允许为负).
     */
    @Update("""
        UPDATE fin_budgets
        SET frozen_amount = GREATEST(frozen_amount - #{delta}, 0),
            update_by = 'system',
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND del_flag = '0'
        """)
    int atomicUnfreeze(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /**
     * 原子扣减: frozen - delta, used + delta.
     * <p>配合 selectActiveForUpdate, 由 Service 在同一事务内调用.
     */
    @Update("""
        UPDATE fin_budgets
        SET frozen_amount = GREATEST(frozen_amount - #{delta}, 0),
            used_amount   = used_amount + #{delta},
            update_by = 'system',
            update_time = NOW(),
            version = version + 1
        WHERE id = #{id}
          AND del_flag = '0'
        """)
    int atomicDeduct(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
