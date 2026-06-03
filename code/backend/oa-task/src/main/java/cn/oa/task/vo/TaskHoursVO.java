package cn.oa.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时VO
 */
@Data
public class TaskHoursVO {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long empId;
    private String empName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    private BigDecimal hours;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
