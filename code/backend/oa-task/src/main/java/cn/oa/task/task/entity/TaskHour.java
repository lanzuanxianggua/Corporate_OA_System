package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工时.
 *
 * <p>对应表 task_hours.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_hours")
@Schema(description = "工时")
public class TaskHour extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 id")
    @TableField("item_id")
    private Long itemId;

    @Schema(description = "工作日期")
    @TableField("work_date")
    private LocalDate workDate;

    @Schema(description = "工时数")
    @TableField("hours")
    private BigDecimal hours;

    @Schema(description = "工作描述")
    @TableField("description")
    private String description;

    @Schema(description = "员工 emp_id")
    @TableField("emp_id")
    private Long empId;
}
