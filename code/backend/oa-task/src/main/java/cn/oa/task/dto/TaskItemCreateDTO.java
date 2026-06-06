package cn.oa.task.dto;
import io.swagger.v3.oas.annotations.media.Schema; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import lombok.Data; import java.time.LocalDate;
@Data @Schema(description = "创建任务请求")
public class TaskItemCreateDTO {
    @NotNull @Schema(description = "项目ID") private Long projectId;
    @NotBlank @Schema(description = "任务名称") private String taskName;
    @Schema(description = "描述") private String description;
    @Schema(description = "负责人ID") private Long assigneeId;
    @Schema(description = "优先级") private String priority = "NORMAL";
    @Schema(description = "计划开始日期") private LocalDate planStartDate;
    @Schema(description = "计划结束日期") private LocalDate planEndDate;
    @Schema(description = "父任务ID") private Long parentTaskId;
}