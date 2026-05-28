package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ScheduleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long empId;

    @NotBlank(message = "日程标题不能为空")
    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime remindTime;

    private Integer status;
}
