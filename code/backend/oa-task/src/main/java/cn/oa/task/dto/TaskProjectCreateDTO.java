package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data @Schema(description = "创建项目请求")
public class TaskProjectCreateDTO {
    @NotBlank @Schema(description = "项目名称") private String projectName;
    @Schema(description = "描述") private String description;
    @Schema(description = "开始日期") private LocalDate startDate;
    @Schema(description = "结束日期") private LocalDate endDate;
    @Schema(description = "部门ID") private Long deptId;
}