package cn.oa.hr;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("hr_leave_balance")
public class HrLeaveBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String leaveType;

    private Integer year;

    private Double totalDays;

    private Double usedDays;

    private Double remainingDays;

    private LocalDate expireDate;
}