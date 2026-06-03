package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "创建任务请求")
public class TaskItemCreateDTO {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @NotBlank(message = "任务标题不能为空")
    @Schema(description = "任务标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "优先级：LOWEST, LOW, MEDIUM, HIGH, URGENT")
    private String priority;

    @Schema(description = "负责人ID")
    private Long assigneeId;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEndDate;

    @Schema(description = "预估工时(小时)")
    private BigDecimal estimatedHours;

    @Schema(description = "标签(JSON数组)")
    private String tags;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
