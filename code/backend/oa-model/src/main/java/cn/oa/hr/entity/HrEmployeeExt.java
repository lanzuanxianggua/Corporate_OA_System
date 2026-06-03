package cn.oa.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hr_employee_ext")
public class HrEmployeeExt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String education;

    private String major;

    private String graduateSchool;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate probationEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractEnd;

    private String emergencyContact;

    private String emergencyPhone;

    private String address;

    private String remark;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}