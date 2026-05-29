package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanExportVO {

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("借款金额")
    @ColumnWidth(12)
    private BigDecimal loanAmount;

    @ExcelProperty("借款原因")
    @ColumnWidth(30)
    private String loanReason;

    @ExcelProperty("还款计划")
    @ColumnWidth(20)
    private String repaymentPlan;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;
}
