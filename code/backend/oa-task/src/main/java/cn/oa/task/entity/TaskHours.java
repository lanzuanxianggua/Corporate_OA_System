package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_hours")
public class TaskHours {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long empId;

    private LocalDate workDate;

    private BigDecimal hours;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
