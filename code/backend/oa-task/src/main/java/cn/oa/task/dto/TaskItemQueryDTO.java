package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "任务查询参数")
public class TaskItemQueryDTO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "父任务ID")
    private Long parentTaskId;

    @Schema(description = "标题(模糊)")
    private String title;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "负责人ID")
    private Long assigneeId;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "是否仅根任务(不包含子任务)")
    private Boolean rootOnly;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "是否升序")
    private Boolean asc;
}
