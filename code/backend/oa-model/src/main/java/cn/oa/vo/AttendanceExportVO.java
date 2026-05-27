package cn.oa.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class AttendanceExportVO {

    @ExcelProperty("员工编号")
    @ColumnWidth(15)
    private String empCode;

    @ExcelProperty("员工姓名")
    @ColumnWidth(15)
    private String empName;

    @ExcelProperty("日期")
    @ColumnWidth(15)
    private String workDate;

    @ExcelProperty("上班时间")
    @ColumnWidth(20)
    private String clockIn;

    @ExcelProperty("下班时间")
    @ColumnWidth(20)
    private String clockOut;

    @ExcelProperty("状态")
    @ColumnWidth(12)
    private String statusText;

    @ExcelProperty("备注")
    @ColumnWidth(20)
    private String remark;
}
