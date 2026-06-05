package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinLoanRepayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 还款记录 Mapper.
 */
@Mapper
public interface FinLoanRepaymentMapper extends BaseMapper<FinLoanRepayment> {

    /**
     * 按 loanId 查还款记录列表.
     */
    @Select("""
        SELECT *
        FROM fin_loan_repayments
        WHERE del_flag = '0'
          AND loan_id = #{loanId}
        ORDER BY repay_date DESC, create_time DESC
        """)
    List<FinLoanRepayment> findByLoanId(@Param("loanId") Long loanId);

    /**
     * 按 expense_id 查关联的还款记录 (报销冲抵场景).
     */
    @Select("""
        SELECT *
        FROM fin_loan_repayments
        WHERE del_flag = '0'
          AND expense_id = #{expenseId}
        LIMIT 1
        """)
    FinLoanRepayment findByExpenseId(@Param("expenseId") Long expenseId);
}
