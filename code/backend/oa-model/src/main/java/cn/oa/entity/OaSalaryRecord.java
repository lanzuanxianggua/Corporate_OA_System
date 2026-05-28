package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_salary_record")
public class OaSalaryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    @TableField(exist = false)
    private String empName;

    private String salaryMonth;

    private BigDecimal baseSalary;

    private BigDecimal postSalary;

    private BigDecimal meritSalary;

    private BigDecimal allowance;

    private BigDecimal deduction = BigDecimal.ZERO;

    private BigDecimal actualAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    private String status = "0";

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;
}
