package cn.oa.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "任务信息")
public class TaskItemVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "所属项目ID")
    private Long projectId;
    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "任务名称")
    private String taskName;
    @Schema(description = "任务描述")
    private String description;
    @Schema(description = "负责人ID")
    private Long assigneeId;
    @Schema(description = "负责人姓名")
    private String assigneeName;
    @Schema(description = "状态: TODO/IN_PROGRESS/DONE/CLOSED")
    private String status;
    @Schema(description = "优先级: HIGH/NORMAL/LOW")
    private String priority;
    @Schema(description = "计划开始日期")
    private LocalDate planStartDate;
    @Schema(description = "计划结束日期")
    private LocalDate planEndDate;
    @Schema(description = "实际开始时间")
    private LocalDateTime actualStart;
    @Schema(description = "实际结束时间")
    private LocalDateTime actualEnd;
    @Schema(description = "进度 0-100")
    private Integer progress;
    @Schema(description = "父任务ID")
    private Long parentTaskId;
    @Schema(description = "子任务数")
    private int subTaskCount;
    @Schema(description = "排序号")
    private Integer sortOrder;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
