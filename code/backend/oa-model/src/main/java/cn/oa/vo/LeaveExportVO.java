package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveExportVO {

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("请假类型")
    @ColumnWidth(12)
    private String leaveType;

    @ExcelProperty("开始时间")
    @ColumnWidth(20)
    private String startTime;

    @ExcelProperty("结束时间")
    @ColumnWidth(20)
    private String endTime;

    @ExcelProperty("天数")
    @ColumnWidth(10)
    private BigDecimal days;

    @ExcelProperty("原因")
    @ColumnWidth(30)
    private String reason;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;
}
