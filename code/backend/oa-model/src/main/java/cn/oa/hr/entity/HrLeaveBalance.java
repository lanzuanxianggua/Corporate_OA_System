package cn.oa.hr.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("hr_leave_balance")
public class HrLeaveBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String leaveType;

    private Integer year;

    private BigDecimal totalDays;

    private BigDecimal usedDays;

    private BigDecimal remainingDays;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
}