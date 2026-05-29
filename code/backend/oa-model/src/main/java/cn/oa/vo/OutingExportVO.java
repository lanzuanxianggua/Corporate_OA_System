package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OutingExportVO {

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("目的地")
    @ColumnWidth(15)
    private String destination;

    @ExcelProperty("原因")
    @ColumnWidth(30)
    private String reason;

    @ExcelProperty("开始时间")
    @ColumnWidth(20)
    private String startTime;

    @ExcelProperty("结束时间")
    @ColumnWidth(20)
    private String endTime;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;
}
