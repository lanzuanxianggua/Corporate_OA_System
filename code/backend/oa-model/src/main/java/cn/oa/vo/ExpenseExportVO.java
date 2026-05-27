package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseExportVO {

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("标题")
    @ColumnWidth(20)
    private String title;

    @ExcelProperty("类别")
    @ColumnWidth(12)
    private String category;

    @ExcelProperty("金额")
    @ColumnWidth(12)
    private BigDecimal amount;

    @ExcelProperty("描述")
    @ColumnWidth(30)
    private String description;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;
}
