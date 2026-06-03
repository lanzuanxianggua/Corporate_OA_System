package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "更新项目请求")
public class TaskProjectUpdateDTO {

    @Schema(description = "项目ID")
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "状态：PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD, CANCELLED")
    private String status;

    @Schema(description = "进度百分比(0-100)")
    private Integer progress;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEndDate;

    @Schema(description = "实际开始日期")
    private LocalDate actualStartDate;

    @Schema(description = "实际结束日期")
    private LocalDate actualEndDate;
}
