package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务.
 *
 * <p>对应表 task_items, 状态: TODO/IN_PROGRESS/DONE/CLOSED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_items")
@Schema(description = "任务")
public class TaskItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属项目 id")
    @TableField("project_id")
    private Long projectId;

    @Schema(description = "任务名称")
    @TableField("task_name")
    private String taskName;

    @Schema(description = "任务描述")
    @TableField("description")
    private String description;

    @Schema(description = "负责人 emp_id")
    @TableField("assignee_id")
    private Long assigneeId;

    @Schema(description = "状态: TODO/IN_PROGRESS/DONE/CLOSED")
    @TableField("status")
    private String status;

    @Schema(description = "优先级: HIGH/NORMAL/LOW")
    @TableField("priority")
    private String priority;

    @Schema(description = "计划开始日期")
    @TableField("plan_start_date")
    private LocalDate planStartDate;

    @Schema(description = "计划结束日期")
    @TableField("plan_end_date")
    private LocalDate planEndDate;

    @Schema(description = "实际开始时间")
    @TableField("actual_start")
    private LocalDateTime actualStart;

    @Schema(description = "实际结束时间")
    @TableField("actual_end")
    private LocalDateTime actualEnd;

    @Schema(description = "进度百分比 (0-100)")
    @TableField("progress")
    private Integer progress;

    @Schema(description = "父任务 id")
    @TableField("parent_task_id")
    private Long parentTaskId;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;
}
