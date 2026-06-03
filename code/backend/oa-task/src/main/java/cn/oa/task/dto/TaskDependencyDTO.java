package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "任务依赖请求")
public class TaskDependencyDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @NotNull(message = "依赖任务ID不能为空")
    @Schema(description = "依赖任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dependsOnTaskId;

    @NotBlank(message = "依赖类型不能为空")
    @Schema(description = "依赖类型：FS(完成-开始), FF(完成-完成), SS(开始-开始), SF(开始-完成)")
    private String dependencyType;
}
