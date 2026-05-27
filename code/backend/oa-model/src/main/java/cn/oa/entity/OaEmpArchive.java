package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_emp_archive")
public class OaEmpArchive {

    @TableId(type = IdType.ASSIGN_ID)
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

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String empName;

    @TableField(exist = false)
    private String empNo;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String phone;

    @TableField(exist = false)
    private String email;

    @TableField(exist = false)
    private String idCard;

    @TableField(exist = false)
    private LocalDate hireDate;

    @TableLogic
    private Integer delFlag;
}
