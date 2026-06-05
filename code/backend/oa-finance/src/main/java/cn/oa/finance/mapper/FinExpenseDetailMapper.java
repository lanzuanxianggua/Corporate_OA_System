package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinExpenseDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 报销明细 Mapper.
 */
@Mapper
public interface FinExpenseDetailMapper extends BaseMapper<FinExpenseDetail> {

    /**
     * 按 expenseId 查明细列表.
     */
    @Select("""
        SELECT *
        FROM fin_expense_details
        WHERE del_flag = '0'
          AND expense_id = #{expenseId}
        ORDER BY fee_date ASC, id ASC
        """)
    List<FinExpenseDetail> findByExpenseId(@Param("expenseId") Long expenseId);

    /**
     * 按 invoice_no 查明细.
     */
    @Select("""
        SELECT *
        FROM fin_expense_details
        WHERE del_flag = '0'
          AND invoice_no = #{invoiceNo}
        LIMIT 1
        """)
    FinExpenseDetail findByInvoiceNo(@Param("invoiceNo") String invoiceNo);
}
