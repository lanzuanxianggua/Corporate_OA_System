package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_project_member")
public class TaskProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long empId;

    /** OWNER, ADMIN, MEMBER */
    private String role;

    private LocalDateTime joinedAt;
}
