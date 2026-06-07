package cn.oa.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "项目信息")
public class TaskProjectVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "项目编码")
    private String projectCode;
    @Schema(description = "项目描述")
    private String description;
    @Schema(description = "状态: ACTIVE/FROZEN/COMPLETED/ARCHIVED")
    private String status;
    @Schema(description = "开始日期")
    private LocalDate startDate;
    @Schema(description = "结束日期")
    private LocalDate endDate;
    @Schema(description = "所属部门ID")
    private Long deptId;
    @Schema(description = "所属部门名称")
    private String deptName;
    @Schema(description = "负责人ID")
    private Long ownerEmpId;
    @Schema(description = "负责人姓名")
    private String ownerName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
