package cn.oa.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tk_item")
public class TkItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String title;

    private String description;

    private Long assigneeId;

    private Long reporterId;

    private String status = "0";

    private String priority;

    private Integer progress;

    private LocalDate plannedStart;

    private LocalDate plannedEnd;

    private LocalDate actualStart;

    private LocalDate actualEnd;

    private String sourceType;

    private Long sourceId;

    private Long parentTaskId;

    private Integer sortOrder;

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