package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "项目查询参数")
public class TaskProjectQueryDTO {

    @Schema(description = "项目名称(模糊)")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "当前用户参与的(true=查询我参与的)")
    private Boolean mine;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "是否升序")
    private Boolean asc;
}
