package cn.oa.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务VO
 */
@Data
public class TaskItemVO {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long parentTaskId;
    private String parentTaskTitle;
    private String title;
    private String description;
    private String status;
    private String statusName;
    private String priority;
    private String priorityName;
    private Integer progress;
    private Long assigneeId;
    private String assigneeName;
    private Long creatorId;
    private String creatorName;
    private Integer childCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedEndDate;

    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private String tags;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
