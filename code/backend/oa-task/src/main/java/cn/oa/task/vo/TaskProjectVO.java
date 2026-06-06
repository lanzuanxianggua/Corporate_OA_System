package cn.oa.task.vo;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Schema(description = "项目信息")
public class TaskProjectVO {
    private Long id; private String projectName; private String projectCode; private String description;
    private String status; private LocalDate startDate; private LocalDate endDate;
    private Long deptId; private String deptName; private Long ownerEmpId; private String ownerName;
    private LocalDateTime createTime;
}