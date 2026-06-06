package cn.oa.task.dto;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data;
@Data @Schema(description = "项目查询参数")
public class TaskProjectQueryDTO {
    @Schema(description = "状态") private String status;
    @Schema(description = "页码") private Integer pageNum = 1;
    @Schema(description = "每页大小") private Integer pageSize = 10;
}