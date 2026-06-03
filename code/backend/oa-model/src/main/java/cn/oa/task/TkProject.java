package cn.oa.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tk_project")
public class TkProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String status = "0";

    private LocalDate plannedStart;

    private LocalDate plannedEnd;

    private LocalDate actualStart;

    private LocalDate actualEnd;

    private Long managerId;

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