package cn.oa.hr;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hr_transfer")
public class HrTransfer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String transferType;

    private Long fromDeptId;

    private Long toDeptId;

    private Long fromPostId;

    private Long toPostId;

    private LocalDate effectiveDate;

    private String reason;

    private Long processInstanceId;

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