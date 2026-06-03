package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_dependency")
public class TaskDependency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long dependsOnTaskId;

    /** FS, FF, SS, SF */
    private String dependencyType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
