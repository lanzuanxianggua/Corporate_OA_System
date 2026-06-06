package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目.
 *
 * <p>对应表 task_projects, 状态: ACTIVE/FROZEN/COMPLETED/ARCHIVED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_projects")
@Schema(description = "项目")
public class TaskProject extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目名称")
    @TableField("project_name")
    private String projectName;

    @Schema(description = "项目编码")
    @TableField("project_code")
    private String projectCode;

    @Schema(description = "项目描述")
    @TableField("description")
    private String description;

    @Schema(description = "状态: ACTIVE/FROZEN/COMPLETED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "开始日期")
    @TableField("start_date")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @TableField("end_date")
    private LocalDate endDate;

    @Schema(description = "所属部门 id")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "负责人 emp_id")
    @TableField("owner_emp_id")
    private Long ownerEmpId;
}
