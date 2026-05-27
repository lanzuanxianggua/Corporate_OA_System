package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_salary_structure")
public class OaSalaryStructure {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;

    private BigDecimal baseSalary;

    private BigDecimal postSalary;

    private BigDecimal meritSalary;

    private BigDecimal allowance;

    @TableField(exist = false)
    private String empName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    private Character status = '0';

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
