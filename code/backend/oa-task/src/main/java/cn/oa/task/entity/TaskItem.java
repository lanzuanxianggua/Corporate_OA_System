package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_item")
public class TaskItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long parentTaskId;

    private String title;

    private String description;

    /** TODO, IN_PROGRESS, IN_REVIEW, DONE, OVERDUE */
    private String status;

    /** LOWEST, LOW, MEDIUM, HIGH, URGENT */
    private String priority;

    /** 0-100 */
    private Integer progress;

    private Long assigneeId;

    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedEndDate;

    private BigDecimal estimatedHours;

    private BigDecimal actualHours;

    /** JSON array of tags */
    private String tags;

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
