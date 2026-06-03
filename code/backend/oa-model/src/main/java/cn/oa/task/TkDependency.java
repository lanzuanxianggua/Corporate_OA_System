package cn.oa.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tk_dependency")
public class TkDependency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long dependsOnTaskId;

    private String dependencyType;
}