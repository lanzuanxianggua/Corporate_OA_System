package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OvertimeExportVO {

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("加班日期")
    @ColumnWidth(15)
    private String overtimeDate;

    @ExcelProperty("开始时间")
    @ColumnWidth(20)
    private String startTime;

    @ExcelProperty("结束时间")
    @ColumnWidth(20)
    private String endTime;

    @ExcelProperty("时长(小时)")
    @ColumnWidth(12)
    private BigDecimal hours;

    @ExcelProperty("原因")
    @ColumnWidth(30)
    private String reason;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;
}
