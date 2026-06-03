package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "登记工时请求")
public class TaskHoursCreateDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @NotNull(message = "工作日期不能为空")
    @Schema(description = "工作日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate workDate;

    @NotNull(message = "工时不能为空")
    @Schema(description = "工时(小时)", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal hours;

    @Schema(description = "工作内容描述")
    private String description;
}
