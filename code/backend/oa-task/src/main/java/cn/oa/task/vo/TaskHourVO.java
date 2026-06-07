package cn.oa.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "工时记录")
public class TaskHourVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "任务ID")
    private Long itemId;
    @Schema(description = "工作日期")
    private LocalDate workDate;
    @Schema(description = "工时数")
    private BigDecimal hours;
    @Schema(description = "工作描述")
    private String description;
    @Schema(description = "员工ID")
    private Long empId;
    @Schema(description = "员工姓名")
    private String empName;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
