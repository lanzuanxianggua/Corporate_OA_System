package cn.oa.hr;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hr_leave_rule")
public class HrLeaveRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private String leaveType;

    private String ruleScript;

    private Double minUnit;

    private Integer deductSalary;

    private Integer requireAttachment;

    private String status;

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