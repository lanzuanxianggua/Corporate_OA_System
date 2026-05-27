package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_leave_balance")
public class OaLeaveBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private Integer leaveType;

    private Integer year;

    private BigDecimal totalDays;

    private BigDecimal usedDays = BigDecimal.ZERO;

    private BigDecimal remainingDays;

    @TableField(exist = false)
    private String empName;

    @TableField(exist = false)
    private String deptName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
