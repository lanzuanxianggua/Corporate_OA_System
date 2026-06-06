package cn.oa.task.dto;
import io.swagger.v3.oas.annotations.media.Schema; import jakarta.validation.constraints.NotNull; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDate;
@Data @Schema(description = "登记工时请求")
public class TaskHourCreateDTO {
    @NotNull @Schema(description = "任务ID") private Long itemId;
    @NotNull @Schema(description = "工作日期") private LocalDate workDate;
    @NotNull @Schema(description = "工时数") private BigDecimal hours;
    @Schema(description = "说明") private String description;
}