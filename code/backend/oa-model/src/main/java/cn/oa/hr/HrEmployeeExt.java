package cn.oa.hr;

import com.baomidou.mybatisplus.annotation.*;
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

    private LocalDate entryDate;

    private LocalDate probationEndDate;

    private LocalDate contractStart;

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