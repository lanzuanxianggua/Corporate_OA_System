package cn.oa.task.vo;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Schema(description = "任务信息")
public class TaskItemVO {
    private Long id; private Long projectId; private String projectName; private String taskName;
    private String description; private Long assigneeId; private String assigneeName;
    private String status; private String priority; private LocalDate planStartDate; private LocalDate planEndDate;
    private Integer progress; private Long parentTaskId; private int subTaskCount; private LocalDateTime createTime;
}