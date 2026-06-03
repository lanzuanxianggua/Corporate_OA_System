package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "更新任务请求")
public class TaskItemUpdateDTO {

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "状态：TODO, IN_PROGRESS, IN_REVIEW, DONE, OVERDUE")
    private String status;

    @Schema(description = "优先级：LOWEST, LOW, MEDIUM, HIGH, URGENT")
    private String priority;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "负责人ID")
    private Long assigneeId;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEndDate;

    @Schema(description = "预估工时(小时)")
    private BigDecimal estimatedHours;

    @Schema(description = "实际工时(小时)")
    private BigDecimal actualHours;

    @Schema(description = "标签(JSON数组)")
    private String tags;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "父任务ID")
    private Long parentTaskId;
}
