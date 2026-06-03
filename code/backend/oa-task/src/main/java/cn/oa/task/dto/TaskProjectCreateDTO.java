package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "创建项目请求")
public class TaskProjectCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "项目描述")
    private String description;

    @NotNull(message = "负责人不能为空")
    @Schema(description = "负责人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ownerId;

    @Schema(description = "计划开始日期")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期")
    private LocalDate plannedEndDate;
}
